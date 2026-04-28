"use client"

import React, { useEffect, useRef, useState } from 'react'
import Link from "next/link"
import { Maximize2, ExternalLink, Flag, Loader2, AlertCircle, RefreshCw, Move3d } from 'lucide-react'
import { Button } from './button'

interface VirtualTourEmbedProps {
  tourUrl?: string         // Cloudinary URL of the 360° equirectangular image
  title?: string
  className?: string
  onFlag?: () => void
  listingId?: string
  height?: string
  // legacy props kept for backward compat but not used
  embedCode?: string
  tourProvider?: string
}

declare global {
  interface Window {
    pannellum: any
  }
}

export function VirtualTourEmbed({
  tourUrl,
  title = "360° Virtual Tour",
  className = "",
  onFlag,
  listingId,
  height = "h-96",
}: VirtualTourEmbedProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const viewerRef = useRef<any>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [hasError, setHasError] = useState(false)
  const [scriptLoaded, setScriptLoaded] = useState(false)
  const [showFallback, setShowFallback] = useState(false)

  // Load Pannellum CSS + JS from CDN once
  useEffect(() => {
    if (document.getElementById('pannellum-css')) {
      setScriptLoaded(true)
      return
    }

    const link = document.createElement('link')
    link.id = 'pannellum-css'
    link.rel = 'stylesheet'
    link.href = 'https://cdn.jsdelivr.net/npm/pannellum@2.5.6/build/pannellum.css'
    document.head.appendChild(link)

    const script = document.createElement('script')
    script.src = 'https://cdn.jsdelivr.net/npm/pannellum@2.5.6/build/pannellum.js'
    script.async = true
    script.onload = () => setScriptLoaded(true)
    script.onerror = () => setHasError(true)
    document.body.appendChild(script)

    return () => {
      // Don't remove scripts on unmount — other instances may use them
    }
  }, [])

  // Fallback timeout - show image if Pannellum doesn't load in 5 seconds
  useEffect(() => {
    const timeout = setTimeout(() => {
      if (isLoading) {
        console.warn('⏱️ Pannellum loading timeout - showing fallback image')
        setShowFallback(true)
        setIsLoading(false)
      }
    }, 5000)
    return () => clearTimeout(timeout)
  }, [isLoading])

  // Init Pannellum viewer once script is loaded and URL is available
  useEffect(() => {
    if (!scriptLoaded || !tourUrl || !containerRef.current) return

    // Check if window.pannellum is actually available
    if (!window.pannellum) {
      console.warn('⏳ Pannellum script loaded but window.pannellum not yet available, retrying...')
      const checkInterval = setInterval(() => {
        if (window.pannellum) {
          clearInterval(checkInterval)
          setScriptLoaded(true) // Trigger re-render
        }
      }, 100)
      return () => clearInterval(checkInterval)
    }

    console.log('🔮 Initializing Pannellum 360° viewer with URL:', tourUrl)

    try {
      // Destroy previous viewer instance if any
      if (viewerRef.current) {
        viewerRef.current.destroy()
        viewerRef.current = null
      }

      viewerRef.current = window.pannellum.viewer(containerRef.current, {
        type: 'equirectangular',
        panorama: tourUrl,
        autoLoad: true,
        autoRotate: -2,          // Slow auto-rotation
        compass: true,           // Show compass for orientation
        showControls: true,
        showFullscreenCtrl: true,
        showZoomCtrl: true,
        mouseZoom: true,
        draggable: true,         // Enable drag to look around
        keyboardZoom: true,      // Enable keyboard zoom
        disableKeyboardCtrl: false,
        hfov: 120,               // Wider initial field of view for immersive feel
        minHfov: 50,             // Minimum zoom out
        maxHfov: 120,            // Maximum zoom in
        pitch: 0,                // Start looking straight ahead
        yaw: 0,                  // Start facing forward
        strings: {
          loadButtonLabel: 'Click to Load 360° Tour',
          loadingLabel: 'Loading 360° Tour...',
          bylineLabel: '',
          noPanoramaError: 'No 360° image provided.',
          fileAccessError: 'The 360° image could not be loaded.',
          imageCorsError: 'The image cannot be displayed due to CORS restrictions.',
          imageSizeError: 'The image is too large.',
          swfNotFoundError: '',
        },
        onLoad: () => {
          console.log('✅ Pannellum 360° tour loaded successfully')
          setIsLoading(false)
        },
        onError: (err: any) => {
          console.error('❌ Pannellum load error:', err)
          setIsLoading(false)
          setHasError(true)
        },
      })
    } catch (e) {
      console.error('❌ Pannellum init error:', e)
      setHasError(true)
      setIsLoading(false)
    }

    return () => {
      if (viewerRef.current) {
        viewerRef.current.destroy()
        viewerRef.current = null
      }
    }
  }, [scriptLoaded, tourUrl])

  const handleRetry = () => {
    setHasError(false)
    setIsLoading(true)
    setScriptLoaded(false)   // triggers script re-check
    setTimeout(() => setScriptLoaded(true), 100)
  }

  if (!tourUrl) {
    return (
      <div className={`bg-slate-100 rounded-xl flex items-center justify-center ${height} ${className}`}>
        <div className="text-center">
          <Move3d className="h-12 w-12 text-slate-400 mx-auto mb-3" />
          <p className="text-slate-600 font-medium">360° Tour Not Available</p>
          <p className="text-sm text-slate-500 mt-1">No panoramic image provided</p>
        </div>
      </div>
    )
  }

  return (
    <div className={`relative rounded-xl overflow-hidden bg-black ${className}`}>
      {/* Loading overlay */}
      {isLoading && !hasError && (
        <div className={`absolute inset-0 bg-black/60 flex items-center justify-center z-20 ${height}`}>
          <div className="text-center">
            <Loader2 className="h-8 w-8 text-white animate-spin mx-auto mb-3" />
            <p className="text-white text-sm">Loading 360° Tour...</p>
          </div>
        </div>
      )}

      {/* Error overlay */}
      {hasError && (
        <div className={`absolute inset-0 bg-black/70 flex items-center justify-center z-30 ${height}`}>
          <div className="text-center bg-white rounded-xl p-6 max-w-xs mx-4">
            <AlertCircle className="h-10 w-10 text-red-500 mx-auto mb-3" />
            <p className="text-slate-800 font-medium mb-1">Failed to load 360° Tour</p>
            <p className="text-slate-500 text-xs mb-4">
              The panoramic image could not be loaded. Make sure the image URL is accessible.
            </p>
            <div className="space-y-2">
              <Button size="sm" onClick={handleRetry} className="w-full">
                <RefreshCw className="h-4 w-4 mr-2" /> Retry
              </Button>
              <Button size="sm" variant="outline" onClick={() => window.open(tourUrl, '_blank')} className="w-full">
                <ExternalLink className="h-4 w-4 mr-2" /> Open Image
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Pannellum mounts here */}
      <div ref={containerRef} className={`w-full ${height}`} />

      {/* Controls overlay */}
      {!isLoading && !hasError && (
        <div className="absolute bottom-4 left-4 right-4 flex items-center justify-between z-10 pointer-events-none">
          <div className="flex items-center gap-2 pointer-events-auto">
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
            <Button
              size="sm"
              variant="ghost"
              className="text-white bg-black/50 backdrop-blur-sm hover:bg-black/70"
              onClick={() => window.open(tourUrl, '_blank')}
              title="Open full image"
            >
              <ExternalLink className="h-4 w-4" />
            </Button>
            {listingId && (
              <>
                <Button
                  asChild
                  size="sm"
                  variant="ghost"
                  className="text-white bg-blue-600/80 backdrop-blur-sm hover:bg-blue-600"
                  title="Enter AR rehearsal mode"
                >
                  <Link href={`/listings/${listingId}/ar`}>
                    <Maximize2 className="h-4 w-4 mr-1" />
                    AR
                  </Link>
                </Button>
                <Button
                  asChild
                  size="sm"
                  variant="ghost"
                  className="text-white bg-violet-600/80 backdrop-blur-sm hover:bg-violet-600"
                  title="Enter immersive VR mode"
                >
                  <Link href={`/listings/${listingId}/vr`}>
                    <Maximize2 className="h-4 w-4 mr-1" />
                    VR
                  </Link>
                </Button>
              </>
            )}
          </div>
          <div className="bg-black/50 backdrop-blur-sm text-white text-xs px-3 py-1.5 rounded-full">
            🖱️ Drag to look around · Scroll to zoom
          </div>
        </div>
      )}

      {/* Badge */}
      {!isLoading && !hasError && (
        <div className="absolute top-4 left-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white px-3 py-1.5 rounded-full text-xs font-medium flex items-center gap-2 z-10">
          <div className="w-2 h-2 bg-white rounded-full animate-pulse" />
          360° Interactive Tour
        </div>
      )}
    </div>
  )
}
