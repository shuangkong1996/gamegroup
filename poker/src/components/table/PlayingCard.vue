<script setup lang="ts">
import { computed } from 'vue'
import { parseCard } from '@/shared/poker'

const props = defineProps<{
  code?: string
  hidden?: boolean
  small?: boolean
}>()

const card = computed(() => (props.code ? parseCard(props.code) : null))
</script>

<template>
  <div
    v-if="hidden"
    class="playing-card playing-card-back"
    :class="small ? 'w-9 h-[54px] rounded-md' : 'w-14 h-20 rounded-lg'"
  ></div>
  <div
    v-else-if="card"
    class="playing-card bg-white"
    :class="[card.red ? 'text-red-600' : 'text-slate-900', small ? 'w-9 h-[54px] rounded-md' : 'w-14 h-20 rounded-lg']"
  >
    <span class="leading-none font-bold" :class="small ? 'text-sm' : 'text-lg'">{{ card.rankLabel }}</span>
    <span class="leading-none" :class="small ? 'text-xs' : 'text-base'">{{ card.suitSymbol }}</span>
  </div>
  <div
    v-else
    class="playing-card border border-dashed border-line bg-slate-800/40"
    :class="small ? 'w-9 h-[54px] rounded-md' : 'w-14 h-20 rounded-lg'"
  ></div>
</template>

<style scoped>
.playing-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  box-shadow: 0 4px 14px rgb(0 0 0 / 0.35);
}

.playing-card-back {
  background: repeating-linear-gradient(
    45deg,
    #1e3a5f,
    #1e3a5f 4px,
    #16283f 4px,
    #16283f 8px
  );
  border: 1px solid #334155;
}
</style>
