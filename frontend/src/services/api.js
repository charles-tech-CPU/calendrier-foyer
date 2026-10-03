import axios from 'axios'

const BASE_URL = 'http://localhost:8084/api'
const client = axios.create({ baseURL: BASE_URL })

// Construit AAAA-MM-JJ a partir des composantes LOCALES de la date (jamais
// toISOString(), qui convertit en UTC et peut decaler la date d'un jour
// pour un fuseau horaire comme la France - ex: minuit local le 1er octobre
// deviendrait "30 septembre" en UTC).
function iso(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export default {
  getOccurrences(from, to) {
    return client.get('/events', { params: { from: iso(from), to: iso(to) } }).then(r => r.data)
  },
  getEvent(id) {
    return client.get(`/events/${id}`).then(r => r.data)
  },
  createEvent(payload) {
    return client.post('/events', payload).then(r => r.data)
  },
  updateEvent(id, payload) {
    return client.put(`/events/${id}`, payload).then(r => r.data)
  },
  deleteEvent(id) {
    return client.delete(`/events/${id}`)
  },

  // Cocher / decocher "c'est fait" une occurrence precise (date = AAAA-MM-JJ)
  markDone(eventId, date) {
    return client.put(`/events/${eventId}/done/${date}`)
  },
  unmarkDone(eventId, date) {
    return client.delete(`/events/${eventId}/done/${date}`)
  },

  getCategories() {
    return client.get('/categories').then(r => r.data)
  },
  createCategory(payload) {
    return client.post('/categories', payload).then(r => r.data)
  },
  updateCategory(id, payload) {
    return client.put(`/categories/${id}`, payload).then(r => r.data)
  },
  deleteCategory(id) {
    return client.delete(`/categories/${id}`)
  },

  getPersons() {
    return client.get('/persons').then(r => r.data)
  },
  createPerson(payload) {
    return client.post('/persons', payload).then(r => r.data)
  },
  updatePerson(id, payload) {
    return client.put(`/persons/${id}`, payload).then(r => r.data)
  },
  deletePerson(id) {
    return client.delete(`/persons/${id}`)
  },
  uploadPhoto(id, blob) {
    const form = new FormData()
    form.append('file', blob, 'photo.jpg')
    return client.put(`/persons/${id}/photo`, form)
  },
  deletePhoto(id) {
    return client.delete(`/persons/${id}/photo`)
  },
  // URL directe (balise <img>) ; version = anti-cache apres changement de photo
  photoUrl(id, version) {
    return `${BASE_URL}/persons/${id}/photo?v=${version}`
  }
}
