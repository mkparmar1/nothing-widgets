import React, { useState, useEffect } from 'react';
import { 
  Wifi, 
  Bluetooth, 
  Flashlight, 
  BellOff, 
  Play, 
  Pause, 
  RotateCcw, 
  Flag,
  CloudSun,
  Sun,
  CloudRain,
  Compass,
  Activity,
  Zap,
  TrendingUp,
  Cpu
} from 'lucide-react';
import { DotString } from './DotDigit';

interface WidgetRenderProps {
  id: string;
  accent: string;
  scale?: number;
  interactive?: boolean;
}

export const LiveWidget: React.FC<WidgetRenderProps> = ({
  id,
  accent,
  scale = 1,
  interactive = true,
}) => {
  // Live Clock Hook
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Audio Visualizer Hook
  const [eqBars, setEqBars] = useState([3, 5, 7, 4, 8, 6, 4, 2]);
  useEffect(() => {
    if (id !== 'sensor.visualizer.eq') return;
    const interval = setInterval(() => {
      setEqBars(prev =>
        prev.map(() => Math.floor(Math.random() * 8) + 1)
      );
    }, 120);
    return () => clearInterval(interval);
  }, [id]);

  // Compass Heading Hook
  const [heading, setHeading] = useState(48);
  useEffect(() => {
    if (id !== 'sensor.compass.heading') return;
    const interval = setInterval(() => {
      setHeading(h => (h + 1) % 360);
    }, 400);
    return () => clearInterval(interval);
  }, [id]);

  // Stopwatch state
  const [swRunning, setSwRunning] = useState(false);
  const [swTime, setSwTime] = useState(14820); // ms
  const [swLaps, setSwLaps] = useState<number[]>([12400]);

  useEffect(() => {
    if (!swRunning) return;
    const start = Date.now() - swTime;
    const interval = setInterval(() => {
      setSwTime(Date.now() - start);
    }, 33);
    return () => clearInterval(interval);
  }, [swRunning, swTime]);

  // Toggles state
  const [wifiOn, setWifiOn] = useState(true);
  const [btOn, setBtOn] = useState(true);
  const [torchOn, setTorchOn] = useState(false);
  const [dndOn, setDndOn] = useState(false);

  // Real Battery state (detects device battery via navigator.getBattery or interactive toggle)
  const [batteryLevel, setBatteryLevel] = useState(84);
  const [isCharging, setIsCharging] = useState(true);

  useEffect(() => {
    if (typeof navigator !== 'undefined' && 'getBattery' in navigator) {
      (navigator as any).getBattery?.().then((bm: any) => {
        setBatteryLevel(Math.round(bm.level * 100));
        setIsCharging(bm.charging);
        bm.addEventListener('levelchange', () => setBatteryLevel(Math.round(bm.level * 100)));
        bm.addEventListener('chargingchange', () => setIsCharging(bm.charging));
      }).catch(() => {});
    }
  }, []);

  // Focus Countdown Timer state
  const [timerRunning, setTimerRunning] = useState(false);
  const [timerRemaining, setTimerRemaining] = useState(25 * 60);

  useEffect(() => {
    if (!timerRunning) return;
    const interval = setInterval(() => {
      setTimerRemaining(prev => (prev > 0 ? prev - 1 : 25 * 60));
    }, 1000);
    return () => clearInterval(interval);
  }, [timerRunning]);

  // Dice Roll Game state
  const [dice, setDice] = useState<[number, number]>([3, 5]);
  const rollDice = () => {
    setDice([Math.floor(Math.random() * 6) + 1, Math.floor(Math.random() * 6) + 1]);
  };

  // Retro Snake Game state
  const [snakeBody, setSnakeBody] = useState<[number, number][]>([[3, 2], [4, 2], [5, 2], [6, 2], [7, 2]]);
  const [snakeFood, setSnakeFood] = useState<[number, number]>([11, 3]);
  const [snakeScore, setSnakeScore] = useState(42);

  const stepSnake = () => {
    const head = snakeBody[snakeBody.length - 1];
    const dx = head[0] < snakeFood[0] ? 1 : head[0] > snakeFood[0] ? -1 : 0;
    const dy = dx === 0 ? (head[1] < snakeFood[1] ? 1 : head[1] > snakeFood[1] ? -1 : 0) : 0;
    const nextX = ((head[0] + (dx !== 0 ? dx : 1)) % 15 + 15) % 15;
    const nextY = ((head[1] + dy) % 8 + 8) % 8;
    const nextHead: [number, number] = [nextX, nextY];
    const ate = nextHead[0] === snakeFood[0] && nextHead[1] === snakeFood[1];
    const nextBody = [...snakeBody, nextHead];
    if (!ate) nextBody.shift();
    setSnakeBody(nextBody);
    if (ate) {
      setSnakeScore(s => s + 10);
      setSnakeFood([Math.floor(Math.random() * 15), Math.floor(Math.random() * 8)]);
    }
  };

  // Glyph lighting state
  const [glyphSegment, setGlyphSegment] = useState<number>(0);
  useEffect(() => {
    if (id !== 'sensor.glyph.lights') return;
    const interval = setInterval(() => {
      setGlyphSegment(s => (s + 1) % 6);
    }, 800);
    return () => clearInterval(interval);
  }, [id]);

  const hoursStr = String(now.getHours()).padStart(2, '0');
  const minsStr = String(now.getMinutes()).padStart(2, '0');
  const secsStr = String(now.getSeconds()).padStart(2, '0');
  const dayStr = String(now.getDate()).padStart(2, '0');
  const monthNames = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
  const daysNames = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];

  // Base widget container styling
  const containerClasses = "relative bg-[#111111]/90 backdrop-blur-md rounded-2xl border border-white/10 p-3 flex flex-col justify-between overflow-hidden shadow-lg select-none transition-all duration-200 hover:border-white/20";

  switch (id) {
    // ----------------------------------------------------
    // CLOCKS
    // ----------------------------------------------------
    case 'clock.dotdigital.m':
      return (
        <div className={`${containerClasses} h-full min-h-[120px]`}>
          <div className="flex items-center justify-between text-xs font-mono text-zinc-400">
            <span className="flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: accent }} />
              {daysNames[now.getDay()]} {monthNames[now.getMonth()]} {dayStr}
            </span>
            <span className="text-[10px] tracking-widest text-zinc-500">DOT-CLOCK</span>
          </div>

          <div className="my-auto py-1 flex items-center justify-center gap-3">
            <DotString text={hoursStr} dotSize={3.5} dotGap={2} activeColor="#FFFFFF" />
            <div className="flex flex-col gap-2">
              <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: accent }} />
              <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: accent }} />
            </div>
            <DotString text={minsStr} dotSize={3.5} dotGap={2} activeColor="#FFFFFF" />
          </div>

          <div className="flex items-center justify-between pt-1 border-t border-white/5 text-[11px] font-mono text-zinc-400">
            <div className="flex items-center gap-1">
              <span className="text-zinc-500">SEC</span>
              <span className="text-white font-bold">{secsStr}</span>
            </div>
            <div className="flex gap-0.5">
              {Array.from({ length: 12 }).map((_, i) => (
                <div
                  key={i}
                  className="w-1.5 h-1 rounded-sm"
                  style={{
                    backgroundColor: i <= Math.floor(now.getSeconds() / 5) ? accent : '#222222',
                  }}
                />
              ))}
            </div>
          </div>
        </div>
      );

    case 'clock.analog.ring': {
      const secAngle = (now.getSeconds() / 60) * 360;
      const minAngle = (now.getMinutes() / 60) * 360;
      const hrAngle = ((now.getHours() % 12) / 12) * 360 + (now.getMinutes() / 60) * 30;

      return (
        <div className={`${containerClasses} h-full aspect-square flex flex-col items-center justify-center p-2`}>
          <div className="relative w-24 h-24 flex items-center justify-center">
            {/* 12 Outer Hour Dots */}
            {Array.from({ length: 12 }).map((_, i) => {
              const angle = (i * 30 * Math.PI) / 180;
              const x = 48 + 40 * Math.sin(angle);
              const y = 48 - 40 * Math.cos(angle);
              const isQuarter = i % 3 === 0;
              return (
                <div
                  key={i}
                  className="absolute rounded-full"
                  style={{
                    left: `${x - (isQuarter ? 2.5 : 1.5)}px`,
                    top: `${y - (isQuarter ? 2.5 : 1.5)}px`,
                    width: isQuarter ? '5px' : '3px',
                    height: isQuarter ? '5px' : '3px',
                    backgroundColor: isQuarter ? accent : '#555555',
                  }}
                />
              );
            })}

            {/* Analog Hands in dot matrix style */}
            <div
              className="absolute w-1 h-7 rounded-full origin-bottom"
              style={{
                backgroundColor: '#FFFFFF',
                bottom: '48px',
                transform: `rotate(${hrAngle}deg)`,
              }}
            />
            <div
              className="absolute w-0.5 h-10 rounded-full origin-bottom"
              style={{
                backgroundColor: '#DDDDDD',
                bottom: '48px',
                transform: `rotate(${minAngle}deg)`,
              }}
            />
            <div
              className="absolute w-0.5 h-11 origin-bottom"
              style={{
                backgroundColor: accent,
                bottom: '48px',
                transform: `rotate(${secAngle}deg)`,
              }}
            />
            <div className="w-2 h-2 rounded-full z-10" style={{ backgroundColor: accent }} />
          </div>
          <span className="text-[10px] font-mono text-zinc-400 mt-1">
            {hoursStr}:{minsStr}
          </span>
        </div>
      );
    }

    case 'clock.stacked.hhmm':
      return (
        <div className={`${containerClasses} h-full aspect-square flex flex-col items-center justify-center p-3`}>
          <div className="flex flex-col items-center gap-1.5">
            <DotString text={hoursStr} dotSize={3} dotGap={1.5} activeColor="#FFFFFF" />
            <div className="w-8 h-0.5 rounded-full" style={{ backgroundColor: accent }} />
            <DotString text={minsStr} dotSize={3} dotGap={1.5} activeColor={accent} />
          </div>
          <span className="text-[9px] font-mono text-zinc-500 mt-2 tracking-widest">LOCAL</span>
        </div>
      );

    case 'clock.seconds.bar':
      return (
        <div className={`${containerClasses} h-full min-h-[110px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span>CHRONO TIME</span>
            <span className="text-white font-bold">{secsStr}s</span>
          </div>
          <div className="my-auto flex items-center justify-center gap-2">
            <DotString text={`${hoursStr}:${minsStr}`} dotSize={3.2} dotGap={2} activeColor="#FFFFFF" />
          </div>
          <div className="grid grid-cols-15 gap-1 py-1">
            {Array.from({ length: 30 }).map((_, i) => (
              <div
                key={i}
                className="h-1.5 rounded-xs"
                style={{
                  backgroundColor: i <= Math.floor(now.getSeconds() / 2) ? accent : '#222222',
                }}
              />
            ))}
          </div>
        </div>
      );

    case 'clock.binary': {
      const h = now.getHours();
      const m = now.getMinutes();
      const s = now.getSeconds();
      const cols = [
        Math.floor(h / 10), h % 10,
        Math.floor(m / 10), m % 10,
        Math.floor(s / 10), s % 10,
      ];
      return (
        <div className={`${containerClasses} h-full aspect-square flex flex-col justify-between p-3`}>
          <div className="flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>BINARY BCD</span>
            <span style={{ color: accent }}>{hoursStr}:{minsStr}</span>
          </div>
          <div className="flex justify-around items-center py-1">
            {cols.map((val, colIdx) => (
              <div key={colIdx} className="flex flex-col gap-1.5 items-center">
                {[8, 4, 2, 1].map((bit, bitIdx) => {
                  const active = (val & bit) !== 0;
                  return (
                    <div
                      key={bitIdx}
                      className="w-2 h-2 rounded-full transition-all"
                      style={{
                        backgroundColor: active ? accent : '#222222',
                        boxShadow: active ? `0 0 4px ${accent}` : 'none',
                      }}
                    />
                  );
                })}
              </div>
            ))}
          </div>
          <div className="text-[9px] font-mono text-center text-zinc-600">8 4 2 1 MATRIX</div>
        </div>
      );
    }

    // ----------------------------------------------------
    // BATTERY WIDGETS
    // ----------------------------------------------------
    case 'battery.ring': {
      return (
        <div 
          onClick={() => setIsCharging(!isCharging)}
          title="Click to toggle charger plugged / unplugged"
          className={`${containerClasses} h-full aspect-square flex flex-col items-center justify-between p-3 cursor-pointer`}
        >
          <div className="w-full flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>{isCharging ? 'CHARGING' : 'BATTERY'}</span>
            <Zap size={12} style={{ color: isCharging ? accent : '#555555' }} className={isCharging ? 'animate-pulse' : ''} />
          </div>
          <div className="relative w-20 h-20 flex items-center justify-center">
            {Array.from({ length: 20 }).map((_, i) => {
              const angle = (i * (360 / 20) * Math.PI) / 180;
              const x = 38 + 32 * Math.sin(angle);
              const y = 38 - 32 * Math.cos(angle);
              const active = i < Math.round((batteryLevel / 100) * 20);
              return (
                <div
                  key={i}
                  className="absolute w-2 h-2 rounded-full"
                  style={{
                    left: `${x - 1}px`,
                    top: `${y - 1}px`,
                    backgroundColor: active ? accent : '#262626',
                    boxShadow: active ? `0 0 3px ${accent}` : 'none',
                  }}
                />
              );
            })}
            <div className="flex flex-col items-center">
              <span className="text-xl font-bold font-mono text-white">{batteryLevel}</span>
              <span className="text-[9px] font-mono" style={{ color: accent }}>%</span>
            </div>
          </div>
          <span className="text-[9px] font-mono text-zinc-500">CHARGING • 4.1V</span>
        </div>
      );
    }

    case 'battery.segmented':
      return (
        <div className={`${containerClasses} h-full min-h-[110px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span className="flex items-center gap-1.5">
              <Zap size={14} style={{ color: accent }} />
              BATTERY VITALS
            </span>
            <span className="text-white font-bold">84%</span>
          </div>

          <div className="my-auto py-1">
            <div className="flex gap-1.5">
              {Array.from({ length: 16 }).map((_, i) => {
                const filled = i < 14;
                return (
                  <div
                    key={i}
                    className="flex-1 h-5 rounded-xs transition-all"
                    style={{
                      backgroundColor: filled ? (i < 4 ? '#E02B2B' : accent) : '#222222',
                    }}
                  />
                );
              })}
            </div>
          </div>

          <div className="flex justify-between text-[10px] font-mono text-zinc-500">
            <span>TEMP: 31.4°C</span>
            <span>HEALTH: GOOD</span>
            <span>DISCHARGE: -240mA</span>
          </div>
        </div>
      );

    case 'battery.quad':
      return (
        <div className={`${containerClasses} h-full min-h-[120px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400 mb-2">
            <span>ECOSYSTEM POWER</span>
            <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: accent }} />
          </div>
          <div className="grid grid-cols-2 gap-2 my-auto">
            <div className="bg-black/40 rounded-xl p-2 border border-white/5 flex items-center justify-between">
              <div>
                <div className="text-[9px] font-mono text-zinc-500">PHONE (2)</div>
                <div className="text-sm font-bold font-mono text-white">84%</div>
              </div>
              <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: accent }} />
            </div>
            <div className="bg-black/40 rounded-xl p-2 border border-white/5 flex items-center justify-between">
              <div>
                <div className="text-[9px] font-mono text-zinc-500">EAR (2)</div>
                <div className="text-sm font-bold font-mono text-white">100%</div>
              </div>
              <div className="w-2.5 h-2.5 rounded-full bg-emerald-400" />
            </div>
            <div className="bg-black/40 rounded-xl p-2 border border-white/5 flex items-center justify-between">
              <div>
                <div className="text-[9px] font-mono text-zinc-500">WATCH PRO</div>
                <div className="text-sm font-bold font-mono text-white">62%</div>
              </div>
              <div className="w-2.5 h-2.5 rounded-full bg-amber-400" />
            </div>
            <div className="bg-black/40 rounded-xl p-2 border border-white/5 flex items-center justify-between">
              <div>
                <div className="text-[9px] font-mono text-zinc-500">CMF BUDS</div>
                <div className="text-sm font-bold font-mono text-white">90%</div>
              </div>
              <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: accent }} />
            </div>
          </div>
        </div>
      );

    // ----------------------------------------------------
    // WEATHER WIDGETS
    // ----------------------------------------------------
    case 'weather.current.temp':
      return (
        <div className={`${containerClasses} h-full aspect-square flex flex-col justify-between p-3`}>
          <div className="flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>TOKYO</span>
            <CloudSun size={14} style={{ color: accent }} />
          </div>
          <div className="my-auto flex flex-col items-center">
            <div className="flex items-start">
              <DotString text="21" dotSize={3.5} dotGap={2} activeColor="#FFFFFF" />
              <DotString text="°" dotSize={2} dotGap={1} activeColor={accent} />
            </div>
            <span className="text-[10px] font-mono text-zinc-400 mt-1">PARTLY CLOUDY</span>
          </div>
          <div className="flex justify-between text-[9px] font-mono text-zinc-500">
            <span>H: 24°</span>
            <span>L: 16°</span>
          </div>
        </div>
      );

    case 'weather.5day.strip':
      return (
        <div className={`${containerClasses} h-full min-h-[110px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span>5-DAY OUTLOOK</span>
            <span style={{ color: accent }}>TOKYO</span>
          </div>
          <div className="grid grid-cols-5 gap-1.5 my-auto py-1">
            {[
              { day: 'TOD', temp: '21°', icon: 'sun' },
              { day: 'FRI', temp: '19°', icon: 'cloud' },
              { day: 'SAT', temp: '18°', icon: 'rain' },
              { day: 'SUN', temp: '22°', icon: 'sun' },
              { day: 'MON', temp: '23°', icon: 'sun' },
            ].map((d, i) => (
              <div key={i} className="flex flex-col items-center bg-black/30 rounded-lg p-1.5 border border-white/5">
                <span className="text-[9px] font-mono text-zinc-400">{d.day}</span>
                <div className="my-1">
                  {d.icon === 'sun' && <Sun size={12} style={{ color: accent }} />}
                  {d.icon === 'cloud' && <CloudSun size={12} className="text-zinc-400" />}
                  {d.icon === 'rain' && <CloudRain size={12} className="text-cyan-400" />}
                </div>
                <span className="text-xs font-mono font-bold text-white">{d.temp}</span>
              </div>
            ))}
          </div>
          <div className="flex justify-between text-[9px] font-mono text-zinc-500">
            <span>PRECIPITATION: 10%</span>
            <span>WIND: 8 KM/H</span>
          </div>
        </div>
      );

    case 'weather.sunrise.arc':
      return (
        <div className={`${containerClasses} h-full min-h-[110px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span>SOLAR TRACKER</span>
            <span style={{ color: accent }}>DAYLIGHT 11H 40M</span>
          </div>
          <div className="relative h-12 my-auto flex items-end justify-center">
            {/* Half circle dots */}
            {Array.from({ length: 15 }).map((_, i) => {
              const angle = (i / 14) * Math.PI;
              const x = 50 - 42 * Math.cos(angle);
              const y = 40 - 32 * Math.sin(angle);
              const isSun = i === 9;
              return (
                <div
                  key={i}
                  className="absolute rounded-full"
                  style={{
                    left: `${x}%`,
                    top: `${y}px`,
                    width: isSun ? '8px' : '4px',
                    height: isSun ? '8px' : '4px',
                    backgroundColor: isSun ? accent : i < 9 ? '#888888' : '#222222',
                    boxShadow: isSun ? `0 0 8px ${accent}` : 'none',
                  }}
                />
              );
            })}
          </div>
          <div className="flex justify-between text-[10px] font-mono text-zinc-400">
            <span>SUNRISE 05:48</span>
            <span className="font-bold text-white">NOW 14:32</span>
            <span>SUNSET 17:28</span>
          </div>
        </div>
      );

    case 'weather.lunar.moon':
      return (
        <div className={`${containerClasses} h-full aspect-square flex flex-col justify-between p-3`}>
          <div className="flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>LUNAR PHASE</span>
            <span style={{ color: accent }}>88%</span>
          </div>
          <div className="my-auto flex items-center justify-center">
            <div className="grid grid-cols-7 gap-1">
              {Array.from({ length: 49 }).map((_, i) => {
                const r = Math.floor(i / 7) - 3;
                const c = (i % 7) - 3;
                const dist = Math.sqrt(r * r + c * c);
                const inside = dist <= 3.2;
                const illuminated = inside && c >= -1.5;
                return (
                  <div
                    key={i}
                    className="w-2 h-2 rounded-full"
                    style={{
                      backgroundColor: !inside ? 'transparent' : illuminated ? accent : '#202020',
                      boxShadow: illuminated ? `0 0 3px ${accent}` : 'none',
                    }}
                  />
                );
              })}
            </div>
          </div>
          <span className="text-[9px] font-mono text-center text-zinc-400">WAXING GIBBOUS</span>
        </div>
      );

    // ----------------------------------------------------
    // SENSORS & SYSTEM
    // ----------------------------------------------------
    case 'sensor.compass.heading':
      return (
        <div className={`${containerClasses} h-full aspect-square flex flex-col items-center justify-between p-3`}>
          <div className="w-full flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>BEARING</span>
            <Compass size={12} style={{ color: accent }} />
          </div>
          <div className="relative w-20 h-20 flex items-center justify-center">
            {/* 16 dot compass dial */}
            {Array.from({ length: 16 }).map((_, i) => {
              const angle = (i * 22.5 * Math.PI) / 180;
              const x = 38 + 32 * Math.sin(angle);
              const y = 38 - 32 * Math.cos(angle);
              const isNorth = i === 0;
              return (
                <div
                  key={i}
                  className="absolute rounded-full"
                  style={{
                    left: `${x - 1}px`,
                    top: `${y - 1}px`,
                    width: isNorth ? '5px' : '3px',
                    height: isNorth ? '5px' : '3px',
                    backgroundColor: isNorth ? accent : '#444444',
                  }}
                />
              );
            })}
            <div
              className="absolute w-0.5 h-10 origin-bottom"
              style={{
                backgroundColor: accent,
                bottom: '38px',
                transform: `rotate(${heading}deg)`,
                boxShadow: `0 0 6px ${accent}`,
              }}
            />
            <div className="flex flex-col items-center z-10">
              <span className="text-sm font-bold font-mono text-white">{heading}°</span>
              <span className="text-[9px] font-mono text-zinc-400">NE</span>
            </div>
          </div>
          <span className="text-[9px] font-mono text-zinc-500">35°40' N 139°45' E</span>
        </div>
      );

    case 'sensor.visualizer.eq':
      return (
        <div className={`${containerClasses} h-full min-h-[110px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span className="flex items-center gap-1.5">
              <Activity size={14} style={{ color: accent }} />
              SPECTRUM VISUALIZER
            </span>
            <span className="text-[10px] text-zinc-500">8-BAND EQ</span>
          </div>

          <div className="my-auto py-2 flex justify-between items-end gap-1.5 px-2">
            {eqBars.map((height, bandIdx) => (
              <div key={bandIdx} className="flex-1 flex flex-col-reverse items-center gap-1">
                {Array.from({ length: 8 }).map((_, dotIdx) => {
                  const active = dotIdx < height;
                  const isPeak = dotIdx === height - 1;
                  return (
                    <div
                      key={dotIdx}
                      className="w-full h-1.5 rounded-xs transition-all duration-100"
                      style={{
                        backgroundColor: isPeak ? '#FFFFFF' : active ? accent : '#202020',
                        boxShadow: isPeak ? '0 0 4px #FFFFFF' : active ? `0 0 2px ${accent}` : 'none',
                      }}
                    />
                  );
                })}
                <span className="text-[8px] font-mono text-zinc-500 mt-1">
                  {['60', '150', '400', '1K', '2.5K', '6K', '10K', '16K'][bandIdx]}
                </span>
              </div>
            ))}
          </div>

          <div className="flex justify-between text-[10px] font-mono text-zinc-500">
            <span>SOURCE: AUDIO DSP</span>
            <span className="text-white">44.1 kHz 24-BIT</span>
          </div>
        </div>
      );

    case 'sensor.glyph.lights':
      return (
        <div className={`${containerClasses} h-full min-h-[120px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span>GLYPH MATRIX</span>
            <span style={{ color: accent }}>MODE: PULSE</span>
          </div>
          <div className="my-auto py-2 flex items-center justify-center">
            <div className="relative w-44 h-16 flex items-center justify-between px-4">
              {/* Central Ring */}
              <div
                className="w-12 h-12 rounded-full border-2 border-dashed flex items-center justify-center transition-all duration-300"
                style={{
                  borderColor: glyphSegment === 0 ? accent : '#333333',
                  boxShadow: glyphSegment === 0 ? `0 0 12px ${accent}` : 'none',
                }}
              >
                <div
                  className="w-4 h-4 rounded-full"
                  style={{ backgroundColor: glyphSegment === 0 ? accent : '#222222' }}
                />
              </div>

              {/* Diagonal strip */}
              <div
                className="w-1.5 h-10 rounded-full transition-all duration-300 transform -rotate-45"
                style={{
                  backgroundColor: glyphSegment === 1 ? accent : '#333333',
                  boxShadow: glyphSegment === 1 ? `0 0 8px ${accent}` : 'none',
                }}
              />

              {/* Arc bar */}
              <div
                className="w-14 h-8 rounded-t-full border-t-2 border-x-2 transition-all duration-300"
                style={{
                  borderColor: glyphSegment === 2 ? accent : '#333333',
                  boxShadow: glyphSegment === 2 ? `0 0 10px ${accent}` : 'none',
                }}
              />
            </div>
          </div>
          <div className="flex justify-between text-[10px] font-mono text-zinc-500">
            <span>BRIGHTNESS: 100%</span>
            <span>HARDWARE LINKED</span>
          </div>
        </div>
      );

    case 'sensor.ram.meter':
      return (
        <div className={`${containerClasses} h-full min-h-[110px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span className="flex items-center gap-1.5">
              <Cpu size={14} style={{ color: accent }} />
              RAM & VITALS
            </span>
            <span className="text-white font-bold">5.8 / 12 GB</span>
          </div>

          <div className="my-auto py-1">
            <div className="flex gap-1">
              {Array.from({ length: 24 }).map((_, i) => (
                <div
                  key={i}
                  className="flex-1 h-4 rounded-xs"
                  style={{
                    backgroundColor: i < 12 ? accent : '#222222',
                  }}
                />
              ))}
            </div>
          </div>

          <div className="flex justify-between text-[10px] font-mono text-zinc-500">
            <span>LOAD: 48%</span>
            <span>STORAGE: 182 GB FREE</span>
            <span>TEMP: 34°C</span>
          </div>
        </div>
      );

    case 'sensor.fitness.rings':
      return (
        <div className={`${containerClasses} h-full aspect-square flex flex-col justify-between p-3`}>
          <div className="flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>ACTIVITY</span>
            <span style={{ color: accent }}>84%</span>
          </div>
          <div className="relative w-20 h-20 my-auto mx-auto flex items-center justify-center">
            {/* Outer ring: Steps */}
            {Array.from({ length: 16 }).map((_, i) => {
              const angle = (i * 22.5 * Math.PI) / 180;
              const x = 38 + 32 * Math.sin(angle);
              const y = 38 - 32 * Math.cos(angle);
              const active = i < 14;
              return (
                <div
                  key={`outer-${i}`}
                  className="absolute w-2 h-2 rounded-full"
                  style={{
                    left: `${x - 1}px`,
                    top: `${y - 1}px`,
                    backgroundColor: active ? accent : '#222222',
                  }}
                />
              );
            })}
            {/* Inner ring: Calories */}
            {Array.from({ length: 12 }).map((_, i) => {
              const angle = (i * 30 * Math.PI) / 180;
              const x = 38 + 20 * Math.sin(angle);
              const y = 38 - 20 * Math.cos(angle);
              const active = i < 9;
              return (
                <div
                  key={`inner-${i}`}
                  className="absolute w-1.5 h-1.5 rounded-full"
                  style={{
                    left: `${x - 0.75}px`,
                    top: `${y - 0.75}px`,
                    backgroundColor: active ? '#FFFFFF' : '#222222',
                  }}
                />
              );
            })}
            <div className="text-center">
              <span className="text-xs font-bold font-mono text-white">8,420</span>
              <div className="text-[8px] font-mono text-zinc-500">STEPS</div>
            </div>
          </div>
          <span className="text-[9px] font-mono text-zinc-500 text-center">540 KCAL BURNED</span>
        </div>
      );

    case 'sensor.stopwatch.chrono': {
      const minutes = Math.floor(swTime / 60000);
      const seconds = Math.floor((swTime % 60000) / 1000);
      const millis = Math.floor((swTime % 1000) / 10);
      const mStr = String(minutes).padStart(2, '0');
      const sStr = String(seconds).padStart(2, '0');
      const msStr = String(millis).padStart(2, '0');

      return (
        <div className={`${containerClasses} h-full min-h-[120px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span>CHRONOMETER</span>
            <span style={{ color: accent }}>{swRunning ? 'RUNNING' : 'STOPPED'}</span>
          </div>

          <div className="my-auto py-1 flex items-center justify-center gap-2">
            <span className="text-2xl font-mono font-bold text-white tracking-wider">
              {mStr}:{sStr}
            </span>
            <span className="text-lg font-mono font-bold" style={{ color: accent }}>
              .{msStr}
            </span>
          </div>

          <div className="flex items-center justify-between pt-1 border-t border-white/5">
            <button
              onClick={() => setSwRunning(!swRunning)}
              className="px-3 py-1 rounded-md text-xs font-mono font-bold flex items-center gap-1.5 transition-colors"
              style={{
                backgroundColor: swRunning ? '#333333' : accent,
                color: swRunning ? '#FFFFFF' : '#000000',
              }}
            >
              {swRunning ? <Pause size={12} /> : <Play size={12} />}
              {swRunning ? 'PAUSE' : 'START'}
            </button>
            <button
              onClick={() => {
                setSwRunning(false);
                setSwTime(0);
              }}
              className="p-1 rounded text-zinc-400 hover:text-white"
            >
              <RotateCcw size={14} />
            </button>
          </div>
        </div>
      );
    }

    case 'sensor.crypto.ticker':
      return (
        <div className={`${containerClasses} h-full min-h-[110px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span className="flex items-center gap-1.5">
              <TrendingUp size={14} style={{ color: accent }} />
              BTC / USD
            </span>
            <span className="text-emerald-400 text-xs font-bold font-mono">+4.2%</span>
          </div>

          <div className="my-auto py-1 flex items-center justify-between">
            <div className="text-xl font-mono font-bold text-white">$94,820</div>
            {/* 16 dot trend sparkline */}
            <div className="flex items-end gap-1 h-6">
              {[4, 5, 3, 6, 8, 7, 9, 8, 10, 12, 11, 14, 13, 16, 18, 20].map((val, i) => (
                <div
                  key={i}
                  className="w-1 rounded-xs"
                  style={{
                    height: `${(val / 20) * 100}%`,
                    backgroundColor: i >= 12 ? accent : '#555555',
                  }}
                />
              ))}
            </div>
          </div>

          <div className="flex justify-between text-[10px] font-mono text-zinc-500">
            <span>24H HIGH: $95,140</span>
            <span>VOL: 28.4K BTC</span>
          </div>
        </div>
      );

    // ----------------------------------------------------
    // QUICK TOGGLES
    // ----------------------------------------------------
    case 'toggle.grid.2x2':
      return (
        <div className={`${containerClasses} h-full min-h-[120px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400 mb-2">
            <span>QUICK ACTIONS</span>
            <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: accent }} />
          </div>
          <div className="grid grid-cols-2 gap-2 my-auto">
            <button
              onClick={() => setWifiOn(!wifiOn)}
              className="flex items-center gap-2 p-2 rounded-xl transition-all border border-white/5"
              style={{
                backgroundColor: wifiOn ? accent : 'rgba(255,255,255,0.05)',
                color: wifiOn ? '#000000' : '#888888',
              }}
            >
              <Wifi size={14} />
              <span className="text-[11px] font-mono font-bold">WI-FI</span>
            </button>
            <button
              onClick={() => setBtOn(!btOn)}
              className="flex items-center gap-2 p-2 rounded-xl transition-all border border-white/5"
              style={{
                backgroundColor: btOn ? accent : 'rgba(255,255,255,0.05)',
                color: btOn ? '#000000' : '#888888',
              }}
            >
              <Bluetooth size={14} />
              <span className="text-[11px] font-mono font-bold">BT</span>
            </button>
            <button
              onClick={() => setTorchOn(!torchOn)}
              className="flex items-center gap-2 p-2 rounded-xl transition-all border border-white/5"
              style={{
                backgroundColor: torchOn ? accent : 'rgba(255,255,255,0.05)',
                color: torchOn ? '#000000' : '#888888',
              }}
            >
              <Flashlight size={14} />
              <span className="text-[11px] font-mono font-bold">TORCH</span>
            </button>
            <button
              onClick={() => setDndOn(!dndOn)}
              className="flex items-center gap-2 p-2 rounded-xl transition-all border border-white/5"
              style={{
                backgroundColor: dndOn ? accent : 'rgba(255,255,255,0.05)',
                color: dndOn ? '#000000' : '#888888',
              }}
            >
              <BellOff size={14} />
              <span className="text-[11px] font-mono font-bold">DND</span>
            </button>
          </div>
        </div>
      );

    case 'toggle.torch':
      return (
        <div
          onClick={() => setTorchOn(!torchOn)}
          className={`${containerClasses} h-full aspect-square flex flex-col justify-between p-3 cursor-pointer`}
          style={{
            backgroundColor: torchOn ? 'rgba(255,255,255,0.15)' : 'rgba(17,17,17,0.9)',
          }}
        >
          <div className="flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>TORCH</span>
            <div
              className="w-2 h-2 rounded-full"
              style={{ backgroundColor: torchOn ? accent : '#333333' }}
            />
          </div>
          <div className="my-auto flex flex-col items-center">
            <div
              className="p-3 rounded-full transition-all"
              style={{
                backgroundColor: torchOn ? accent : 'rgba(255,255,255,0.05)',
                color: torchOn ? '#000000' : '#FFFFFF',
                boxShadow: torchOn ? `0 0 16px ${accent}` : 'none',
              }}
            >
              <Flashlight size={22} />
            </div>
          </div>
          <span className="text-[9px] font-mono text-center" style={{ color: torchOn ? accent : '#777777' }}>
            {torchOn ? 'ACTIVE' : 'OFF'}
          </span>
        </div>
      );

    // ----------------------------------------------------
    // CALENDAR WIDGETS
    // ----------------------------------------------------
    case 'calendar.day.date':
      return (
        <div className={`${containerClasses} h-full aspect-square flex flex-col justify-between p-3`}>
          <div className="flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>{daysNames[now.getDay()]}</span>
            <span style={{ color: accent }}>{monthNames[now.getMonth()]}</span>
          </div>
          <div className="my-auto flex flex-col items-center">
            <DotString text={dayStr} dotSize={3.5} dotGap={2} activeColor="#FFFFFF" />
          </div>
          <span className="text-[9px] font-mono text-center text-zinc-500">WEEK 42</span>
        </div>
      );

    case 'calendar.month.grid':
      return (
        <div className={`${containerClasses} h-full min-h-[120px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400 mb-1">
            <span>{monthNames[now.getMonth()]} {now.getFullYear()}</span>
            <span style={{ color: accent }}>TODAY: {dayStr}</span>
          </div>
          <div className="grid grid-cols-7 gap-1 text-center my-auto">
            {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => (
              <span key={i} className="text-[8px] font-mono text-zinc-500">{d}</span>
            ))}
            {Array.from({ length: 31 }).map((_, i) => {
              const dayNum = i + 1;
              const isToday = dayNum === now.getDate();
              return (
                <div
                  key={i}
                  className="h-4 flex items-center justify-center rounded-xs text-[9px] font-mono font-medium"
                  style={{
                    backgroundColor: isToday ? accent : 'transparent',
                    color: isToday ? '#000000' : '#CCCCCC',
                  }}
                >
                  {dayNum}
                </div>
              );
            })}
          </div>
        </div>
      );

    // ----------------------------------------------------
    // TIMER WIDGETS
    // ----------------------------------------------------
    case 'prod.focus.timer':
    case 'prod.countdown.timer': {
      const mins = Math.floor(timerRemaining / 60);
      const secs = timerRemaining % 60;
      const mStr = String(mins).padStart(2, '0');
      const sStr = String(secs).padStart(2, '0');

      return (
        <div className={`${containerClasses} h-full min-h-[120px]`}>
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span>POMODORO TIMER</span>
            <span style={{ color: timerRunning ? accent : '#888888' }}>
              {timerRunning ? 'FOCUSING' : 'PAUSED'}
            </span>
          </div>

          <div className="my-auto py-1 flex items-center justify-center gap-2">
            <span className="text-2xl font-mono font-bold text-white tracking-wider">
              {mStr}:{sStr}
            </span>
          </div>

          <div className="flex items-center justify-between pt-1 border-t border-white/5">
            <button
              onClick={() => setTimerRunning(!timerRunning)}
              className="px-3 py-1 rounded-md text-xs font-mono font-bold flex items-center gap-1.5 transition-colors"
              style={{
                backgroundColor: timerRunning ? '#333333' : accent,
                color: timerRunning ? '#FFFFFF' : '#000000',
              }}
            >
              {timerRunning ? <Pause size={12} /> : <Play size={12} />}
              {timerRunning ? 'PAUSE' : 'START'}
            </button>
            <button
              onClick={() => {
                setTimerRunning(false);
                setTimerRemaining(25 * 60);
              }}
              className="p-1 rounded text-zinc-400 hover:text-white"
            >
              <RotateCcw size={14} />
            </button>
          </div>
        </div>
      );
    }

    // ----------------------------------------------------
    // FUN & GAMES WIDGETS
    // ----------------------------------------------------
    case 'fun.snake.dot':
      return (
        <div 
          onClick={stepSnake}
          className={`${containerClasses} h-full min-h-[120px] cursor-pointer`}
          title="Click to step snake"
        >
          <div className="flex justify-between items-center text-xs font-mono text-zinc-400">
            <span style={{ color: accent }}>SNAKE • SCORE: {snakeScore}</span>
            <span className="text-[10px] text-zinc-500">TAP TO STEP</span>
          </div>

          <div className="my-auto py-1 flex items-center justify-center">
            <div className="grid grid-cols-15 gap-1 p-1 bg-black/40 rounded-lg border border-white/5">
              {Array.from({ length: 8 }).map((_, r) =>
                Array.from({ length: 15 }).map((_, c) => {
                  const isFood = c === snakeFood[0] && r === snakeFood[1];
                  const isSnake = snakeBody.some(p => p[0] === c && p[1] === r);
                  return (
                    <div
                      key={`${r}-${c}`}
                      className="w-1.5 h-1.5 rounded-full"
                      style={{
                        backgroundColor: isFood ? accent : isSnake ? '#FFFFFF' : '#1a1a1a',
                        boxShadow: isFood ? `0 0 6px ${accent}` : isSnake ? '0 0 2px #fff' : 'none',
                      }}
                    />
                  );
                })
              )}
            </div>
          </div>
        </div>
      );

    case 'fun.dice.roll':
      return (
        <div
          onClick={rollDice}
          className={`${containerClasses} h-full aspect-square flex flex-col justify-between p-3 cursor-pointer`}
          title="Click to roll dice"
        >
          <div className="flex justify-between items-center text-[10px] font-mono text-zinc-400">
            <span>DICE ROLL</span>
            <span style={{ color: accent }}>{dice[0] + dice[1]}</span>
          </div>

          <div className="my-auto flex items-center justify-center gap-3">
            {[dice[0], dice[1]].map((val, dIdx) => (
              <div
                key={dIdx}
                className="w-10 h-10 rounded-xl bg-zinc-900 border border-white/20 flex flex-col justify-between p-1.5 shadow-md transform hover:rotate-6 transition-transform"
              >
                <div className="flex justify-between">
                  <div className={`w-1.5 h-1.5 rounded-full ${[2, 3, 4, 5, 6].includes(val) ? 'bg-white' : 'opacity-0'}`} />
                  <div className={`w-1.5 h-1.5 rounded-full ${[4, 5, 6].includes(val) ? 'bg-white' : 'opacity-0'}`} />
                </div>
                <div className="flex justify-center">
                  <div className={`w-1.5 h-1.5 rounded-full ${[1, 3, 5].includes(val) ? '' : 'opacity-0'}`} style={{ backgroundColor: accent }} />
                </div>
                <div className="flex justify-between">
                  <div className={`w-1.5 h-1.5 rounded-full ${[4, 5, 6].includes(val) ? 'bg-white' : 'opacity-0'}`} />
                  <div className={`w-1.5 h-1.5 rounded-full ${[2, 3, 4, 5, 6].includes(val) ? 'bg-white' : 'opacity-0'}`} />
                </div>
              </div>
            ))}
          </div>

          <span className="text-[9px] font-mono text-center text-zinc-500">TAP TO ROLL</span>
        </div>
      );

    // Fallback default
    default:
      return (
        <div className={`${containerClasses} h-full min-h-[100px] flex flex-col justify-center items-center`}>
          <span className="text-xs font-mono text-zinc-400">{id}</span>
          <span className="text-[10px] font-mono text-zinc-600 mt-1">DOT MATRIX WIDGET</span>
        </div>
      );
  }
};
