"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useAuth } from "@/context/auth-context"
import api from "@/lib/api"
import { realtorService } from "@/services/realtor.service"
import type { RealtorProfile } from "@/types/realtor"
import {
  BadgeCheck,
  Clock,
  AlertCircle,
  ShieldOff,
  Shield,
  Building2,
  MapPin,
  Phone,
  MessageCircle,
  Home,
  Users,
  Star,
  FileText,
  Pencil,
} from "lucide-react"

function StatusBadge({ status }: { status: string }) {
  switch (status) {
    case "VERIFIED":
      return (
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-100 text-emerald-700 rounded-full">
          <BadgeCheck className="h-5 w-5" />
          <span className="font-medium">Verified realtor</span>
        </div>
      )
    case "PENDING":
      return (
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-amber-100 text-amber-700 rounded-full">
          <Clock className="h-5 w-5" />
          <span className="font-medium">Under review</span>
        </div>
      )
    case "REJECTED":
      return (
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-red-100 text-red-700 rounded-full">
          <AlertCircle className="h-5 w-5" />
          <span className="font-medium">Rejected</span>
        </div>
      )
    case "SUSPENDED":
      return (
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-slate-200 text-slate-700 rounded-full">
          <ShieldOff className="h-5 w-5" />
          <span className="font-medium">Suspended</span>
        </div>
      )
    default:
      return (
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-slate-100 text-slate-600 rounded-full">
          <Shield className="h-5 w-5" />
          <span className="font-medium">Not verified</span>
        </div>
      )
  }
}

interface ListingStats {
  totalListings: number
  activeListings: number
  rentedListings: number
}

export default function RealtorDashboardPage() {
  const router = useRouter()
  const { user } = useAuth()
  const [profile, setProfile] = useState<RealtorProfile | null>(null)
  const [listingStats, setListingStats] = useState<ListingStats | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const load = useCallback(async () => {
    setIsLoading(true)
    try {
      const p = await realtorService.getMe()
      if (!p) {
        router.replace("/realtor/onboarding")
        return
      }
      setProfile(p)
    } catch (err) {
      console.error("Failed to load realtor profile:", err)
    } finally {
      setIsLoading(false)
    }
  }, [router])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!user?.id || profile?.verificationStatus !== "VERIFIED") return
    api.get(`/listings/landlord/${user.id}/statistics`)
      .then((res) => setListingStats(res.data))
      .catch(() => setListingStats(null))
  }, [user?.id, profile?.verificationStatus])

  if (isLoading || !profile) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Realtor" showNotifications={false} showLanguage={false} />
        <div className="p-4 space-y-4">
          <Skeleton className="h-40 rounded-2xl" />
          <Skeleton className="h-32 rounded-2xl" />
          <Skeleton className="h-48 rounded-2xl" />
        </div>
      </div>
    )
  }

  const isVerified = profile.verificationStatus === "VERIFIED"

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Realtor dashboard" showNotifications={false} showLanguage={false} />

      <div className="flex-1 overflow-y-auto">
        <div className="p-4 space-y-6 max-w-2xl mx-auto w-full">
          {/* Status card */}
          <div className="bg-white rounded-2xl p-6 shadow-sm text-center space-y-3">
            <h2 className="text-xl font-bold text-slate-900">
              {profile.agencyName || profile.fullName}
            </h2>
            <StatusBadge status={String(profile.verificationStatus)} />

            {profile.verificationStatus === "PENDING" && (
              <p className="text-sm text-slate-500">
                Our team is reviewing your documents. This usually takes 1–2 business days.
              </p>
            )}
            {profile.verificationStatus === "REJECTED" && profile.rejectionReason && (
              <div className="mt-2 p-3 bg-red-50 border border-red-200 rounded-xl text-left">
                <p className="text-sm text-red-700">
                  <strong>Reason:</strong> {profile.rejectionReason}
                </p>
                <Link href="/realtor/onboarding" className="text-sm text-red-700 underline font-medium">
                  Update your details and resubmit
                </Link>
              </div>
            )}
            {profile.verificationStatus === "SUSPENDED" && (
              <p className="text-sm text-slate-500">
                Your realtor account is suspended{profile.rejectionReason ? `: ${profile.rejectionReason}` : "."}
                {" "}Contact support to appeal.
              </p>
            )}
          </div>

          {/* Trust + stats */}
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-white rounded-2xl p-4 shadow-sm text-center">
              <Star className="h-5 w-5 text-amber-500 mx-auto mb-1" />
              <div className="text-2xl font-bold text-slate-900">{profile.trustScore ?? 0}</div>
              <div className="text-xs text-slate-500">Trust score</div>
            </div>
            <div className="bg-white rounded-2xl p-4 shadow-sm text-center">
              <Home className="h-5 w-5 text-blue-500 mx-auto mb-1" />
              <div className="text-2xl font-bold text-slate-900">{profile.totalListings ?? 0}</div>
              <div className="text-xs text-slate-500">Listings</div>
            </div>
            <div className="bg-white rounded-2xl p-4 shadow-sm text-center">
              <Users className="h-5 w-5 text-emerald-500 mx-auto mb-1" />
              <div className="text-2xl font-bold text-slate-900">{profile.successfulRentals ?? 0}</div>
              <div className="text-xs text-slate-500">Rentals</div>
            </div>
          </div>

          {/* Profile summary */}
          <div className="bg-white rounded-2xl p-4 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-slate-900">Agency profile</h3>
              <Button variant="ghost" size="sm" asChild>
                <Link href="/realtor/onboarding" className="flex items-center gap-1 text-slate-600">
                  <Pencil className="h-4 w-4" /> Edit
                </Link>
              </Button>
            </div>
            <div className="space-y-2 text-sm text-slate-700">
              <div className="flex items-center gap-2">
                <Building2 className="h-4 w-4 text-slate-400" />
                {profile.agencyName || "—"}
              </div>
              <div className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-slate-400" />
                {profile.city}
                {profile.areasCovered?.length > 0 && ` · ${profile.areasCovered.join(", ")}`}
              </div>
              {profile.phoneNumber && (
                <div className="flex items-center gap-2">
                  <Phone className="h-4 w-4 text-slate-400" />
                  {profile.phoneNumber}
                </div>
              )}
              {profile.whatsappNumber && (
                <div className="flex items-center gap-2">
                  <MessageCircle className="h-4 w-4 text-slate-400" />
                  {profile.whatsappNumber}
                </div>
              )}
            </div>
          </div>

          {/* Documents */}
          <div className="bg-white rounded-2xl p-4 shadow-sm space-y-3">
            <h3 className="font-semibold text-slate-900 flex items-center gap-2">
              <FileText className="h-4 w-4 text-slate-500" /> Verification documents
            </h3>
            {profile.documents?.length > 0 ? (
              profile.documents.map((d) => (
                <div key={d.id} className="flex items-center justify-between gap-3 p-3 rounded-xl bg-slate-50 text-sm">
                  <span className="text-slate-700">{d.documentType}</span>
                  <span className="text-xs text-slate-500">{d.status}</span>
                </div>
              ))
            ) : (
              <p className="text-sm text-slate-500">
                No documents yet.{" "}
                <Link href="/realtor/onboarding" className="text-violet-600 underline font-medium">
                  Upload one
                </Link>{" "}
                to get verified.
              </p>
            )}
          </div>

          {/* Listings + leads */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {isVerified ? (
              <Link href="/realtor/listings">
                <div className="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 hover:border-violet-300 transition-colors h-full">
                  <Home className="h-5 w-5 text-violet-600 mb-2" />
                  <h4 className="font-medium text-slate-700">Your listings</h4>
                  {listingStats ? (
                    <p className="text-xs text-slate-500 mt-1">
                      {listingStats.activeListings} active · {listingStats.totalListings} total
                    </p>
                  ) : (
                    <p className="text-xs text-slate-500 mt-1">Manage your listings</p>
                  )}
                </div>
              </Link>
            ) : (
              <div className="bg-white rounded-2xl p-4 shadow-sm border border-dashed border-slate-200">
                <Home className="h-5 w-5 text-slate-400 mb-2" />
                <h4 className="font-medium text-slate-700">Your listings</h4>
                <p className="text-xs text-slate-500 mt-1">Available once you're verified.</p>
              </div>
            )}
            <div className="bg-white rounded-2xl p-4 shadow-sm border border-dashed border-slate-200">
              <Users className="h-5 w-5 text-slate-400 mb-2" />
              <h4 className="font-medium text-slate-700">Leads</h4>
              <p className="text-xs text-slate-500 mt-1">Tenant enquiries will appear here soon.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
