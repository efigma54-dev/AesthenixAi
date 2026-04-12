const { app, BrowserWindow, Menu, dialog, shell } = require('electron');
const { exec, spawn } = require('child_process');
const path = require('path');
const waitOn = require('wait-on');

let mainWindow;
let backendProcess;
let frontendProcess;

// Check if we're in development or production
const isDev = process.env.NODE_ENV === 'development';

function createWindow() {
  // Create the browser window
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1000,
    minHeight: 700,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      enableRemoteModule: false,
    },
    icon: path.join(__dirname, '../assets/icon.png'), // Add icon later
    titleBarStyle: 'default',
    show: false, // Don't show until ready
  });

  // Load the app
  const startUrl = isDev
    ? 'http://localhost:3000'
    : `file://${path.join(__dirname, '../frontend-react/dist/index.html')}`;

  mainWindow.loadURL(startUrl);

  // Show window when ready to prevent visual flash
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  // Open external links in default browser
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  // Handle window closed
  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  // Create application menu
  createMenu();
}

function createMenu() {
  const template = [
    {
      label: 'File',
      submenu: [
        {
          label: 'Open Repository',
          accelerator: 'CmdOrCtrl+O',
          click: () => {
            dialog.showOpenDialog(mainWindow, {
              properties: ['openDirectory'],
              title: 'Select Repository Directory'
            });
          }
        },
        { type: 'separator' },
        {
          label: 'Exit',
          accelerator: process.platform === 'darwin' ? 'Cmd+Q' : 'Ctrl+Q',
          click: () => {
            app.quit();
          }
        }
      ]
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { role: 'toggleDevTools' },
        { type: 'separator' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' }
      ]
    },
    {
      label: 'Help',
      submenu: [
        {
          label: 'About AI Code Reviewer',
          click: () => {
            dialog.showMessageBox(mainWindow, {
              type: 'info',
              title: 'About',
              message: 'AI Code Reviewer',
              detail: 'Intelligent code analysis platform powered by local AI\nVersion 1.0.0'
            });
          }
        },
        {
          label: 'Check for Updates',
          click: () => {
            dialog.showMessageBox(mainWindow, {
              type: 'info',
              message: 'Update Check',
              detail: 'This is the latest version.'
            });
          }
        }
      ]
    }
  ];

  // macOS specific menu adjustments
  if (process.platform === 'darwin') {
    template.unshift({
      label: app.getName(),
      submenu: [
        { role: 'about' },
        { type: 'separator' },
        { role: 'services' },
        { type: 'separator' },
        { role: 'hide' },
        { role: 'hideOthers' },
        { role: 'unhide' },
        { type: 'separator' },
        { role: 'quit' }
      ]
    });
  }

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
}

async function startServices() {
  try {
    console.log('🚀 Starting AI Code Reviewer...');

    // Start backend first
    console.log('📡 Starting Spring Boot backend...');
    await startBackend();

    // Start frontend
    console.log('🌐 Starting React frontend...');
    await startFrontend();

    // Wait for services to be ready
    console.log('⏳ Waiting for services to be ready...');
    await waitForServices();

    console.log('✅ All services ready!');

  } catch (error) {
    console.error('❌ Failed to start services:', error);
    dialog.showErrorBox('Startup Error', `Failed to start services: ${error.message}`);
    app.quit();
  }
}

function startBackend() {
  return new Promise((resolve, reject) => {
    const backendPath = path.join(__dirname, '../ai-code-reviewer');

    // Use start.sh on Unix-like systems, or direct java on Windows
    const isWindows = process.platform === 'win32';
    const command = isWindows ? 'mvnw.cmd' : './mvnw';
    const args = isWindows ? ['spring-boot:run'] : ['spring-boot:run'];

    backendProcess = spawn(command, args, {
      cwd: backendPath,
      stdio: ['ignore', 'pipe', 'pipe'],
      shell: isWindows
    });

    backendProcess.stdout.on('data', (data) => {
      console.log(`[Backend] ${data.toString().trim()}`);
    });

    backendProcess.stderr.on('data', (data) => {
      console.error(`[Backend Error] ${data.toString().trim()}`);
    });

    backendProcess.on('error', (error) => {
      console.error('Backend start error:', error);
      reject(error);
    });

    // Wait a bit for backend to start
    setTimeout(resolve, 2000);
  });
}

function startFrontend() {
  return new Promise((resolve, reject) => {
    if (!isDev) {
      // In production, frontend is already built
      resolve();
      return;
    }

    const frontendPath = path.join(__dirname, '../frontend-react');

    frontendProcess = spawn('npm', ['run', 'dev'], {
      cwd: frontendPath,
      stdio: ['ignore', 'pipe', 'pipe']
    });

    frontendProcess.stdout.on('data', (data) => {
      console.log(`[Frontend] ${data.toString().trim()}`);
    });

    frontendProcess.stderr.on('data', (data) => {
      console.error(`[Frontend Error] ${data.toString().trim()}`);
    });

    frontendProcess.on('error', (error) => {
      console.error('Frontend start error:', error);
      reject(error);
    });

    // Wait a bit for frontend to start
    setTimeout(resolve, 1000);
  });
}

async function waitForServices() {
  const services = [
    'http://localhost:8080/api/health', // Backend health check
  ];

  if (isDev) {
    services.push('http://localhost:3000'); // Frontend dev server
  }

  await waitOn({
    resources: services,
    timeout: 30000, // 30 seconds
    interval: 1000,
  });
}

// App event handlers
app.whenReady().then(async () => {
  await startServices();
  createWindow();
});

app.on('window-all-closed', () => {
  // Clean up processes
  if (backendProcess) {
    backendProcess.kill();
  }
  if (frontendProcess) {
    frontendProcess.kill();
  }

  // On macOS, keep app running even when all windows are closed
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  // On macOS, re-create window when dock icon is clicked
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

// Handle app shutdown gracefully
process.on('SIGINT', () => {
  if (backendProcess) backendProcess.kill();
  if (frontendProcess) frontendProcess.kill();
  app.quit();
});

process.on('SIGTERM', () => {
  if (backendProcess) backendProcess.kill();
  if (frontendProcess) frontendProcess.kill();
  app.quit();
});