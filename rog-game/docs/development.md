# 《纹铸》开发文档

本文档把 `docs/game-design.md` 落成可以直接分工实现的工程方案。规则以设计文档为准；本文只规定代码怎么组织、数据怎么描述、系统之间怎么通信，以及按什么顺序交付。设计文档里的章节号在本文中写作「设计 §n」。

## 1. 目标与约束

- 纯前端单页，桌面浏览器横屏为主，移动端横屏可玩。不需要服务器、账号和联网。
- 战斗逻辑与渲染分离。逻辑可以在没有 Phaser、没有 DOM 的 Node 环境里跑，用于单元测试和数值模拟。
- 内容数据驱动。新增天赋、技能、装备、连携优先只改数据和效果注册表，不改战斗管道。
- 同一种子、同一输入序列，战斗结果一致。用于复现 bug 和回归测试。
- 性能目标：中端笔记本 60 帧；场上 200 个敌人、400 个投射物、60 个伤害数字时，单帧逻辑不超过 4 毫秒。

## 2. 技术选型

| 部分 | 选择 | 说明 |
| --- | --- | --- |
| 语言 | TypeScript，`strict` 打开 | 内容数据也用 TS 写，借类型检查拦住拼错的 id |
| 构建 | Vite | 开发热更新；生产输出静态文件，可选打成单个 `index.html` |
| 渲染 | Phaser 4，WebGL | 只负责像素世界。内部分辨率 320×180，`pixelArt: true`，`roundPixels: true` |
| 界面 | 原生 DOM + CSS | 商店、账本、三选一、暂停、HUD 文字。不引入 React 等框架，避免和 Phaser 抢帧 |
| 音频 | Web Audio API | 程序合成，不加载音频文件 |
| 测试 | Vitest | 逻辑层单元测试与数值模拟 |
| 代码风格 | ESLint + Prettier | 提交前检查 |

Phaser 内置的物理、音频、输入不启用：

```ts
new Phaser.Game({
  type: Phaser.WEBGL,
  parent: "world",
  width: 320,
  height: 180,
  pixelArt: true,
  roundPixels: true,
  backgroundColor: "#0b0a14",
  audio: { noAudio: true },
  input: { keyboard: false, mouse: false, touch: false, gamepad: false },
  scale: { mode: Phaser.Scale.NONE },
  banner: false,
  scene: [BattleView, HubView],
});
```

输入由 `input/` 模块统一监听 `window`，转换成逻辑层的指令，避免 DOM 界面和 Phaser 同时吞掉按键。窗口缩放由 `view/viewport.ts` 计算最大整数倍率并设置 canvas 的 CSS 尺寸。

## 3. 目录结构

```
rog-game/
├── docs/
│   ├── game-design.md
│   └── development.md
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── public/
│   └── fonts/                 像素数字字体（可选，本地文件，不走外链）
├── src/
│   ├── main.ts                启动：存档、内容校验、Phaser、界面、音频
│   ├── app/
│   │   ├── App.ts             顶层状态机：标题、营地、战斗、商店、结算
│   │   └── flow.ts            一局的流程推进：波次 → 账本 → 三选一 → 补给站
│   ├── sim/                   战斗逻辑，禁止引用 Phaser 和 DOM
│   │   ├── World.ts           单位、投射物、区域效果的容器与固定步长循环
│   │   ├── rng.ts             可播种随机数
│   │   ├── time.ts            逻辑时钟
│   │   ├── units/
│   │   │   ├── Unit.ts
│   │   │   ├── Player.ts
│   │   │   └── Enemy.ts
│   │   ├── stats/
│   │   │   ├── StatBlock.ts   属性定义与默认值
│   │   │   └── computeStats.ts 分层汇总，产出最终属性与来源明细
│   │   ├── combat/
│   │   │   ├── Damage.ts      伤害包与治疗包
│   │   │   ├── pipeline.ts    结算顺序（设计 §9.1）
│   │   │   ├── events.ts      事件总线与事件类型
│   │   │   ├── statuses.ts    状态的挂载、叠层、跳伤、消耗
│   │   │   ├── combo.ts       连段窗口
│   │   │   └── procGuard.ts   深度上限、同源防环、衰减
│   │   ├── skills/
│   │   │   ├── SkillRuntime.ts 冷却、充能、阶、修饰器
│   │   │   └── casts/         每个技能的施放函数
│   │   ├── effects/
│   │   │   ├── registry.ts    效果 id → 实现
│   │   │   └── impl/          按来源分文件：talents、items、bonds、forge、synergies
│   │   ├── build/
│   │   │   ├── Build.ts       本局构筑：技能、天赋、装备、背包、药水、锻体
│   │   │   ├── bonds.ts       羁绊计数
│   │   │   ├── synergies.ts   宽层规则与窄层具名连携的判定
│   │   │   └── affixes.ts     词缀并持
│   │   ├── economy/
│   │   │   ├── Ledger.ts      渠道记账
│   │   │   ├── wallet.ts      金币增减的唯一入口
│   │   │   └── pricing.ts     装备、刷新、锻体、合成抵扣
│   │   ├── waves/
│   │   │   ├── WaveDirector.ts 刷怪、倒计时、狂暴、首领波
│   │   │   └── ai/            敌人行为槽
│   │   ├── collision/
│   │   │   └── SpatialHash.ts
│   │   └── output.ts          表现事件队列（给 view、ui、audio 消费）
│   ├── meta/
│   │   ├── draft.ts           天赋与技能三选一的抽取与加权
│   │   ├── shop.ts            补给站货架生成、刷新、合成格
│   │   ├── forge.ts           锻体三选一与价格
│   │   ├── blackMarket.ts     黑市
│   │   ├── heat.ts            炉温试炼与契约
│   │   └── camp.ts            营地、淬炼、灵魂钱币
│   ├── content/               全部静态数据
│   │   ├── ids.ts             id 字面量联合类型
│   │   ├── characters.ts
│   │   ├── skills.ts
│   │   ├── talents.ts
│   │   ├── items.ts
│   │   ├── recipes.ts
│   │   ├── bonds.ts
│   │   ├── synergies.ts
│   │   ├── forge.ts
│   │   ├── potions.ts
│   │   ├── enemies.ts
│   │   ├── bosses.ts
│   │   ├── waves.ts
│   │   ├── modes.ts
│   │   ├── heat.ts
│   │   ├── blackMarket.ts
│   │   └── validate.ts        启动时校验引用完整性
│   ├── view/                  Phaser 场景与精灵，只读 sim 状态和表现事件
│   │   ├── BattleView.ts
│   │   ├── HubView.ts
│   │   ├── sprites/           程序生成的像素图集
│   │   ├── fx/                闪白、残影、预警圈、伤害数字、震屏
│   │   └── viewport.ts
│   ├── ui/                    DOM 界面
│   │   ├── hud/
│   │   ├── ledger/
│   │   ├── draft/
│   │   ├── shop/
│   │   ├── forge/
│   │   ├── pause/
│   │   ├── codex/
│   │   ├── tooltip.ts
│   │   └── styles/
│   ├── audio/
│   │   ├── AudioEngine.ts
│   │   └── voices/            每类事件的合成函数
│   ├── input/
│   │   ├── keyboard.ts
│   │   ├── pointer.ts
│   │   └── touch.ts
│   ├── save/
│   │   ├── storage.ts
│   │   ├── schema.ts
│   │   └── migrate.ts
│   └── debug/
│       ├── overlay.ts         碰撞圈、帧时间、事件流
│       └── cheats.ts          开发模式下的加钱、给天赋、跳波
└── tests/
    ├── sim/
    ├── meta/
    └── content/
```

依赖方向只能向下：`app → ui / view / audio → sim / meta → content`。`sim` 不知道界面存在；界面通过 `output.ts` 的事件和只读快照获取信息。

## 4. 运行时总览

### 4.1 顶层状态机

```
Boot
 └─ Title
     └─ Camp
         ├─ Codex
         └─ ModeSelect ─ HeatSetup（仅困难）
              └─ Run
                  ├─ Wave            战斗，逻辑循环运行
                  ├─ Ledger          本波账本
                  ├─ Draft           天赋或技能三选一（按波次表）
                  ├─ Shop            补给站，可能附带黑市和技能残页
                  ├─ FormChoice      预留，本版不做
                  ├─ Downed / Victory
                  └─ Settlement      灵魂结算 → 回到 Camp
```

`App` 持有当前状态和一个 `Run` 对象。状态切换只在 `flow.ts` 里发生，界面只发出意图（例如 `confirmLedger`、`pickDraft(id)`、`leaveShop`）。

### 4.2 一局的推进

```ts
async function runLoop(run: Run) {
  for (let wave = 1; wave <= run.mode.waves; wave++) {
    const result = await playWave(run, wave);      // Wave 状态
    if (result === "downed") return settle(run, "downed");
    run.economy.closeWave(wave);                    // 结息、期货、波次奖励
    await showLedger(run, wave);                    // Ledger
    for (const pick of run.mode.draftsAfter(wave)) {
      await draft(run, pick);                       // Draft：talent 或 skill
    }
    if (wave < run.mode.waves) await shop(run, wave); // Shop
  }
  return settle(run, "cleared");
}
```

顺序与设计 §5 一致：账本，然后三选一，然后补给站。黑市是补给站里的附加摊位，不是独立状态。

### 4.3 固定步长

逻辑以 60 赫兹固定步长推进，渲染按浏览器帧率插值。

```ts
const STEP = 1 / 60;
let acc = 0;
function frame(dtReal: number) {
  acc += Math.min(dtReal, 0.25);
  while (acc >= STEP) {
    world.step(STEP, input.consume());
    acc -= STEP;
  }
  view.render(world.snapshot(), acc / STEP);
}
```

暂停、标签页隐藏、WebGL 上下文丢失时停止累加。逻辑时间和墙钟时间分开，冷却、状态、波次倒计时都读逻辑时间。

## 5. 内容数据

所有静态内容在 `src/content/` 里用 TS 对象描述。数据只写数值、标签和效果 id，不写函数体；行为放在效果注册表里。这样数据可以被校验、被图鉴读取、被工具导出。

### 5.1 公共类型

```ts
type DamageType = "physical" | "magic" | "true";
type DamageTag = "attack" | "skill" | "dot" | "thorns" | "proc" | "combo" | "environment";
type Quality = "common" | "rare" | "epic" | "legendary";
type TalentTier = "silver" | "gold" | "prismatic";
type ForgeQuality = "normal" | "fine" | "perfect";

type BondId = "scorch" | "chain" | "vault" | "bulwark" | "swift" | "flask";
type AffixId = "blade" | "hex" | "wind" | "market";
type VeinId = "blade" | "hex" | "wind" | "market";

type SkillTag =
  | "dash" | "area" | "bounce"
  | "applyBurn" | "consumeBurn"
  | "applyMark" | "consumeMark"
  | "slow" | "shield" | "dodge";

type StatKey =
  | "ad" | "ap" | "as" | "crit" | "critDmg" | "ah" | "ov"
  | "hp" | "armor" | "mr" | "dr" | "dodge"
  | "ms" | "luck" | "pickup" | "goldPct"
  | "dmgPct";

interface StatMod {
  stat: StatKey;
  flat?: number;
  pct?: number;       // 0.12 表示 +12%
}

interface EffectRef {
  id: string;         // 效果注册表里的 key
  params?: Record<string, number>;
}
```

### 5.2 角色

```ts
interface CharacterDef {
  id: "bladeguard" | "hexcannon" | "drifter";
  name: string;
  base: Partial<Record<StatKey, number>>;
  attack: {
    kind: "arc" | "projectile" | "dagger";
    interval: number;          // 秒
    range: number;             // 像素
    scaling: { ad?: number; ap?: number };
    damageType: DamageType;
  };
  innate: EffectRef;           // 本命被动
  signatureSkill: SkillId;     // 本命技能，不可替换
  bondTags: BondId[];          // 本命技能带来的羁绊计数（通常为空或 1 个）
}
```

### 5.3 技能

本命技能和习得技能用同一种定义，`slotKind` 区分。

```ts
interface SkillDef {
  id: SkillId;
  name: string;
  slotKind: "signature" | "learned";
  tags: SkillTag[];            // 1 到 2 个
  bondTags?: BondId[];
  baseCooldown: number;
  cast: string;                // casts/ 下的施放函数 key
  params: Record<string, number>;   // 每段伤害系数、半径、持续时间等
  rankUp?: {
    rank2: { valuePct: number; cooldownPct: number };   // 默认 +30%，-10%
    rank3: EffectRef;          // 3 阶的一句变化
  };
}
```

`params` 里的数值随阶提升时，由 `SkillRuntime` 统一乘系数，施放函数只读取当前值，不判断自己几阶。3 阶效果用修饰器挂上（见 §8.4）。

### 5.4 天赋

```ts
interface TalentDef {
  id: TalentId;
  name: string;
  tier: TalentTier;
  category: "general" | "signatureRewrite" | "tagHook" | "synergySeed";
  kind: "rewrite" | "mechanic" | "stat" | "income";
  bondTag?: BondId;
  requires?: Requirement;      // 抽取前置，例如只给刃卫、只在本命为位移时出现
  stats?: StatMod[];
  effects?: EffectRef[];
  cost?: { text: string; effects: EffectRef[]; stats?: StatMod[] };
  seedFor?: SynergyId[];       // 连携种子：卡面展示还缺什么
}

type Requirement =
  | { character: CharacterDef["id"] }
  | { signatureHasTag: SkillTag }
  | { anySkillHasTag: SkillTag }
  | { all: Requirement[] }
  | { any: Requirement[] };
```

代价拆成单独字段，卡面单独一行展示，逻辑上与收益同时挂载、同时移除。

### 5.5 装备与配方

```ts
interface ItemDef {
  id: ItemId;
  name: string;
  quality: Quality;
  price: number;
  stats: StatMod[];
  hook?: EffectRef;            // 最多一个钩子
  affix?: AffixId;             // 并持词缀
  bondTag?: BondId;            // 纹章只有这一项
  isEmblem?: boolean;
  maxStars: 3;
}

interface RecipeDef {
  id: string;
  result: ItemId;
  materials: ItemId[];         // 2 或 3 个
}
```

升星规则写在代码里：每星数值词条 +40%，包括 `stats` 和 `hook.params` 里标记为可缩放的字段。

### 5.6 羁绊、词缀、连携

```ts
interface BondDef {
  id: BondId;
  name: string;
  tiers: { at: 2 | 4 | 6; text: string; stats?: StatMod[]; effects?: EffectRef[] }[];
}

interface AffixDef {
  id: AffixId;
  pairText: string;
  stats?: StatMod[];
  effects?: EffectRef[];
}

interface SynergyDef {
  id: SynergyId;
  name: string;
  pairing: "skill×skill" | "skill×item" | "item×item" | "talent×skill" | "forge×skill" | "character×skill";
  when: SynergyCondition;
  effects: EffectRef[];
  text: string;
}

type SynergyCondition =
  | { hasSkill: SkillId }
  | { hasSkillTag: SkillTag }
  | { hasItem: ItemId }
  | { hasTalent: TalentId }
  | { character: CharacterDef["id"] }
  | { signatureHasTag: SkillTag }
  | { veinAtLeast: { vein: VeinId; count: number } }
  | { all: SynergyCondition[] };
```

宽层不需要数据表：标签、钩子、连段、施加与消耗、词缀并持都由规则代码处理。只有窄层需要 `SynergyDef`。

### 5.7 锻体

```ts
interface VeinDef {
  id: VeinId;
  name: string;
  perShard: StatMod[];         // 普通碎片的基础属性
  milestones: { at: 3 | 6 | 10; name: string; text: string; effects?: EffectRef[]; stats?: StatMod[] }[];
  fineAffixes: EffectRef[];    // 精良、完美碎片额外词条的抽取池
}

interface BodyRealmDef {
  at: number;                  // 碎片总数
  name: string;
  stats: StatMod[];
  paletteSwap: string;         // 精灵换色方案 id
}

const FORGE_QUALITY_MULT = { normal: 1, fine: 1.6, perfect: 2.4 };
const FORGE_STAT_MULT    = { normal: 1, fine: 1.8, perfect: 2.6 };
```

### 5.8 敌人与波次

```ts
interface EnemyDef {
  id: string;
  archetype: "brawler" | "thrower" | "sprinter" | "armored" | "wisp";
  hp: number;
  speed: number;
  radius: number;
  armor: number;
  mr: number;
  attack: { damage: number; type: DamageType; range: number; interval: number };
  behavior: string;            // ai/ 下的行为 key
  goldChannel: "kill" | "elite" | "boss" | "bounty";
  gold: number;
}

interface WaveDef {
  index: number;
  duration: number;            // 秒；首领波为 0
  budget: number;              // 刷怪预算
  pool: { enemy: string; weight: number; cost: number }[];
  elites?: { count: number; prefixes: string[] };
  boss?: string;
}

interface ModeDef {
  id: "drill" | "standard" | "hard";
  waves: number;
  bossWaves: number[];
  talentDraftsAfter: number[];
  skillDraftsAfter: number[];
  bondCap: 4 | 6;
  blackMarketWindows: [number, number][];
  scaling: { fromWave: number; hp: number; dmg: number; eliteRate: number };
}
```

### 5.9 内容校验

`content/validate.ts` 在开发模式启动时运行，在 CI 里作为测试运行。检查项：

- 所有 id 唯一，所有引用（技能、物品、效果、行为、配方材料）都存在。
- 每份技能 1 到 2 个标签；施加与消耗成对的标签至少各有一个技能提供。
- 每件装备最多一个钩子，最多一个羁绊标签。
- 每张天赋最多一个羁绊标签；`cost` 只出现在黄金和棱彩。
- 窄层连携不超过 12 条，`pairing` 两侧类型与 `when` 条件一致。
- 每件传说至少被一条配方产出，且配方材料里至少有两件普通或稀有。
- 至少 3 条配方的材料跨两个羁绊。
- 波次表覆盖模式的全部波数，首领波与 `bossWaves` 一致。

## 6. 属性系统

### 6.1 分层汇总

最终属性 = 基础值经过一组修正层。每层产出若干 `StatMod`，汇总时先加所有平值，再乘所有百分比：

```
final = (base + Σflat) × (1 + Σpct)
```

层的顺序只影响来源明细的展示顺序，不影响数值：

1. 角色基础
2. 局外淬炼
3. 装备（含升星）
4. 天赋（含代价）
5. 羁绊
6. 词缀并持
7. 锻体碎片与碎片词条
8. 脉络拐点
9. 身体境界
10. 炉温
11. 限时状态（药水、急速、风行移速）

`computeStats(build, statuses)` 返回：

```ts
interface ComputedStats {
  values: Record<StatKey, number>;
  sources: Record<StatKey, { layer: string; sourceId: string; flat: number; pct: number }[]>;
}
```

暂停界面的属性来源直接渲染 `sources`。

### 6.2 缓存

构筑变化（买卖装备、选天赋、锻造、学技能）时整体重算一次。限时状态单独作为一层增量，每次状态挂载或到期时只重算这一层并合并。战斗中不允许每帧完整重算。

依赖持有金币的属性（「金身」、市脉络 10）在金币变化时标脏，在下一步开始前重算，避免同一帧内属性抖动。

### 6.3 上限与公式

| 属性 | 实现 |
| --- | --- |
| 攻速 | `interval = baseInterval / (1 + as)`；`as` 不设上限，但攻击间隔最小 0.12 秒 |
| 暴击率 | 截断到 `[0, 1]` |
| 技能急速 | `cooldown = base × 100 / (100 + ah)`；`ah` 可为负，最低 -50 |
| 护甲、魔抗 | `reduction = x / (x + 100)`；负值时 `reduction = x / (100 - x)`，表示增伤 |
| 伤害减免 | 截断到 `[0, 0.4]` |
| 闪避 | 截断到 `[0, 0.45]` |

## 7. 战斗管道

### 7.1 事件总线

```ts
type CombatEvent =
  | { type: "onHit"; ctx: HitCtx }
  | { type: "onSkill"; ctx: SkillCtx }
  | { type: "onSkillHit"; ctx: SkillHitCtx }
  | { type: "onCombo"; ctx: ComboCtx }
  | { type: "onMarkConsume"; ctx: ConsumeCtx }
  | { type: "onBurnConsume"; ctx: ConsumeCtx }
  | { type: "onCrit"; ctx: HitCtx }
  | { type: "onKill"; ctx: KillCtx }
  | { type: "onDodge"; ctx: DodgeCtx }
  | { type: "onDamageTaken"; ctx: DamageCtx }
  | { type: "onDamageDealt"; ctx: DamageCtx }
  | { type: "onShieldAbsorb"; ctx: DamageCtx }
  | { type: "onTick"; ctx: TickCtx }
  | { type: "onWaveEnd"; ctx: WaveCtx }
  | { type: "onShopReroll"; ctx: ShopCtx }
  | { type: "onPurchase"; ctx: ShopCtx };
```

监听器按优先级排序。同一优先级内按挂载顺序。优先级用于「改写包」和「追加效果」的先后：

| 优先级 | 用途 |
| --- | --- |
| 100 | 修改即将发出的包（例如连段系数、白热把灼烧拆成真实） |
| 50 | 默认，追加效果 |
| 10 | 记账、统计、表现 |

事件在一步内排队派发，不在监听器里递归派发同类事件。新产生的伤害包进入同一步的待处理队列，由管道按 `depth` 依次处理。

### 7.2 伤害包与结算

```ts
interface Damage {
  amount: number;
  type: DamageType;
  tags: Set<DamageTag>;
  sourceId: string;            // "skill:ring-slash"、"talent:thorns"、"item:furnace-tongue"…
  chain: string[];             // 触发链上的全部 sourceId，用于防环
  owner: UnitId;
  target: UnitId;
  crit: boolean;
  procRatio: number;
  depth: number;
  skillId?: SkillId;
  comboStep?: 1 | 2 | 3;
}
```

`pipeline.resolve(dmg)` 严格按设计 §9.1 的八步执行。实现要点：

- 第 1 步的加成用加法汇总后一次相乘：`amount × (1 + Σ dmgPct + comboBonus)`。
- 暴击只对带 `attack` 的包判定。技能想暴击，必须由效果显式设置 `crit = true`。
- 第 6 步若目标死亡，取消该目标本步尚未开始结算的反击类包（`thorns`）。
- 吸血在第 8 步，`dot` 系数 1/3。`environment` 标签的伤害不触发吸血、荆棘、`onDamageDealt`。

### 7.3 防环与衰减

`procGuard.derive(parent, sourceId)` 是派生新包的唯一入口：

```ts
function derive(parent: Damage, sourceId: string, ratio = 0.7): Damage | null {
  if (parent.depth + 1 > 4) return null;
  if (parent.chain.includes(sourceId)) return null;
  return {
    ...parent,
    sourceId,
    chain: [...parent.chain, sourceId],
    depth: parent.depth + 1,
    procRatio: parent.procRatio * ratio,
    crit: false,
    tags: new Set(["proc"]),
  };
}
```

效果实现禁止直接构造带 `depth > 0` 的包，必须走 `derive`。代码评审时检查这一点。

### 7.4 状态

```ts
interface StatusInstance {
  id: "burn" | "slow" | "stun" | "shield" | "haste" | "mark" | "energized";
  stacks: number;
  maxStacks: number;
  expiresAt: number;
  nextTickAt?: number;
  sourceId: string;
  payload: Record<string, number>;
}
```

每种状态一个处理器，负责施加、刷新、跳伤、到期、消耗：

| 状态 | 施加 | 跳伤 | 消耗 |
| --- | --- | --- | --- |
| 灼烧 | 层数 +n，截断到上限；刷新到期时间，不改 `nextTickAt` | 每秒一次，`stacks × perStack`，包带 `dot`，类型魔法（白热时拆成两包） | `consumeBurn(target, perStack)` 返回被消耗层数，并广播 `onBurnConsume` |
| 缓速 | 取更高的减速比例，到期时间取较晚者 | 无 | 无 |
| 眩晕 | 取较晚到期时间；打断目标正在前摇的技能 | 无 | 无 |
| 护盾 | 新增一个独立实例 | 无 | 吸收时按剩余时间从短到长扣 |
| 急速 | 同 `sourceId` 刷新 | 无 | 无 |
| 标记 | 层数 +1，上限 3 | 无 | `consumeMark(target)` 返回层数并清零，广播 `onMarkConsume` |
| 电能 | 移动距离累加到 100 | 无 | 下一次普攻命中时清零 |

灼烧的层数上限默认 5，「炉心」改为 10。传播的灼烧是新施加，`sourceId` 为传播者的效果，跳伤 `procRatio` 0.4。

「燃尽」窄层：在 `onBurnConsume` 的优先级 100 监听器里把移除改为保留 1 层。消耗接口因此返回 `{ consumed, remaining }`，由监听器修改 `remaining`。

### 7.5 连段

```ts
class ComboTracker {
  private windowEndsAt = 0;
  private usedSkills: SkillId[] = [];

  onCast(skillId: SkillId, now: number): 1 | 2 | 3 | null {
    if (now > this.windowEndsAt) this.usedSkills = [];
    if (this.usedSkills.includes(skillId)) return null;   // 同技能不推进
    this.usedSkills.push(skillId);
    const step = this.usedSkills.length as 1 | 2 | 3;
    this.windowEndsAt = step === 3 ? 0 : now + 1.2;
    if (step === 3) this.usedSkills = [];
    return step;
  }
}
```

返回的 `step` 写进该次施放产生的全部伤害包的 `comboStep`。管道第 1 步读取：第二下 +0.15，第三下 +0.30，并给包加 `combo` 标签。`step >= 2` 时派发 `onCombo`。

同一技能在窗口内再次施放时返回 `null`，不重置也不延长窗口。

### 7.6 碰撞

- 所有单位和投射物都是圆。
- `SpatialHash` 格子大小 32 像素，每步重建。敌人数量在 200 以内时比维护增量更简单，开销也够。
- 近战弧形攻击：先查圆形范围，再按角度过滤。
- 区域效果（火焰地面、刃圈、预警圈）是带有存活时间的圆或扇形，按自身的跳伤间隔查询。
- 敌人之间做轻量分离，避免叠成一点：每步对邻近敌人施加一个推力，不做精确的刚体解算。

### 7.7 表现事件

`sim/output.ts` 维护一个每步清空的队列，逻辑只往里写：

```ts
type OutputEvent =
  | { kind: "damage"; target: UnitId; amount: number; type: DamageType; crit: boolean; dot: boolean; comboStep?: number; x: number; y: number }
  | { kind: "heal"; target: UnitId; amount: number }
  | { kind: "gold"; amount: number; channel: LedgerChannel; x: number; y: number }
  | { kind: "status"; target: UnitId; status: string; stacks: number }
  | { kind: "cast"; skillId: SkillId; slot: "signature" | "e" | "r"; x: number; y: number; angle: number }
  | { kind: "impact"; strength: 1 | 2 | 3; x: number; y: number }
  | { kind: "telegraph"; shape: TelegraphShape; type: DamageType; delay: number }
  | { kind: "bond"; bond: BondId; tier: number }
  | { kind: "synergy"; synergy: SynergyId }
  | { kind: "realm"; realm: string }
  | { kind: "death"; unit: UnitId };
```

`view`、`ui`、`audio` 各自订阅需要的种类。逻辑层不知道闪白和震屏存在。

## 8. 技能运行时

### 8.1 槽位

```ts
interface SkillSlot {
  skillId: SkillId;
  rank: 1 | 2 | 3;
  charges: number;
  maxCharges: number;          // 「第二下」把本命改为 2
  rechargeAt: number[];        // 每层充能的就绪时间
  modifiers: SkillModifier[];
}

interface PlayerSkills {
  signature: SkillSlot;
  learned: [SkillSlot | null, SkillSlot | null];   // E、R
}
```

### 8.2 施放流程

1. 输入层产生 `CastIntent { slot, aimX, aimY, targetId? }`。
2. 检查眩晕、充能、正在前摇。失败时给出表现事件（冷却中的短提示音），不消耗输入。
3. 扣一层充能，按当前技能急速计算该层的就绪时间。
4. 调用 `ComboTracker.onCast` 得到 `comboStep`。
5. 构造 `SkillCtx`（施法者、属性快照、阶、修饰器、`comboStep`），派发 `onSkill`。
6. 调用施放函数 `casts[def.cast](ctx)`。施放函数可以立即造成伤害，也可以创建投射物、区域、位移。
7. 每段命中时派发 `onSkillHit`，伤害包带 `skillId` 和 `comboStep`。
8. 施放结束时依次调用修饰器的 `onCastEnd`。

### 8.3 施放函数

施放函数只做这一份技能自己的事，不判断天赋、装备或连携：

```ts
casts.shortDash = (ctx) => {
  const path = ctx.motion.dash(ctx.caster, ctx.aim, ctx.param("distance"), ctx.param("duration"));
  path.onTouch((enemy) => {
    ctx.hit(enemy, {
      amount: ctx.param("base") + ctx.param("adRatio") * ctx.stats.ad,
      type: "physical",
      tags: ["skill"],
    });
    ctx.statuses.apply(enemy, "slow", { pct: ctx.param("slowPct"), duration: ctx.param("slowDuration") });
  });
  path.onEnd(() => ctx.end());
};
```

`ctx.hit` 内部构造伤害包并送进管道，自动带上 `skillId`、`comboStep`、`sourceId = skill:<id>`。

### 8.4 修饰器

天赋的本命改写、技能 3 阶、窄层连携优先用修饰器实现，不复制一份新技能：

```ts
interface SkillModifier {
  id: string;                  // 与 sourceId 相同，例如 "talent:endpoint"
  appliesTo: (slot: SkillSlot, def: SkillDef) => boolean;
  params?: (p: Record<string, number>) => Record<string, number>;   // 改数值
  extraTags?: SkillTag[];      // 额外标签，参与钩子判定
  onCast?: (ctx: SkillCtx) => void;
  onHit?: (ctx: SkillHitCtx) => void;
  onCastEnd?: (ctx: SkillCtx) => void;
  extraCharges?: number;
  cooldownPct?: number;
}
```

例：「终点」只给刃卫的短折，在 `onCastEnd` 打一次范围伤害；「折靴」窄层给本命位移，在 `onCastEnd` 再触发一次一半伤害的位移，并在第二段上设置 `suppressPathHooks`，让钩子不再触发。

修饰器的 `extraTags` 让「折线」这种「若本命是位移」的天赋和以后新增的位移技能自然兼容。

### 8.5 获取与替换

- 三选一里已拥有的技能显示为升阶卡。升阶时 `rank += 1`，数值按 `rankUp` 重新计算，3 阶时挂上 `rank3` 修饰器。
- 两个槽都满时，选择新技能后弹出替换选择：E、R 或取消。取消视为放弃本次三选一。
- 技能残页走商店购买，购买后同样走「学会或升阶或替换」流程。
- 退课（黑市）移除一个习得槽的技能，并移除其全部修饰器、羁绊计数、相关窄层。

技能变化后必须重新计算：羁绊、窄层连携、钩子的生效列表、属性。

## 9. 构筑与组合

### 9.1 Build

```ts
interface Build {
  character: CharacterDef["id"];
  skills: PlayerSkills;
  talents: TalentId[];
  items: (OwnedItem | null)[];       // 6 格
  bag: (OwnedItem | null)[];         // 2 格，不生效
  potions: (PotionId | null)[];      // 默认 3 格，最多 4
  forge: { shards: ForgeShard[]; count: Record<VeinId, number> };
  soulUpgrades: Record<string, number>;
  heat?: HeatConfig;
}

interface OwnedItem { id: ItemId; stars: 1 | 2 | 3; paid: number }
interface ForgeShard { vein: VeinId; quality: ForgeQuality; affixes: EffectRef[] }
```

`OwnedItem.paid` 记录这件装备累计花掉的金币（含升星），用于合成抵扣。

### 9.2 生效列表

构筑每次变化后执行 `rebuildActive(build)`，产出一个只读的生效列表，战斗里只读它：

```ts
interface ActiveSet {
  stats: ComputedStats;
  listeners: Listener[];             // 天赋、装备钩子、羁绊、词缀、锻体拐点、窄层
  modifiers: SkillModifier[];
  bonds: Record<BondId, { count: number; tier: 0 | 2 | 4 | 6 }>;
  affixPairs: AffixId[];
  synergies: { active: SynergyId[]; nearlyActive: { id: SynergyId; missing: string[] }[] };
  hooksBySkillTag: Record<SkillTag, Listener[]>;
}
```

`rebuildActive` 对比新旧 `ActiveSet`，把新亮起的羁绊档位、窄层连携和境界写进表现事件。

### 9.3 羁绊计数

```ts
function countBonds(build: Build, cap: 4 | 6) {
  const count: Record<BondId, number> = emptyBondCount();
  for (const item of build.items) if (item) add(count, items[item.id].bondTag);
  for (const t of build.talents) add(count, talents[t].bondTag);
  for (const slot of allSkillSlots(build)) for (const b of skills[slot.skillId].bondTags ?? []) add(count, b);
  for (const p of build.potions) if (p) add(count, potions[p].bondTag);
  return mapValues(count, (c) => ({ count: c, tier: tierFor(Math.min(c, cap)) }));
}
```

背包不计数。装备升星不加层。药水挂「药瓶」标签，按设计 §18 计入。

### 9.4 宽层规则

宽层没有数据表，是以下几段代码：

1. **钩子派发。** `onSkill`、`onSkillHit` 发生时，按技能的标签（含修饰器的 `extraTags`）取 `hooksBySkillTag` 并执行。位移的第二段若设置了 `suppressPathHooks`，跳过带 `pathHook` 标记的钩子。
2. **连段。** 见 §7.5。
3. **施加与消耗。** 见 §7.4。消耗技能的施放函数调用 `consumeBurn` 或 `consumeMark`，用返回的层数计算追加伤害。同一次施放只调用一次消耗接口，由 `SkillCtx.consumedThisCast` 守住。
4. **词缀并持。** `rebuildActive` 统计装备栏中各词缀的数量，数量达到 2 的词缀挂上 `AffixDef` 的效果。数量再多不叠加。

### 9.5 窄层判定

```ts
function evalCondition(cond: SynergyCondition, b: Build, a: Partial<ActiveSet>): boolean {
  switch (true) {
    case "hasSkill" in cond:        return allSkillSlots(b).some((s) => s.skillId === cond.hasSkill);
    case "hasSkillTag" in cond:     return allSkillSlots(b).some((s) => effectiveTags(s).includes(cond.hasSkillTag));
    case "hasItem" in cond:         return b.items.some((i) => i?.id === cond.hasItem);
    case "hasTalent" in cond:       return b.talents.includes(cond.hasTalent);
    case "character" in cond:       return b.character === cond.character;
    case "signatureHasTag" in cond: return effectiveTags(b.skills.signature).includes(cond.signatureHasTag);
    case "veinAtLeast" in cond:     return b.forge.count[cond.veinAtLeast.vein] >= cond.veinAtLeast.count;
    case "all" in cond:             return cond.all.every((c) => evalCondition(c, b, a));
  }
}
```

「只差一个条件」：对 `all` 条件逐项求值，恰好一项为假时进入 `nearlyActive`，并把那一项转成可读文字。连携种子天赋的抽取加权也读这个结果（见 §11.2）。

窄层效果和其他监听器一样注册，`sourceId` 为 `synergy:<id>`。

## 10. 经济

### 10.1 渠道

```ts
type LedgerChannel =
  | "kill" | "elite" | "boss" | "wave" | "interest" | "bounty"
  | "futuresIn" | "futuresOut" | "dropout" | "critGold" | "pickup"
  | "spendItem" | "spendReroll" | "spendForge" | "spendPotion" | "spendSkill" | "spendBlackMarket";
```

`futuresIn` 记为负数，`futuresOut` 为期货回款，`dropout` 为退课收入。

### 10.2 唯一入口

所有金币变化必须经过 `wallet`：

```ts
wallet.earn(amount, channel, at?: { x: number; y: number });
wallet.spend(amount, channel): boolean;   // 余额不足返回 false，不扣钱
```

`earn` 内部：

1. 应用渠道级改写（「暴君税」把普通击杀改为 0，精英与首领翻倍；「点钞」把收账改记 `bounty` 并 +1；点金药水把击杀翻倍）。
2. 正收入乘 `1 + goldPct`。期货投入和全部支出不乘。
3. 四舍五入到整数。小于 1 的部分累加到该渠道的余数，凑满再发，避免百分比加成永远被吃掉。
4. 写入 `Ledger`，产生 `gold` 表现事件。
5. 标脏依赖持有金币的属性。

场上掉落的金币先生成拾取物，带渠道 id；被捡起时才调用 `earn`。波次结束时自动吸收全部未拾取金币，不丢钱。

### 10.3 波次结算顺序

`economy.closeWave(wave)` 固定顺序：

1. 吸收场上剩余金币。
2. 发放波次奖励 `4 + wave`（加上「小费」等修正）。
3. 期货：到期的投入按倍数回款。
4. 利息：`min(cap, floor(gold × rate))`。利息按发放波次奖励和期货之后的持有金币计算。
5. 派发 `onWaveEnd`（金库 6 层在这里检查持有金币，决定下一波伤害加成）。
6. 冻结本波账本，交给界面。

### 10.4 定价

| 项 | 公式 |
| --- | --- |
| 刷新 | 本站第 k 次（从 0 起）为 `1 + k × step`，`step` 默认 1，「金身」改为 2，金库 4 层费用减半后向上取整 |
| 锻体 | `(8 + 3 × forgeCount) × FORGE_QUALITY_MULT[q]`，向上取整 |
| 装备 | 定义价 × 折扣乘积（金身九折、市脉络 10 九折、首领六折券），向上取整 |
| 合成格 | `max(0, 成品价 - Σ 材料.paid)` |
| 技能残页 | 稀有装备价区间内，按技能定义的 `shardPrice` |
| 药水 | 定义价；药瓶 4 层后下一站打七折 |

折扣在同一类里取乘积，不同来源互不覆盖。所有价格在展示和扣款时用同一个函数计算，避免界面显示与实扣不一致。

## 11. 抽取与商店

### 11.1 随机数

每局一个主种子。各子系统从主种子派生独立的随机流，互不干扰：

```ts
const rng = {
  combat: fork(seed, "combat"),
  talents: fork(seed, "talents"),
  skills: fork(seed, "skills"),
  shop: fork(seed, "shop"),
  forge: fork(seed, "forge"),
  waves: fork(seed, "waves"),
  blackMarket: fork(seed, "blackMarket"),
};
```

这样在商店里多刷新一次，不会改变下一波刷出的敌人。实现使用 `mulberry32` 或 `xoshiro128**`，禁止在逻辑层调用 `Math.random`。ESLint 加规则拦截。

### 11.2 天赋抽取

1. 按模式和当前是第几次天赋选择确定品质权重，再加上幸运修正（每点幸运黄金 +2%、棱彩 +0.5%）。
2. 先抽品质，再在该品质内抽卡。3 张卡不重复，已拥有的天赋不出现。
3. 卡池过滤 `requires`。不满足的卡不出现，例外是「标签钩子」：没有对应标签时仍可出现，卡面标注尚未接上。
4. 类别权重：

| 类别 | 权重 |
| --- | --- |
| 通用 | 1.0 |
| 本命改写 | 白银 1.0，黄金和棱彩 1.5 |
| 标签钩子 | 已有该标签 2.0，否则 0.6 |
| 连携种子 | 对应窄层处于 `nearlyActive` 时 2.5，否则 0.5 |

5. 标准模式棱彩每局最多 1 张，抽到后本局棱彩权重归零。

### 11.3 技能抽取

- 3 张卡不重复。已拥有且未满 3 阶的技能以升阶卡形式出现，权重 1.5。已满 3 阶的不出现。
- 已经拥有施加类标签而没有对应消耗类时，消耗类技能权重 1.5；反之亦然。
- 重抽：天赋和技能共用每局 1 次，`run.rerollsLeft` 计数。

### 11.4 补给站货架

- 装备栏 5 格，按波次决定品质权重。幸运提高稀有及以上的权重。
- 困难模式羁绊上限为 6，已有羁绊达到 3 层以上时，货架有一定概率出现对应纹章。
- 合成格 1 格：遍历配方，材料（装备栏 + 背包）齐全的配方进入候选，随机挑一个展示。
- 锻体：按价付款后生成三选一，必须选一张。付款后刷新商店不影响这次三选一。
- 药水最多 2 瓶。
- 技能残页：从第 5 波起，每站 35% 概率出现 1 张。
- 黑市：只在模式规定的窗口里出现，窗口内按站随机选一站。

刷新只重抽装备栏和药水，不动合成格、锻体、技能残页和黑市。

## 12. 波次与敌人

### 12.1 WaveDirector

- 波次开始时按 `budget` 和 `pool` 生成刷怪计划：一串 `{ at, enemy, count, edge }`。前 20% 时长只刷低成本敌人，后段提高密度。
- 敌人从屏幕外的环形区域进入，不在玩家 48 像素内出生。
- 精英插入在波次中段，带地面描边和前缀。
- 倒计时结束：剩余敌人获得狂暴（移速 +40%，伤害 +30%），之后它们的普通金币不再掉落。场上清空后进入结算。
- 首领波：没有倒计时，首领死亡后清理小怪并结算。

困难模式第 11 波起叠加 `scaling`，炉温在敌人生成时再乘一次。

### 12.2 行为槽

每种原型一个行为函数，读世界快照，写意图：

```ts
type Behavior = (self: Enemy, world: WorldView, dt: number) => EnemyIntent;
```

| 原型 | 行为 |
| --- | --- |
| 徒手 | 朝玩家移动，进入攻击距离后挥击 |
| 投石 | 保持 90 到 120 像素距离，周期性投出慢速石头 |
| 疾奔 | 冲刺接近，短暂停顿后再冲 |
| 重甲 | 慢速逼近，攻击带 0.5 秒预警 |
| 咒灵 | 漂移靠近，死亡时生成 0.8 秒后爆炸的魔法圈 |

首领用分阶段的招式表，每个招式包含预警形状、延迟、伤害包和冷却。预警延迟不小于 0.7 秒。

## 13. 表现层

### 13.1 精灵

美术资源全部程序生成或手绘成极小的像素表，在启动时绘制到一张图集纹理里：

- 角色 16×24，敌人 12 到 20 像素，首领 32 到 48 像素。
- 境界变化通过调色板替换实现，不新增动画帧。
- 技能特效用几何图元（弧、圆、线段、折线）实时绘制。

### 13.2 冲击反馈

| 反馈 | 规则 |
| --- | --- |
| 闪白 | 受击单位一帧近白色 |
| 受击位移 | 渲染层偏移 1 像素，下一帧回到逻辑位置 |
| 震屏 | 按 `impact.strength`，全局冷却 0.12 秒；本命被棱彩改写后的爆发可无视一次冷却 |
| 伤害数字 | 对象池，最多 60 个；同目标同类型 0.2 秒内合并；灼烧更小更透明；暴击更大更短；连段第二下偏高并画连接线 |
| 金币 | 弹出 `+n`，颜色按渠道；拾取时向玩家飞 |
| 横幅 | 羁绊升档、窄层亮起、境界、传说装备，不超过 1.2 秒，不阻断操作 |

### 13.3 预警

预警圈画成 1 像素虚线描边加低透明度填充，颜色按伤害类型：物理暖橙、魔法冷紫、真实近白。圈从出现到结算按比例收紧填充，玩家能读出剩余时间。

## 14. 界面

### 14.1 结构

`#app` 下两层：`#world` 放 Phaser 的 canvas，`#ui` 绝对定位叠在上面。界面组件是简单的类，接收状态快照，负责渲染和发出意图。每个界面只在显示时订阅状态。

```ts
interface Screen<S> {
  mount(root: HTMLElement): void;
  update(state: S): void;
  unmount(): void;
}
```

### 14.2 界面清单

| 界面 | 内容 | 主要操作 |
| --- | --- | --- |
| HUD | 生命与护盾、金币、波次与倒计时、首领血条、天赋图标、羁绊层数、境界名、三个技能键与冷却扇形、连段进度、药水格 | 无，点击药水格可饮用 |
| 账本 | 本波入账与支出按渠道列出，持有金币滚动 | 确认 |
| 三选一 | 3 张天赋或技能卡；会改写已有技能的卡第一行写被改写的技能名；代价单独一行；尚未接上的钩子标注 | 1–3 选择，重抽，放弃 |
| 替换 | 新技能对比 E、R 两槽 | 选择槽位或取消 |
| 补给站 | 装备、锻体、药水三栏；技能残页和黑市贴在装备栏旁；右侧构筑 | 购买、拖动、刷新、锻造 |
| 锻体三选一 | 三张碎片，显示属性、词条、推进了哪一档拐点 | 必须选一 |
| 合成与连携 | 配方、窄层已亮与只差一项、宽层当前接上的规则 | 查看 |
| 暂停 | 属性来源、连携、本局累计账本、设置 | 继续、存档退出、放弃 |
| 结算 | 灵魂钱币明细、伤害来源、收入渠道 | 回营地 |
| 图鉴 | 天赋、技能、连携、装备、羁绊、锻体词条、敌人、首领 | 查看 |

### 14.3 卡面文字

卡面文字由数据生成，不手写在界面里，保证和实际数值一致：

```ts
describeTalent(def, build): { title: string; rewrites?: string; body: string; cost?: string; notConnected?: string }
describeSkill(def, rank, build): { ... }
describeItem(item, build): { ... }
```

数值使用当前属性实时计算，例如「20 + 40% AD（当前 34）」。

### 14.4 输入焦点

- 战斗中键盘输入给逻辑层，界面不抢焦点。
- 打开任何界面时逻辑暂停（账本、三选一、商店本来就在战斗外），暂停菜单打开时逻辑停止推进。
- 所有界面支持键盘完成全部操作，Esc 关闭最上层。

## 15. 音频

`AudioEngine` 在用户第一次交互后创建 `AudioContext`，之前的事件丢弃。

- 每个声音是一个函数：`(ctx, when, params) => void`，用振荡器、噪声缓冲、滤波器和包络现场合成。
- 同类声音限制并发数（普攻命中 6、灼烧 3、金币 4），超过时丢弃最老的。
- 同一帧内多个伤害只播一次命中音，音量按合并后的数量轻微上升。
- 主音量、音效、环境音三个总线，设置里可以分别调节和关闭。
- 关闭音效后，表现层的闪白和震屏不受影响。

声音与事件的对应按设计 §25 实现。新增内容默认继承标签对应的音色，不需要逐个技能写声音。

## 16. 存档

### 16.1 存储

```ts
interface SaveFile {
  schemaVersion: number;
  app: "wenzhu";
  slots: (SlotData | null)[];   // 3 个槽
  settings: Settings;
}

interface SlotData {
  createdAt: string;
  updatedAt: string;
  profile: {
    souls: number;
    soulUpgrades: Record<CharacterDef["id"], Record<string, number>>;
    unlocks: { hardMode: boolean };
    heatRecords: Record<CharacterDef["id"], number>;
    heatRewardsClaimed: Record<CharacterDef["id"], number[]>;
    codex: Record<string, true>;
    flags: Record<string, boolean>;
  };
  run: RunSnapshot | null;
  pendingSettlement: SettlementReceipt | null;
}
```

`localStorage` 的键为 `wenzhu.save`。写入先写 `wenzhu.save.tmp`，成功后再覆盖正式键，最后删除临时键。启动时若存在临时键而正式键损坏，用临时键恢复。

### 16.2 局内存档

只在局间安全点保存：账本确认后、三选一结束后、离开补给站时。战斗中不保存，「存档退出」在战斗中选择时，回到本波开始前的快照。

`RunSnapshot` 只存可重建的数据：模式、种子、各随机流的状态、构筑、金币、账本、当前波次与阶段、商店货架、待选的三选一。生效列表、属性缓存在读档后重算。

### 16.3 结算防重复

结算时先写 `pendingSettlement`（包含本次应发的灵魂钱币和一个唯一 id），再清空 `run`。界面确认后才把钱币加入 `profile.souls`，并记下已处理的 id。崩溃重开时若看到 `pendingSettlement`，重新显示结算界面，不重复发放。

### 16.4 迁移

`migrate.ts` 维护 `schemaVersion` 递增的迁移函数链。读档时依次执行。无法迁移的局内快照丢弃，并提示玩家；营地进度必须保留。

### 16.5 多标签页

用 `localStorage` 的 `storage` 事件加一个带时间戳的锁键，防止两个标签页同时写同一个槽。后打开的标签页进入只读模式并提示。

## 17. 调试工具

开发模式下（`import.meta.env.DEV`）启用：

- 叠加层：碰撞圆、空间哈希网格、帧时间、逻辑步数、活跃监听器数量。
- 事件流面板：滚动显示最近的战斗事件和伤害包，包含 `sourceId`、`depth`、`procRatio`、`chain`。用于排查连锁效果。
- 作弊指令：加钱、给天赋、给技能、给装备、设置锻体脉络数量、跳到指定波次、无敌、冻结敌人。
- 种子与输入录制：导出本局种子和输入序列，粘贴回放复现问题。
- URL 参数：`?seed=123&wave=8&character=hexcannon&talents=furnaceHeart&skills=ember,blast` 直接进入指定构筑的战斗。

生产构建通过编译期常量剔除这些代码。

## 18. 测试

### 18.1 单元测试

逻辑层不依赖浏览器，直接在 Vitest 里测。必须覆盖：

| 模块 | 用例 |
| --- | --- |
| 管道 | 八步顺序；暴击只对 `attack`；护甲负值；伤害减免上限；护盾吸收顺序；吸血的 `dot` 系数；`environment` 不触发荆棘 |
| 防环 | 深度 4 截断；同源不能进入自己的链；`procRatio` 逐级乘 0.7 |
| 状态 | 灼烧叠层与上限；刷新不改跳伤节拍；消耗返回层数；「燃尽」保留 1 层；标记上限 3 |
| 连段 | 第二下和第三下的系数；同技能不推进；超时重置；第三下后窗口关闭 |
| 属性 | 分层汇总；来源明细与数值一致；金币依赖属性在金币变化后更新 |
| 羁绊 | 背包不计数；升星不加层；模式上限 4 与 6；药水计入 |
| 窄层 | 每条连携的成立与不成立；只差一项的识别 |
| 经济 | 渠道改写；金币获取不放大期货投入；余数累加；结算顺序；利息上限 |
| 定价 | 刷新递增与重置；锻体曲线；合成抵扣到 0 不退款；折扣叠乘 |
| 抽取 | 同种子结果一致；不重复；已拥有天赋不出现；标准模式棱彩最多 1 张 |
| 存档 | 迁移链；临时键恢复；结算防重复 |

### 18.2 数值模拟

`tests/sim/balance.test.ts` 用无渲染的 `World` 跑自动战斗：玩家站桩或按简单脚本走位，自动施放技能。输出每个构筑在各波的击杀时间、受到伤害、金币收入。

用途：

- 检查某个天赋或窄层是否导致伤害远超同档。
- 检查经济曲线：在不同收入构筑下，第 10 波和第 20 波的累计金币。
- 检查连锁效果在大量敌人下的性能。

模拟结果写成表格输出，不作为断言失败条件，只在超出阈值时警告。

### 18.3 内容测试

`tests/content/validate.test.ts` 调用 §5.9 的校验，CI 上必须通过。

### 18.4 手动验收

每个里程碑结束时按清单手动走一遍，包括键鼠、触屏、窗口缩放、切换标签页、断开音频设备。

## 19. 性能

| 项 | 做法 |
| --- | --- |
| 单位与投射物 | 对象池；死亡后回收，不在战斗中分配新对象 |
| 碰撞 | 空间哈希；近战弧先查圆再判角 |
| 伤害数字 | 对象池上限 60；合并同目标 |
| 事件 | 每步一次性派发，监听器按事件类型分桶，不遍历全部监听器 |
| 属性 | 构筑变化时整体重算，状态只重算增量层 |
| 渲染 | 一张图集；预警圈、特效用 Graphics 批量绘制；关闭不需要的 Phaser 插件 |
| 界面 | HUD 只在数值变化时写 DOM；冷却扇形用 CSS 变量驱动 |
| 特效档位 | 设置里提供「完整、精简、关闭」：精简减半粒子和数字，关闭只保留闪白 |

性能预算超标时，优先降低表现，不降低逻辑精度。

## 20. 里程碑

每个里程碑结束都必须可以打开页面玩到当前范围，并通过对应测试。

### M0 工程骨架

- Vite + TS + Phaser 4 + Vitest 搭好，ESLint 禁止逻辑层使用 `Math.random`、Phaser、DOM。
- 固定步长循环、可播种随机数、输入层、整数缩放的 320×180 画面。
- 一个能移动的方块角色。

验收：窗口缩放保持整数倍；标签页隐藏时逻辑停止。

### M1 战斗核心

- 单位、敌人行为（徒手、投石）、空间哈希。
- 伤害管道、事件总线、防环。
- 属性汇总与来源明细。
- 一个角色（刃卫）的普攻、本命被动、本命技能。
- 闪白、伤害数字、震屏、命中音。

验收：管道、防环、属性单元测试通过；刃卫能清掉一波 50 个敌人。

### M2 状态、连段与技能运行时

- 灼烧、缓速、眩晕、护盾、急速、标记、电能。
- 连段窗口、施加与消耗。
- 技能槽、充能、修饰器、升阶。
- 另外两名角色；9 个习得技能。

验收：状态和连段测试通过；火种接爆燃、钉刺接收账在战斗里可见。

### M3 一局循环与经济

- 波次导演、倒计时、狂暴、精英、首领（环纹看守）。
- 钱包、渠道、账本界面、波次结算顺序。
- 天赋与技能三选一（先接入白银与黄金各 4 张）、替换、重抽。
- 演练模式完整可玩。

验收：经济与抽取测试通过；演练 5 波从开局打到首领结算。

### M4 补给站与构筑

- 装备、升星、背包、合成格与抵扣。
- 锻体：碎片、价格曲线、脉络拐点、身体境界与换色。
- 药水与饮用间隔。
- 羁绊计数、词缀并持、钩子派发。
- 补给站界面与构筑面板、属性来源。

验收：定价和羁绊测试通过；一局里能凑出 2 层羁绊、铜躯和一组并持。

### M5 组合与内容

- 窄层具名连携与「只差一项」。
- 全部首发天赋（含代价）、技能残页、黑市。
- 标准模式 20 波、首领账房。
- 合成与连携界面、暂停里的连携页。

验收：每条窄层都有测试；标准模式完整通关一次；数值模拟没有超出阈值的构筑。

### M6 困难、局外与存档

- 困难模式 30 波、后两个首领、羁绊 6 层与纹章。
- 炉温试炼、契约、里程碑奖励。
- 营地场景、淬炼、图鉴、灵魂结算。
- 存档槽、局内安全点、迁移、结算防重复、多标签页锁。

验收：存档测试通过；中途存档退出再读回，构筑与金币一致；崩溃后不重复发放灵魂钱币。

### M7 打磨

- 全部音效与环境音、特效档位。
- 触屏摇杆与按键、横屏提示、全屏。
- 首次游玩引导（演练里的提示卡）。
- 性能压测与优化、低端设备特效降级。

验收：§1 的性能目标在目标设备上达成；手动验收清单全部通过。

## 21. 编码约定

- 逻辑层所有时间单位为秒，距离为内部像素，比例为小数（0.15 表示 15%）。
- `sourceId` 统一格式 `<类别>:<id>`，类别为 `skill`、`talent`、`item`、`bond`、`affix`、`forge`、`realm`、`synergy`、`potion`、`heat`、`enemy`。
- 效果实现文件按来源类别分，每个效果导出一个对象，key 与数据里的 `EffectRef.id` 一致。
- 不在效果实现里直接修改生命值、金币和属性；分别走管道、`wallet`、修正层。
- 新增内容的提交需要同时包含：数据、效果实现（如有）、至少一个测试、图鉴文字。
- 提交信息使用 `type(scope): description`，scope 取模块名，例如 `feat(combo): 连段第三下关闭窗口`。

## 22. 待定与风险

| 问题 | 影响 | 处理 |
| --- | --- | --- |
| 连段第三下需要三个不同技能，前期只有本命时连段不存在 | 前几波没有连段反馈 | 第 4 波第一次技能三选一后才出现，演练里给教学技能；必要时让普攻在窗口内不推进但延长窗口 0.2 秒 |
| 天赋代价「炉心」持续掉血与护盾、吸血的交互 | 可能被吸血完全抵消，失去代价含义 | `environment` 伤害不吃护盾，测试里验证 |
| 金币依赖属性（金身、市 10）让玩家倾向于不花钱 | 商店体验变差 | 上限 40 / 70 已限制；数值模拟观察持币曲线 |
| 窄层「折靴」第二段与钩子 | 可能重复触发路径钩子 | 用 `suppressPathHooks` 明确屏蔽，单元测试覆盖 |
| 同一帧大量灼烧跳伤 | 伤害数字和声音过载 | 合并与并发上限，见 §13.2、§15 |
| 本命被动「步幅」与链闪、连锁羁绊叠加 | 弹射次数过高造成性能问题 | 单次弹射总数上限 12，派生包受深度限制 |
| Phaser 4 接口与 3 版本差异 | 文档或示例不匹配 | 只使用场景、图集、Graphics、相机这些稳定部分；封装在 `view/` 内，替换成本低 |
