import React, { useState } from 'react';
import { Wifi, Battery, Signal, Plus, Trash2, Smartphone, Sparkles } from 'lucide-react';
import { LiveWidget } from './LiveWidgets';
import { WIDGETS_LIST } from '../data/mockData';

interface PhoneSimulatorProps {
  accent: string;
  wallpaperPattern: string;
  activeWidgetIds: string[];
  onRemoveWidget: (index: number) => void;
  onAddWidgetPrompt: () => void;
}

export const PhoneSimulator: React.FC<PhoneSimulatorProps> = ({
  accent,
  wallpaperPattern,
  activeWidgetIds,
  onRemoveWidget,
  onAddWidgetPrompt,
}) => {
  const [time, setTime] = useState(() => {
    const d = new Date();
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  });

  React.useEffect(() => {
    const timer = setInterval(() => {
      const d = new Date();
      setTime(`${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`);
    }, 10000);
    return () => clearInterval(timer);
  }, []);

  // Wallpaper pattern background styling
  const getWallpaperBackground = () => {
    switch (wallpaperPattern) {
      case 'matrix':
        return 'radial-gradient(circle at center, #111a14 0%, #050806 100%)';
      case 'radial':
        return 'radial-gradient(circle at 50% 60%, #1e1329 0%, #08050c 100%)';
      case 'circuit':
        return 'radial-gradient(circle at 20% 20%, #201309 0%, #0a0604 100%)';
      case 'dots':
        return 'radial-gradient(circle at center, #151515 0%, #050505 100%)';
      case 'glyph':
      default:
        return 'radial-gradient(circle at 50% 30%, #1a1a1a 0%, #080808 90%)';
    }
  };

  return (
    <div className="relative mx-auto w-full max-w-[340px] aspect-[9/19] rounded-[48px] p-3.5 bg-[#1a1a1a] shadow-[0_25px_60px_-15px_rgba(0,0,0,0.9),0_0_0_1px_rgba(255,255,255,0.12)] border-4 border-[#282828] overflow-hidden flex flex-col justify-between">
      {/* Side buttons simulation */}
      <div className="absolute -left-[6px] top-28 w-1 h-10 bg-zinc-700 rounded-l-md" />
      <div className="absolute -left-[6px] top-42 w-1 h-10 bg-zinc-700 rounded-l-md" />
      <div className="absolute -right-[6px] top-32 w-1 h-14 bg-zinc-700 rounded-r-md" />

      {/* Screen Container */}
      <div
        className="relative w-full h-full rounded-[38px] overflow-hidden flex flex-col justify-between p-3 select-none"
        style={{
          background: getWallpaperBackground(),
        }}
      >
        {/* Subtle dot matrix overlay on wallpaper */}
        <div
          className="absolute inset-0 pointer-events-none opacity-20"
          style={{
            backgroundImage: `radial-gradient(circle, ${accent} 1px, transparent 1px)`,
            backgroundSize: '16px 16px',
          }}
        />

        {/* Top Status Bar & Punch hole */}
        <div className="relative z-10 w-full pt-1 pb-2 flex items-center justify-between text-[11px] font-mono text-zinc-300">
          <span className="font-bold pl-2">{time}</span>
          {/* Punch-hole camera */}
          <div className="w-3.5 h-3.5 rounded-full bg-black border border-white/20 shadow-inner" />
          <div className="flex items-center gap-1.5 pr-2">
            <Signal size={12} />
            <Wifi size={12} />
            <span className="text-[10px]">84%</span>
            <Battery size={13} className="text-white" />
          </div>
        </div>

        {/* Homescreen Widgets Grid */}
        <div className="relative z-10 flex-1 overflow-y-auto no-scrollbar py-2 space-y-2.5">
          {activeWidgetIds.map((widgetId, index) => {
            const def = WIDGETS_LIST.find(w => w.id === widgetId);
            return (
              <div key={`${widgetId}-${index}`} className="relative group">
                <LiveWidget id={widgetId} accent={accent} />
                {/* Delete overlay button on hover */}
                <button
                  onClick={() => onRemoveWidget(index)}
                  className="absolute -top-1.5 -right-1.5 w-6 h-6 rounded-full bg-red-600/90 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-md z-20 hover:scale-110"
                  title="Remove from homescreen"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            );
          })}

          {activeWidgetIds.length < 5 && (
            <button
              onClick={onAddWidgetPrompt}
              className="w-full py-3.5 rounded-2xl border border-dashed border-white/20 bg-white/5 hover:bg-white/10 text-zinc-400 hover:text-white flex items-center justify-center gap-2 text-xs font-mono transition-colors"
            >
              <Plus size={14} />
              <span>ADD WIDGET TO HOMESCREEN</span>
            </button>
          )}
        </div>

        {/* Bottom App Dock */}
        <div className="relative z-10 pt-2 pb-1">
          <div className="bg-black/50 backdrop-blur-md rounded-2xl border border-white/10 p-2 flex items-center justify-around">
            {['📞', '💬', '🌐', '📷'].map((emoji, idx) => (
              <div
                key={idx}
                className="w-9 h-9 rounded-xl bg-zinc-800/80 border border-white/10 flex items-center justify-center text-sm shadow cursor-pointer hover:scale-105 transition-transform"
              >
                {emoji}
              </div>
            ))}
          </div>
          {/* Home indicator bar */}
          <div className="w-24 h-1 bg-white/30 rounded-full mx-auto mt-2" />
        </div>
      </div>
    </div>
  );
};
