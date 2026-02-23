import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electronAPI', {
  platform: process.platform,
  // Expose IPC for future backend calls if needed
  send: (channel: string, ...args: unknown[]) => ipcRenderer.send(channel, ...args),
  on: (channel: string, fn: (...args: unknown[]) => void) => {
    ipcRenderer.on(channel, (_, ...args) => fn(...args))
  },
})
