import React from 'react';
import { IconWrapper } from './IconWrapper';
import { IconProps } from './types';

/**
 * Sample Icon - Replace with your Figma icon
 *
 * To create a new icon from Figma:
 * 1. Export the SVG from Figma
 * 2. Copy this file and rename it
 * 3. Replace the path elements inside IconWrapper with your SVG paths
 * 4. Remove stroke, fill, stroke-width attributes from paths (IconWrapper handles these)
 */
export const SampleIcon: React.FC<IconProps> = (props) => {
  return (
    <IconWrapper {...props} viewBox="0 0 24 24">
      {/* Replace these paths with your Figma SVG paths */}
      <circle cx="12" cy="12" r="10" />
      <path d="M12 8v4m0 4h.01" />
    </IconWrapper>
  );
};
