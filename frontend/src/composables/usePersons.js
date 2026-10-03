import { ref } from 'vue'
import api from '../services/api'

// Liste des personnes partagee par tous les ecrans (chargee une fois,
// rechargee apres chaque modification sur l'ecran Famille).
const persons = ref([])
// Change a chaque modification de photo, pour forcer le navigateur a la recharger
const photoVersion = ref(Date.now())
let loaded = false

async function reload() {
  persons.value = await api.getPersons()
  photoVersion.value = Date.now()
  loaded = true
}

function byId(id) {
  return persons.value.find(p => p.id === id) || null
}

function photoUrl(person) {
  return person && person.hasPhoto ? api.photoUrl(person.id, photoVersion.value) : null
}

export function usePersons() {
  if (!loaded) reload()
  return { persons, reload, byId, photoUrl }
}
