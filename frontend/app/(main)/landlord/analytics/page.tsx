"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import {
  Eye,
  Heart,
  Users,
  Home,
  Calendar,
  BarChart3,
  ArrowUpRight,
  ArrowDownRight,
  Glasses,
  ClipboardList,
} from "lucide-react"
import { Skeleton } from "@/components/ui/skeleton"
import api from "@/lib/api"

interface AnalyticsData {
  range: string
  periodDays: number
  totalViews: number
  viewsChange: number
  totalFavorites: number
  favoritesChange: number
  totalApplications: number
  applicationsChange: number
  activeListings: number
  rentedListings: number
  occupancyRate: number
  avgTimeToRentDays: number | null
  avgListingQuality: number | null
  pendingApplications: number
  acceptedApplications: number
  rejectedApplications: number
  visitsRequested: number
  visitsAccepted: number
  visitsCompleted: number
  funnel: {
    views: number
    favorites: number
    applications: number
    visits: number
    acceptedApplications: number
  }
  topListings: {
    id: string
    title: string
    views: number
    favorites: number
    applications: number
    qualityScore: number | null
  }[]
}

export default function LandlordAnalyticsPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [data, setData] = useState<AnalyticsData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [period, setPeriod] = useState<"7d" | "30d" | "90d">("30d")

  const fetchAnalytics = useCallback(async () => {
    if (!user?.id) return

    setIsLoading(true)
    try {
      const response = await api.get(`/landlord/analytics?range=${period}`)
      setData(response.data)
    } catch (err) {
      console.error("Failed to fetch analytics:", err)
      setData(null)
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
    color,
  }: {
    icon: React.ComponentType<{ className?: string }>
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
          <div
            className={`flex items-center text-xs font-medium ${change >= 0 ? "text-emerald-600" : "text-red-600"}`}
          >
            {change >= 0 ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
            {Math.abs(change)}%
          </div>
        )}
      </div>
    </div>
  )

  const qualityColor = (score: number | null | undefined) => {
    if (score == null) return "bg-slate-100 text-slate-600"
    if (score >= 80) return "bg-emerald-100 text-emerald-700"
    if (score >= 50) return "bg-amber-100 text-amber-700"
    return "bg-red-100 text-red-700"
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Analytics" />

      <div ref={containerRef} className="flex-1 overflow-y-auto relative">
        <PullToRefreshIndicator pullProgress={pullProgress} isRefreshing={isRefreshing} />

        <div className="p-4 space-y-4">
          <div className="flex gap-2 bg-white rounded-xl p-1">
            {[
              { value: "7d", label: "7 Days" },
              { value: "30d", label: "30 Days" },
              { value: "90d", label: "90 Days" },
            ].map((p) => (
              <button
                key={p.value}
                onClick={() => setPeriod(p.value as "7d" | "30d" | "90d")}
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
                label={`Views (${period})`}
                value={data?.totalViews?.toLocaleString() || 0}
                change={data?.viewsChange}
                color="bg-blue-100 text-blue-600"
              />
              <StatCard
                icon={Heart}
                label={`Favorites (${period})`}
                value={data?.totalFavorites || 0}
                change={data?.favoritesChange}
                color="bg-pink-100 text-pink-600"
              />
              <StatCard
                icon={Users}
                label={`Applications (${period})`}
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

          {!isLoading && data && (
            <div className="grid grid-cols-2 gap-3">
              <StatCard
                icon={ClipboardList}
                label="Pending Apps"
                value={data.pendingApplications}
                color="bg-violet-100 text-violet-600"
              />
              <StatCard
                icon={BarChart3}
                label="Avg Quality"
                value={data.avgListingQuality != null ? `${data.avgListingQuality}%` : "—"}
                color="bg-slate-100 text-slate-700"
              />
            </div>
          )}

          {!isLoading && data && (
            <div className="bg-white rounded-2xl p-4 shadow-sm">
              <h3 className="font-semibold text-slate-900 mb-4">Performance</h3>
              <div className="space-y-4">
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm text-slate-600">Occupancy Rate</span>
                    <span className="text-sm font-semibold text-slate-900">{data.occupancyRate}%</span>
                  </div>
                  <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-emerald-500 rounded-full transition-all"
                      style={{ width: `${data.occupancyRate}%` }}
                    />
                  </div>
                </div>
                {data.avgTimeToRentDays != null && (
                  <div className="flex items-center justify-between py-3 border-t border-slate-100">
                    <div className="flex items-center gap-2">
                      <Calendar className="h-4 w-4 text-slate-400" />
                      <span className="text-sm text-slate-600">Avg. Time to Rent</span>
                    </div>
                    <span className="text-sm font-semibold text-slate-900">{data.avgTimeToRentDays} days</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {!isLoading && data && (
            <div className="bg-white rounded-2xl p-4 shadow-sm">
              <h3 className="font-semibold text-slate-900 mb-4">Visit Funnel</h3>
              <div className="grid grid-cols-3 gap-2 text-center">
                <div className="rounded-xl bg-slate-50 p-3">
                  <p className="text-lg font-bold text-slate-900">{data.visitsRequested}</p>
                  <p className="text-xs text-slate-500">Requested</p>
                </div>
                <div className="rounded-xl bg-slate-50 p-3">
                  <p className="text-lg font-bold text-slate-900">{data.visitsAccepted}</p>
                  <p className="text-xs text-slate-500">Accepted</p>
                </div>
                <div className="rounded-xl bg-slate-50 p-3">
                  <p className="text-lg font-bold text-slate-900">{data.visitsCompleted}</p>
                  <p className="text-xs text-slate-500">Completed</p>
                </div>
              </div>
              <div className="mt-4 pt-4 border-t border-slate-100">
                <p className="text-xs font-medium text-slate-500 mb-2">Period funnel ({period})</p>
                <div className="flex flex-wrap gap-2 text-xs text-slate-600">
                  <span className="rounded-full bg-blue-50 px-2 py-1">{data.funnel.views} views</span>
                  <span className="rounded-full bg-pink-50 px-2 py-1">{data.funnel.favorites} favorites</span>
                  <span className="rounded-full bg-emerald-50 px-2 py-1">{data.funnel.applications} apps</span>
                  <span className="rounded-full bg-violet-50 px-2 py-1">{data.funnel.visits} visits</span>
                  <span className="rounded-full bg-amber-50 px-2 py-1">
                    {data.funnel.acceptedApplications} accepted
                  </span>
                </div>
              </div>
            </div>
          )}

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
                      <div className="flex items-center gap-2">
                        <p className="font-medium text-slate-900 text-sm line-clamp-1">{listing.title}</p>
                        {listing.qualityScore != null && (
                          <span
                            className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-semibold ${qualityColor(listing.qualityScore)}`}
                          >
                            {listing.qualityScore}%
                          </span>
                        )}
                      </div>
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

          {!isLoading && (!data?.topListings || data.topListings.length === 0) && (
            <div className="bg-white rounded-2xl p-6 shadow-sm text-center">
              <BarChart3 className="h-10 w-10 text-slate-300 mx-auto mb-3" />
              <h3 className="font-medium text-slate-900 mb-1">No Data Yet</h3>
              <p className="text-sm text-slate-500">
                Analytics will appear once your listings get more activity in this period
              </p>
            </div>
          )}

          <div className="rounded-2xl border border-slate-200 bg-gradient-to-br from-slate-50 to-violet-50/60 p-4 shadow-sm">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-900 text-white">
                <Glasses className="h-5 w-5" aria-hidden />
              </div>
              <div className="min-w-0 space-y-1">
                <h3 className="text-sm font-semibold text-slate-900">{t.landlordJourney.analyticsRoadmapTitle}</h3>
                <p className="text-xs leading-relaxed text-slate-600">{t.landlordJourney.analyticsRoadmapBody}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
