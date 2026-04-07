let inited = false
let editorRef: any = null

export async function getMarkdownEditor() {
  if (editorRef) {
    return editorRef
  }

  const [{ default: VMdEditor }, { default: githubTheme }, { default: hljs }] = await Promise.all([
    import('@kangc/v-md-editor'),
    import('@kangc/v-md-editor/lib/theme/github.js'),
    import('highlight.js')
  ])

  await Promise.all([
    import('@kangc/v-md-editor/lib/style/base-editor.css'),
    import('@kangc/v-md-editor/lib/theme/style/github.css')
  ])

  if (!inited) {
    VMdEditor.use(githubTheme, {
      Hljs: hljs,
    })
    inited = true
  }

  editorRef = VMdEditor
  return editorRef
}
