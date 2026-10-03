<template>
  <header class="topbar">
    <h1>📅 Calendrier Foyer</h1>
    <div class="month-nav">
      <button class="icon secondary" @click="previousMonth">‹</button>
      <div class="month-label">{{ monthLabel }}</div>
      <button class="icon secondary" @click="nextMonth">›</button>
    </div>
    <div class="inline">
      <button class="secondary" @click="goToday">Aujourd'hui</button>
      <button class="secondary" @click="router.push('/famille')">👨‍👩‍👧 Famille</button>
      <button class="secondary" @click="router.push('/categories')">🏷️ Catégories</button>
      <button @click="openNewEvent(today)">➕ Évènement</button>
    </div>
  </header>

  <!-- Filtre par personne : on touche sa photo pour ne voir que ses evenements (+ ceux de toute la famille).
       Les etoiles comptent les taches cochees "c'est fait" ce mois-ci. -->
  <nav v-if="persons.length" class="person-filter">
    <button class="filter-chip" :class="{ selected: selectedPersonId === null }" @click="selectedPersonId = null">
      <PersonAvatar :person="null" :size="52" />
      <span>Tout le monde</span>
    </button>
    <button
      v-for="p in persons" :key="p.id"
      class="filter-chip"
      :class="{ selected: selectedPersonId === p.id }"
      :style="selectedPersonId === p.id ? { borderColor: p.color, background: p.color + '22' } : {}"
      @click="selectedPersonId = selectedPersonId === p.id ? null : p.id"
    >
      <PersonAvatar :person="p" :size="52" />
      <span>{{ p.firstName }}</span>
      <span v-if="starsByPerson[p.id]" class="stars">⭐ {{ starsByPerson[p.id] }}</span>
    </button>
  </nav>

  <main>
    <div class="calendar-grid">
      <div v-for="w in weekdayLabels" :key="w" class="weekday-label">{{ w }}</div>
      <div
        v-for="day in gridDays" :key="day.iso"
        class="day-cell"
        :class="{ 'outside-month': !day.inCurrentMonth, today: day.iso === today }"
        role="button"
        tabindex="0"
        @click="openDayPanel(day.iso)"
        @keydown.enter="openDayPanel(day.iso)"
      >
        <div class="day-number">{{ day.date.getDate() }}</div>
        <div
          v-for="occ in (occurrencesByDay[day.iso] || []).slice(0, 3)"
          :key="occKey(occ)"
          class="event-pill"
          :class="[{ done: occ.done }, spanClasses(occ, day)]"
          :style="pillStyle(occ)"
        >
          <span class="pill-icon">{{ occ.done ? '✅' : (occ.icon || DEFAULT_ICON) }}</span>
          <span v-if="showPillText(occ, day)" class="pill-text">
            {{ occ.allDay ? '' : formatTime(occ.startTime) + ' ' }}{{ occ.title }}
          </span>
        </div>
        <div v-if="(occurrencesByDay[day.iso] || []).length > 3" class="event-pill-more">
          + {{ (occurrencesByDay[day.iso] || []).length - 3 }} autre(s)
        </div>
      </div>
    </div>
  </main>

  <!-- Panneau des evenements du jour selectionne -->
  <div v-if="selectedDay" class="overlay" @click.self="selectedDay = null" @keydown.esc="selectedDay = null">
    <div class="panel">
      <div class="panel-header">
        <h2 class="date-title" style="margin:0">{{ formatFullDate(selectedDay) }}</h2>
        <button class="icon secondary" @click="selectedDay = null">✕</button>
      </div>

      <p v-if="!(occurrencesByDay[selectedDay] || []).length" class="empty-hint">
        Aucun évènement ce jour-là.
      </p>

      <div
        v-for="occ in occurrencesByDay[selectedDay] || []"
        :key="occKey(occ)"
        class="event-row"
        :class="{ done: occ.done }"
        :style="{ borderLeftColor: occ.color || FAMILY_COLOR }"
        role="button"
        tabindex="0"
        @click="openOccurrence(occ)"
        @keydown.enter.self="openOccurrence(occ)"
      >
        <div class="event-row-icon">{{ occ.icon || DEFAULT_ICON }}</div>
        <PersonAvatar :person="byId(occ.personId)" :size="48" />
        <div style="flex:1; min-width:0">
          <div class="event-title">{{ occ.title }}<span v-if="occ.recurring"> 🔁</span></div>
          <div v-if="occ.spanStart" class="event-time">
            🗓️ Du {{ formatShortDate(occ.spanStart) }} au {{ formatShortDate(occ.spanEnd) }}
            · jour {{ dayNumberInSpan(occ) }} sur {{ spanLength(occ) }}
            <span v-if="occ.reminderMinutes !== null"> · 🔔</span>
          </div>
          <div v-else class="event-time">
            <template v-if="occ.allDay">☀️ Toute la journée</template>
            <template v-else>
              🕘 {{ formatTime(occ.startTime) }}<span v-if="occ.endTime"> → {{ formatTime(occ.endTime) }}</span>
            </template>
            <span v-if="occ.reminderMinutes !== null"> · 🔔</span>
          </div>
          <div v-if="occ.location" class="event-time">📍 {{ occ.location }}</div>
        </div>
        <button
          v-if="canSpeak()"
          class="round-button"
          title="Écouter"
          @click.stop="speak(describeOccurrence(occ, byId(occ.personId)))"
        >🔊</button>
        <button
          v-if="occ.eventId !== null"
          class="round-button check"
          :class="{ checked: occ.done }"
          :title="occ.done ? 'Fait !' : 'Marquer comme fait'"
          @click.stop="toggleDone(occ)"
        >✔</button>
      </div>

      <button style="margin-top:16px; width:100%" @click="openNewEvent(selectedDay)">➕ Ajouter un évènement</button>
    </div>
  </div>

  <EventModal
    v-if="showModal"
    :event-id="editingEventId"
    :default-date="modalDefaultDate"
    :default-person-id="selectedPersonId"
    @close="showModal = false"
    @saved="onSavedOrDeleted"
    @deleted="onSavedOrDeleted"
  />
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import EventModal from '../components/EventModal.vue'
import PersonAvatar from '../components/PersonAvatar.vue'
import { DEFAULT_ICON } from '../composables/useCategories'
import { usePersons } from '../composables/usePersons'
import { dismiss, refreshReminders } from '../composables/useReminders'
import { FAMILY_COLOR, textColorOn } from '../utils/color'
import { canSpeak, describeOccurrence, speak } from '../utils/speech'

// Sur une tablette murale la page reste ouverte des jours : on recharge
// regulierement (modifs faites depuis un autre appareil, passage a minuit).
const AUTO_REFRESH_MS = 5 * 60 * 1000

const router = useRouter()
const { persons, byId } = usePersons()
// null = tout le monde ; sinon on ne montre que cette personne + les evenements de toute la famille
const selectedPersonId = ref(null)

const weekdayLabels = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim']
const current = ref(startOfMonth(new Date()))
const today = ref(toIso(new Date()))
const occurrences = ref([])
const selectedDay = ref(null)
const showModal = ref(false)
const editingEventId = ref(null)
const modalDefaultDate = ref('')

function startOfMonth(d) {
  return new Date(d.getFullYear(), d.getMonth(), 1)
}
function toIso(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const monthLabel = computed(() => current.value.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' }))

const gridDays = computed(() => {
  const year = current.value.getFullYear()
  const month = current.value.getMonth()
  const firstOfMonth = new Date(year, month, 1)
  const startOffset = (firstOfMonth.getDay() + 6) % 7 // lundi = 0
  const gridStart = new Date(year, month, 1 - startOffset)
  const days = []
  for (let i = 0; i < 42; i++) {
    const d = new Date(gridStart)
    d.setDate(gridStart.getDate() + i)
    days.push({ date: d, iso: toIso(d), inCurrentMonth: d.getMonth() === month })
  }
  return days
})

const occurrencesByDay = computed(() => {
  const map = {}
  const visible = selectedPersonId.value === null
    ? occurrences.value
    : occurrences.value.filter(o => o.personId === selectedPersonId.value || o.personId === null)
  for (const occ of visible) {
    if (!map[occ.date]) map[occ.date] = []
    map[occ.date].push(occ)
  }
  return map
})

// Une etoile par tache cochee "c'est fait" dans le mois affiche
const starsByPerson = computed(() => {
  const monthPrefix = toIso(current.value).slice(0, 7)
  const stars = {}
  for (const occ of occurrences.value) {
    if (occ.done && occ.personId !== null && occ.date.startsWith(monthPrefix)) {
      stars[occ.personId] = (stars[occ.personId] || 0) + 1
    }
  }
  return stars
})

async function load() {
  const days = gridDays.value
  occurrences.value = await api.getOccurrences(days[0].date, days[days.length - 1].date)
}

function previousMonth() {
  current.value = new Date(current.value.getFullYear(), current.value.getMonth() - 1, 1)
}
function nextMonth() {
  current.value = new Date(current.value.getFullYear(), current.value.getMonth() + 1, 1)
}
function goToday() {
  current.value = startOfMonth(new Date())
}

function openDayPanel(iso) {
  selectedDay.value = iso
}
function openNewEvent(iso) {
  editingEventId.value = null
  modalDefaultDate.value = iso
  showModal.value = true
}
// Un anniversaire automatique n'est pas un evenement modifiable : il vient de la fiche de la personne
function openOccurrence(occ) {
  if (occ.eventId === null) {
    router.push('/famille')
  } else {
    editingEventId.value = occ.eventId
    modalDefaultDate.value = selectedDay.value
    showModal.value = true
  }
}
function onSavedOrDeleted() {
  showModal.value = false
  selectedDay.value = null
  load()
}

// Coche / decoche "c'est fait" (mise a jour immediate a l'ecran, annulee si le serveur refuse)
async function toggleDone(occ) {
  const done = !occ.done
  occ.done = done
  try {
    if (done) {
      await api.markDone(occ.eventId, occ.date)
      dismiss(`${occ.eventId}:${occ.date}`)
      speak('Bravo !')
    } else {
      await api.unmarkDone(occ.eventId, occ.date)
    }
    refreshReminders()
  } catch {
    occ.done = !done
  }
}

function occKey(occ) {
  return occ.eventId === null ? `bday-${occ.personId}-${occ.date}` : `${occ.eventId}-${occ.date}`
}
function pillStyle(occ) {
  const bg = occ.color || FAMILY_COLOR
  return { background: bg, color: textColorOn(bg) }
}
// Periode sur plusieurs jours : la bande se prolonge vers le jour voisin (dans la meme semaine)
function spanClasses(occ, day) {
  if (!occ.spanStart) return []
  const weekday = (day.date.getDay() + 6) % 7 // lundi = 0
  return [
    'span',
    occ.spanStart < day.iso && weekday > 0 ? 'continues-left' : '',
    occ.spanEnd > day.iso && weekday < 6 ? 'continues-right' : ''
  ]
}
// Le titre d'une periode s'affiche au premier jour, puis au debut de chaque semaine
function showPillText(occ, day) {
  if (!occ.spanStart) return true
  return occ.spanStart === day.iso || day.date.getDay() === 1 || day.iso === gridDays.value[0].iso
}
function daysBetween(a, b) {
  const [y1, m1, d1] = a.split('-').map(Number)
  const [y2, m2, d2] = b.split('-').map(Number)
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000)
}
function dayNumberInSpan(occ) {
  return daysBetween(occ.spanStart, occ.date) + 1
}
function spanLength(occ) {
  return daysBetween(occ.spanStart, occ.spanEnd) + 1
}
function formatShortDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })
}

function formatTime(t) {
  return t ? t.slice(0, 5) : ''
}
function formatFullDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })
}

let refreshTimer = null
function autoRefresh() {
  const nowIso = toIso(new Date())
  if (nowIso !== today.value) {
    // Passage a un nouveau jour : on suit (y compris le changement de mois)
    const wasOnCurrentMonth = toIso(current.value).slice(0, 7) === today.value.slice(0, 7)
    today.value = nowIso
    if (wasOnCurrentMonth) goToday()
  }
  if (!showModal.value) load()
}

watch(current, load)
onMounted(() => {
  load()
  refreshTimer = setInterval(autoRefresh, AUTO_REFRESH_MS)
})
onBeforeUnmount(() => clearInterval(refreshTimer))
</script>
