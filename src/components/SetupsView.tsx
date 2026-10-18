import React from 'react';
import { CURATED_SETUPS, WIDGETS_LIST } from '../data/mockData';
import { HomescreenSetup } from '../types';
import { Check, Sparkles, Smartphone, ArrowRight } from 'lucide-react';
import { LiveWidget } from './LiveWidgets';

interface SetupsViewProps {
  currentAccent: string;
  onApplySetup: (setup: HomescreenSetup) => void;
  activeSetupId: string;
}

export const SetupsView: React.FC<SetupsViewProps> = ({
  currentAccent,
  onApplySetup,
  activeSetupId,
}) => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-white/10">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Sparkles size={20} style={{ color: currentAccent }} />
            CURATED AESTHETIC SETUPS
          </h2>
          <p className="text-sm text-zinc-400 mt-1">
            Handcrafted homescreen compositions combining matching dot-matrix widgets, wallpapers, and signature accents.
          </p>
        </div>
        <span className="text-xs font-mono px-3 py-1.5 rounded-full bg-white/5 border border-white/10 text-zinc-400 self-start sm:self-auto">
          {CURATED_SETUPS.length} PRESETS READY
        </span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {CURATED_SETUPS.map((setup) => {
          const isActive = setup.id === activeSetupId;
          const widgets = setup.widgetIds.map(id => WIDGETS_LIST.find(w => w.id === id)).filter(Boolean);

          return (
            <div
              key={setup.id}
              className={`rounded-3xl p-5 border transition-all duration-200 flex flex-col justify-between ${
                isActive
                  ? 'bg-zinc-900/90 border-white/40 shadow-2xl ring-1 ring-white/20'
                  : 'bg-zinc-900/50 border-white/10 hover:border-white/25 hover:bg-zinc-900/70'
              }`}
            >
              <div>
                {/* Header */}
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-lg font-bold text-white tracking-wide">{setup.name}</h3>
                      <span
                        className="w-2.5 h-2.5 rounded-full"
                        style={{ backgroundColor: setup.accent }}
                      />
                    </div>
                    <p className="text-xs font-mono text-zinc-400 mt-0.5">{setup.tagline}</p>
                  </div>
                  {isActive && (
                    <span className="flex items-center gap-1 text-[11px] font-mono px-2.5 py-1 rounded-full bg-white/10 text-white border border-white/20">
                      <Check size={12} /> APPLIED
                    </span>
                  )}
                </div>

                <p className="text-xs text-zinc-400 mb-4 line-clamp-2 leading-relaxed">
                  {setup.description}
                </p>

                {/* Mini Preview of the widgets included */}
                <div className="bg-black/40 rounded-2xl p-3 border border-white/5 mb-4 grid grid-cols-2 gap-2">
                  {widgets.slice(0, 2).map((widget) => widget && (
                    <div key={widget.id} className="h-28 overflow-hidden pointer-events-none scale-90 origin-top-left -m-1">
                      <LiveWidget id={widget.id} accent={setup.accent} interactive={false} />
                    </div>
                  ))}
                </div>

                {/* Widget tags */}
                <div className="flex flex-wrap gap-1.5 mb-5">
                  {widgets.map((widget) => widget && (
                    <span
                      key={widget.id}
                      className="text-[10px] font-mono px-2 py-0.5 rounded-md bg-white/5 text-zinc-400 border border-white/5"
                    >
                      {widget.title}
                    </span>
                  ))}
                </div>
              </div>

              {/* Action Button */}
              <button
                onClick={() => onApplySetup(setup)}
                className={`w-full py-2.5 rounded-xl font-mono text-xs font-bold flex items-center justify-center gap-2 transition-all ${
                  isActive
                    ? 'bg-white/10 text-white cursor-default'
                    : 'bg-white text-black hover:bg-zinc-200 shadow-md active:scale-[0.99]'
                }`}
              >
                {isActive ? (
                  <>
                    <Check size={14} /> CURRENTLY IN SIMULATOR
                  </>
                ) : (
                  <>
                    <Smartphone size={14} /> APPLY SETUP TO SIMULATOR <ArrowRight size={14} />
                  </>
                )}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};
