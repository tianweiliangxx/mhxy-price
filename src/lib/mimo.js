export async function recognizeWithMimo(blob, mode, onReasoning) {
  const body = new FormData()
  body.append('image', blob, 'frame.jpg')
  body.append('mode', mode)
  let response
  try {
    response = await fetch('/api/recognize', { method: 'POST', body })
  } catch {
    throw new Error('没有连上识别后台。请先启动 Spring Boot。')
  }
  const type = response.headers.get('content-type') || ''
  if (!type.includes('text/event-stream')) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.error || '识别失败')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let items = []
  let failed = ''
  while (true) {
    const step = await reader.read()
    buffer += decoder.decode(step.value || new Uint8Array(), { stream: !step.done })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    for (const part of parts) {
      const payload = part.split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trim())
        .join('\n')
      if (!payload) continue
      const event = JSON.parse(payload)
      if (event.type === 'reasoning' && event.text) onReasoning(event.text)
      if (event.type === 'done') items = event.items || []
      if (event.type === 'error') failed = event.error || '识别失败'
    }
    if (step.done) break
  }
  if (failed) throw new Error(failed)
  if (!Array.isArray(items) || items.length === 0) throw new Error('没有认出物品')
  return items
}
