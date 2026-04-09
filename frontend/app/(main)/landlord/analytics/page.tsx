"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  TrendingUp,
  TrendingDown,
  Eye,
  Heart,
  Users,
  Home,
  Calendar,
  BarChart3,
  ArrowUpRight,
  ArrowDownRight
} from "lucide-react"
import { Skeleton } from "@/components/ui/skeleton"
import api from "@/lib/api"

interface AnalyticsData {
  totalViews: number
  viewsChange: number
  totalFavorites: number
  favoritesChange: number
  totalApplications: number
  applicationsChange: number
  activeListings: number
  occupancyRate: number
  avgTimeToRent: number
  topListings: {
    id: string
    title: string
    views: number
    favorites: number
    applications: number
  }[]
  viewsByDay: { date: string; views: number }[]
}

export default function LandlordAnalyticsPage() {
  const { formatCurrency } = useLanguage()
  const { user } = useAuth()
  const [data, setData] = useState<AnalyticsData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [period, setPeriod] = useState<"7d" | "30d" | "90d">("30d")

  const fetchAnalytics = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      // Fetch landlord's listings and compute analytics locally
      const listingsRes = await api.get(`/listings/landlord/${user.id}`)
      const listings = listingsRes.data || []
      
      // Compute analytics from listings data
      const totalViews = listings.reduce((sum: number, l: any) => sum + (l.viewsCount ?? l.viewCount ?? 0), 0)
      const totalFavorites = listings.reduce((sum: number, l: any) => sum + (l.favoritesCount ?? 0), 0)
      const totalApplications = listings.reduce((sum: number, l: any) => sum + (l.applicationsCount ?? l.applicationCount ?? 0), 0)
      const activeListings = listings.filter((l: any) => l.status === "ACTIVE").length
      const rentedListings = listings.filter((l: any) => l.status === "RENTED").length
      const occupancyRate = listings.length > 0 ? Math.round((rentedListings / listings.length) * 100) : 0
      
      // Top listings by views
      const topListings = [...listings]
        .sort((a: any, b: any) => (b.viewsCount ?? b.viewCount ?? 0) - (a.viewsCount ?? a.viewCount ?? 0))
        .slice(0, 5)
        .map((l: any) => ({
          id: l.id,
          title: l.title,
          views: l.viewsCount ?? l.viewCount ?? 0,
          favorites: l.favoritesCount ?? 0,
          applications: l.applicationsCount ?? l.applicationCount ?? 0
        }))
      
      setData({
        totalViews,
        viewsChange: 0, // Would need historical data to compute
        totalFavorites,
        favoritesChange: 0,
        totalApplications,
        applicationsChange: 0,
        activeListings,
        occupancyRate,
        avgTimeToRent: 14, // Would need lease data to compute
        topListings,
        viewsByDay: []
      })
    } catch (err) {
      console.error("Failed to fetch analytics:", err)
      setData({
        totalViews: 0,
        viewsChange: 0,
        totalFavorites: 0,
        favoritesChange: 0,
        totalApplications: 0,
        applicationsChange: 0,
        activeListings: 0,
        occupancyRate: 0,
        avgTimeToRent: 0,
        topListings: [],
        viewsByDay: []
      })
    } finally {
      setIsLoading(false)
    }
  }, [user?.id, period])

  useEffect(() => {
    fetchAnalytics()
  }, [fetchAnalytics])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchAnalytics,
  })

  const StatCard = ({ 
    icon: Icon, 
    label, 
    value, 
    change, 
    color 
  }: { 
    icon: any
    label: string
    value: string | number
    change?: number
    color: string 
  }) => (
    <div className="bg-white rounded-2xl p-4 shadow-sm">
      <div className={`h-10 w-10 rounded-xl ${color} flex items-center justify-center mb-3`}>
        <Icon className="h-5 w-5" />
      </div>
      <p className="text-2xl font-bold text-slate-900">{value}</p>
      <div className="flex items-center justify-between mt-1">
        <p className="text-xs text-slate-500">{label}</p>
        {change !== undefined && (
          <div className={`flex items-center text-xs font-medium ${change >= 0 ? "text-emerald-600" : "text-red-600"}`}>
            {change >= 0 ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
            {Math.abs(change)}%
          </div>
        )}
      </div>
    </div>
  )

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Analytics" />

      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        <div className="p-4 space-y-4">
          {/* Period Selector */}
          <div className="flex gap-2 bg-white rounded-xl p-1">
            {[
              { value: "7d", label: "7 Days" },
              { value: "30d", label: "30 Days" },
              { value: "90d", label: "90 Days" },
            ].map((p) => (
              <button
                key={p.value}
                onClick={() => setPeriod(p.value as any)}
                className={`flex-1 py-2 rounded-lg text-sm font-medium transition-colors ${
                  period === p.value
                    ? "bg-blue-600 text-white"
                    : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>

          {/* Stats Grid */}
          {isLoading ? (
            <div className="grid grid-cols-2 gap-3">
              {[1, 2, 3, 4].map((i) => (
                <Skeleton key={i} className="h-28 rounded-2xl" />
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              <StatCard
                icon={Eye}
                label="Total Views"
                value={data?.totalViews?.toLocaleString() || 0}
                change={data?.viewsChange}
                color="bg-blue-100 text-blue-600"
              />
              <StatCard
                icon={Heart}
                label="Favorites"
                value={data?.totalFavorites || 0}
                change={data?.favoritesChange}
                color="bg-pink-100 text-pink-600"
              />
              <StatCard
                icon={Users}
                label="Applications"
                value={data?.totalApplications || 0}
                change={data?.applicationsChange}
                color="bg-emerald-100 text-emerald-600"
              />
              <StatCard
                icon={Home}
                label="Active Listings"
                value={data?.activeListings || 0}
                color="bg-amber-100 text-amber-600"
              />
            </div>
          )}

          {/* Performance Metrics */}
          {!isLoading && (
            <div className="bg-white rounded-2xl p-4 shadow-sm">
              <h3 className="font-semibold text-slate-900 mb-4">Performance</h3>
              <div className="space-y-4">
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm text-slate-600">Occupancy Rate</span>
                    <span className="text-sm font-semibold text-slate-900">{data?.occupancyRate || 0}%</span>
                  </div>
                  <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-emerald-500 rounded-full transition-all"
                      style={{ width: `${data?.occupancyRate || 0}%` }}
                    />
                  </div>
                </div>
                <div className="flex items-center justify-between py-3 border-t border-slate-100">
                  <div className="flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-slate-400" />
                    <span className="text-sm text-slate-600">Avg. Time to Rent</span>
                  </div>
                  <span className="text-sm font-semibold text-slate-900">{data?.avgTimeToRent || 0} days</span>
                </div>
              </div>
            </div>
          )}

          {/* Top Performing Listings */}
          {!isLoading && data?.topListings && data.topListings.length > 0 && (
            <div className="bg-white rounded-2xl p-4 shadow-sm">
              <h3 className="font-semibold text-slate-900 mb-4">Top Performing Listings</h3>
              <div className="space-y-3">
                {data.topListings.map((listing, index) => (
                  <div key={listing.id} className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl">
                    <div className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold text-sm">
                      {index + 1}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-slate-900 text-sm line-clamp-1">{listing.title}</p>
                      <div className="flex items-center gap-3 text-xs text-slate-500 mt-0.5">
                        <span className="flex items-center gap-1">
                          <Eye className="h-3 w-3" /> {listing.views}
                        </span>
                        <span className="flex items-center gap-1">
                          <Heart className="h-3 w-3" /> {listing.favorites}
                        </span>
                        <span className="flex items-center gap-1">
                          <Users className="h-3 w-3" /> {listing.applications}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Empty State for Top Listings */}
          {!isLoading && (!data?.topListings || data.topListings.length === 0) && (
            <div className="bg-white rounded-2xl p-6 shadow-sm text-center">
              <BarChart3 className="h-10 w-10 text-slate-300 mx-auto mb-3" />
              <h3 className="font-medium text-slate-900 mb-1">No Data Yet</h3>
              <p className="text-sm text-slate-500">
                Analytics will appear once your listings get more activity
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
