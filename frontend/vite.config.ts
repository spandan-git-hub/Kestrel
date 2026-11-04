import { defineConfig, Plugin } from 'vite';
import react from '@vitejs/plugin-react';

function devAlertServerPlugin(): Plugin {
  return {
    name: 'dev-alert-server-plugin',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.url === '/dev-alert/toast' && req.method === 'POST') {
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ ok: true, delivered: 'BROWSER_NATIVE_NO_TERMINAL' }));
          return;
        }

        if (req.url === '/dev-alert/schedule' && req.method === 'POST') {
          let body = '';
          req.on('data', (chunk) => (body += chunk));
          req.on('end', () => {
            try {
              const data = JSON.parse(body || '{}');
              const delaySeconds = Math.max(5, Number(data.delaySeconds) || 60);

              // Silent Node.js in-process timer - ZERO external processes or terminal windows
              setTimeout(async () => {
                if (data.botToken && data.chatId) {
                  try {
                    await fetch(`https://api.telegram.org/bot${data.botToken}/sendMessage`, {
                      method: 'POST',
                      headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({
                        chat_id: data.chatId,
                        text: `${data.title || '🚨 KESTREL HAZARD ALERT'}\n${data.message || 'Background alert delivered.'}`,
                      }),
                    });
                  } catch {
                    // Ignore network error
                  }
                }
              }, delaySeconds * 1000);

              res.setHeader('Content-Type', 'application/json');
              res.end(
                JSON.stringify({
                  ok: true,
                  scheduledForSeconds: delaySeconds,
                  message: `Background alert timer armed for ${delaySeconds} seconds without opening any terminal windows.`,
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
