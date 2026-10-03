// Genere src/assets/emojis-fr.json : la bibliotheque d'icones proposee quand on
// cree une categorie, a partir des donnees Unicode d'emojibase-data (libelles et
// mots-cles en francais, pour la recherche).
// Relancer avec "npm run emojis" apres une mise a jour d'emojibase-data.
import { readFileSync, writeFileSync } from 'node:fs'

// Emojis trop recents ecartes : ils s'afficheraient en carre vide sur une
// tablette dont le systeme n'est pas a jour (Emoji 15.0 = Android 14 / iOS 16.4).
const MAX_EMOJI_VERSION = 15.0
// Groupes ecartes : 2 = composants (teintes de peau...), 9 = drapeaux
// (affiches en simples lettres sous Windows).
const EXCLUDED_GROUPS = new Set([2, 9])

const data = JSON.parse(readFileSync('node_modules/emojibase-data/fr/data.json', 'utf8'))
const messages = JSON.parse(readFileSync('node_modules/emojibase-data/fr/messages.json', 'utf8'))

const emojis = data
  .filter(e => e.group !== undefined && !EXCLUDED_GROUPS.has(e.group) && e.version <= MAX_EMOJI_VERSION)
  .sort((a, b) => a.order - b.order)
  .map(e => ({ e: e.emoji, l: e.label, t: (e.tags || []).join(' '), g: e.group }))

const groups = messages.groups
  .filter(g => !EXCLUDED_GROUPS.has(g.order))
  .map(g => ({ id: g.order, label: g.message }))

writeFileSync('src/assets/emojis-fr.json', JSON.stringify({ groups, emojis }))
console.log(`${emojis.length} emojis ecrits dans src/assets/emojis-fr.json`)
