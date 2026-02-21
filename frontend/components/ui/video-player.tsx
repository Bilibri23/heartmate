"use client"

import React, { useState, useRef, useEffect } from 'react'
import { Play, Pause, Volume2, VolumeX, Maximize2, RotateCcw, Flag } from 'lucide-react'
import { Button } from './button'

interface VideoPlayerProps {
  src: string
  thumbnail?: string
  title?: string
  className?: string
  autoPlay?: boolean
  showControls?: boolean
  onPlay?: () => void
  onPause?: () => void
  onFlag?: () => void
  listingId?: string
}

export function VideoPlayer({
  src,
  thumbnail,
  title = "Virtual Tour",
  className = "",
  autoPlay = false,
  showControls = true,
  onPlay,
  onPause,
  onFlag,
  listingId
}: VideoPlayerProps) {
  const [isPlaying, setIsPlaying] = useState(false)
  const [isMuted, setIsMuted] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [showPlayOverlay, setShowPlayOverlay] = useState(true)
  const [isFullscreen, setIsFullscreen] = useState(false)
  
  const videoRef = useRef<HTMLVideoElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

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

  return (
    <div 
      ref={containerRef}
      className={`relative bg-black rounded-xl overflow-hidden ${className}`}
    >
      <video
        ref={videoRef}
        src={src}
        poster={thumbnail}
        className="w-full h-full object-contain"
        playsInline
        muted={isMuted}
        onClick={togglePlay}
      />

      {/* Play Overlay */}
      {showPlayOverlay && !isLoading && (
        <div 
          className="absolute inset-0 bg-black/30 flex items-center justify-center cursor-pointer"
          onClick={togglePlay}
        >
          <div className="w-16 h-16 bg-white/90 rounded-full flex items-center justify-center hover:bg-white transition-colors">
            <Play className="h-8 w-8 text-gray-900 ml-1" />
          </div>
        </div>
      )}

      {/* Loading Spinner */}
      {isLoading && (
        <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
          <div className="w-8 h-8 border-2 border-white border-t-transparent rounded-full animate-spin" />
        </div>
      )}

      {/* Controls */}
      {showControls && !isLoading && (
        <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-4">
          {/* Progress Bar */}
          <div className="mb-3">
            <input
              type="range"
              min="0"
              max={duration}
              value={currentTime}
              onChange={handleSeek}
              className="w-full h-1 bg-white/30 rounded-full appearance-none cursor-pointer slider"
              style={{
                background: `linear-gradient(to right, white ${(currentTime / duration) * 100}%, rgba(255,255,255,0.3) ${(currentTime / duration) * 100}%)`
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
      )}

      {/* Virtual Tour Badge */}
      <div className="absolute top-4 left-4 bg-blue-600 text-white px-3 py-1 rounded-full text-sm font-medium flex items-center gap-2">
        <div className="w-2 h-2 bg-white rounded-full animate-pulse" />
        Virtual Tour
      </div>

      <style jsx>{`
        .slider::-webkit-slider-thumb {
          appearance: none;
          width: 12px;
          height: 12px;
          background: white;
          border-radius: 50%;
          cursor: pointer;
        }
        .slider::-moz-range-thumb {
          width: 12px;
          height: 12px;
          background: white;
          border-radius: 50%;
          cursor: pointer;
          border: none;
        }
      `}</style>
    </div>
  )
}
