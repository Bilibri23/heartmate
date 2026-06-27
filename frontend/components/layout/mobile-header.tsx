"use client"

import { Bell, Globe, ArrowLeft } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useEffect, useState, useCallback } from "react"
import { useLanguage } from "@/context/language-context"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/context/auth-context"
import api, { isRateLimited } from "@/lib/api"
import { useWebSocketNotifications } from "@/hooks/use-websocket-notifications"
import { toast } from "sonner"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

interface MobileHeaderProps {
  title?: string
  showNotifications?: boolean
  showLanguage?: boolean
  showBack?: boolean
}

export function MobileHeader({ 
  title = "RoomBay", 
  showNotifications = true,
  showLanguage = true,
  showBack = false
}: MobileHeaderProps) {
  const { language, setLanguage, t } = useLanguage()
  const router = useRouter()
  const { user } = useAuth()
  const [unreadCount, setUnreadCount] = useState(0)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const fetchUnreadCount = useCallback(async () => {
    // Skip while in a 429 backoff window so we don't keep hammering the API.
    if (!user?.id || !showNotifications || isRateLimited()) return
    try {
      const response = await api.get("/notifications/unread-count")
      const count = response.data?.count ?? 0
      setUnreadCount(Number.isFinite(count) ? count : 0)
    } catch {
      // Backoff is recorded by the API client (429); stay quiet to avoid console flooding.
    }
  }, [user?.id, showNotifications])

  useEffect(() => {
    fetchUnreadCount()
  }, [fetchUnreadCount])

  // Refetch when tab becomes visible (user returns to app) - fallback when WebSocket fails
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") fetchUnreadCount()
    }
    document.addEventListener("visibilitychange", handleVisibilityChange)
    return () => document.removeEventListener("visibilitychange", handleVisibilityChange)
  }, [fetchUnreadCount])

  // Poll every 60s as fallback when WebSocket doesn't deliver
  useEffect(() => {
    if (!user?.id || !showNotifications) return
    const interval = setInterval(fetchUnreadCount, 60000)
    return () => clearInterval(interval)
  }, [fetchUnreadCount, user?.id, showNotifications])

  const handleNotification = useCallback((notification: { title: string; message?: string; actionUrl?: string }) => {
    setUnreadCount((prev) => prev + 1)
    
    // Show toast notification
    toast(notification.title, {
      description: notification.message,
      action: notification.actionUrl ? {
        label: "View",
        onClick: () => router.push(notification.actionUrl!),
      } : undefined,
    })
  }, [router])

  useWebSocketNotifications({
    enabled: mounted && Boolean(user?.id) && showNotifications,
    onNotification: handleNotification,
  })

  if (!mounted) return null

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur-lg safe-area-top">
      <div className="flex h-14 items-center justify-between px-4">
        <div className="flex items-center gap-2">
          {showBack && (
            <Button 
              variant="ghost" 
              size="icon" 
              className="h-9 w-9"
              onClick={() => router.back()}
            >
              <ArrowLeft className="h-5 w-5 text-slate-600" />
            </Button>
          )}
          <h1 className="text-lg font-bold text-slate-900">{title}</h1>
        </div>
        
        <div className="flex items-center gap-2">
          {showLanguage && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-9 w-9">
                  <Globe className="h-5 w-5 text-slate-600" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem 
                  onClick={() => setLanguage("en")}
                  className={language === "en" ? "bg-blue-50" : ""}
                >
                  🇬🇧 English
                </DropdownMenuItem>
                <DropdownMenuItem 
                  onClick={() => setLanguage("fr")}
                  className={language === "fr" ? "bg-blue-50" : ""}
                >
                  🇫🇷 Français
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
          
          {showNotifications && (
            <Link href="/notifications">
              <Button variant="ghost" size="icon" className="relative h-9 w-9">
                <Bell className="h-5 w-5 text-slate-600" />
                {/* Notification badge - will be dynamic */}
                {unreadCount > 0 && (
                  <span className="absolute right-1 top-1 min-w-[16px] h-4 px-1 rounded-full bg-red-500 text-white text-[10px] flex items-center justify-center">
                    {unreadCount > 9 ? "9+" : unreadCount}
                  </span>
                )}
              </Button>
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
