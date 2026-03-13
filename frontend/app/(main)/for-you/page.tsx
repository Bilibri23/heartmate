"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { ListingCard } from "@/components/cards/listing-card"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { QuickTour } from "@/components/ui/quick-tour"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Clock, Filter, Flame, Star } from "lucide-react"
import Link from "next/link"
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
  averageRating?: number
  reviewCount?: number
  status?: string | null
  isAvailable?: boolean | null
}

export default function ForYouPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [listings, setListings] = useState<RecommendedListing[]>([])
  const [trendingListings, setTrendingListings] = useState<RecommendedListing[]>([])
  const [recentListings, setRecentListings] = useState<RecommendedListing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const normalizeListing = (l: any): RecommendedListing => ({
    listingId: l.id || l.listingId,
    title: l.title,
    description: l.description || "",
    rentAmount: l.rentAmount,
    city: l.city,
    neighborhood: l.neighborhood,
    propertyType: l.propertyType || "",
    primaryPhotoUrl: l.photos?.[0]?.photoUrl || l.primaryPhotoUrl || null,
    bedrooms: l.bedrooms,
    bathrooms: l.bathrooms,
    matchScore: l.matchScore ?? l.totalScore ?? l.compatibilityScore ?? 0,
    preferenceScore: l.preferenceScore ?? 0,
    behaviorScore: l.behaviorScore ?? 0,
    reasons: l.reasons || [],
    isViewed: l.isViewed ?? false,
    isFavorited: l.isFavorited ?? false,
    viewsCount: l.viewsCount ?? 0,
    verified: l.verified,
    featured: l.featured,
    averageRating: l.averageRating ?? null,
    reviewCount: l.reviewCount ?? 0,
    status: l.status ?? null,
    isAvailable: l.isAvailable ?? (l.status ? l.status === "ACTIVE" : null),
  })

  // Fetch all sections on initial load (Netflix-style: all rows in parallel)
  const fetchAllSections = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    try {
      const [recResponse, activeListingsResponse] = await Promise.allSettled([
        user?.id ? api.get("/recommendations/listings") : Promise.reject(),
        api.get("/listings/active", { params: user?.id ? { userId: user.id } : {} }),
      ])

      // For You: recommendations (if user) or ES forYou mode
      if (recResponse.status === "fulfilled") {
        const recData = Array.isArray(recResponse.value.data) ? recResponse.value.data : []
        if (recData.length > 0) {
          setListings(recData.map(normalizeListing))
        } else {
          const forYouRes = await api.get("/search", { params: { mode: "forYou", size: 12, userId: user?.id } })
          const content = forYouRes.data?.content ?? forYouRes.data ?? []
          setListings(content.map(normalizeListing))
        }
      } else {
        const forYouRes = await api.get("/search", { params: { mode: "forYou", size: 12, userId: user?.id } })
        const content = forYouRes.data?.content ?? forYouRes.data ?? []
        setListings(content.map(normalizeListing))
      }

      // Trending & Recent: fetch in parallel
      const [trendingRes, recentRes] = await Promise.all([
        api.get("/search", { params: { mode: "trending", size: 12, userId: user?.id } }),
        api.get("/search", { params: { mode: "recent", size: 12, userId: user?.id } }),
      ])
      const trendingContent = trendingRes.data?.content ?? trendingRes.data ?? []
      const recentContent = recentRes.data?.content ?? recentRes.data ?? []
      setTrendingListings(trendingContent.map(normalizeListing))
      setRecentListings(recentContent.map(normalizeListing))
    } catch (err: any) {
      console.error("Failed to fetch listings:", err)
      setError(err.message || "Failed to load listings")
      // Fallback to active listings
      try {
        const res = await api.get("/listings/active", { params: user?.id ? { userId: user.id } : {} })
        const all = res.data || []
        const trending = [...all].sort((a, b) => (b.viewsCount || 0) - (a.viewsCount || 0)).slice(0, 12).map(normalizeListing)
        const recent = [...all].sort((a, b) => (new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())).slice(0, 12).map(normalizeListing)
        setListings(all.slice(0, 12).map(normalizeListing))
        setTrendingListings(trending)
        setRecentListings(recent)
      } catch { /* ignore */ }
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchAllSections()
  }, [fetchAllSections])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchAllSections,
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

  const ListingRow = ({ title, subtitle, icon: Icon, iconBg, listings, emptyMsg, showRank }: {
    title: string; subtitle?: string; icon: React.ElementType; iconBg: string
    listings: RecommendedListing[]; emptyMsg: string; showRank?: boolean
  }) => (
    <section className="mb-8">
      <div className="flex items-center justify-between px-4 mb-3">
        <div className="flex items-center gap-2">
          <div className={`p-1.5 ${iconBg} rounded-lg`}>
            <Icon className="h-5 w-5 text-current" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-900">{title}</h2>
            {subtitle && <p className="text-xs text-slate-500">{subtitle}</p>}
          </div>
        </div>
      </div>
      {listings.length > 0 ? (
        <div className="flex gap-4 overflow-x-auto pb-4 px-4 scrollbar-hide snap-x snap-mandatory">
          {listings.map((listing, index) => (
            <div key={listing.listingId} className="flex-shrink-0 w-72 snap-start relative">
              {showRank && index < 3 && (
                <div className="absolute -top-2 -left-2 z-10 w-8 h-8 bg-gradient-to-br from-orange-400 to-red-500 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-lg">
                  {index + 1}
                </div>
              )}
              <ListingCard
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
                rating={listing.averageRating}
                status={listing.status}
                isAvailable={listing.isAvailable}
                onFavoriteToggle={handleFavoriteToggle}
              />
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-6 px-4">
          <p className="text-slate-500 text-sm">{emptyMsg}</p>
        </div>
      )}
    </section>
  )

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <QuickTour role="STUDENT" storageKey="roombuddy_student_tour" />
      <MobileHeader title={t.nav.forYou} />

      {/* Filter bar */}
      <div className="sticky top-14 z-30 bg-white/95 backdrop-blur border-b border-slate-200">
        <div className="flex px-4 py-2">
          <Link href="/search" className="flex items-center gap-2 px-4 py-2 rounded-full bg-slate-100 text-slate-600 hover:bg-slate-200 text-sm font-medium transition-colors">
            <Filter className="h-4 w-4" />
            {t.common.filter}
          </Link>
        </div>
      </div>

      <div ref={containerRef} className="flex-1 overflow-y-auto relative">
        <PullToRefreshIndicator pullProgress={pullProgress} isRefreshing={isRefreshing} />

        <div className="py-4">
          {isLoading && (
            <div className="px-4 space-y-8">
              {[1, 2, 3].map((s) => (
                <div key={s}>
                  <Skeleton className="h-6 w-40 mb-3" />
                  <div className="flex gap-4 overflow-hidden">
                    {[1, 2, 3].map((i) => (
                      <div key={i} className="flex-shrink-0 w-72 rounded-2xl bg-white p-3 shadow-sm">
                        <Skeleton className="aspect-[4/3] rounded-xl mb-3" />
                        <Skeleton className="h-5 w-24 mb-2" />
                        <Skeleton className="h-4 w-full mb-2" />
                        <Skeleton className="h-4 w-32" />
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          {error && !isLoading && (
            <div className="text-center py-12 px-4">
              <p className="text-slate-500 mb-4">{error}</p>
              <Button onClick={fetchAllSections}>{t.common.retry}</Button>
            </div>
          )}

          {!isLoading && !error && (
            <>
              {/* Row 1: For You */}
              <ListingRow
                title={t.nav.forYou}
                subtitle={user ? "Personalized for you" : "Featured & verified picks"}
                icon={Star}
                iconBg="bg-amber-100 text-amber-600"
                listings={listings}
                emptyMsg={user ? "Complete your preferences for personalized picks" : "No listings yet"}
              />

              {/* Row 2: Trending */}
              <ListingRow
                title="Hot Right Now"
                subtitle="Most viewed by students"
                icon={Flame}
                iconBg="bg-orange-100 text-orange-500"
                listings={trendingListings}
                emptyMsg="No trending listings yet"
                showRank
              />

              {/* Row 3: Recent */}
              <ListingRow
                title="Just Listed"
                subtitle="Fresh listings added recently"
                icon={Clock}
                iconBg="bg-green-100 text-green-600"
                listings={recentListings}
                emptyMsg="No recent listings"
              />

              {/* View all CTA */}
              <div className="px-4 pt-4 pb-8">
                <Link href="/search">
                  <Button variant="outline" className="w-full rounded-xl">
                    Browse All Listings
                  </Button>
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
