import { app, BrowserWindow, Tray, Menu, nativeImage } from 'electron'
import path from 'path'

let mainWindow: BrowserWindow | null = null
let tray: Tray | null = null

function createWindow(): BrowserWindow {
  const win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    webPreferences: {
      preload: path.join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
    titleBarStyle: 'hiddenInset',
    show: false,
  })

  const isDev = process.env.NODE_ENV === 'development' || process.env.VITE_DEV_SERVER_URL
  if (isDev && process.env.VITE_DEV_SERVER_URL) {
    win.loadURL(process.env.VITE_DEV_SERVER_URL)
  } else if (isDev) {
    win.loadURL('http://localhost:5173')
  } else {
    win.loadFile(path.join(__dirname, '../../dist/index.html'))
  }

  win.once('ready-to-show', () => win.show())
  win.on('closed', () => { mainWindow = null })
  return win
}

function setupTray(): void {
  try {
    const iconPath = path.join(__dirname, '../../assets/tray-icon.png')
    const icon = nativeImage.createFromPath(iconPath)
    if (!icon || icon.isEmpty()) return
    tray = new Tray(icon)
    tray.setToolTip('ATDiscord')
    tray.setContextMenu(Menu.buildFromTemplate([
      { label: 'Show ATDiscord', click: () => mainWindow?.show() },
      { type: 'separator' },
      { label: 'Quit', click: () => app.quit() },
    ]))
    tray.on('click', () => mainWindow?.show())
  } catch {
    // No tray icon or unsupported
  }
}

app.whenReady().then(() => {
  mainWindow = createWindow()
  setupTray()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) mainWindow = createWindow()
    else mainWindow?.show()
  })
})

app.on('window-all-closed', () => {
  tray?.destroy()
  tray = null
  if (process.platform !== 'darwin') app.quit()
})
