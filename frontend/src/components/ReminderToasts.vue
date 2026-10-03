<template>
  <!-- Rappels empiles en bas a droite de l'ecran -->
  <div class="toasts">
    <div
      v-for="t in toasts" :key="t.key"
      class="toast"
      :style="{ borderColor: t.occ.color || FAMILY_COLOR }"
    >
      <div class="toast-head">
        <span class="bell">🔔</span>
        <span class="when">{{ whenLabel(t.occ) }}</span>
      </div>
      <div class="toast-body">
        <span class="toast-icon">{{ t.occ.icon || DEFAULT_ICON }}</span>
        <PersonAvatar v-if="t.occ.personId" :person="byId(t.occ.personId)" :size="52" />
        <div style="min-width:0">
          <div class="toast-title">{{ t.occ.title }}</div>
          <div class="toast-time">
            <template v-if="t.occ.spanStart">🗓️ Jusqu'au {{ formatShortDate(t.occ.spanEnd) }}</template>
            <template v-else-if="t.occ.allDay">☀️ Toute la journée</template>
            <template v-else>🕘 {{ t.occ.startTime.slice(0, 5) }}</template>
            <span v-if="t.occ.location"> · 📍 {{ t.occ.location }}</span>
          </div>
        </div>
      </div>
      <div class="toast-actions">
        <button v-if="canSpeak()" class="secondary" @click="read(t.occ)">🔊 Écouter</button>
        <button @click="dismiss(t.key)">👍 OK</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import PersonAvatar from './PersonAvatar.vue'
import { startOf, useReminderToasts } from '../composables/useReminders'
import { usePersons } from '../composables/usePersons'
import { DEFAULT_ICON } from '../composables/useCategories'
import { FAMILY_COLOR } from '../utils/color'
import { canSpeak, describeOccurrence, speak } from '../utils/speech'

const { toasts, dismiss } = useReminderToasts()
const { byId } = usePersons()

// Rafraichit "dans 10 min" -> "dans 9 min"... chaque minute
const now = ref(Date.now())
let timer = null
onMounted(() => { timer = setInterval(() => { now.value = Date.now() }, 30 * 1000) })
onBeforeUnmount(() => clearInterval(timer))

function whenLabel(occ) {
  const todayIso = new Date().toLocaleDateString('sv-SE')
  if (occ.allDay) return occ.date === todayIso ? "Aujourd'hui" : 'Demain'
  const minutes = Math.round((startOf(occ).getTime() - now.value) / 60000)
  if (minutes <= 0) return "C'est l'heure !"
  if (minutes < 60) return `Dans ${minutes} min`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return `Dans ${hours} h${rest ? ' ' + String(rest).padStart(2, '0') : ''}`
}

function formatShortDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })
}

function read(occ) {
  speak(`${whenLabel(occ)}. ${describeOccurrence(occ, byId(occ.personId), { withDate: false })}`)
}
</script>

<style scoped>
.toasts {
  position: fixed; right: 16px; bottom: 16px; z-index: 100;
  display: flex; flex-direction: column; gap: 12px;
  width: min(400px, calc(100vw - 32px));
}
.toast {
  background: var(--surface); border-radius: 18px; border: 4px solid; padding: 14px;
  box-shadow: 0 10px 30px rgba(30, 42, 56, 0.3);
  animation: arrive 0.4s ease-out;
}
@keyframes arrive {
  from { transform: translateY(30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
.toast-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.bell { font-size: 24px; animation: ring 1s ease-in-out 3; }
@keyframes ring {
  0%, 100% { transform: rotate(0); }
  25% { transform: rotate(18deg); }
  75% { transform: rotate(-18deg); }
}
.when { font-size: 20px; font-weight: 800; color: var(--primary); }
.toast-body { display: flex; align-items: center; gap: 12px; }
.toast-icon { font-size: 48px; line-height: 1; }
.toast-title { font-size: 20px; font-weight: 800; }
.toast-time { font-size: 15px; color: var(--text-muted); margin-top: 2px; }
.toast-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
</style>
