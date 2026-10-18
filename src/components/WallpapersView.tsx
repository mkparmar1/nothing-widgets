import React, { useRef, useState, useEffect } from 'react';
import { WALLPAPERS } from '../data/mockData';
import { WallpaperItem } from '../types';
import { Download, Sparkles, Check, RefreshCw } from 'lucide-react';

interface WallpapersViewProps {
  currentAccent: string;
  activeWallpaperId: string;
  onSelectWallpaper: (wp: WallpaperItem) => void;
}

export const WallpapersView: React.FC<WallpapersViewProps> = ({
  currentAccent,
  activeWallpaperId,
  onSelectWallpaper,
}) => {
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  // Function to generate and download wallpaper as a real high-res PNG file (1080x2400 phone resolution)
  const downloadWallpaper = (wp: WallpaperItem) => {
    setDownloadingId(wp.id);
    const canvas = document.createElement('canvas');
    canvas.width = 1080;
    canvas.height = 2400;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Background gradient
    const grad = ctx.createRadialGradient(540, 1200, 100, 540, 1200, 1200);
    grad.addColorStop(0, '#1a1a1a');
    grad.addColorStop(1, '#050505');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, 1080, 2400);

    // Draw dot matrix pattern based on type
    const dotSpacing = 36;
    const cols = Math.floor(1080 / dotSpacing);
    const rows = Math.floor(2400 / dotSpacing);

    ctx.fillStyle = '#222222';
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        const x = c * dotSpacing + dotSpacing / 2;
        const y = r * dotSpacing + dotSpacing / 2;

        if (wp.pattern === 'radial') {
          const dist = Math.hypot(x - 540, y - 1200);
          const isRing = Math.abs((dist % 240) - 120) < 18;
          ctx.fillStyle = isRing ? wp.accentColor : '#181818';
          ctx.beginPath();
          ctx.arc(x, y, isRing ? 4 : 2, 0, Math.PI * 2);
          ctx.fill();
        } else if (wp.pattern === 'glyph') {
          const isCenter = Math.abs(x - 540) < 220 && Math.abs(y - 900) < 300;
          ctx.fillStyle = isCenter ? wp.accentColor : '#202020';
          ctx.beginPath();
          ctx.arc(x, y, isCenter ? 5 : 2, 0, Math.PI * 2);
          ctx.fill();
        } else if (wp.pattern === 'circuit') {
          const isLine = (r * 7 + c * 3) % 19 === 0;
          ctx.fillStyle = isLine ? wp.accentColor : '#1a1a1a';
          ctx.beginPath();
          ctx.arc(x, y, isLine ? 4.5 : 2, 0, Math.PI * 2);
          ctx.fill();
        } else {
          // Standard dot grid
          ctx.fillStyle = (r + c) % 8 === 0 ? wp.accentColor : '#1c1c1c';
          ctx.beginPath();
          ctx.arc(x, y, (r + c) % 8 === 0 ? 3.5 : 2, 0, Math.PI * 2);
          ctx.fill();
        }
      }
    }

    // Trigger download
    setTimeout(() => {
      const link = document.createElement('a');
      link.download = `dot-widgets-${wp.id}.png`;
      link.href = canvas.toDataURL('image/png');
      link.click();
      setDownloadingId(null);
    }, 400);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-white/10">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Sparkles size={20} style={{ color: currentAccent }} />
            CURATED DOT-MATRIX WALLPAPERS
          </h2>
          <p className="text-sm text-zinc-400 mt-1">
            Engineered specifically to complement high-contrast monochrome and red widgets with zero visual clutter.
          </p>
        </div>
        <span className="text-xs font-mono px-3 py-1.5 rounded-full bg-white/5 border border-white/10 text-zinc-400 self-start sm:self-auto">
          HIGH-DPI 1080x2400 RES
        </span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {WALLPAPERS.map((wp) => {
          const isActive = wp.id === activeWallpaperId;

          return (
            <div
              key={wp.id}
              className={`rounded-3xl p-4 border transition-all duration-200 flex flex-col justify-between ${
                isActive
                  ? 'bg-zinc-900/90 border-white/40 shadow-xl ring-1 ring-white/20'
                  : 'bg-zinc-900/40 border-white/10 hover:border-white/25 hover:bg-zinc-900/60'
              }`}
            >
              <div>
                {/* Visual Wallpaper Preview Card */}
                <div
                  className="relative w-full aspect-[9/16] rounded-2xl overflow-hidden border border-white/10 mb-4 p-3 flex flex-col justify-between"
                  style={{ background: wp.bgGradient }}
                >
                  {/* Dot Pattern Overlay */}
                  <div
                    className="absolute inset-0 pointer-events-none opacity-30"
                    style={{
                      backgroundImage: `radial-gradient(circle, ${wp.accentColor} 1.5px, transparent 1.5px)`,
                      backgroundSize: '18px 18px',
                    }}
                  />

                  {/* Top tag */}
                  <div className="relative z-10 flex justify-between items-center text-[10px] font-mono text-zinc-400">
                    <span className="px-2 py-0.5 rounded-full bg-black/60 backdrop-blur-xs">
                      {wp.pattern.toUpperCase()}
                    </span>
                    <span
                      className="w-2.5 h-2.5 rounded-full"
                      style={{ backgroundColor: wp.accentColor }}
                    />
                  </div>

                  {/* Center Dot matrix decorative element */}
                  <div className="relative z-10 flex items-center justify-center">
                    <div className="w-16 h-16 rounded-full border border-white/20 flex items-center justify-center">
                      <div
                        className="w-6 h-6 rounded-full"
                        style={{ backgroundColor: wp.accentColor }}
                      />
                    </div>
                  </div>

                  {/* Bottom tagline */}
                  <div className="relative z-10 bg-black/70 backdrop-blur-sm p-2 rounded-xl text-center">
                    <div className="text-xs font-mono font-bold text-white">{wp.name}</div>
                  </div>
                </div>

                <div className="mb-4">
                  <h3 className="text-base font-bold text-white">{wp.name}</h3>
                  <p className="text-xs font-mono text-zinc-400 mt-0.5">{wp.tagline}</p>
                </div>
              </div>

              {/* Actions */}
              <div className="grid grid-cols-2 gap-2 pt-2 border-t border-white/5">
                <button
                  onClick={() => onSelectWallpaper(wp)}
                  className={`py-2 px-3 rounded-xl font-mono text-xs font-bold flex items-center justify-center gap-1.5 transition-colors ${
                    isActive
                      ? 'bg-white/10 text-white'
                      : 'bg-zinc-800 hover:bg-zinc-700 text-white'
                  }`}
                >
                  {isActive ? <Check size={13} /> : null}
                  {isActive ? 'ACTIVE' : 'APPLY'}
                </button>

                <button
                  onClick={() => downloadWallpaper(wp)}
                  disabled={downloadingId === wp.id}
                  className="py-2 px-3 rounded-xl font-mono text-xs font-bold bg-white text-black hover:bg-zinc-200 flex items-center justify-center gap-1.5 transition-colors disabled:opacity-50"
                >
                  {downloadingId === wp.id ? (
                    <RefreshCw size={13} className="animate-spin" />
                  ) : (
                    <Download size={13} />
                  )}
                  {downloadingId === wp.id ? 'RENDERING' : 'SAVE PNG'}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
