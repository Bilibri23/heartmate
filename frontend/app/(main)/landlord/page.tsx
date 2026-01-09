"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  Plus, 
  Home, 
  Users, 
  FileText, 
  TrendingUp,
  Eye,
  Heart,
  ChevronRight,
  MoreVertical
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import Link from "next/link"
import api from "@/lib/api"

interface LandlordStats {
  totalListings: number
  activeListings: number
  totalApplications: number
  pendingApplications: number
  totalViews: number
  totalFavorites: number
}

interface Listing {
  id: string
  title: string
  city: string
  neighborhood: string
  rentAmount: number
  status: "ACTIVE" | "RENTED" | "INACTIVE"
  viewsCount: number
  favoritesCount: number
  applicationsCount: number
  photos: { photoUrl: string; isPrimary: boolean }[]
  createdAt: string
}

export default function LandlordDashboard() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const [stats, setStats] = useState<LandlordStats | null>(null)
  const [listings, setListings] = useState<Listing[]>([])
  const [isLoading, setIsLoading] = useState(true)

  const fetchData = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const [statsRes, listingsRes] = await Promise.all([
        api.get(`/listings/landlord/${user.id}/statistics`),
        api.get(`/listings/landlord/${user.id}`)
      ])
      setStats(statsRes.data)
      setListings(listingsRes.data || [])
    } catch (err) {
      console.error("Failed to fetch landlord data:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchData,
  })

  const handleMarkRented = async (listingId: string) => {
    if (!user?.id) return
    try {
      await api.post(`/listings/${listingId}/mark-rented`, null, {
        params: { landlordId: user.id }
      })
      fetchData()
    } catch (err) {
      console.error("Failed to mark as rented:", err)
    }
  }

  const handleMarkAvailable = async (listingId: string) => {
    if (!user?.id) return
    try {
      await api.post(`/listings/${listingId}/mark-available`, null, {
        params: { landlordId: user.id }
      })
      fetchData()
    } catch (err) {
      console.error("Failed to mark as available:", err)
    }
  }

  const statCards = [
    { icon: Home, label: "Active Listings", value: stats?.activeListings || 0, color: "bg-blue-100 text-blue-600" },
    { icon: Users, label: "Pending Apps", value: stats?.pendingApplications || 0, color: "bg-amber-100 text-amber-600" },
    { icon: Eye, label: "Total Views", value: stats?.totalViews || 0, color: "bg-emerald-100 text-emerald-600" },
    { icon: Heart, label: "Favorites", value: stats?.totalFavorites || 0, color: "bg-pink-100 text-pink-600" },
  ]

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Dashboard" />

      {/* Content */}
      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        <div className="p-4 space-y-6">
          {/* Stats Grid */}
          <div className="grid grid-cols-2 gap-3">
            {isLoading ? (
              <>
                {[1, 2, 3, 4].map((i) => (
                  <Skeleton key={i} className="h-24 rounded-2xl" />
                ))}
              </>
            ) : (
              statCards.map((stat, index) => {
                const Icon = stat.icon
                return (
                  <div key={index} className="bg-white rounded-2xl p-4 shadow-sm">
                    <div className={`h-10 w-10 rounded-xl ${stat.color} flex items-center justify-center mb-2`}>
                      <Icon className="h-5 w-5" />
                    </div>
                    <p className="text-2xl font-bold text-slate-900">{stat.value}</p>
                    <p className="text-xs text-slate-500">{stat.label}</p>
                  </div>
                )
              })
            )}
          </div>

          {/* Quick Actions */}
          <div className="flex gap-3">
            <Link href="/landlord/listings/new" className="flex-1">
              <Button className="w-full h-12 rounded-xl">
                <Plus className="h-5 w-5 mr-2" />
                Add Listing
              </Button>
            </Link>
            <Link href="/landlord/applications" className="flex-1">
              <Button variant="outline" className="w-full h-12 rounded-xl">
                <FileText className="h-5 w-5 mr-2" />
                Applications
              </Button>
            </Link>
          </div>

          {/* My Listings */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-lg font-semibold text-slate-900">My Listings</h2>
              <Link href="/landlord/listings" className="text-sm text-blue-600">
                View All
              </Link>
            </div>

            {isLoading ? (
              <div className="space-y-3">
                {[1, 2].map((i) => (
                  <Skeleton key={i} className="h-28 rounded-2xl" />
                ))}
              </div>
            ) : listings.length === 0 ? (
              <div className="text-center py-8 bg-white rounded-2xl">
                <div className="mx-auto mb-3 h-14 w-14 rounded-full bg-slate-100 flex items-center justify-center">
                  <Home className="h-7 w-7 text-slate-400" />
                </div>
                <p className="text-slate-500 text-sm mb-3">No listings yet</p>
                <Link href="/landlord/listings/new">
                  <Button size="sm">Create Your First Listing</Button>
                </Link>
              </div>
            ) : (
              <div className="space-y-3">
                {listings.slice(0, 5).map((listing) => (
                  <div key={listing.id} className="bg-white rounded-2xl shadow-sm overflow-hidden">
                    <div className="flex gap-3 p-3">
                      {/* Image */}
                      <div className="h-20 w-20 rounded-xl bg-slate-100 overflow-hidden flex-shrink-0">
                        {listing.photos?.[0]?.photoUrl ? (
                          <img 
                            src={listing.photos[0].photoUrl} 
                            alt={listing.title}
                            className="h-full w-full object-cover"
                          />
                        ) : (
                          <div className="h-full w-full flex items-center justify-center text-2xl">
                            🏠
                          </div>
                        )}
                      </div>

                      {/* Details */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-2">
                          <h3 className="font-medium text-slate-900 line-clamp-1">
                            {listing.title}
                          </h3>
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" className="h-8 w-8 -mr-2">
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem asChild>
                                <Link href={`/landlord/listings/${listing.id}/edit`}>
                                  Edit Listing
                                </Link>
                              </DropdownMenuItem>
                              {listing.status === "ACTIVE" ? (
                                <DropdownMenuItem onClick={() => handleMarkRented(listing.id)}>
                                  Mark as Rented
                                </DropdownMenuItem>
                              ) : (
                                <DropdownMenuItem onClick={() => handleMarkAvailable(listing.id)}>
                                  Mark as Available
                                </DropdownMenuItem>
                              )}
                              <DropdownMenuItem asChild>
                                <Link href={`/listings/${listing.id}`}>
                                  View Public Page
                                </Link>
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </div>
                        
                        <p className="text-sm text-slate-500">
                          {listing.neighborhood}, {listing.city}
                        </p>
                        
                        <p className="text-sm font-semibold text-slate-900 mt-1">
                          {formatCurrency(listing.rentAmount)}/mois
                        </p>

                        {/* Stats */}
                        <div className="flex items-center gap-3 mt-2 text-xs text-slate-500">
                          <span className="flex items-center gap-1">
                            <Eye className="h-3.5 w-3.5" />
                            {listing.viewsCount}
                          </span>
                          <span className="flex items-center gap-1">
                            <Heart className="h-3.5 w-3.5" />
                            {listing.favoritesCount}
                          </span>
                          <span className="flex items-center gap-1">
                            <Users className="h-3.5 w-3.5" />
                            {listing.applicationsCount}
                          </span>
                          <span className={`ml-auto px-2 py-0.5 rounded-full text-[10px] font-medium ${
                            listing.status === "ACTIVE" 
                              ? "bg-emerald-100 text-emerald-700" 
                              : listing.status === "RENTED"
                              ? "bg-blue-100 text-blue-700"
                              : "bg-slate-100 text-slate-600"
                          }`}>
                            {listing.status}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
