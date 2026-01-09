"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Home, Search, MessageCircle, User, Sparkles, Building2, FileText, Plus, Shield, Users, Settings } from "lucide-react"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { cn } from "@/lib/utils"

interface NavItem {
  href: string
  icon: React.ElementType
  label: string
}

// Student navigation items
const studentNavItems: NavItem[] = [
  { href: "/for-you", icon: Sparkles, label: "For You" },
  { href: "/search", icon: Search, label: "Search" },
  { href: "/matches", icon: User, label: "Matches" },
  { href: "/messages", icon: MessageCircle, label: "Messages" },
  { href: "/profile", icon: User, label: "Profile" },
]

// Landlord navigation items
const landlordNavItems: NavItem[] = [
  { href: "/landlord", icon: Home, label: "Dashboard" },
  { href: "/landlord/listings/new", icon: Plus, label: "Add" },
  { href: "/landlord/applications", icon: FileText, label: "Apps" },
  { href: "/messages", icon: MessageCircle, label: "Messages" },
  { href: "/landlord/profile", icon: User, label: "Profile" },
]

// Admin navigation items
const adminNavItems: NavItem[] = [
  { href: "/admin", icon: Home, label: "Dashboard" },
  { href: "/admin/verifications", icon: Shield, label: "Verify" },
  { href: "/admin/listings", icon: Building2, label: "Listings" },
  { href: "/admin/payments", icon: FileText, label: "Payments" },
  { href: "/admin/settings", icon: Settings, label: "Settings" },
]

export function BottomNav() {
  const pathname = usePathname()
  const { user } = useAuth()

  // Don't show on auth pages or landing
  if (pathname?.startsWith("/login") || pathname?.startsWith("/register") || pathname === "/") {
    return null
  }

  // Select nav items based on user role
  const getNavItems = () => {
    if (user?.role === "ADMIN") return adminNavItems
    if (user?.role === "LANDLORD") return landlordNavItems
    return studentNavItems
  }
  const navItems = getNavItems()

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 border-t border-slate-200 bg-white/95 backdrop-blur-lg safe-area-bottom">
      <div className="mx-auto flex h-16 max-w-lg items-center justify-around px-2">
        {navItems.map((item) => {
          const isActive = pathname === item.href || 
            (item.href !== "/" && item.href !== "/landlord" && pathname?.startsWith(item.href)) ||
            (item.href === "/landlord" && pathname === "/landlord")
          const Icon = item.icon

          // Special styling for the Add button (landlord)
          const isAddButton = item.href === "/landlord/listings/new"

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex flex-1 flex-col items-center justify-center gap-1 py-2 transition-colors",
                isAddButton 
                  ? "text-white" 
                  : isActive
                    ? "text-blue-600"
                    : "text-slate-500 hover:text-slate-900"
              )}
            >
              {isAddButton ? (
                <div className="h-10 w-10 rounded-full bg-blue-600 flex items-center justify-center shadow-lg -mt-4">
                  <Icon className="h-5 w-5 text-white" />
                </div>
              ) : (
                <>
                  <Icon className={cn("h-5 w-5", isActive && "scale-110")} />
                  <span className="text-[10px] font-medium">
                    {item.label}
                  </span>
                </>
              )}
            </Link>
          )
        })}
      </div>
    </nav>
  )
}
