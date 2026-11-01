'use client';

import React from 'react';
import { MicrophoneIcon } from './index';
import { IconVariant } from './types';

/**
 * Icon Variation Demo
 *
 * Demonstrates how to use the 6 variations from Figma icon sheets
 */
export const VariationDemo = () => {
  const variants: { name: IconVariant; label: string; description: string }[] = [
    { name: 'linear', label: 'Linear', description: 'Row 1 (y 4-52)' },
    { name: 'bold', label: 'Bold', description: 'Row 2 (y 52-100)' },
    { name: 'outline', label: 'Outline', description: 'Row 3 (y 100-148)' },
    { name: 'twotone', label: 'TwoTone', description: 'Row 4 (y 148-196)' },
    { name: 'bulk', label: 'Bulk', description: 'Row 5 (y 196-244)' },
    { name: 'broken', label: 'Broken', description: 'Row 6 (y 244-292)' },
  ];

  return (
    <div className="p-8 space-y-8">
      <div>
        <h1 className="text-3xl font-bold mb-2">Icon Variations Demo</h1>
        <p className="text-gray-600">
          Your Figma exports contain 6 variations of each icon stacked vertically.
          Use the <code className="bg-gray-100 px-2 py-1 rounded">variant</code> prop to select which one to display.
        </p>
      </div>

      {/* All 6 variations */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">All Variations (Microphone Icon)</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
          {variants.map(({ name, label, description }) => (
            <div key={name} className="flex flex-col items-center gap-3 p-6 border rounded-lg bg-white">
              <MicrophoneIcon variant={name} size={48} className="text-gray-700" />
              <div className="text-center">
                <div className="font-semibold text-sm">{label}</div>
                <div className="text-xs text-gray-500 mt-1">{description}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Different sizes */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Size Variations (Bold)</h2>
        <div className="flex items-end gap-4">
          <MicrophoneIcon variant="bold" size={16} />
          <MicrophoneIcon variant="bold" size={24} />
          <MicrophoneIcon variant="bold" size={32} />
          <MicrophoneIcon variant="bold" size={48} />
          <MicrophoneIcon variant="bold" size={64} />
        </div>
      </section>

      {/* Different colors */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Color Variations (Linear)</h2>
        <div className="flex items-center gap-4">
          <MicrophoneIcon variant="linear" size={32} className="text-blue-500" />
          <MicrophoneIcon variant="linear" size={32} className="text-green-500" />
          <MicrophoneIcon variant="linear" size={32} className="text-red-500" />
          <MicrophoneIcon variant="linear" size={32} className="text-purple-500" />
          <MicrophoneIcon variant="linear" size={32} className="text-orange-500" />
        </div>
      </section>

      {/* Usage examples */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Usage Examples</h2>
        <div className="grid grid-cols-2 gap-4">
          {/* Example 1: Mute button */}
          <div className="border rounded-lg p-6 space-y-3">
            <h3 className="font-semibold">Mute Button (Bold variant)</h3>
            <button className="flex items-center gap-2 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600">
              <MicrophoneIcon variant="bold" size={20} />
              Mute
            </button>
            <pre className="text-xs bg-gray-100 p-2 rounded overflow-x-auto">
{`<MicrophoneIcon
  variant="bold"
  size={20}
/>`}
            </pre>
          </div>

          {/* Example 2: Settings icon */}
          <div className="border rounded-lg p-6 space-y-3">
            <h3 className="font-semibold">Settings Icon (Outline variant)</h3>
            <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">
              <MicrophoneIcon variant="outline" size={20} className="text-gray-600" />
              Audio Settings
            </button>
            <pre className="text-xs bg-gray-100 p-2 rounded overflow-x-auto">
{`<MicrophoneIcon
  variant="outline"
  size={20}
  className="text-gray-600"
/>`}
            </pre>
          </div>

          {/* Example 3: TwoTone variant */}
          <div className="border rounded-lg p-6 space-y-3">
            <h3 className="font-semibold">Two-Tone Style</h3>
            <div className="flex items-center gap-2">
              <MicrophoneIcon variant="twotone" size={32} className="text-indigo-600" />
              <span>Voice Input</span>
            </div>
            <pre className="text-xs bg-gray-100 p-2 rounded overflow-x-auto">
{`<MicrophoneIcon
  variant="twotone"
  size={32}
  className="text-indigo-600"
/>`}
            </pre>
          </div>

          {/* Example 4: Linear variant */}
          <div className="border rounded-lg p-6 space-y-3">
            <h3 className="font-semibold">Linear Style</h3>
            <div className="flex items-center gap-2">
              <MicrophoneIcon variant="linear" size={32} className="text-gray-800" />
              <span>Microphone</span>
            </div>
            <pre className="text-xs bg-gray-100 p-2 rounded overflow-x-auto">
{`<MicrophoneIcon
  variant="linear"
  size={32}
  className="text-gray-800"
/>`}
            </pre>
          </div>
        </div>
      </section>

      {/* Quick reference */}
      <section className="bg-blue-50 border border-blue-200 rounded-lg p-6 space-y-3">
        <h2 className="text-xl font-semibold text-blue-900">Quick Reference</h2>
        <div className="space-y-2 text-sm">
          <p><strong>Import:</strong> <code className="bg-blue-100 px-2 py-1 rounded">import {`{ MicrophoneIcon }`} from '@/components/icons';</code></p>
          <p><strong>Variants:</strong></p>
          <ul className="list-disc list-inside ml-4 space-y-1">
            <li><code className="bg-blue-100 px-2 py-1 rounded">variant="linear"</code> - Linear style</li>
            <li><code className="bg-blue-100 px-2 py-1 rounded">variant="bold"</code> - Filled/solid style</li>
            <li><code className="bg-blue-100 px-2 py-1 rounded">variant="broken"</code> - Broken style</li>
            <li><code className="bg-blue-100 px-2 py-1 rounded">variant="bulk"</code> - Bulk style</li>
            <li><code className="bg-blue-100 px-2 py-1 rounded">variant="outline"</code> - Thin outline</li>
            <li><code className="bg-blue-100 px-2 py-1 rounded">variant="twotone"</code> - With opacity/two-tone</li>
            <li><code className="bg-blue-100 px-2 py-1 rounded">variant="default"</code> - Alias of linear</li>
          </ul>
          <p className="mt-3"><strong>Props:</strong> size, className, color, strokeWidth, variant</p>
        </div>
      </section>
    </div>
  );
};
