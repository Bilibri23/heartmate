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
import { uploadApi } from "@/lib/api"

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
  const [phoneNumber, setPhoneNumber] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [currentLeaseId, setCurrentLeaseId] = useState<string | null>(null)
  const [cancelPaymentId, setCancelPaymentId] = useState<string | null>(null)

  const fetchPayments = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await uploadApi.get("/payments/my")
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

  // Handle leaseId param - wait for payments to load first
  useEffect(() => {
    if (leaseIdParam && !isLoading && payments.length >= 0) {
      // Check if we already have a pending/submitted payment for this lease
      const existingPayment = payments.find(
        (p) => p.leaseId === leaseIdParam && 
               (p.status === "PENDING" || p.status === "SUBMITTED")
      )
      
      if (existingPayment) {
        // Show existing payment instead of creating new one
        setCurrentLeaseId(leaseIdParam)
        if (existingPayment.status === "PENDING") {
          setPaymentInstructions({
            amount: existingPayment.amount,
            mtnNumber: "677000000",
            orangeNumber: "699000000",
            recipientName: "RoomBuddy",
            reference: existingPayment.id,
          })
          setIsPaySheetOpen(true)
        }
      } else if (!isPaySheetOpen) {
        // No existing payment, initiate new one
        setCurrentLeaseId(leaseIdParam)
        initiatePayment(leaseIdParam)
      }
    }
  }, [leaseIdParam, isLoading, payments])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchPayments,
  })

  const [paymentError, setPaymentError] = useState<string | null>(null)

  const initiatePayment = async (leaseId: string) => {
    setPaymentError(null)
    setCurrentLeaseId(leaseId)

    const existingPayment = payments.find(
      (payment) =>
        payment.leaseId === leaseId &&
        (payment.status === "PENDING" || payment.status === "SUBMITTED")
    )
    if (existingPayment) {
      if (existingPayment.status === "PENDING") {
        setPaymentInstructions({
          amount: existingPayment.amount,
          mtnNumber: "677000000",
          orangeNumber: "699000000",
          recipientName: "RoomBuddy",
          reference: existingPayment.id,
        })
        setIsPaySheetOpen(true)
      } else {
        setPaymentError("Payment already submitted. Please wait for verification.")
      }
      return
    }

    try {
      const response = await uploadApi.post(`/payments/initiate/${leaseId}`, null, {
        validateStatus: (status) => status < 500,
      })

      if (response.status >= 400) {
        const errorMessage = response.data?.message || "Failed to initiate payment"
        if (errorMessage.toLowerCase().includes("not awaiting payment")) {
          setPaymentError("This lease is not awaiting payment.")
        } else if (errorMessage.toLowerCase().includes("already submitted")) {
          setPaymentError("Payment already submitted. Please wait for verification.")
        } else if (errorMessage.toLowerCase().includes("already pending")) {
          setPaymentError("You have a pending payment for this lease.")
        } else {
          setPaymentError(errorMessage)
        }
        return
      }

      setPaymentInstructions(response.data)
      setIsPaySheetOpen(true)
    } catch (err: any) {
      console.error("Failed to initiate payment:", err)
      const errorMessage = err.response?.data?.message || "Failed to initiate payment"
      if (errorMessage.includes("already pending")) {
        // A payment is already pending - fetch existing payment and show it
        setPaymentError("You have a pending payment for this lease. Please complete it or wait for confirmation.")
        // Try to get existing pending payment
        try {
          const existingPayment = payments.find(p => p.leaseId === leaseId && p.status === "PENDING")
          if (existingPayment) {
            setPaymentInstructions({
              amount: existingPayment.amount,
              mtnNumber: "677000000",
              orangeNumber: "699000000",
              recipientName: "RoomBuddy",
              reference: existingPayment.id,
            })
            setIsPaySheetOpen(true)
          }
        } catch (e) {
          // Ignore
        }
      } else {
        setPaymentError(errorMessage)
      }
    }
  }

  const submitPaymentProof = async () => {
    if (!paymentInstructions || !transactionId) return

    setIsSubmitting(true)
    try {
      // Map frontend method names to backend enum values
      const methodMap: Record<string, string> = {
        mtn: "MTN_MOMO",
        orange: "ORANGE_MONEY",
        card: "BANK_TRANSFER",
      }
      await uploadApi.post("/payments/submit", {
        leaseId: currentLeaseId || leaseIdParam,
        momoTransactionId: transactionId,
        momoPhoneNumber: phoneNumber,
        paymentMethod: methodMap[selectedMethod] || selectedMethod.toUpperCase(),
      })
      setIsPaySheetOpen(false)
      setTransactionId("")
      setPhoneNumber("")
      fetchPayments()
    } catch (err) {
      console.error("Failed to submit payment:", err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleCancelPayment = async (paymentId: string) => {
    try {
      await uploadApi.delete(`/payments/${paymentId}`)
      setCancelPaymentId(null)
      fetchPayments()
    } catch (err) {
      console.error("Failed to cancel payment:", err)
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

                {payment.status === "PENDING" && (
                  <div className="flex gap-2 mt-3">
                    <Button 
                      size="sm" 
                      className="flex-1 rounded-xl"
                      onClick={() => {
                        setCurrentLeaseId(payment.leaseId)
                        setPaymentInstructions({
                          amount: payment.amount,
                          mtnNumber: "677000000",
                          orangeNumber: "699000000",
                          recipientName: "RoomBuddy",
                          reference: payment.id,
                        })
                        setIsPaySheetOpen(true)
                      }}
                    >
                      Complete Payment
                    </Button>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      className="rounded-xl text-red-600 border-red-200 hover:bg-red-50"
                      onClick={() => setCancelPaymentId(payment.id)}
                    >
                      Cancel
                    </Button>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Payment Sheet */}
      <Sheet open={isPaySheetOpen} onOpenChange={setIsPaySheetOpen}>
        <SheetContent side="bottom" className="max-h-[90vh] rounded-t-3xl overflow-hidden flex flex-col">
          <SheetHeader className="flex-shrink-0">
            <SheetTitle>{t.payments.selectMethod}</SheetTitle>
          </SheetHeader>

          {paymentInstructions && (
            <div className="mt-4 space-y-4 overflow-y-auto flex-1 pb-4">
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
                  <span
                    role="button"
                    tabIndex={0}
                    onClick={(e) => {
                      e.stopPropagation()
                      copyToClipboard(paymentInstructions.mtnNumber)
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault()
                        e.stopPropagation()
                        copyToClipboard(paymentInstructions.mtnNumber)
                      }
                    }}
                    className="p-2 cursor-pointer"
                  >
                    <Copy className="h-4 w-4 text-slate-400" />
                  </span>
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
                  <span
                    role="button"
                    tabIndex={0}
                    onClick={(e) => {
                      e.stopPropagation()
                      copyToClipboard(paymentInstructions.orangeNumber)
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault()
                        e.stopPropagation()
                        copyToClipboard(paymentInstructions.orangeNumber)
                      }
                    }}
                    className="p-2 cursor-pointer"
                  >
                    <Copy className="h-4 w-4 text-slate-400" />
                  </span>
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
                      Your Phone Number
                    </Label>
                    <Input
                      placeholder="e.g., 677123456"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      className="h-12 rounded-xl"
                      type="tel"
                    />
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
                    disabled={isSubmitting || !transactionId || !phoneNumber}
                  >
                    {isSubmitting ? t.common.loading : t.common.submit}
                  </Button>
                </div>
              )}

              {/* Card Payment (manual verification for now) */}
              {selectedMethod === "card" && (
                <div className="space-y-4">
                  <div className="bg-blue-50 rounded-2xl p-4">
                    <p className="text-sm text-slate-600 mb-2">
                      <strong>Card Payment Instructions:</strong>
                    </p>
                    <ol className="text-sm text-slate-600 space-y-1 list-decimal list-inside">
                      <li>Make a bank transfer to our account</li>
                      <li>Use reference: {paymentInstructions.reference}</li>
                      <li>Amount: {formatCurrency(paymentInstructions.amount)}</li>
                      <li>Enter your transaction/reference number below</li>
                    </ol>
                    <div className="mt-3 p-3 bg-white rounded-xl">
                      <p className="text-xs text-slate-500">Bank Details:</p>
                      <p className="text-sm font-medium">RoomBuddy SARL</p>
                      <p className="text-sm text-slate-600">Account: XXXX-XXXX-XXXX</p>
                    </div>
                  </div>

                  <div>
                    <Label className="text-slate-700 mb-2 block">
                      Your Phone Number
                    </Label>
                    <Input
                      placeholder="e.g., 677123456"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      className="h-12 rounded-xl"
                      type="tel"
                    />
                  </div>

                  <div>
                    <Label className="text-slate-700 mb-2 block">
                      Transaction Reference
                    </Label>
                    <Input
                      placeholder="e.g., REF123456789"
                      value={transactionId}
                      onChange={(e) => setTransactionId(e.target.value)}
                      className="h-12 rounded-xl"
                    />
                  </div>

                  <Button
                    className="w-full h-12 rounded-xl"
                    onClick={submitPaymentProof}
                    disabled={isSubmitting || !transactionId || !phoneNumber}
                  >
                    {isSubmitting ? t.common.loading : t.common.submit}
                  </Button>
                </div>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>

      {/* Cancel Confirmation Dialog */}
      {cancelPaymentId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-2xl p-6 mx-4 max-w-sm w-full shadow-xl">
            <h3 className="text-lg font-semibold text-slate-900 mb-2">Cancel Payment?</h3>
            <p className="text-sm text-slate-600 mb-6">
              Are you sure you want to cancel this payment? This action cannot be undone.
            </p>
            <div className="flex gap-3">
              <Button
                variant="outline"
                className="flex-1 rounded-xl"
                onClick={() => setCancelPaymentId(null)}
              >
                Keep Payment
              </Button>
              <Button
                className="flex-1 rounded-xl bg-red-600 hover:bg-red-700"
                onClick={() => handleCancelPayment(cancelPaymentId)}
              >
                Cancel Payment
              </Button>
            </div>
          </div>
        </div>
      )}
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
