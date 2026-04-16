"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { ListingCard } from "@/components/cards/listing-card"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Heart } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import Link from "next/link"
import api from "@/lib/api"

interface Listing {
  id: string
  title: string
  rentAmount: number
  city: string
  neighborhood: string
  photos: { photoUrl: string; isPrimary: boolean }[]
  bedrooms: number
  bathrooms: number
  verified: boolean
  trustTier?: string | null
  featured: boolean
}

export default function FavoritesPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [favorites, setFavorites] = useState<Listing[]>([])
  const [isLoading, setIsLoading] = useState(true)

  const fetchFavorites = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await api.get("/listings/favorites")
      setFavorites(response.data || [])
    } catch (err) {
      console.error("Failed to fetch favorites:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchFavorites()
  }, [fetchFavorites])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchFavorites,
  })

  const handleFavoriteToggle = async (listingId: string) => {
    if (!user?.id) return
    try {
      await api.post(`/listings/${listingId}/favorite`, null, {
        params: { userId: user.id }
      })
      // Remove from list
      setFavorites(prev => prev.filter(l => l.id !== listingId))
    } catch (err) {
      console.error("Failed to toggle favorite:", err)
    }
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.nav.favorites} />

      {/* Content */}
      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        <div className="p-4">
          {/* Loading */}
          {isLoading && (
            <div className="grid gap-4 sm:grid-cols-2">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="rounded-2xl bg-white p-3 shadow-sm">
                  <Skeleton className="aspect-[4/3] rounded-xl mb-3" />
                  <Skeleton className="h-5 w-24 mb-2" />
                  <Skeleton className="h-4 w-full mb-2" />
                  <Skeleton className="h-4 w-32" />
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {!isLoading && favorites.length === 0 && (
            <div className="text-center py-12">
              <div className="mx-auto mb-4 h-20 w-20 rounded-full bg-red-100 flex items-center justify-center">
                <Heart className="h-10 w-10 text-red-400" />
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                No favorites yet
              </h3>
              <p className="text-slate-500 text-sm mb-4">
                Save listings you like to find them easily later
              </p>
              <Link href="/search">
                <Button>{t.nav.search}</Button>
              </Link>
            </div>
          )}

          {/* Favorites Grid */}
          {!isLoading && favorites.length > 0 && (
            <div className="grid gap-4 sm:grid-cols-2">
              {favorites.map((listing) => (
                <ListingCard
                  key={listing.id}
                  id={listing.id}
                  title={listing.title}
                  price={listing.rentAmount}
                  city={listing.city}
                  neighborhood={listing.neighborhood}
                  bedrooms={listing.bedrooms}
                  bathrooms={listing.bathrooms}
                  imageUrl={listing.photos?.find(p => p.isPrimary)?.photoUrl || listing.photos?.[0]?.photoUrl}
                  isVerified={listing.verified}
                  trustTier={listing.trustTier}
                  isFeatured={listing.featured}
                  isFavorited={true}
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
