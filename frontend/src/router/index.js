import { createRouter, createWebHistory } from 'vue-router'
import CalendarView from '../views/CalendarView.vue'

const routes = [
  { path: '/', component: CalendarView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
