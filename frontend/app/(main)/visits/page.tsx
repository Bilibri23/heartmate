"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import {
  Clock,
  CheckCircle,
  XCircle,
  CalendarClock,
  CalendarCheck,
  UserX,
  MapPin,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import Link from "next/link"
import { toast } from "sonner"
import api from "@/lib/api"
import type { LucideIcon } from "lucide-react"
import type { Visit, VisitStatus } from "@/types/visit"

function errorMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } }
  return e?.response?.data?.message || fallback
}

const STATUS_CONFIG: Record<VisitStatus, { icon: LucideIcon; color: string; bg: string; label: string }> = {
  REQUESTED: { icon: Clock, color: "text-amber-600", bg: "bg-amber-50", label: "Requested" },
  ACCEPTED: { icon: CalendarCheck, color: "text-emerald-600", bg: "bg-emerald-50", label: "Confirmed" },
  RESCHEDULED: { icon: CalendarClock, color: "text-blue-600", bg: "bg-blue-50", label: "Rescheduled" },
  COMPLETED: { icon: CheckCircle, color: "text-emerald-700", bg: "bg-emerald-50", label: "Completed" },
  CANCELLED: { icon: XCircle, color: "text-red-600", bg: "bg-red-50", label: "Cancelled" },
  NO_SHOW: { icon: UserX, color: "text-slate-600", bg: "bg-slate-100", label: "No-show" },
}

function formatWhen(iso?: string | null) {
  if (!iso) return "—"
  return new Date(iso).toLocaleString("en-GB", {
    weekday: "short",
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  })
}

export default function VisitsPage() {
  const { user } = useAuth()
  const [visits, setVisits] = useState<Visit[]>([])
  const [isLoading, setIsLoading] = useState(true)

  const fetchVisits = useCallback(async () => {
    if (!user?.id) return
    setIsLoading(true)
    try {
      const response = await api.get("/visits/my", { params: { size: 50 } })
      const content = response.data?.content || response.data || []
      setVisits(content)
    } catch (err) {
      console.error("Failed to fetch visits:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchVisits()
  }, [fetchVisits])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({ onRefresh: fetchVisits })

  const handleCancel = async (visitId: string) => {
    try {
      await api.put(`/visits/${visitId}/cancel`, { reason: "Cancelled by tenant" })
      toast.success("Visit cancelled")
      fetchVisits()
    } catch (err) {
      toast.error(errorMessage(err, "Failed to cancel visit"))
    }
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="My Visits" />

      <div ref={containerRef} className="flex-1 overflow-y-auto relative">
        <PullToRefreshIndicator pullProgress={pullProgress} isRefreshing={isRefreshing} />

        <div className="p-4 space-y-3">
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

          {!isLoading && visits.length === 0 && (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">📅</div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">No visits yet</h3>
              <p className="text-slate-500 text-sm mb-4">
                Find a place you like and request a visit from the listing page. Landlords confirm a time here.
              </p>
              <Link href="/search">
                <Button>Browse listings</Button>
              </Link>
            </div>
          )}

          {!isLoading &&
            visits.map((visit) => {
              const cfg = STATUS_CONFIG[visit.status]
              const StatusIcon = cfg.icon
              const showTime = visit.visitDatetime || visit.requestedDatetime
              const canCancel = visit.status === "REQUESTED" || visit.status === "ACCEPTED" || visit.status === "RESCHEDULED"
              return (
                <div key={visit.id} className="bg-white rounded-2xl shadow-sm overflow-hidden">
                  <Link href={`/listings/${visit.listingId}`}>
                    <div className="p-4">
                      <div className="flex gap-3">
                        <div className="h-20 w-20 rounded-xl bg-slate-100 overflow-hidden flex-shrink-0">
                          {visit.listingPrimaryPhotoUrl ? (
                            <img src={visit.listingPrimaryPhotoUrl} alt={visit.listingTitle} className="h-full w-full object-cover" />
                          ) : (
                            <div className="h-full w-full flex items-center justify-center text-2xl">🏠</div>
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <h3 className="font-semibold text-slate-900 line-clamp-1">{visit.listingTitle}</h3>
                          <p className="text-sm text-slate-500 flex items-center gap-1 mt-0.5">
                            <MapPin className="h-3.5 w-3.5" />
                            {[visit.listingNeighborhood, visit.listingCity].filter(Boolean).join(", ")}
                          </p>
                          <p className="text-sm font-medium text-slate-900 mt-1 flex items-center gap-1">
                            <CalendarClock className="h-3.5 w-3.5 text-slate-400" />
                            {formatWhen(showTime)}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center justify-between mt-3 pt-3 border-t border-slate-100">
                        <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full ${cfg.bg}`}>
                          <StatusIcon className={`h-4 w-4 ${cfg.color}`} />
                          <span className={`text-xs font-medium ${cfg.color}`}>{cfg.label}</span>
                        </div>
                        {visit.status === "RESCHEDULED" && (
                          <span className="text-xs text-blue-600">New time proposed</span>
                        )}
                      </div>

                      {visit.landlordResponse && (
                        <div className="mt-3 p-3 bg-slate-50 rounded-xl">
                          <p className="text-xs text-slate-500 mb-1">Landlord note:</p>
                          <p className="text-sm text-slate-700">{visit.landlordResponse}</p>
                        </div>
                      )}
                    </div>
                  </Link>

                  {canCancel && (
                    <div className="px-4 pb-4">
                      <Button
                        variant="outline"
                        size="sm"
                        className="w-full rounded-xl text-red-600 border-red-200 hover:bg-red-50"
                        onClick={() => handleCancel(visit.id)}
                      >
                        Cancel visit
                      </Button>
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
