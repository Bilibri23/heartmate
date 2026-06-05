"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  FileText, 
  Calendar, 
  CheckCircle, 
  Clock, 
  XCircle,
  ChevronRight,
  MapPin,
  CreditCard,
  Download
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { NextStepCard } from "@/components/workflows/next-step-card"
import Link from "next/link"
import api from "@/lib/api"

interface Lease {
  id: string
  referenceCode: string
  listingId: string
  listingTitle: string
  listingCity: string
  listingNeighborhood: string
  listingPhotoUrl: string | null
  landlordName: string
  studentName: string
  monthlyRent: number
  depositAmount: number
  startDate: string
  endDate: string
  status: "PENDING_ACCEPTANCE" | "PENDING_PAYMENT" | "PENDING_SIGNATURES" | "ACTIVE" | "COMPLETED" | "TERMINATED"
  studentAccepted: boolean
  landlordAccepted: boolean
  createdAt: string
}

type StatusFilter = "all" | "ACTIVE" | "PENDING_ACCEPTANCE" | "PENDING_PAYMENT" | "COMPLETED"

const STATUS_CONFIG: Record<string, { icon: typeof Clock; color: string; bg: string; label: string }> = {
  PENDING_ACCEPTANCE: { 
    icon: Clock, 
    color: "text-amber-600", 
    bg: "bg-amber-50",
    label: "Pending Acceptance"
  },
  PENDING_PAYMENT: { 
    icon: CreditCard, 
    color: "text-orange-600", 
    bg: "bg-orange-50",
    label: "Pending Payment"
  },
  PENDING_SIGNATURES: { 
    icon: FileText, 
    color: "text-purple-600", 
    bg: "bg-purple-50",
    label: "Pending Signatures"
  },
  ACTIVE: { 
    icon: CheckCircle, 
    color: "text-emerald-600", 
    bg: "bg-emerald-50",
    label: "Active"
  },
  COMPLETED: { 
    icon: CheckCircle, 
    color: "text-blue-600", 
    bg: "bg-blue-50",
    label: "Completed"
  },
  TERMINATED: { 
    icon: XCircle, 
    color: "text-red-600", 
    bg: "bg-red-50",
    label: "Terminated"
  },
}

export default function LeasesPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const [leases, setLeases] = useState<Lease[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all")

  const fetchLeases = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await api.get("/leases/my")
      const content = response.data?.content || response.data || []
      setLeases(content)
    } catch (err) {
      console.error("Failed to fetch leases:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchLeases()
  }, [fetchLeases])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchLeases,
  })

  const [acceptingLeaseId, setAcceptingLeaseId] = useState<string | null>(null)
  const [acceptError, setAcceptError] = useState<string | null>(null)

  const handleAcceptTerms = async (leaseId: string) => {
    setAcceptingLeaseId(leaseId)
    setAcceptError(null)
    try {
      await api.post(`/leases/${leaseId}/accept-terms`)
      fetchLeases()
    } catch (err: any) {
      console.error("Failed to accept terms:", err)
      const message = err.response?.data?.message || "Failed to accept terms"
      setAcceptError(message)
    } finally {
      setAcceptingLeaseId(null)
    }
  }

  const filters: { id: StatusFilter; label: string }[] = [
    { id: "all", label: t.common.all },
    { id: "ACTIVE", label: t.leases.active },
    { id: "PENDING_PAYMENT", label: "Pending Payment" },
    { id: "PENDING_ACCEPTANCE", label: t.leases.pending },
    { id: "COMPLETED", label: t.leases.completed },
  ]

  const filteredLeases = leases.filter((lease) => {
    if (statusFilter === "all") return true
    return lease.status === statusFilter
  })

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("fr-CM", {
      day: "numeric",
      month: "short",
      year: "numeric"
    })
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.leases.title} />

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
          {!isLoading && leases.length > 0 && (
            <NextStepCard
              title="What happens next?"
              body="Review lease terms first. If payment is required, RoomBay will send you to the payment flow; signed and verified leases become active after the required steps are complete."
              href="/payments"
              actionLabel="Open payments"
              icon={<FileText className="h-5 w-5" />}
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
          {!isLoading && leases.length === 0 && (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">📄</div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                No leases yet
              </h3>
              <p className="text-slate-500 text-sm mb-4">
                Accepted applications create lease steps here. Keep checking applications if you are still waiting for landlord review.
              </p>
              <Link href="/applications">
                <Button>{t.nav.applications}</Button>
              </Link>
              <div className="mt-3">
                <Link href="/help" className="text-sm font-medium text-blue-600">
                  Get help
                </Link>
              </div>
            </div>
          )}

          {/* Leases List */}
          {!isLoading && filteredLeases.map((lease) => {
            const statusConfig = STATUS_CONFIG[lease.status] || { 
              icon: Clock, 
              color: "text-slate-600", 
              bg: "bg-slate-50", 
              label: lease.status 
            }
            const StatusIcon = statusConfig.icon
            const needsAcceptance = (lease.status === "PENDING_ACCEPTANCE" || lease.status === "PENDING_PAYMENT") && !lease.studentAccepted

            return (
              <div 
                key={lease.id} 
                className="bg-white rounded-2xl shadow-sm overflow-hidden"
              >
                <div className="p-4">
                  <div className="flex gap-3">
                    {/* Listing Image */}
                    <div className="h-20 w-20 rounded-xl bg-slate-100 overflow-hidden flex-shrink-0">
                      {lease.listingPhotoUrl ? (
                        <img 
                          src={lease.listingPhotoUrl} 
                          alt={lease.listingTitle}
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
                          {lease.listingTitle}
                        </h3>
                      </div>
                      
                      <p className="text-sm text-slate-500 flex items-center gap-1 mt-0.5">
                        <MapPin className="h-3.5 w-3.5" />
                        {lease.listingNeighborhood}, {lease.listingCity}
                      </p>
                      
                      <p className="text-sm font-semibold text-slate-900 mt-1">
                        {formatCurrency(lease.monthlyRent)}/mois
                      </p>
                    </div>
                  </div>

                  {/* Lease Details */}
                  <div className="mt-4 grid grid-cols-2 gap-3">
                    <div className="bg-slate-50 rounded-xl p-3">
                      <p className="text-xs text-slate-500 mb-1">{t.leases.startDate}</p>
                      <p className="text-sm font-medium text-slate-900">
                        {formatDate(lease.startDate)}
                      </p>
                    </div>
                    <div className="bg-slate-50 rounded-xl p-3">
                      <p className="text-xs text-slate-500 mb-1">{t.leases.endDate}</p>
                      <p className="text-sm font-medium text-slate-900">
                        {formatDate(lease.endDate)}
                      </p>
                    </div>
                  </div>

                  {/* Reference Code */}
                  <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
                    <span>Ref: {lease.referenceCode}</span>
                    <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full ${statusConfig.bg}`}>
                      <StatusIcon className={`h-4 w-4 ${statusConfig.color}`} />
                      <span className={`font-medium ${statusConfig.color}`}>
                        {statusConfig.label}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="px-4 pb-4 space-y-2">
                  {lease.status === "PENDING_PAYMENT" && (
                    <Link href={`/payments?leaseId=${lease.id}`}>
                      <Button className="w-full rounded-xl bg-orange-600 hover:bg-orange-700">
                        <CreditCard className="h-4 w-4 mr-2" />
                        Pay Deposit & First Month
                      </Button>
                    </Link>
                  )}

                  {lease.status === "PENDING_SIGNATURES" && !lease.studentAccepted && (
                    <Link href={`/leases/${lease.id}/sign`}>
                      <Button className="w-full rounded-xl">
                        Sign Lease Agreement
                      </Button>
                    </Link>
                  )}

                  {lease.status === "PENDING_SIGNATURES" && lease.studentAccepted && (
                    <div className="text-center text-sm text-slate-500 py-2">
                      ✓ You signed • Waiting for landlord...
                    </div>
                  )}
                  
                  {lease.status === "ACTIVE" && user?.role === "STUDENT" && (
                    <div className="flex gap-2">
                      <Button 
                        variant="outline" 
                        className="flex-1 rounded-xl"
                        onClick={async () => {
                          try {
                            const token = localStorage.getItem('token')
                            const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082/api'
                            const response = await fetch(`${backendUrl}/leases/${lease.id}/document`, {
                              method: 'GET',
                              headers: {
                                'Authorization': `Bearer ${token}`
                              }
                            })
                            
                            if (!response.ok) {
                              throw new Error('Failed to download document')
                            }
                            
                            const blob = await response.blob()
                            const url = window.URL.createObjectURL(blob)
                            const link = document.createElement('a')
                            link.href = url
                            link.download = `lease-${lease.referenceCode}.pdf`
                            document.body.appendChild(link)
                            link.click()
                            link.remove()
                            window.URL.revokeObjectURL(url)
                          } catch (err) {
                            console.error('Failed to download lease document:', err)
                          }
                        }}
                      >
                        <Download className="h-4 w-4" />
                      </Button>
                    </div>
                  )}

                  {lease.status === "COMPLETED" && (
                    <Link href={`/reviews/new?leaseId=${lease.id}`}>
                      <Button variant="outline" className="w-full rounded-xl">
                        {t.reviews.writeReview}
                      </Button>
                    </Link>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
