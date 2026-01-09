"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { MessageSquare, Search } from "lucide-react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import Link from "next/link"
import api from "@/lib/api"

interface Conversation {
  id: string
  participantId: string
  participantName: string
  participantPhotoUrl: string | null
  lastMessage: string
  lastMessageTime: string
  unreadCount: number
  listingTitle?: string
}

export default function MessagesPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")

  const fetchConversations = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await api.get("/messages/conversations")
      setConversations(response.data || [])
    } catch (err) {
      console.error("Failed to fetch conversations:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchConversations()
  }, [fetchConversations])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchConversations,
  })

  const formatTime = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24))
    
    if (diffDays === 0) {
      return date.toLocaleTimeString("fr-CM", { hour: "2-digit", minute: "2-digit" })
    } else if (diffDays === 1) {
      return "Yesterday"
    } else if (diffDays < 7) {
      return date.toLocaleDateString("fr-CM", { weekday: "short" })
    } else {
      return date.toLocaleDateString("fr-CM", { day: "numeric", month: "short" })
    }
  }

  const filteredConversations = conversations.filter((conv) => {
    if (!searchQuery) return true
    return conv.participantName.toLowerCase().includes(searchQuery.toLowerCase())
  })

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.messages.title} />

      {/* Search */}
      <div className="sticky top-14 z-30 bg-white border-b border-slate-200 p-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input
            placeholder="Search conversations..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 h-11 rounded-xl border-slate-200 bg-slate-50"
          />
        </div>
      </div>

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
              <div key={i} className="flex items-center gap-3 p-3 bg-white rounded-xl">
                <Skeleton className="h-12 w-12 rounded-full" />
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-3 w-full" />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!isLoading && conversations.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
            <div className="bg-slate-100 p-6 rounded-full mb-4">
              <MessageSquare className="h-12 w-12 text-slate-400" />
            </div>
            <h3 className="text-lg font-semibold text-slate-900 mb-2">
              {t.messages.noMessages}
            </h3>
            <p className="text-slate-500 text-sm max-w-xs">
              When you contact a landlord or receive messages, they will appear here.
            </p>
          </div>
        )}

        {/* Conversations List */}
        {!isLoading && filteredConversations.length > 0 && (
          <div className="divide-y divide-slate-100">
            {filteredConversations.map((conv) => (
              <Link key={conv.id} href={`/messages/${conv.participantId}`}>
                <div className="flex items-center gap-3 p-4 bg-white hover:bg-slate-50 transition-colors active:bg-slate-100">
                  <div className="relative">
                    <Avatar className="h-12 w-12">
                      <AvatarImage src={conv.participantPhotoUrl || undefined} />
                      <AvatarFallback className="bg-blue-100 text-blue-600">
                        {conv.participantName.split(" ").map(n => n[0]).join("").toUpperCase()}
                      </AvatarFallback>
                    </Avatar>
                    {conv.unreadCount > 0 && (
                      <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center">
                        {conv.unreadCount > 9 ? "9+" : conv.unreadCount}
                      </span>
                    )}
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <h3 className={`font-medium truncate ${conv.unreadCount > 0 ? "text-slate-900" : "text-slate-700"}`}>
                        {conv.participantName}
                      </h3>
                      <span className="text-xs text-slate-500 flex-shrink-0">
                        {formatTime(conv.lastMessageTime)}
                      </span>
                    </div>
                    
                    {conv.listingTitle && (
                      <p className="text-xs text-blue-600 truncate">
                        Re: {conv.listingTitle}
                      </p>
                    )}
                    
                    <p className={`text-sm truncate ${conv.unreadCount > 0 ? "text-slate-900 font-medium" : "text-slate-500"}`}>
                      {conv.lastMessage}
                    </p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
