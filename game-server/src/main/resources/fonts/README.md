# 简历 PDF 中文字体

`resume-cjk.*` 只是项目里的本地文件名，不是网上现成的字体包。

把任意一款支持简体中文的字体文件放到本目录，并命名为下面之一即可：

- `resume-cjk.otf`（推荐）
- `resume-cjk.ttf`
- `resume-cjk.ttc`

## 推荐下载：Noto Sans SC（开源免费）

1. 打开：https://github.com/notofonts/noto-cjk/releases/tag/Sans2.004  
2. 在 Assets 里下载：  
   **Region Specific Subset OTFs Simplified Chinese (简体中文)**（约 47.8 MB）  
   （对应文件名可能是 `18_NotoSansSC.zip`，页面上不一定直接显示这个名字）
3. 解压后找到 `NotoSansSC-Regular.otf`
4. 复制并重命名到本目录：

```bash
cp /path/to/NotoSansSC-Regular.otf \
  game-server/src/main/resources/fonts/resume-cjk.otf
```

也可以用命令直接下载 Regular（约 8MB）：

```bash
cd game-server/src/main/resources/fonts
curl -L -o resume-cjk.otf \
  "https://github.com/notofonts/noto-cjk/raw/main/Sans/SubsetOTF/SC/NotoSansSC-Regular.otf"
```

不要选那几个几百 MB 的 “All …” 大包，也别选 Japanese/Korean。
## 不想塞进项目时

在 Linux 服务器安装系统字体即可，例如：

```bash
sudo apt-get install -y fonts-wqy-microhei
```

服务会自动探测常见系统字体路径。

## 许可

Noto CJK 为 SIL Open Font License，可商用、可嵌入。详见字体包内 LICENSE。
