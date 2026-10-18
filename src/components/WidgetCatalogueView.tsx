import React, { useState } from 'react';
import { WIDGETS_LIST } from '../data/mockData';
import { WidgetCategory, WidgetItem, SizeBucket } from '../types';
import { LiveWidget } from './LiveWidgets';
import { Search, Plus, Check, SlidersHorizontal, Sparkles } from 'lucide-react';

interface WidgetCatalogueViewProps {
  currentAccent: string;
  onAddWidgetToSimulator: (id: string) => void;
  activeSimulatorWidgetIds: string[];
}

export const WidgetCatalogueView: React.FC<WidgetCatalogueViewProps> = ({
  currentAccent,
  onAddWidgetToSimulator,
  activeSimulatorWidgetIds,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<WidgetCategory>('all');
  const [selectedSize, setSelectedSize] = useState<SizeBucket | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const categories: { id: WidgetCategory; label: string }[] = [
    { id: 'all', label: 'ALL WIDGETS' },
    { id: 'clock', label: 'CLOCKS' },
    { id: 'weather', label: 'WEATHER' },
    { id: 'battery', label: 'BATTERY' },
    { id: 'sensors', label: 'SENSORS & SYSTEM' },
    { id: 'toggles', label: 'QUICK TOGGLES' },
    { id: 'calendar', label: 'CALENDAR' },
  ];

  const filteredWidgets = WIDGETS_LIST.filter((widget) => {
    if (selectedCategory !== 'all' && widget.category !== selectedCategory) return false;
    if (selectedSize !== 'ALL' && widget.size !== selectedSize) return false;
    if (searchQuery.trim() !== '') {
      const q = searchQuery.toLowerCase();
      const matchTitle = widget.title.toLowerCase().includes(q);
      const matchDesc = widget.description.toLowerCase().includes(q);
      const matchTags = widget.tags.some(t => t.toLowerCase().includes(q));
      if (!matchTitle && !matchDesc && !matchTags) return false;
    }
    return true;
  });

  return (
    <div className="space-y-6">
      {/* Category Pills & Filters */}
      <div className="flex flex-col gap-4 pb-4 border-b border-white/10">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
          {/* Search bar */}
          <div className="relative flex-1 max-w-md">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-500" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search widgets by name, sensor, or feature..."
              className="w-full pl-10 pr-4 py-2 bg-zinc-900 border border-white/10 rounded-xl text-xs font-mono text-white placeholder-zinc-500 focus:outline-none focus:border-white/30"
            />
          </div>

          {/* Size Filter */}
          <div className="flex items-center gap-1.5 self-start md:self-auto">
            <span className="text-xs font-mono text-zinc-500 mr-1 flex items-center gap-1">
              <SlidersHorizontal size={12} /> SIZE:
            </span>
            {(['ALL', 'S', 'M'] as const).map((size) => (
              <button
                key={size}
                onClick={() => setSelectedSize(size)}
                className={`px-3 py-1 rounded-lg text-xs font-mono font-medium transition-colors ${
                  selectedSize === size
                    ? 'bg-white text-black font-bold'
                    : 'bg-zinc-900 text-zinc-400 hover:text-white border border-white/5'
                }`}
              >
                {size}
              </button>
            ))}
          </div>
        </div>

        {/* Categories Bar */}
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-1">
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(cat.id)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-mono font-medium whitespace-nowrap transition-colors ${
                selectedCategory === cat.id
                  ? 'bg-white/15 text-white border border-white/30'
                  : 'bg-zinc-900/80 text-zinc-400 hover:text-zinc-200 border border-white/5'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </div>

      {/* Widgets Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {filteredWidgets.map((widget) => {
          const isPlaced = activeSimulatorWidgetIds.includes(widget.id);

          return (
            <div
              key={widget.id}
              className="bg-zinc-900/60 rounded-3xl p-4 border border-white/10 hover:border-white/20 transition-all flex flex-col justify-between group shadow-lg"
            >
              <div>
                {/* Header */}
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div>
                    <h3 className="text-sm font-bold text-white font-mono flex items-center gap-2">
                      {widget.title}
                      {widget.isPopular && (
                        <span
                          className="w-1.5 h-1.5 rounded-full animate-pulse"
                          style={{ backgroundColor: currentAccent }}
                          title="Popular widget"
                        />
                      )}
                    </h3>
                    <span className="text-[10px] font-mono text-zinc-500 uppercase">
                      {widget.category} • SIZE {widget.size}
                    </span>
                  </div>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-white/5 text-zinc-400 border border-white/5">
                    {widget.size === 'S' ? '1x1' : '2x1'}
                  </span>
                </div>

                {/* Live Widget Preview Card */}
                <div className="h-36 mb-4 flex items-center justify-center bg-black/40 rounded-2xl p-2 border border-white/5 overflow-hidden">
                  <div className="w-full h-full">
                    <LiveWidget id={widget.id} accent={currentAccent} />
                  </div>
                </div>

                {/* Description */}
                <p className="text-xs text-zinc-400 mb-4 line-clamp-2">
                  {widget.description}
                </p>
              </div>

              {/* Action Button */}
              <button
                onClick={() => onAddWidgetToSimulator(widget.id)}
                className={`w-full py-2 px-3 rounded-xl font-mono text-xs font-bold flex items-center justify-center gap-2 transition-all ${
                  isPlaced
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    : 'bg-white text-black hover:bg-zinc-200 active:scale-[0.99]'
                }`}
              >
                {isPlaced ? (
                  <>
                    <Check size={14} /> ON HOMESCREEN
                  </>
                ) : (
                  <>
                    <Plus size={14} /> ADD TO HOMESCREEN
                  </>
                )}
              </button>
            </div>
          );
        })}
      </div>

      {filteredWidgets.length === 0 && (
        <div className="py-16 text-center">
          <p className="text-sm font-mono text-zinc-500">No widgets match your search criteria.</p>
        </div>
      )}
    </div>
  );
};
