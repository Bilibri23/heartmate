"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { 
  Camera,
  User,
  Mail,
  Phone,
  MapPin,
  Building2,
  Loader2
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import api from "@/lib/api"

interface ProfileData {
  firstName: string
  lastName: string
  email: string
  phone: string
  bio: string
  city: string
  profilePhotoUrl?: string
}

export default function EditLandlordProfilePage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [profilePhoto, setProfilePhoto] = useState<File | null>(null)
  const [photoPreview, setPhotoPreview] = useState<string | null>(null)
  const [formData, setFormData] = useState<ProfileData>({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    bio: "",
    city: "",
    profilePhotoUrl: ""
  })

  const fetchProfile = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      // Try to get existing profile, but don't fail if it doesn't exist
      const response = await api.get(`/profiles/${user.id}`)
      setFormData({
        firstName: response.data?.firstName || user.firstName || "",
        lastName: response.data?.lastName || user.lastName || "",
        email: response.data?.email || user.email || "",
        phone: response.data?.phone || user.phone || "",
        bio: response.data?.bio || "",
        city: response.data?.city || "",
        profilePhotoUrl: response.data?.profilePhotoUrl || ""
      })
    } catch (err) {
      // Profile may not exist yet - use auth context data as fallback
      console.log("Profile not found, using auth context data")
      setFormData({
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        email: user.email || "",
        phone: user.phone || "",
        bio: "",
        city: "",
        profilePhotoUrl: ""
      })
    } finally {
      setIsLoading(false)
    }
  }, [user])

  useEffect(() => {
    fetchProfile()
  }, [fetchProfile])

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setProfilePhoto(file)
      setPhotoPreview(URL.createObjectURL(file))
    }
  }

  const handleSave = async () => {
    if (!user?.id) return
    
    setIsSaving(true)
    try {
      // Try to update profile, if it fails with 404, create it
      try {
        await api.put(`/profiles/${user.id}`, {
          bio: formData.bio,
        })
      } catch (updateErr: any) {
        if (updateErr?.response?.status === 404) {
          // Profile doesn't exist, create it
          await api.post(`/profiles`, {
            bio: formData.bio,
          }, { params: { userId: user.id } })
        } else {
          throw updateErr
        }
      }

      // Upload photo if changed - use dedicated photo upload endpoint
      if (profilePhoto) {
        const photoFormData = new FormData()
        photoFormData.append("file", profilePhoto)
        const token = localStorage.getItem("token")
        await fetch("http://localhost:8080/api/upload/profile-photo", {
          method: "POST",
          headers: token ? { "Authorization": `Bearer ${token}` } : {},
          body: photoFormData
        })
      }

      router.push("/landlord/profile")
    } catch (err) {
      console.error("Failed to save profile:", err)
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Edit Profile" />
        <div className="p-4 space-y-4">
          <div className="flex justify-center">
            <Skeleton className="h-24 w-24 rounded-full" />
          </div>
          <Skeleton className="h-12 rounded-xl" />
          <Skeleton className="h-12 rounded-xl" />
          <Skeleton className="h-12 rounded-xl" />
          <Skeleton className="h-24 rounded-xl" />
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Edit Profile" />

      <div className="flex-1 overflow-y-auto">
        <div className="p-4 space-y-6">
          {/* Profile Photo */}
          <div className="flex flex-col items-center">
            <div className="relative">
              <Avatar className="h-24 w-24">
                <AvatarImage src={photoPreview || formData.profilePhotoUrl} />
                <AvatarFallback className="bg-blue-100 text-blue-600 text-2xl">
                  {formData.firstName?.[0]}{formData.lastName?.[0]}
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
          <div className="bg-white rounded-2xl p-4 shadow-sm space-y-4">
            <h3 className="font-semibold text-slate-900">Personal Information</h3>
            
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label className="text-slate-700 mb-2 block">First Name</Label>
                <Input
                  value={formData.firstName}
                  onChange={(e) => setFormData(prev => ({ ...prev, firstName: e.target.value }))}
                  className="h-12 rounded-xl"
                />
              </div>
              <div>
                <Label className="text-slate-700 mb-2 block">Last Name</Label>
                <Input
                  value={formData.lastName}
                  onChange={(e) => setFormData(prev => ({ ...prev, lastName: e.target.value }))}
                  className="h-12 rounded-xl"
                />
              </div>
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block flex items-center gap-2">
                <Mail className="h-4 w-4" /> Email
              </Label>
              <Input
                type="email"
                value={formData.email}
                disabled
                className="h-12 rounded-xl bg-slate-50"
              />
              <p className="text-xs text-slate-400 mt-1">Email cannot be changed</p>
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block flex items-center gap-2">
                <Phone className="h-4 w-4" /> Phone
              </Label>
              <Input
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData(prev => ({ ...prev, phone: e.target.value }))}
                placeholder="+237 6XX XXX XXX"
                className="h-12 rounded-xl"
              />
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block flex items-center gap-2">
                <MapPin className="h-4 w-4" /> City
              </Label>
              <Input
                value={formData.city}
                onChange={(e) => setFormData(prev => ({ ...prev, city: e.target.value }))}
                placeholder="e.g., Douala"
                className="h-12 rounded-xl"
              />
            </div>
          </div>

          {/* Bio */}
          <div className="bg-white rounded-2xl p-4 shadow-sm space-y-4">
            <h3 className="font-semibold text-slate-900">About You</h3>
            
            <div>
              <Label className="text-slate-700 mb-2 block">Bio</Label>
              <Textarea
                value={formData.bio}
                onChange={(e) => setFormData(prev => ({ ...prev, bio: e.target.value }))}
                placeholder="Tell potential tenants about yourself..."
                className="min-h-[120px] rounded-xl resize-none"
              />
              <p className="text-xs text-slate-400 mt-1">
                This will be shown on your public profile
              </p>
            </div>
          </div>

          {/* Save Button */}
          <div className="pb-8">
            <Button
              className="w-full h-14 rounded-xl text-base"
              onClick={handleSave}
              disabled={isSaving}
            >
              {isSaving ? (
                <>
                  <Loader2 className="h-5 w-5 mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                "Save Changes"
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
