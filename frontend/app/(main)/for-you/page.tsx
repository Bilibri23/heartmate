"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { ListingCard } from "@/components/cards/listing-card"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { QuickTour } from "@/components/ui/quick-tour"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Sparkles, TrendingUp, Clock, Filter, ChevronLeft, ChevronRight, Flame, Eye, Star } from "lucide-react"
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

type FeedTab = "forYou" | "trending" | "recent"

export default function ForYouPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [activeTab, setActiveTab] = useState<FeedTab>("forYou")
  const [listings, setListings] = useState<RecommendedListing[]>([])
  const [trendingListings, setTrendingListings] = useState<RecommendedListing[]>([])
  const [recentListings, setRecentListings] = useState<RecommendedListing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const PAGE_SIZE = 10

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

  // Fetch all sections on initial load
  const fetchAllSections = useCallback(async () => {
    if (!user?.id) return

    setIsLoading(true)
    setError(null)

    try {
      // Fetch all three sections in parallel
      const [recResponse, activeListingsResponse] = await Promise.allSettled([
        api.get("/recommendations/listings"),
        api.get("/listings/active", { params: user?.id ? { userId: user.id } : {} }),
      ])

      // Process recommendations
      if (recResponse.status === "fulfilled") {
        const recData = Array.isArray(recResponse.value.data) ? recResponse.value.data : []
        if (recData.length > 0) {
          setListings(recData.map(normalizeListing))
        } else {
          // Fallback to active listings
          if (activeListingsResponse.status === "fulfilled") {
            const fallbackData = activeListingsResponse.value.data || []
            setListings(fallbackData.slice(0, 20).map(normalizeListing))
          }
        }
      } else {
        // Fallback on error
        if (activeListingsResponse.status === "fulfilled") {
          const fallbackData = activeListingsResponse.value.data || []
          setListings(fallbackData.slice(0, 20).map(normalizeListing))
        }
      }

      // Process trending and recent from active listings
      if (activeListingsResponse.status === "fulfilled") {
        const allListings = activeListingsResponse.value.data || []

        // Trending: Sort by views count, take top 15
        const trending = [...allListings]
          .sort((a, b) => (b.viewsCount || 0) - (a.viewsCount || 0))
          .slice(0, 15)
          .map(normalizeListing)
        setTrendingListings(trending)

        // Recent: Sort by created date, take top 15
        const recent = [...allListings]
          .sort((a, b) => {
            const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
            const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
            return dateB - dateA
          })
          .slice(0, 15)
          .map(normalizeListing)
        setRecentListings(recent)
        setTotalPages(1)
        setCurrentPage(0)
      }
    } catch (err: any) {
      console.error("Failed to fetch listings:", err)
      setError(err.message || "Failed to load listings")
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  // Fetch more recent listings (client-side pagination)
  const fetchMoreRecent = useCallback(async (page: number) => {
    if (!user?.id) return

    try {
      const response = await api.get("/listings/active", {
        params: user?.id ? { userId: user.id } : {}
      })
      const allListings = response.data || []

      // Client-side sorting and pagination
      const sorted = [...allListings]
        .sort((a, b) => {
          const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0
          const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0
          return dateB - dateA
        })

      const totalPages = Math.ceil(sorted.length / PAGE_SIZE)
      const start = page * PAGE_SIZE
      const end = start + PAGE_SIZE
      const paginated = sorted.slice(start, end).map(normalizeListing)

      setRecentListings(paginated)
      setTotalPages(totalPages)
      setCurrentPage(page)
    } catch (err) {
      console.error("Failed to fetch more listings:", err)
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

  const tabs = [
    { id: "forYou" as FeedTab, label: t.nav.forYou, icon: Sparkles },
    { id: "trending" as FeedTab, label: "Trending", icon: TrendingUp },
    { id: "recent" as FeedTab, label: "Recent", icon: Clock },
  ]

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <QuickTour role="STUDENT" storageKey="roombuddy_student_tour" />
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
                className={`flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${activeTab === tab.id
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

        <div className="py-4 space-y-6">
          {/* Loading State */}
          {isLoading && (
            <div className="px-4 space-y-6">
              {[1, 2].map((section) => (
                <div key={section}>
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

          {/* Error State */}
          {error && !isLoading && (
            <div className="text-center py-12 px-4">
              <p className="text-slate-500 mb-4">{error}</p>
              <Button onClick={fetchAllSections}>
                {t.common.retry}
              </Button>
            </div>
          )}

          {/* Main Content - Show all sections */}
          {!isLoading && !error && (
            <>
              {/* ===== FOR YOU SECTION ===== */}
              {activeTab === "forYou" && (
                <>
                  {/* Recommended Listings */}
                  {listings.length > 0 ? (
                    <div>
                      <div className="flex items-center justify-between px-4 mb-3">
                        <div className="flex items-center gap-2">
                          <Star className="h-5 w-5 text-amber-500" />
                          <h2 className="text-lg font-bold text-slate-900">Top Matches</h2>
                        </div>
                        <span className="text-sm text-slate-500">{listings.length} found</span>
                      </div>
                      <div className="px-4">
                        <div className="grid gap-4 sm:grid-cols-2">
                          {listings.slice(0, 6).map((listing) => (
                            <div key={listing.listingId} className="relative">
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
                                status={listing.status}
                                isAvailable={listing.isAvailable}
                                rating={listing.averageRating}
                                onFavoriteToggle={handleFavoriteToggle}
                              />

                              {/* Match Reasons — compact, below the card */}
                              {listing.reasons && listing.reasons.length > 0 && listing.reasons[0] && (
                                <div className="mt-1.5 px-1">
                                  <p className="text-xs text-slate-500 flex items-center gap-1 line-clamp-1">
                                    <Sparkles className="h-3 w-3 text-blue-500 flex-shrink-0" />
                                    {listing.reasons[0]}
                                  </p>
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                        {listings.length > 6 && (
                          <Button variant="outline" className="w-full mt-4 rounded-xl" onClick={() => setActiveTab("recent")}>
                            View All Listings
                          </Button>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div className="text-center py-12 px-4">
                      <div className="text-6xl mb-4">🏠</div>
                      <h3 className="text-lg font-semibold text-slate-900 mb-2">
                        No recommendations yet
                      </h3>
                      <p className="text-slate-500 text-sm mb-4">
                        Complete your profile and preferences to get personalized recommendations
                      </p>
                      <Button onClick={() => window.location.href = "/preferences"}>
                        Set Preferences
                      </Button>
                    </div>
                  )}
                </>
              )}

              {/* ===== TRENDING SECTION ===== */}
              {activeTab === "trending" && (
                <div>
                  {/* Section Header */}
                  <div className="flex items-center justify-between px-4 mb-3">
                    <div className="flex items-center gap-2">
                      <div className="p-1.5 bg-orange-100 rounded-lg">
                        <Flame className="h-5 w-5 text-orange-500" />
                      </div>
                      <div>
                        <h2 className="text-lg font-bold text-slate-900">Hot Right Now</h2>
                        <p className="text-xs text-slate-500">Most viewed by students this week</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1 text-sm text-slate-500">
                      <Eye className="h-4 w-4" />
                      <span>Popular</span>
                    </div>
                  </div>

                  {/* Horizontal Scroll */}
                  {trendingListings.length > 0 ? (
                    <div className="relative">
                      <div className="flex gap-4 overflow-x-auto pb-4 px-4 scrollbar-hide snap-x snap-mandatory">
                        {trendingListings.map((listing, index) => (
                          <div key={listing.listingId} className="flex-shrink-0 w-72 snap-start relative">
                            {index < 3 && (
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
                    </div>
                  ) : (
                    <div className="text-center py-8 px-4">
                      <p className="text-slate-500">No trending listings yet</p>
                    </div>
                  )}
                </div>
              )}

              {/* ===== RECENT SECTION ===== */}
              {activeTab === "recent" && (
                <div>
                  {/* Section Header */}
                  <div className="flex items-center justify-between px-4 mb-3">
                    <div className="flex items-center gap-2">
                      <div className="p-1.5 bg-green-100 rounded-lg">
                        <Clock className="h-5 w-5 text-green-600" />
                      </div>
                      <div>
                        <h2 className="text-lg font-bold text-slate-900">Just Listed</h2>
                        <p className="text-xs text-slate-500">Fresh listings added recently</p>
                      </div>
                    </div>
                  </div>

                  {/* Horizontal Scroll for Recent */}
                  {recentListings.length > 0 ? (
                    <>
                      <div className="relative">
                        <div className="flex gap-4 overflow-x-auto pb-4 px-4 scrollbar-hide snap-x snap-mandatory">
                          {recentListings.map((listing) => (
                            <div key={listing.listingId} className="flex-shrink-0 w-72 snap-start">
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
                      </div>

                      {/* Pagination Controls */}
                      {totalPages > 1 && (
                        <div className="flex items-center justify-center gap-4 px-4 py-4">
                          <Button
                            variant="outline"
                            size="sm"
                            className="rounded-full"
                            disabled={currentPage === 0}
                            onClick={() => fetchMoreRecent(currentPage - 1)}
                          >
                            <ChevronLeft className="h-4 w-4 mr-1" />
                            Previous
                          </Button>
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-medium text-slate-700">
                              Page {currentPage + 1} of {totalPages}
                            </span>
                          </div>
                          <Button
                            variant="outline"
                            size="sm"
                            className="rounded-full"
                            disabled={currentPage >= totalPages - 1}
                            onClick={() => fetchMoreRecent(currentPage + 1)}
                          >
                            Next
                            <ChevronRight className="h-4 w-4 ml-1" />
                          </Button>
                        </div>
                      )}
                    </>
                  ) : (
                    <div className="text-center py-8 px-4">
                      <p className="text-slate-500">No recent listings</p>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
