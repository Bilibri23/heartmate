"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { ListingCard } from "@/components/cards/listing-card"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Clock, Filter, Flame, Star, WifiOff } from "lucide-react"
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
  recommendationReasonCodes: string[]
  isViewed: boolean
  isFavorited: boolean
  viewsCount: number
  verified: boolean
  trustTier?: string | null
  featured: boolean
  averageRating?: number
  reviewCount?: number
  status?: string | null
  isAvailable?: boolean | null
}

export default function ForYouPage() {
  const { t, language } = useLanguage()
  const { user } = useAuth()
  const [listings, setListings] = useState<RecommendedListing[]>([])
  const [trendingListings, setTrendingListings] = useState<RecommendedListing[]>([])
  const [recentListings, setRecentListings] = useState<RecommendedListing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const normalizeListing = (l: Record<string, unknown>, defaultReasonCodes?: string[]): RecommendedListing => {
    const fromApi = l.recommendationReasonCodes as string[] | undefined
    const codes =
      fromApi && fromApi.length > 0
        ? fromApi
        : defaultReasonCodes && defaultReasonCodes.length > 0
          ? defaultReasonCodes
          : []
    const photos = l.photos as { photoUrl?: string }[] | undefined
    const firstPhoto = photos?.[0]?.photoUrl
    const statusStr = l.status != null ? String(l.status) : null

    return {
      listingId: String(l.id ?? l.listingId ?? ""),
      title: String(l.title ?? ""),
      description: String(l.description ?? ""),
      rentAmount: Number(l.rentAmount ?? 0),
      city: String(l.city ?? ""),
      neighborhood: String(l.neighborhood ?? ""),
      propertyType: String(l.propertyType ?? ""),
      primaryPhotoUrl: firstPhoto || (l.primaryPhotoUrl as string) || null,
      bedrooms: Number(l.bedrooms ?? 0),
      bathrooms: Number(l.bathrooms ?? 0),
      matchScore: Number(l.matchScore ?? l.totalScore ?? l.compatibilityScore ?? 0),
      preferenceScore: Number(l.preferenceScore ?? 0),
      behaviorScore: Number(l.behaviorScore ?? 0),
      reasons: (l.reasons as string[]) || [],
      recommendationReasonCodes: codes,
      isViewed: Boolean(l.isViewed),
      isFavorited: Boolean(l.isFavorited),
      viewsCount: Number(l.viewsCount ?? 0),
      verified: Boolean(l.verified),
      trustTier: (l.trustTier as string) ?? null,
      featured: Boolean(l.featured),
      averageRating: l.averageRating != null ? Number(l.averageRating) : undefined,
      reviewCount: Number(l.reviewCount ?? 0),
      status: statusStr,
      isAvailable:
        l.isAvailable != null
          ? Boolean(l.isAvailable)
          : statusStr
            ? statusStr === "ACTIVE"
            : undefined,
    }
  }

  // Single compound feed (GET /api/feed) — replaces multiple /search + /recommendations round-trips
  const fetchAllSections = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    try {
      const res = await api.get("/feed", {
        params: {
          sections: "forYou,trending,recent",
          size: 12,
          lang: language === "fr" ? "fr" : "en",
        },
      })
      const data = res.data || {}
      const forYouItems: Record<string, unknown>[] = data.forYou?.items ?? []
      const trendingItems: Record<string, unknown>[] = data.trending?.items ?? []
      const recentItems: Record<string, unknown>[] = data.recent?.items ?? []
      setListings(forYouItems.map((l) => normalizeListing(l)))
      setTrendingListings(trendingItems.map((l) => normalizeListing(l, ["TRENDING_VIEWS"])))
      setRecentListings(recentItems.map((l) => normalizeListing(l, ["NEW_LISTING"])))
    } catch (err: unknown) {
      console.error("Failed to fetch listings:", err)
      // Detect HTTP status for user-friendly messaging
      const status = (err as { response?: { status?: number } })?.response?.status
      const friendlyError =
        status === 503 || status === 502
          ? "Our server is warming up — listings will appear shortly. Pull down to retry."
          : status === 401
            ? "Please sign in again to view listings."
            : "Couldn't load listings right now. Check your connection and try again."
      setError(friendlyError)
      // Try the simple fallback endpoint — if it succeeds, clear the error banner
      try {
        const res = await api.get("/listings/active", { params: user?.id ? { userId: user.id } : {} })
        const all: Record<string, unknown>[] = res.data || []
        if (all.length > 0) {
          const trending = [...all].sort((a, b) => ((b.viewsCount as number) || 0) - ((a.viewsCount as number) || 0)).slice(0, 12)
          const recent = [...all].sort((a, b) => (new Date((b.createdAt as string) || 0).getTime() - new Date((a.createdAt as string) || 0).getTime())).slice(0, 12)
          setListings(all.slice(0, 12).map((l) => normalizeListing(l, ["BROWSE_POPULAR"])))
          setTrendingListings(trending.map((l) => normalizeListing(l, ["TRENDING_VIEWS"])))
          setRecentListings(recent.map((l) => normalizeListing(l, ["NEW_LISTING"])))
          setError(null) // fallback succeeded — hide the error banner
        }
      } catch { /* ignore fallback failure — keep friendly error message */ }
    } finally {
      setIsLoading(false)
    }
  }, [user?.id, language])

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

  const ListingRow = ({ title, subtitle, icon: Icon, iconBg, listings, emptyMsg, showRank, sectionId }: {
    title: string; subtitle?: string; icon: React.ElementType; iconBg: string
    listings: RecommendedListing[]; emptyMsg: string; showRank?: boolean
    sectionId: string
  }) => (
    <section className="mb-8" role="region" aria-labelledby={sectionId}>
      <div className="flex items-center justify-between px-4 mb-3">
        <div className="flex items-center gap-2">
          <div className={`p-1.5 ${iconBg} rounded-lg`}>
            <Icon className="h-5 w-5 text-current" />
          </div>
          <div>
            <h2 id={sectionId} className="text-lg font-bold text-slate-900">{title}</h2>
            {subtitle && <p className="text-xs text-slate-500">{subtitle}</p>}
          </div>
        </div>
      </div>
      {listings.length > 0 ? (
        <div
          className="flex gap-4 overflow-x-auto pb-4 px-4 scrollbar-hide snap-x snap-mandatory motion-reduce:scroll-auto"
          tabIndex={0}
          aria-label={title}
        >
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
                trustTier={listing.trustTier}
                isFeatured={listing.featured}
                isFavorited={listing.isFavorited}
                matchScore={listing.matchScore}
                rating={listing.averageRating}
                status={listing.status}
                isAvailable={listing.isAvailable}
                reasonCodes={listing.recommendationReasonCodes}
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
      <div data-tour="tenant-welcome">
        <MobileHeader title={t.nav.discoverHome} />
      </div>

      {/* Filter bar */}
      <div
        data-tour="tenant-filter-bar"
        className="sticky top-14 z-30 bg-white/95 backdrop-blur border-b border-slate-200"
      >
        <div className="flex px-4 py-2 gap-2 items-center flex-wrap">
          <Link href="/search" className="flex items-center gap-2 px-4 py-2 rounded-full bg-slate-100 text-slate-600 hover:bg-slate-200 text-sm font-medium transition-colors">
            <Filter className="h-4 w-4" />
            {t.common.filter}
          </Link>
          <Link
            href="/matches"
            className="flex items-center gap-2 px-4 py-2 rounded-full bg-violet-50 text-violet-700 hover:bg-violet-100 text-sm font-medium transition-colors border border-violet-100"
          >
            {t.nav.roommates}
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
            <div className="mx-4 mt-4 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4">
              <WifiOff className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-amber-900">{error}</p>
              </div>
              <Button
                size="sm"
                variant="outline"
                className="shrink-0 border-amber-300 text-amber-800 hover:bg-amber-100"
                onClick={fetchAllSections}
              >
                {t.common.retry}
              </Button>
            </div>
          )}

          {!isLoading && (listings.length > 0 || trendingListings.length > 0 || recentListings.length > 0) && (
            <>
              {/* Row 1: For You */}
              <div data-tour="tenant-feed">
                <ListingRow
                  title={t.nav.discoverHome}
                  subtitle={user ? t.discovery.forYouPersonalized : t.discovery.forYouGuest}
                  icon={Star}
                  iconBg="bg-amber-100 text-amber-600"
                  listings={listings}
                  emptyMsg={user ? t.discovery.forYouEmptyPrefs : t.discovery.forYouEmptyList}
                  sectionId="feed-for-you-heading"
                />
              </div>

              {/* Row 2: Trending */}
              <ListingRow
                title={t.discovery.hotRightNow}
                subtitle={t.discovery.trendingSubtitle}
                icon={Flame}
                iconBg="bg-orange-100 text-orange-500"
                listings={trendingListings}
                emptyMsg={t.discovery.noTrending}
                showRank
                sectionId="feed-trending-heading"
              />

              {/* Row 3: Recent */}
              <ListingRow
                title={t.discovery.justListed}
                subtitle={t.discovery.recentSubtitle}
                icon={Clock}
                iconBg="bg-green-100 text-green-600"
                listings={recentListings}
                emptyMsg={t.discovery.noRecent}
                sectionId="feed-recent-heading"
              />

              {/* View all CTA */}
              <div className="px-4 pt-4 pb-8">
                <Link href="/search">
                  <Button variant="outline" className="w-full rounded-xl">
                    {t.discovery.browseAllListings}
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
