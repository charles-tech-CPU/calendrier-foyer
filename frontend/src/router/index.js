import { createRouter, createWebHistory } from 'vue-router'
import CalendarView from '../views/CalendarView.vue'
import CategoriesView from '../views/CategoriesView.vue'
import PersonsView from '../views/PersonsView.vue'

const routes = [
  { path: '/', component: CalendarView },
  { path: '/famille', component: PersonsView },
  { path: '/categories', component: CategoriesView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
