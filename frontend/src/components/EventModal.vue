<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="panel">
      <div class="panel-header">
        <h2 style="margin:0">{{ isEditing ? 'Modifier' : 'Nouvel' }} évènement</h2>
        <button class="icon secondary" @click="$emit('close')">✕</button>
      </div>

      <form @submit.prevent="save">
        <!-- 1. C'est quoi ? (grosses icones) -->
        <label>C'est quoi ?</label>
        <div class="tile-grid">
          <button
            v-for="c in categories" :key="c.id"
            type="button"
            class="tile"
            :class="{ selected: form.categoryId === c.id }"
            @click="pickCategory(c)"
          >
            <span class="tile-icon">{{ c.icon }}</span>
            <span class="tile-label">{{ c.name }}</span>
          </button>
          <button type="button" class="tile add" @click="showCategoryModal = true">
            <span class="tile-icon">➕</span>
            <span class="tile-label">Nouvelle</span>
          </button>
        </div>

        <!-- 2. Pour qui ? (photos) -->
        <label>Pour qui ?</label>
        <div class="tile-grid persons">
          <button
            type="button"
            class="tile"
            :class="{ selected: form.personId === null }"
            @click="form.personId = null"
          >
            <PersonAvatar :person="null" :size="56" />
            <span class="tile-label">Toute la famille</span>
          </button>
          <button
            v-for="p in persons" :key="p.id"
            type="button"
            class="tile"
            :class="{ selected: form.personId === p.id }"
            :style="form.personId === p.id ? { borderColor: p.color } : {}"
            @click="form.personId = p.id"
          >
            <PersonAvatar :person="p" :size="56" />
            <span class="tile-label">{{ p.firstName }}</span>
          </button>
        </div>
        <p v-if="!persons.length" class="hint">
          Ajoute les membres de la famille depuis l'écran « 👨‍👩‍👧 Famille » pour pouvoir leur associer des évènements.
        </p>

        <label>Titre</label>
        <input v-model="form.title" required placeholder="Ex: Garderie, Rendez-vous..." />

        <!-- 3. Quand ? Un seul jour, ou une periode (ex: vacances du ... au ...) -->
        <label>Quand ?</label>
        <div class="choice-row">
          <button type="button" class="choice" :class="{ selected: !form.isPeriod }" @click="form.isPeriod = false">
            📅 Un seul jour
          </button>
          <button type="button" class="choice" :class="{ selected: form.isPeriod }" @click="setPeriod">
            🗓️ Plusieurs jours
          </button>
        </div>

        <template v-if="form.isPeriod">
          <div class="inline">
            <div style="flex:1">
              <label>Du</label>
              <input v-model="form.startDate" type="date" required />
            </div>
            <div style="flex:1">
              <label>Au (inclus)</label>
              <input v-model="form.endDate" type="date" required :min="form.startDate" />
            </div>
          </div>
          <p v-if="periodDays > 1" class="hint">🗓️ {{ periodDays }} jours</p>
        </template>

        <template v-else>
          <label>Date</label>
          <input v-model="form.startDate" type="date" required />

          <label class="inline" style="margin-top:14px">
            <input v-model="form.allDay" type="checkbox" style="width:auto; min-height:auto" />
            Journée entière
          </label>
        </template>

        <template v-if="!form.allDay">
          <div class="inline">
            <div style="flex:1">
              <label>Heure de début</label>
              <input v-model="form.startTime" type="time" required />
            </div>
            <div style="flex:1">
              <label>Heure de fin (optionnel)</label>
              <input v-model="form.endTime" type="time" />
            </div>
          </div>
        </template>

        <label>Lieu (optionnel)</label>
        <input v-model="form.location" placeholder="Ex: Garderie Lormont" />

        <label>Description (optionnel)</label>
        <textarea v-model="form.description"></textarea>

        <!-- La couleur d'un evenement personnel est celle de la personne -->
        <template v-if="form.personId === null">
          <label>Couleur</label>
          <input v-model="form.color" type="color" style="padding:4px; max-width:100px" />
        </template>

        <label>🔔 Rappel</label>
        <div class="choice-row">
          <button
            v-for="r in reminderChoices" :key="String(r.value)"
            type="button"
            class="choice"
            :class="{ selected: form.reminderMinutes === r.value }"
            @click="form.reminderMinutes = r.value"
          >
            {{ r.label }}
          </button>
        </div>

        <label>Répétition</label>
        <select v-model="form.recurrenceFrequency">
          <option value="NONE">Aucune (une seule fois)</option>
          <option value="DAILY">Tous les jours</option>
          <option value="WEEKLY">Toutes les semaines</option>
          <option value="MONTHLY">Tous les mois</option>
          <option value="YEARLY">Tous les ans</option>
        </select>

        <template v-if="form.recurrenceFrequency !== 'NONE'">
          <label>Tous les combien de {{ intervalUnit }} ?</label>
          <input v-model.number="form.recurrenceInterval" type="number" min="1" />

          <template v-if="form.recurrenceFrequency === 'WEEKLY'">
            <label>Jours concernés</label>
            <div class="checkbox-days">
              <label v-for="d in daysOfWeek" :key="d.code">
                <input v-model="form.selectedDays" type="checkbox" :value="d.code" />
                {{ d.label }}
              </label>
            </div>
          </template>

          <label>Se termine le (optionnel, laisser vide = jamais)</label>
          <input v-model="form.recurrenceEndDate" type="date" />
        </template>

        <p v-if="error" class="error">{{ error }}</p>

        <div class="inline" style="margin-top:18px; justify-content:space-between">
          <button v-if="isEditing" type="button" class="danger" @click="remove">Supprimer</button>
          <div class="inline" style="margin-left:auto">
            <button type="button" class="secondary" @click="$emit('close')">Annuler</button>
            <button type="submit">Enregistrer</button>
          </div>
        </div>
      </form>
    </div>
  </div>

  <CategoryModal
    v-if="showCategoryModal"
    :category="null"
    @close="showCategoryModal = false"
    @saved="onCategoryCreated"
  />
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import api from '../services/api'
import CategoryModal from './CategoryModal.vue'
import PersonAvatar from './PersonAvatar.vue'
import { useCategories } from '../composables/useCategories'
import { usePersons } from '../composables/usePersons'
import { refreshReminders } from '../composables/useReminders'
import { FAMILY_COLOR } from '../utils/color'

const props = defineProps({
  eventId: { type: Number, default: null },
  defaultDate: { type: String, required: true },
  defaultPersonId: { type: Number, default: null }
})
const emit = defineEmits(['close', 'saved', 'deleted'])

const { persons } = usePersons()
const { categories, reload: reloadCategories, byId: categoryById } = useCategories()
const showCategoryModal = ref(false)
const isEditing = computed(() => props.eventId !== null)
const error = ref('')

const daysOfWeek = [
  { code: 'MON', label: 'Lun' }, { code: 'TUE', label: 'Mar' }, { code: 'WED', label: 'Mer' },
  { code: 'THU', label: 'Jeu' }, { code: 'FRI', label: 'Ven' }, { code: 'SAT', label: 'Sam' }, { code: 'SUN', label: 'Dim' }
]

const intervalUnit = computed(() => ({
  DAILY: 'jour(s)', WEEKLY: 'semaine(s)', MONTHLY: 'mois', YEARLY: 'année(s)'
}[form.recurrenceFrequency] || ''))

// Rappel en minutes avant le debut (null = aucun). Journee entiere : compte a partir de 8h.
const TIMED_REMINDERS = [
  { value: null, label: 'Aucun' }, { value: 0, label: "À l'heure" }, { value: 5, label: '5 min avant' },
  { value: 15, label: '15 min avant' }, { value: 30, label: '30 min avant' }, { value: 60, label: '1 h avant' },
  { value: 120, label: '2 h avant' }, { value: 1440, label: 'La veille' }
]
const ALL_DAY_REMINDERS = [
  { value: null, label: 'Aucun' }, { value: 0, label: 'Le matin (8 h)' }, { value: 1440, label: 'La veille (8 h)' }
]

const form = reactive({
  title: '', description: '', location: '', color: FAMILY_COLOR,
  categoryId: null, personId: props.defaultPersonId, reminderMinutes: 0,
  isPeriod: false, endDate: '',
  allDay: true, startDate: props.defaultDate, startTime: '09:00', endTime: '',
  recurrenceFrequency: 'NONE', recurrenceInterval: 1, selectedDays: [], recurrenceEndDate: ''
})

if (isEditing.value) {
  api.getEvent(props.eventId).then(e => {
    form.title = e.title
    form.description = e.description || ''
    form.location = e.location || ''
    form.color = e.color || FAMILY_COLOR
    form.categoryId = e.categoryId ?? null
    form.personId = e.personId ?? null
    form.reminderMinutes = e.reminderMinutes ?? null
    form.allDay = e.allDay
    form.startDate = e.startDate
    form.isPeriod = e.endDate !== null
    form.endDate = e.endDate || ''

    form.startTime = e.startTime ? e.startTime.slice(0, 5) : '09:00'
    form.endTime = e.endTime ? e.endTime.slice(0, 5) : ''
    form.recurrenceFrequency = e.recurrenceFrequency
    form.recurrenceInterval = e.recurrenceInterval
    form.selectedDays = e.recurrenceDaysOfWeek ? e.recurrenceDaysOfWeek.split(',') : []
    form.recurrenceEndDate = e.recurrenceEndDate || ''
  })
}

// Une periode est toujours en journee entiere ; on propose par defaut une semaine
function setPeriod() {
  form.isPeriod = true
  form.allDay = true
  if (!form.endDate || form.endDate <= form.startDate) {
    const [y, m, d] = form.startDate.split('-').map(Number)
    form.endDate = new Date(y, m - 1, d + 6).toLocaleDateString('sv-SE')
  }
}

const periodDays = computed(() => {
  if (!form.startDate || !form.endDate) return 0
  return Math.round((new Date(form.endDate) - new Date(form.startDate)) / 86400000) + 1
})

const reminderChoices = computed(() => (form.allDay ? ALL_DAY_REMINDERS : TIMED_REMINDERS))

// Un evenement avec horaire previent 15 min avant par defaut, une journee entiere le matin
watch(() => form.allDay, allDay => {
  if (allDay && !ALL_DAY_REMINDERS.some(r => r.value === form.reminderMinutes)) {
    form.reminderMinutes = 0
  } else if (!allDay && !isEditing.value && form.reminderMinutes === 0) {
    form.reminderMinutes = 15
  }
})

// Toucher la categorie deja choisie la retire. Choisir une icone remplit le titre
// s'il est vide (ou s'il venait d'une autre icone), et un anniversaire se repete
// tout seul chaque annee.
function pickCategory(c) {
  const previousName = categoryById(form.categoryId)?.name
  if (form.categoryId === c.id) {
    form.categoryId = null
    if (form.title === c.name) form.title = ''
    return
  }
  form.categoryId = c.id
  if (!form.title || form.title === previousName) {
    form.title = c.name
  }
  if (c.icon === '🎂' && !isEditing.value) {
    form.allDay = true
    form.recurrenceFrequency = 'YEARLY'
  }
}

async function onCategoryCreated(created) {
  showCategoryModal.value = false
  await reloadCategories()
  pickCategory(created)
}

async function save() {
  error.value = ''
  if (form.isPeriod && form.endDate <= form.startDate) {
    error.value = 'La date de fin doit être après la date de début.'
    return
  }
  const payload = {
    title: form.title,
    description: form.description || null,
    location: form.location || null,
    color: form.personId === null ? (form.color || null) : null,
    categoryId: form.categoryId,
    personId: form.personId,
    reminderMinutes: form.reminderMinutes,
    allDay: form.isPeriod || form.allDay,
    endDate: form.isPeriod ? form.endDate : null,
    startDate: form.startDate,
    startTime: form.isPeriod || form.allDay ? null : form.startTime,
    endTime: form.isPeriod || form.allDay ? null : (form.endTime || null),
    recurrenceFrequency: form.recurrenceFrequency,
    recurrenceInterval: form.recurrenceFrequency === 'NONE' ? 1 : form.recurrenceInterval,
    recurrenceDaysOfWeek: form.recurrenceFrequency === 'WEEKLY' && form.selectedDays.length ? form.selectedDays.join(',') : null,
    recurrenceEndDate: form.recurrenceFrequency === 'NONE' ? null : (form.recurrenceEndDate || null)
  }
  try {
    if (isEditing.value) {
      await api.updateEvent(props.eventId, payload)
    } else {
      await api.createEvent(payload)
    }
    refreshReminders()
    emit('saved')
  } catch (e) {
    error.value = e.response?.data?.error ?? "Erreur lors de l'enregistrement."
  }
}

async function remove() {
  await api.deleteEvent(props.eventId)
  emit('deleted')
}
</script>

<style scoped>
.choice-row { display: flex; flex-wrap: wrap; gap: 8px; }
.choice {
  background: var(--surface-2); color: var(--text); border: 3px solid transparent;
  border-radius: 24px; padding: 10px 16px;
}
.choice.selected { border-color: var(--primary); background: var(--primary-soft); }
</style>
