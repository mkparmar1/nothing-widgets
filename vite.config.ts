import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

import fs from 'fs';
import path from 'path';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    {
      name: 'apk-mime-middleware',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url && (req.url.startsWith('/app-debug.apk') || req.url.startsWith('/DotWidgets'))) {
            const apkPath = path.resolve(__dirname, 'public/app-debug.apk');
            if (fs.existsSync(apkPath)) {
              const stat = fs.statSync(apkPath);
              res.writeHead(200, {
                'Content-Type': 'application/vnd.android.package-archive',
                'Content-Length': stat.size,
                'Content-Disposition': 'attachment; filename="DotWidgets-v1.0.0.apk"',
                'Cache-Control': 'no-cache',
              });
              fs.createReadStream(apkPath).pipe(res);
              return;
            }
          }
          next();
        });
      },
    },
  ],
  publicDir: 'public',
  server: {
    host: '0.0.0.0',
    port: 3000,
  },
});
