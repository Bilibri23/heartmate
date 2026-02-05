"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  Bell, 
  MessageCircle, 
  FileText, 
  CreditCard, 
  CheckCircle,
  Home,
  Trash2,
  User,
  MessageSquare
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import Link from "next/link"
import api from "@/lib/api"

interface Notification {
  id: string
  type: string // Backend uses enum, can be any notification type
  title: string
  message: string | null
  read: boolean
  actionUrl: string | null
  createdAt: string
  referenceId?: string | null
  referenceType?: string | null
}

// Map backend notification types to icons and colors
const getNotificationIcon = (type: string) => {
  if (type?.includes("APPLICATION")) return FileText
  if (type?.includes("MESSAGE")) return MessageCircle
  if (type?.includes("PAYMENT")) return CreditCard
  if (type?.includes("LEASE")) return FileText
  if (type?.includes("LISTING")) return Home
  if (type?.includes("MATCH")) return User
  if (type?.includes("REVIEW")) return MessageSquare
  return Bell
}

const getNotificationColor = (type: string) => {
  if (type?.includes("APPLICATION")) return "bg-blue-100 text-blue-600"
  if (type?.includes("MESSAGE")) return "bg-emerald-100 text-emerald-600"
  if (type?.includes("PAYMENT")) return "bg-amber-100 text-amber-600"
  if (type?.includes("LEASE")) return "bg-purple-100 text-purple-600"
  if (type?.includes("LISTING")) return "bg-pink-100 text-pink-600"
  if (type?.includes("MATCH")) return "bg-indigo-100 text-indigo-600"
  if (type?.includes("REVIEW")) return "bg-yellow-100 text-yellow-600"
  return "bg-slate-100 text-slate-600"
}

export default function NotificationsPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [isLoading, setIsLoading] = useState(true)

  const fetchNotifications = useCallback(async () => {
    if (!user?.id) {
      setIsLoading(false)
      return
    }
    
    setIsLoading(true)
    try {
      const response = await api.get("/notifications", {
        params: { page: 0, size: 50 },
        validateStatus: (status) => status < 500,
      })
      if (response.status === 401 || response.status === 403) {
        setNotifications([])
        return
      }
      
      // Backend returns Spring Page object: { content: [...], totalElements, totalPages, etc. }
      let notifications: Notification[] = []
      
      if (response.data) {
        // Check if it's a Page object with content array
        if (response.data.content && Array.isArray(response.data.content)) {
          notifications = response.data.content
        } 
        // Fallback: if it's already an array
        else if (Array.isArray(response.data)) {
          notifications = response.data
        }
      }
      
      setNotifications(notifications)
    } catch (err: any) {
      console.error("Failed to fetch notifications:", err)
      console.error("Error details:", err.response?.data || err.message)
      // Set empty array on error to show empty state
      setNotifications([])
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchNotifications()
  }, [fetchNotifications])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchNotifications,
  })

  const markAsRead = async (notificationId: string) => {
    try {
      await api.put(`/notifications/${notificationId}/read`)
      setNotifications(prev => 
        prev.map(n => n.id === notificationId ? { ...n, read: true } : n)
      )
    } catch (err) {
      console.error("Failed to mark as read:", err)
    }
  }

  const markAllAsRead = async () => {
    try {
      await api.put("/notifications/read-all")
      setNotifications(prev => prev.map(n => ({ ...n, read: true })))
    } catch (err) {
      console.error("Failed to mark all as read:", err)
    }
  }

  const deleteNotification = async (notificationId: string) => {
    try {
      await api.delete(`/notifications/${notificationId}`)
      setNotifications(prev => prev.filter(n => n.id !== notificationId))
    } catch (err) {
      console.error("Failed to delete notification:", err)
    }
  }

  const formatTime = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / (1000 * 60))
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

    if (diffMins < 1) return "Just now"
    if (diffMins < 60) return `${diffMins}m ago`
    if (diffHours < 24) return `${diffHours}h ago`
    if (diffDays < 7) return `${diffDays}d ago`
    return date.toLocaleDateString("fr-CM", { day: "numeric", month: "short" })
  }

  const unreadCount = notifications.filter(n => !n.read).length

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.notifications.title} showNotifications={false} />

      {/* Header Actions */}
      {notifications.length > 0 && unreadCount > 0 && (
        <div className="bg-white border-b border-slate-200 px-4 py-2">
          <Button 
            variant="ghost" 
            size="sm" 
            onClick={markAllAsRead}
            className="text-blue-600"
          >
            <CheckCircle className="h-4 w-4 mr-1" />
            {t.notifications.markAllRead}
          </Button>
        </div>
      )}

      {/* Content */}
      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        {/* Loading */}
        {isLoading && (
          <div className="p-4 space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="flex items-start gap-3 p-4 bg-white rounded-xl">
                <Skeleton className="h-10 w-10 rounded-full" />
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-3 w-full" />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!isLoading && notifications.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
            <div className="bg-slate-100 p-6 rounded-full mb-4">
              <Bell className="h-12 w-12 text-slate-400" />
            </div>
            <h3 className="text-lg font-semibold text-slate-900 mb-2">
              {t.notifications.noNotifications}
            </h3>
            <p className="text-slate-500 text-sm max-w-xs">
              You're all caught up! New notifications will appear here.
            </p>
          </div>
        )}

        {/* Notifications List */}
        {!isLoading && notifications.length > 0 && (
          <div className="divide-y divide-slate-100">
            {notifications.map((notification) => {
              const Icon = getNotificationIcon(notification.type)
              const colorClass = getNotificationColor(notification.type)

              const content = (
                <div 
                  className={`flex items-start gap-3 p-4 transition-colors ${
                    notification.read ? "bg-white" : "bg-blue-50/50"
                  }`}
                  onClick={() => !notification.read && markAsRead(notification.id)}
                >
                  <div className={`flex-shrink-0 h-10 w-10 rounded-full flex items-center justify-center ${colorClass}`}>
                    <Icon className="h-5 w-5" />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <h3 className={`text-sm ${notification.read ? "text-slate-700" : "text-slate-900 font-semibold"}`}>
                        {notification.title}
                      </h3>
                      <span className="text-xs text-slate-500 flex-shrink-0">
                        {formatTime(notification.createdAt)}
                      </span>
                    </div>
                    <p className="text-sm text-slate-500 mt-0.5 line-clamp-2">
                      {notification.message}
                    </p>
                  </div>

                  <button
                    onClick={(e) => {
                      e.preventDefault()
                      e.stopPropagation()
                      deleteNotification(notification.id)
                    }}
                    className="flex-shrink-0 p-2 text-slate-400 hover:text-red-500"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              )

              if (notification.actionUrl) {
                return (
                  <Link key={notification.id} href={notification.actionUrl}>
                    {content}
                  </Link>
                )
              }

              return <div key={notification.id}>{content}</div>
            })}
          </div>
        )}
      </div>
    </div>
  )
}
