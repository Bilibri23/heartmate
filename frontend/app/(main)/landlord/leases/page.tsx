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
  User,
  CheckCircle,
  Clock,
  XCircle,
  AlertCircle,
  Home,
  Phone,
  Mail,
  Download,
  PenTool
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import Link from "next/link"
import api from "@/lib/api"

interface Lease {
  id: string
  referenceCode: string
  listingId: string
  listingTitle: string
  listingAddress: string
  studentId: string
  studentName: string
  studentEmail: string
  studentPhone: string
  startDate: string
  endDate: string
  monthlyRent: number
  depositAmount: number
  totalInitialPayment: number
  status: string
  studentAcceptedTerms: boolean
  landlordAcceptedTerms: boolean
  studentAcceptedAt: string | null
  landlordAcceptedAt: string | null
  durationMonths: number
  createdAt: string
}

type StatusFilter = "all" | "PENDING_SIGNATURES" | "ACTIVE" | "COMPLETED" | "TERMINATED"

export default function LandlordLeasesPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const [leases, setLeases] = useState<Lease[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all")
  const [acceptingLeaseId, setAcceptingLeaseId] = useState<string | null>(null)
  const [acceptError, setAcceptError] = useState<string | null>(null)

  const fetchLeases = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await api.get("/leases/as-landlord")
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
    { id: "all", label: "All" },
    { id: "PENDING_SIGNATURES", label: "Pending Signature" },
    { id: "ACTIVE", label: "Active" },
    { id: "COMPLETED", label: "Completed" },
    { id: "TERMINATED", label: "Terminated" },
  ]

  const filteredLeases = leases.filter((lease) => {
    if (statusFilter === "all") return true
    return lease.status === statusFilter
  })

  const getStatusConfig = (status: string) => {
    const configs: Record<string, { icon: typeof CheckCircle; color: string; bg: string; label: string }> = {
      PENDING_SIGNATURES: { icon: Clock, color: "text-blue-600", bg: "bg-blue-100", label: "Pending Signatures" },
      PENDING_PAYMENT: { icon: AlertCircle, color: "text-orange-600", bg: "bg-orange-100", label: "Pending Payment" },
      ACTIVE: { icon: CheckCircle, color: "text-green-600", bg: "bg-green-100", label: "Active" },
      COMPLETED: { icon: CheckCircle, color: "text-slate-600", bg: "bg-slate-100", label: "Completed" },
      TERMINATED: { icon: XCircle, color: "text-red-600", bg: "bg-red-100", label: "Terminated" },
    }
    return configs[status] || { icon: Clock, color: "text-slate-600", bg: "bg-slate-100", label: status }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("fr-CM", {
      day: "numeric",
      month: "short",
      year: "numeric"
    })
  }

  // Count leases needing landlord signature
  const pendingSignatureCount = leases.filter(
    l => l.status === "PENDING_SIGNATURES" && l.studentAcceptedTerms && !l.landlordAcceptedTerms
  ).length

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Leases" showBack />
      
      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto"
      >
        <PullToRefreshIndicator isRefreshing={isRefreshing} pullProgress={pullProgress} />
        
        <div className="p-4 space-y-4">
          {/* Pending Signature Alert */}
          {pendingSignatureCount > 0 && (
            <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                  <FileText className="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <p className="font-medium text-blue-900">
                    {pendingSignatureCount} lease{pendingSignatureCount > 1 ? 's' : ''} awaiting your signature
                  </p>
                  <p className="text-sm text-blue-700">
                    Students have accepted. Please review and sign.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Filters */}
          <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4">
            {filters.map((filter) => (
              <button
                key={filter.id}
                onClick={() => setStatusFilter(filter.id)}
                className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                  statusFilter === filter.id
                    ? "bg-blue-600 text-white"
                    : "bg-white text-slate-600 border border-slate-200"
                }`}
              >
                {filter.label}
              </button>
            ))}
          </div>

          {/* Leases List */}
          {isLoading ? (
            <div className="space-y-4">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-48 rounded-2xl" />
              ))}
            </div>
          ) : filteredLeases.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl">
              <FileText className="h-12 w-12 text-slate-300 mx-auto mb-4" />
              <p className="text-slate-600">No leases found</p>
            </div>
          ) : (
            <div className="space-y-4 pb-24">
              {filteredLeases.map((lease) => {
                const statusConfig = getStatusConfig(lease.status)
                const StatusIcon = statusConfig.icon
                const needsLandlordSignature = lease.status === "PENDING_SIGNATURES" && 
                  lease.studentAcceptedTerms && !lease.landlordAcceptedTerms

                return (
                  <div key={lease.id} className="bg-white rounded-2xl shadow-sm overflow-hidden">
                    {/* Header */}
                    <div className="p-4 border-b border-slate-100">
                      <div className="flex items-start justify-between">
                        <div className="flex items-center gap-3">
                          <div className="h-10 w-10 rounded-xl bg-slate-100 flex items-center justify-center">
                            <Home className="h-5 w-5 text-slate-600" />
                          </div>
                          <div>
                            <p className="font-semibold text-slate-900">{lease.listingTitle}</p>
                            <p className="text-xs text-slate-500">{lease.referenceCode}</p>
                          </div>
                        </div>
                        <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full ${statusConfig.bg}`}>
                          <StatusIcon className={`h-4 w-4 ${statusConfig.color}`} />
                          <span className={`text-xs font-medium ${statusConfig.color}`}>
                            {statusConfig.label}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Tenant Info */}
                    <div className="p-4 bg-slate-50">
                      <p className="text-xs text-slate-500 mb-2">Tenant</p>
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                          <User className="h-5 w-5 text-blue-600" />
                        </div>
                        <div className="flex-1">
                          <p className="font-medium text-slate-900">{lease.studentName}</p>
                          <div className="flex items-center gap-3 text-xs text-slate-500 mt-0.5">
                            <span className="flex items-center gap-1">
                              <Mail className="h-3 w-3" />
                              {lease.studentEmail}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Details */}
                    <div className="p-4 grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs text-slate-500">Duration</p>
                        <p className="font-medium text-slate-900">{lease.durationMonths} months</p>
                        <p className="text-xs text-slate-500 mt-0.5">
                          {formatDate(lease.startDate)} - {formatDate(lease.endDate)}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-500">Monthly Rent</p>
                        <p className="font-semibold text-slate-900">{formatCurrency(lease.monthlyRent)}</p>
                      </div>
                    </div>

                    {/* Signature Status */}
                    {lease.status === "PENDING_SIGNATURES" && (
                      <div className="px-4 pb-2">
                        <div className="flex items-center gap-4 text-xs">
                          <span className={`flex items-center gap-1 ${lease.studentAcceptedTerms ? 'text-green-600' : 'text-slate-400'}`}>
                            {lease.studentAcceptedTerms ? <CheckCircle className="h-3 w-3" /> : <Clock className="h-3 w-3" />}
                            Student {lease.studentAcceptedTerms ? 'signed' : 'pending'}
                          </span>
                          <span className={`flex items-center gap-1 ${lease.landlordAcceptedTerms ? 'text-green-600' : 'text-slate-400'}`}>
                            {lease.landlordAcceptedTerms ? <CheckCircle className="h-3 w-3" /> : <Clock className="h-3 w-3" />}
                            You {lease.landlordAcceptedTerms ? 'signed' : 'pending'}
                          </span>
                        </div>
                      </div>
                    )}

                    {/* Actions */}
                    {needsLandlordSignature && (
                      <div className="px-4 pb-4">
                        <Link href={`/leases/${lease.id}/sign`}>
                          <Button className="w-full rounded-xl bg-green-600 hover:bg-green-700">
                            <PenTool className="h-4 w-4 mr-2" />
                            Sign Lease Agreement
                          </Button>
                        </Link>
                      </div>
                    )}

                    {lease.status === "PENDING_SIGNATURES" && !lease.studentAcceptedTerms && (
                      <div className="px-4 pb-4">
                        <div className="text-center text-sm text-slate-500 py-2 bg-slate-50 rounded-xl">
                          Waiting for tenant to sign first...
                        </div>
                      </div>
                    )}

                    {/* Download PDF for active leases */}
                    {lease.status === "ACTIVE" && (
                      <div className="px-4 pb-4">
                        <Button 
                          variant="outline"
                          className="w-full rounded-xl"
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
                              document.body.removeChild(link)
                              window.URL.revokeObjectURL(url)
                            } catch (err) {
                              console.error('Failed to download lease document:', err)
                            }
                          }}
                        >
                          <Download className="h-4 w-4 mr-2" />
                          Download Lease Agreement
                        </Button>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
