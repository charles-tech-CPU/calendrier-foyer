<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="panel wide">
      <div class="panel-header">
        <h2 style="margin:0">{{ isEditing ? 'Modifier la catégorie' : 'Nouvelle catégorie' }}</h2>
        <button class="icon secondary" @click="$emit('close')">✕</button>
      </div>

      <form @submit.prevent="save">
        <!-- Apercu de la tuile telle qu'elle apparaitra -->
        <div class="preview">
          <div class="preview-icon">{{ form.icon || '❔' }}</div>
          <div style="flex:1">
            <label style="margin-top:0">Nom</label>
            <input v-model="form.name" required maxlength="60" placeholder="Ex: Piscine, Vider le lave-vaisselle…" />
          </div>
        </div>

        <label>Choisis une icône</label>
        <EmojiPicker v-model="form.icon" />

        <p v-if="error" class="error">{{ error }}</p>

        <div class="inline" style="margin-top:18px; justify-content:space-between">
          <template v-if="isEditing">
            <button v-if="!confirmDelete" type="button" class="danger" @click="confirmDelete = true">Supprimer</button>
            <button v-else type="button" class="danger" @click="remove">Vraiment supprimer ?</button>
          </template>
          <div class="inline" style="margin-left:auto">
            <button type="button" class="secondary" @click="$emit('close')">Annuler</button>
            <button type="submit" :disabled="saving || !form.icon">Enregistrer</button>
          </div>
        </div>
        <p v-if="confirmDelete" class="hint">
          Les évènements de cette catégorie resteront dans le calendrier, avec l'icône 📌.
        </p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import api from '../services/api'
import EmojiPicker from './EmojiPicker.vue'

const props = defineProps({ category: { type: Object, default: null } })
const emit = defineEmits(['close', 'saved', 'deleted'])

const isEditing = computed(() => props.category !== null)
const form = reactive({ name: props.category?.name || '', icon: props.category?.icon || '' })
const error = ref('')
const saving = ref(false)
const confirmDelete = ref(false)

async function save() {
  error.value = ''
  saving.value = true
  try {
    const saved = isEditing.value
      ? await api.updateCategory(props.category.id, { ...form })
      : await api.createCategory({ ...form })
    emit('saved', saved)
  } catch (e) {
    error.value = e.response?.data?.error ?? "Erreur lors de l'enregistrement."
  } finally {
    saving.value = false
  }
}

async function remove() {
  await api.deleteCategory(props.category.id)
  emit('deleted')
}
</script>

<style scoped>
.preview { display: flex; align-items: center; gap: 16px; }
.preview-icon {
  width: 96px; height: 96px; border-radius: 20px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 60px; background: var(--primary-soft);
}
</style>
