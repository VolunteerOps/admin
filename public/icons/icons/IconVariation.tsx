import React from 'react';

export type IconVariant =
  | 'default'
  | 'linear'
  | 'bold'
  | 'broken'
  | 'bulk'
  | 'twotone'
  | 'outline';

interface IconVariationProps {
  children: React.ReactNode;
  variant?: IconVariant;
  className?: string;
}

/**
 * IconVariation Wrapper
 *
 * Displays a specific variation from Figma icon sheets (6 variations per icon)
 *
 * The Figma exports have 6 icon variations stacked vertically.
 * Sheet geometry: 56x296 with 4px top/bottom padding and 48px rows.
 *
 * Usage:
 *   <IconVariation variant="linear">
 *     <MicrophoneIcon />
 *   </IconVariation>
 *
 *   <IconVariation variant="bold">
 *     <MicrophoneIcon />
 *   </IconVariation>
 */
export const IconVariation: React.FC<IconVariationProps> = ({
  children,
  variant = 'default',
  className = '',
}) => {
  const normalizedVariant = variant === 'default' ? 'linear' : variant;

  const variantOffsets: Record<IconVariant, number> = {
    default: 4,
    linear: 4,
    bold: 52,
    outline: 100,
    twotone: 148,
    bulk: 196,
    broken: 244,
  };

  const offset = variantOffsets[normalizedVariant];
  const singleIconHeight = 48;

  return (
    <div
      className={`inline-flex ${className}`}
      style={{
        position: 'relative',
        width: '56px',
        height: `${singleIconHeight}px`,
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          position: 'absolute',
          top: `-${offset}px`,
          left: 0,
        }}
      >
        {children}
      </div>
    </div>
  );
};
