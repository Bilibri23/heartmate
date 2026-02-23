"use client"

import { useState, useEffect } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { 
  Upload, 
  CheckCircle, 
  Clock, 
  XCircle, 
  Camera,
  FileText,
  Shield,
  ArrowRight
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useRouter } from "next/navigation"
import api, { uploadApi } from "@/lib/api"

type VerificationStatus = "NONE" | "PENDING" | "APPROVED" | "REJECTED" | "VERIFIED"

interface VerificationData {
  status: VerificationStatus
  universityName?: string
  studentIdNumber?: string
  documentUrl?: string
  rejectionReason?: string
  submittedAt?: string
}

export default function VerificationPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [verification, setVerification] = useState<VerificationData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formData, setFormData] = useState({
    universityName: "",
    studentIdNumber: "",
    documentFile: null as File | null,
  })
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)

  useEffect(() => {
    const fetchVerification = async () => {
      if (!user?.id) return
      
      try {
        const response = await api.get(`/verifications/${user.id}`)
        setVerification(response.data)
        if (response.data.universityName) {
          setFormData(prev => ({
            ...prev,
            universityName: response.data.universityName,
            studentIdNumber: response.data.studentIdNumber || "",
          }))
        }
      } catch (err: any) {
        if (err.response?.status === 404) {
          setVerification({ status: "NONE" })
        }
      } finally {
        setIsLoading(false)
      }
    }

    fetchVerification()
  }, [user?.id])

  // Refetch verification status when page gains focus (user switches back from admin)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && user?.id) {
        const fetchVerification = async () => {
          try {
            const response = await api.get(`/verifications/${user.id}`)
            setVerification(response.data)
          } catch (err: any) {
            if (err.response?.status === 404) {
              setVerification({ status: "NONE" })
            }
          }
        }
        fetchVerification()
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange)
  }, [user?.id])

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setFormData(prev => ({ ...prev, documentFile: file }))
      setPreviewUrl(URL.createObjectURL(file))
    }
  }

  const handleSubmit = async () => {
    if (!user?.id || !formData.documentFile) return

    setIsSubmitting(true)
    try {
      // Step 1: Upload the student ID photo first
      const uploadFormData = new FormData()
      uploadFormData.append("file", formData.documentFile)
      const uploadResponse = await uploadApi.post("/upload/student-id", uploadFormData)
      const studentIdPhotoUrl = uploadResponse.data?.data || uploadResponse.data?.url || uploadResponse.data

      // Step 2: Submit verification with JSON body (backend expects university and studentId fields)
      await api.post(`/verifications?userId=${user.id}`, {
        university: formData.universityName,
        studentId: formData.studentIdNumber,
        studentIdPhotoUrl: studentIdPhotoUrl
      })

      setVerification({ status: "PENDING" })
    } catch (err: any) {
      console.error("Failed to submit verification:", err)
      const errorMessage = err?.response?.data?.message || ""
      if (errorMessage.includes("already exists") || errorMessage.includes("VERIFIED")) {
        // Already verified, refresh status
        setVerification({ status: "VERIFIED" })
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const renderContent = () => {
    if (isLoading) {
      return (
        <div className="flex items-center justify-center py-20">
          <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" />
        </div>
      )
    }

    // Already verified
    if (verification?.status === "VERIFIED") {
      return (
        <div className="text-center py-12 px-4">
          <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-emerald-100">
            <CheckCircle className="h-10 w-10 text-emerald-600" />
          </div>
          <h2 className="text-2xl font-bold text-slate-900 mb-2">
            {t.verification.approved}
          </h2>
          <p className="text-slate-500 mb-6">
            Your student ID has been verified. You can now access all features.
          </p>
          
          {/* Verified Benefits */}
          <div className="bg-emerald-50 rounded-xl p-6 mb-6 text-left">
            <h3 className="font-semibold text-emerald-800 mb-4 flex items-center justify-center">
              <Shield className="h-5 w-5 mr-2" />
              Verified Student Benefits
            </h3>
            <div className="space-y-3 max-w-sm mx-auto">
              <div className="flex items-start gap-3">
                <CheckCircle className="h-5 w-5 text-emerald-600 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-sm font-medium text-slate-800">Priority Applications</p>
                  <p className="text-xs text-slate-600">Your applications are highlighted to landlords</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <CheckCircle className="h-5 w-5 text-emerald-600 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-sm font-medium text-slate-800">Verified Badge</p>
                  <p className="text-xs text-slate-600">Show your verification status on profile</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <CheckCircle className="h-5 w-5 text-emerald-600 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-sm font-medium text-slate-800">Increased Trust</p>
                  <p className="text-xs text-slate-600">Landlords prefer verified students</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <CheckCircle className="h-5 w-5 text-emerald-600 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="text-sm font-medium text-slate-800">Full Access</p>
                  <p className="text-xs text-slate-600">Apply to any listing without restrictions</p>
                </div>
              </div>
            </div>
          </div>
          
          <Button onClick={() => router.push("/search")} className="rounded-xl">
            Browse Listings
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </div>
      )
    }

    // Pending verification
    if (verification?.status === "PENDING") {
      const refreshStatus = async () => {
        if (!user?.id) return
        try {
          const response = await api.get(`/verifications/${user.id}`)
          setVerification(response.data)
        } catch (err: any) {
          if (err.response?.status === 404) {
            setVerification({ status: "NONE" })
          }
        }
      }

      return (
        <div className="text-center py-12 px-4">
          <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-amber-100">
            <Clock className="h-10 w-10 text-amber-600" />
          </div>
          <h2 className="text-2xl font-bold text-slate-900 mb-2">
            {t.verification.pending}
          </h2>
          <p className="text-slate-500 mb-2">
            We're reviewing your documents. This usually takes 24-48 hours.
          </p>
          <p className="text-sm text-slate-400 mb-4">
            Submitted: {verification.submittedAt ? new Date(verification.submittedAt).toLocaleDateString() : "Recently"}
          </p>
          <Button onClick={refreshStatus} variant="outline" className="rounded-xl mb-4">
            Refresh Status
          </Button>
        </div>
      )
    }

    // Rejected - show form again
    if (verification?.status === "REJECTED") {
      return (
        <div className="p-4">
          <div className="mb-6 p-4 bg-red-50 rounded-2xl">
            <div className="flex items-center gap-2 text-red-600 mb-2">
              <XCircle className="h-5 w-5" />
              <span className="font-semibold">{t.verification.rejected}</span>
            </div>
            <p className="text-sm text-red-600">
              {verification.rejectionReason || "Your verification was rejected. Please resubmit with clearer documents."}
            </p>
          </div>
          {renderForm()}
        </div>
      )
    }

    // No verification - show form
    return (
      <div className="p-4">
        {renderForm()}
      </div>
    )
  }

  const renderForm = () => (
    <div className="space-y-6">
      {/* Info Banner */}
      <div className="bg-blue-50 rounded-2xl p-4">
        <div className="flex items-center gap-2 text-blue-600 mb-2">
          <Shield className="h-5 w-5" />
          <span className="font-semibold">Why verify?</span>
        </div>
        <p className="text-sm text-blue-700">
          Verification builds trust with landlords and increases your chances of getting accepted.
        </p>
      </div>

      {/* University Name */}
      <div>
        <Label className="text-slate-700 mb-2 block">University / Institution</Label>
        <Input
          placeholder="e.g., University of Yaoundé I"
          value={formData.universityName}
          onChange={(e) => setFormData(prev => ({ ...prev, universityName: e.target.value }))}
          className="h-12 rounded-xl"
        />
      </div>

      {/* Student ID Number */}
      <div>
        <Label className="text-slate-700 mb-2 block">Student ID Number</Label>
        <Input
          placeholder="e.g., 21A1234"
          value={formData.studentIdNumber}
          onChange={(e) => setFormData(prev => ({ ...prev, studentIdNumber: e.target.value }))}
          className="h-12 rounded-xl"
        />
      </div>

      {/* Document Upload */}
      <div>
        <Label className="text-slate-700 mb-2 block">{t.verification.uploadId}</Label>
        <div className="relative">
          {previewUrl ? (
            <div className="relative rounded-2xl overflow-hidden border-2 border-dashed border-slate-200">
              <img 
                src={previewUrl} 
                alt="ID Preview" 
                className="w-full h-48 object-cover"
              />
              <button
                onClick={() => {
                  setPreviewUrl(null)
                  setFormData(prev => ({ ...prev, documentFile: null }))
                }}
                className="absolute top-2 right-2 bg-white rounded-full p-2 shadow-lg"
              >
                <XCircle className="h-5 w-5 text-slate-600" />
              </button>
            </div>
          ) : (
            <label className="flex flex-col items-center justify-center h-48 rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50 cursor-pointer hover:bg-slate-100 transition-colors">
              <div className="flex flex-col items-center">
                <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-blue-100">
                  <Camera className="h-6 w-6 text-blue-600" />
                </div>
                <p className="text-sm font-medium text-slate-700">
                  Take a photo or upload
                </p>
                <p className="text-xs text-slate-500 mt-1">
                  JPG, PNG up to 5MB
                </p>
              </div>
              <input
                type="file"
                accept="image/*"
                capture="environment"
                onChange={handleFileChange}
                className="hidden"
              />
            </label>
          )}
        </div>
      </div>

      {/* Tips */}
      <div className="bg-slate-50 rounded-2xl p-4">
        <p className="text-sm font-medium text-slate-700 mb-2">Tips for a quick approval:</p>
        <ul className="text-sm text-slate-500 space-y-1">
          <li className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Make sure your name is clearly visible
          </li>
          <li className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Include the current academic year
          </li>
          <li className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Ensure good lighting and no blur
          </li>
        </ul>
      </div>

      {/* Submit Button */}
      <Button
        className="w-full h-12 rounded-xl"
        onClick={handleSubmit}
        disabled={isSubmitting || !formData.universityName || !formData.documentFile}
      >
        {isSubmitting ? t.common.loading : t.common.submit}
      </Button>
    </div>
  )

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.verification.title} />
      
      <div className="flex-1 overflow-y-auto">
        {renderContent()}
      </div>
    </div>
  )
}
