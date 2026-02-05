"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Camera, User } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import api from "@/lib/api"

interface Profile {
  id: string
  bio: string
  languages: string[]
  whatsappNumber: string
  emergencyContact: string
  profilePhotoUrl: string | null
}

export default function EditProfilePage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [photoFile, setPhotoFile] = useState<File | null>(null)
  const [photoPreview, setPhotoPreview] = useState<string | null>(null)
  const [formData, setFormData] = useState({
    bio: "",
    languages: "",
    whatsappNumber: "",
    emergencyContact: "",
  })

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.id) return
      
      try {
        const response = await api.get(`/profiles/${user.id}`)
        const profile = response.data
        setFormData({
          bio: profile.bio || "",
          languages: profile.languages?.join(", ") || "",
          whatsappNumber: profile.whatsappNumber || "",
          emergencyContact: profile.emergencyContact || "",
        })
        setPhotoPreview(profile.profilePhotoUrl)
      } catch (err: any) {
        // 404 means no profile exists yet - that's okay, user can create one
        if (err.response?.status !== 404) {
          console.error("Failed to fetch profile:", err)
        }
      } finally {
        setIsLoading(false)
      }
    }

    fetchProfile()
  }, [user?.id])

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setPhotoFile(file)
      setPhotoPreview(URL.createObjectURL(file))
    }
  }

  const handleSubmit = async () => {
    if (!user?.id) return

    setIsSubmitting(true)
    try {
      // If there's a new photo, use multipart form
      if (photoFile) {
        const formDataObj = new FormData()
        formDataObj.append("file", photoFile)
        formDataObj.append("bio", formData.bio)
        formDataObj.append("languages", formData.languages)
        formDataObj.append("whatsappNumber", formData.whatsappNumber)
        formDataObj.append("emergencyContact", formData.emergencyContact)
        formDataObj.append("userId", user.id)

        // Try update first, if 404 then create
        try {
          await api.put(`/profiles/${user.id}`, formDataObj, {
            headers: { "Content-Type": "multipart/form-data" }
          })
        } catch (err: any) {
          if (err.response?.status === 404) {
            // Profile doesn't exist, create it
            await api.post(`/profiles`, formDataObj, {
              headers: { "Content-Type": "multipart/form-data" }
            })
          } else {
            throw err
          }
        }
      } else {
        // JSON update/create
        const profileData = {
          bio: formData.bio,
          languages: formData.languages.split(",").map(l => l.trim()).filter(Boolean),
          whatsappNumber: formData.whatsappNumber,
          emergencyContact: formData.emergencyContact,
        }
        
        try {
          await api.put(`/profiles/${user.id}`, profileData)
        } catch (err: any) {
          if (err.response?.status === 404) {
            // Profile doesn't exist, create it
            await api.post(`/profiles?userId=${user.id}`, profileData)
          } else {
            throw err
          }
        }
      }

      router.push("/profile")
    } catch (err) {
      console.error("Failed to update profile:", err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const userInitials = user?.firstName && user?.lastName
    ? `${user.firstName[0]}${user.lastName[0]}`.toUpperCase()
    : "U"

  if (isLoading) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title={t.profile.editProfile} />
        <div className="flex-1 flex items-center justify-center">
          <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" />
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.profile.editProfile} />

      <div className="flex-1 overflow-y-auto">
        <div className="p-4 space-y-6">
          {/* Profile Photo */}
          <div className="flex flex-col items-center">
            <div className="relative">
              <Avatar className="h-24 w-24">
                <AvatarImage src={photoPreview || undefined} />
                <AvatarFallback className="bg-blue-100 text-blue-600 text-2xl">
                  {userInitials}
                </AvatarFallback>
              </Avatar>
              <label className="absolute bottom-0 right-0 h-8 w-8 rounded-full bg-blue-600 flex items-center justify-center cursor-pointer shadow-lg">
                <Camera className="h-4 w-4 text-white" />
                <input
                  type="file"
                  accept="image/*"
                  onChange={handlePhotoChange}
                  className="hidden"
                />
              </label>
            </div>
            <p className="text-sm text-slate-500 mt-2">Tap to change photo</p>
          </div>

          {/* Form */}
          <div className="bg-white rounded-2xl shadow-sm p-4 space-y-4">
            {/* Name (read-only) */}
            <div>
              <Label className="text-slate-700 mb-2 block">Name</Label>
              <Input
                value={`${user?.firstName || ""} ${user?.lastName || ""}`}
                disabled
                className="h-12 rounded-xl bg-slate-50"
              />
              <p className="text-xs text-slate-500 mt-1">Contact support to change your name</p>
            </div>

            {/* Email (read-only) */}
            <div>
              <Label className="text-slate-700 mb-2 block">Email</Label>
              <Input
                value={user?.email || ""}
                disabled
                className="h-12 rounded-xl bg-slate-50"
              />
            </div>

            {/* Bio */}
            <div>
              <Label className="text-slate-700 mb-2 block">Bio</Label>
              <Textarea
                placeholder="Tell us about yourself..."
                value={formData.bio}
                onChange={(e) => setFormData(prev => ({ ...prev, bio: e.target.value }))}
                className="min-h-[100px] rounded-xl"
              />
            </div>

            {/* Languages */}
            <div>
              <Label className="text-slate-700 mb-2 block">Languages</Label>
              <Input
                placeholder="e.g., French, English, Pidgin"
                value={formData.languages}
                onChange={(e) => setFormData(prev => ({ ...prev, languages: e.target.value }))}
                className="h-12 rounded-xl"
              />
              <p className="text-xs text-slate-500 mt-1">Separate with commas</p>
            </div>

            {/* WhatsApp */}
            <div>
              <Label className="text-slate-700 mb-2 block">WhatsApp Number</Label>
              <Input
                placeholder="+237 6XX XXX XXX"
                value={formData.whatsappNumber}
                onChange={(e) => setFormData(prev => ({ ...prev, whatsappNumber: e.target.value }))}
                className="h-12 rounded-xl"
              />
            </div>

            {/* Emergency Contact */}
            <div>
              <Label className="text-slate-700 mb-2 block">Emergency Contact</Label>
              <Input
                placeholder="Name and phone number"
                value={formData.emergencyContact}
                onChange={(e) => setFormData(prev => ({ ...prev, emergencyContact: e.target.value }))}
                className="h-12 rounded-xl"
              />
            </div>
          </div>

          {/* Submit */}
          <Button
            className="w-full h-14 rounded-xl text-base"
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? t.common.loading : t.common.save}
          </Button>

          <div className="h-8" />
        </div>
      </div>
    </div>
  )
}
