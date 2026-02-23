"use client"

import React, { useState, useRef, useEffect } from 'react'
import { Play, Pause, Volume2, VolumeX, Maximize2, RotateCcw, Move3d, Eye, Navigation, Flag, Home, ZoomIn, ZoomOut, RotateCw } from 'lucide-react'
import { Button } from './button'

interface VirtualTour3DProps {
  src: string
  thumbnail?: string
  title?: string
  className?: string
  autoPlay?: boolean
  onPlay?: () => void
  onPause?: () => void
  onFlag?: () => void
  listingId?: string
  is360Video?: boolean
  tourType?: 'video' | '3d' | '360'
}

export function VirtualTour3D({
  src,
  thumbnail,
  title = "Virtual Tour",
  className = "",
  autoPlay = false,
  onPlay,
  onPause,
  onFlag,
  listingId,
  is360Video = false,
  tourType = 'video'
}: VirtualTour3DProps) {
  const [isPlaying, setIsPlaying] = useState(false)
  const [isMuted, setIsMuted] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [showPlayOverlay, setShowPlayOverlay] = useState(true)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [viewMode, setViewMode] = useState<'normal' | '360' | 'vr'>('normal')
  const [zoom, setZoom] = useState(1)
  const [rotation, setRotation] = useState(0)
  const [isDragging, setIsDragging] = useState(false)
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 })
  const [viewAngle, setViewAngle] = useState({ x: 0, y: 0 })
  
  const videoRef = useRef<HTMLVideoElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const handleLoadedMetadata = () => {
      setDuration(video.duration)
      setIsLoading(false)
    }

    const handleTimeUpdate = () => {
      setCurrentTime(video.currentTime)
    }

    const handleEnded = () => {
      setIsPlaying(false)
      setShowPlayOverlay(true)
    }

    video.addEventListener('loadedmetadata', handleLoadedMetadata)
    video.addEventListener('timeupdate', handleTimeUpdate)
    video.addEventListener('ended', handleEnded)

    return () => {
      video.removeEventListener('loadedmetadata', handleLoadedMetadata)
      video.removeEventListener('timeupdate', handleTimeUpdate)
      video.removeEventListener('ended', handleEnded)
    }
  }, [])

  const togglePlay = () => {
    const video = videoRef.current
    if (!video) return

    if (isPlaying) {
      video.pause()
      setIsPlaying(false)
      onPause?.()
    } else {
      video.play()
      setIsPlaying(true)
      setShowPlayOverlay(false)
      onPlay?.()
    }
  }

  const toggleMute = () => {
    const video = videoRef.current
    if (!video) return

    video.muted = !isMuted
    setIsMuted(!isMuted)
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

  const restart = () => {
    const video = videoRef.current
    if (!video) return

    video.currentTime = 0
    setCurrentTime(0)
    setShowPlayOverlay(true)
    setIsPlaying(false)
  }

  const formatTime = (time: number) => {
    const minutes = Math.floor(time / 60)
    const seconds = Math.floor(time % 60)
    return `${minutes}:${seconds.toString().padStart(2, '0')}`
  }

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const video = videoRef.current
    if (!video) return

    const newTime = parseFloat(e.target.value)
    video.currentTime = newTime
    setCurrentTime(newTime)
  }

  const handleMouseDown = (e: React.MouseEvent) => {
    if (viewMode === '360' || viewMode === 'vr') {
      setIsDragging(true)
      setDragStart({ x: e.clientX, y: e.clientY })
    }
  }

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return
    
    const deltaX = e.clientX - dragStart.x
    const deltaY = e.clientY - dragStart.y
    
    setViewAngle(prev => ({
      x: prev.x + deltaX * 0.5,
      y: Math.max(-90, Math.min(90, prev.y + deltaY * 0.5))
    }))
    
    setDragStart({ x: e.clientX, y: e.clientY })
  }

  const handleMouseUp = () => {
    setIsDragging(false)
  }

  const handleZoomIn = () => {
    setZoom(prev => Math.min(prev + 0.2, 3))
  }

  const handleZoomOut = () => {
    setZoom(prev => Math.max(prev - 0.2, 0.5))
  }

  const resetView = () => {
    setZoom(1)
    setViewAngle({ x: 0, y: 0 })
    setRotation(0)
  }

  const toggleViewMode = () => {
    const modes: Array<'normal' | '360' | 'vr'> = ['normal', '360', 'vr']
    const currentIndex = modes.indexOf(viewMode)
    const nextMode = modes[(currentIndex + 1) % modes.length]
    setViewMode(nextMode)
    resetView()
  }

  return (
    <div 
      ref={containerRef}
      className={`relative bg-black rounded-xl overflow-hidden ${className}`}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
    >
      {/* Video Element */}
      <div 
        className="relative w-full h-full"
        style={{
          transform: `scale(${zoom}) rotateY(${viewAngle.x}deg) rotateX(${-viewAngle.y}deg)`,
          transition: isDragging ? 'none' : 'transform 0.3s ease',
          transformStyle: 'preserve-3d'
        }}
      >
        <video
          ref={videoRef}
          src={src}
          poster={thumbnail}
          className="w-full h-full object-contain"
          playsInline
          muted={isMuted}
          onClick={togglePlay}
          style={{
            transform: viewMode === '360' ? 'rotateY(0deg)' : 'none'
          }}
        />
      </div>

      {/* 3D/360 Overlay Effects */}
      {viewMode === '360' && (
        <div className="absolute inset-0 pointer-events-none">
          <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent animate-pulse" />
          <div className="absolute top-4 right-4 bg-black/50 backdrop-blur-sm rounded-lg px-3 py-2">
            <div className="flex items-center gap-2 text-white text-sm">
              <Move3d className="h-4 w-4" />
              360° View
            </div>
          </div>
        </div>
      )}

      {viewMode === 'vr' && (
        <div className="absolute inset-0 pointer-events-none">
          <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-transparent to-black/30" />
          <div className="absolute top-4 right-4 bg-black/50 backdrop-blur-sm rounded-lg px-3 py-2">
            <div className="flex items-center gap-2 text-white text-sm">
              <Eye className="h-4 w-4" />
              VR Mode
            </div>
          </div>
        </div>
      )}

      {/* Play Overlay */}
      {showPlayOverlay && !isLoading && (
        <div 
          className="absolute inset-0 bg-black/30 flex items-center justify-center cursor-pointer"
          onClick={togglePlay}
        >
          <div className="w-20 h-20 bg-white/90 rounded-full flex items-center justify-center hover:bg-white transition-all hover:scale-105">
            <Play className="h-10 w-10 text-gray-900 ml-2" />
          </div>
        </div>
      )}

      {/* Loading Spinner */}
      {isLoading && (
        <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
          <div className="text-center">
            <div className="w-12 h-12 border-3 border-white border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-white text-sm">Loading Virtual Tour...</p>
          </div>
        </div>
      )}

      {/* Enhanced Controls for 3D */}
      <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/90 to-transparent p-4">
        {/* Progress Bar */}
        <div className="mb-3">
          <input
            type="range"
            min="0"
            max={duration}
            value={currentTime}
            onChange={handleSeek}
            className="w-full h-2 bg-white/30 rounded-full appearance-none cursor-pointer slider"
            style={{
              background: `linear-gradient(to right, #3b82f6 ${(currentTime / duration) * 100}%, rgba(255,255,255,0.3) ${(currentTime / duration) * 100}%)`
            }}
          />
          <div className="flex justify-between text-xs text-white mt-1">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* Control Buttons */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Button
              size="sm"
              variant="ghost"
              className="text-white hover:bg-white/20"
              onClick={togglePlay}
            >
              {isPlaying ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
            </Button>
            
            <Button
              size="sm"
              variant="ghost"
              className="text-white hover:bg-white/20"
              onClick={restart}
            >
              <RotateCcw className="h-4 w-4" />
            </Button>

            <Button
              size="sm"
              variant="ghost"
              className="text-white hover:bg-white/20"
              onClick={toggleMute}
            >
              {isMuted ? <VolumeX className="h-4 w-4" /> : <Volume2 className="h-4 w-4" />}
            </Button>

            {/* 3D Controls */}
            <div className="h-6 w-px bg-white/30 mx-1" />
            
            <Button
              size="sm"
              variant="ghost"
              className="text-white hover:bg-white/20"
              onClick={toggleViewMode}
              title={`Current: ${viewMode}. Click to change view mode.`}
            >
              {viewMode === 'normal' && <Move3d className="h-4 w-4" />}
              {viewMode === '360' && <Navigation className="h-4 w-4" />}
              {viewMode === 'vr' && <Eye className="h-4 w-4" />}
            </Button>

            <Button
              size="sm"
              variant="ghost"
              className="text-white hover:bg-white/20"
              onClick={handleZoomOut}
              disabled={zoom <= 0.5}
            >
              <ZoomOut className="h-4 w-4" />
            </Button>

            <Button
              size="sm"
              variant="ghost"
              className="text-white hover:bg-white/20"
              onClick={handleZoomIn}
              disabled={zoom >= 3}
            >
              <ZoomIn className="h-4 w-4" />
            </Button>

            <Button
              size="sm"
              variant="ghost"
              className="text-white hover:bg-white/20"
              onClick={resetView}
              title="Reset view"
            >
              <Home className="h-4 w-4" />
            </Button>

            {onFlag && (
              <Button
                size="sm"
                variant="ghost"
                className="text-white hover:bg-red-500/20"
                onClick={onFlag}
                title="Report inappropriate content"
              >
                <Flag className="h-4 w-4" />
              </Button>
            )}
          </div>

          <Button
            size="sm"
            variant="ghost"
            className="text-white hover:bg-white/20"
            onClick={toggleFullscreen}
          >
            <Maximize2 className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Virtual Tour Badge */}
      <div className="absolute top-4 left-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white px-4 py-2 rounded-full text-sm font-medium flex items-center gap-2 shadow-lg">
        <div className="w-2 h-2 bg-white rounded-full animate-pulse" />
        {viewMode === 'normal' && 'Virtual Tour'}
        {viewMode === '360' && '360° Tour'}
        {viewMode === 'vr' && 'VR Tour'}
      </div>

      {/* Instructions Overlay */}
      {viewMode !== 'normal' && !isPlaying && (
        <div className="absolute top-20 left-4 right-4 bg-black/60 backdrop-blur-sm rounded-lg p-3">
          <p className="text-white text-sm">
            {viewMode === '360' && '🖱️ Drag to look around • Scroll to zoom • Click play to start'}
            {viewMode === 'vr' && '🥽 VR Mode • Use controls to navigate • Best experienced with VR headset'}
          </p>
        </div>
      )}

      <style jsx>{`
        .slider::-webkit-slider-thumb {
          appearance: none;
          width: 16px;
          height: 16px;
          background: #3b82f6;
          border-radius: 50%;
          cursor: pointer;
          border: 2px solid white;
        }
        .slider::-moz-range-thumb {
          width: 16px;
          height: 16px;
          background: #3b82f6;
          border-radius: 50%;
          cursor: pointer;
          border: 2px solid white;
        }
      `}</style>
    </div>
  )
}
