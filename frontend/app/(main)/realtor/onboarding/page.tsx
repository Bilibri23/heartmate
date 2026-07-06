"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Skeleton } from "@/components/ui/skeleton"
import { uploadApi } from "@/lib/api"
import { realtorService } from "@/services/realtor.service"
import type { RealtorProfile } from "@/types/realtor"
import { toast } from "sonner"
import {
  Building2,
  MapPin,
  Phone,
  MessageCircle,
  FileText,
  Upload,
  CheckCircle,
  ArrowRight,
  BadgeCheck,
} from "lucide-react"

const DOCUMENT_TYPES = [
  { value: "BUSINESS_REGISTRATION", label: "Business registration" },
  { value: "AGENT_ID", label: "Agent / National ID" },
  { value: "AGENCY_LICENSE", label: "Agency license" },
  { value: "OTHER", label: "Other supporting document" },
]

export default function RealtorOnboardingPage() {
  const router = useRouter()
  const [profile, setProfile] = useState<RealtorProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isUploading, setIsUploading] = useState(false)

  // Profile form
  const [agencyName, setAgencyName] = useState("")
  const [city, setCity] = useState("")
  const [areasCovered, setAreasCovered] = useState("")
  const [phoneNumber, setPhoneNumber] = useState("")
  const [whatsappNumber, setWhatsappNumber] = useState("")
  const [businessRegistrationNumber, setBusinessRegistrationNumber] = useState("")
  const [bio, setBio] = useState("")

  // Document form
  const [documentType, setDocumentType] = useState(DOCUMENT_TYPES[0].value)
  const [docFile, setDocFile] = useState<File | null>(null)

  const applyProfile = useCallback((p: RealtorProfile) => {
    setProfile(p)
    setAgencyName(p.agencyName || "")
    setCity(p.city || "")
    setAreasCovered((p.areasCovered || []).join(", "))
    setPhoneNumber(p.phoneNumber || "")
    setWhatsappNumber(p.whatsappNumber || "")
    setBusinessRegistrationNumber(p.businessRegistrationNumber || "")
    setBio(p.bio || "")
  }, [])

  const load = useCallback(async () => {
    setIsLoading(true)
    try {
      const p = await realtorService.getMe()
      if (p) applyProfile(p)
    } catch (err) {
      console.error("Failed to load realtor profile:", err)
    } finally {
      setIsLoading(false)
    }
  }, [applyProfile])

  useEffect(() => {
    void load()
  }, [load])

  const buildForm = () => ({
    agencyName: agencyName.trim(),
    city: city.trim(),
    areasCovered: areasCovered
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean),
    phoneNumber: phoneNumber.trim() || undefined,
    whatsappNumber: whatsappNumber.trim() || undefined,
    businessRegistrationNumber: businessRegistrationNumber.trim() || undefined,
    bio: bio.trim() || undefined,
  })

  const handleSaveProfile = async () => {
    if (!agencyName.trim()) {
      toast.error("Agency or agent name is required")
      return
    }
    if (!city.trim()) {
      toast.error("City is required")
      return
    }
    setIsSaving(true)
    try {
      const form = buildForm()
      const saved = profile
        ? await realtorService.updateProfile(form)
        : await realtorService.registerProfile(form)
      applyProfile(saved)
      toast.success(profile ? "Profile updated" : "Profile created — now add a verification document")
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } }
      toast.error(ax?.response?.data?.message || "Failed to save profile")
    } finally {
      setIsSaving(false)
    }
  }

  const uploadFile = async (file: File): Promise<string> => {
    const formData = new FormData()
    formData.append("file", file)
    const response = await uploadApi.post("/upload/profile-photo", formData)
    const url = response.data?.data ?? response.data?.url
    if (typeof url === "string" && url.startsWith("http")) return url
    throw new Error("Upload did not return a valid URL.")
  }

  const handleAddDocument = async () => {
    if (!docFile) {
      toast.error("Choose a file first")
      return
    }
    setIsUploading(true)
    try {
      const documentUrl = await uploadFile(docFile)
      const updated = await realtorService.addDocument({ documentType, documentUrl })
      applyProfile(updated)
      setDocFile(null)
      toast.success("Document submitted for review")
    } catch (err: unknown) {
      const ax = err as { message?: string; response?: { data?: { message?: string } } }
      toast.error(ax?.response?.data?.message || ax?.message || "Failed to upload document")
    } finally {
      setIsUploading(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Realtor onboarding" showNotifications={false} showLanguage={false} />
        <div className="p-4 space-y-4">
          <Skeleton className="h-32 rounded-2xl" />
          <Skeleton className="h-64 rounded-2xl" />
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Realtor onboarding" showNotifications={false} showLanguage={false} />

      <div className="flex-1 overflow-y-auto">
        <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
          {/* Intro */}
          <div className="bg-white rounded-2xl p-6 shadow-sm text-center">
            <div className="mx-auto mb-4 h-16 w-16 rounded-full bg-violet-100 flex items-center justify-center">
              <BadgeCheck className="h-8 w-8 text-violet-600" />
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-2">Become a verified realtor</h2>
            <p className="text-slate-500 text-sm">
              Tell us about your agency, then upload a document so our team can verify you. Verified
              realtors get a trust badge and can list on RoomBay.
            </p>
          </div>

          {/* Profile form */}
          <div className="bg-white rounded-2xl p-4 shadow-sm space-y-4">
            <h3 className="font-semibold text-slate-900 flex items-center gap-2">
              <Building2 className="h-4 w-4 text-slate-500" /> Agency details
            </h3>

            <div>
              <Label className="text-slate-700 mb-1.5 block">Agency or agent name *</Label>
              <Input value={agencyName} onChange={(e) => setAgencyName(e.target.value)} placeholder="Grace Homes" />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <Label className="text-slate-700 mb-1.5 flex items-center gap-1.5">
                  <MapPin className="h-3.5 w-3.5" /> City *
                </Label>
                <Input value={city} onChange={(e) => setCity(e.target.value)} placeholder="Yaoundé" />
              </div>
              <div>
                <Label className="text-slate-700 mb-1.5 block">Business registration #</Label>
                <Input
                  value={businessRegistrationNumber}
                  onChange={(e) => setBusinessRegistrationNumber(e.target.value)}
                  placeholder="RC/YAO/2024/..."
                />
              </div>
            </div>

            <div>
              <Label className="text-slate-700 mb-1.5 block">Areas covered</Label>
              <Input
                value={areasCovered}
                onChange={(e) => setAreasCovered(e.target.value)}
                placeholder="Melen, Damas, Bastos"
              />
              <p className="text-xs text-slate-400 mt-1">Separate neighborhoods with commas.</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <Label className="text-slate-700 mb-1.5 flex items-center gap-1.5">
                  <Phone className="h-3.5 w-3.5" /> Phone
                </Label>
                <Input value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} placeholder="+237 6..." />
              </div>
              <div>
                <Label className="text-slate-700 mb-1.5 flex items-center gap-1.5">
                  <MessageCircle className="h-3.5 w-3.5" /> WhatsApp
                </Label>
                <Input
                  value={whatsappNumber}
                  onChange={(e) => setWhatsappNumber(e.target.value)}
                  placeholder="+237 6..."
                />
              </div>
            </div>

            <div>
              <Label className="text-slate-700 mb-1.5 block">Bio</Label>
              <Textarea
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Briefly describe your agency and the kind of properties you handle."
                rows={3}
              />
            </div>

            <Button className="w-full h-11 rounded-xl" onClick={handleSaveProfile} disabled={isSaving}>
              {isSaving ? "Saving..." : profile ? "Save changes" : "Create profile"}
            </Button>
          </div>

          {/* Document upload — only after a profile exists */}
          {profile && (
            <div className="bg-white rounded-2xl p-4 shadow-sm space-y-4">
              <h3 className="font-semibold text-slate-900 flex items-center gap-2">
                <FileText className="h-4 w-4 text-slate-500" /> Verification documents
              </h3>

              {profile.documents?.length > 0 && (
                <div className="space-y-2">
                  {profile.documents.map((d) => (
                    <div
                      key={d.id}
                      className="flex items-center justify-between gap-3 p-3 rounded-xl bg-slate-50 text-sm"
                    >
                      <span className="flex items-center gap-2 text-slate-700">
                        <CheckCircle className="h-4 w-4 text-emerald-500" />
                        {d.documentType}
                      </span>
                      <span className="text-xs text-slate-500">{d.status}</span>
                    </div>
                  ))}
                </div>
              )}

              <div>
                <Label className="text-slate-700 mb-1.5 block">Document type</Label>
                <select
                  value={documentType}
                  onChange={(e) => setDocumentType(e.target.value)}
                  className="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm"
                >
                  {DOCUMENT_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>
              </div>

              <label className="flex items-center justify-center gap-2 p-4 border-2 border-dashed border-slate-300 rounded-xl cursor-pointer hover:border-violet-500 hover:bg-violet-50 transition-colors">
                <Upload className="h-5 w-5 text-slate-400" />
                <span className="text-sm text-slate-500">{docFile ? docFile.name : "Choose a file"}</span>
                <input
                  type="file"
                  accept="image/*,.pdf"
                  onChange={(e) => setDocFile(e.target.files?.[0] || null)}
                  className="hidden"
                />
              </label>

              <Button
                className="w-full h-11 rounded-xl"
                variant="outline"
                onClick={handleAddDocument}
                disabled={isUploading || !docFile}
              >
                {isUploading ? "Uploading..." : "Submit document"}
              </Button>

              <Button
                className="w-full h-11 rounded-xl bg-violet-600 hover:bg-violet-700"
                onClick={() => router.push("/realtor")}
              >
                Go to my dashboard
                <ArrowRight className="h-4 w-4 ml-1" />
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
