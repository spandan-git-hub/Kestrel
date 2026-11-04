import { defineConfig, Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import { spawn } from 'child_process';

function buildWindowsToastScript(
  title: string,
  message: string,
  delaySeconds = 0,
  botToken?: string,
  chatId?: string
): string {
  const xmlTitle = (title || '🚨 KESTREL HAZARD ALERT')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
  const xmlMsg = (message || 'Hazard alert delivered to Windows Action Center.')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

  let script = '';
  if (delaySeconds > 0) {
    script += `Start-Sleep -Seconds ${delaySeconds}\n`;
  }

  script += `
[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null

$xml = New-Object Windows.Data.Xml.Dom.XmlDocument
$xml.LoadXml('<toast><visual><binding template="ToastGeneric"><text>${xmlTitle}</text><text>${xmlMsg}</text></binding></visual></toast>')
$toast = New-Object Windows.UI.Notifications.ToastNotification $xml

$delivered = $false
try {
  $notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('{1AC14E77-02E7-4E5D-B744-2EB1AE5198B7}\\WindowsPowerShell\\v1.0\\powershell.exe')
  $notifier.Show($toast)
  $delivered = $true
} catch {}

if (-not $delivered) {
  try {
    $notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Chrome')
    $notifier.Show($toast)
    $delivered = $true
  } catch {}
}

try { [System.Media.SystemSounds]::Exclamation.Play() } catch {}
`;

  if (botToken && chatId) {
    const tgText = `${title}\n${message}`.replace(/"/g, '`"');
    script += `
try {
  $body = @{ chat_id = '${chatId}'; text = "${tgText}" } | ConvertTo-Json
  Invoke-RestMethod -Uri "https://api.telegram.org/bot${botToken}/sendMessage" -Method Post -ContentType "application/json" -Body $body | Out-Null
} catch {}
`;
  }

  return script;
}

function launchDetachedPowerShell(script: string) {
  const b64 = Buffer.from(script, 'utf16le').toString('base64');
  const cmd = `powershell.exe -NoProfile -WindowStyle Hidden -EncodedCommand ${b64}`;

  // Launch via WMI Win32_Process: completely decoupled from Vite's process tree!
  // It continues running even if Vite (localhost) and Chrome are closed.
  const launcher = spawn(
    'powershell.exe',
    [
      '-NoProfile',
      '-Command',
      `Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine = '${cmd}'}`,
    ],
    { windowsHide: true, stdio: 'ignore' }
  );
  launcher.unref();
}

function devAlertServerPlugin(): Plugin {
  return {
    name: 'dev-alert-server-plugin',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.url === '/dev-alert/toast' && req.method === 'POST') {
          let body = '';
          req.on('data', (chunk) => (body += chunk));
          req.on('end', () => {
            try {
              const data = JSON.parse(body || '{}');
              const script = buildWindowsToastScript(data.title, data.message, 0);
              launchDetachedPowerShell(script);

              res.setHeader('Content-Type', 'application/json');
              res.end(JSON.stringify({ ok: true, delivered: 'WINDOWS_ACTION_CENTER' }));
            } catch (e: any) {
              res.statusCode = 500;
              res.end(JSON.stringify({ ok: false, error: e?.message }));
            }
          });
          return;
        }

        if (req.url === '/dev-alert/schedule' && req.method === 'POST') {
          let body = '';
          req.on('data', (chunk) => (body += chunk));
          req.on('end', () => {
            try {
              const data = JSON.parse(body || '{}');
              const delaySeconds = Math.max(5, Number(data.delaySeconds) || 60);
              const script = buildWindowsToastScript(
                data.title || '🚨 KESTREL DELAYED TEST ALERT',
                data.message || 'Background alert delivered while Chrome & app were closed.',
                delaySeconds,
                data.botToken,
                data.chatId
              );
              launchDetachedPowerShell(script);

              res.setHeader('Content-Type', 'application/json');
              res.end(
                JSON.stringify({
                  ok: true,
                  scheduledForSeconds: delaySeconds,
                  message: `Detached background alert scheduled for ${delaySeconds} seconds! You can safely close Chrome and Kestrel now.`,
                })
              );
            } catch (e: any) {
              res.statusCode = 500;
              res.end(JSON.stringify({ ok: false, error: e?.message }));
            }
          });
          return;
        }

        next();
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), devAlertServerPlugin()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
