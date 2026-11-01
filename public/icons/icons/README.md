# Custom Icons

This directory contains custom icons exported from Figma.

## How to Add a New Icon from Figma

### 1. Export from Figma
- Select the icon in Figma
- Right-click → Export → SVG (1x scale)
- Download the SVG file

### 2. Convert to React Component

Create a new file in this directory (e.g., `MyIcon.tsx`):

```tsx
import React from 'react';
import { IconWrapper } from './IconWrapper';
import { IconProps } from './types';

export const MyIcon: React.FC<IconProps> = (props) => {
  return (
    <IconWrapper {...props}>
      {/* Paste your SVG path/shape elements here */}
      <path d="M12 2L2 7l10 5 10-5-10-5z" />
      <path d="M2 17l10 5 10-5M2 12l10 5 10-5" />
    </IconWrapper>
  );
};
```

### 3. Export from index.ts

Add your icon to `index.ts`:

```tsx
export { MyIcon } from './MyIcon';
```

### 4. Use in Your App

```tsx
import { MyIcon } from '@/components/icons';

<MyIcon size={24} className="text-blue-500" />
```

## Icon Component Props

All icons accept these props:

- `className?: string` - Tailwind classes or custom CSS classes
- `size?: number` - Width and height in pixels (default: 24)
- `color?: string` - Stroke color (default: 'currentColor')
- `strokeWidth?: number` - Stroke width (default: 2)

## Example SVG Conversion

**Original Figma SVG:**
```xml
<svg width="24" height="24" viewBox="0 0 24 24" fill="none">
  <path d="M12 2L2 7l10 5 10-5-10-5z" stroke="#000" stroke-width="2"/>
  <path d="M2 17l10 5 10-5M2 12l10 5 10-5" stroke="#000" stroke-width="2"/>
</svg>
```

**Converted React Component:**
```tsx
import React from 'react';
import { IconWrapper } from './IconWrapper';
import { IconProps } from './types';

export const LayersIcon: React.FC<IconProps> = (props) => {
  return (
    <IconWrapper {...props}>
      <path d="M12 2L2 7l10 5 10-5-10-5z" />
      <path d="M2 17l10 5 10-5M2 12l10 5 10-5" />
    </IconWrapper>
  );
};
```

**Note:** Remove `stroke`, `stroke-width`, and `fill` attributes from path elements - IconWrapper handles these.

## Tips

1. **Clean up SVG code**: Remove unnecessary attributes like `stroke`, `fill`, `stroke-width` from paths
2. **Preserve path data**: Only keep the `d` attribute for `<path>` elements
3. **Check viewBox**: Ensure viewBox matches your original SVG (usually "0 0 24 24")
4. **Use semantic names**: Name icons based on what they represent, not how they look
5. **Test responsiveness**: Verify icons scale correctly with different size props
