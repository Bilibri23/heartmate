"use client"

import { Bell, Globe, ArrowLeft } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useLanguage } from "@/context/language-context"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

interface MobileHeaderProps {
  title?: string
  showNotifications?: boolean
  showLanguage?: boolean
  showBack?: boolean
}

export function MobileHeader({ 
  title = "RoomBuddy", 
  showNotifications = true,
  showLanguage = true,
  showBack = false
}: MobileHeaderProps) {
  const { language, setLanguage, t } = useLanguage()
  const router = useRouter()

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur-lg safe-area-top">
      <div className="flex h-14 items-center justify-between px-4">
        <div className="flex items-center gap-2">
          {showBack && (
            <Button 
              variant="ghost" 
              size="icon" 
              className="h-9 w-9"
              onClick={() => router.back()}
            >
              <ArrowLeft className="h-5 w-5 text-slate-600" />
            </Button>
          )}
          <h1 className="text-lg font-bold text-slate-900">{title}</h1>
        </div>
        
        <div className="flex items-center gap-2">
          {showLanguage && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-9 w-9">
                  <Globe className="h-5 w-5 text-slate-600" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem 
                  onClick={() => setLanguage("en")}
                  className={language === "en" ? "bg-blue-50" : ""}
                >
                  🇬🇧 English
                </DropdownMenuItem>
                <DropdownMenuItem 
                  onClick={() => setLanguage("fr")}
                  className={language === "fr" ? "bg-blue-50" : ""}
                >
                  🇫🇷 Français
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
          
          {showNotifications && (
            <Link href="/notifications">
              <Button variant="ghost" size="icon" className="relative h-9 w-9">
                <Bell className="h-5 w-5 text-slate-600" />
                {/* Notification badge - will be dynamic */}
                <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-red-500" />
              </Button>
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
