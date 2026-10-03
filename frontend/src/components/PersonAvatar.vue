<template>
  <!-- Photo ronde cerclee de la couleur de la personne, ou son initiale sur sa couleur -->
  <div
    class="avatar"
    :style="{
      width: size + 'px', height: size + 'px', fontSize: Math.round(size * 0.45) + 'px',
      borderColor: person ? person.color : FAMILY_COLOR,
      background: person ? person.color : FAMILY_COLOR,
      color: textColorOn(person ? person.color : FAMILY_COLOR)
    }"
  >
    <img v-if="photo" :src="photo" alt="" />
    <span v-else-if="person">{{ person.firstName.charAt(0).toUpperCase() }}</span>
    <span v-else>👨‍👩‍👧</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { usePersons } from '../composables/usePersons'
import { FAMILY_COLOR, textColorOn } from '../utils/color'

// person = null : avatar "toute la famille"
const props = defineProps({
  person: { type: Object, default: null },
  size: { type: Number, default: 48 }
})

const { photoUrl } = usePersons()
const photo = computed(() => photoUrl(props.person))
</script>

<style scoped>
.avatar {
  border-radius: 50%;
  border: 3px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  font-weight: 800;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }
</style>
