"use client"

import { useState, useEffect, use } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { SignaturePad, SignatureData } from "@/components/ui/signature-pad"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { 
  FileText, 
  CheckCircle, 
  Clock, 
  AlertTriangle,
  Download,
  User,
  Calendar
} from "lucide-react"
import api, { uploadApi } from "@/lib/api"
import { toast } from "sonner"

interface Lease {
  id: string
  referenceCode: string
  listingTitle: string
  listingAddress: string
  studentId: string
  studentName: string
  landlordId: string
  landlordName: string
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
  termsContent?: string
}

export default function LeaseSignPage({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = use(params)
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  
  const [lease, setLease] = useState<Lease | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSigning, setIsSigning] = useState(false)
  const [showTerms, setShowTerms] = useState(false)
  const [hasReadTerms, setHasReadTerms] = useState(false)

  useEffect(() => {
    const fetchLease = async () => {
      try {
        const response = await api.get(`/leases/${resolvedParams.id}`)
        setLease(response.data)
      } catch (err) {
        console.error("Failed to fetch lease:", err)
        toast.error("Failed to load lease details")
      } finally {
        setIsLoading(false)
      }
    }

    fetchLease()
  }, [resolvedParams.id])

  const handleSignatureComplete = async (signatureData: SignatureData) => {
    if (!lease || !user) return
    
    setIsSigning(true)
    try {
      // First, save the signature
      await uploadApi.post(`/leases/${lease.id}/signature`, {
        signatureData: signatureData.data,
        signatureType: signatureData.type,
        timestamp: signatureData.timestamp,
        signerName: signatureData.signerName,
      })
      
      // Then accept the terms
      await api.post(`/leases/${lease.id}/accept-terms`)
      
      toast.success("Lease signed successfully!")
      router.push("/leases")
    } catch (err: any) {
      console.error("Failed to sign lease:", err)
      toast.error(err.response?.data?.message || "Failed to sign lease")
    } finally {
      setIsSigning(false)
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("fr-CM", {
      day: "numeric",
      month: "long",
      year: "numeric"
    })
  }

  const isStudent = user?.id === lease?.studentId
  const isLandlord = user?.id === lease?.landlordId
  const hasAlreadySigned = isStudent ? lease?.studentAcceptedTerms : lease?.landlordAcceptedTerms
  const otherPartySigned = isStudent ? lease?.landlordAcceptedTerms : lease?.studentAcceptedTerms
  const canSign = lease?.status === "PENDING_SIGNATURES" && !hasAlreadySigned

  if (isLoading) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Sign Lease" showBack />
        <div className="flex-1 p-4 space-y-4">
          <Skeleton className="h-32 w-full rounded-2xl" />
          <Skeleton className="h-48 w-full rounded-2xl" />
          <Skeleton className="h-64 w-full rounded-2xl" />
        </div>
      </div>
    )
  }

  if (!lease) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Sign Lease" showBack />
        <div className="flex-1 flex items-center justify-center p-4">
          <div className="text-center">
            <AlertTriangle className="h-12 w-12 text-amber-500 mx-auto mb-4" />
            <h2 className="text-lg font-semibold text-slate-900">Lease not found</h2>
            <p className="text-slate-500 mt-2">This lease may have been deleted or you don't have access.</p>
            <Button onClick={() => router.push("/leases")} className="mt-4">
              Back to Leases
            </Button>
          </div>
        </div>
      </div>
    )
  }

  if (lease.status !== "PENDING_SIGNATURES") {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Sign Lease" showBack />
        <div className="flex-1 flex items-center justify-center p-4">
          <div className="text-center">
            <AlertTriangle className="h-12 w-12 text-amber-500 mx-auto mb-4" />
            <h2 className="text-lg font-semibold text-slate-900">Cannot sign this lease</h2>
            <p className="text-slate-500 mt-2">
              {lease.status === "PENDING_PAYMENT" 
                ? "Payment must be verified before signing."
                : "This lease is not awaiting signatures."}
            </p>
            <Button onClick={() => router.push("/leases")} className="mt-4">
              Back to Leases
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Sign Lease Agreement" showBack />

      <div className="flex-1 overflow-y-auto p-4 space-y-4 pb-8">
        {/* Lease Summary */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <div className="flex items-center gap-3 mb-4">
            <div className="h-12 w-12 rounded-xl bg-purple-100 flex items-center justify-center">
              <FileText className="h-6 w-6 text-purple-600" />
            </div>
            <div>
              <h2 className="font-semibold text-slate-900">{lease.listingTitle}</h2>
              <p className="text-sm text-slate-500">Ref: {lease.referenceCode}</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-slate-50 rounded-xl p-3">
              <p className="text-slate-500">Monthly Rent</p>
              <p className="font-semibold text-slate-900">{formatCurrency(lease.monthlyRent)}</p>
            </div>
            <div className="bg-slate-50 rounded-xl p-3">
              <p className="text-slate-500">Deposit</p>
              <p className="font-semibold text-slate-900">{formatCurrency(lease.depositAmount)}</p>
            </div>
            <div className="bg-slate-50 rounded-xl p-3">
              <p className="text-slate-500">Start Date</p>
              <p className="font-semibold text-slate-900">{formatDate(lease.startDate)}</p>
            </div>
            <div className="bg-slate-50 rounded-xl p-3">
              <p className="text-slate-500">End Date</p>
              <p className="font-semibold text-slate-900">{formatDate(lease.endDate)}</p>
            </div>
          </div>
        </div>

        {/* Parties */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-3">Contract Parties</h3>
          
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                  <User className="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <p className="font-medium text-slate-900">{lease.landlordName}</p>
                  <p className="text-xs text-slate-500">Landlord</p>
                </div>
              </div>
              {lease.landlordAcceptedTerms ? (
                <div className="flex items-center gap-1 text-green-600">
                  <CheckCircle className="h-5 w-5" />
                  <span className="text-xs">Signed</span>
                </div>
              ) : (
                <div className="flex items-center gap-1 text-amber-600">
                  <Clock className="h-5 w-5" />
                  <span className="text-xs">Pending</span>
                </div>
              )}
            </div>

            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-full bg-emerald-100 flex items-center justify-center">
                  <User className="h-5 w-5 text-emerald-600" />
                </div>
                <div>
                  <p className="font-medium text-slate-900">{lease.studentName}</p>
                  <p className="text-xs text-slate-500">Tenant</p>
                </div>
              </div>
              {lease.studentAcceptedTerms ? (
                <div className="flex items-center gap-1 text-green-600">
                  <CheckCircle className="h-5 w-5" />
                  <span className="text-xs">Signed</span>
                </div>
              ) : (
                <div className="flex items-center gap-1 text-amber-600">
                  <Clock className="h-5 w-5" />
                  <span className="text-xs">Pending</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Terms and Conditions */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-slate-900">Terms & Conditions</h3>
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => setShowTerms(!showTerms)}
            >
              {showTerms ? "Hide" : "Read"}
            </Button>
          </div>
          
          {showTerms && (
            <div className="bg-slate-50 rounded-xl p-4 max-h-64 overflow-y-auto text-sm text-slate-700 whitespace-pre-wrap">
              {lease.termsContent || `RENTAL AGREEMENT

This agreement is made between the Landlord and the Tenant for the property listed.

TERMS AND CONDITIONS:

1. RENT PAYMENT
   - The Tenant agrees to pay rent of ${formatCurrency(lease.monthlyRent)} per month
   - Rent is due on the 1st of each month
   - Late payment may incur additional fees

2. SECURITY DEPOSIT
   - A deposit of ${formatCurrency(lease.depositAmount)} has been paid
   - This will be refunded within 30 days of move-out, minus any damages

3. PROPERTY CARE
   - The Tenant agrees to maintain the property in good condition
   - Any damages beyond normal wear must be reported immediately
   
4. TERMINATION
   - Either party may terminate with 30 days written notice
   - Early termination may result in forfeiture of deposit

5. HOUSE RULES
   - The Tenant agrees to follow all house rules set by the Landlord
   - Subletting is not permitted without written consent

By signing this agreement, both parties acknowledge they have read, understood, and agree to all terms.`}
            </div>
          )}

          {showTerms && (
            <label className="flex items-center gap-2 mt-4 cursor-pointer">
              <input
                type="checkbox"
                checked={hasReadTerms}
                onChange={(e) => setHasReadTerms(e.target.checked)}
                className="h-5 w-5 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-sm text-slate-700">
                I have read and understood the terms and conditions
              </span>
            </label>
          )}
        </div>

        {/* Already Signed Message */}
        {hasAlreadySigned && (
          <div className="bg-green-50 rounded-2xl p-4">
            <div className="flex items-center gap-3">
              <CheckCircle className="h-8 w-8 text-green-600" />
              <div>
                <h3 className="font-semibold text-green-900">You've Already Signed</h3>
                <p className="text-sm text-green-700">
                  {otherPartySigned 
                    ? "Both parties have signed. The lease is now active!"
                    : "Waiting for the other party to sign..."}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Signature Pad */}
        {canSign && (
          <div className="bg-white rounded-2xl p-4 shadow-sm">
            <h3 className="font-semibold text-slate-900 mb-4">Your Signature</h3>
            
            {!hasReadTerms ? (
              <div className="text-center py-8 text-slate-500">
                <FileText className="h-12 w-12 mx-auto mb-3 text-slate-300" />
                <p>Please read and acknowledge the terms above before signing</p>
              </div>
            ) : (
              <SignaturePad
                onSignatureComplete={handleSignatureComplete}
                signerName={isStudent ? lease.studentName : lease.landlordName}
                disabled={isSigning}
              />
            )}
          </div>
        )}

        {/* Info about signing */}
        {canSign && (
          <div className="bg-amber-50 rounded-2xl p-4">
            <div className="flex items-start gap-3">
              <AlertTriangle className="h-5 w-5 text-amber-600 mt-0.5" />
              <div className="text-sm text-amber-800">
                <p className="font-medium">Important</p>
                <p className="mt-1">
                  By signing this document, you are entering into a legally binding agreement. 
                  Make sure you have read and understood all the terms before proceeding.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
