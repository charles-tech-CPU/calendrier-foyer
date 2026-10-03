import { ref } from 'vue'
import api from '../services/api'

// Categories (nom + icone) creees par le foyer, partagees par tous les ecrans.
const categories = ref([])
let loaded = false

// Icone affichee pour un evenement sans categorie
export const DEFAULT_ICON = '📌'

async function reload() {
  categories.value = await api.getCategories()
  loaded = true
}

function byId(id) {
  return categories.value.find(c => c.id === id) || null
}

export function useCategories() {
  if (!loaded) reload()
  return { categories, reload, byId }
}
