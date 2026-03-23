"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  CreditCard, 
  TrendingUp,
  Calendar,
  CheckCircle,
  Clock,
  AlertCircle,
  Download,
  Filter,
  ChevronRight,
  Home
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import api from "@/lib/api"

interface Payment {
  id: string
  amount: number
  status: string
  paymentMethod: string
  payerName?: string
  payerId?: string
  recipientName?: string
  leaseId?: string
  leaseReferenceCode?: string
  listingTitle?: string
  momoTransactionId?: string
  createdAt: string
  // Legacy nested fields (for backwards compatibility)
  tenant?: {
    id: string
    firstName: string
    lastName: string
  }
  listing?: {
    id: string
    title: string
  }
  paymentDate?: string
  transactionId?: string
}

interface PaymentStats {
  totalReceived: number
  pendingAmount: number
  thisMonth: number
  lastMonth: number
}

export default function LandlordPaymentsPage() {
  const { formatCurrency } = useLanguage()
  const { user } = useAuth()
  const [payments, setPayments] = useState<Payment[]>([])
  const [stats, setStats] = useState<PaymentStats | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<string>("ALL")

  const fetchPayments = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      // Use the correct endpoint: /api/payments/my (works for both students and landlords)
      const paymentsRes = await api.get(`/payments/my`)
      const paymentsData = paymentsRes.data?.content || paymentsRes.data || []
      setPayments(paymentsData)
      
      // Calculate stats from payments data
      const completed = paymentsData.filter((p: Payment) => p.status === "VERIFIED" || p.status === "COMPLETED")
      const pending = paymentsData.filter((p: Payment) => p.status === "PENDING" || p.status === "SUBMITTED")
      const now = new Date()
      const thisMonthStart = new Date(now.getFullYear(), now.getMonth(), 1)
      const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      
      const thisMonthPayments = completed.filter((p: Payment) => new Date(p.createdAt || p.paymentDate || Date.now()) >= thisMonthStart)
      const lastMonthPayments = completed.filter((p: Payment) => {
        const date = new Date(p.createdAt || p.paymentDate || Date.now())
        return date >= lastMonthStart && date < thisMonthStart
      })
      
      setStats({
        totalReceived: completed.reduce((sum: number, p: Payment) => sum + p.amount, 0),
        pendingAmount: pending.reduce((sum: number, p: Payment) => sum + p.amount, 0),
        thisMonth: thisMonthPayments.reduce((sum: number, p: Payment) => sum + p.amount, 0),
        lastMonth: lastMonthPayments.reduce((sum: number, p: Payment) => sum + p.amount, 0)
      })
    } catch (err) {
      console.error("Failed to fetch payments:", err)
      setPayments([])
      setStats({
        totalReceived: 0,
        pendingAmount: 0,
        thisMonth: 0,
        lastMonth: 0
      })
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchPayments()
  }, [fetchPayments])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchPayments,
  })

  const filteredPayments = payments.filter(payment => 
    statusFilter === "ALL" || payment.status === statusFilter
  )

  const getStatusIcon = (status: string) => {
    switch (status) {
      case "VERIFIED":
      case "COMPLETED": return <CheckCircle className="h-4 w-4 text-emerald-500" />
      case "PENDING":
      case "SUBMITTED": return <Clock className="h-4 w-4 text-amber-500" />
      case "REJECTED":
      case "FAILED": return <AlertCircle className="h-4 w-4 text-red-500" />
      default: return null
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case "VERIFIED":
      case "COMPLETED": return "bg-emerald-100 text-emerald-700"
      case "PENDING":
      case "SUBMITTED": return "bg-amber-100 text-amber-700"
      case "REJECTED":
      case "FAILED": return "bg-red-100 text-red-700"
      default: return "bg-slate-100 text-slate-600"
    }
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Payments" />

      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        <div className="p-4 space-y-4">
          {/* Stats Cards */}
          {isLoading ? (
            <div className="grid grid-cols-2 gap-3">
              {[1, 2, 3, 4].map((i) => (
                <Skeleton key={i} className="h-24 rounded-2xl" />
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              <div className="bg-gradient-to-br from-emerald-500 to-emerald-600 rounded-2xl p-4 text-white">
                <CreditCard className="h-6 w-6 mb-2 opacity-80" />
                <p className="text-2xl font-bold">{formatCurrency(stats?.totalReceived || 0)}</p>
                <p className="text-xs opacity-80">Total Received</p>
              </div>
              <div className="bg-gradient-to-br from-amber-500 to-amber-600 rounded-2xl p-4 text-white">
                <Clock className="h-6 w-6 mb-2 opacity-80" />
                <p className="text-2xl font-bold">{formatCurrency(stats?.pendingAmount || 0)}</p>
                <p className="text-xs opacity-80">Pending</p>
              </div>
              <div className="bg-white rounded-2xl p-4 shadow-sm">
                <TrendingUp className="h-5 w-5 text-blue-600 mb-1" />
                <p className="text-xl font-bold text-slate-900">{formatCurrency(stats?.thisMonth || 0)}</p>
                <p className="text-xs text-slate-500">This Month</p>
              </div>
              <div className="bg-white rounded-2xl p-4 shadow-sm">
                <Calendar className="h-5 w-5 text-slate-400 mb-1" />
                <p className="text-xl font-bold text-slate-900">{formatCurrency(stats?.lastMonth || 0)}</p>
                <p className="text-xs text-slate-500">Last Month</p>
              </div>
            </div>
          )}

          {/* Filter */}
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-slate-900">Payment History</h2>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm" className="rounded-lg">
                  <Filter className="h-4 w-4 mr-1" />
                  {statusFilter === "ALL" ? "All" : statusFilter}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => setStatusFilter("ALL")}>All</DropdownMenuItem>
                <DropdownMenuItem onClick={() => setStatusFilter("VERIFIED")}>Verified</DropdownMenuItem>
                <DropdownMenuItem onClick={() => setStatusFilter("PENDING")}>Pending</DropdownMenuItem>
                <DropdownMenuItem onClick={() => setStatusFilter("SUBMITTED")}>Awaiting Verification</DropdownMenuItem>
                <DropdownMenuItem onClick={() => setStatusFilter("REJECTED")}>Rejected</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>

          {/* Payments List */}
          {isLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-20 rounded-2xl" />
              ))}
            </div>
          ) : filteredPayments.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl">
              <div className="mx-auto mb-3 h-14 w-14 rounded-full bg-slate-100 flex items-center justify-center">
                <CreditCard className="h-7 w-7 text-slate-400" />
              </div>
              <h3 className="font-semibold text-slate-900 mb-1">No Payments Yet</h3>
              <p className="text-slate-500 text-sm">
                Payments from your tenants will appear here
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {filteredPayments.map((payment) => (
                <div key={payment.id} className="bg-white rounded-xl p-4 shadow-sm">
                  <div className="flex items-start justify-between">
                    <div className="flex items-start gap-3">
                      <div className="h-10 w-10 rounded-full bg-slate-100 flex items-center justify-center">
                        <Home className="h-5 w-5 text-slate-500" />
                      </div>
                      <div>
                        <p className="font-medium text-slate-900">
                          {payment.payerName || (payment.tenant ? `${payment.tenant.firstName} ${payment.tenant.lastName}` : "Unknown")}
                        </p>
                        <p className="text-xs text-slate-500 line-clamp-1">
                          {payment.leaseReferenceCode || payment.listingTitle || payment.listing?.title || "Lease Payment"}
                        </p>
                        <p className="text-xs text-slate-400 mt-1">
                          {new Date(payment.createdAt || payment.paymentDate || Date.now()).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-slate-900">
                        {formatCurrency(payment.amount)}
                      </p>
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium mt-1 ${getStatusColor(payment.status)}`}>
                        {getStatusIcon(payment.status)}
                        {payment.status}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
