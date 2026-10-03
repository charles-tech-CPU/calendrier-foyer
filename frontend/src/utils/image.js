// Reduit une photo (souvent plusieurs Mo depuis un telephone) en JPEG carre
// de 512 px max avant envoi : largement suffisant pour un avatar, et leger.
export function resizeImage(file, maxSize = 512) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      // Recadrage centre en carre, pour un avatar rond bien rempli
      const side = Math.min(img.width, img.height)
      const size = Math.min(side, maxSize)
      const canvas = document.createElement('canvas')
      canvas.width = size
      canvas.height = size
      canvas.getContext('2d').drawImage(
        img, (img.width - side) / 2, (img.height - side) / 2, side, side, 0, 0, size, size
      )
      URL.revokeObjectURL(url)
      canvas.toBlob(blob => (blob ? resolve(blob) : reject(new Error('Image illisible'))), 'image/jpeg', 0.85)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Image illisible'))
    }
    img.src = url
  })
}
