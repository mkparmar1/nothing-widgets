import React from 'react';

// 5x7 Dot Matrix Font Map
const FONT_MAP: Record<string, number[][]> = {
  '0': [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 1, 1],
    [1, 0, 1, 0, 1],
    [1, 1, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
  ],
  '1': [
    [0, 0, 1, 0, 0],
    [0, 1, 1, 0, 0],
    [0, 0, 1, 0, 0],
    [0, 0, 1, 0, 0],
    [0, 0, 1, 0, 0],
    [0, 0, 1, 0, 0],
    [0, 1, 1, 1, 0],
  ],
  '2': [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [0, 0, 0, 0, 1],
    [0, 0, 1, 1, 0],
    [0, 1, 0, 0, 0],
    [1, 0, 0, 0, 0],
    [1, 1, 1, 1, 1],
  ],
  '3': [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [0, 0, 0, 0, 1],
    [0, 0, 1, 1, 0],
    [0, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
  ],
  '4': [
    [0, 0, 0, 1, 0],
    [0, 0, 1, 1, 0],
    [0, 1, 0, 1, 0],
    [1, 0, 0, 1, 0],
    [1, 1, 1, 1, 1],
    [0, 0, 0, 1, 0],
    [0, 0, 0, 1, 0],
  ],
  '5': [
    [1, 1, 1, 1, 1],
    [1, 0, 0, 0, 0],
    [1, 1, 1, 1, 0],
    [0, 0, 0, 0, 1],
    [0, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
  ],
  '6': [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 0],
    [1, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
  ],
  '7': [
    [1, 1, 1, 1, 1],
    [0, 0, 0, 0, 1],
    [0, 0, 0, 1, 0],
    [0, 0, 1, 0, 0],
    [0, 1, 0, 0, 0],
    [0, 1, 0, 0, 0],
    [0, 1, 0, 0, 0],
  ],
  '8': [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
  ],
  '9': [
    [0, 1, 1, 1, 0],
    [1, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 1],
    [0, 0, 0, 0, 1],
    [1, 0, 0, 0, 1],
    [0, 1, 1, 1, 0],
  ],
  ':': [
    [0, 0, 0],
    [0, 1, 0],
    [0, 1, 0],
    [0, 0, 0],
    [0, 1, 0],
    [0, 1, 0],
    [0, 0, 0],
  ],
  '.': [
    [0, 0, 0],
    [0, 0, 0],
    [0, 0, 0],
    [0, 0, 0],
    [0, 0, 0],
    [0, 1, 0],
    [0, 1, 0],
  ],
  '%': [
    [1, 1, 0, 0, 1],
    [1, 1, 0, 1, 0],
    [0, 0, 1, 0, 0],
    [0, 1, 0, 0, 0],
    [1, 0, 0, 1, 1],
    [0, 0, 0, 1, 1],
    [0, 0, 0, 0, 0],
  ],
  '°': [
    [0, 1, 1, 0],
    [1, 0, 0, 1],
    [1, 0, 0, 1],
    [0, 1, 1, 0],
    [0, 0, 0, 0],
    [0, 0, 0, 0],
    [0, 0, 0, 0],
  ],
  '-': [
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [1, 1, 1, 1, 1],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0],
  ],
  ' ': [
    [0, 0],
    [0, 0],
    [0, 0],
    [0, 0],
    [0, 0],
    [0, 0],
    [0, 0],
  ],
};

interface DotCharProps {
  char: string;
  dotSize?: number;
  dotGap?: number;
  activeColor?: string;
  inactiveColor?: string;
  showInactive?: boolean;
}

export const DotChar: React.FC<DotCharProps> = ({
  char,
  dotSize = 3,
  dotGap = 2,
  activeColor = '#FFFFFF',
  inactiveColor = '#1F1F1F',
  showInactive = true,
}) => {
  const matrix = FONT_MAP[char] || FONT_MAP[' '];
  const width = matrix[0].length;
  const height = matrix.length;

  return (
    <div
      className="inline-grid select-none"
      style={{
        gridTemplateColumns: `repeat(${width}, ${dotSize}px)`,
        gap: `${dotGap}px`,
      }}
    >
      {matrix.map((row, r) =>
        row.map((cell, c) => (
          <div
            key={`${r}-${c}`}
            className="rounded-full transition-colors duration-150"
            style={{
              width: `${dotSize}px`,
              height: `${dotSize}px`,
              backgroundColor: cell ? activeColor : showInactive ? inactiveColor : 'transparent',
              boxShadow: cell && activeColor !== '#FFFFFF' ? `0 0 6px ${activeColor}66` : 'none',
            }}
          />
        ))
      )}
    </div>
  );
};

interface DotStringProps {
  text: string;
  dotSize?: number;
  dotGap?: number;
  charGap?: number;
  activeColor?: string;
  inactiveColor?: string;
  showInactive?: boolean;
}

export const DotString: React.FC<DotStringProps> = ({
  text,
  dotSize = 3,
  dotGap = 2,
  charGap = 4,
  activeColor = '#FFFFFF',
  inactiveColor = '#1A1A1A',
  showInactive = true,
}) => {
  return (
    <div className="inline-flex items-center" style={{ gap: `${charGap}px` }}>
      {text.split('').map((char, index) => (
        <DotChar
          key={index}
          char={char}
          dotSize={dotSize}
          dotGap={dotGap}
          activeColor={activeColor}
          inactiveColor={inactiveColor}
          showInactive={showInactive}
        />
      ))}
    </div>
  );
};
