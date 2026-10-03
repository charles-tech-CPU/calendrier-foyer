<template>
  <header class="topbar">
    <button class="secondary" @click="router.push('/')">📅 Calendrier</button>
    <h1>🏷️ Les catégories</h1>
    <button @click="openNew">➕ Ajouter</button>
  </header>

  <main>
    <p class="hint">Touche une catégorie pour changer son nom ou son icône.</p>
    <div class="tile-grid big">
      <button v-for="c in categories" :key="c.id" class="tile" @click="openEdit(c)">
        <span class="tile-icon">{{ c.icon }}</span>
        <span class="tile-label">{{ c.name }}</span>
      </button>
      <button class="tile add" @click="openNew">
        <span class="tile-icon">➕</span>
        <span class="tile-label">Nouvelle</span>
      </button>
    </div>
  </main>

  <CategoryModal
    v-if="showModal"
    :category="editing"
    @close="showModal = false"
    @saved="onChanged"
    @deleted="onChanged"
  />
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import CategoryModal from '../components/CategoryModal.vue'
import { useCategories } from '../composables/useCategories'

const router = useRouter()
const { categories, reload } = useCategories()
const showModal = ref(false)
const editing = ref(null)

function openNew() {
  editing.value = null
  showModal.value = true
}
function openEdit(c) {
  editing.value = c
  showModal.value = true
}
async function onChanged() {
  showModal.value = false
  await reload()
}
</script>

<style scoped>
.tile-grid.big { grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 12px; }
.tile-grid.big .tile { min-height: 130px; background: var(--surface); box-shadow: var(--shadow); }
.tile-grid.big .tile-icon { font-size: 56px; }
.tile-grid.big .tile-label { font-size: 15px; }
</style>
