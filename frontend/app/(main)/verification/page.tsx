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
import { NextStepCard } from "@/components/workflows/next-step-card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useRouter } from "next/navigation"
import api, { uploadApi } from "@/lib/api"

type VerificationStatus = "NONE" | "PENDING" | "APPROVED" | "REJECTED" | "VERIFIED"

const ID_TYPES = [
  { value: "PASSPORT", label: "Passport" },
  { value: "NATIONAL_ID", label: "National ID" },
  { value: "DRIVERS_LICENSE", label: "Driver's License" },
] as const

interface VerificationData {
  status: VerificationStatus
  idType?: string
  idNumber?: string
  idPhotoUrl?: string
  selfiePhotoUrl?: string
  university?: string
  studentId?: string
  studentIdPhotoUrl?: string
  rejectionReason?: string
  submittedAt?: string
  createdAt?: string
}

export default function VerificationPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [verification, setVerification] = useState<VerificationData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formData, setFormData] = useState({
    idType: "" as string,
    idNumber: "",
    idPhotoFile: null as File | null,
    selfieFile: null as File | null,
  })
  const [idPreviewUrl, setIdPreviewUrl] = useState<string | null>(null)
  const [selfiePreviewUrl, setSelfiePreviewUrl] = useState<string | null>(null)

  useEffect(() => {
    const fetchVerification = async () => {
      if (!user?.id) return
      
      try {
        const response = await api.get(`/verifications/${user.id}`)
        const data = response.data
        setVerification({
          status: data.status || "NONE",
          idType: data.idType,
          idNumber: data.idNumber,
          idPhotoUrl: data.idPhotoUrl,
          selfiePhotoUrl: data.selfiePhotoUrl,
          university: data.university,
          studentId: data.studentId,
          studentIdPhotoUrl: data.studentIdPhotoUrl,
          rejectionReason: data.rejectionReason,
          submittedAt: data.createdAt,
          createdAt: data.createdAt,
        })
        if (data.idType || data.idNumber) {
          setFormData(prev => ({
            ...prev,
            idType: data.idType || "",
            idNumber: data.idNumber || "",
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

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && user?.id) {
        const fetchVerification = async () => {
          try {
            const response = await api.get(`/verifications/${user.id}`)
            const data = response.data
            setVerification({
              status: data.status || "NONE",
              idType: data.idType,
              idNumber: data.idNumber,
              idPhotoUrl: data.idPhotoUrl,
              selfiePhotoUrl: data.selfiePhotoUrl,
              university: data.university,
              studentId: data.studentId,
              studentIdPhotoUrl: data.studentIdPhotoUrl,
              rejectionReason: data.rejectionReason,
              submittedAt: data.createdAt,
              createdAt: data.createdAt,
            })
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

  const handleIdFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setFormData(prev => ({ ...prev, idPhotoFile: file }))
      setIdPreviewUrl(URL.createObjectURL(file))
    }
  }

  const handleSelfieFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setFormData(prev => ({ ...prev, selfieFile: file }))
      setSelfiePreviewUrl(URL.createObjectURL(file))
    }
  }

  const handleSubmit = async () => {
    if (!user?.id || !formData.idPhotoFile || !formData.selfieFile) return

    setIsSubmitting(true)
    try {
      const uploadFormData = new FormData()
      
      uploadFormData.append("file", formData.idPhotoFile)
      const idUploadRes = await uploadApi.post("/upload/verification-document", uploadFormData)
      const idPhotoUrl = idUploadRes.data?.data || idUploadRes.data?.url || idUploadRes.data

      const selfieFormData = new FormData()
      selfieFormData.append("file", formData.selfieFile)
      const selfieUploadRes = await uploadApi.post("/upload/verification-document", selfieFormData)
      const selfiePhotoUrl = selfieUploadRes.data?.data || selfieUploadRes.data?.url || selfieUploadRes.data

      await api.post(`/verifications?userId=${user.id}`, {
        idType: formData.idType,
        idNumber: formData.idNumber,
        idPhotoUrl,
        selfiePhotoUrl,
      })

      setVerification({ status: "PENDING" })
    } catch (err: any) {
      console.error("Failed to submit verification:", err)
      const errorMessage = err?.response?.data?.message || ""
      if (errorMessage.includes("already exists") || errorMessage.includes("VERIFIED")) {
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
            Your identity has been verified. You can now access all features.
          </p>
          <div className="mb-6 text-left">
            <NextStepCard
              title="What happens next?"
              body="You can apply with a verified profile. Keep using RoomBay messaging and lease/payment flows so the process stays trackable."
              href="/search"
              actionLabel="Browse listings"
              icon={<Shield className="h-5 w-5" />}
            />
          </div>
          
          <div className="bg-emerald-50 rounded-xl p-6 mb-6 text-left">
            <h3 className="font-semibold text-emerald-800 mb-4 flex items-center justify-center">
              <Shield className="h-5 w-5 mr-2" />
              Verified Tenant Benefits
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
                  <p className="text-xs text-slate-600">Landlords prefer verified tenants</p>
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

    if (verification?.status === "PENDING") {
      const refreshStatus = async () => {
        if (!user?.id) return
        try {
          const response = await api.get(`/verifications/${user.id}`)
          const data = response.data
          setVerification({
            status: data.status || "NONE",
            idType: data.idType,
            idNumber: data.idNumber,
            idPhotoUrl: data.idPhotoUrl,
            selfiePhotoUrl: data.selfiePhotoUrl,
            university: data.university,
            studentId: data.studentId,
            studentIdPhotoUrl: data.studentIdPhotoUrl,
            rejectionReason: data.rejectionReason,
            submittedAt: data.createdAt,
            createdAt: data.createdAt,
          })
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
          <NextStepCard
            title="What happens next?"
            body="RoomBay reviews submitted verification documents before unlocking the full application flow. You can keep browsing and saving listings while you wait."
            href="/search"
            actionLabel="Keep browsing"
            icon={<Clock className="h-5 w-5" />}
          />
        </div>
      )
    }

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
          <div className="mb-6">
            <NextStepCard
              title="What happens next?"
              body="Upload clearer documents and make sure the name, photo, and ID number are readable before resubmitting."
              icon={<Upload className="h-5 w-5" />}
            />
          </div>
          {renderForm()}
        </div>
      )
    }

    return (
      <div className="p-4">
        {renderForm()}
      </div>
    )
  }

  const renderForm = () => (
    <div className="space-y-6">
      <div className="bg-blue-50 rounded-2xl p-4">
        <div className="flex items-center gap-2 text-blue-600 mb-2">
          <Shield className="h-5 w-5" />
          <span className="font-semibold">Why verify?</span>
        </div>
        <p className="text-sm text-blue-700">
          Verification builds trust with landlords and increases your chances of getting accepted.
        </p>
      </div>

      <div>
        <Label className="text-slate-700 mb-2 block">ID Type</Label>
        <Select value={formData.idType} onValueChange={(v) => setFormData(prev => ({ ...prev, idType: v }))}>
          <SelectTrigger className="h-12 rounded-xl">
            <SelectValue placeholder="Select ID type" />
          </SelectTrigger>
          <SelectContent>
            {ID_TYPES.map((t) => (
              <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div>
        <Label className="text-slate-700 mb-2 block">ID Number</Label>
        <Input
          placeholder="e.g., AB1234567"
          value={formData.idNumber}
          onChange={(e) => setFormData(prev => ({ ...prev, idNumber: e.target.value }))}
          className="h-12 rounded-xl"
        />
      </div>

      <div>
        <Label className="text-slate-700 mb-2 block">ID Document Photo</Label>
        <div className="relative">
          {idPreviewUrl ? (
            <div className="relative rounded-2xl overflow-hidden border-2 border-dashed border-slate-200">
              <img 
                src={idPreviewUrl} 
                alt="ID Preview" 
                className="w-full h-48 object-cover"
              />
              <button
                onClick={() => {
                  setIdPreviewUrl(null)
                  setFormData(prev => ({ ...prev, idPhotoFile: null }))
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
                <p className="text-sm font-medium text-slate-700">Upload ID document</p>
                <p className="text-xs text-slate-500 mt-1">JPG, PNG up to 5MB</p>
              </div>
              <input
                type="file"
                accept="image/*"
                capture="environment"
                onChange={handleIdFileChange}
                className="hidden"
              />
            </label>
          )}
        </div>
      </div>

      <div>
        <Label className="text-slate-700 mb-2 block">Selfie Photo</Label>
        <div className="relative">
          {selfiePreviewUrl ? (
            <div className="relative rounded-2xl overflow-hidden border-2 border-dashed border-slate-200">
              <img 
                src={selfiePreviewUrl} 
                alt="Selfie Preview" 
                className="w-full h-48 object-cover"
              />
              <button
                onClick={() => {
                  setSelfiePreviewUrl(null)
                  setFormData(prev => ({ ...prev, selfieFile: null }))
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
                <p className="text-sm font-medium text-slate-700">Take a selfie</p>
                <p className="text-xs text-slate-500 mt-1">JPG, PNG up to 5MB</p>
              </div>
              <input
                type="file"
                accept="image/*"
                capture="user"
                onChange={handleSelfieFileChange}
                className="hidden"
              />
            </label>
          )}
        </div>
      </div>

      <div className="bg-slate-50 rounded-2xl p-4">
        <p className="text-sm font-medium text-slate-700 mb-2">Tips for a quick approval:</p>
        <ul className="text-sm text-slate-500 space-y-1">
          <li className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Ensure your face and ID are clearly visible
          </li>
          <li className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Good lighting, no blur or glare
          </li>
          <li className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Selfie should match your ID photo
          </li>
        </ul>
      </div>

      <Button
        className="w-full h-12 rounded-xl"
        onClick={handleSubmit}
        disabled={isSubmitting || !formData.idType || !formData.idNumber || !formData.idPhotoFile || !formData.selfieFile}
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
