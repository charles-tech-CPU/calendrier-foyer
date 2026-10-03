import { onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../services/api'

// Rappels affiches dans l'appli (en bas a droite) quand un evenement approche.
// Tant que la page est ouverte (ex: tablette murale), on surveille les
// evenements des prochains jours et on affiche un rappel "reminderMinutes"
// avant le debut (8h du matin pour un evenement journee entiere).

const CHECK_EVERY_MS = 30 * 1000
const RELOAD_EVERY_MS = 2 * 60 * 1000
// Un rappel rate (tablette en veille...) reste affichable pendant ce delai
const LATE_TOLERANCE_MS = 30 * 60 * 1000
const ALL_DAY_HOUR = 8
const STORAGE_KEY = 'calendrier-foyer.rappels-vus'

const toasts = ref([])
let reloadNow = null

// A appeler apres l'enregistrement d'un evenement, pour prendre en compte son rappel sans attendre
export function refreshReminders() {
  if (reloadNow) void reloadNow()
}

export function startOf(occ) {
  const [y, m, d] = occ.date.split('-').map(Number)
  if (occ.allDay) return new Date(y, m - 1, d, ALL_DAY_HOUR, 0)
  const [h, min] = occ.startTime.split(':').map(Number)
  return new Date(y, m - 1, d, h, min)
}

// Rappels deja affiches (survit a un rechargement de la page) ; on oublie ceux de plus de 3 jours
function loadSeen() {
  try {
    const seen = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    const limit = Date.now() - 3 * 24 * 3600 * 1000
    return Object.fromEntries(Object.entries(seen).filter(([, t]) => t > limit))
  } catch {
    return {}
  }
}
function saveSeen(seen) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(seen))
  } catch {
    // stockage indisponible (navigation privee...) : tant pis, pas de memoire entre rechargements
  }
}

// Petit carillon (deux notes) genere par le navigateur, sans fichier son
function chime() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    ;[880, 1175].forEach((freq, i) => {
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.frequency.value = freq
      const t = ctx.currentTime + i * 0.25
      gain.gain.setValueAtTime(0.0001, t)
      gain.gain.exponentialRampToValueAtTime(0.3, t + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.6)
      osc.connect(gain).connect(ctx.destination)
      osc.start(t)
      osc.stop(t + 0.65)
    })
  } catch {
    // son indisponible : le rappel visuel suffit
  }
}

export function dismiss(key) {
  toasts.value = toasts.value.filter(t => t.key !== key)
}

export function useReminderToasts() {
  return { toasts, dismiss }
}

// A appeler une seule fois, dans App.vue
export function useReminders() {
  let occurrences = []
  let checkTimer = null
  let reloadTimer = null

  async function reload() {
    const today = new Date()
    const until = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 2)
    try {
      occurrences = await api.getOccurrences(new Date(today.getFullYear(), today.getMonth(), today.getDate()), until)
      check()
    } catch {
      // backend injoignable : on reessaiera au prochain rechargement
    }
  }

  function check() {
    const now = Date.now()
    const seen = loadSeen()
    let added = false
    for (const occ of occurrences) {
      if (occ.reminderMinutes === null || occ.reminderMinutes === undefined || occ.done) continue
      const key = `${occ.eventId}:${occ.date}`
      if (seen[key]) continue
      const trigger = startOf(occ).getTime() - occ.reminderMinutes * 60 * 1000
      if (now >= trigger && now < trigger + LATE_TOLERANCE_MS) {
        seen[key] = now
        toasts.value = [...toasts.value, { key, occ }]
        added = true
      }
    }
    if (added) {
      saveSeen(seen)
      chime()
    }
  }

  onMounted(() => {
    reloadNow = reload
    void reload()
    checkTimer = setInterval(check, CHECK_EVERY_MS)
    reloadTimer = setInterval(reload, RELOAD_EVERY_MS)
  })
  onBeforeUnmount(() => {
    clearInterval(checkTimer)
    clearInterval(reloadTimer)
    reloadNow = null
  })
}
