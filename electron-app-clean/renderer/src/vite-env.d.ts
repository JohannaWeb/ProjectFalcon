/// <reference types="vite/client" />

interface Window {
  electronAPI?: {
    platform: string
    send: (channel: string, ...args: unknown[]) => void
    on: (channel: string, fn: (...args: unknown[]) => void) => void
  }
}
