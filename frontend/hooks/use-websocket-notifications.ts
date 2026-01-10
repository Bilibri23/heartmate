"use client"

import { useEffect, useRef, useCallback, useState } from "react"
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
  const wsRef = useRef<WebSocket | null>(null)
  const [isConnected, setIsConnected] = useState(false)
  const [lastNotification, setLastNotification] = useState<Notification | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)

  const getToken = () => {
    if (typeof window !== "undefined") {
      return localStorage.getItem("accessToken")
    }
    return null
  }

  const connect = useCallback(() => {
    if (!user?.id || !enabled) return

    const token = getToken()
    if (!token) return

    // Close existing connection
    if (wsRef.current) {
      wsRef.current.close()
    }

    try {
      // Connect to WebSocket endpoint
      const wsUrl = `${process.env.NEXT_PUBLIC_API_URL?.replace('http', 'ws').replace('/api', '')}/ws/websocket`
      const ws = new WebSocket(wsUrl)

      ws.onopen = () => {
        console.log("[WebSocket] Connected")
        setIsConnected(true)
        
        // Send CONNECT frame
        ws.send(`CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer ${token}\n\n\0`)
      }

      ws.onmessage = (event) => {
        const data = event.data
        
        // Parse STOMP frame
        if (data.includes("CONNECTED")) {
          console.log("[WebSocket] STOMP Connected")
          // Subscribe to notifications
          ws.send(`SUBSCRIBE\nid:sub-0\ndestination:/user/${user.id}/queue/notifications\n\n\0`)
        } else if (data.includes("MESSAGE")) {
          try {
            // Extract body from STOMP message
            const bodyStart = data.indexOf("\n\n") + 2
            const bodyEnd = data.indexOf("\0")
            const body = data.substring(bodyStart, bodyEnd)
            
            if (body) {
              const notification: Notification = JSON.parse(body)
              console.log("[WebSocket] Received notification:", notification)
              setLastNotification(notification)
              onNotification?.(notification)
            }
          } catch (error) {
            console.error("[WebSocket] Failed to parse message:", error)
          }
        }
      }

      ws.onclose = () => {
        console.log("[WebSocket] Disconnected")
        setIsConnected(false)
        
        // Attempt to reconnect after 5 seconds
        if (enabled && user?.id) {
          reconnectTimeoutRef.current = setTimeout(() => {
            console.log("[WebSocket] Attempting to reconnect...")
            connect()
          }, 5000)
        }
      }

      ws.onerror = (error) => {
        console.error("[WebSocket] Error:", error)
        setIsConnected(false)
      }

      wsRef.current = ws
    } catch (error) {
      console.error("[WebSocket] Connection failed:", error)
    }
  }, [user?.id, enabled, onNotification])

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
    }
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
      setIsConnected(false)
    }
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
