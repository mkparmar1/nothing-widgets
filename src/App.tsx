import React, { useState } from 'react';
import { 
  Sparkles, 
  Smartphone, 
  Wallpaper, 
  Sliders, 
  Layers, 
  Palette, 
  Download,
  Plus,
  RefreshCw,
  Zap,
  Info
} from 'lucide-react';
import { ACCENTS, CURATED_SETUPS, WALLPAPERS, WIDGETS_LIST } from './data/mockData';
import { AccentOption, HomescreenSetup, WallpaperItem } from './types';
import { PhoneSimulator } from './components/PhoneSimulator';
import { WidgetCatalogueView } from './components/WidgetCatalogueView';
import { SetupsView } from './components/SetupsView';
import { WallpapersView } from './components/WallpapersView';
import { SettingsView } from './components/SettingsView';

export const App: React.FC = () => {
  const [currentAccent, setCurrentAccent] = useState<string>('#E02B2B');
  const [activeTab, setActiveTab] = useState<'catalogue' | 'simulator' | 'setups' | 'wallpapers' | 'settings'>('catalogue');
  const [activeWallpaperPattern, setActiveWallpaperPattern] = useState<string>('glyph');
  const [activeWallpaperId, setActiveWallpaperId] = useState<string>('wp_glyph');
  const [activeSetupId, setActiveSetupId] = useState<string>('setup.nothing_red');
  
  // Placed widgets in the simulator
  const [placedWidgetIds, setPlacedWidgetIds] = useState<string[]>([
    'clock.dotdigital.m',
    'battery.ring',
    'weather.current.temp',
    'sensor.visualizer.eq',
  ]);

  const [showAccentPicker, setShowAccentPicker] = useState<boolean>(false);

  // Apply a curated setup
  const handleApplySetup = (setup: HomescreenSetup) => {
    setActiveSetupId(setup.id);
    setCurrentAccent(setup.accent);
    setActiveWallpaperPattern(setup.wallpaperPattern);
    setPlacedWidgetIds([...setup.widgetIds]);
  };

  // Select wallpaper
  const handleSelectWallpaper = (wp: WallpaperItem) => {
    setActiveWallpaperId(wp.id);
    setActiveWallpaperPattern(wp.pattern);
  };

  // Add widget to simulator
  const handleAddWidget = (widgetId: string) => {
    if (placedWidgetIds.includes(widgetId)) {
      setPlacedWidgetIds(placedWidgetIds.filter(id => id !== widgetId));
    } else {
      if (placedWidgetIds.length >= 5) {
        // Replace last
        setPlacedWidgetIds([...placedWidgetIds.slice(0, 4), widgetId]);
      } else {
        setPlacedWidgetIds([...placedWidgetIds, widgetId]);
      }
    }
  };

  // Remove widget
  const handleRemoveWidget = (index: number) => {
    const updated = [...placedWidgetIds];
    updated.splice(index, 1);
    setPlacedWidgetIds(updated);
  };

  return (
    <div className="min-h-screen bg-[#0A0A0A] text-[#F2F2F2] flex flex-col font-sans selection:bg-[#E02B2B]/30 selection:text-white">
      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-50 bg-[#0E0E0E]/90 backdrop-blur-md border-b border-white/10 px-4 sm:px-8 py-3.5">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
          {/* Brand */}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="text-base sm:text-lg font-bold font-mono tracking-wider text-white">
                DOT WIDGETS
              </span>
              <span
                className="w-2.5 h-2.5 rounded-full transition-colors duration-300 shadow-sm"
                style={{
                  backgroundColor: currentAccent,
                  boxShadow: `0 0 10px ${currentAccent}`,
                }}
              />
            </div>
            <span className="hidden md:inline-block text-[10px] font-mono px-2 py-0.5 rounded-full bg-white/10 text-zinc-400 border border-white/10">
              NOTHING OS STYLE
            </span>
          </div>

          {/* Quick Controls & Accents */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Color Accent Picker Trigger */}
            <div className="relative">
              <button
                onClick={() => setShowAccentPicker(!showAccentPicker)}
                className="px-3 py-1.5 rounded-xl bg-zinc-900 border border-white/10 hover:border-white/25 text-xs font-mono flex items-center gap-2 transition-colors"
                title="Change accent color"
              >
                <span
                  className="w-3.5 h-3.5 rounded-full"
                  style={{ backgroundColor: currentAccent }}
                />
                <span className="hidden sm:inline">ACCENT</span>
              </button>

              {/* Accent Dropdown Popover */}
              {showAccentPicker && (
                <div className="absolute right-0 mt-2 p-3 bg-zinc-900 border border-white/20 rounded-2xl shadow-2xl z-50 flex flex-col gap-2 min-w-[200px]">
                  <div className="text-[10px] font-mono text-zinc-400 px-1">SELECT ACCENT</div>
                  <div className="grid grid-cols-4 gap-2">
                    {ACCENTS.map((acc) => (
                      <button
                        key={acc.id}
                        onClick={() => {
                          setCurrentAccent(acc.color);
                          setShowAccentPicker(false);
                        }}
                        className={`w-9 h-9 rounded-xl border flex items-center justify-center transition-transform hover:scale-110 ${
                          currentAccent === acc.color ? 'border-white ring-2 ring-white/20' : 'border-white/10'
                        }`}
                        style={{ backgroundColor: acc.color }}
                        title={acc.name}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* APK Download Button */}
            <a
              href="/app-debug.apk"
              download="DotWidgets-v1.0.0.apk"
              className="px-3 sm:px-4 py-1.5 rounded-xl text-xs font-mono font-bold bg-white text-black hover:bg-zinc-200 flex items-center gap-1.5 transition-all shadow-md"
            >
              <Download size={14} />
              <span className="hidden sm:inline">GET</span> APK
            </a>
          </div>
        </div>

        {/* Tab Navigation Navigation Items */}
        <div className="max-w-7xl mx-auto flex items-center gap-1 sm:gap-2 mt-3 pt-2 border-t border-white/5 overflow-x-auto no-scrollbar">
          {[
            { id: 'catalogue', label: 'WIDGETS', icon: Sliders },
            { id: 'simulator', label: 'PHONE SIMULATOR', icon: Smartphone },
            { id: 'setups', label: 'CURATED SETUPS', icon: Sparkles },
            { id: 'wallpapers', label: 'WALLPAPERS', icon: Wallpaper },
            { id: 'settings', label: 'ANDROID & SETTINGS', icon: Layers },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-mono font-medium transition-all whitespace-nowrap ${
                  isActive
                    ? 'bg-white text-black font-bold shadow-md'
                    : 'text-zinc-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <Icon size={14} style={{ color: isActive ? '#000000' : currentAccent }} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-8">
        {activeTab === 'catalogue' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Widget Catalogue List */}
            <div className="lg:col-span-8">
              <WidgetCatalogueView
                currentAccent={currentAccent}
                onAddWidgetToSimulator={handleAddWidget}
                activeSimulatorWidgetIds={placedWidgetIds}
              />
            </div>

            {/* Sticky Phone Simulator Preview on Desktop */}
            <div className="hidden lg:block lg:col-span-4 sticky top-32">
              <div className="p-4 rounded-3xl bg-zinc-900/30 border border-white/10 backdrop-blur-xs">
                <div className="flex items-center justify-between mb-3 px-2">
                  <span className="text-xs font-mono text-zinc-400 flex items-center gap-1.5">
                    <Smartphone size={14} style={{ color: currentAccent }} />
                    LIVE HOMESCREEN
                  </span>
                  <span className="text-[10px] font-mono text-zinc-500">
                    {placedWidgetIds.length}/5 PLACED
                  </span>
                </div>
                <PhoneSimulator
                  accent={currentAccent}
                  wallpaperPattern={activeWallpaperPattern}
                  activeWidgetIds={placedWidgetIds}
                  onRemoveWidget={handleRemoveWidget}
                  onAddWidgetPrompt={() => {}}
                />
              </div>
            </div>
          </div>
        )}

        {activeTab === 'simulator' && (
          <div className="max-w-xl mx-auto space-y-6">
            <div className="text-center space-y-1">
              <h2 className="text-xl font-bold font-mono text-white">INTERACTIVE PHONE SIMULATOR</h2>
              <p className="text-xs font-mono text-zinc-400">
                Tap widgets to trigger actions, remove or add components, and experience the Nothing OS aesthetic.
              </p>
            </div>
            <PhoneSimulator
              accent={currentAccent}
              wallpaperPattern={activeWallpaperPattern}
              activeWidgetIds={placedWidgetIds}
              onRemoveWidget={handleRemoveWidget}
              onAddWidgetPrompt={() => setActiveTab('catalogue')}
            />
          </div>
        )}

        {activeTab === 'setups' && (
          <SetupsView
            currentAccent={currentAccent}
            onApplySetup={handleApplySetup}
            activeSetupId={activeSetupId}
          />
        )}

        {activeTab === 'wallpapers' && (
          <WallpapersView
            currentAccent={currentAccent}
            activeWallpaperId={activeWallpaperId}
            onSelectWallpaper={handleSelectWallpaper}
          />
        )}

        {activeTab === 'settings' && (
          <SettingsView
            currentAccent={currentAccent}
            onSelectAccent={(acc) => setCurrentAccent(acc.color)}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="mt-auto border-t border-white/10 py-6 px-4 text-center text-xs font-mono text-zinc-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span>DOT WIDGETS STUDIO</span>
            <span>•</span>
            <span style={{ color: currentAccent }}>V1.0.0</span>
          </div>
          <div className="flex items-center gap-4 text-[11px]">
            <span>GLANCE WIDGET PROVIDER</span>
            <span>JETPACK COMPOSE</span>
            <span>5x7 BITMASK TYPOGRAPHY</span>
          </div>
        </div>
      </footer>
    </div>
  );
};
export default App;
