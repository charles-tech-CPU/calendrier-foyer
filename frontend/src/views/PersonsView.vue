<template>
  <header class="topbar">
    <button class="secondary big-nav" @click="router.push('/')">📅 Calendrier</button>
    <h1>👨‍👩‍👧 La famille</h1>
    <button class="big-nav" @click="openNew">➕ Ajouter</button>
  </header>

  <main>
    <p v-if="!persons.length" class="empty-hint big">
      Personne pour l'instant.<br />Touche « ➕ Ajouter » pour créer la première personne.
    </p>

    <div class="person-grid">
      <button
        v-for="p in persons" :key="p.id"
        class="person-card"
        :style="{ borderColor: p.color }"
        @click="openEdit(p)"
      >
        <PersonAvatar :person="p" :size="128" />
        <div class="person-name">{{ p.firstName }}</div>
        <div class="person-sub">{{ p.lastName }}</div>
        <div class="person-sub">🎂 {{ formatBirth(p.birthDate) }} · {{ age(p.birthDate) }} ans</div>
      </button>
    </div>
  </main>

  <PersonModal
    v-if="showModal"
    :person="editing"
    :used-colors="usedColors"
    @close="showModal = false"
    @saved="onChanged"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import PersonAvatar from '../components/PersonAvatar.vue'
import PersonModal from '../components/PersonModal.vue'
import { usePersons } from '../composables/usePersons'

const router = useRouter()
const { persons, reload } = usePersons()
const showModal = ref(false)
const editing = ref(null)

// Couleurs deja prises par les AUTRES personnes (la sienne reste choisissable)
const usedColors = computed(() =>
  persons.value.filter(p => !editing.value || p.id !== editing.value.id).map(p => p.color)
)

function openNew() {
  editing.value = null
  showModal.value = true
}
function openEdit(p) {
  editing.value = p
  showModal.value = true
}
async function onChanged() {
  showModal.value = false
  await reload()
}

function parse(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}
function formatBirth(iso) {
  return parse(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })
}
function age(iso) {
  const birth = parse(iso)
  const today = new Date()
  let a = today.getFullYear() - birth.getFullYear()
  if (today.getMonth() < birth.getMonth() || (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())) {
    a--
  }
  return a
}
</script>

<style scoped>
.person-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 12px;
}
.person-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 22px 14px;
  background: var(--surface);
  border: 4px solid;
  border-radius: 20px;
  color: var(--text);
  box-shadow: var(--shadow);
}
.person-name { font-size: 26px; font-weight: 800; margin-top: 8px; }
.person-sub { font-size: 15px; color: var(--text-muted); }
.empty-hint.big { font-size: 20px; line-height: 1.6; }
</style>
