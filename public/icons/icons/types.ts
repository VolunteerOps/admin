export type IconVariant =
  | 'default' // Backward-compatible alias of 'linear'
  | 'linear'
  | 'bold'
  | 'broken'
  | 'bulk'
  | 'twotone'
  | 'outline';

export interface IconProps {
  className?: string;
  size?: number;
  color?: string;
  strokeWidth?: number;
  variant?: IconVariant; // For Figma icon sheets with 6 variations
}

export type IconComponent = React.FC<IconProps>;
