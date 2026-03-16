import React, { useState } from 'react';
import { GoogleGenAI } from '@google/genai';
import { Image as ImageIcon, Loader2, Download } from 'lucide-react';

export const ImageGen: React.FC = () => {
  const [prompt, setPrompt] = useState('');
  const [size, setSize] = useState<'1K' | '2K' | '4K'>('1K');
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedImage, setGeneratedImage] = useState<string | null>(null);

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim() || isGenerating) return;

    setIsGenerating(true);
    setGeneratedImage(null);

    try {
      // NOTE: gemini-3-pro-image-preview requires user's own API key.
      // In this environment, we assume window.aistudio handles key selection if needed,
      // but for simplicity we use the injected GEMINI_API_KEY.
      const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
      
      const response = await ai.models.generateContent({
        model: 'gemini-3-pro-image-preview',
        contents: {
          parts: [{ text: prompt }],
        },
        config: {
          imageConfig: {
            aspectRatio: '1:1',
            imageSize: size
          }
        }
      });

      for (const part of response.candidates?.[0]?.content?.parts || []) {
        if (part.inlineData) {
          const base64EncodeString = part.inlineData.data;
          setGeneratedImage(`data:image/png;base64,${base64EncodeString}`);
          break;
        }
      }
    } catch (error) {
      console.error('Image generation error:', error);
      alert('Failed to generate image. Please try again.');
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center space-x-3 mb-6">
        <div className="p-3 bg-indigo-100 text-indigo-600 rounded-xl">
          <ImageIcon size={24} />
        </div>
        <div>
          <h2 className="text-2xl font-bold text-gray-900">AI Image Generator</h2>
          <p className="text-gray-600">Create stunning images using Nano Banana Pro</p>
        </div>
      </div>

      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
        <form onSubmit={handleGenerate} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Describe your image</label>
            <textarea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="A futuristic city with flying cars at sunset..."
              rows={4}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 resize-none"
            />
          </div>

          <div className="flex items-center space-x-4">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Image Size</label>
              <select
                value={size}
                onChange={(e) => setSize(e.target.value as any)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              >
                <option value="1K">1K (Standard)</option>
                <option value="2K">2K (High Quality)</option>
                <option value="4K">4K (Ultra HD)</option>
              </select>
            </div>
            
            <div className="flex-1 flex items-end">
              <button
                type="submit"
                disabled={!prompt.trim() || isGenerating}
                className="w-full flex justify-center items-center px-6 py-2.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
              >
                {isGenerating ? (
                  <>
                    <Loader2 className="animate-spin mr-2" size={20} />
                    Generating...
                  </>
                ) : (
                  'Generate Image'
                )}
              </button>
            </div>
          </div>
        </form>
      </div>

      {generatedImage && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-bold text-gray-900">Result</h3>
            <a
              href={generatedImage}
              download="generated-image.png"
              className="flex items-center px-4 py-2 text-sm font-medium text-indigo-600 bg-indigo-50 rounded-lg hover:bg-indigo-100"
            >
              <Download size={16} className="mr-2" />
              Download
            </a>
          </div>
          <div className="rounded-lg overflow-hidden border border-gray-200 bg-gray-50 flex justify-center">
            <img src={generatedImage} alt="Generated AI" className="max-w-full h-auto object-contain max-h-[600px]" />
          </div>
        </div>
      )}
    </div>
  );
};
