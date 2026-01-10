"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { 
  Building2,
  Settings,
  HelpCircle,
  LogOut,
  ChevronRight,
  Shield,
  Star,
  FileText,
  Bell,
  CreditCard,
  Users,
  TrendingUp,
  BadgeCheck,
  Edit
} from "lucide-react"
import Link from "next/link"
import api from "@/lib/api"

interface LandlordProfile {
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string
  profilePhotoUrl?: string
  verified: boolean
  createdAt: string
  totalListings: number
  activeListings: number
  totalTenants: number
  averageRating: number
  reviewsCount: number
}

export default function LandlordProfilePage() {
  const { t } = useLanguage()
  const { user, logout } = useAuth()
  const router = useRouter()
  const [profile, setProfile] = useState<LandlordProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const fetchProfile = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      // Fetch stats - profile data comes from auth context
      const statsRes = await api.get(`/listings/landlord/${user.id}/statistics`)
      
      // Try to get profile for photo and bio
      let profilePhotoUrl: string | undefined
      try {
        const profileRes = await api.get(`/profiles/${user.id}`)
        profilePhotoUrl = profileRes.data?.profilePhotoUrl
      } catch {
        // Profile may not exist yet
      }
      
      // Also try to get user details for photo
      try {
        const userRes = await api.get(`/users/${user.id}`)
        if (!profilePhotoUrl && userRes.data?.profilePhotoUrl) {
          profilePhotoUrl = userRes.data.profilePhotoUrl
        }
      } catch {
        // User endpoint may not exist
      }
      
      setProfile({
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email || "",
        phone: user.phone || "",
        profilePhotoUrl: profilePhotoUrl,
        verified: false,
        createdAt: new Date().toISOString(),
        totalListings: statsRes.data?.totalListings || 0,
        activeListings: statsRes.data?.activeListings || 0,
        totalTenants: statsRes.data?.totalTenants || 0,
        averageRating: statsRes.data?.averageRating || 0,
        reviewsCount: statsRes.data?.reviewsCount || 0,
      })
    } catch (err) {
      console.error("Failed to fetch profile:", err)
      // Set basic profile from auth context
      setProfile({
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email || "",
        phone: user.phone || "",
        verified: false,
        createdAt: new Date().toISOString(),
        totalListings: 0,
        activeListings: 0,
        totalTenants: 0,
        averageRating: 0,
        reviewsCount: 0,
      })
    } finally {
      setIsLoading(false)
    }
  }, [user])

  useEffect(() => {
    fetchProfile()
  }, [fetchProfile])

  const handleLogout = () => {
    logout()
    router.push("/login")
  }

  const menuItems = [
    {
      section: "Properties",
      items: [
        { icon: Building2, label: "My Listings", href: "/landlord", badge: profile?.activeListings },
        { icon: FileText, label: "Applications", href: "/landlord/applications" },
        { icon: FileText, label: "Leases", href: "/landlord/leases" },
        { icon: Users, label: "Tenants", href: "/landlord/tenants" },
        { icon: CreditCard, label: "Payments", href: "/landlord/payments" },
      ]
    },
    {
      section: "Account",
      items: [
        { icon: Shield, label: "Verification", href: "/landlord/verification", badge: profile?.verified ? "✓" : "!" },
        { icon: TrendingUp, label: "Analytics", href: "/landlord/analytics" },
        { icon: Bell, label: "Notifications", href: "/notifications" },
        { icon: Settings, label: "Settings", href: "/settings" },
      ]
    },
    {
      section: "Support",
      items: [
        { icon: HelpCircle, label: "Help Center", href: "/help" },
      ]
    }
  ]

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Profile" />

      <div className="flex-1 overflow-y-auto pb-24">
        {/* Profile Header */}
        <div className="bg-gradient-to-br from-blue-600 to-purple-700 px-4 pt-6 pb-8">
          {isLoading ? (
            <div className="flex items-center gap-4">
              <Skeleton className="h-20 w-20 rounded-full" />
              <div className="space-y-2">
                <Skeleton className="h-6 w-32 bg-white/20" />
                <Skeleton className="h-4 w-24 bg-white/20" />
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-4">
              <Avatar className="h-20 w-20 ring-4 ring-white/30">
                <AvatarImage src={profile?.profilePhotoUrl} />
                <AvatarFallback className="bg-white text-blue-600 text-2xl font-bold">
                  {profile?.firstName?.[0]}{profile?.lastName?.[0]}
                </AvatarFallback>
              </Avatar>
              
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <h1 className="text-xl font-bold text-white">
                    {profile?.firstName} {profile?.lastName}
                  </h1>
                  {profile?.verified && (
                    <BadgeCheck className="h-5 w-5 text-emerald-300" />
                  )}
                </div>
                <p className="text-white/80 text-sm">Landlord</p>
                
                {/* Rating */}
                {profile?.reviewsCount && profile.reviewsCount > 0 && (
                  <div className="flex items-center gap-1 mt-1">
                    <Star className="h-4 w-4 text-amber-400 fill-amber-400" />
                    <span className="text-white font-medium">{profile.averageRating.toFixed(1)}</span>
                    <span className="text-white/60 text-sm">({profile.reviewsCount} reviews)</span>
                  </div>
                )}
              </div>
              
              {/* Edit Button */}
              <Link href="/landlord/profile/edit">
                <Button size="icon" variant="ghost" className="h-10 w-10 rounded-full bg-white/20 hover:bg-white/30">
                  <Edit className="h-5 w-5 text-white" />
                </Button>
              </Link>
            </div>
          )}

          {/* Stats */}
          <div className="grid grid-cols-3 gap-3 mt-6">
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-3 text-center">
              <p className="text-2xl font-bold text-white">{profile?.totalListings || 0}</p>
              <p className="text-xs text-white/70">Listings</p>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-3 text-center">
              <p className="text-2xl font-bold text-white">{profile?.activeListings || 0}</p>
              <p className="text-xs text-white/70">Active</p>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-3 text-center">
              <p className="text-2xl font-bold text-white">{profile?.totalTenants || 0}</p>
              <p className="text-xs text-white/70">Tenants</p>
            </div>
          </div>
        </div>

        {/* Menu Sections */}
        <div className="p-4 space-y-6">
          {menuItems.map((section) => (
            <div key={section.section}>
              <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2 px-1">
                {section.section}
              </h2>
              <div className="bg-white rounded-2xl overflow-hidden shadow-sm">
                {section.items.map((item, index) => {
                  const Icon = item.icon
                  return (
                    <Link
                      key={item.label}
                      href={item.href}
                      className={`flex items-center gap-3 px-4 py-3.5 hover:bg-slate-50 transition-colors ${
                        index !== section.items.length - 1 ? "border-b border-slate-100" : ""
                      }`}
                    >
                      <div className="h-9 w-9 rounded-xl bg-slate-100 flex items-center justify-center">
                        <Icon className="h-5 w-5 text-slate-600" />
                      </div>
                      <span className="flex-1 font-medium text-slate-900">{item.label}</span>
                      {item.badge && (
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                          item.badge === "!" 
                            ? "bg-amber-100 text-amber-700" 
                            : item.badge === "✓"
                            ? "bg-emerald-100 text-emerald-700"
                            : "bg-blue-100 text-blue-700"
                        }`}>
                          {item.badge}
                        </span>
                      )}
                      <ChevronRight className="h-5 w-5 text-slate-400" />
                    </Link>
                  )
                })}
              </div>
            </div>
          ))}

          {/* Logout */}
          <Button
            variant="outline"
            className="w-full h-12 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
            onClick={handleLogout}
          >
            <LogOut className="h-5 w-5 mr-2" />
            Log Out
          </Button>
        </div>
      </div>
    </div>
  )
}
