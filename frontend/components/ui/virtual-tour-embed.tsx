"use client"

import React, { useState, useRef } from 'react'
import { Play, Pause, Maximize2, ExternalLink, Eye, Navigation, Move3d, Flag, Loader2, RefreshCw, AlertCircle } from 'lucide-react'
import { Button } from './button'

interface VirtualTourEmbedProps {
  tourUrl?: string
  embedCode?: string
  title?: string
  className?: string
  onFlag?: () => void
  listingId?: string
  tourProvider?: 'panoee' | 'kuula' | 'zillow' | 'cloudpano' | 'generic'
  height?: string
}

const TOUR_PROVIDERS = {
  panoee: {
    name: 'Panoee',
    icon: '🌐',
    color: 'from-blue-600 to-purple-600',
    features: ['360° Views', 'Interactive Hotspots', 'Mobile Friendly']
  },
  kuula: {
    name: 'Kuula',
    icon: '🎯',
    color: 'from-green-600 to-blue-600',
    features: ['360° Photos', 'Hotspot Navigation', 'VR Support']
  },
  zillow: {
    name: 'Zillow 3D Home',
    icon: '🏠',
    color: 'from-blue-500 to-cyan-600',
    features: ['Phone Capture', 'Quick Setup', 'Free']
  },
  cloudpano: {
    name: 'CloudPano',
    icon: '☁️',
    color: 'from-indigo-600 to-purple-600',
    features: ['Custom Branding', 'Analytics', 'Fast Loading']
  },
  generic: {
    name: 'Virtual Tour',
    icon: '🎮',
    color: 'from-slate-600 to-slate-800',
    features: ['Interactive Tour', '360° Experience', 'Web Ready']
  }
}

export function VirtualTourEmbed({
  tourUrl,
  embedCode,
  title = "Virtual Tour",
  className = "",
  onFlag,
  listingId,
  tourProvider = 'generic',
  height = "h-96"
}: VirtualTourEmbedProps) {
  const [isLoading, setIsLoading] = useState(true)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [hasError, setHasError] = useState(false)
  const [showControls, setShowControls] = useState(true)
  const iframeRef = useRef<HTMLIFrameElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const provider = TOUR_PROVIDERS[tourProvider]

  const getEmbedUrl = () => {
    // Try embed code first
    if (embedCode) {
      const srcMatch = embedCode.match(/src=["']([^"']+)["']/)
      if (srcMatch) {
        console.log('Extracted embed URL:', srcMatch[1])
        return srcMatch[1]
      }
      // Fallback: extract any URL from embed code
      const urlMatch = embedCode.match(/https?:\/\/[^\s"']+/)
      if (urlMatch) {
        console.log('Fallback URL from embed:', urlMatch[0])
        return urlMatch[0]
      }
    }
    
    // Fallback to tour URL
    if (tourUrl) {
      console.log('Using tour URL:', tourUrl, 'Provider:', tourProvider)
      
      // Simple provider-specific handling
      if (tourProvider === 'kuula' && !tourUrl.includes('/embed/')) {
        return `${tourUrl}/embed/transparent/800x600`
      }
      
      // For all other providers, return URL as-is
      return tourUrl
    }
    
    console.log('No URL or embed code found')
    return null
  }

  const handleLoad = () => {
    setIsLoading(false)
    setHasError(false)
  }

  const handleError = () => {
    setIsLoading(false)
    setHasError(true)
  }

  const toggleFullscreen = () => {
    const container = containerRef.current
    if (!container) return

    if (!isFullscreen) {
      container.requestFullscreen?.()
      setIsFullscreen(true)
    } else {
      document.exitFullscreen?.()
      setIsFullscreen(false)
    }
  }

  const refreshTour = () => {
    setIsLoading(true)
    setHasError(false)
    if (iframeRef.current) {
      iframeRef.current.src = iframeRef.current.src
    }
  }

  const openInNewTab = () => {
    const url = getEmbedUrl()
    if (url) {
      window.open(url, '_blank')
    }
  }

  const embedUrl = getEmbedUrl()

  // Debug logging
  console.log('VirtualTourEmbed Props:', { tourUrl, embedCode, tourProvider })
  console.log('Final embedUrl:', embedUrl)

  if (!embedUrl) {
    return (
      <div className={`bg-slate-100 rounded-xl flex items-center justify-center ${height} ${className}`}>
        <div className="text-center">
          <Move3d className="h-12 w-12 text-slate-400 mx-auto mb-3" />
          <p className="text-slate-600 font-medium">Virtual Tour Not Available</p>
          <p className="text-sm text-slate-500 mt-1">Check back later for an interactive tour</p>
          <div className="mt-3 p-2 bg-slate-200 rounded text-xs text-left">
            <div>URL: {tourUrl || 'none'}</div>
            <div>Embed: {embedCode ? 'provided' : 'none'}</div>
            <div>Provider: {tourProvider}</div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div 
      ref={containerRef}
      className={`relative bg-black rounded-xl overflow-hidden ${className}`}
      onMouseEnter={() => setShowControls(true)}
      onMouseLeave={() => setShowControls(false)}
    >
      {/* Loading State */}
      {isLoading && (
        <div className="absolute inset-0 bg-black/50 flex items-center justify-center z-20">
          <div className="text-center">
            <Loader2 className="h-8 w-8 text-white animate-spin mx-auto mb-3" />
            <p className="text-white text-sm">Loading Virtual Tour...</p>
          </div>
        </div>
      )}

      {/* Error State */}
      {hasError && (
        <div className="absolute inset-0 bg-black/70 flex items-center justify-center z-30">
          <div className="text-center bg-white rounded-xl p-6 max-w-sm mx-4">
            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-3">
              <AlertCircle className="h-6 w-6 text-red-600" />
            </div>
            <p className="text-slate-800 text-sm font-medium mb-2">Tour Failed to Load</p>
            <p className="text-slate-600 text-xs mb-4">This tour may not be available in iframe</p>
            <div className="space-y-2">
              <Button size="sm" onClick={refreshTour} className="w-full">
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry Loading
              </Button>
              {tourUrl && (
                <Button size="sm" variant="outline" onClick={() => window.open(tourUrl, '_blank')} className="w-full">
                  <ExternalLink className="h-4 w-4 mr-2" />
                  Open in New Tab
                </Button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Virtual Tour iframe */}
      <iframe
        ref={iframeRef}
        src={embedUrl}
        title={`${title} - Virtual Tour`}
        className={`w-full ${height} border-0`}
        onLoad={handleLoad}
        onError={handleError}
        allowFullScreen
        allow="xr-spatial-tracking; gyroscope; accelerometer; magnetometer; vr; camera; microphone"
        loading="lazy"
      />

      {/* Tour Provider Badge */}
      <div className="absolute top-4 left-4 bg-gradient-to-r from-black/70 to-black/50 backdrop-blur-sm text-white px-3 py-2 rounded-full text-xs font-medium flex items-center gap-2 z-10">
        <span className="text-lg">{provider.icon}</span>
        <span>{provider.name}</span>
      </div>

      {/* Interactive Controls */}
      {showControls && !isLoading && !hasError && (
        <div className="absolute bottom-4 left-4 right-4 flex items-center justify-between z-10">
          {/* Left Controls */}
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="ghost"
              className="text-white bg-black/50 backdrop-blur-sm hover:bg-black/70"
              onClick={openInNewTab}
              title="Open in new tab"
            >
              <ExternalLink className="h-4 w-4" />
            </Button>

            <Button
              size="sm"
              variant="ghost"
              className="text-white bg-black/50 backdrop-blur-sm hover:bg-black/70"
              onClick={refreshTour}
              title="Refresh tour"
            >
              <RefreshCw className="h-4 w-4" />
            </Button>

            {onFlag && (
              <Button
                size="sm"
                variant="ghost"
                className="text-white bg-black/50 backdrop-blur-sm hover:bg-red-500/20"
                onClick={onFlag}
                title="Report inappropriate content"
              >
                <Flag className="h-4 w-4" />
              </Button>
            )}
          </div>

          {/* Right Controls */}
          <Button
            size="sm"
            variant="ghost"
            className="text-white bg-black/50 backdrop-blur-sm hover:bg-black/70"
            onClick={toggleFullscreen}
            title="Fullscreen"
          >
            <Maximize2 className="h-4 w-4" />
          </Button>
        </div>
      )}

      {/* Tour Features Badge */}
      <div className="absolute top-4 right-4 bg-gradient-to-r from-black/70 to-black/50 backdrop-blur-sm text-white px-3 py-2 rounded-lg text-xs z-10">
        <div className="flex items-center gap-1 mb-1">
          <Eye className="h-3 w-3" />
          <span className="font-medium">Interactive</span>
        </div>
        <div className="text-xs opacity-80">
          {provider.features[0]}
        </div>
      </div>

      {/* Instructions Overlay */}
      {!isLoading && !hasError && (
        <div className="absolute bottom-20 left-4 right-4 bg-black/60 backdrop-blur-sm rounded-lg p-3 opacity-0 hover:opacity-100 transition-opacity z-10">
          <p className="text-white text-xs">
            🖱️ Click and drag to look around • Use mouse wheel to zoom • Click hotspots for details
          </p>
        </div>
      )}
    </div>
  )
}
