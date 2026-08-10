# Role & Goal
你是一位顶级的资深前端开发专家与 UI/UX 设计师。你需要复刻并重构一个类似于 `tools-web` (https://github.com/naroat/tools-web) 的在线多功能工具箱。
虽然我们要复刻它的核心功能和结构，但我们需要完全重塑它的视觉风格，摒弃平庸的默认组件感，采用极具现代感的「Modern Minimalist SaaS / 极简科技感」美学风格。

请严格遵守以下的设计规范与代码硬约束。

---

## 一、 视觉设计系统 (Design System) 硬性约束

请将以下视觉规范固化到 CSS 变量或 Tailwind 配置文件中，严禁在编码时随意使用高饱和度、高对比度的“土味”颜色。

### 1. 配色方案 (Color Palette)
*   **浅色模式 (Light Mode):**
    *   背景色 (Background): 纯白或极浅的冷灰色 `bg-[#f8fafc]` (slate-50) 或 `bg-[#f4f4f5]` (zinc-100)。
    *   卡片/容器: 纯白 `bg-white`，带有极其微弱的灰色边框。
    *   主文字: 深灰 `text-[#09090b]` (zinc-950) 或 `text-[#0f172a]` (slate-900)。
    *   次要文字: 中灰 `text-[#71717a]` (zinc-500)。
*   **深色模式 (Dark Mode):**
    *   背景色: 极深墨色 `bg-[#09090b]` (zinc-950) 或 `bg-[#030712]` (slate-950)。
    *   卡片/容器: 带有微弱透明度的深灰 `bg-[#18181b]` (zinc-900) 配合 `backdrop-blur-md`。
    *   主文字: 纯白或微白 `text-[#f4f4f5]` (zinc-100)。
    *   次要文字: 灰白 `text-[#a1a1aa]` (zinc-400)。
*   **品牌主题色 (Theme Accent):**
    *   使用高雅的 **靛蓝色 (Indigo)** 或 **翡翠绿 (Emerald)** 作为点缀：
        *   Indigo: `#4f46e5` (indigo-600) / `#6366f1` (indigo-500)
        *   Emerald: `#059669` (emerald-600) / `#10b981` (emerald-500)
    *   【严禁】使用刺眼的纯红、纯绿或纯蓝色作为大面积背景。

### 2. 边框与阴影 (Borders & Shadows)
*   **圆角 (Border Radius):** 所有的卡片、输入框、按钮一律使用优雅的大圆角。
    *   卡片/容器: `rounded-xl` (12px) 或 `rounded-2xl` (16px)。
    *   小组件/输入框/按钮: `rounded-lg` (8px)。
*   **边框 (Borders):** 采用极细且低对比度的边框，营造精致感。
    *   浅色: `border border-slate-200/60`
    *   深色: `border border-zinc-800/60`
*   **阴影 (Shadows):** 严禁使用浓黑、大范围的生硬投影。
    *   仅使用环境光投影或微弱的立体投影：`shadow-sm` 或配合 Tailwind 的 `shadow-[0_8px_30px_rgb(0,0,0,0.04)]`。

### 3. 排版与间距 (Typography & Spacing)
*   **字体:** 优先使用系统默认的无衬线现代字体：`font-sans antialiased`（如 Inter, system-ui）。
*   **字重层级:** 标题使用 `font-semibold` 或 `font-bold`（不宜过粗），正文使用 `font-normal` 或 `font-medium`。
*   **间距 (Whitespace):** 拒绝拥挤。卡片内边距至少保持 `p-5` (20px) 到 `p-6` (24px)；组件间距使用 `gap-6` (24px)。

---

## 二、 核心布局与组件重构

你的任务是构建以下三个核心区域，并让它们看起来高级、协调：

### 1. 左侧分类导航边栏 (Sidebar)
*   **结构:**
    *   顶部是精致的 Logo + 站点标题（带微弱渐变色）。
    *   下方是工具分类列表（如：开发运维、文本处理、图片处理、数据图表等）。
*   **视觉效果:**
    *   分类项不采用生硬的纯背景，默认仅显示“文字 + 精美单色 SVG 图标”， hover 时背景渐变滑入（ transition 持续时间 200ms ）。
    *   激活态分类：文字和图标变为主题色（Indigo），背景变为非常淡的半透明主题色（如 `bg-indigo-50` 或在深色模式下为 `bg-indigo-950/30`），左侧带有一条 `w-1 h-5 rounded-full bg-indigo-600` 的精致小立条。

### 2. 顶部工具栏 (Header)
*   **结构:**
    *   包含一个位于中央偏右的「全局搜索框」，右侧包含「暗黑模式切换按钮」和「个人/Github链接」。
*   **视觉效果:**
    *   搜索框采用高档的“胶囊状”或“微圆角扁平”设计，内部右侧显示键盘快捷键提示（如置灰的 `⌘ K` 或 `Ctrl+K` 药丸标）。
    *   Header 整体采用 **毛玻璃效果 (Glassmorphism)**：`bg-white/80 dark:bg-zinc-950/80 backdrop-blur-md sticky top-0 z-50 border-b border-slate-200/50 dark:border-zinc-800/50`。

### 3. 主体网格区域 (Main Grid - Bento Grid 风格)
*   **结构:**
    *   右侧主体展示对应分类下的工具卡片。
*   **卡片视觉细节 (Crucial):**
    *   **图标设计:** 每个工具卡片的图标，应包裹在一个带有微弱背景色的圆形或微圆角方形容器中（例如，MD5 工具配淡蓝背景，时间戳工具配淡橙背景，背景透明度设为 10% ~ 15% ）。
    *   **内容布局:** 标题在上，加粗 `text-slate-800 dark:text-zinc-100 font-medium`；下方是一行极其简短、置灰的描述文字 `text-xs text-slate-500 dark:text-zinc-400`。
    *   **交互动效:**
        *   鼠标悬停 (Hover) 时，卡片整体平滑向上微移 `hover:-translate-y-1`，阴影增强，边框颜色向主题色靠拢（如 `hover:border-indigo-500/30`）。
        *   可以加入微弱的向外发光效果（Glow Effect）。

### 4. 独立工具的交互控制面板 (Tool Interface Workspace)
当用户点击进入具体的工具（例如“JSON在线转换”或“随机密码生成”）：
*   **布局:** 采用**左右分栏（双栏式）**或在移动端下自动切换为**上下分栏**。左侧/上方为「输入/控制区」，右侧/下方为「输出/结果区」。
*   **输入框/文本域 (Textarea):**
    *   使用现代代码编辑器风格的字体（如 `font-mono`），行高适中。
    *   获取焦点时（Focus），边框高亮平滑过渡，并带有柔和的 `ring-2 ring-indigo-500/10` 呼吸灯效果。
*   **操作按钮:**
    *   “主要操作按钮”（如：转换、生成）使用渐变微光质感（例如：`bg-gradient-to-r from-indigo-600 to-violet-600 hover:opacity-90 text-white font-medium shadow-md shadow-indigo-500/10`）。
    *   “次要操作按钮”（如：清空、重置）使用无背景或浅灰背景，低调不抢戏。
    *   “一键复制”按钮必须配有“已复制”的成功提示（Toast/Tooltip）和对勾图标的微动画反馈。

---

## 三、 绝对禁止的“低审美”行为 (Anti-Patterns)
1.  【严禁】使用原生 HTML 的 `input`、`button`、`select` 默认样式。
2.  【严禁】使用纯黑 (`#000000`) 的文字或纯黑生硬的边框。
3.  【严禁】给卡片使用非常厚的立体投影或极度刺眼的渐变色背景。
4.  【严禁】工具卡片排版太挤，必须保留足够的内边距 (`padding`) 和外边距 (`margin`)。
5.  【严禁】在无过渡动效的情况下瞬间切换 hover 状态，所有视觉反馈（颜色、位置、阴影）必须加上 `transition-all duration-300 ease-in-out`。

请基于这些要求，使用现代前端技术栈（推荐 Vue3/React + Tailwind CSS）编写符合该高审美标准的页面结构和主要组件代码。