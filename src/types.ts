export type SizeBucket = 'S' | 'M' | 'L';

export type WidgetCategory = 
  | 'all'
  | 'clock'
  | 'calendar'
  | 'battery'
  | 'weather'
  | 'sensors'
  | 'toggles';

export interface AccentOption {
  id: string;
  name: string;
  color: string;
  bgSubtle: string;
  borderSubtle: string;
  glow: string;
}

export interface WidgetItem {
  id: string;
  title: string;
  category: Exclude<WidgetCategory, 'all'>;
  description: string;
  size: SizeBucket;
  isPopular?: boolean;
  tags: string[];
}

export interface HomescreenSetup {
  id: string;
  name: string;
  tagline: string;
  description: string;
  accent: string;
  wallpaperPattern: string;
  widgetIds: string[];
}

export interface WallpaperItem {
  id: string;
  name: string;
  tagline: string;
  pattern: 'glyph' | 'matrix' | 'radial' | 'minimal' | 'circuit' | 'dots';
  bgGradient: string;
  accentColor: string;
}
