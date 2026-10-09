import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const pricesPath = path.resolve(path.dirname(fileURLToPath(import.meta.url)), 'prices.json')

function pricesJson() {
  const send = (req, res, next) => {
    const url = req.url?.split('?')[0]
    if (url !== '/prices.json') return next()
    fs.readFile(pricesPath, (error, data) => {
      if (error) {
        res.statusCode = 404
        res.setHeader('Content-Type', 'text/plain; charset=utf-8')
        res.end('prices.json not found')
        return
      }
      res.setHeader('Content-Type', 'application/json; charset=utf-8')
      res.end(data)
    })
  }

  return {
    name: 'prices-json',
    configureServer(server) {
      server.middlewares.use(send)
    },
    configurePreviewServer(server) {
      server.middlewares.use(send)
    },
    generateBundle() {
      this.emitFile({
        type: 'asset',
        fileName: 'prices.json',
        source: fs.readFileSync(pricesPath),
      })
    },
  }
}

export default defineConfig({
  plugins: [vue(), pricesJson()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
    },
  },
})
