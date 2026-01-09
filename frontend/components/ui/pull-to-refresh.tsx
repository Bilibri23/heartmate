"use client"

import { RefreshCw } from "lucide-react"
import { cn } from "@/lib/utils"

interface PullToRefreshIndicatorProps {
  pullProgress: number
  isRefreshing: boolean
}

export function PullToRefreshIndicator({ 
  pullProgress, 
  isRefreshing 
}: PullToRefreshIndicatorProps) {
  if (pullProgress === 0 && !isRefreshing) return null

  return (
    <div 
      className="absolute left-0 right-0 top-0 flex items-center justify-center overflow-hidden transition-all"
      style={{ 
        height: isRefreshing ? 48 : pullProgress * 48,
        opacity: Math.min(pullProgress * 2, 1)
      }}
    >
      <RefreshCw 
        className={cn(
          "h-6 w-6 text-blue-600 transition-transform",
          isRefreshing && "animate-spin"
        )}
        style={{ 
          transform: `rotate(${pullProgress * 360}deg)` 
        }}
      />
    </div>
  )
}
