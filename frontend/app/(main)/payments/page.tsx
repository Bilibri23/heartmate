"use client"

import { useState, useEffect, useCallback, Suspense } from "react"
import { useSearchParams } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  CreditCard, 
  Smartphone, 
  CheckCircle, 
  Clock, 
  XCircle,
  Upload,
  Copy,
  ChevronRight
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import api from "@/lib/api"

interface Payment {
  id: string
  leaseId: string
  listingTitle: string
  amount: number
  status: "PENDING" | "SUBMITTED" | "VERIFIED" | "REJECTED"
  paymentMethod: string
  transactionId: string | null
  proofUrl: string | null
  createdAt: string
  verifiedAt: string | null
}

interface PaymentInstructions {
  mtnNumber: string
  orangeNumber: string
  recipientName: string
  amount: number
  reference: string
}

const STATUS_CONFIG = {
  PENDING: { 
    icon: Clock, 
    color: "text-amber-600", 
    bg: "bg-amber-50",
    label: "Pending"
  },
  SUBMITTED: { 
    icon: Clock, 
    color: "text-blue-600", 
    bg: "bg-blue-50",
    label: "Verifying"
  },
  VERIFIED: { 
    icon: CheckCircle, 
    color: "text-emerald-600", 
    bg: "bg-emerald-50",
    label: "Verified"
  },
  REJECTED: { 
    icon: XCircle, 
    color: "text-red-600", 
    bg: "bg-red-50",
    label: "Rejected"
  },
}

function PaymentsContent() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const searchParams = useSearchParams()
  const leaseIdParam = searchParams.get("leaseId")
  
  const [payments, setPayments] = useState<Payment[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isPaySheetOpen, setIsPaySheetOpen] = useState(false)
  const [paymentInstructions, setPaymentInstructions] = useState<PaymentInstructions | null>(null)
  const [selectedMethod, setSelectedMethod] = useState<"mtn" | "orange" | "card">("mtn")
  const [transactionId, setTransactionId] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const fetchPayments = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await api.get("/payments/my")
      const content = response.data?.content || response.data || []
      setPayments(content)
    } catch (err) {
      console.error("Failed to fetch payments:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchPayments()
  }, [fetchPayments])

  useEffect(() => {
    if (leaseIdParam) {
      initiatePayment(leaseIdParam)
    }
  }, [leaseIdParam])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchPayments,
  })

  const initiatePayment = async (leaseId: string) => {
    try {
      const response = await api.post(`/payments/initiate/${leaseId}`)
      setPaymentInstructions(response.data)
      setIsPaySheetOpen(true)
    } catch (err) {
      console.error("Failed to initiate payment:", err)
    }
  }

  const submitPaymentProof = async () => {
    if (!paymentInstructions || !transactionId) return

    setIsSubmitting(true)
    try {
      await api.post("/payments/submit", {
        leaseId: leaseIdParam,
        transactionId,
        paymentMethod: selectedMethod.toUpperCase(),
      })
      setIsPaySheetOpen(false)
      setTransactionId("")
      fetchPayments()
    } catch (err) {
      console.error("Failed to submit payment:", err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("fr-CM", {
      day: "numeric",
      month: "short",
      year: "numeric"
    })
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.payments.title} />

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
          {/* Loading */}
          {isLoading && (
            <>
              {[1, 2, 3].map((i) => (
                <div key={i} className="bg-white rounded-2xl p-4 shadow-sm">
                  <Skeleton className="h-5 w-32 mb-2" />
                  <Skeleton className="h-4 w-full mb-2" />
                  <Skeleton className="h-4 w-24" />
                </div>
              ))}
            </>
          )}

          {/* Empty State */}
          {!isLoading && payments.length === 0 && (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">💳</div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                No payments yet
              </h3>
              <p className="text-slate-500 text-sm">
                Your payment history will appear here.
              </p>
            </div>
          )}

          {/* Payments List */}
          {!isLoading && payments.map((payment) => {
            const statusConfig = STATUS_CONFIG[payment.status]
            const StatusIcon = statusConfig.icon

            return (
              <div 
                key={payment.id} 
                className="bg-white rounded-2xl shadow-sm p-4"
              >
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h3 className="font-semibold text-slate-900">
                      {formatCurrency(payment.amount)}
                    </h3>
                    <p className="text-sm text-slate-500">
                      {payment.listingTitle}
                    </p>
                  </div>
                  <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full ${statusConfig.bg}`}>
                    <StatusIcon className={`h-4 w-4 ${statusConfig.color}`} />
                    <span className={`text-xs font-medium ${statusConfig.color}`}>
                      {statusConfig.label}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span>{formatDate(payment.createdAt)}</span>
                  {payment.transactionId && (
                    <span>ID: {payment.transactionId}</span>
                  )}
                </div>

                {payment.status === "REJECTED" && (
                  <Button 
                    variant="outline" 
                    size="sm" 
                    className="w-full mt-3 rounded-xl"
                    onClick={() => initiatePayment(payment.leaseId)}
                  >
                    Retry Payment
                  </Button>
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Payment Sheet */}
      <Sheet open={isPaySheetOpen} onOpenChange={setIsPaySheetOpen}>
        <SheetContent side="bottom" className="h-[85vh] rounded-t-3xl">
          <SheetHeader>
            <SheetTitle>{t.payments.selectMethod}</SheetTitle>
          </SheetHeader>

          {paymentInstructions && (
            <div className="mt-6 space-y-6">
              {/* Amount */}
              <div className="text-center py-4 bg-blue-50 rounded-2xl">
                <p className="text-sm text-blue-600 mb-1">{t.payments.amount}</p>
                <p className="text-3xl font-bold text-blue-700">
                  {formatCurrency(paymentInstructions.amount)}
                </p>
              </div>

              {/* Payment Methods */}
              <div className="space-y-3">
                <button
                  onClick={() => setSelectedMethod("mtn")}
                  className={`w-full flex items-center gap-3 p-4 rounded-2xl border-2 transition-colors ${
                    selectedMethod === "mtn" 
                      ? "border-amber-500 bg-amber-50" 
                      : "border-slate-200"
                  }`}
                >
                  <div className="h-12 w-12 rounded-xl bg-amber-500 flex items-center justify-center">
                    <Smartphone className="h-6 w-6 text-white" />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="font-semibold text-slate-900">{t.payments.mtn}</p>
                    <p className="text-sm text-slate-500">{paymentInstructions.mtnNumber}</p>
                  </div>
                  <button 
                    onClick={(e) => {
                      e.stopPropagation()
                      copyToClipboard(paymentInstructions.mtnNumber)
                    }}
                    className="p-2"
                  >
                    <Copy className="h-4 w-4 text-slate-400" />
                  </button>
                </button>

                <button
                  onClick={() => setSelectedMethod("orange")}
                  className={`w-full flex items-center gap-3 p-4 rounded-2xl border-2 transition-colors ${
                    selectedMethod === "orange" 
                      ? "border-orange-500 bg-orange-50" 
                      : "border-slate-200"
                  }`}
                >
                  <div className="h-12 w-12 rounded-xl bg-orange-500 flex items-center justify-center">
                    <Smartphone className="h-6 w-6 text-white" />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="font-semibold text-slate-900">{t.payments.orange}</p>
                    <p className="text-sm text-slate-500">{paymentInstructions.orangeNumber}</p>
                  </div>
                  <button 
                    onClick={(e) => {
                      e.stopPropagation()
                      copyToClipboard(paymentInstructions.orangeNumber)
                    }}
                    className="p-2"
                  >
                    <Copy className="h-4 w-4 text-slate-400" />
                  </button>
                </button>

                <button
                  onClick={() => setSelectedMethod("card")}
                  className={`w-full flex items-center gap-3 p-4 rounded-2xl border-2 transition-colors ${
                    selectedMethod === "card" 
                      ? "border-blue-500 bg-blue-50" 
                      : "border-slate-200"
                  }`}
                >
                  <div className="h-12 w-12 rounded-xl bg-blue-600 flex items-center justify-center">
                    <CreditCard className="h-6 w-6 text-white" />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="font-semibold text-slate-900">{t.payments.card}</p>
                    <p className="text-sm text-slate-500">Visa, Mastercard</p>
                  </div>
                  <ChevronRight className="h-5 w-5 text-slate-400" />
                </button>
              </div>

              {/* Transaction ID Input (for Mobile Money) */}
              {(selectedMethod === "mtn" || selectedMethod === "orange") && (
                <div className="space-y-4">
                  <div className="bg-slate-50 rounded-2xl p-4">
                    <p className="text-sm text-slate-600 mb-2">
                      <strong>Instructions:</strong>
                    </p>
                    <ol className="text-sm text-slate-600 space-y-1 list-decimal list-inside">
                      <li>Dial *126# (MTN) or #150# (Orange)</li>
                      <li>Select "Transfer Money"</li>
                      <li>Enter the number above</li>
                      <li>Enter amount: {formatCurrency(paymentInstructions.amount)}</li>
                      <li>Confirm and note your transaction ID</li>
                    </ol>
                  </div>

                  <div>
                    <Label className="text-slate-700 mb-2 block">
                      {t.payments.transactionId}
                    </Label>
                    <Input
                      placeholder="e.g., TXN123456789"
                      value={transactionId}
                      onChange={(e) => setTransactionId(e.target.value)}
                      className="h-12 rounded-xl"
                    />
                  </div>

                  <Button
                    className="w-full h-12 rounded-xl"
                    onClick={submitPaymentProof}
                    disabled={isSubmitting || !transactionId}
                  >
                    {isSubmitting ? t.common.loading : t.common.submit}
                  </Button>
                </div>
              )}

              {/* Card Payment (placeholder) */}
              {selectedMethod === "card" && (
                <div className="text-center py-8">
                  <p className="text-slate-500">
                    Card payment integration coming soon.
                  </p>
                </div>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}

export default function PaymentsPage() {
  return (
    <Suspense fallback={
      <div className="flex flex-col min-h-screen bg-slate-50">
        <div className="h-14 bg-white border-b border-slate-200" />
        <div className="flex-1 flex items-center justify-center">
          <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" />
        </div>
      </div>
    }>
      <PaymentsContent />
    </Suspense>
  )
}
