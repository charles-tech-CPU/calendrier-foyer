<template>
  <div class="overlay" @click.self="$emit('close')" @keydown.esc="$emit('close')">
    <div class="panel">
      <div class="panel-header">
        <h2 style="margin:0">{{ isEditing ? 'Modifier' : 'Nouvelle personne' }}</h2>
        <button class="icon secondary" @click="$emit('close')">✕</button>
      </div>

      <form @submit.prevent="save">
        <!-- Photo : apercu + boutons -->
        <div class="photo-zone">
          <div class="photo-preview" :style="{ borderColor: form.color, background: form.color }">
            <img v-if="previewUrl" :src="previewUrl" alt="" />
            <span v-else :style="{ color: textColorOn(form.color) }">{{ (form.firstName || '?').charAt(0).toUpperCase() }}</span>
          </div>
          <div class="photo-actions">
            <label class="file-button">
              📷 {{ previewUrl ? 'Changer la photo' : 'Ajouter une photo' }}
              <input type="file" accept="image/*" @change="onPhotoPicked" />
            </label>
            <button v-if="previewUrl" type="button" class="secondary" @click="removePhoto">🗑️ Enlever la photo</button>
            <small>Facultatif</small>
          </div>
        </div>

        <label for="person-1">Prénom</label>
        <input id="person-1" v-model="form.firstName" required maxlength="80" placeholder="Ex: Caitlyn" />

        <label for="person-2">Nom</label>
        <input id="person-2" v-model="form.lastName" required maxlength="80" />

        <label for="person-3">Date de naissance</label>
        <input id="person-3" v-model="form.birthDate" type="date" required :max="todayIso" />

        <label>Couleur</label>
        <div class="swatches">
          <button
            v-for="c in PERSON_PALETTE" :key="c"
            type="button"
            class="swatch"
            :class="{ selected: form.color === c, taken: isTaken(c) }"
            :style="{ background: c }"
            :disabled="isTaken(c)"
            :title="isTaken(c) ? 'Déjà prise' : ''"
            @click="form.color = c"
          >
            <span v-if="form.color === c" :style="{ color: textColorOn(c) }">✔</span>
            <span v-else-if="isTaken(c)">✕</span>
          </button>
          <label class="swatch custom" title="Autre couleur">
            🎨
            <input v-model="form.color" type="color" />
          </label>
        </div>
        <p v-if="isTaken(form.color)" class="error">Cette couleur est déjà utilisée par quelqu'un d'autre.</p>

        <p v-if="error" class="error">{{ error }}</p>

        <div class="inline" style="margin-top:22px; justify-content:space-between">
          <template v-if="isEditing">
            <button v-if="!confirmDelete" type="button" class="danger" @click="confirmDelete = true">Supprimer</button>
            <button v-else type="button" class="danger" @click="remove">Vraiment supprimer ?</button>
          </template>
          <div class="inline" style="margin-left:auto">
            <button type="button" class="secondary" @click="$emit('close')">Annuler</button>
            <button type="submit" :disabled="saving || isTaken(form.color)">Enregistrer</button>
          </div>
        </div>
        <p v-if="confirmDelete" class="hint">
          Ses évènements resteront dans le calendrier, avec sa couleur, mais sans personne associée.
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import api from '../services/api'
import { usePersons } from '../composables/usePersons'
import { PERSON_PALETTE, textColorOn } from '../utils/color'
import { resizeImage } from '../utils/image'

const props = defineProps({
  person: { type: Object, default: null },
  usedColors: { type: Array, default: () => [] }
})
const emit = defineEmits(['close', 'saved'])

const { photoUrl } = usePersons()
const isEditing = computed(() => props.person !== null)
const todayIso = new Date().toLocaleDateString('sv-SE') // AAAA-MM-JJ en heure locale

const form = reactive({
  firstName: props.person?.firstName || '',
  lastName: props.person?.lastName || '',
  birthDate: props.person?.birthDate || '',
  color: props.person?.color || PERSON_PALETTE.find(c => !props.usedColors.includes(c)) || '#e91e63'
})

// Photo : null = inchangee, Blob = nouvelle photo, 'remove' = a supprimer
const photoChange = ref(null)
const localPreview = ref(null)
const previewUrl = computed(() => {
  if (photoChange.value === 'remove') return null
  return localPreview.value || photoUrl(props.person)
})

const error = ref('')
const saving = ref(false)
const confirmDelete = ref(false)

function isTaken(color) {
  return props.usedColors.some(c => c.toLowerCase() === (color || '').toLowerCase())
}

async function onPhotoPicked(event) {
  const file = event.target.files[0]
  if (!file) return
  try {
    const blob = await resizeImage(file)
    if (localPreview.value) URL.revokeObjectURL(localPreview.value)
    localPreview.value = URL.createObjectURL(blob)
    photoChange.value = blob
    error.value = ''
  } catch {
    error.value = "Impossible de lire cette image."
  }
}

function removePhoto() {
  if (localPreview.value) URL.revokeObjectURL(localPreview.value)
  localPreview.value = null
  photoChange.value = 'remove'
}

async function save() {
  error.value = ''
  saving.value = true
  const payload = {
    firstName: form.firstName,
    lastName: form.lastName,
    birthDate: form.birthDate,
    color: form.color
  }
  try {
    const saved = isEditing.value
      ? await api.updatePerson(props.person.id, payload)
      : await api.createPerson(payload)
    if (photoChange.value === 'remove') {
      await api.deletePhoto(saved.id)
    } else if (photoChange.value) {
      await api.uploadPhoto(saved.id, photoChange.value)
    }
    emit('saved')
  } catch (e) {
    error.value = e.response?.data?.error ?? "Erreur lors de l'enregistrement."
  } finally {
    saving.value = false
  }
}

async function remove() {
  await api.deletePerson(props.person.id)
  emit('saved')
}

onBeforeUnmount(() => {
  if (localPreview.value) URL.revokeObjectURL(localPreview.value)
})
</script>

<style scoped>
.photo-zone { display: flex; align-items: center; gap: 18px; margin: 8px 0 4px; }
.photo-preview {
  width: 120px; height: 120px; border-radius: 50%; border: 4px solid;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; font-size: 52px; font-weight: 800; flex-shrink: 0;
}
.photo-preview img { width: 100%; height: 100%; object-fit: cover; }
.photo-actions { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; }
.photo-actions small { color: var(--text-muted); }
.file-button {
  display: inline-flex; align-items: center; min-height: 48px; margin: 0;
  padding: 12px 18px; border-radius: 12px; background: var(--primary); color: var(--on-primary);
  font-size: 16px; font-weight: 600; cursor: pointer;
}
.file-button input { display: none; }

.swatches { display: flex; flex-wrap: wrap; gap: 10px; }
.swatch {
  width: 56px; height: 56px; min-height: 56px; padding: 0; border-radius: 50%;
  border: 3px solid transparent; font-size: 22px; font-weight: 800;
  display: flex; align-items: center; justify-content: center; color: #fff;
}
.swatch.selected { border-color: var(--text); box-shadow: 0 0 0 3px var(--surface) inset; }
.swatch.taken { opacity: 0.25; cursor: not-allowed; }
.swatch.custom { background: var(--surface-2); border-color: var(--border); margin: 0; position: relative; cursor: pointer; }
.swatch.custom input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }

</style>
