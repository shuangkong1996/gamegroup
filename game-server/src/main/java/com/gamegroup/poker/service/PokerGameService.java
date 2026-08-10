package com.gamegroup.poker.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.entity.PokerHandAction;
import com.gamegroup.entity.PokerHandEntity;
import com.gamegroup.entity.PokerHandPlayer;
import com.gamegroup.entity.PokerRoom;
import com.gamegroup.entity.PokerRoomPlayer;
import com.gamegroup.entity.User;
import com.gamegroup.poker.engine.Card;
import com.gamegroup.poker.engine.HandEvaluator;
import com.gamegroup.poker.engine.HandValue;
import com.gamegroup.poker.engine.PokerHand;
import com.gamegroup.poker.engine.PokerTable;
import com.gamegroup.poker.ws.SessionRegistry;
import com.gamegroup.poker.ws.WsInbound;
import com.gamegroup.poker.ws.WsJson;
import com.gamegroup.poker.ws.WsOutbound;
import com.gamegroup.poker.ws.WsTypes;
import com.gamegroup.repository.PokerHandActionRepository;
import com.gamegroup.repository.PokerHandEntityRepository;
import com.gamegroup.repository.PokerHandPlayerRepository;
import com.gamegroup.repository.PokerRoomPlayerRepository;
import com.gamegroup.repository.PokerRoomRepository;
import com.gamegroup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 德州扑克游戏服务：持有各房间运行时状态（引擎对象），驱动牌局并通过 WS 广播快照。
 * 房间运行时状态保存在内存（单实例部署），Redis 用于未来多实例扩展。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PokerGameService {

    private final PokerRoomRepository roomRepository;
    private final PokerRoomPlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final PokerHandEntityRepository handEntityRepository;
    private final PokerHandPlayerRepository handPlayerRepository;
    private final PokerHandActionRepository handActionRepository;
    private final SessionRegistry sessionRegistry;

    /** roomId -> 房间运行时 */
    private final Map<Long, TableSession> sessions = new ConcurrentHashMap<>();

    /** 房间运行时：引擎桌 + 当前手 + 最近结算。 */
    private static final class TableSession {
        final Long roomId;
        final PokerTable table;
        final ReentrantLock lock = new ReentrantLock();
        PokerHand hand;
        PokerHand.HandResult lastHand;
        boolean running;

        /** 当前行动者的超时截止时间戳（epoch millis） */
        volatile long actionDeadline;
        /** 当前手开始时间戳 */
        long handStartTime;
        /** 当前手的编号（与 t_poker_room.hand_no 一致，用于落库） */
        int currentHandNo;
        /** 本手已执行的动作（落库前缓存，handId 待定） */
        final List<PokerHandAction> pendingActions = new ArrayList<>();

        TableSession(Long roomId, PokerTable table) {
            this.roomId = roomId;
            this.table = table;
        }
    }

    public void handleMessage(WebSocketSession session, Long userId, WsInbound msg) {
        String type = msg.getType();
        if (type == null) {
            return;
        }
        switch (type) {
            case WsTypes.HEARTBEAT -> sendTo(session, WsTypes.PONG, null);
            case WsTypes.JOIN -> onJoin(session, userId, msg);
            case WsTypes.SIT_DOWN -> onSitDown(session, userId, msg);
            case WsTypes.STAND_UP -> onStandUp(session, userId, msg);
            case WsTypes.START_GAME -> onStartGame(session, userId, msg);
            case WsTypes.ACTION -> onAction(session, userId, msg);
            case WsTypes.LEAVE -> sessionRegistry.unbindRoom(session);
            default -> log.warn("未知消息类型: {}", type);
        }
    }

    public void onDisconnect(Long userId) {
        // M4 断线重连处理：暂不解除座位
    }

    /**
     * 创建房间时注册运行时状态（由 PokerRoomService 调用）。
     */
    public void registerRoom(Long roomId, PokerTable table) {
        sessions.put(roomId, new TableSession(roomId, table));
    }

    /**
     * 解散房间：广播关闭并移除运行时状态。
     */
    public void closeRoom(Long roomId) {
        sessionRegistry.broadcast(roomId, WsOutbound.of(WsTypes.ROOM_CLOSED, null));
        sessions.remove(roomId);
    }

    /**
     * 重启服务后按需恢复运行时状态（预留）。
     */
    public void restoreRoom(Long roomId, PokerTable table) {
        sessions.putIfAbsent(roomId, new TableSession(roomId, table));
    }

    // ------------------------------------------------------------------ JOIN

    private void onJoin(WebSocketSession session, Long userId, WsInbound msg) {
        Long roomId = msg.getRoomId();
        if (roomId == null || !sessions.containsKey(roomId)) {
            sendError(session, roomId, "房间不存在");
            return;
        }
        sessionRegistry.bindRoom(session, roomId);
        pushSnapshot(roomId, userId);
    }

    // -------------------------------------------------------------- SIT_DOWN

    private void onSitDown(WebSocketSession session, Long userId, WsInbound msg) {
        Long roomId = msg.getRoomId();
        TableSession ts = getSession(roomId, session);
        if (ts == null) {
            return;
        }
        Integer seat = msg.getSeat();
        BigDecimal buyIn = msg.getBuyIn();
        if (seat == null || buyIn == null) {
            sendError(session, roomId, "参数不完整：需要 seat 和 buyIn");
            return;
        }
        ts.lock.lock();
        try {
            if (ts.running) {
                sendError(session, roomId, "对局进行中，暂不能入座");
                return;
            }
            if (ts.table.isOccupied(seat)) {
                sendError(session, roomId, ErrorCode.POKER_SEAT_TAKEN.getMessage());
                return;
            }
            PokerRoom room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POKER_ROOM_NOT_FOUND));
            if (buyIn.compareTo(room.getBuyInMin()) < 0 || buyIn.compareTo(room.getBuyInMax()) > 0) {
                sendError(session, roomId, ErrorCode.POKER_BUY_IN_INVALID.getMessage());
                return;
            }
            User user = userRepository.findById(userId).orElse(null);
            ts.table.sitDown(seat, userId, buyIn);

            PokerRoomPlayer p = new PokerRoomPlayer();
            p.setRoomId(roomId);
            p.setUserId(userId);
            p.setSeat(seat);
            p.setNickname(user != null ? user.getNickname() : "玩家" + userId);
            p.setAvatarUrl(user != null ? user.getAvatarUrl() : "");
            p.setChip(buyIn);
            playerRepository.save(p);

            syncPlayerCount(room);
            log.info("玩家入座: roomId={}, seat={}, userId={}, buyIn={}", roomId, seat, userId, buyIn);
        } finally {
            ts.lock.unlock();
        }
        broadcastSnapshot(roomId);
    }

    // -------------------------------------------------------------- STAND_UP

    private void onStandUp(WebSocketSession session, Long userId, WsInbound msg) {
        Long roomId = msg.getRoomId();
        TableSession ts = getSession(roomId, session);
        if (ts == null) {
            return;
        }
        ts.lock.lock();
        try {
            if (ts.running) {
                sendError(session, roomId, "对局进行中，暂不能离座");
                return;
            }
            boolean removed = ts.table.snapshot().stream()
                    .anyMatch(s -> s.playerId() == userId);
            if (!removed) {
                sendError(session, roomId, ErrorCode.POKER_NOT_IN_ROOM.getMessage());
                return;
            }
            int seat = ts.table.snapshot().stream()
                    .filter(s -> s.playerId() == userId)
                    .findFirst().map(PokerTable.Seat::seat).orElse(-1);
            ts.table.standUp(seat);

            playerRepository.findByRoomIdAndUserId(roomId, userId).ifPresent(p -> {
                p.setStatus("LEFT");
                p.setLeaveTime(LocalDateTime.now());
                playerRepository.save(p);
            });

            roomRepository.findById(roomId).ifPresent(this::syncPlayerCount);
            log.info("玩家离座: roomId={}, userId={}", roomId, userId);
        } finally {
            ts.lock.unlock();
        }
        broadcastSnapshot(roomId);
    }

    // -------------------------------------------------------------- START

    private void onStartGame(WebSocketSession session, Long userId, WsInbound msg) {
        Long roomId = msg.getRoomId();
        TableSession ts = getSession(roomId, session);
        if (ts == null) {
            return;
        }
        ts.lock.lock();
        try {
            PokerRoom room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POKER_ROOM_NOT_FOUND));
            if (!room.getOwnerId().equals(userId)) {
                sendError(session, roomId, ErrorCode.FORBIDDEN.getMessage());
                return;
            }
            if (ts.running) {
                sendError(session, roomId, ErrorCode.POKER_ROOM_STATUS_INVALID.getMessage());
                return;
            }
            if (!ts.table.canStart()) {
                sendError(session, roomId, "至少需要 2 名玩家才能开局");
                return;
            }
            ts.hand = ts.table.startHand(new Random());
            ts.lastHand = null;
            ts.pendingActions.clear();
            ts.handStartTime = System.currentTimeMillis();
            ts.running = true;
            refreshDeadline(ts, room);
            room.setStatus("PLAYING");
            room.setHandNo(room.getHandNo() + 1);
            ts.currentHandNo = room.getHandNo();
            roomRepository.save(room);
            log.info("开局: roomId={}, handNo={}", roomId, room.getHandNo());
        } finally {
            ts.lock.unlock();
        }
        broadcastSnapshot(roomId);
    }

    // -------------------------------------------------------------- ACTION

    private void onAction(WebSocketSession session, Long userId, WsInbound msg) {
        Long roomId = msg.getRoomId();
        TableSession ts = getSession(roomId, session);
        if (ts == null) {
            return;
        }
        String actionName = msg.getAction();
        BigDecimal amount = msg.getAmount();
        if (actionName == null) {
            sendError(session, roomId, "缺少动作");
            return;
        }
        ts.lock.lock();
        try {
            PokerHand hand = ts.hand;
            if (hand == null || !ts.running) {
                sendError(session, roomId, ErrorCode.POKER_GAME_NOT_STARTED.getMessage());
                return;
            }
            int seat = ts.table.snapshot().stream()
                    .filter(s -> s.playerId() == userId)
                    .findFirst().map(PokerTable.Seat::seat).orElse(-1);
            if (seat < 0) {
                sendError(session, roomId, ErrorCode.POKER_NOT_IN_ROOM.getMessage());
                return;
            }
            if (hand.currentActor() != seat) {
                sendError(session, roomId, ErrorCode.POKER_NOT_YOUR_TURN.getMessage());
                return;
            }
            PokerHand.Action action = PokerHand.Action.valueOf(actionName.toUpperCase());
            try {
                hand.act(seat, action, amount);
            } catch (IllegalStateException e) {
                sendError(session, roomId, e.getMessage());
                return;
            }
            recordAction(ts, seat, hand.phase().name(), action.name(), amount);

            if (hand.isFinished()) {
                finishHand(ts, roomId);
            } else {
                refreshDeadline(ts, roomRepository.findById(roomId).orElse(null));
            }
            log.info("动作: roomId={}, seat={}, action={}, amount={}", roomId, seat, action, amount);
        } finally {
            ts.lock.unlock();
        }
        broadcastSnapshot(roomId);
    }

    /**
     * 一手结束：结算回写、落库、同步 DB、自动开始下一手。
     */
    private void finishHand(TableSession ts, Long roomId) {
        List<PokerHand.PlayerRuntime> runtimes = ts.hand.playerStates();
        PokerHand.HandResult result = ts.hand.result();
        ts.table.applyResult(result);
        ts.lastHand = result;
        ts.hand = null;

        PokerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POKER_ROOM_NOT_FOUND));
        int handNo = ts.currentHandNo;
        persistHand(ts, room, handNo, result, runtimes);

        // 同步破产玩家（引擎已移除座位）
        syncBustedPlayers(roomId);
        syncPlayerCount(room);

        if (ts.table.activeCount() >= 2) {
            // 自动开始下一手
            room.setHandNo(room.getHandNo() + 1);
            ts.currentHandNo = room.getHandNo();
            roomRepository.save(room);
            ts.hand = ts.table.startHand(new Random());
            ts.pendingActions.clear();
            ts.handStartTime = System.currentTimeMillis();
            refreshDeadline(ts, room);
            log.info("下一手开始: roomId={}, handNo={}", roomId, room.getHandNo());
        } else {
            ts.running = false;
            room.setStatus("WAITING");
            roomRepository.save(room);
            log.info("对局结束，等待玩家: roomId={}", roomId);
        }
    }

    /**
     * 将一手牌及其玩家明细、动作流水写入数据库。
     */
    private void persistHand(TableSession ts, PokerRoom room, int handNo,
                             PokerHand.HandResult result, List<PokerHand.PlayerRuntime> runtimes) {
        try {
            PokerHandEntity entity = new PokerHandEntity();
            entity.setRoomId(room.getId());
            entity.setHandNo(handNo);
            entity.setDealerSeat(result.dealerSeat());
            entity.setSmallBlind(room.getSmallBlind());
            entity.setBigBlind(room.getBigBlind());
            entity.setBoardCards(result.board().stream()
                    .map(Card::toString).collect(Collectors.joining(",")));
            entity.setPot(result.pot());
            entity.setStartTime(LocalDateTime.ofEpochSecond(ts.handStartTime / 1000, 0,
                    java.time.ZoneOffset.ofHours(8)));
            entity.setEndTime(LocalDateTime.now());
            entity = handEntityRepository.save(entity);

            for (PokerHand.PlayerRuntime rt : runtimes) {
                PokerHandPlayer p = new PokerHandPlayer();
                p.setHandId(entity.getId());
                p.setUserId(rt.playerId());
                p.setSeat(rt.seat());
                p.setHoleCards(java.util.Arrays.stream(rt.holeCards())
                        .map(Card::toString).collect(Collectors.joining(",")));
                p.setStartingChip(rt.chip().add(rt.totalBet()));
                p.setBetTotal(rt.totalBet());
                HandValue hv = result.bestHands().get(rt.seat());
                if (hv != null) {
                    p.setHandRank(hv.rank().name());
                }
                boolean won = result.winners().contains(rt.seat());
                p.setResult(won ? "WIN" : (rt.folded() ? "FOLD" : "LOSE"));
                p.setWinAmount(result.chipDelta().getOrDefault(rt.seat(), BigDecimal.ZERO));
                handPlayerRepository.save(p);
            }

            for (PokerHandAction a : ts.pendingActions) {
                a.setHandId(entity.getId());
            }
            if (!ts.pendingActions.isEmpty()) {
                handActionRepository.saveAll(ts.pendingActions);
            }
            log.info("对局落库: roomId={}, handNo={}, players={}, actions={}",
                    room.getId(), handNo, runtimes.size(), ts.pendingActions.size());
        } catch (Exception e) {
            log.error("对局落库失败: roomId={}, handNo={}", room.getId(), handNo, e);
        }
    }

    private void recordAction(TableSession ts, Integer seat, String round, String action, BigDecimal amount) {
        PokerHandAction a = new PokerHandAction();
        a.setSeat(seat);
        a.setRound(round);
        a.setAction(action);
        a.setAmount(amount == null ? BigDecimal.ZERO : amount);
        ts.pendingActions.add(a);
    }

    /**
     * 刷新当前行动者的思考超时截止时间。
     */
    private void refreshDeadline(TableSession ts, PokerRoom room) {
        int think = room != null && room.getThinkSeconds() != null ? room.getThinkSeconds() : 20;
        ts.actionDeadline = System.currentTimeMillis() + think * 1000L;
    }

    /**
     * 每 1 秒扫描一次：超时未行动的玩家自动过牌/弃牌。
     */
    @Scheduled(fixedRate = 1000)
    public void scanTimeouts() {
        long now = System.currentTimeMillis();
        for (TableSession ts : sessions.values()) {
            ts.lock.lock();
            try {
                PokerHand hand = ts.hand;
                if (hand == null || hand.isFinished() || ts.actionDeadline <= 0) {
                    continue;
                }
                int actor = hand.currentActor();
                if (actor < 0 || now < ts.actionDeadline) {
                    continue;
                }
                PokerRoom room = roomRepository.findById(ts.roomId).orElse(null);
                if (room == null) {
                    continue;
                }
                BigDecimal call = hand.callAmount();
                boolean shouldFold = call.signum() > 0;
                try {
                    hand.act(actor, shouldFold ? PokerHand.Action.FOLD : PokerHand.Action.CHECK, null);
                } catch (IllegalStateException e) {
                    log.warn("超时自动动作失败 roomId={}, seat={}", ts.roomId, actor, e);
                    continue;
                }
                recordAction(ts, actor, hand.phase().name(),
                        shouldFold ? "FOLD" : "CHECK", BigDecimal.ZERO);
                log.info("超时自动动作: roomId={}, seat={}, action={}", ts.roomId, actor, shouldFold ? "FOLD" : "CHECK");

                if (hand.isFinished()) {
                    finishHand(ts, ts.roomId);
                } else {
                    refreshDeadline(ts, room);
                }
                broadcastSnapshot(ts.roomId);
            } finally {
                ts.lock.unlock();
            }
        }
    }

    private void syncBustedPlayers(Long roomId) {
        TableSession ts = sessions.get(roomId);
        java.util.Set<Long> alive = ts != null
                ? ts.table.snapshot().stream()
                        .map(PokerTable.Seat::playerId).collect(Collectors.toSet())
                : java.util.Set.of();
        for (PokerRoomPlayer p : playerRepository.findByRoomId(roomId)) {
            if (!alive.contains(p.getUserId())) {
                p.setStatus("LEFT");
                p.setLeaveTime(LocalDateTime.now());
                playerRepository.save(p);
            }
        }
    }

    private void syncPlayerCount(PokerRoom room) {
        long count = playerRepository.findByRoomId(room.getId()).stream()
                .filter(p -> "SEATED".equals(p.getStatus()))
                .count();
        room.setCurrentPlayers((int) count);
        roomRepository.save(room);
    }

    // ------------------------------------------------------------- snapshot

    private void broadcastSnapshot(Long roomId) {
        sessionRegistry.broadcastSnapshot(roomId, userId -> buildSnapshot(roomId, userId));
    }

    private void pushSnapshot(Long roomId, Long userId) {
        sessionRegistry.sendToUser(roomId, userId,
                WsOutbound.of(WsTypes.TABLE_SNAPSHOT, buildSnapshot(roomId, userId)));
    }

    private PokerTableSnapshot buildSnapshot(Long roomId, Long viewerId) {
        TableSession ts = sessions.get(roomId);
        if (ts == null) {
            return null;
        }
        PokerRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POKER_ROOM_NOT_FOUND));

        Map<Integer, PokerRoomPlayer> dbPlayers = playerRepository.findByRoomId(roomId).stream()
                .filter(p -> "SEATED".equals(p.getStatus()))
                .collect(Collectors.toMap(PokerRoomPlayer::getSeat, p -> p, (a, b) -> a));

        List<PokerTableSnapshot.PlayerView> players = new ArrayList<>();
        for (PokerTable.Seat s : ts.table.snapshot().stream()
                .sorted(Comparator.comparingInt(PokerTable.Seat::seat)).toList()) {
            PokerRoomPlayer db = dbPlayers.get(s.seat());
            PokerTableSnapshot.PlayerView.PlayerViewBuilder b = PokerTableSnapshot.PlayerView.builder()
                    .seat(s.seat())
                    .userId(s.playerId())
                    .chip(s.chip())
                    .nickname(db != null ? db.getNickname() : "玩家" + s.playerId())
                    .avatarUrl(db != null ? db.getAvatarUrl() : "")
                    .isDealer(ts.hand != null && ts.hand.dealerSeat() == s.seat());
            if (ts.hand != null) {
                PokerHand.PlayerRuntime rt = ts.hand.playerStates().stream()
                        .filter(r -> r.seat() == s.seat()).findFirst().orElse(null);
                if (rt != null) {
                    b.roundBet(rt.roundBet()).totalBet(rt.totalBet())
                            .folded(rt.folded()).allIn(rt.allIn());
                    if (s.playerId() == viewerId) {
                        b.holeCards(List.of(rt.holeCards()[0].toString(), rt.holeCards()[1].toString()));
                    }
                }
            }
            players.add(b.build());
        }

        Integer mySeat = ts.table.snapshot().stream()
                .filter(s -> s.playerId() == viewerId)
                .map(PokerTable.Seat::seat).findFirst().orElse(null);

        Integer dealerSeat = null;
        if (ts.hand != null) {
            dealerSeat = ts.hand.dealerSeat();
        } else if (room.getHandNo() > 0) {
            dealerSeat = ts.table.currentDealerSeat();
        }

        PokerTableSnapshot.PokerTableSnapshotBuilder snapshot = PokerTableSnapshot.builder()
                .roomId(roomId)
                .roomNo(room.getRoomNo())
                .name(room.getName())
                .tableSize(room.getTableSize())
                .smallBlind(room.getSmallBlind())
                .bigBlind(room.getBigBlind())
                .status(ts.running ? "PLAYING" : "WAITING")
                .handNo(room.getHandNo())
                .dealerSeat(dealerSeat)
                .phase(ts.hand != null ? ts.hand.phase().name() : "WAITING")
                .board(ts.hand != null ? ts.hand.board().stream().map(Card::toString).toList() : List.of())
                .pot(ts.hand != null ? ts.hand.pot() : BigDecimal.ZERO)
                .currentBet(ts.hand != null ? ts.hand.callAmount() : BigDecimal.ZERO)
                .minRaise(ts.hand != null ? ts.hand.minRaise() : BigDecimal.ZERO)
                .currentActor(ts.hand != null ? ts.hand.currentActor() : null)
                .callAmount(ts.hand != null ? ts.hand.callAmount() : BigDecimal.ZERO)
                .actionDeadline(ts.hand != null && ts.running ? ts.actionDeadline : null)
                .isOwner(room.getOwnerId().equals(viewerId))
                .mySeat(mySeat)
                .players(players)
                .lastHand(toResultView(ts.lastHand));

        return snapshot.build();
    }

    private PokerTableSnapshot.HandResultView toResultView(PokerHand.HandResult r) {
        if (r == null) {
            return null;
        }
        Map<Integer, List<String>> shown = new HashMap<>();
        Map<Integer, String> best = new HashMap<>();
        r.revealedHoleCards().forEach((seat, cards) -> {
            List<Card> hole = List.of(cards);
            if (!r.board().isEmpty()) {
                // 摊牌：显示组成最大牌型的最佳 5 张（底牌 + 公共牌 7 选 5）
                List<Card> all = new ArrayList<>(hole);
                all.addAll(r.board());
                HandEvaluator.EvaluatedHand eh = HandEvaluator.evaluateWithBestFive(all);
                shown.put(seat, eh.bestFive().stream().map(Card::toString).toList());
                best.put(seat, eh.value().rank().name());
            } else {
                // 提前结束（未发公共牌）：仅显示 2 张底牌，按点数降序
                List<Card> sorted = hole.stream()
                        .sorted((x, y) -> Integer.compare(y.rank(), x.rank()))
                        .toList();
                shown.put(seat, sorted.stream().map(Card::toString).toList());
            }
        });
        return PokerTableSnapshot.HandResultView.builder()
                .pot(r.pot())
                .winners(r.winners())
                .chipDelta(r.chipDelta())
                .revealedHoleCards(shown)
                .bestHands(best)
                .build();
    }

    // ------------------------------------------------------------------ util

    private TableSession getSession(Long roomId, WebSocketSession session) {
        if (roomId == null || !sessions.containsKey(roomId)) {
            sendError(session, roomId, ErrorCode.POKER_ROOM_NOT_FOUND.getMessage());
            return null;
        }
        return sessions.get(roomId);
    }

    private void sendError(WebSocketSession session, Long roomId, String message) {
        sendTo(session, WsTypes.ERROR, Map.of("message", message));
    }

    private void sendTo(WebSocketSession session, String type, Object data) {
        try {
            session.sendMessage(new org.springframework.web.socket.TextMessage(
                    WsJson.toJson(WsOutbound.of(type, data))));
        } catch (Exception e) {
            log.warn("发送消息失败", e);
        }
    }
}
