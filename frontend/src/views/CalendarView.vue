<template>
  <header class="topbar">
    <h1>📅 Calendrier Foyer</h1>
    <div class="month-nav">
      <button class="icon secondary" @click="previousMonth">‹</button>
      <div class="month-label">{{ monthLabel }}</div>
      <button class="icon secondary" @click="nextMonth">›</button>
    </div>
    <button @click="openNewEvent(todayIso())">+ Évènement</button>
  </header>

  <main>
    <div class="calendar-grid">
      <div class="weekday-label" v-for="w in weekdayLabels" :key="w">{{ w }}</div>
      <div
        v-for="day in gridDays" :key="day.iso"
        class="day-cell"
        :class="{ 'outside-month': !day.inCurrentMonth, today: day.iso === todayIso() }"
        @click="openDayPanel(day.iso)"
      >
        <div class="day-number">{{ day.date.getDate() }}</div>
        <div
          v-for="occ in (occurrencesByDay[day.iso] || []).slice(0, 3)"
          :key="occ.eventId + '-' + occ.date"
          class="event-pill"
          :style="{ background: occ.color || '#2b6cb0' }"
        >
          {{ occ.allDay ? '' : formatTime(occ.startTime) + ' ' }}{{ occ.title }}
        </div>
        <div v-if="(occurrencesByDay[day.iso] || []).length > 3" class="event-pill-more">
          + {{ (occurrencesByDay[day.iso] || []).length - 3 }} autre(s)
        </div>
      </div>
    </div>
  </main>

  <!-- Panneau des evenements du jour selectionne -->
  <div class="overlay" v-if="selectedDay" @click.self="selectedDay = null">
    <div class="panel">
      <div class="panel-header">
        <h2 style="margin:0">{{ formatFullDate(selectedDay) }}</h2>
        <button class="icon secondary" @click="selectedDay = null">✕</button>
      </div>

      <p v-if="!(occurrencesByDay[selectedDay] || []).length" class="empty-hint">
        Aucun évènement ce jour-là.
      </p>

      <div
        v-for="occ in occurrencesByDay[selectedDay] || []"
        :key="occ.eventId"
        class="event-row"
        @click="openEditEvent(occ.eventId)"
      >
        <div class="event-color-dot" :style="{ background: occ.color || '#2b6cb0' }"></div>
        <div>
          <div class="event-title">{{ occ.title }}<span v-if="occ.recurring"> 🔁</span></div>
          <div class="event-time" v-if="!occ.allDay">
            {{ formatTime(occ.startTime) }}<span v-if="occ.endTime"> - {{ formatTime(occ.endTime) }}</span>
          </div>
          <div class="event-time" v-if="occ.location">{{ occ.location }}</div>
        </div>
      </div>

      <button style="margin-top:16px; width:100%" @click="openNewEvent(selectedDay)">+ Ajouter un évènement</button>
    </div>
  </div>

  <EventModal
    v-if="showModal"
    :event-id="editingEventId"
    :default-date="modalDefaultDate"
    @close="showModal = false"
    @saved="onSavedOrDeleted"
    @deleted="onSavedOrDeleted"
  />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import EventModal from '../components/EventModal.vue'

const weekdayLabels = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim']
const current = ref(startOfMonth(new Date()))
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
function todayIso() {
  return toIso(new Date())
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
  for (const occ of occurrences.value) {
    if (!map[occ.date]) map[occ.date] = []
    map[occ.date].push(occ)
  }
  return map
})

async function load() {
  const days = gridDays.value
  const from = days[0].date
  const to = days[days.length - 1].date
  occurrences.value = await api.getOccurrences(from, to)
}

function previousMonth() {
  current.value = new Date(current.value.getFullYear(), current.value.getMonth() - 1, 1)
}
function nextMonth() {
  current.value = new Date(current.value.getFullYear(), current.value.getMonth() + 1, 1)
}

function openDayPanel(iso) {
  selectedDay.value = iso
}
function openNewEvent(iso) {
  editingEventId.value = null
  modalDefaultDate.value = iso
  showModal.value = true
}
function openEditEvent(eventId) {
  editingEventId.value = eventId
  modalDefaultDate.value = selectedDay.value
  showModal.value = true
}
function onSavedOrDeleted() {
  showModal.value = false
  selectedDay.value = null
  load()
}

function formatTime(t) {
  return t ? t.slice(0, 5) : ''
}
function formatFullDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })
}

watch(current, load)
onMounted(load)
</script>
