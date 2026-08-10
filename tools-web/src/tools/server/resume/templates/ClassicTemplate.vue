<template>
  <article class="resume-classic">
    <header class="resume-classic__header">
      <div>
        <h1 class="resume-classic__name">{{ content.basics.name || '姓名' }}</h1>
        <p class="resume-classic__title">{{ content.basics.title || '职位意向' }}</p>
      </div>
      <ul class="resume-classic__contacts">
        <li v-if="content.basics.phone">{{ content.basics.phone }}</li>
        <li v-if="content.basics.email">{{ content.basics.email }}</li>
        <li v-if="content.basics.location">{{ content.basics.location }}</li>
        <li v-if="content.basics.website">{{ content.basics.website }}</li>
      </ul>
    </header>

    <template v-for="section in orderedSections" :key="section">
      <section v-if="section === 'summary' && content.summary.trim()" class="resume-classic__section">
        <h2>个人简介</h2>
        <p class="resume-classic__summary">{{ content.summary }}</p>
      </section>

      <section v-else-if="section === 'experience' && content.experience.length" class="resume-classic__section">
        <h2>工作经历</h2>
        <div v-for="(item, index) in content.experience" :key="`exp-${index}`" class="resume-classic__block">
          <div class="resume-classic__row">
            <strong>{{ item.role || '职位' }} · {{ item.company || '公司' }}</strong>
            <span>{{ period(item.start, item.end) }}</span>
          </div>
          <ul v-if="visibleBullets(item.bullets).length">
            <li v-for="(bullet, bulletIndex) in visibleBullets(item.bullets)" :key="`exp-b-${index}-${bulletIndex}`">
              {{ bullet }}
            </li>
          </ul>
        </div>
      </section>

      <section v-else-if="section === 'projects' && content.projects.length" class="resume-classic__section">
        <h2>项目经历</h2>
        <div v-for="(item, index) in content.projects" :key="`proj-${index}`" class="resume-classic__block">
          <div class="resume-classic__row">
            <strong>
              {{ item.name || '项目名称' }}
              <template v-if="item.role"> · {{ item.role }}</template>
            </strong>
            <span v-if="item.link" class="resume-classic__link">{{ item.link }}</span>
          </div>
          <ul v-if="visibleBullets(item.bullets).length">
            <li v-for="(bullet, bulletIndex) in visibleBullets(item.bullets)" :key="`proj-b-${index}-${bulletIndex}`">
              {{ bullet }}
            </li>
          </ul>
        </div>
      </section>

      <section v-else-if="section === 'education' && content.education.length" class="resume-classic__section">
        <h2>教育经历</h2>
        <div v-for="(item, index) in content.education" :key="`edu-${index}`" class="resume-classic__block">
          <div class="resume-classic__row">
            <strong>{{ item.school || '学校' }} · {{ item.degree || '学历' }}</strong>
            <span>{{ period(item.start, item.end) }}</span>
          </div>
          <p v-if="item.description.trim()" class="resume-classic__desc">{{ item.description }}</p>
        </div>
      </section>

      <section v-else-if="section === 'skills' && visibleSkills.length" class="resume-classic__section">
        <h2>技能</h2>
        <p class="resume-classic__skills">{{ visibleSkills.join(' · ') }}</p>
      </section>
    </template>
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
.resume-classic {
  color: #111827;
  font-family: 'Source Han Sans SC', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  line-height: 1.55;
}

.resume-classic__header {
  display: flex;
  justify-content: space-between;
  gap: 1.25rem;
  padding-bottom: 0.85rem;
  border-bottom: 2px solid #111827;
}

.resume-classic__name {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.2;
}

.resume-classic__title {
  margin: 0.35rem 0 0;
  color: #374151;
  font-size: 14px;
}

.resume-classic__contacts {
  margin: 0;
  padding: 0;
  list-style: none;
  text-align: right;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.7;
}

.resume-classic__section {
  margin-top: 1rem;
}

.resume-classic__section h2 {
  margin: 0 0 0.45rem;
  padding-bottom: 0.2rem;
  border-bottom: 1px solid #d1d5db;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.resume-classic__summary,
.resume-classic__desc,
.resume-classic__skills {
  margin: 0;
  color: #1f2937;
  white-space: pre-wrap;
}

.resume-classic__block + .resume-classic__block {
  margin-top: 0.7rem;
}

.resume-classic__row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: baseline;
}

.resume-classic__row strong {
  font-weight: 650;
}

.resume-classic__row span,
.resume-classic__link {
  color: #6b7280;
  font-size: 12px;
  white-space: nowrap;
}

.resume-classic__block ul {
  margin: 0.35rem 0 0;
  padding-left: 1.1rem;
}

.resume-classic__block li + li {
  margin-top: 0.15rem;
}
</style>
