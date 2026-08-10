package com.gamegroup.poker.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 单手德州扑克状态机（纯 Java）。
 * <p>
 * 生命周期：构造（抽盲 + 发底牌）→ 由外部按轮次调用 {@link #act} →
 * 每圈下注结束后自动推进阶段（翻牌/转牌/河牌）→ 摊牌或唯一存活者结算 → {@link #result()}。
 * <p>
 * 动作合法性由服务端强制校验，非法动作抛出 {@link IllegalStateException}。
 */
public final class PokerHand {

    public enum Action { FOLD, CHECK, CALL, BET, RAISE, ALL_IN }

    public enum Phase { PREFLOP, FLOP, TURN, RIVER, FINISHED }

    /** 玩家本手状态。 */
    static final class PlayerState {
        final int seat;
        final long playerId;
        BigDecimal chip;
        BigDecimal roundBet = BigDecimal.ZERO;
        BigDecimal totalBet = BigDecimal.ZERO;
        final Card[] hole = new Card[2];
        boolean folded;
        boolean allIn;

        PlayerState(int seat, long playerId, BigDecimal chip) {
            this.seat = seat;
            this.playerId = playerId;
            this.chip = chip;
        }
    }

    /** 一手牌结算结果。 */
    public record HandResult(
            int dealerSeat,
            List<Card> board,
            BigDecimal pot,
            List<Integer> winners,
            Map<Integer, BigDecimal> chipDelta,
            Map<Integer, Card[]> revealedHoleCards,
            Map<Integer, HandValue> bestHands) {
    }

    private final PokerTable table;
    private final PlayerState[] state;
    private final Deck deck;
    private final int dealerSeat;
    private final BigDecimal smallBlind;
    private final BigDecimal bigBlind;

    private final List<Card> board = new ArrayList<>(5);
    private Phase phase;
    private boolean finished;

    private int bbSeat = -1;
    private BigDecimal currentBet = BigDecimal.ZERO;
    private BigDecimal minRaiseSize;
    private final Set<Integer> toAct = new LinkedHashSet<>();
    private int lastActedSeat = -1;

    private HandResult result;

    public PokerHand(PokerTable table, Random random) {
        this.table = table;
        this.smallBlind = table.smallBlind();
        this.bigBlind = table.bigBlind();
        this.minRaiseSize = this.bigBlind;
        this.dealerSeat = table.currentDealerSeat();
        this.state = new PlayerState[table.tableSize()];
        for (PokerTable.Seat s : table.snapshot()) {
            state[s.seat()] = new PlayerState(s.seat(), s.playerId(), s.chip());
        }

        this.deck = new Deck();
        this.deck.shuffle(random);

        dealHoleCards();
        postBlinds();
        startPreflop();
    }

    public Phase phase() {
        return phase;
    }

    public boolean isFinished() {
        return finished;
    }

    public List<Card> board() {
        return List.copyOf(board);
    }

    public Card[] holeCards(int seat) {
        return state[seat] == null ? null : state[seat].hole.clone();
    }

    /** 玩家本手运行时状态（用于构建牌桌快照）。 */
    public record PlayerRuntime(int seat, long playerId, BigDecimal chip, BigDecimal roundBet,
                                BigDecimal totalBet, boolean folded, boolean allIn, Card[] holeCards) {
    }

    public List<PlayerRuntime> playerStates() {
        List<PlayerRuntime> list = new ArrayList<>();
        for (PlayerState p : state) {
            if (p != null) {
                list.add(new PlayerRuntime(p.seat, p.playerId, p.chip, p.roundBet,
                        p.totalBet, p.folded, p.allIn, p.hole.clone()));
            }
        }
        return list;
    }

    public int dealerSeat() {
        return dealerSeat;
    }

    public BigDecimal pot() {
        BigDecimal sum = BigDecimal.ZERO;
        for (PlayerState p : state) {
            if (p != null) {
                sum = sum.add(p.totalBet);
            }
        }
        return sum;
    }

    /**
     * 当前需要行动的座位；-1 表示无需行动（等待发牌/已结束）。
     */
    public int currentActor() {
        if (finished) {
            return -1;
        }
        for (int i = 1; i <= table.tableSize(); i++) {
            int s = ((lastActedSeat + i) % table.tableSize() + table.tableSize()) % table.tableSize();
            if (toAct.contains(s)) {
                return s;
            }
        }
        return -1;
    }

    /**
     * 当前本圈跟注额（需补齐到该数额；0 表示可过牌）。
     */
    public BigDecimal callAmount() {
        if (finished) {
            return BigDecimal.ZERO;
        }
        int actor = currentActor();
        if (actor < 0) {
            return BigDecimal.ZERO;
        }
        return currentBet.subtract(state[actor].roundBet).max(BigDecimal.ZERO);
    }

    public BigDecimal minRaise() {
        return currentBet.add(minRaiseSize);
    }

    /**
     * 执行动作。非法动作抛出异常。
     *
     * @param amount BET/RAISE 时表示"本圈投入总额"，其余动作传 null
     */
    public void act(int seat, Action action, BigDecimal amount) {
        if (finished) {
            throw new IllegalStateException("本手已结束");
        }
        if (seat < 0 || seat >= table.tableSize() || state[seat] == null) {
            throw new IllegalStateException("无效座位: " + seat);
        }
        if (currentActor() != seat) {
            throw new IllegalStateException("未轮到你操作");
        }
        PlayerState p = state[seat];
        if (p.folded || p.allIn) {
            throw new IllegalStateException("该玩家本手已不可操作");
        }

        switch (action) {
            case FOLD -> {
                p.folded = true;
                toAct.remove(seat);
            }
            case CHECK -> {
                if (p.roundBet.compareTo(currentBet) < 0) {
                    throw new IllegalStateException("存在跟注额，不能过牌");
                }
                toAct.remove(seat);
            }
            case CALL -> {
                if (p.roundBet.compareTo(currentBet) >= 0) {
                    throw new IllegalStateException("无注可跟");
                }
                payIntoPot(seat, currentBet.subtract(p.roundBet));
                toAct.remove(seat);
            }
            case BET -> {
                if (currentBet.signum() > 0) {
                    throw new IllegalStateException("本圈已有人下注，请加注");
                }
                requireAmount(amount, bigBlind);
                BigDecimal paid = payIntoPot(seat, amount.subtract(p.roundBet));
                currentBet = paid;
                minRaiseSize = paid;
                toAct.remove(seat);
                requeue(seat);
            }
            case RAISE -> {
                if (currentBet.signum() <= 0) {
                    throw new IllegalStateException("本圈无人下注，不能加注");
                }
                if (amount.compareTo(currentBet) <= 0) {
                    throw new IllegalStateException("加注额必须超过当前注");
                }
                // all-in（投入超过剩余筹码）时可低于最小加注
                boolean allInRaise = amount.compareTo(p.chip.add(p.roundBet)) >= 0;
                if (!allInRaise && amount.compareTo(currentBet.add(minRaiseSize)) < 0) {
                    throw new IllegalStateException("加注额不足最小加注");
                }
                BigDecimal paid = payIntoPot(seat, amount.subtract(p.roundBet));
                BigDecimal newBet = p.roundBet;
                if (newBet.compareTo(currentBet) > 0) {
                    if (newBet.compareTo(currentBet.add(minRaiseSize)) >= 0) {
                        minRaiseSize = newBet.subtract(currentBet);
                    }
                    currentBet = newBet;
                    toAct.remove(seat);
                    requeue(seat);
                } else {
                    toAct.remove(seat);
                }
            }
            case ALL_IN -> {
                BigDecimal newBet = p.roundBet.add(p.chip);
                payIntoPot(seat, p.chip);
                toAct.remove(seat);
                if (newBet.compareTo(currentBet) > 0) {
                    if (newBet.compareTo(currentBet.add(minRaiseSize)) >= 0) {
                        minRaiseSize = newBet.subtract(currentBet);
                    }
                    currentBet = newBet;
                    requeue(seat);
                }
            }
        }
        lastActedSeat = seat;
        advanceIfRoundOver();
    }

    /**
     * 结算结果；未结束时返回 null。
     */
    public HandResult result() {
        return result;
    }

    private void dealHoleCards() {
        List<Integer> seats = occupiedSeats();
        for (int r = 0; r < 2; r++) {
            for (int s : seats) {
                state[s].hole[r] = deck.deal();
            }
        }
    }

    private void postBlinds() {
        int sb;
        if (occupiedSeats().size() == 2) {
            // heads-up：按钮即小盲
            sb = dealerSeat;
            bbSeat = nextAliveSeatFrom(dealerSeat);
        } else {
            sb = nextAliveSeatFrom(dealerSeat);
            bbSeat = nextAliveSeatFrom(sb);
        }
        payIntoPot(sb, smallBlind);
        payIntoPot(bbSeat, bigBlind);
    }

    private void startPreflop() {
        phase = Phase.PREFLOP;
        currentBet = bigBlind;
        // preflop 首个行动者：大盲下一位（多人局 UTG；heads-up 即按钮/小盲）
        int start;
        if (bbSeat < 0 || state[bbSeat].allIn) {
            start = nextAliveSeatFrom(dealerSeat);
        } else {
            start = nextAliveSeatFrom(bbSeat);
        }
        initActionQueue(start);

        // 若所有人都已 all-in（无行动者），直接推进摊牌
        if (toAct.isEmpty()) {
            advanceIfRoundOver();
        }
    }

    /**
     * 构建本轮行动队列：从 start 顺时针遍历，非弃牌且非 all-in 者入队。
     */
    private void initActionQueue(int start) {
        toAct.clear();
        int s = start;
        do {
            if (state[s] != null && !state[s].folded && !state[s].allIn) {
                toAct.add(s);
            }
            s = nextAliveSeatFrom(s);
        } while (s != start);
        lastActedSeat = prevAliveSeat(start);
    }

    private void advanceIfRoundOver() {
        if (!toAct.isEmpty()) {
            return;
        }
        List<Integer> alive = aliveSeats();
        if (alive.size() == 1) {
            finishSingleWinner(alive.get(0));
            return;
        }
        if (phase == Phase.RIVER) {
            finishShowdown();
            return;
        }
        advanceStage();
    }

    private void advanceStage() {
        phase = switch (phase) {
            case PREFLOP -> Phase.FLOP;
            case FLOP -> Phase.TURN;
            case TURN -> Phase.RIVER;
            default -> throw new IllegalStateException("无法推进阶段: " + phase);
        };
        if (phase == Phase.FLOP) {
            for (int i = 0; i < 3; i++) {
                board.add(deck.deal());
            }
        } else {
            board.add(deck.deal());
        }
        currentBet = BigDecimal.ZERO;
        minRaiseSize = bigBlind;
        for (PlayerState p : state) {
            if (p != null) {
                p.roundBet = BigDecimal.ZERO;
            }
        }
        initActionQueue(nextAliveSeatFrom(dealerSeat));
    }

    private void finishSingleWinner(int winner) {
        finished = true;
        phase = Phase.FINISHED;
        BigDecimal total = pot();
        Map<Integer, BigDecimal> delta = new HashMap<>();
        for (PlayerState p : state) {
            if (p != null) {
                delta.put(p.seat, p.totalBet.negate());
            }
        }
        delta.put(winner, total.subtract(state[winner].totalBet));

        Map<Integer, HandValue> best = new HashMap<>();
        if (!board.isEmpty()) {
            // 提前结束无公共牌，不评估牌型
            List<Card> cards = new ArrayList<>(List.of(state[winner].hole));
            cards.addAll(board);
            best.put(winner, HandEvaluator.evaluate(cards));
        }

        result = new HandResult(dealerSeat, List.copyOf(board), total, List.of(winner),
                delta, Map.of(winner, state[winner].hole.clone()), best);
    }

    private void finishShowdown() {
        finished = true;
        phase = Phase.FINISHED;
        List<PotCalculator.Contributor> contributors = new ArrayList<>();
        Map<Integer, HandValue> best = new HashMap<>();
        Map<Integer, Card[]> revealed = new HashMap<>();

        for (PlayerState p : state) {
            if (p == null) {
                continue;
            }
            boolean inHand = !p.folded;
            HandValue hv = null;
            if (inHand) {
                List<Card> cards = new ArrayList<>(List.of(p.hole));
                cards.addAll(board);
                hv = HandEvaluator.evaluate(cards);
                best.put(p.seat, hv);
                revealed.put(p.seat, p.hole.clone());
            }
            contributors.add(new PotCalculator.Contributor(p.seat, p.totalBet, inHand, hv));
        }

        Map<Integer, BigDecimal> payouts = PotCalculator.calculate(contributors);
        BigDecimal total = pot();
        Map<Integer, BigDecimal> delta = new HashMap<>();
        for (PlayerState p : state) {
            if (p != null) {
                delta.put(p.seat, payouts.getOrDefault(p.seat, BigDecimal.ZERO).subtract(p.totalBet));
            }
        }
        List<Integer> winners = contributors.stream()
                .filter(PotCalculator.Contributor::inHand)
                .filter(c -> payouts.getOrDefault(c.seat(), BigDecimal.ZERO).signum() > 0)
                .map(PotCalculator.Contributor::seat)
                .sorted()
                .toList();

        result = new HandResult(dealerSeat, List.copyOf(board), total, winners, delta, revealed, best);
    }

    /**
     * 投入金额：扣除筹码并计入本圈/全程投入；筹码归零视为 all-in。
     */
    private BigDecimal payIntoPot(int seat, BigDecimal amount) {
        PlayerState p = state[seat];
        BigDecimal pay = amount.min(p.chip).max(BigDecimal.ZERO);
        p.chip = p.chip.subtract(pay);
        p.roundBet = p.roundBet.add(pay);
        p.totalBet = p.totalBet.add(pay);
        if (p.chip.signum() == 0) {
            p.allIn = true;
        }
        return pay;
    }

    /**
     * 有人加注后，其余未弃牌且未 all-in 的玩家重新入队。
     */
    private void requeue(int aggressor) {
        for (PlayerState p : state) {
            if (p != null && p.seat != aggressor && !p.folded && !p.allIn) {
                toAct.add(p.seat);
            }
        }
    }

    private void requireAmount(BigDecimal amount, BigDecimal min) {
        if (amount == null || amount.compareTo(min) < 0) {
            throw new IllegalStateException("下注金额不足");
        }
    }

    private List<Integer> occupiedSeats() {
        List<Integer> list = new ArrayList<>();
        for (PlayerState p : state) {
            if (p != null) {
                list.add(p.seat);
            }
        }
        return list;
    }

    private List<Integer> aliveSeats() {
        List<Integer> list = new ArrayList<>();
        for (PlayerState p : state) {
            if (p != null && !p.folded) {
                list.add(p.seat);
            }
        }
        return list;
    }

    /**
     * 顺时针找下一个未弃牌座位。
     */
    private int nextAliveSeatFrom(int seat) {
        for (int i = 1; i <= table.tableSize(); i++) {
            int s = (seat + i) % table.tableSize();
            if (state[s] != null && !state[s].folded) {
                return s;
            }
        }
        throw new IllegalStateException("无存活玩家");
    }

    /**
     * 逆时针找上一个未弃牌座位。
     */
    private int prevAliveSeat(int seat) {
        for (int i = 1; i <= table.tableSize(); i++) {
            int s = (seat - i + table.tableSize()) % table.tableSize();
            if (state[s] != null && !state[s].folded) {
                return s;
            }
        }
        throw new IllegalStateException("无存活玩家");
    }
}
