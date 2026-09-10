# 简历 PDF 中文字体

`resume-cjk.*` 只是项目里的本地文件名，不是网上现成的字体包。

把任意一款支持简体中文的字体文件放到本目录，并命名为下面之一即可：

- `resume-cjk.ttf`（推荐，OpenHTMLToPDF 可正常嵌入）
- `resume-cjk.otf`（部分 CFF 字体不兼容）
- `resume-cjk.ttc`

## 推荐下载：Noto Sans SC（开源免费）

1. 打开：https://github.com/notofonts/noto-cjk/releases/tag/Sans2.004  
2. 下载静态 TrueType 字体文件（不要使用可变字体）
3. 重命名为 `resume-cjk.ttf`
4. 复制到本目录：

```bash
cp /path/to/static-cjk-font.ttf \
  game-server/src/main/resources/fonts/resume-cjk.ttf
```

项目已内置经过静态化处理的 Noto Sans SC Regular 字体。不要直接替换为可变字体，否则 OpenHTMLToPDF 无法加载。
## 不想塞进项目时

在 Linux 服务器安装系统字体即可，例如：

```bash
sudo apt-get install -y fonts-wqy-microhei
```

服务会自动探测常见系统字体路径。

## 许可

Noto CJK 为 SIL Open Font License，可商用、可嵌入。详见字体包内 LICENSE。
