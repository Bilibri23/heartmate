"use client"

import { useEffect, useRef, useCallback, useState } from "react"
import { Client } from "@stomp/stompjs"
import SockJS from "sockjs-client"
import { useAuth } from "@/context/auth-context"

interface Notification {
  id: string
  type: string
  title: string
  message: string
  referenceId?: string
  referenceType?: string
  actionUrl?: string
  read: boolean
  createdAt: string
}

interface UseWebSocketNotificationsOptions {
  onNotification?: (notification: Notification) => void
  enabled?: boolean
}

// Simple WebSocket hook for real-time notifications
// Uses native WebSocket with STOMP-like message handling
export function useWebSocketNotifications(options: UseWebSocketNotificationsOptions = {}) {
  const { onNotification, enabled = true } = options
  const { user } = useAuth()
  const clientRef = useRef<Client | null>(null)
  const isDisconnectingRef = useRef(false)
  const onNotificationRef = useRef(onNotification)
  const [isConnected, setIsConnected] = useState(false)
  const [lastNotification, setLastNotification] = useState<Notification | null>(null)

  // Keep the callback ref up to date
  useEffect(() => {
    onNotificationRef.current = onNotification
  }, [onNotification])

  const getToken = () => {
    if (typeof window !== "undefined") {
      return localStorage.getItem("token")
    }
    return null
  }

  const connect = useCallback(async () => {
    if (!user?.id || !enabled) return

    const token = getToken()
    if (!token) return

    try {
      // Prefer the configured backend origin, but fall back to same-origin /ws so Next rewrites can proxy it.
      const configuredApiUrl = process.env.NEXT_PUBLIC_API_URL
      const wsUrl = configuredApiUrl
        ? `${configuredApiUrl.replace(/\/api\/?$/, "").replace(/\/$/, "")}/ws`
        : "/ws"

      // Probe the SockJS endpoint before connecting to avoid noisy errors
      try {
        const controller = new AbortController()
        const timeoutId = setTimeout(() => controller.abort(), 1500)
        const probe = await fetch(`${wsUrl}/info`, { signal: controller.signal })
        clearTimeout(timeoutId)
        if (!probe.ok) {
          console.warn("[WebSocket] SockJS not available:", probe.status)
          return
        }
      } catch (err) {
        console.warn("[WebSocket] SockJS probe failed:", err)
        return
      }

      if (clientRef.current) {
        clientRef.current.deactivate()
      }

      isDisconnectingRef.current = false
      const client = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        onConnect: () => {
          setIsConnected(true)
          client.subscribe(`/user/queue/notifications`, (message) => {
            if (!message.body) return
            try {
              const notification: Notification = JSON.parse(message.body)
              setLastNotification(notification)
              onNotificationRef.current?.(notification)
            } catch (error) {
              console.error("[WebSocket] Failed to parse message:", error)
            }
          })
        },
        onStompError: (frame) => {
          console.error("[WebSocket] STOMP error:", frame.headers["message"])
          setIsConnected(false)
        },
        onWebSocketError: () => {
          setIsConnected(false)
        },
        onWebSocketClose: () => {
          setIsConnected(false)
        },
      })

      client.activate()
      clientRef.current = client
    } catch (error) {
      console.error("[WebSocket] Connection failed:", error)
    }
  }, [user?.id, enabled])

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      isDisconnectingRef.current = true
      clientRef.current.deactivate()
      clientRef.current = null
    }
    setIsConnected(false)
    setTimeout(() => {
      isDisconnectingRef.current = false
    }, 500)
  }, [])

  useEffect(() => {
    connect()
    return () => disconnect()
  }, [connect, disconnect])

  return {
    isConnected,
    lastNotification,
    reconnect: connect,
    disconnect,
  }
}
