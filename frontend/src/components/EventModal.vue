<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="panel">
      <div class="panel-header">
        <h2 style="margin:0">{{ isEditing ? 'Modifier' : 'Nouvel' }} évènement</h2>
        <button class="icon secondary" @click="$emit('close')">✕</button>
      </div>

      <form @submit.prevent="save">
        <label>Titre</label>
        <input v-model="form.title" required placeholder="Ex: Garderie, Rendez-vous..." />

        <label class="inline" style="margin-top:14px">
          <input v-model="form.allDay" type="checkbox" style="width:auto; min-height:auto" />
          Journée entière
        </label>

        <label>Date</label>
        <input v-model="form.startDate" type="date" required />

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

        <label>Couleur</label>
        <input v-model="form.color" type="color" style="padding:4px; max-width:100px" />

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

        <p v-if="error" style="color:#ff6b6b">{{ error }}</p>

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
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import api from '../services/api'

const props = defineProps({
  eventId: { type: Number, default: null },
  defaultDate: { type: String, required: true }
})
const emit = defineEmits(['close', 'saved', 'deleted'])

const isEditing = computed(() => props.eventId !== null)
const error = ref('')

const daysOfWeek = [
  { code: 'MON', label: 'Lun' }, { code: 'TUE', label: 'Mar' }, { code: 'WED', label: 'Mer' },
  { code: 'THU', label: 'Jeu' }, { code: 'FRI', label: 'Ven' }, { code: 'SAT', label: 'Sam' }, { code: 'SUN', label: 'Dim' }
]

const intervalUnit = computed(() => ({
  DAILY: 'jour(s)', WEEKLY: 'semaine(s)', MONTHLY: 'mois', YEARLY: 'année(s)'
}[form.recurrenceFrequency] || ''))

const form = reactive({
  title: '', description: '', location: '', color: '#4a90d9',
  allDay: true, startDate: props.defaultDate, startTime: '09:00', endTime: '',
  recurrenceFrequency: 'NONE', recurrenceInterval: 1, selectedDays: [], recurrenceEndDate: ''
})

if (isEditing.value) {
  api.getEvent(props.eventId).then(e => {
    form.title = e.title
    form.description = e.description || ''
    form.location = e.location || ''
    form.color = e.color || '#4a90d9'
    form.allDay = e.allDay
    form.startDate = e.startDate
    form.startTime = e.startTime ? e.startTime.slice(0, 5) : '09:00'
    form.endTime = e.endTime ? e.endTime.slice(0, 5) : ''
    form.recurrenceFrequency = e.recurrenceFrequency
    form.recurrenceInterval = e.recurrenceInterval
    form.selectedDays = e.recurrenceDaysOfWeek ? e.recurrenceDaysOfWeek.split(',') : []
    form.recurrenceEndDate = e.recurrenceEndDate || ''
  })
}

async function save() {
  error.value = ''
  const payload = {
    title: form.title,
    description: form.description || null,
    location: form.location || null,
    color: form.color || null,
    allDay: form.allDay,
    startDate: form.startDate,
    startTime: form.allDay ? null : form.startTime,
    endTime: form.allDay ? null : (form.endTime || null),
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
