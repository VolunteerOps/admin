import React from 'react';
import { IconProps } from './types';

/**
 * Wrapper component for custom SVG icons
 * Provides consistent sizing, coloring, and className support
 *
 * For Figma icon sheets with 6 stacked variations (viewBox="0 0 56 296"):
 * - linear:  row 1 (y 4-52)
 * - bold:    row 2 (y 52-100)
 * - outline: row 3 (y 100-148)
 * - twotone: row 4 (y 148-196)
 * - bulk:    row 5 (y 196-244)
 * - broken:  row 6 (y 244-292)
 *
 * `default` is kept as a backward-compatible alias of `linear`.
 */
export const IconWrapper: React.FC<IconProps & { children: React.ReactNode; viewBox?: string }> = ({
  children,
  className = '',
  size = 24,
  color = 'currentColor',
  strokeWidth = 2,
  viewBox = '0 0 24 24',
  variant = 'default',
}) => {
  // Check if this is a stacked Figma icon sheet.
  // Most files are *x296, but a small subset (e.g. ArrowDown1) is *x348.
  const sheetMatch = viewBox.match(/^0 0 ([0-9.]+) ([0-9.]+)$/);
  const parsedSheetWidth = sheetMatch ? Number(sheetMatch[1]) : NaN;
  const parsedSheetHeight = sheetMatch ? Number(sheetMatch[2]) : NaN;
  const isFigmaSheet = Number.isFinite(parsedSheetWidth) && (parsedSheetHeight === 296 || parsedSheetHeight === 297 || parsedSheetHeight === 348);

  if (isFigmaSheet) {
    const normalizedVariant = variant === 'default' ? 'linear' : variant;

    // Source sheets use six 48px rows.
    // - 296-high format starts at y=4
    // - 348-high format starts at y=12
    const firstRowOffset = parsedSheetHeight === 348 ? 12 : 4;
    const rowHeight = 48;
    const variantOffsets = {
      linear: firstRowOffset,
      bold: firstRowOffset + rowHeight,
      outline: firstRowOffset + (rowHeight * 2),
      twotone: firstRowOffset + (rowHeight * 3),
      bulk: firstRowOffset + (rowHeight * 4),
      broken: firstRowOffset + (rowHeight * 5),
    };

    const yOffset = variantOffsets[normalizedVariant];
    const adjustedViewBox = `${parsedSheetWidth / 2 - 12} ${yOffset + 12} 24 24`;

    return (
      <svg
        aria-hidden="true"
        width={size}
        height={size}
        viewBox={adjustedViewBox}
        fill="none"
        color={color}
        stroke="currentColor"
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
        className={className}
        xmlns="http://www.w3.org/2000/svg"
      >
        {children}
      </svg>
    );
  }

  // Regular icon (single design)
  return (
    <svg
      aria-hidden="true"
        width={size}
      height={size}
      viewBox={viewBox}
      fill="none"
      color={color}
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      xmlns="http://www.w3.org/2000/svg"
    >
      {children}
    </svg>
  );
};
