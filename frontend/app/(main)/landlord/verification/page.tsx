"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { 
  Shield,
  CheckCircle,
  Clock,
  AlertCircle,
  Upload,
  FileText,
  Camera,
  User,
  Building2,
  BadgeCheck
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import api from "@/lib/api"
import { toast } from "sonner"

interface VerificationStatus {
  identityStatus: string
  businessStatus: string
  propertyStatus: string
  trustScore: number
  isTrustedLandlord: boolean
  createdAt?: string
  updatedAt?: string
  identityRejectionReason?: string
}

export default function LandlordVerificationPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [status, setStatus] = useState<VerificationStatus | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [idDocument, setIdDocument] = useState<File | null>(null)
  const [selfie, setSelfie] = useState<File | null>(null)
  const [propertyDoc, setPropertyDoc] = useState<File | null>(null)

  const fetchStatus = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await api.get(`/landlord-verifications/me`)
      setStatus({
        identityStatus: response.data?.identityStatus || "NOT_SUBMITTED",
        businessStatus: response.data?.businessStatus || "NOT_SUBMITTED",
        propertyStatus: response.data?.propertyStatus || "NOT_SUBMITTED",
        trustScore: response.data?.trustScore || 0,
        isTrustedLandlord: response.data?.isTrustedLandlord || false,
        createdAt: response.data?.createdAt,
        updatedAt: response.data?.updatedAt,
        identityRejectionReason: response.data?.identityRejectionReason
      })
    } catch (err: any) {
      // 404 means no verification record exists yet - this is normal for new landlords
      if (err?.response?.status === 404) {
        setStatus({
          identityStatus: "NOT_SUBMITTED",
          businessStatus: "NOT_SUBMITTED",
          propertyStatus: "NOT_SUBMITTED",
          trustScore: 0,
          isTrustedLandlord: false
        })
      } else {
        console.error("Failed to fetch verification status:", err)
        setStatus({
          identityStatus: "NOT_SUBMITTED",
          businessStatus: "NOT_SUBMITTED",
          propertyStatus: "NOT_SUBMITTED",
          trustScore: 0,
          isTrustedLandlord: false
        })
      }
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchStatus()
  }, [fetchStatus])

  const uploadFile = async (file: File): Promise<string> => {
    const formData = new FormData()
    formData.append("file", file)
    
    const response = await api.post("/upload/profile-photo", formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    
    console.log("Upload response:", response.data)
    return response.data?.data || response.data?.url || response.data
  }

  const handleSubmit = async () => {
    if (!user?.id || !idDocument) return
    
    setIsSubmitting(true)
    try {
      // Step 1: Upload files to get URLs
      console.log("Uploading ID document...")
      const idFrontPhotoUrl = await uploadFile(idDocument)
      console.log("ID photo URL:", idFrontPhotoUrl)
      
      let selfieWithIdUrl = idFrontPhotoUrl
      if (selfie) {
        console.log("Uploading selfie...")
        selfieWithIdUrl = await uploadFile(selfie)
        console.log("Selfie URL:", selfieWithIdUrl)
      }
      
      // Step 2: Submit identity verification with JSON body
      const requestBody = {
        idType: "NATIONAL_ID",
        idNumber: "PENDING_REVIEW",
        idFrontPhotoUrl: idFrontPhotoUrl,
        selfieWithIdUrl: selfieWithIdUrl,
        addressLine1: "To be verified",
        city: "Douala",
        region: "Littoral"
      }
      console.log("Submitting verification:", requestBody)
      
      await api.post("/landlord-verifications/identity", requestBody)
      
      // Step 3: If property doc provided, submit property verification
      if (propertyDoc) {
        const propertyDocUrl = await uploadFile(propertyDoc)
        await api.post("/landlord-verifications/property", {
          propertyOwnershipDocUrl: propertyDocUrl
        })
      }
      
      fetchStatus()
    } catch (err: any) {
      console.error("Failed to submit verification:", err)
      const errorMessage = err?.response?.data?.message || "Failed to submit verification"
      if (errorMessage.includes("already verified") || errorMessage.includes("already submitted")) {
        toast.error("Verification already submitted", {
          description: "Please wait for admin review or contact support."
        })
        fetchStatus()
      } else {
        toast.error(errorMessage)
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const getOverallStatus = () => {
    if (!status) return "NOT_STARTED"
    if (status.isTrustedLandlord) return "VERIFIED"
    if (status.identityStatus === "VERIFIED") return "VERIFIED"
    if (status.identityStatus === "PENDING" || status.businessStatus === "PENDING" || status.propertyStatus === "PENDING") return "PENDING"
    if (status.identityStatus === "REJECTED") return "REJECTED"
    return "NOT_STARTED"
  }

  const getStatusBadge = () => {
    if (!status) return null
    const overallStatus = getOverallStatus()
    
    switch (overallStatus) {
      case "VERIFIED":
        return (
          <div className="flex items-center gap-2 px-4 py-2 bg-emerald-100 text-emerald-700 rounded-full">
            <BadgeCheck className="h-5 w-5" />
            <span className="font-medium">Verified (Score: {status.trustScore}%)</span>
          </div>
        )
      case "PENDING":
        return (
          <div className="flex items-center gap-2 px-4 py-2 bg-amber-100 text-amber-700 rounded-full">
            <Clock className="h-5 w-5" />
            <span className="font-medium">Under Review</span>
          </div>
        )
      case "REJECTED":
        return (
          <div className="flex items-center gap-2 px-4 py-2 bg-red-100 text-red-700 rounded-full">
            <AlertCircle className="h-5 w-5" />
            <span className="font-medium">Rejected</span>
          </div>
        )
      default:
        return (
          <div className="flex items-center gap-2 px-4 py-2 bg-slate-100 text-slate-600 rounded-full">
            <Shield className="h-5 w-5" />
            <span className="font-medium">Not Verified</span>
          </div>
        )
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Verification" />
        <div className="p-4 space-y-4">
          <Skeleton className="h-32 rounded-2xl" />
          <Skeleton className="h-48 rounded-2xl" />
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Verification" />

      <div className="flex-1 overflow-y-auto">
        <div className="p-4 space-y-6">
          {/* Status Card */}
          <div className="bg-white rounded-2xl p-6 shadow-sm text-center">
            <div className="mx-auto mb-4 h-16 w-16 rounded-full bg-blue-100 flex items-center justify-center">
              <Shield className="h-8 w-8 text-blue-600" />
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-2">Landlord Verification</h2>
            <p className="text-slate-500 text-sm mb-4">
              Verify your identity to build trust with potential tenants
            </p>
            {getStatusBadge()}
          </div>

          {/* Verification Checklist */}
          <div className="bg-white rounded-2xl p-4 shadow-sm">
            <h3 className="font-semibold text-slate-900 mb-4">Verification Checklist</h3>
            <div className="space-y-3">
              <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50">
                {status?.identityStatus === "VERIFIED" ? (
                  <CheckCircle className="h-5 w-5 text-emerald-500" />
                ) : status?.identityStatus === "PENDING" ? (
                  <Clock className="h-5 w-5 text-amber-500" />
                ) : (
                  <div className="h-5 w-5 rounded-full border-2 border-slate-300" />
                )}
                <span className={status?.identityStatus === "VERIFIED" ? "text-slate-900" : "text-slate-500"}>
                  Identity Verification
                </span>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50">
                {status?.businessStatus === "VERIFIED" ? (
                  <CheckCircle className="h-5 w-5 text-emerald-500" />
                ) : status?.businessStatus === "PENDING" ? (
                  <Clock className="h-5 w-5 text-amber-500" />
                ) : (
                  <div className="h-5 w-5 rounded-full border-2 border-slate-300" />
                )}
                <span className={status?.businessStatus === "VERIFIED" ? "text-slate-900" : "text-slate-500"}>
                  Business Registration (Optional)
                </span>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50">
                {status?.propertyStatus === "VERIFIED" ? (
                  <CheckCircle className="h-5 w-5 text-emerald-500" />
                ) : status?.propertyStatus === "PENDING" ? (
                  <Clock className="h-5 w-5 text-amber-500" />
                ) : (
                  <div className="h-5 w-5 rounded-full border-2 border-slate-300" />
                )}
                <span className={status?.propertyStatus === "VERIFIED" ? "text-slate-900" : "text-slate-500"}>
                  Property Ownership
                </span>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-50">
                {status?.isTrustedLandlord ? (
                  <CheckCircle className="h-5 w-5 text-emerald-500" />
                ) : (
                  <div className="h-5 w-5 rounded-full border-2 border-slate-300" />
                )}
                <span className={status?.isTrustedLandlord ? "text-slate-900" : "text-slate-500"}>
                  Trusted Landlord Badge
                </span>
              </div>
            </div>
          </div>

          {/* Upload Section - Only show if not verified */}
          {getOverallStatus() !== "VERIFIED" && getOverallStatus() !== "PENDING" && (
            <div className="bg-white rounded-2xl p-4 shadow-sm">
              <h3 className="font-semibold text-slate-900 mb-4">Submit Documents</h3>
              
              {status?.identityStatus === "REJECTED" && status.identityRejectionReason && (
                <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl">
                  <p className="text-sm text-red-700">
                    <strong>Rejection Reason:</strong> {status.identityRejectionReason}
                  </p>
                </div>
              )}

              <div className="space-y-4">
                {/* ID Document */}
                <div>
                  <Label className="text-slate-700 mb-2 block flex items-center gap-2">
                    <FileText className="h-4 w-4" />
                    ID Document (CNI/Passport) *
                  </Label>
                  <label className="flex items-center justify-center gap-2 p-4 border-2 border-dashed border-slate-300 rounded-xl cursor-pointer hover:border-blue-500 hover:bg-blue-50 transition-colors">
                    <Upload className="h-5 w-5 text-slate-400" />
                    <span className="text-sm text-slate-500">
                      {idDocument ? idDocument.name : "Upload ID Document"}
                    </span>
                    <input
                      type="file"
                      accept="image/*,.pdf"
                      onChange={(e) => setIdDocument(e.target.files?.[0] || null)}
                      className="hidden"
                    />
                  </label>
                </div>

                {/* Selfie */}
                <div>
                  <Label className="text-slate-700 mb-2 block flex items-center gap-2">
                    <Camera className="h-4 w-4" />
                    Selfie with ID (Optional)
                  </Label>
                  <label className="flex items-center justify-center gap-2 p-4 border-2 border-dashed border-slate-300 rounded-xl cursor-pointer hover:border-blue-500 hover:bg-blue-50 transition-colors">
                    <Upload className="h-5 w-5 text-slate-400" />
                    <span className="text-sm text-slate-500">
                      {selfie ? selfie.name : "Upload Selfie"}
                    </span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) => setSelfie(e.target.files?.[0] || null)}
                      className="hidden"
                    />
                  </label>
                </div>

                {/* Property Document */}
                <div>
                  <Label className="text-slate-700 mb-2 block flex items-center gap-2">
                    <Building2 className="h-4 w-4" />
                    Property Ownership Proof (Optional)
                  </Label>
                  <label className="flex items-center justify-center gap-2 p-4 border-2 border-dashed border-slate-300 rounded-xl cursor-pointer hover:border-blue-500 hover:bg-blue-50 transition-colors">
                    <Upload className="h-5 w-5 text-slate-400" />
                    <span className="text-sm text-slate-500">
                      {propertyDoc ? propertyDoc.name : "Upload Document"}
                    </span>
                    <input
                      type="file"
                      accept="image/*,.pdf"
                      onChange={(e) => setPropertyDoc(e.target.files?.[0] || null)}
                      className="hidden"
                    />
                  </label>
                </div>

                <Button
                  className="w-full h-12 rounded-xl"
                  onClick={handleSubmit}
                  disabled={isSubmitting || !idDocument}
                >
                  {isSubmitting ? "Submitting..." : "Submit for Verification"}
                </Button>
              </div>
            </div>
          )}

          {/* Pending Message */}
          {getOverallStatus() === "PENDING" && (
            <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4">
              <div className="flex items-start gap-3">
                <Clock className="h-5 w-5 text-amber-600 mt-0.5" />
                <div>
                  <h4 className="font-medium text-amber-800">Under Review</h4>
                  <p className="text-sm text-amber-700 mt-1">
                    Your documents are being reviewed. This usually takes 1-2 business days.
                  </p>
                  {status?.createdAt && (
                    <p className="text-xs text-amber-600 mt-2">
                      Submitted: {new Date(status.createdAt).toLocaleDateString()}
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Verified Message */}
          {getOverallStatus() === "VERIFIED" && (
            <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4">
              <div className="flex items-start gap-3">
                <BadgeCheck className="h-5 w-5 text-emerald-600 mt-0.5" />
                <div>
                  <h4 className="font-medium text-emerald-800">Verified Landlord</h4>
                  <p className="text-sm text-emerald-700 mt-1">
                    Your account is verified. Your listings will display a verified badge.
                  </p>
                  {status?.updatedAt && (
                    <p className="text-xs text-emerald-600 mt-2">
                      Verified: {new Date(status.updatedAt).toLocaleDateString()}
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
