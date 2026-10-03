import axios from 'axios'

const client = axios.create({ baseURL: 'http://localhost:8084/api' })

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
  }
}
