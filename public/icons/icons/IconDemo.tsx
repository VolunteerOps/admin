'use client';

import React from 'react';
import { CameraIcon, VideoIcon, MicrophoneIcon } from './index';

/**
 * Icon Demo Component
 *
 * Shows all available custom icons with different sizes and colors
 * Use this to preview your icons during development
 *
 * To view: Import and render this component in any page
 */
export const IconDemo = () => {
  return (
    <div className="p-8 space-y-8">
      <h1 className="text-3xl font-bold mb-6">Custom Icons Demo</h1>

      {/* Size variations */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Size Variations</h2>
        <div className="flex items-center gap-4">
          <CameraIcon size={16} />
          <CameraIcon size={24} />
          <CameraIcon size={32} />
          <CameraIcon size={48} />
          <CameraIcon size={64} />
        </div>
      </section>

      {/* Color variations */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Color Variations</h2>
        <div className="flex items-center gap-4">
          <CameraIcon className="text-blue-500" size={32} />
          <CameraIcon className="text-green-500" size={32} />
          <CameraIcon className="text-red-500" size={32} />
          <CameraIcon className="text-purple-500" size={32} />
          <CameraIcon className="text-gray-500" size={32} />
        </div>
      </section>

      {/* Stroke width variations */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Stroke Width Variations</h2>
        <div className="flex items-center gap-4">
          <CameraIcon strokeWidth={1} size={32} />
          <CameraIcon strokeWidth={1.5} size={32} />
          <CameraIcon strokeWidth={2} size={32} />
          <CameraIcon strokeWidth={2.5} size={32} />
          <CameraIcon strokeWidth={3} size={32} />
        </div>
      </section>

      {/* Example icons */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Example Icons</h2>
        <div className="flex items-center gap-6">
          <div className="flex flex-col items-center gap-2">
            <VideoIcon size={32} className="text-blue-600" />
            <span className="text-sm">Video</span>
          </div>
          <div className="flex flex-col items-center gap-2">
            <MicrophoneIcon size={32} className="text-green-600" />
            <span className="text-sm">Microphone</span>
          </div>
          <div className="flex flex-col items-center gap-2">
            <CameraIcon size={32} className="text-purple-600" />
            <span className="text-sm">Camera</span>
          </div>
        </div>
      </section>

      {/* Usage in buttons */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Usage in Buttons</h2>
        <div className="flex items-center gap-4">
          <button className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">
            <VideoIcon size={20} />
            Start Video
          </button>
          <button className="flex items-center gap-2 px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600">
            <MicrophoneIcon size={20} />
            Unmute
          </button>
          <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">
            <CameraIcon size={20} className="text-gray-600" />
            Custom Action
          </button>
        </div>
      </section>

      {/* Code examples */}
      <section className="space-y-4">
        <h2 className="text-2xl font-semibold">Usage Examples</h2>
        <div className="bg-gray-100 p-4 rounded-lg">
          <pre className="text-sm overflow-x-auto">
            <code>{`// Import icons
import { VideoIcon, MicrophoneIcon } from '@/components/icons';

// Basic usage
<VideoIcon />

// With size
<VideoIcon size={32} />

// With color (Tailwind)
<VideoIcon className="text-blue-500" />

// With custom stroke width
<VideoIcon strokeWidth={1.5} />

// Combined
<VideoIcon
  size={24}
  className="text-red-600"
  strokeWidth={2.5}
/>`}</code>
          </pre>
        </div>
      </section>
    </div>
  );
};
