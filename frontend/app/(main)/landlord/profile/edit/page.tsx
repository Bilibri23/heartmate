"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useAuth } from "@/context/auth-context"
import {
  Camera,
  Mail,
  Phone,
  MapPin,
  Loader2,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import api, { uploadApi } from "@/lib/api"
import { authService } from "@/services/auth.service"
import { toast } from "sonner"

interface ProfileData {
  firstName: string
  lastName: string
  email: string
  phone: string
  bio: string
  city: string
  profilePhotoUrl?: string
}

function readAxiosMessage(err: unknown): string | undefined {
  const ax = err as { response?: { data?: { message?: string } } }
  const m = ax.response?.data?.message
  return typeof m === "string" && m.length > 0 ? m : undefined
}

export default function EditLandlordProfilePage() {
  const { user, refreshUser } = useAuth()
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
    profilePhotoUrl: "",
  })

  const fetchProfile = useCallback(async () => {
    if (!user?.id) return

    setIsLoading(true)
    try {
      let me: Awaited<ReturnType<typeof authService.getMe>> | null = null
      try {
        me = await authService.getMe()
      } catch {
        /* fall back to stored user */
      }

      const baseAccount = {
        firstName: me?.firstName ?? user.firstName ?? "",
        lastName: me?.lastName ?? user.lastName ?? "",
        email: me?.email ?? user.email ?? "",
        phone: me?.phone ?? user.phone ?? "",
      }

      try {
        const response = await api.get(`/profiles/${user.id}`)
        const p = response.data as {
          bio?: string
          city?: string
          profilePhotoUrl?: string
        }
        setFormData({
          ...baseAccount,
          bio: p?.bio ?? "",
          city: p?.city ?? "",
          profilePhotoUrl: p?.profilePhotoUrl ?? "",
        })
      } catch (err: unknown) {
        const status = (err as { response?: { status?: number } }).response?.status
        if (status === 404) {
          setFormData({
            ...baseAccount,
            bio: "",
            city: "",
            profilePhotoUrl: "",
          })
        } else {
          throw err
        }
      }
    } catch (err) {
      console.error("Failed to load profile:", err)
      toast.error(readAxiosMessage(err) ?? "Could not load your profile.")
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
      let newPhotoUrl: string | undefined
      if (profilePhoto) {
        const fd = new FormData()
        fd.append("file", profilePhoto)
        const up = await uploadApi.post<{ data?: string; url?: string }>(
          "/upload/profile-photo",
          fd
        )
        const url = up.data?.data ?? (up.data as { url?: string })?.url
        if (typeof url !== "string" || !url.startsWith("http")) {
          throw new Error("Photo upload did not return a valid image URL.")
        }
        newPhotoUrl = url
      }

      await authService.updateMe({
        firstName: formData.firstName.trim(),
        lastName: formData.lastName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
      })

      const profilePayload: {
        bio: string
        city: string
        profilePhotoUrl?: string
      } = {
        bio: formData.bio,
        city: formData.city.trim(),
      }
      if (newPhotoUrl) {
        profilePayload.profilePhotoUrl = newPhotoUrl
      }

      try {
        await api.put(`/profiles/${user.id}`, profilePayload)
      } catch (updateErr: unknown) {
        const status = (updateErr as { response?: { status?: number } }).response?.status
        if (status === 404) {
          await api.post(
            `/profiles?userId=${encodeURIComponent(user.id)}`,
            profilePayload
          )
        } else {
          throw updateErr
        }
      }

      await refreshUser()
      toast.success("Profile updated.")
      router.push("/landlord/profile")
    } catch (err) {
      console.error("Failed to save profile:", err)
      const msg =
        readAxiosMessage(err) ??
        (err instanceof Error ? err.message : undefined) ??
        "Could not save your profile."
      toast.error(msg)
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
          <div className="flex flex-col items-center">
            <div className="relative">
              <Avatar className="h-24 w-24">
                <AvatarImage src={photoPreview || formData.profilePhotoUrl} />
                <AvatarFallback className="bg-blue-100 text-blue-600 text-2xl">
                  {formData.firstName?.[0]}
                  {formData.lastName?.[0]}
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

          <div className="bg-white rounded-2xl p-4 shadow-sm space-y-4">
            <h3 className="font-semibold text-slate-900">Personal Information</h3>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label className="text-slate-700 mb-2 block">First Name</Label>
                <Input
                  value={formData.firstName}
                  onChange={(e) =>
                    setFormData((prev) => ({ ...prev, firstName: e.target.value }))
                  }
                  className="h-12 rounded-xl"
                />
              </div>
              <div>
                <Label className="text-slate-700 mb-2 block">Last Name</Label>
                <Input
                  value={formData.lastName}
                  onChange={(e) =>
                    setFormData((prev) => ({ ...prev, lastName: e.target.value }))
                  }
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
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                className="h-12 rounded-xl"
              />
              <p className="text-xs text-slate-400 mt-1">
                If you change your email, we send a new verification link and mark your
                email as unverified until you confirm it.
              </p>
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block flex items-center gap-2">
                <Phone className="h-4 w-4" /> Phone
              </Label>
              <Input
                type="tel"
                value={formData.phone}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, phone: e.target.value }))
                }
                placeholder="+237 6XX XXX XXX"
                className="h-12 rounded-xl"
              />
              <p className="text-xs text-slate-400 mt-1">
                Changing your phone clears phone verification until you verify the new
                number.
              </p>
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block flex items-center gap-2">
                <MapPin className="h-4 w-4" /> City
              </Label>
              <Input
                value={formData.city}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, city: e.target.value }))
                }
                placeholder="e.g., Douala"
                className="h-12 rounded-xl"
              />
            </div>
          </div>

          <div className="bg-white rounded-2xl p-4 shadow-sm space-y-4">
            <h3 className="font-semibold text-slate-900">About You</h3>

            <div>
              <Label className="text-slate-700 mb-2 block">Bio</Label>
              <Textarea
                value={formData.bio}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, bio: e.target.value }))
                }
                placeholder="Tell potential tenants about yourself..."
                className="min-h-[120px] rounded-xl resize-none"
              />
              <p className="text-xs text-slate-400 mt-1">
                This will be shown on your public profile
              </p>
            </div>
          </div>

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
