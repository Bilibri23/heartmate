"use client"

import { useState, useEffect } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { 
  User, 
  Mail, 
  Phone, 
  Shield, 
  ChevronRight, 
  LogOut,
  Heart,
  FileText,
  CreditCard,
  MessageSquare,
  Settings,
  HelpCircle,
  CheckCircle,
  AlertCircle,
  Users,
  Sparkles
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import Link from "next/link"
import { useRouter } from "next/navigation"
import api from "@/lib/api"

interface MenuItem {
  icon: React.ElementType
  label: string
  href: string
  badge?: string
  badgeColor?: string
}

export default function ProfilePage() {
  const { t } = useLanguage()
  const { user, logout } = useAuth()
  const router = useRouter()
  const [isVerified, setIsVerified] = useState(false)

  // Fetch verification status
  useEffect(() => {
    const fetchVerificationStatus = async () => {
      if (!user?.id) return
      
      try {
        const response = await api.get(`/verifications/${user.id}`)
        setIsVerified(response.data?.status === "VERIFIED")
      } catch (err: any) {
        // User might not have verification record yet
        setIsVerified(false)
      }
    }

    fetchVerificationStatus()
  }, [user?.id])

  const handleLogout = () => {
    logout()
    router.push("/login")
  }

  const menuItems: MenuItem[] = [
    { icon: User, label: t.profile.editProfile, href: "/profile/edit" },
    { icon: Sparkles, label: "My Preferences", href: "/preferences" },
    { icon: Users, label: "Find Roommates", href: "/matches" },
    { icon: Heart, label: t.nav.favorites, href: "/favorites" },
    { icon: FileText, label: t.nav.applications, href: "/applications" },
    { icon: FileText, label: t.nav.leases, href: "/leases" },
    { icon: CreditCard, label: t.nav.payments, href: "/payments" },
    { icon: MessageSquare, label: t.reviews.title, href: "/reviews" },
  ]

  const settingsItems: MenuItem[] = [
    { icon: Settings, label: t.nav.settings, href: "/settings" },
    { icon: HelpCircle, label: "Help & Support", href: "/help" },
  ]

  const getInitials = (firstName?: string, lastName?: string) => {
    return `${firstName?.[0] || ""}${lastName?.[0] || ""}`.toUpperCase() || "U"
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.nav.profile} />

      <div className="flex-1 overflow-y-auto">
        {/* Profile Header */}
        <div className="bg-white px-4 py-6 border-b border-slate-100">
          <div className="flex items-center gap-4">
            <Avatar className="h-20 w-20 border-2 border-slate-100">
              <AvatarImage src={undefined} />
              <AvatarFallback className="bg-blue-100 text-blue-600 text-xl font-semibold">
                {getInitials(user?.firstName, user?.lastName)}
              </AvatarFallback>
            </Avatar>
            
            <div className="flex-1">
              <h2 className="text-xl font-bold text-slate-900">
                {user?.firstName} {user?.lastName}
              </h2>
              <p className="text-sm text-slate-500 flex items-center gap-1 mt-0.5">
                <Mail className="h-3.5 w-3.5" />
                {user?.email}
              </p>
              {user?.phone && (
                <p className="text-sm text-slate-500 flex items-center gap-1 mt-0.5">
                  <Phone className="h-3.5 w-3.5" />
                  {user.phone}
                </p>
              )}
            </div>
          </div>

          {/* Verification Status */}
          <div className="mt-4">
            {isVerified ? (
              <div className="flex items-center gap-2 px-3 py-2 bg-emerald-50 rounded-xl">
                <CheckCircle className="h-5 w-5 text-emerald-600" />
                <span className="text-sm font-medium text-emerald-700">
                  {t.profile.verified}
                </span>
              </div>
            ) : (
              <Link href="/verification">
                <div className="flex items-center justify-between px-3 py-2 bg-amber-50 rounded-xl">
                  <div className="flex items-center gap-2">
                    <AlertCircle className="h-5 w-5 text-amber-600" />
                    <span className="text-sm font-medium text-amber-700">
                      {t.profile.notVerified}
                    </span>
                  </div>
                  <Button size="sm" variant="ghost" className="text-amber-700">
                    {t.profile.verifyNow}
                    <ChevronRight className="h-4 w-4 ml-1" />
                  </Button>
                </div>
              </Link>
            )}
          </div>
        </div>

        {/* Menu Items */}
        <div className="p-4 space-y-4">
          {/* Main Menu */}
          <div className="bg-white rounded-2xl overflow-hidden shadow-sm">
            {menuItems.map((item, index) => {
              const Icon = item.icon
              return (
                <Link key={item.href} href={item.href}>
                  <div className={`flex items-center justify-between px-4 py-3.5 ${
                    index !== menuItems.length - 1 ? "border-b border-slate-100" : ""
                  }`}>
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100">
                        <Icon className="h-5 w-5 text-slate-600" />
                      </div>
                      <span className="text-sm font-medium text-slate-700">
                        {item.label}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      {item.badge && (
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                          item.badgeColor || "bg-blue-100 text-blue-600"
                        }`}>
                          {item.badge}
                        </span>
                      )}
                      <ChevronRight className="h-5 w-5 text-slate-400" />
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>

          {/* Settings Menu */}
          <div className="bg-white rounded-2xl overflow-hidden shadow-sm">
            {settingsItems.map((item, index) => {
              const Icon = item.icon
              return (
                <Link key={item.href} href={item.href}>
                  <div className={`flex items-center justify-between px-4 py-3.5 ${
                    index !== settingsItems.length - 1 ? "border-b border-slate-100" : ""
                  }`}>
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100">
                        <Icon className="h-5 w-5 text-slate-600" />
                      </div>
                      <span className="text-sm font-medium text-slate-700">
                        {item.label}
                      </span>
                    </div>
                    <ChevronRight className="h-5 w-5 text-slate-400" />
                  </div>
                </Link>
              )
            })}
          </div>

          {/* Logout Button */}
          <Button 
            variant="outline" 
            className="w-full h-12 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
            onClick={handleLogout}
          >
            <LogOut className="h-5 w-5 mr-2" />
            {t.nav.logout}
          </Button>

          {/* App Version */}
          <p className="text-center text-xs text-slate-400 pt-4">
            RoomBay v1.0.0
          </p>
        </div>
      </div>
    </div>
  )
}
