export async function recognizeWithMimo(blob) {
  const body = new FormData()
  body.append('image', blob, 'frame.jpg')
  let response
  try {
    response = await fetch('/api/recognize', { method: 'POST', body })
  } catch {
    throw new Error('没有连上识别后台。请先启动 Spring Boot。')
  }
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.error || '识别失败')
  }
  if (!Array.isArray(data.items) || data.items.length === 0) {
    throw new Error('没有认出物品')
  }
  return data.items
}
