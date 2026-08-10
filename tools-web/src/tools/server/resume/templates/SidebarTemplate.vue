<template>
  <article class="resume-sidebar">
    <aside class="resume-sidebar__aside">
      <h1 class="resume-sidebar__name">{{ content.basics.name || '姓名' }}</h1>
      <p class="resume-sidebar__title">{{ content.basics.title || '职位意向' }}</p>

      <h2 class="resume-sidebar__aside-title">联系方式</h2>
      <ul class="resume-sidebar__contacts">
        <li v-if="content.basics.phone">{{ content.basics.phone }}</li>
        <li v-if="content.basics.email">{{ content.basics.email }}</li>
        <li v-if="content.basics.location">{{ content.basics.location }}</li>
        <li v-if="content.basics.website">{{ content.basics.website }}</li>
      </ul>

      <template v-if="visibleSkills.length">
        <h2 class="resume-sidebar__aside-title">技能</h2>
        <div class="resume-sidebar__skills">
          <span v-for="skill in visibleSkills" :key="skill" class="resume-sidebar__chip">{{ skill }}</span>
        </div>
      </template>
    </aside>

    <main class="resume-sidebar__main">
      <template v-for="section in orderedSections" :key="section">
        <section v-if="section === 'summary' && content.summary.trim()" class="resume-sidebar__section">
          <h2>个人简介</h2>
          <p class="resume-sidebar__text">{{ content.summary }}</p>
        </section>

        <section v-else-if="section === 'experience' && content.experience.length" class="resume-sidebar__section">
          <h2>工作经历</h2>
          <div v-for="(item, index) in content.experience" :key="`exp-${index}`" class="resume-sidebar__block">
            <div class="resume-sidebar__heading">{{ item.role || '职位' }} · {{ item.company || '公司' }}</div>
            <div class="resume-sidebar__meta">{{ period(item.start, item.end) }}</div>
            <ul v-if="visibleBullets(item.bullets).length">
              <li v-for="(bullet, bulletIndex) in visibleBullets(item.bullets)" :key="`exp-b-${index}-${bulletIndex}`">
                {{ bullet }}
              </li>
            </ul>
          </div>
        </section>

        <section v-else-if="section === 'projects' && content.projects.length" class="resume-sidebar__section">
          <h2>项目经历</h2>
          <div v-for="(item, index) in content.projects" :key="`proj-${index}`" class="resume-sidebar__block">
            <div class="resume-sidebar__heading">
              {{ item.name || '项目名称' }}
              <template v-if="item.role"> · {{ item.role }}</template>
            </div>
            <div v-if="item.link" class="resume-sidebar__meta">{{ item.link }}</div>
            <ul v-if="visibleBullets(item.bullets).length">
              <li v-for="(bullet, bulletIndex) in visibleBullets(item.bullets)" :key="`proj-b-${index}-${bulletIndex}`">
                {{ bullet }}
              </li>
            </ul>
          </div>
        </section>

        <section v-else-if="section === 'education' && content.education.length" class="resume-sidebar__section">
          <h2>教育经历</h2>
          <div v-for="(item, index) in content.education" :key="`edu-${index}`" class="resume-sidebar__block">
            <div class="resume-sidebar__heading">{{ item.school || '学校' }} · {{ item.degree || '学历' }}</div>
            <div class="resume-sidebar__meta">{{ period(item.start, item.end) }}</div>
            <p v-if="item.description.trim()" class="resume-sidebar__text">{{ item.description }}</p>
          </div>
        </section>
      </template>
    </main>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { period, visibleBullets } from '../format'
import { normalizeSectionOrder, type ResumeContent } from '../types'

const props = defineProps<{
  content: ResumeContent
}>()

const orderedSections = computed(() => normalizeSectionOrder(props.content.sectionOrder))
const visibleSkills = computed(() => props.content.skills.map((item) => item.trim()).filter(Boolean))
</script>

<style scoped>
.resume-sidebar {
  display: flex;
  min-height: 297mm;
  color: #111827;
  font-family: 'Source Han Sans SC', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12.5px;
  line-height: 1.55;
  background: #fff;
}

.resume-sidebar__aside {
  width: 68mm;
  padding: 14mm 10mm;
  background: #1f2937;
  color: #f9fafb;
}

.resume-sidebar__name {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.25;
}

.resume-sidebar__title {
  margin: 0.4rem 0 1.2rem;
  color: #d1d5db;
  font-size: 13px;
}

.resume-sidebar__aside-title {
  margin: 0 0 0.45rem;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #93c5fd;
}

.resume-sidebar__contacts {
  margin: 0 0 1.2rem;
  padding: 0;
  list-style: none;
  color: #e5e7eb;
  font-size: 11.5px;
  line-height: 1.75;
}

.resume-sidebar__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.resume-sidebar__chip {
  display: inline-block;
  padding: 0.15rem 0.45rem;
  border: 1px solid #4b5563;
  border-radius: 999px;
  font-size: 11px;
  color: #f3f4f6;
}

.resume-sidebar__main {
  flex: 1;
  padding: 14mm 12mm;
}

.resume-sidebar__section {
  margin-bottom: 1rem;
}

.resume-sidebar__section h2 {
  margin: 0 0 0.5rem;
  padding-bottom: 0.25rem;
  border-bottom: 2px solid #e5e7eb;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #111827;
}

.resume-sidebar__block + .resume-sidebar__block {
  margin-top: 0.75rem;
}

.resume-sidebar__heading {
  font-weight: 650;
}

.resume-sidebar__meta {
  margin-top: 0.15rem;
  color: #6b7280;
  font-size: 11.5px;
}

.resume-sidebar__text {
  margin: 0;
  color: #1f2937;
  white-space: pre-wrap;
}

.resume-sidebar__block ul {
  margin: 0.35rem 0 0;
  padding-left: 1.1rem;
}

.resume-sidebar__block li + li {
  margin-top: 0.15rem;
}
</style>
