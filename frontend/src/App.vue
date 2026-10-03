<template>
  <router-view />
  <ReminderToasts />
</template>

<script setup>
import ReminderToasts from './components/ReminderToasts.vue'
import { useReminders } from './composables/useReminders'

// Surveille les evenements a venir et affiche les rappels, quel que soit l'ecran ouvert
useReminders()
</script>
