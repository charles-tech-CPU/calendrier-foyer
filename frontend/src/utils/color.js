// Texte noir ou blanc selon la couleur de fond, pour rester lisible
// meme sur une couleur claire (jaune, cyan...).
export function textColorOn(hex) {
  if (!hex || !/^#[0-9a-fA-F]{6}$/.test(hex)) return '#fff'
  const [r, g, b] = [1, 3, 5].map(i => parseInt(hex.slice(i, i + 2), 16) / 255)
  const luminance = 0.299 * r + 0.587 * g + 0.114 * b
  return luminance > 0.62 ? '#111' : '#fff'
}

// Couleurs vives et bien distinctes, proposees pour les personnes du foyer.
export const PERSON_PALETTE = [
  '#e91e63', '#2196f3', '#4caf50', '#ff9800', '#9c27b0',
  '#00bcd4', '#f44336', '#ffc107', '#795548', '#607d8b'
]

export const FAMILY_COLOR = '#4a90d9'
