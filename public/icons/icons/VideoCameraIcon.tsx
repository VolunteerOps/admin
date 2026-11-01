import React from 'react';
import { IconWrapper } from './IconWrapper';
import { IconProps } from './types';

/**
 * Video Camera Icon
 *
 * Example icon - Replace path with your Figma design
 */
export const VideoCameraIcon: React.FC<IconProps> = (props) => {
  return (
    <IconWrapper {...props} viewBox="0 0 24 24">
      <path d="M23 7l-7 5 7 5V7z" />
      <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
    </IconWrapper>
  );
};
