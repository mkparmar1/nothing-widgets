import React from 'react';
import { Download, Smartphone, Cpu, ShieldCheck, Terminal, Layers, ExternalLink } from 'lucide-react';
import { AccentOption } from '../types';
import { ACCENTS } from '../data/mockData';

interface SettingsViewProps {
  currentAccent: string;
  onSelectAccent: (accent: AccentOption) => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  currentAccent,
  onSelectAccent,
}) => {
  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      {/* Header */}
      <div className="pb-4 border-b border-white/10">
        <h2 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
          <Terminal size={20} style={{ color: currentAccent }} />
          STUDIO SETTINGS & ANDROID BUILD
        </h2>
        <p className="text-sm text-zinc-400 mt-1">
          Technical specifications, Android APK package distribution, and dot-matrix hardware rendering parameters.
        </p>
      </div>

      {/* APK Distribution Card */}
      <div className="bg-gradient-to-br from-zinc-900 to-black rounded-3xl p-6 border border-white/15 shadow-2xl relative overflow-hidden">
        <div
          className="absolute -right-10 -top-10 w-48 h-48 rounded-full opacity-20 pointer-events-none blur-3xl"
          style={{ backgroundColor: currentAccent }}
        />

        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-white/10 text-white border border-white/20">
                v1.0.0 RELEASE
              </span>
              <span className="text-xs font-mono text-emerald-400 flex items-center gap-1">
                <ShieldCheck size={14} /> SIGNED DEBUG BUILD READY
              </span>
            </div>
            <h3 className="text-lg font-bold text-white font-mono">
              Dot Widgets for Android (APK)
            </h3>
            <p className="text-xs text-zinc-400 max-w-xl leading-relaxed">
              Standalone native Android app with Glance & Jetpack Compose widget providers, Glance receiver updates, Room DB preset storage, and real-time battery / sensor broadcasts.
            </p>
          </div>

          <div className="flex flex-col gap-2 w-full md:w-auto">
            <a
              href="/app-debug.apk"
              download="DotWidgets-v1.0.0.apk"
              className="px-6 py-3.5 rounded-2xl font-mono text-xs font-bold bg-white text-black hover:bg-zinc-200 flex items-center justify-center gap-2 transition-all shadow-lg active:scale-95"
            >
              <Download size={16} />
              DOWNLOAD APK (23.1 MB)
            </a>
            <span className="text-[10px] font-mono text-zinc-500 text-center">
              Verified Android Package • Min SDK 24+
            </span>
          </div>
        </div>

        {/* Nothing Phone Install Guide */}
        <div className="mt-4 p-4 rounded-2xl bg-black/40 border border-white/5 space-y-2">
          <span className="text-[11px] font-mono font-bold text-zinc-300 block">
            HOW TO INSTALL ON NOTHING PHONE / ANDROID:
          </span>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-[11px] font-mono text-zinc-400">
            <div className="flex items-start gap-1.5">
              <span className="text-white bg-zinc-800 rounded-full w-4 h-4 flex items-center justify-center text-[9px] shrink-0">1</span>
              <span>Tap <strong>DOWNLOAD APK</strong> (file size: ~23.1 MB).</span>
            </div>
            <div className="flex items-start gap-1.5">
              <span className="text-white bg-zinc-800 rounded-full w-4 h-4 flex items-center justify-center text-[9px] shrink-0">2</span>
              <span>Open the download & allow <em>"Install unknown apps"</em> if prompted.</span>
            </div>
            <div className="flex items-start gap-1.5">
              <span className="text-white bg-zinc-800 rounded-full w-4 h-4 flex items-center justify-center text-[9px] shrink-0">3</span>
              <span>Long-press home screen &gt; <em>Widgets</em> &gt; <em>Dot Widgets</em> to place!</span>
            </div>
          </div>
        </div>

        {/* Build metadata pills */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6 pt-6 border-t border-white/10 text-xs font-mono">
          <div>
            <span className="text-zinc-500 block text-[10px]">PACKAGE</span>
            <span className="text-zinc-300 font-medium">com.aistudio.dotwidgets</span>
          </div>
          <div>
            <span className="text-zinc-500 block text-[10px]">SIGNING</span>
            <span className="text-zinc-300 font-medium">Debug RSA 2048</span>
          </div>
          <div>
            <span className="text-zinc-500 block text-[10px]">UI FRAMEWORK</span>
            <span className="text-zinc-300 font-medium">Jetpack Compose + Glance</span>
          </div>
          <div>
            <span className="text-zinc-500 block text-[10px]">REFRESH INTERVAL</span>
            <span className="text-zinc-300 font-medium">Real-time / 15m alarm</span>
          </div>
        </div>
      </div>

      {/* Global Accent Theme Selector */}
      <div className="bg-zinc-900/50 rounded-3xl p-6 border border-white/10 space-y-4">
        <h3 className="text-base font-bold text-white font-mono flex items-center gap-2">
          <Layers size={18} style={{ color: currentAccent }} />
          SIGNATURE ACCENT COLOR PALETTE
        </h3>
        <p className="text-xs text-zinc-400">
          Select your primary luminescent accent color used for dot dials, indicator dots, active states, and highlights.
        </p>

        <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-7 gap-3 pt-2">
          {ACCENTS.map((acc) => {
            const isSelected = acc.color === currentAccent;
            return (
              <button
                key={acc.id}
                onClick={() => onSelectAccent(acc)}
                className={`p-3 rounded-2xl border transition-all flex flex-col items-center gap-2 text-center ${
                  isSelected
                    ? 'border-white bg-white/10 shadow-lg ring-1 ring-white/30'
                    : 'border-white/10 bg-black/40 hover:border-white/20'
                }`}
              >
                <div
                  className="w-7 h-7 rounded-full shadow-inner border border-white/20"
                  style={{
                    backgroundColor: acc.color,
                    boxShadow: isSelected ? `0 0 12px ${acc.color}` : 'none',
                  }}
                />
                <span className="text-[11px] font-mono font-medium text-white">{acc.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Font & Rendering Architecture */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-zinc-900/40 rounded-3xl p-5 border border-white/10 space-y-2">
          <h4 className="text-sm font-bold font-mono text-white flex items-center gap-2">
            <Cpu size={16} style={{ color: currentAccent }} />
            5x7 Dot-Matrix Font Engine
          </h4>
          <p className="text-xs text-zinc-400 leading-relaxed">
            Custom discrete bitmask typography renderer for zero anti-aliasing fuzziness. Supports full alphanumeric matrix, colon pulse, degree units, and percentage readouts on arbitrary canvas resolutions.
          </p>
        </div>

        <div className="bg-zinc-900/40 rounded-3xl p-5 border border-white/10 space-y-2">
          <h4 className="text-sm font-bold font-mono text-white flex items-center gap-2">
            <Smartphone size={16} style={{ color: currentAccent }} />
            App Widget Host Protocol
          </h4>
          <p className="text-xs text-zinc-400 leading-relaxed">
            Glance-based widget implementation delivers battery-efficient rendering without background daemon drains. Uses Android AlarmManager and broadcast receivers for live tick events.
          </p>
        </div>
      </div>
    </div>
  );
};
