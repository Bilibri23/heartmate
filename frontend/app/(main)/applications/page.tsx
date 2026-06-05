"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  Clock, 
  CheckCircle, 
  XCircle, 
  AlertCircle,
  ChevronRight,
  MapPin,
  Calendar
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { NextStepCard } from "@/components/workflows/next-step-card"
import Link from "next/link"
import api from "@/lib/api"

interface Application {
  id: string
  listingId: string
  listingTitle: string
  listingCity: string
  listingNeighborhood: string
  listingPhotoUrl: string | null
  rentAmount: number
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "WITHDRAWN" | "SHORTLISTED"
  message: string
  landlordResponse: string | null
  createdAt: string
  updatedAt: string
}

type StatusFilter = "all" | "PENDING" | "ACCEPTED" | "REJECTED" | "SHORTLISTED"

const STATUS_CONFIG = {
  PENDING: { 
    icon: Clock, 
    color: "text-amber-600", 
    bg: "bg-amber-50",
    label: "Pending"
  },
  ACCEPTED: { 
    icon: CheckCircle, 
    color: "text-emerald-600", 
    bg: "bg-emerald-50",
    label: "Accepted"
  },
  REJECTED: { 
    icon: XCircle, 
    color: "text-red-600", 
    bg: "bg-red-50",
    label: "Rejected"
  },
  WITHDRAWN: { 
    icon: AlertCircle, 
    color: "text-slate-600", 
    bg: "bg-slate-50",
    label: "Withdrawn"
  },
  SHORTLISTED: { 
    icon: CheckCircle, 
    color: "text-blue-600", 
    bg: "bg-blue-50",
    label: "Shortlisted"
  },
}

export default function ApplicationsPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const [applications, setApplications] = useState<Application[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all")


  const fetchApplications = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const params: Record<string, any> = { size: 50 }
      if (statusFilter !== "all") {
        params.status = statusFilter
      }
      
      const response = await api.get("/applications/my", { params })
      const content = response.data?.content || response.data || []
      // Map backend response to frontend format
      const mapped = content.map((app: any) => ({
        ...app,
        listingPhotoUrl: app.listingPhotoUrl || app.listingPrimaryPhotoUrl || null,
        listingNeighborhood: app.listingNeighborhood || app.listingAddress || '',
        rentAmount: app.rentAmount || app.listingPrice || 0,
      }))
      setApplications(mapped)
    } catch (err) {
      console.error("Failed to fetch applications:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id, statusFilter])

  useEffect(() => {
    fetchApplications()
  }, [fetchApplications])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchApplications,
  })

  const handleWithdraw = async (applicationId: string) => {
    try {
      await api.put(`/applications/${applicationId}/withdraw`)
      fetchApplications()
    } catch (err) {
      console.error("Failed to withdraw application:", err)
    }
  }

  const filters: { id: StatusFilter; label: string }[] = [
    { id: "all", label: t.common.all },
    { id: "PENDING", label: t.applications.pending },
    { id: "ACCEPTED", label: t.applications.accepted },
    { id: "SHORTLISTED", label: t.applications.shortlisted },
    { id: "REJECTED", label: t.applications.rejected },
  ]

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("fr-CM", {
      day: "numeric",
      month: "short",
      year: "numeric"
    })
  }

  return (
    <div data-tour="tenant-applications" className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.applications.title} />


      {/* Status Filters */}
      <div className="sticky top-14 z-30 bg-white border-b border-slate-200">
        <div className="flex px-4 gap-2 py-2 overflow-x-auto scrollbar-hide">
          {filters.map((filter) => (
            <button
              key={filter.id}
              onClick={() => setStatusFilter(filter.id)}
              className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                statusFilter === filter.id
                  ? "bg-blue-600 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {filter.label}
            </button>
          ))}
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

        <div className="p-4 space-y-3">
          {!isLoading && applications.length > 0 && (
            <NextStepCard
              title="What happens next?"
              body="Pending applications wait for landlord review. If an application is accepted, RoomBay guides you into lease and payment steps from the same account."
              href="/leases"
              actionLabel="Check leases"
              icon={<CheckCircle className="h-5 w-5" />}
            />
          )}

          {/* Loading */}
          {isLoading && (
            <>
              {[1, 2, 3].map((i) => (
                <div key={i} className="bg-white rounded-2xl p-4 shadow-sm">
                  <div className="flex gap-3">
                    <Skeleton className="h-20 w-20 rounded-xl" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-5 w-32" />
                      <Skeleton className="h-4 w-full" />
                      <Skeleton className="h-4 w-24" />
                    </div>
                  </div>
                </div>
              ))}
            </>
          )}

          {/* Empty State */}
          {!isLoading && applications.length === 0 && (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">📋</div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                {t.applications.noApplications}
              </h3>
              <p className="text-slate-500 text-sm mb-4">
                Start with search, save homes you like, then apply when a listing fits your budget and move-in timing.
              </p>
              <Link href="/search">
                <Button>{t.nav.search}</Button>
              </Link>
              <div className="mt-3">
                <Link href="/help" className="text-sm font-medium text-blue-600">
                  Get help
                </Link>
              </div>
            </div>
          )}

          {/* Applications List */}
          {!isLoading && applications.map((app) => {
            const statusConfig = STATUS_CONFIG[app.status]
            const StatusIcon = statusConfig.icon

            return (
              <div 
                key={app.id} 
                className="bg-white rounded-2xl shadow-sm overflow-hidden"
              >
                <Link href={`/listings/${app.listingId}`}>
                  <div className="p-4">
                    <div className="flex gap-3">
                      {/* Listing Image */}
                      <div className="h-20 w-20 rounded-xl bg-slate-100 overflow-hidden flex-shrink-0">
                        {app.listingPhotoUrl ? (
                          <img 
                            src={app.listingPhotoUrl} 
                            alt={app.listingTitle}
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
                          <h3 className="font-semibold text-slate-900 line-clamp-1">
                            {app.listingTitle}
                          </h3>
                          <ChevronRight className="h-5 w-5 text-slate-400 flex-shrink-0" />
                        </div>
                        
                        <p className="text-sm text-slate-500 flex items-center gap-1 mt-0.5">
                          <MapPin className="h-3.5 w-3.5" />
                          {app.listingNeighborhood}, {app.listingCity}
                        </p>
                        
                        <p className="text-sm font-semibold text-slate-900 mt-1">
                          {formatCurrency(app.rentAmount)}/mois
                        </p>
                      </div>
                    </div>

                    {/* Status & Date */}
                    <div className="flex items-center justify-between mt-3 pt-3 border-t border-slate-100">
                      <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full ${statusConfig.bg}`}>
                        <StatusIcon className={`h-4 w-4 ${statusConfig.color}`} />
                        <span className={`text-xs font-medium ${statusConfig.color}`}>
                          {statusConfig.label}
                        </span>
                      </div>
                      
                      <div className="flex items-center gap-1 text-xs text-slate-500">
                        <Calendar className="h-3.5 w-3.5" />
                        {formatDate(app.createdAt)}
                      </div>
                    </div>

                    {/* Landlord Response */}
                    {app.landlordResponse && (
                      <div className="mt-3 p-3 bg-slate-50 rounded-xl">
                        <p className="text-xs text-slate-500 mb-1">Landlord response:</p>
                        <p className="text-sm text-slate-700">{app.landlordResponse}</p>
                      </div>
                    )}
                  </div>
                </Link>

                {/* Actions */}
                {app.status === "PENDING" && (
                  <div className="px-4 pb-4">
                    <Button 
                      variant="outline" 
                      size="sm"
                      className="w-full rounded-xl text-red-600 border-red-200 hover:bg-red-50"
                      onClick={() => handleWithdraw(app.id)}
                    >
                      {t.applications.withdraw}
                    </Button>
                  </div>
                )}

                {app.status === "ACCEPTED" && (
                  <div className="px-4 pb-4">
                    <Link href={`/leases?applicationId=${app.id}`}>
                      <Button className="w-full rounded-xl">
                        View Lease Details
                      </Button>
                    </Link>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
