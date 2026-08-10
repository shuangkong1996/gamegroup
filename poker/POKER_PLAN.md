# Poker 德州扑克网页游戏 · 项目规划

> 项目定位：以**娱乐为主**的在线德州扑克网页游戏，不涉及真实货币/筹码。
> 后端复用 `game-server`，前端以 **Vue 3** 为基础框架，独立目录 `poker/` 开发。

---

## 1. 项目概述

| 维度 | 说明 |
| --- | --- |
| 产品定位 | 娱乐向、好友房模式的德州扑克游戏，虚拟筹码，无真实金钱交易 |
| 核心玩法 | 6/9 人桌，标准德州扑克规则：盲注、下注圈、比牌、奖池分配（含 all-in 边池） |
| 实时性要求 | 高。下注、发牌、比牌等需实时同步 |
| 并发规模 | 低。娱乐向，单房 9 人，同时在线房数量有限（MVP 阶段可单机部署） |

### 1.1 目标

1. 上线一个可玩的德州扑克 Web 游戏（MVP 可玩性优先）。
2. 完整复用 `game-server` 的**用户体系（JWT）**、**统一响应/错误码**、**Redis**、**MySQL** 与部署方式。
3. 前端基于 Vue 3，代码组织与现有 `tools-web` 保持一致风格。
4. 游戏主体逻辑（发牌、下注、比牌）全部放在后端，前端只做渲染与交互，杜绝前端作弊（服务端可信）。

---

## 2. 技术选型

### 2.1 后端（复用 game-server）

| 技术 | 现状 | 用途/改造 |
| --- | --- | --- |
| Spring Boot 3.2 + Java 17 | 已有 | 保持 |
| Spring Web MVC | 已有 | REST 接口（房间管理、历史查询） |
| JPA + MySQL | 已有 | 持久化房间、对局历史、战绩统计 |
| Redis | 已有依赖，暂无业务使用 | **新增核心用途**：存放游戏进行中的实时状态（房间、手牌、筹码） |
| JWT 认证 | 已有 `AuthInterceptor` | 保持，WebSocket 握手单独校验 token |
| WebSocket | **未引入** | **新增 `spring-boot-starter-websocket`**，承载实时游戏消息 |

> 决策说明（KISS）：
> - 实时通信选 **原生 WebSocket + 简单 JSON 信封协议**，不引入 STOMP。消息类型少、协议简单，娱乐游戏足够，且前端不需要额外 STOMP 库。
> - 对局中的状态放 **Redis**（天然过期、跨实例共享、避免 DB 高频读写），对局结束再落 MySQL 用于历史与统计。这正是"游戏用 Redis 做状态、DB 做持久化"的职责分离（SRP）。

### 2.2 前端（新增 poker 项目）

| 技术 | 说明 |
| --- | --- |
| Vue 3 + TypeScript | 基础框架，与 `tools-web` 一致 |
| Vite | 构建 |
| Pinia | 全局状态（用户、房间、游戏桌快照） |
| vue-router | 页面路由 |
| axios | REST 请求（封装统一拦截器，复用 JWT） |
| TailwindCSS | 样式，与 `tools-web` 一致 |
| 原生 WebSocket | 游戏实时通信，封装为单例服务 |

> 不引入重型 3D/动画库（YAGNI），桌面牌桌用 CSS 布局 + 简单过渡动画即可。

---

## 3. 整体架构

```
┌───────────────────────────┐        ┌────────────────────────────────────┐
│      poker (Vue 3 Web)    │        │            game-server              │
│                           │  REST  │                                    │
│  ┌─────────┐  ┌────────┐ │───────▶│  AuthController / PokerController   │
│  │ 大厅页面 │  │ 游戏桌  │ │        │  ┌──────────────────────────────┐  │
│  └─────────┘  └────────┘ │        │  │ PokerGameService (核心状态机) │  │
│                           │  WS    │  │  - 发牌/下注/比牌/结算         │  │
│  ┌──────────────┐         │◀──────▶│  └──────────────────────────────┘  │
│  │ PokerSocket  │         │        │  ┌──────────┐  ┌──────────────┐   │
│  │  (单例封装)   │         │        │  │  Redis   │  │  MySQL (JPA) │   │
│  └──────────────┘         │        │  │ 实时状态  │  │ 历史/统计     │   │
└───────────────────────────┘        └──────────────┴──────────────┘   │
```

---

## 4. 后端规划

### 4.1 现有体系复用点

| 复用项 | 说明 |
| --- | --- |
| `AuthInterceptor` + JWT | REST 接口认证保持不变，`@RequestAttribute("userId")` 获取用户 |
| `ApiResponse` / `ErrorCode` | 统一响应；在 `ErrorCode` 中新增 `9xxx` 段 Poker 错误码 |
| `t_user` | 用户即扑克玩家，不新建用户表 |
| `MatchRoom` 房间思路 | 借鉴"验证码/房号加入"体验，但**不直接复用** `t_match_room`（其设计面向双人匹配，无法承载盲注/座位/筹码），新增独立表 |
| 配置方式 / docker-compose | 复用 MySQL、Redis、Nginx 等部署 |

### 4.2 需要新增/改造的 Maven 依赖

```xml
<!-- pom.xml 新增 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 4.3 新增包结构（com.gamegroup 下新增 poker 子包）

```
com.gamegroup.poker
├── controller/           PokerRoomController、PokerStatsController（REST）
├── service/              PokerRoomService（房间/入座）、PokerGameService（核心状态机）
├── engine/               （纯 Java 游戏引擎，不依赖 Spring）
│   ├── Card.java                # 单张牌
│   ├── Deck.java                # 洗牌/发牌
│   ├── HandEvaluator.java       # 牌型评估（皇家同花顺→高牌，7选5）
│   ├── HandRank.java            # 牌型枚举 + 比较
│   ├── PokerRoom.java           # 房间运行时模型（纯 POJO，可序列化到 Redis）
│   ├── PokerHand.java           # 单手牌运行时模型
│   └── PotCalculator.java       # 奖池分配（含 all-in 边池）
├── ws/
│   ├── PokerWebSocketConfig.java    # 注册 /ws/poker
│   ├── PokerHandshakeInterceptor.java # 握手时校验 JWT
│   ├── PokerWebSocketHandler.java   # 消息收发路由
│   └── SessionRegistry.java         # userId -> WebSocketSession 映射
├── entity/               PokerRoom、PokerRoomPlayer、PokerHand、PokerHandPlayer、PokerHandAction、PokerWallet
├── repository/           （对应 Repository）
└── dto/                  房间 DTO、游戏桌快照 DTO、WS 入站/出站消息
```

### 4.4 数据库表设计（DDL）

命名遵循项目规范：前缀 `t_`、小写+下划线。**表结构对应"持久化"数据；进行中的实时状态以 Redis 为准**。

#### 4.4.1 `t_poker_room` 房间表

```sql
CREATE TABLE t_poker_room (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_no         VARCHAR(8)  NOT NULL UNIQUE COMMENT '6位房号，用于加入',
    name            VARCHAR(50) NOT NULL COMMENT '房间名',
    owner_id        BIGINT      NOT NULL COMMENT '房主 userId',
    table_size      TINYINT     NOT NULL DEFAULT 6 COMMENT '座位数：6 或 9',
    small_blind     DECIMAL(10,2) NOT NULL DEFAULT 1.00 COMMENT '小盲',
    big_blind       DECIMAL(10,2) NOT NULL DEFAULT 2.00 COMMENT '大盲',
    ante            DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '底注（可选）',
    buy_in_min      DECIMAL(12,2) NOT NULL DEFAULT 100.00 COMMENT '最小买入',
    buy_in_max      DECIMAL(12,2) NOT NULL DEFAULT 500.00 COMMENT '最大买入',
    think_seconds   INT         NOT NULL DEFAULT 20 COMMENT '单玩家操作倒计时（秒）',
    status          VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/PLAYING/ENDED',
    hand_no         INT         NOT NULL DEFAULT 0 COMMENT '已进行手牌数',
    current_players INT         NOT NULL DEFAULT 0 COMMENT '当前入座人数',
    create_time     DATETIME    NOT NULL,
    update_time     DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='德州扑克房间';
```

#### 4.4.2 `t_poker_room_player` 房间玩家表

```sql
CREATE TABLE t_poker_room_player (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id     BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    seat        INT         NOT NULL COMMENT '座位号 0~8',
    nickname    VARCHAR(64) COMMENT '快照，防用户改名影响历史',
    avatar_url  VARCHAR(512),
    chip        DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '当前筹码（实时值以 Redis 为准，此为入座记录）',
    status      VARCHAR(20) NOT NULL DEFAULT 'SEATED' COMMENT 'SEATED/BUSTED/LEFT',
    join_time   DATETIME    NOT NULL,
    leave_time  DATETIME,
    UNIQUE KEY uk_room_seat (room_id, seat),
    KEY idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间入座记录';
```

#### 4.4.3 `t_poker_hand` 每手牌记录表

```sql
CREATE TABLE t_poker_hand (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id         BIGINT      NOT NULL,
    hand_no         INT         NOT NULL COMMENT '第几手',
    dealer_seat     INT         NOT NULL COMMENT '庄家座位',
    small_blind     DECIMAL(10,2) NOT NULL,
    big_blind       DECIMAL(10,2) NOT NULL,
    board_cards     VARCHAR(40) COMMENT '公共牌，如 "AS,KD,2H,9C,TH"',
    pot             DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '总奖池',
    start_time      DATETIME    NOT NULL,
    end_time        DATETIME,
    KEY idx_room_no (room_id, hand_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每手牌记录';
```

#### 4.4.4 `t_poker_hand_player` 每手玩家明细表

```sql
CREATE TABLE t_poker_hand_player (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    hand_id         BIGINT      NOT NULL,
    room_player_id  BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    seat            INT         NOT NULL,
    hole_cards      VARCHAR(20) COMMENT '底牌，如 "AS,KD"（仅服务端存，历史回放用）',
    starting_chip   DECIMAL(12,2) NOT NULL,
    bet_total       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '本手总投入',
    hand_rank       VARCHAR(40) COMMENT '最终牌型，如 "TWO_PAIR"',
    result          VARCHAR(20) COMMENT 'FOLD/WIN/LOSE',
    win_amount      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '本手赢取',
    KEY idx_hand (hand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每手玩家明细';
```

#### 4.4.5 `t_poker_hand_action` 玩家行为流水表（对局审计/回放）

```sql
CREATE TABLE t_poker_hand_action (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    hand_id     BIGINT      NOT NULL,
    seat        INT         NOT NULL,
    round       VARCHAR(10) NOT NULL COMMENT 'PREFLOP/FLOP/TURN/RIVER',
    action      VARCHAR(20) NOT NULL COMMENT 'FOLD/CHECK/CALL/BET/RAISE/ALL_IN',
    amount      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '涉及金额',
    create_time DATETIME    NOT NULL,
    KEY idx_hand (hand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家行为流水';
```

#### 4.4.6 `t_poker_wallet` 虚拟筹码钱包表（可选，M2）

> 娱乐为主，MVP 可不做钱包——每次入座由服务端发放虚拟起始筹码。若要做"累计金币/签到奖励"，加此表。

```sql
CREATE TABLE t_poker_wallet (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL UNIQUE,
    chip         DECIMAL(14,2) NOT NULL DEFAULT 1000 COMMENT '虚拟筹码余额',
    total_earn   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '累计赢取',
    total_lose   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '累计输掉',
    update_time  DATETIME     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟筹码钱包';
```

#### 表间关系

```
t_poker_room 1 ── n t_poker_room_player  (房间 ↔ 玩家入座)
t_poker_room 1 ── n t_poker_hand         (房间 ↔ 手牌)
t_poker_hand 1 ── n t_poker_hand_player  (手牌 ↔ 玩家明细)
t_poker_hand 1 ── n t_poker_hand_action  (手牌 ↔ 行为流水)
```

### 4.5 Redis Key 设计（实时状态）

| Key | 类型 | 说明 |
| --- | --- | --- |
| `poker:room:{roomId}` | HASH | 房间元数据快照（盲注、状态、当前手牌阶段） |
| `poker:room:{roomId}:state` | STRING(JSON) | **整个牌桌运行时状态快照**（座位、筹码、底牌、下注圈、按钮位置等），单 key 读写，序列化为 JSON |
| `poker:room:{roomId}:lock` | SETNX 锁 | 房间操作互斥锁（带超时，防死锁），保证同一房间操作串行 |
| `poker:room:{roomId}:timer:{seat}` | STRING | 玩家操作倒计时截止时间戳（超时自动过牌/弃牌，由调度器扫描） |

> 决策说明：整个房间状态用一个 JSON key，配合房间锁串行读改写。虽然不如细粒度字段灵活，但对低并发娱乐场景，**实现最简单、出错最少（KISS）**，且天然保证状态一致。

### 4.6 核心游戏引擎设计

#### 4.6.1 牌型评估 `HandEvaluator`

- 标准 52 张牌，每手从玩家 2 张底牌 + 公共牌最多 5 张中选 **7 选 5** 组成最大牌型。
- 牌型枚举 `HandRank`：`ROYAL_FLUSH > STRAIGHT_FLUSH > FOUR_OF_A_KIND > FULL_HOUSE > FLUSH > STRAIGHT > THREE_OF_A_KIND > TWO_PAIR > ONE_PAIR > HIGH_CARD`。
- 牌型比较实现 `Comparable`，输出 `(rank, 排序后的 kicker 数组)` 便于前端展示与确定性判胜。
- **纯 Java、无状态、可单测**（`PokerHandEvaluatorTest`，覆盖每种牌型边界用例，这是保证规则正确性的核心）。

#### 4.6.2 牌桌状态机（`PokerRoom` / `PokerHand`）

房间状态：`WAITING → PLAYING → ENDED`

手牌阶段（一轮完整流程）：

```
开始下一手
  → 洗牌/发底牌/抽大小盲/发庄家位
  → PREFLOP（翻牌前下注圈）
  → 发 3 张公共牌
  → FLOP
  → TURN（第 4 张）
  → RIVER（第 5 张）
  → SHOWDOWN（若 ≥2 家未弃牌，摊牌比牌）
  → 结算（分配奖池，含边池）→ 落库 → 下一手
```

流程控制要点：
- **提前结束**：某圈下注后仅剩 1 家未弃牌 → 直接赢下奖池，进入下一手，不再发公共牌。
- **all-in 边池**：`PotCalculator` 按投入金额分层出主池与边池，逐层按剩余玩家比牌分配。M1 若砍掉 all-in，会在文档标注（见里程碑），建议 M1 支持基础 all-in 但边池逻辑可后置。
- **超时处理**：玩家操作倒计时结束仍未响应 → 自动过牌（Check）；若存在跟注额 → 自动弃牌（Fold）。

#### 4.6.3 操作合法性校验（服务端强制）

每个操作由后端校验并拒绝非法操作，前端只做提示：

| 动作 | 校验点 |
| --- | --- |
| FOLD / CHECK / CALL / BET / RAISE / ALL_IN | 是否轮到该玩家、当前阶段、金额下限（跟注额 / 最小加注额）、是否 all-in 封顶 |
| 加注 | 至少为前一次加注额，且 ≥ 大盲 |
| CALL | 补齐到当前最高下注额；筹码不足视为 ALL_IN |

#### 4.6.4 并发与一致性

- **单房间串行**：所有变更状态的操作先 `SET NX poker:room:{id}:lock`，成功后读状态→变更→写回→释放；锁带过期时间兜底防死锁。
- **操作幂等**：同一操作（如同一用户的重复下注请求）通过"轮到你的回合"判断 + 房间锁保证不重复执行。
- **落库时机**：每手牌结束后，将 hand / hand_player / hand_action 批量写入 MySQL；房间在解散/结束写入最终状态。
- 娱乐向 MVP **不做多实例分布式复杂方案**；若未来需要水平扩展，仅需共享 Redis + 锁即可平滑迁移（OCP）。

### 4.7 实时通信协议（WebSocket）

#### 连接

```
ws://host/api/ws/poker?token=<JWT>      # context-path 为 /api
```

- `PokerHandshakeInterceptor` 从 query 取 token，调用 `JwtUtil` 校验，把 `userId` 放入 attributes；校验失败拒绝握手。
- 连接建立后，客户端发送 `{ type: "JOIN", roomId }`，服务端将 session 绑定房间并推送完整牌桌快照。

#### 出站消息（服务端 → 客户端）

统一信封：`{ "type": "...", "data": {...} }`

| type | 说明 |
| --- | --- |
| `TABLE_SNAPSHOT` | 完整牌桌快照（入座、筹码、公共牌、轮次、奖池；底牌只含本人） |
| `PLAYER_JOINED` / `PLAYER_LEFT` | 玩家入座/离开 |
| `GAME_START` | 开始发牌 |
| `ACTION_REQUIRED` | 轮到你操作（含倒计时） |
| `PLAYER_ACTION` | 某玩家动作广播（含自己） |
| `BOARD_UPDATE` | 公共牌更新 |
| `ROUND_START` / `ROUND_END` | 下注圈开始/结束 |
| `HAND_RESULT` | 摊牌结果 + 各玩家底牌与牌型 |
| `HAND_END` | 一手结束，筹码结算 |
| `ROOM_CLOSED` | 房间解散 |
| `ERROR` | 错误（含错误码） |

#### 入站消息（客户端 → 服务端）

| type | 说明 |
| --- | --- |
| `JOIN` | 进入房间 |
| `SIT_DOWN` / `STAND_UP` | 入座/离座 |
| `BUY_IN` | 买入筹码 |
| `READY` | 准备就绪（房主确认开始） |
| `START_GAME` | 房主开始 |
| `ACTION` | 下注动作 `{action, amount}` |
| `LEAVE` | 离开房间 |
| `HEARTBEAT` | 心跳保活 |

#### 断线重连

- 客户端检测断开后，用 `JOIN` 消息重连，服务端按 `userId + roomId` 恢复状态并回推 `TABLE_SNAPSHOT`。
- 掉线期间超时规则照常生效（自动过牌/弃牌），避免拖垮牌局。

### 4.8 REST 接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/poker/rooms` | 房间列表（分页/状态过滤） |
| POST | `/api/poker/rooms` | 创建房间（设置盲注/座位数/买入区间） |
| POST | `/api/poker/rooms/join` | 按房号加入（返回 roomId，后续走 WS） |
| GET | `/api/poker/rooms/{roomId}` | 房间详情（含入座玩家） |
| POST | `/api/poker/rooms/{roomId}/start` | 房主开始游戏（也可走 WS） |
| DELETE | `/api/poker/rooms/{roomId}` | 解散房间（仅房主） |
| GET | `/api/poker/rooms/{roomId}/hands` | 房间对局历史 |
| GET | `/api/poker/stats/me` | 我的战绩（场次、胜率、累计赢输） |

> 游戏中的高频操作全部走 WS；REST 只负责低频管理类操作。

### 4.9 新增错误码（ErrorCode 9xxx 段）

```
POKER_ROOM_NOT_FOUND(9101, "房间不存在"),
POKER_ROOM_FULL(9102, "房间已满"),
POKER_ROOM_STATUS_INVALID(9103, "房间状态不正确"),
POKER_NOT_YOUR_TURN(9104, "未轮到你操作"),
POKER_INVALID_ACTION(9105, "非法操作"),
POKER_AMOUNT_INVALID(9106, "金额不合法"),
POKER_NOT_IN_ROOM(9107, "不在该房间中"),
POKER_SEAT_TAKEN(9108, "座位已被占用"),
POKER_BUY_IN_INVALID(9109, "买入金额超出区间"),
POKER_GAME_NOT_STARTED(9110, "游戏尚未开始"),
```

---

## 5. 前端规划（poker 项目）

### 5.1 技术栈与脚手架

```bash
# 推荐基于 tools-web 同款技术栈手动搭建（保持仓库内统一，不引入 monorepo 工具链）
vue@3 + typescript + vite + pinia + vue-router + tailwindcss + axios + 原生 WebSocket
```

### 5.2 目录结构

```
poker/
├── index.html
├── vite.config.ts          # 配置 /api 代理到 game-server
├── package.json
└── src/
    ├── main.ts
    ├── App.vue
    ├── router.ts           # 路由：/login /lobby /room/:roomId
    ├── styles/index.css    # Tailwind 入口 + 全局样式
    ├── shared/
    │   ├── api/http.ts     # axios 实例 + JWT 拦截器 + 401 跳转
    │   ├── api/poker.ts    # REST 接口封装
    │   └── ws/socket.ts    # WebSocket 单例封装（自动重连/心跳/消息分发）
    ├── stores/
    │   ├── user.ts         # 登录态、用户信息
    │   └── table.ts        # 牌桌快照（服务端 TABLE_SNAPSHOT 驱动）
    ├── pages/
    │   ├── LoginPage.vue
    │   ├── LobbyPage.vue   # 大厅：房间列表/创建/加入
    │   └── TablePage.vue   # 游戏桌（核心）
    └── components/table/
        ├── PokerTable.vue        # 桌面容器 + 座位环布局
        ├── PlayerSeat.vue        # 单座位（头像/昵称/筹码/状态）
        ├── CommunityCards.vue    # 公共牌 + 奖池
        ├── HoleCards.vue         # 我的底牌（面朝上）
        ├── ActionBar.vue         # 操作区：弃牌/过牌/跟注/加注滑块/全下/倒计时
        ├── HandResultModal.vue   # 摊牌结果弹窗（各家牌型/赢家）
        └── PlayingCard.vue       # 单张牌渲染（花色/点数/翻转动画）
```

### 5.3 页面与核心交互

1. **登录页 `/login`**：账号密码登录（复用 `/api/auth/register` + `/api/auth/admin/login`），或后续对接手机验证码；登录后保存 JWT 到 localStorage。
2. **大厅 `/lobby`**：房间列表（房号、人数、盲注、状态）、创建房间弹窗（盲注/座位数/买入）、输入房号加入。
3. **游戏桌 `/room/:roomId`**（核心）：
   - 入座/买入 → 全部 READY → 房主开始。
   - 桌面环形展示所有座位；公共牌区显示发牌动画；奖池实时更新。
   - 轮到自己时弹出操作区 + 倒计时，操作即时广播。
   - 摊牌时展示各家底牌、牌型、输赢金额。
   - 支持离开、房主解散、断线自动重连提示。

### 5.4 WebSocket 客户端封装（`socket.ts`）

- 单例；`connect(token, roomId)` / `disconnect()`。
- 心跳（每 15s 发 `HEARTBEAT`），网络异常自动重连（指数退避）。
- 消息分发：按 `type` 路由到 store 的 action，store 持有牌桌快照并驱动界面更新。
- 状态驱动的 UI：**前端只渲染快照，不参与游戏逻辑计算**（服务端可信）。

### 5.5 关键实现说明

- 底牌隐私：`TABLE_SNAPSHOT` 中只包含**本人**底牌；其他玩家座位显示牌背。
- 倒计时：以服务端 `ACTION_REQUIRED` 携带的截止时间戳为准，前端本地渲染倒计时。
- 操作防抖：同一操作短时间重复提交，后端幂等保证，前端按钮置灰防误触。

---

## 6. 实施里程碑

### 6.0 里程碑总览

按**垂直切片**推进：每阶段都产出可运行、可验证的成果，优先打通"可玩闭环"，再逐步完善。核心思想：**引擎先行（纯代码+单测，改造成本最低）→ 实时通道 → 联调闭环 → 完善 → 上线**。

| 阶段 | 主题 | 预计工期 | 核心产出 | 验收标准（DoD） |
| --- | --- | --- | --- | --- |
| M0 | 基础设施与脚手架 | 0.5 天 | 前后端骨架、登录打通 | 前端能登录并调通后端 REST |
| M1 | 扑克游戏引擎（纯 Java） | 1~2 天 | 引擎 + 全量单元测试 | 引擎单测全绿，可纯代码模拟完整一手牌 |
| M2 | 房间管理 + WS 实时通道 | 1~2 天 | REST + WS + Redis 状态驱动 | 多端 WS 快照同步、可执行下注流程 |
| M3 | 大厅 + 游戏桌（可玩闭环） | 2~3 天 | 完整一局可玩 | 2~6 人多浏览器完整打完一局 |
| M4 | 对局完善 | 1~2 天 | 超时/重连/落库/历史 | 掉线不卡局、边池正确、对局可查 |
| M5 | 战绩、打磨与部署 | 1~2 天 | 上线可玩 | 公网可访问、完整体验 |

依赖关系（关键路径）：

```
M0 → M1 → M2 → M3 → M4 → M5
     └──────────────────┘   （引擎正确性是全项目技术风险最高点，最先做）
```

---

### M0 · 基础设施与脚手架（0.5 天）

**后端**
- `pom.xml` 新增 `spring-boot-starter-websocket`。
- `ErrorCode` 新增 `9xxx` Poker 错误码段。
- 建立 `com.gamegroup.poker` 包目录骨架（entity/service/controller/ws/engine/dto）。

**前端**
- 搭建 `poker/` Vite + Vue3 + TS 项目（与 `tools-web` 同配置风格：pinia、vue-router、tailwind、axios）。
- `vite.config.ts` 配置 `/api` 代理到 `http://localhost:8080`（`ws: true`）。
- axios 封装 + JWT 拦截器 + 路由守卫；登录页对接 `/api/auth/register`、`/api/auth/admin/login`。

**DoD**：`npm run dev` 启动，可登录拿到 token；后端启动无报错。

---

### M1 · 扑克游戏引擎（1~2 天，纯 Java + 单测驱动）

> 本阶段**不碰 Spring / DB / Redis**，只写纯 Java 类 + 单测。规则错误在此时修复成本最低。

**引擎类（`com.gamegroup.poker.engine`）**
- `Card` / `Deck`：52 张牌，洗牌支持注入 `Random seed`（便于测试复现）。
- `HandRank` + `HandEvaluator`：7 选 5 最优牌型，10 类牌型完整比较，实现 `Comparable`。
- `PotCalculator`：**完整含 all-in 边池**（按投入金额分层主池/边池，逐层比牌分配）。
- `PokerRoom` / `PokerHand` 状态机（纯 POJO）：入座、抽盲注、庄家轮转、四圈下注、轮次与合法性判断、结算、下一手。

**单元测试（`PokerEngineTest` 等）**
- 牌型：10 类牌型边界 + A2345 顺子、踢脚比较、同花顺 vs 四条。
- 状态机：完整模拟一手牌全流程；多人 all-in 边池分配。
- 按钮/盲注/结算金额的确定性断言。

**DoD**：`./mvnw test` 全绿；能纯代码跑通"完整一手牌"并得到正确输赢。

> 说明：边池逻辑提前到 M1 在引擎层实现+单测，比后期集成时再补更稳妥（此时纯代码、无外部依赖，迭代最快）。

---

### M2 · 房间管理 + WS 实时通道（1~2 天）

**后端（开始接入 Spring 生态）**
- Entity + Repository：`PokerRoom`、`PokerRoomPlayer`、`PokerHand`、`PokerHandPlayer`、`PokerHandAction`（DDL 见 4.4，`ddl-auto` 阶段用 update 自动建表，后续出 SQL 迁移脚本）。
- REST（`PokerRoomController`）：创建/加入/详情/列表/解散房间。
- WS 基建（`com.gamegroup.poker.ws`）：`PokerWebSocketConfig`（注册 `/ws/poker`）、`PokerHandshakeInterceptor`（JWT 握手校验）、`PokerWebSocketHandler`（消息路由）、`SessionRegistry`、心跳。
- Redis 状态层：房间状态单 JSON 存取 + 房间锁（SETNX）；操作倒计时 key。
- `PokerGameService`：把 M1 引擎接入 WS——驱动发牌、下注、轮转、比牌、结算、下一手，并向全桌广播。

**DoD**：两个浏览器连 WS 能看到快照同步；能执行一次完整下注流程；服务端拒绝非法操作。

---

### M3 · 前端大厅 + 游戏桌（可玩闭环）（2~3 天）

**前端**
- 大厅页：房间列表（房号/人数/盲注/状态）、创建房间弹窗、房号加入。
- 游戏桌：环形座位、公共牌发牌动画、奖池、本人底牌、操作区（弃牌/过牌/跟注/加注滑块/全下）、倒计时、摊牌结果弹窗、自动下一手。
- `socket.ts` 单例封装（心跳、消息分发）+ `stores/table.ts` 状态驱动 UI。

**后端（配合）**
- `ACTION_REQUIRED` 推送倒计时；超时自动过牌/弃牌（M4 完善，M3 先有基础实现）。

**DoD**：2~6 人（多浏览器/多设备）完整打完一局：盲注 → 四圈下注 → 比牌 → 结算 → 自动下一手，规则正确。

---

### M4 · 对局完善（1~2 天）

**后端**
- 超时机制完善：调度器扫描 `poker:room:{id}:timer:*`，超时自动过牌/弃牌。
- 断线重连：`JOIN` 消息按 `userId + roomId` 恢复快照；掉线期间超时照常生效。
- 对局落库：每手结束写 `hand / hand_player / hand_action`。
- 历史接口：`GET /api/poker/rooms/{roomId}/hands`。
- 房间生命周期：无操作自动过期清理、房主解散、空桌回收。

**前端**
- 断线自动重连（指数退避）+ 快照恢复。
- 历史对局列表页（复用行为流水做简单回放可选）。

**DoD**：all-in 边池分配正确（单测+实测）；玩家掉线/超时牌局不卡死、能重连；对局在 MySQL 可查。

---

### M5 · 战绩、打磨与部署（1~2 天）

**后端**
- 战绩聚合接口：`GET /api/poker/stats/me`（场次、胜率、累计赢输，`t_poker_hand_player` 聚合）。
- 可选：`t_poker_wallet` 虚拟钱包（累计金币/签到）。
- 大厅房间列表分页/筛选参数。

**前端**
- 战绩/我的统计页。
- 体验打磨：发牌/下注/筹码动效、音效、操作区交互细节、移动端适配（可选）。
- 大厅搜索/排序。

**部署**
- Nginx：静态托管 poker 前端；`/api` 与 `/api/ws` 反代到 game-server（配置 `proxy_set_header Upgrade/Connection` 支持 WS）。
- 复用 docker-compose 的 mysql/redis/game-server；补充 nginx 配置与说明。

**DoD**：完整部署上线，公网浏览器可登录、建房、完整体验对局。

---

### 6.5 明确裁剪项（YAGNI，暂不做）

- 房间内聊天、观战模式、排行榜、每周任务（M5 之后的运营玩法，暂不排期）。
- 真实货币 / 充值 / 提现（产品定位即排除）。
- 多机水平扩展与集群部署（Redis 共享后即可平滑扩展，M5 前不需要）。
- 微信登录（web 端用账号密码登录即可，复用现有 `/auth/*`；若后续接小程序再对接 wx-login）。

---

## 7. 部署与联调

- 后端：继续用现有 `docker-compose`（mysql + redis + minio + game-server），WebSocket 走 Nginx `ws://` 反代（需在 nginx 配置 `proxy_set_header Upgrade`/`Connection`）。
- 前端：`vite build` 产物部署到 Nginx 静态目录，`/api` 与 `/api/ws` 反代到 game-server。
- 本地联调：`vite.config.ts` 配置 proxy：`'/api' -> http://localhost:8080`（`ws: true`），复用 game-server dev profile。

---

## 8. 风险与挑战

| 风险 | 应对 |
| --- | --- |
| 牌型判定正确性 | 核心引擎纯 Java + 完整单元测试，覆盖所有 10 类牌型及边界（A2345 顺子、踢脚比较等） |
| 房间状态并发错乱 | 房间级 Redis 锁 + 状态单 JSON 读写 + 操作幂等校验 |
| 掉线拖垮牌局 | 超时自动过牌/弃牌 + 断线重连恢复 |
| 玩家超时/房间长时间挂起 | 房主解散权限 + 房间无操作自动过期清理 |
| 双端（前端/WS）令牌一致 | 复用 JWT；握手拦截器与 REST 拦截器共用 `JwtUtil` |
| WS 反向代理升级失败 | 部署章节明确 nginx Upgrade 配置，联调先行验证 |

---

## 9. 附录：复用 vs 新建决策表

| 关注点 | 决策 | 理由 |
| --- | --- | --- |
| 用户体系 | 复用 `t_user` + JWT | 避免重复建设（DRY） |
| 匹配房间 `t_match_room` | **不复用**，新建 `t_poker_room` | 语义与字段差异大（盲注/座位/筹码），复用反而引入耦合 |
| 实时通信 | 新增 WebSocket | 现有无实时通道，SSE 无法双向 |
| 对局状态 | Redis 实时 + MySQL 落库 | 性能与持久化职责分离（SRP） |
| 游戏引擎 | 新增纯 Java 模块 | 与 Spring 解耦，便于单测与复用 |
| 前端框架 | Vue 3，与 `tools-web` 同栈 | 仓库内技术统一，维护成本低 |
