<template>
  <main class="flex min-h-screen items-center justify-center bg-canvas px-4">
    <section class="panel page-enter max-w-md p-8 text-center">
      <p class="text-sm font-semibold tracking-wide text-brand-600">页面未找到</p>
      <h1 class="mt-3 text-3xl font-bold tracking-tight text-slate-950 dark:text-zinc-50">页面不存在</h1>
      <p class="mt-3 text-sm text-muted">该地址没有对应内容，请返回工具箱首页继续使用。</p>
      <RouterLink class="primary-button mt-6" to="/">返回首页</RouterLink>
    </section>
  </main>
</template>
