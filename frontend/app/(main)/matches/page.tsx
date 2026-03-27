"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import api from "@/lib/api"
import Link from "next/link"
import { 
  Users, 
  Heart, 
  X, 
  MessageCircle, 
  Sparkles,
  Check,
  Clock,
  UserPlus,
  Settings,
  ChevronRight,
  LayoutGrid,
  Layers,
  MapPin,
  Briefcase,
  GraduationCap,
  Moon,
  Sun,
  Coffee,
  Music,
  Dumbbell,
  ChevronDown,
  RotateCcw,
  Star,
  Info,
  FileText,
  Home
} from "lucide-react"

interface MatchResponse {
  id: string
  userId: string
  matchedUserId: string
  matchedUserFirstName: string
  matchedUserLastName: string
  matchedUserEmail?: string
  matchedUserProfilePhotoUrl?: string
  matchedUserWhatsapp?: string
  compatibilityScore: number
  budgetScore?: number
  lifestyleScore?: number
  scheduleScore?: number
  locationScore?: number
  habitsScore?: number
  status: string
  userAction?: string
  matchedUserAction?: string
  isMutualMatch?: boolean
  explanation?: string
  createdAt: string
  updatedAt?: string
}

interface Match {
  id: string
  matchedUser: {
    id: string
    firstName: string
    lastName: string
    email?: string
    profilePhotoUrl?: string
    whatsapp?: string
  }
  compatibilityScore: number
  budgetScore?: number
  lifestyleScore?: number
  scheduleScore?: number
  locationScore?: number
  habitsScore?: number
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "MUTUAL"
  userAction?: string
  matchedUserAction?: string
  isMutualMatch: boolean
  explanation?: string
  createdAt: string
}

const STATUS_STYLES = {
  PENDING: "bg-amber-100 text-amber-700",
  ACCEPTED: "bg-green-100 text-green-700",
  REJECTED: "bg-red-100 text-red-700",
  MUTUAL: "bg-green-100 text-green-700",
}

const STATUS_LABELS = {
  PENDING: "Pending",
  ACCEPTED: "Matched!",
  REJECTED: "Declined",
  MUTUAL: "Matched!",
}

export default function MatchesPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [matches, setMatches] = useState<Match[]>([])
  const [pendingMatches, setPendingMatches] = useState<Match[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isFinding, setIsFinding] = useState(false)
  const [activeTab, setActiveTab] = useState<"discover" | "pending" | "matched">("discover")
  const [viewMode, setViewMode] = useState<"grid" | "swipe">("grid")
  const [currentSwipeIndex, setCurrentSwipeIndex] = useState(0)
  const [selectedMatch, setSelectedMatch] = useState<Match | null>(null)
  const [swipeDirection, setSwipeDirection] = useState<"left" | "right" | null>(null)
  const cardRef = useRef<HTMLDivElement>(null)

  const normalizeMatch = (m: MatchResponse): Match => ({
    id: m.id,
    matchedUser: {
      id: m.matchedUserId,
      firstName: m.matchedUserFirstName || "Unknown",
      lastName: m.matchedUserLastName || "",
      email: m.matchedUserEmail,
      profilePhotoUrl: m.matchedUserProfilePhotoUrl,
      whatsapp: m.matchedUserWhatsapp,
    },
    compatibilityScore: m.compatibilityScore || 0,
    budgetScore: m.budgetScore,
    lifestyleScore: m.lifestyleScore,
    scheduleScore: m.scheduleScore,
    locationScore: m.locationScore,
    habitsScore: m.habitsScore,
    status: (m.status as Match["status"]) || "PENDING",
    userAction: m.userAction,
    matchedUserAction: m.matchedUserAction,
    isMutualMatch: m.isMutualMatch || false,
    explanation: m.explanation,
    createdAt: m.createdAt,
  })

  const fetchMatches = useCallback(async () => {
    if (!user?.id) return []
    
    setIsLoading(true)
    try {
      const [allRes, pendingRes] = await Promise.all([
        api.get(`/matches/${user.id}`),
        api.get(`/matches/${user.id}/pending`)
      ])
      
      const rawMatches: MatchResponse[] = allRes.data || []
      const rawPending: MatchResponse[] = pendingRes.data || []
      
      const normalizedAll = rawMatches.map(normalizeMatch)
      const normalizedPending = rawPending.map(normalizeMatch)
      
      setMatches(normalizedAll)
      setPendingMatches(normalizedPending)
      
      return normalizedAll
    } catch (err) {
      console.error("Failed to fetch matches:", err)
      setMatches([])
      setPendingMatches([])
      return []
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  const findNewMatches = useCallback(async () => {
    if (!user?.id) return []
    
    setIsFinding(true)
    try {
      const response = await api.post(`/matches/find`, null, {
        params: { userId: user.id }
      })
      const rawMatches: MatchResponse[] = response.data || []
      const normalized = rawMatches.map(normalizeMatch)
      setMatches(normalized)
      return normalized
    } catch (err) {
      console.error("Failed to find matches:", err)
      return []
    } finally {
      setIsFinding(false)
    }
  }, [user?.id])

  useEffect(() => {
    const initMatches = async () => {
      const existingMatches = await fetchMatches()
      // Auto-find matches if none exist
      if (!existingMatches || existingMatches.length === 0) {
        await findNewMatches()
      }
    }
    initMatches()
  }, [fetchMatches, findNewMatches])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: async () => { await fetchMatches() },
  })

  const handleAction = async (matchId: string, action: "ACCEPT" | "REJECT") => {
    if (!user?.id) return
    
    try {
      await api.post(`/matches/${matchId}/action?userId=${user.id}`, { action })
      fetchMatches()
    } catch (err) {
      console.error("Failed to perform action:", err)
    }
  }

  // Discover: matches where the other user initiated (we need to respond)
  // Sent: matches where we initiated and waiting for response
  // Use userAction to determine who initiated
  const discoveredMatches = matches.filter(m => m.status === "PENDING" && !m.userAction)
  const mutualMatches = matches.filter(m => m.status === "MUTUAL" || m.status === "ACCEPTED" || m.isMutualMatch)
  const sentRequests = matches.filter(m => m.status === "PENDING" && m.userAction === "ACCEPT")

  // Swipe handlers
  const handleSwipeAction = async (action: "ACCEPT" | "REJECT") => {
    const currentMatch = discoveredMatches[currentSwipeIndex]
    if (!currentMatch) return
    
    setSwipeDirection(action === "ACCEPT" ? "right" : "left")
    
    setTimeout(async () => {
      await handleAction(currentMatch.id, action)
      setSwipeDirection(null)
      if (currentSwipeIndex < discoveredMatches.length - 1) {
        setCurrentSwipeIndex(prev => prev + 1)
      }
    }, 300)
  }

  const resetSwipe = () => {
    setCurrentSwipeIndex(0)
  }

  // Get compatibility color
  const getScoreColor = (score: number) => {
    if (score >= 80) return "text-green-600"
    if (score >= 60) return "text-blue-600"
    if (score >= 40) return "text-amber-600"
    return "text-red-500"
  }

  const getScoreBg = (score: number) => {
    if (score >= 80) return "bg-green-100"
    if (score >= 60) return "bg-blue-100"
    if (score >= 40) return "bg-amber-100"
    return "bg-red-100"
  }

  // LinkedIn-style Grid Card (compact)
  const renderGridCard = (match: Match) => {
    if (!match.matchedUser) return null
    
    return (
      <div 
        key={match.id}
        className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 hover:shadow-md transition-shadow cursor-pointer"
        onClick={() => setSelectedMatch(match)}
      >
        {/* Profile Header */}
        <div className="flex items-center gap-3 mb-3">
          <div className="relative">
            <Avatar className="h-14 w-14 ring-2 ring-white shadow-sm">
              <AvatarImage src={match.matchedUser.profilePhotoUrl || undefined} />
              <AvatarFallback className="bg-blue-100 text-blue-600 text-lg font-semibold">
                {match.matchedUser.firstName?.[0] || "?"}{match.matchedUser.lastName?.[0] || ""}
              </AvatarFallback>
            </Avatar>
            {/* Compatibility Badge */}
            <div className={`absolute -bottom-1 -right-1 w-7 h-7 rounded-full ${getScoreBg(match.compatibilityScore)} flex items-center justify-center`}>
              <span className={`text-xs font-bold ${getScoreColor(match.compatibilityScore)}`}>
                {match.compatibilityScore}
              </span>
            </div>
          </div>
          
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-slate-900 truncate">
              {match.matchedUser.firstName} {match.matchedUser.lastName?.[0]}.
            </h3>
            <p className="text-xs text-slate-500 flex items-center gap-1">
              <Sparkles className="h-3 w-3 text-blue-500" />
              {match.compatibilityScore}% compatible
            </p>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="flex gap-1 mb-3 flex-wrap">
          {match.budgetScore && match.budgetScore >= 70 && (
            <span className="px-2 py-0.5 bg-green-50 text-green-700 text-xs rounded-full">💰 Budget</span>
          )}
          {match.lifestyleScore && match.lifestyleScore >= 70 && (
            <span className="px-2 py-0.5 bg-purple-50 text-purple-700 text-xs rounded-full">🎯 Lifestyle</span>
          )}
          {match.scheduleScore && match.scheduleScore >= 70 && (
            <span className="px-2 py-0.5 bg-blue-50 text-blue-700 text-xs rounded-full">⏰ Schedule</span>
          )}
        </div>

        {/* Action Buttons */}
        {match.status === "PENDING" && !match.userAction && (
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              className="flex-1 rounded-xl border-slate-200 text-slate-600 hover:bg-slate-50 h-9"
              onClick={(e) => { e.stopPropagation(); handleAction(match.id, "REJECT") }}
            >
              <X className="h-4 w-4" />
            </Button>
            <Button
              size="sm"
              className="flex-1 rounded-xl bg-blue-600 hover:bg-blue-700 h-9"
              onClick={(e) => { e.stopPropagation(); handleAction(match.id, "ACCEPT") }}
            >
              <Heart className="h-4 w-4 mr-1" />
              Connect
            </Button>
          </div>
        )}

        {(match.status === "MUTUAL" || match.status === "ACCEPTED" || match.isMutualMatch) && (
          <Link href={`/messages/${match.matchedUser?.id}`} onClick={(e) => e.stopPropagation()}>
            <Button size="sm" className="w-full rounded-xl bg-green-600 hover:bg-green-700 h-9">
              <MessageCircle className="h-4 w-4 mr-1" />
              Message
            </Button>
          </Link>
        )}

        {match.status === "PENDING" && match.userAction === "ACCEPT" && (
          <div className="flex items-center justify-center gap-2 py-2 text-amber-600 text-sm">
            <Clock className="h-4 w-4" />
            Waiting for response
          </div>
        )}
      </div>
    )
  }

  // Tinder-style Swipe Card (full)
  const renderSwipeCard = (match: Match) => {
    if (!match.matchedUser) return null
    
    return (
      <div 
        ref={cardRef}
        className={`relative bg-white rounded-3xl shadow-xl overflow-hidden transition-transform duration-300 ${
          swipeDirection === "left" ? "-translate-x-full rotate-[-20deg] opacity-0" :
          swipeDirection === "right" ? "translate-x-full rotate-[20deg] opacity-0" : ""
        }`}
        style={{ height: "calc(100vh - 320px)", minHeight: "400px" }}
      >
        {/* Photo/Avatar Section */}
        <div className="relative h-2/3 bg-blue-500">
          {match.matchedUser.profilePhotoUrl ? (
            <img 
              src={match.matchedUser.profilePhotoUrl} 
              alt={match.matchedUser.firstName}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <span className="text-8xl font-bold text-white/30">
                {match.matchedUser.firstName?.[0]}{match.matchedUser.lastName?.[0]}
              </span>
            </div>
          )}
          
          {/* Gradient Overlay */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
          
          {/* Compatibility Score */}
          <div className="absolute top-4 right-4 bg-white/90 backdrop-blur-sm rounded-full px-3 py-1.5 flex items-center gap-1.5 shadow-lg">
            <Sparkles className="h-4 w-4 text-blue-600" />
            <span className={`font-bold ${getScoreColor(match.compatibilityScore)}`}>
              {match.compatibilityScore}%
            </span>
          </div>

          {/* Name & Info */}
          <div className="absolute bottom-4 left-4 right-4 text-white">
            <h2 className="text-2xl font-bold mb-1">
              {match.matchedUser.firstName} {match.matchedUser.lastName}
            </h2>
            {match.explanation && (
              <p className="text-white/80 text-sm line-clamp-2">{match.explanation}</p>
            )}
          </div>
        </div>

        {/* Details Section */}
        <div className="p-4 h-1/3 overflow-y-auto">
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-medium text-slate-700">Compatibility Breakdown</span>
            <button 
              onClick={() => setSelectedMatch(match)}
              className="text-blue-600 text-sm font-medium flex items-center gap-1"
            >
              View Profile <ChevronRight className="h-4 w-4" />
            </button>
          </div>
          
          {/* Score Bars */}
          <div className="space-y-2">
            {[
              { label: "Budget", score: match.budgetScore, icon: "💰" },
              { label: "Lifestyle", score: match.lifestyleScore, icon: "🎯" },
              { label: "Schedule", score: match.scheduleScore, icon: "⏰" },
              { label: "Location", score: match.locationScore, icon: "📍" },
            ].map((item) => item.score !== undefined && (
              <div key={item.label} className="flex items-center gap-2">
                <span className="text-sm w-20">{item.icon} {item.label}</span>
                <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div 
                    className={`h-full rounded-full transition-all ${
                      item.score >= 70 ? "bg-green-500" : 
                      item.score >= 50 ? "bg-blue-500" : "bg-amber-500"
                    }`}
                    style={{ width: `${item.score}%` }}
                  />
                </div>
                <span className="text-xs text-slate-500 w-8">{item.score}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  // Original card for sent/matched tabs
  const renderMatchCard = (match: Match, showActions: boolean = false) => {
    if (!match.matchedUser) return null
    
    return (
      <div 
        key={match.id}
        className="bg-white rounded-2xl p-4 shadow-sm"
      >
        <div className="flex items-start gap-3">
          <Avatar className="h-14 w-14">
            <AvatarImage src={match.matchedUser.profilePhotoUrl || undefined} />
            <AvatarFallback className="bg-blue-100 text-blue-600 text-lg">
              {match.matchedUser.firstName?.[0] || "?"}{match.matchedUser.lastName?.[0] || ""}
            </AvatarFallback>
          </Avatar>
          
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-slate-900">
                {match.matchedUser.firstName || "Unknown"} {match.matchedUser.lastName || ""}
              </h3>
              <div className="flex items-center gap-1 text-blue-600">
                <Sparkles className="h-4 w-4" />
                <span className="text-sm font-medium">{match.compatibilityScore || 0}%</span>
              </div>
            </div>
            
            {match.explanation && (
              <p className="text-sm text-slate-500 truncate">
                {match.explanation}
              </p>
            )}
            
            <div className="flex items-center gap-2 mt-2">
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[match.status]}`}>
                {STATUS_LABELS[match.status]}
              </span>
            </div>
          </div>
        </div>

      {showActions && match.status === "PENDING" && !match.userAction && (
        <div className="flex gap-2 mt-4">
          <Button
            variant="outline"
            className="flex-1 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
            onClick={() => handleAction(match.id, "REJECT")}
          >
            <X className="h-4 w-4 mr-1" />
            Decline
          </Button>
          <Button
            className="flex-1 rounded-xl bg-green-600 hover:bg-green-700"
            onClick={() => handleAction(match.id, "ACCEPT")}
          >
            <Heart className="h-4 w-4 mr-1" />
            Accept
          </Button>
        </div>
      )}

      {(match.status === "MUTUAL" || match.status === "ACCEPTED" || match.isMutualMatch) && (
        <div className="mt-4 flex gap-2">
          <Link href={`/messages/${match.matchedUser?.id}`} className="flex-1">
            <Button className="w-full rounded-xl bg-blue-600 hover:bg-blue-700">
              <MessageCircle className="h-4 w-4 mr-2" />
              Message
            </Button>
          </Link>
          <Link href={`/messages/${match.matchedUser?.id}`} className="flex-1">
            <Button variant="outline" className="w-full rounded-xl border-emerald-200 text-emerald-700 hover:bg-emerald-50">
              <Home className="h-4 w-4 mr-2" />
              Share Room
            </Button>
          </Link>
        </div>
      )}
    </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Roommate Matching" />

      {/* Mutual Matches Tip Banner */}
      {mutualMatches.length > 0 && (
        <div className="mx-4 mt-4 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-2xl p-3 text-white">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-white/20 rounded-full flex items-center justify-center">
              <Home className="h-4 w-4" />
            </div>
            <div>
              <p className="font-medium text-sm">
                {mutualMatches.length} Roommate Match{mutualMatches.length > 1 ? "es" : ""}
              </p>
              <p className="text-xs text-white/80">Message your matches to share your room or find one together</p>
            </div>
          </div>
        </div>
      )}

      {/* Tabs + View Mode Toggle */}
      <div className="px-4 pt-4 pb-2">
        <div className="flex items-center justify-between mb-3">
          <div className="flex gap-2 overflow-x-auto flex-1">
            <button
              onClick={() => { setActiveTab("discover"); setCurrentSwipeIndex(0); }}
              className={`px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                activeTab === "discover"
                  ? "bg-blue-600 text-white"
                  : "bg-white text-slate-600 border border-slate-200"
              }`}
            >
              <Users className="h-3.5 w-3.5 inline mr-1" />
              Discover ({discoveredMatches.length})
            </button>
            <button
              onClick={() => setActiveTab("pending")}
              className={`px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                activeTab === "pending"
                  ? "bg-blue-600 text-white"
                  : "bg-white text-slate-600 border border-slate-200"
              }`}
            >
              <Clock className="h-3.5 w-3.5 inline mr-1" />
              Sent ({sentRequests.length})
            </button>
            <button
              onClick={() => setActiveTab("matched")}
              className={`px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                activeTab === "matched"
                  ? "bg-blue-600 text-white"
                  : "bg-white text-slate-600 border border-slate-200"
              }`}
            >
              <Heart className="h-3.5 w-3.5 inline mr-1" />
              Matched ({mutualMatches.length})
            </button>
          </div>
        </div>

        {/* View Mode Toggle - Only for Discover tab */}
        {activeTab === "discover" && discoveredMatches.length > 0 && (
          <div className="flex items-center justify-between">
            <p className="text-xs text-slate-500">
              {viewMode === "swipe" 
                ? `${currentSwipeIndex + 1} of ${discoveredMatches.length}` 
                : `${discoveredMatches.length} potential roommates`}
            </p>
            <div className="flex bg-slate-100 rounded-lg p-0.5">
              <button
                onClick={() => setViewMode("grid")}
                className={`p-1.5 rounded-md transition-colors ${
                  viewMode === "grid" ? "bg-white shadow-sm" : "text-slate-500"
                }`}
              >
                <LayoutGrid className="h-4 w-4" />
              </button>
              <button
                onClick={() => { setViewMode("swipe"); setCurrentSwipeIndex(0); }}
                className={`p-1.5 rounded-md transition-colors ${
                  viewMode === "swipe" ? "bg-white shadow-sm" : "text-slate-500"
                }`}
              >
                <Layers className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Content */}
      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto"
      >
        <PullToRefreshIndicator isRefreshing={isRefreshing} pullProgress={pullProgress} />

        <div className={`p-4 pb-24 ${activeTab === "discover" && viewMode === "swipe" ? "" : "space-y-3"}`}>
          {isLoading || isFinding ? (
            <>
              <div className="text-center py-4 text-slate-500 text-sm">
                {isFinding ? "Finding compatible roommates..." : "Loading matches..."}
              </div>
              {viewMode === "grid" ? (
                <div className="grid grid-cols-2 gap-3">
                  <Skeleton className="h-44 rounded-2xl" />
                  <Skeleton className="h-44 rounded-2xl" />
                  <Skeleton className="h-44 rounded-2xl" />
                  <Skeleton className="h-44 rounded-2xl" />
                </div>
              ) : (
                <Skeleton className="h-96 rounded-3xl" />
              )}
            </>
          ) : (
            <>
              {activeTab === "discover" && (
                <>
                  {discoveredMatches.length === 0 ? (
                    <div className="text-center py-12">
                      <div className="w-20 h-20 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                        <UserPlus className="h-10 w-10 text-blue-500" />
                      </div>
                      <h3 className="text-lg font-semibold text-slate-900 mb-2">
                        No New Matches
                      </h3>
                      <p className="text-slate-500 text-sm mb-4 max-w-xs mx-auto">
                        {matches.length === 0 
                          ? "Complete your preferences to find compatible roommates"
                          : "Check back later or update your preferences"}
                      </p>
                      <div className="flex gap-2 justify-center">
                        <Link href="/preferences">
                          <Button variant="outline" className="rounded-xl">
                            <Settings className="h-4 w-4 mr-2" />
                            Preferences
                          </Button>
                        </Link>
                        <Button 
                          onClick={findNewMatches}
                          disabled={isFinding}
                          className="rounded-xl bg-blue-600 hover:bg-blue-700"
                        >
                          <Sparkles className="h-4 w-4 mr-2" />
                          {isFinding ? "Searching..." : "Find Matches"}
                        </Button>
                      </div>
                    </div>
                  ) : viewMode === "grid" ? (
                    /* LinkedIn-style Grid View */
                    <div className="grid grid-cols-2 gap-3">
                      {discoveredMatches.map(match => renderGridCard(match))}
                    </div>
                  ) : (
                    /* Tinder-style Swipe View */
                    <div className="relative">
                      {currentSwipeIndex < discoveredMatches.length ? (
                        <>
                          {renderSwipeCard(discoveredMatches[currentSwipeIndex])}
                          
                          {/* Swipe Action Buttons */}
                          <div className="flex justify-center gap-6 mt-4">
                            <button
                              onClick={() => handleSwipeAction("REJECT")}
                              className="w-16 h-16 rounded-full bg-white shadow-lg flex items-center justify-center border-2 border-red-200 hover:border-red-400 hover:bg-red-50 transition-colors"
                            >
                              <X className="h-8 w-8 text-red-500" />
                            </button>
                            <button
                              onClick={resetSwipe}
                              className="w-12 h-12 rounded-full bg-white shadow-md flex items-center justify-center border border-slate-200 hover:bg-slate-50 transition-colors self-center"
                            >
                              <RotateCcw className="h-5 w-5 text-slate-400" />
                            </button>
                            <button
                              onClick={() => handleSwipeAction("ACCEPT")}
                              className="w-16 h-16 rounded-full bg-gradient-to-br from-green-500 to-emerald-600 shadow-lg flex items-center justify-center hover:from-green-600 hover:to-emerald-700 transition-colors"
                            >
                              <Heart className="h-8 w-8 text-white" />
                            </button>
                          </div>
                        </>
                      ) : (
                        <div className="text-center py-12">
                          <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <Check className="h-10 w-10 text-green-600" />
                          </div>
                          <h3 className="text-lg font-semibold text-slate-900 mb-2">
                            All Caught Up!
                          </h3>
                          <p className="text-slate-500 text-sm mb-4">
                            You've reviewed all potential matches
                          </p>
                          <Button 
                            onClick={() => { resetSwipe(); findNewMatches(); }}
                            className="rounded-xl bg-blue-600 hover:bg-blue-700"
                          >
                            <Sparkles className="h-4 w-4 mr-2" />
                            Find More
                          </Button>
                        </div>
                      )}
                    </div>
                  )}
                </>
              )}

              {activeTab === "pending" && (
                <>
                  {sentRequests.length === 0 ? (
                    <div className="text-center py-12">
                      <div className="w-20 h-20 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
                        <Clock className="h-10 w-10 text-amber-500" />
                      </div>
                      <h3 className="text-lg font-semibold text-slate-900 mb-2">
                        No Pending Requests
                      </h3>
                      <p className="text-slate-500 text-sm">
                        Connect with someone to see them here
                      </p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-2 gap-3">
                      {sentRequests.map(match => renderGridCard(match))}
                    </div>
                  )}
                </>
              )}

              {activeTab === "matched" && (
                <>
                  {mutualMatches.length === 0 ? (
                    <div className="text-center py-12">
                      <div className="w-20 h-20 bg-pink-100 rounded-full flex items-center justify-center mx-auto mb-4">
                        <Heart className="h-10 w-10 text-pink-500" />
                      </div>
                      <h3 className="text-lg font-semibold text-slate-900 mb-2">
                        No Matches Yet
                      </h3>
                      <p className="text-slate-500 text-sm max-w-xs mx-auto">
                        When you and another user both connect, you'll be matched!
                      </p>
                    </div>
                  ) : (
                    <>
                      <div className="grid grid-cols-2 gap-3">
                        {mutualMatches.map(match => renderGridCard(match))}
                      </div>
                      
                      {/* Quick Links */}
                      <div className="mt-6 space-y-3">
                        <Link href="/for-you">
                          <div className="flex items-center justify-between p-4 bg-white rounded-xl border border-slate-200 hover:border-blue-300 transition-colors">
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                                <Home className="h-5 w-5 text-green-600" />
                              </div>
                              <div>
                                <p className="font-medium text-slate-900">Browse Listings</p>
                                <p className="text-sm text-slate-500">Find a room to share with your match</p>
                              </div>
                            </div>
                            <ChevronRight className="h-5 w-5 text-slate-400" />
                          </div>
                        </Link>
                        
                        <Link href="/applications">
                          <div className="flex items-center justify-between p-4 bg-white rounded-xl border border-slate-200 hover:border-blue-300 transition-colors">
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                                <FileText className="h-5 w-5 text-blue-600" />
                              </div>
                              <div>
                                <p className="font-medium text-slate-900">My Applications</p>
                                <p className="text-sm text-slate-500">View your rental applications</p>
                              </div>
                            </div>
                            <ChevronRight className="h-5 w-5 text-slate-400" />
                          </div>
                        </Link>
                      </div>
                    </>
                  )}
                </>
              )}
            </>
          )}
        </div>
      </div>

      {/* Profile Detail Sheet */}
      <Sheet open={!!selectedMatch} onOpenChange={() => setSelectedMatch(null)}>
        <SheetContent side="bottom" className="h-[85vh] rounded-t-3xl overflow-y-auto">
          {selectedMatch && (
            <>
              <SheetHeader className="text-left">
                <SheetTitle className="sr-only">Profile Details</SheetTitle>
              </SheetHeader>
              
              {/* Profile Header */}
              <div className="flex flex-col items-center pt-2 pb-6">
                <Avatar className="h-24 w-24 ring-4 ring-white shadow-lg mb-4">
                  <AvatarImage src={selectedMatch.matchedUser.profilePhotoUrl || undefined} />
                  <AvatarFallback className="bg-blue-100 text-blue-600 text-3xl font-bold">
                    {selectedMatch.matchedUser.firstName?.[0]}{selectedMatch.matchedUser.lastName?.[0]}
                  </AvatarFallback>
                </Avatar>
                
                <h2 className="text-xl font-bold text-slate-900">
                  {selectedMatch.matchedUser.firstName} {selectedMatch.matchedUser.lastName}
                </h2>
                
                <div className={`mt-2 px-4 py-1.5 rounded-full ${getScoreBg(selectedMatch.compatibilityScore)} flex items-center gap-2`}>
                  <Sparkles className={`h-4 w-4 ${getScoreColor(selectedMatch.compatibilityScore)}`} />
                  <span className={`font-bold ${getScoreColor(selectedMatch.compatibilityScore)}`}>
                    {selectedMatch.compatibilityScore}% Compatible
                  </span>
                </div>
              </div>

              {/* Compatibility Breakdown */}
              <div className="bg-slate-50 rounded-2xl p-4 mb-4">
                <h3 className="font-semibold text-slate-900 mb-3 flex items-center gap-2">
                  <Star className="h-4 w-4 text-amber-500" />
                  Compatibility Breakdown
                </h3>
                <div className="space-y-3">
                  {[
                    { label: "Budget Match", score: selectedMatch.budgetScore, icon: "💰", desc: "Similar budget range" },
                    { label: "Lifestyle", score: selectedMatch.lifestyleScore, icon: "🎯", desc: "Compatible habits" },
                    { label: "Schedule", score: selectedMatch.scheduleScore, icon: "⏰", desc: "Similar routines" },
                    { label: "Location", score: selectedMatch.locationScore, icon: "📍", desc: "Preferred areas" },
                    { label: "Habits", score: selectedMatch.habitsScore, icon: "🏠", desc: "Living preferences" },
                  ].map((item) => item.score !== undefined && (
                    <div key={item.label}>
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-medium text-slate-700">{item.icon} {item.label}</span>
                        <span className={`text-sm font-bold ${getScoreColor(item.score)}`}>{item.score}%</span>
                      </div>
                      <div className="h-2 bg-slate-200 rounded-full overflow-hidden">
                        <div 
                          className={`h-full rounded-full transition-all ${
                            item.score >= 70 ? "bg-green-500" : 
                            item.score >= 50 ? "bg-blue-500" : "bg-amber-500"
                          }`}
                          style={{ width: `${item.score}%` }}
                        />
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">{item.desc}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Why You Match */}
              {selectedMatch.explanation && (
                <div className="bg-blue-50 rounded-2xl p-4 mb-4">
                  <h3 className="font-semibold text-blue-900 mb-2 flex items-center gap-2">
                    <Info className="h-4 w-4" />
                    Why You Match
                  </h3>
                  <p className="text-sm text-blue-800">{selectedMatch.explanation}</p>
                </div>
              )}

              {/* Action Buttons */}
              <div className="sticky bottom-0 bg-white pt-4 pb-6 border-t border-slate-100 -mx-6 px-6">
                {selectedMatch.status === "PENDING" && !selectedMatch.userAction && (
                  <div className="flex gap-3">
                    <Button
                      variant="outline"
                      className="flex-1 h-12 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
                      onClick={() => { handleAction(selectedMatch.id, "REJECT"); setSelectedMatch(null); }}
                    >
                      <X className="h-5 w-5 mr-2" />
                      Pass
                    </Button>
                    <Button
                      className="flex-1 h-12 rounded-xl bg-blue-600 hover:bg-blue-700"
                      onClick={() => { handleAction(selectedMatch.id, "ACCEPT"); setSelectedMatch(null); }}
                    >
                      <Heart className="h-5 w-5 mr-2" />
                      Connect
                    </Button>
                  </div>
                )}
                
                {(selectedMatch.status === "MUTUAL" || selectedMatch.status === "ACCEPTED" || selectedMatch.isMutualMatch) && (
                  <Link href={`/messages/${selectedMatch.matchedUser?.id}`}>
                    <Button className="w-full h-12 rounded-xl bg-green-600 hover:bg-green-700">
                      <MessageCircle className="h-5 w-5 mr-2" />
                      Send Message
                    </Button>
                  </Link>
                )}
                
                {selectedMatch.status === "PENDING" && selectedMatch.userAction === "ACCEPT" && (
                  <div className="text-center py-3 text-amber-600">
                    <Clock className="h-5 w-5 inline mr-2" />
                    Waiting for {selectedMatch.matchedUser.firstName} to respond
                  </div>
                )}
              </div>
            </>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
