import { createWorker } from 'tesseract.js'

let workerPromise = null

export function loadRecognizer(onProgress) {
  if (!workerPromise) {
    workerPromise = createWorker('chi_sim', 1, {
      langPath: 'https://cdn.jsdelivr.net/npm/@tesseract.js-data/chi_sim/4.0.0',
      logger: (message) => onProgress?.(message),
    }).catch((error) => {
      workerPromise = null
      throw error
    })
  }
  return workerPromise
}

export function detectItemCards(image) {
  const width = image.width
  const height = image.height
  const data = image.data
  const columns = []
  let run = null
  for (let x = 0; x < width; x += 2) {
    let lit = 0
    for (let y = 0; y < height; y += 4) {
      const index = (y * width + x) * 4
      const bright = data[index] + data[index + 1] + data[index + 2]
      if (bright > 140) lit += 1
    }
    const isCard = lit > height / 40
    if (isCard && !run) run = x
    if (!isCard && run != null) {
      if (x - run > 36) columns.push([run, x])
      run = null
    }
  }
  if (run != null && width - run > 36) columns.push([run, width - 1])

  const cards = []
  for (const [x0, x1] of columns) {
    let row = null
    for (let y = 0; y < height; y += 2) {
      let lit = 0
      const samples = Math.max(1, Math.floor((x1 - x0) / 4))
      for (let x = x0; x < x1; x += 4) {
        const index = (y * width + x) * 4
        if (data[index] + data[index + 1] + data[index + 2] > 140) lit += 1
      }
      const isCard = lit > samples * 0.25
      if (isCard && row == null) row = y
      if (!isCard && row != null) {
        if (y - row > 28) cards.push({ x: x0, y: row, width: x1 - x0, height: y - row })
        row = null
      }
    }
  }
  return cards
}

function namesIn(text) {
  const chars = text.replace(/单价/g, '').replace(/[^\u4e00-\u9fff·]/g, '')
  if (chars.length < 2 || chars === '单价') return []
  for (const size of [2, 3, 4]) {
    if (chars.length < size * 2 || chars.length % size !== 0) continue
    const part = chars.slice(0, size)
    if (chars === part.repeat(chars.length / size)) return [part]
  }
  return chars.length <= 8 ? [chars] : []
}

export function readItemsFromOcr(lines) {
  const rows = lines
    .map((line) => ({
      text: (line.text || '').replace(/\s+/g, ''),
      y: line.bbox?.y0 ?? 0,
    }))
    .filter((line) => line.text)
  const items = []
  for (let index = 0; index < rows.length; index += 1) {
    const names = namesIn(rows[index].text)
    if (!names.length) continue
    const priceRow = rows.slice(index, index + 4).find((line) => (
      /\d{3,}/.test(line.text) && Math.abs(line.y - rows[index].y) < 90
    ))
    const price = priceRow ? Number(priceRow.text.match(/(\d{3,})/)[1]) : null
    for (const name of names) {
      if (items.some((item) => item.name === name && item.price === price)) continue
      items.push({ name, price: Number.isFinite(price) ? price : null })
    }
  }
  return items
}

async function toCanvas(source) {
  if (source instanceof HTMLCanvasElement) return source
  const bitmap = await createImageBitmap(source)
  const canvas = document.createElement('canvas')
  canvas.width = bitmap.width
  canvas.height = bitmap.height
  canvas.getContext('2d').drawImage(bitmap, 0, 0)
  return canvas
}

function cropCard(source, card) {
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, card.width)
  canvas.height = Math.max(1, card.height)
  canvas.getContext('2d').drawImage(
    source,
    card.x,
    card.y,
    card.width,
    card.height,
    0,
    0,
    card.width,
    card.height,
  )
  return canvas
}

export async function recognizeImage(source, onProgress) {
  const worker = await loadRecognizer(onProgress)
  const frame = await toCanvas(source)
  const pixels = frame.getContext('2d').getImageData(0, 0, frame.width, frame.height)
  const cards = detectItemCards(pixels).slice(0, 16)
  const found = []
  if (cards.length >= 2) {
    for (const card of cards) {
      const result = await worker.recognize(cropCard(frame, card))
      found.push(...readItemsFromOcr(result.data.lines || []))
    }
  }
  if (found.length) return { items: dedupe(found), text: '' }
  const result = await worker.recognize(frame)
  return {
    items: readItemsFromOcr(result.data.lines || []),
    text: (result.data.text || '').replace(/\s+/g, ' ').trim(),
  }
}

function dedupe(items) {
  const unique = []
  for (const item of items) {
    if (unique.some((row) => row.name === item.name && row.price === item.price)) continue
    unique.push(item)
  }
  return unique
}
