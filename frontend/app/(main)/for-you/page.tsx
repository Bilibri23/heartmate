"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { ListingCard } from "@/components/cards/listing-card"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Sparkles, TrendingUp, Clock, Filter } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import api from "@/lib/api"

interface RecommendedListing {
  listingId: string
  title: string
  description: string
  rentAmount: number
  city: string
  neighborhood: string
  propertyType: string
  primaryPhotoUrl: string | null
  bedrooms: number
  bathrooms: number
  matchScore: number
  preferenceScore: number
  behaviorScore: number
  reasons: string[]
  isViewed: boolean
  isFavorited: boolean
  viewsCount: number
  verified: boolean
  featured: boolean
}

type FeedTab = "forYou" | "trending" | "recent"

export default function ForYouPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [activeTab, setActiveTab] = useState<FeedTab>("forYou")
  const [listings, setListings] = useState<RecommendedListing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchRecommendations = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    setError(null)
    
    try {
      let endpoint = "/listings/active"
      let useRecommendations = false
      
      if (activeTab === "forYou") {
        // Try recommendations first, fallback to active listings
        endpoint = "/recommendations/listings"
        useRecommendations = true
      } else if (activeTab === "trending") {
        endpoint = "/listings/featured"
      } else if (activeTab === "recent") {
        endpoint = "/listings?sortBy=createdAt&sortDir=DESC&size=20"
      }
      
      let response
      try {
        response = await api.get(endpoint)
      } catch (recErr) {
        // Fallback to active listings if recommendations fail
        if (useRecommendations) {
          console.log("Recommendations unavailable, falling back to active listings")
          response = await api.get("/listings/active", { params: { size: 20 } })
          useRecommendations = false
        } else {
          throw recErr
        }
      }
      
      // Normalize response based on endpoint
      if (useRecommendations) {
        // Recommendations endpoint returns properly formatted data
        setListings(response.data || [])
      } else {
        // All other endpoints need normalization (paginated or array)
        const content = response.data?.content || response.data || []
        setListings(content.map((l: any) => ({
          listingId: l.id || l.listingId,
          title: l.title,
          rentAmount: l.rentAmount,
          city: l.city,
          neighborhood: l.neighborhood,
          primaryPhotoUrl: l.photos?.[0]?.photoUrl || l.primaryPhotoUrl || null,
          bedrooms: l.bedrooms,
          bathrooms: l.bathrooms,
          verified: l.verified,
          featured: l.featured,
          isFavorited: l.isFavorited || false,
          matchScore: l.matchScore || 0,
        })))
      }
    } catch (err: any) {
      console.error("Failed to fetch recommendations:", err)
      setError(err.message || "Failed to load recommendations")
    } finally {
      setIsLoading(false)
    }
  }, [user?.id, activeTab])

  useEffect(() => {
    fetchRecommendations()
  }, [fetchRecommendations])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchRecommendations,
  })

  const handleFavoriteToggle = async (listingId: string) => {
    if (!user?.id) return
    try {
      await api.post(`/listings/${listingId}/favorite`, null, {
        params: { userId: user.id }
      })
    } catch (err) {
      console.error("Failed to toggle favorite:", err)
    }
  }

  const tabs = [
    { id: "forYou" as FeedTab, label: t.nav.forYou, icon: Sparkles },
    { id: "trending" as FeedTab, label: "Trending", icon: TrendingUp },
    { id: "recent" as FeedTab, label: "Recent", icon: Clock },
  ]

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.nav.forYou} />
      
      {/* Tabs */}
      <div className="sticky top-14 z-30 bg-white border-b border-slate-200">
        <div className="flex px-4 gap-2 py-2 overflow-x-auto scrollbar-hide">
          {tabs.map((tab) => {
            const Icon = tab.icon
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                  activeTab === tab.id
                    ? "bg-blue-600 text-white"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                <Icon className="h-4 w-4" />
                {tab.label}
              </button>
            )
          })}
          <Button variant="outline" size="sm" className="rounded-full ml-auto">
            <Filter className="h-4 w-4 mr-1" />
            {t.common.filter}
          </Button>
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

        <div className="p-4 space-y-4">
          {/* AI Insight Banner (only for "For You" tab) */}
          {activeTab === "forYou" && !isLoading && listings.length > 0 && (
            <div className="bg-gradient-to-r from-blue-500 to-purple-600 rounded-2xl p-4 text-white">
              <div className="flex items-center gap-2 mb-2">
                <Sparkles className="h-5 w-5" />
                <span className="font-semibold">AI Recommendations</span>
              </div>
              <p className="text-sm text-blue-100">
                Based on your preferences and browsing history, we found {listings.length} listings that match your style.
              </p>
            </div>
          )}

          {/* Loading State */}
          {isLoading && (
            <div className="grid gap-4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="rounded-2xl bg-white p-3 shadow-sm">
                  <Skeleton className="aspect-[4/3] rounded-xl mb-3" />
                  <Skeleton className="h-5 w-24 mb-2" />
                  <Skeleton className="h-4 w-full mb-2" />
                  <Skeleton className="h-4 w-32" />
                </div>
              ))}
            </div>
          )}

          {/* Error State */}
          {error && !isLoading && (
            <div className="text-center py-12">
              <p className="text-slate-500 mb-4">{error}</p>
              <Button onClick={fetchRecommendations}>
                {t.common.retry}
              </Button>
            </div>
          )}

          {/* Empty State */}
          {!isLoading && !error && listings.length === 0 && (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">🏠</div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                No recommendations yet
              </h3>
              <p className="text-slate-500 text-sm">
                Complete your profile and preferences to get personalized recommendations
              </p>
            </div>
          )}

          {/* Listings Grid */}
          {!isLoading && !error && listings.length > 0 && (
            <div className="grid gap-4 sm:grid-cols-2">
              {listings.map((listing) => (
                <ListingCard
                  key={listing.listingId}
                  id={listing.listingId}
                  title={listing.title}
                  price={listing.rentAmount}
                  city={listing.city}
                  neighborhood={listing.neighborhood}
                  bedrooms={listing.bedrooms}
                  bathrooms={listing.bathrooms}
                  imageUrl={listing.primaryPhotoUrl || undefined}
                  isVerified={listing.verified}
                  isFeatured={listing.featured}
                  isFavorited={listing.isFavorited}
                  matchScore={listing.matchScore}
                  onFavoriteToggle={handleFavoriteToggle}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
