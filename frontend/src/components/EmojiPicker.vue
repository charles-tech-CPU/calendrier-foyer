<template>
  <!-- Bibliotheque d'icones (~1600 emojis), avec recherche en francais et onglets par theme -->
  <div class="picker">
    <label for="emoji-search" class="sr-only">Chercher une icône</label>
    <input id="emoji-search" v-model="search" type="search" placeholder="🔍 Chercher une icône : chien, piscine, gâteau, voiture…" />

    <div v-if="!search" class="group-tabs">
      <button
        v-for="g in groups" :key="g.id"
        type="button"
        class="group-tab"
        :class="{ selected: g.id === groupId }"
        :title="g.label"
        @click="groupId = g.id"
      >
        {{ GROUP_ICONS[g.id] || '•' }}
        <span>{{ g.label }}</span>
      </button>
    </div>

    <p v-if="loading" class="hint">Chargement des icônes…</p>
    <p v-else-if="!visible.length" class="hint">Aucune icône trouvée pour « {{ search }} ».</p>

    <div class="emoji-grid">
      <button
        v-for="e in visible" :key="e.e"
        type="button"
        class="emoji"
        :class="{ selected: e.e === modelValue }"
        :title="e.l"
        @click="$emit('update:modelValue', e.e)"
      >
        {{ e.e }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, shallowRef } from 'vue'

defineProps({ modelValue: { type: String, default: '' } })
defineEmits(['update:modelValue'])

// Icone representant chaque onglet (numeros de groupe Unicode)
const GROUP_ICONS = { 0: '😀', 1: '🧑', 3: '🐶', 4: '🍔', 5: '🚗', 6: '⚽', 7: '💡', 8: '❤️' }
const MAX_RESULTS = 300

const groups = ref([])
const emojis = shallowRef([])
const groupId = ref(6)
const search = ref('')
const loading = ref(true)

// Charge la bibliotheque seulement quand le selecteur s'ouvre (fichier separe dans le build)
onMounted(async () => {
  const data = (await import('../assets/emojis-fr.json')).default
  groups.value = data.groups
  emojis.value = data.emojis.map(e => ({ ...e, search: normalize(`${e.l} ${e.t}`) }))
  loading.value = false
})

// Recherche insensible aux accents et majuscules ("gateau" trouve "gâteau")
function normalize(text) {
  return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

const visible = computed(() => {
  const q = normalize(search.value.trim())
  if (q) {
    const words = q.split(/\s+/)
    return emojis.value.filter(e => words.every(w => e.search.includes(w))).slice(0, MAX_RESULTS)
  }
  return emojis.value.filter(e => e.g === groupId.value)
})
</script>

<style scoped>
.picker { display: flex; flex-direction: column; gap: 10px; }
.group-tabs { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 4px; }
.group-tab {
  display: flex; align-items: center; gap: 6px; flex-shrink: 0;
  background: var(--surface-2); color: var(--text); border: 2px solid transparent;
  font-size: 20px; padding: 6px 12px;
}
.group-tab span { font-size: 13px; display: inline-block; }
.group-tab span::first-letter { text-transform: uppercase; }
.group-tab.selected { border-color: var(--primary); background: var(--primary-soft); }
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(56px, 1fr));
  gap: 4px;
  max-height: 320px;
  overflow-y: auto;
  padding: 4px;
  background: var(--surface-2);
  border-radius: 12px;
}
.emoji {
  font-size: 32px; line-height: 1; padding: 6px 0; min-height: 56px;
  background: transparent; border: 3px solid transparent; border-radius: 12px; color: inherit;
}
.emoji:hover { background: var(--surface); }
.emoji.selected { border-color: var(--primary); background: var(--surface); }
</style>
