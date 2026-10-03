// Lecture a voix haute (synthese vocale du navigateur, en francais) : permet
// a quelqu'un qui lit difficilement d'ecouter un evenement en touchant 🔊.

export function canSpeak() {
  return typeof window !== 'undefined' && 'speechSynthesis' in window
}

export function speak(text) {
  if (!canSpeak()) return
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'fr-FR'
  utterance.rate = 0.9
  const voice = window.speechSynthesis.getVoices().find(v => v.lang && v.lang.startsWith('fr'))
  if (voice) utterance.voice = voice
  window.speechSynthesis.speak(utterance)
}

function spokenTime(t) {
  const [h, m] = t.split(':').map(Number)
  return m === 0 ? `${h} heures` : `${h} heures ${m}`
}

// Phrase decrivant une occurrence, ex: "Dentiste, pour Caitlyn, samedi 12 octobre, a 9 heures 30."
export function describeOccurrence(occ, person, { withDate = true } = {}) {
  const parts = [occ.title]
  if (person) parts.push(`pour ${person.firstName}`)
  const longDate = iso => {
    const [y, mo, d] = iso.split('-').map(Number)
    return new Date(y, mo - 1, d).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' })
  }
  if (occ.spanStart) {
    parts.push(`du ${longDate(occ.spanStart)} au ${longDate(occ.spanEnd)}`)
  } else if (withDate) {
    parts.push(longDate(occ.date))
  }
  // Une periode n'a pas d'horaire
  if (!occ.spanStart) {
    parts.push(occ.allDay ? 'toute la journée' : `à ${spokenTime(occ.startTime)}`)
  }
  if (occ.location) parts.push(`à ${occ.location}`)
  return parts.join(', ') + '.'
}
