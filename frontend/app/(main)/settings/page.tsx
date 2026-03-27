"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { 
  Globe, 
  Bell, 
  Shield, 
  HelpCircle, 
  FileText,
  LogOut,
  ChevronRight,
  Moon,
  Smartphone
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import Link from "next/link"

export default function SettingsPage() {
  const { t, language, setLanguage } = useLanguage()
  const { logout } = useAuth()
  const router = useRouter()
  
  const [notifications, setNotifications] = useState({
    push: true,
    email: true,
    messages: true,
    applications: true,
    payments: true,
  })

  const handleLogout = () => {
    logout()
    router.push("/")
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.nav.settings} />

      <div className="flex-1 overflow-y-auto">
        <div className="p-4 space-y-6">
          {/* Language */}
          <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-900 flex items-center gap-2">
                <Globe className="h-5 w-5 text-blue-600" />
                {t.common.language}
              </h3>
            </div>
            <div className="p-4">
              <Select value={language} onValueChange={(v: "en" | "fr") => setLanguage(v)}>
                <SelectTrigger className="h-12 rounded-xl">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="fr">🇫🇷 Français</SelectItem>
                  <SelectItem value="en">🇬🇧 English</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Notifications */}
          <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-900 flex items-center gap-2">
                <Bell className="h-5 w-5 text-amber-600" />
                {t.notifications.title}
              </h3>
            </div>
            <div className="divide-y divide-slate-100">
              <div className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium text-slate-900">Push Notifications</p>
                  <p className="text-sm text-slate-500">Receive push notifications</p>
                </div>
                <Switch 
                  checked={notifications.push}
                  onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, push: checked }))}
                />
              </div>
              <div className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium text-slate-900">Email Notifications</p>
                  <p className="text-sm text-slate-500">Receive email updates</p>
                </div>
                <Switch 
                  checked={notifications.email}
                  onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, email: checked }))}
                />
              </div>
              <div className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium text-slate-900">{t.messages.title}</p>
                  <p className="text-sm text-slate-500">New message alerts</p>
                </div>
                <Switch 
                  checked={notifications.messages}
                  onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, messages: checked }))}
                />
              </div>
              <div className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium text-slate-900">{t.applications.title}</p>
                  <p className="text-sm text-slate-500">Application status updates</p>
                </div>
                <Switch 
                  checked={notifications.applications}
                  onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, applications: checked }))}
                />
              </div>
              <div className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium text-slate-900">{t.payments.title}</p>
                  <p className="text-sm text-slate-500">Payment reminders</p>
                </div>
                <Switch 
                  checked={notifications.payments}
                  onCheckedChange={(checked) => setNotifications(prev => ({ ...prev, payments: checked }))}
                />
              </div>
            </div>
          </div>

          {/* Privacy & Security */}
          <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-900 flex items-center gap-2">
                <Shield className="h-5 w-5 text-emerald-600" />
                Privacy & Security
              </h3>
            </div>
            <div className="divide-y divide-slate-100">
              <Link href="/settings/password" className="flex items-center justify-between p-4">
                <p className="font-medium text-slate-900">Change Password</p>
                <ChevronRight className="h-5 w-5 text-slate-400" />
              </Link>
              <Link href="/verification" className="flex items-center justify-between p-4">
                <p className="font-medium text-slate-900">{t.verification.title}</p>
                <ChevronRight className="h-5 w-5 text-slate-400" />
              </Link>
            </div>
          </div>

          {/* Support */}
          <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-900 flex items-center gap-2">
                <HelpCircle className="h-5 w-5 text-purple-600" />
                Support
              </h3>
            </div>
            <div className="divide-y divide-slate-100">
              <Link href="/help" className="flex items-center justify-between p-4">
                <p className="font-medium text-slate-900">Help Center</p>
                <ChevronRight className="h-5 w-5 text-slate-400" />
              </Link>
              <Link href="/contact" className="flex items-center justify-between p-4">
                <p className="font-medium text-slate-900">Contact Us</p>
                <ChevronRight className="h-5 w-5 text-slate-400" />
              </Link>
              <Link href="/terms" className="flex items-center justify-between p-4">
                <p className="font-medium text-slate-900">Terms of Service</p>
                <ChevronRight className="h-5 w-5 text-slate-400" />
              </Link>
              <Link href="/privacy" className="flex items-center justify-between p-4">
                <p className="font-medium text-slate-900">Privacy Policy</p>
                <ChevronRight className="h-5 w-5 text-slate-400" />
              </Link>
            </div>
          </div>

          {/* App Info */}
          <div className="bg-white rounded-2xl shadow-sm p-4">
            <div className="flex items-center gap-3">
              <div className="h-12 w-12 rounded-xl bg-blue-600 flex items-center justify-center">
                <Smartphone className="h-6 w-6 text-white" />
              </div>
              <div>
                <p className="font-semibold text-slate-900">RoomBay</p>
                <p className="text-sm text-slate-500">Version 1.0.0</p>
              </div>
            </div>
          </div>

          {/* Logout */}
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button 
                variant="outline" 
                className="w-full h-12 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
              >
                <LogOut className="h-5 w-5 mr-2" />
                {t.auth.logout}
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent className="rounded-2xl">
              <AlertDialogHeader>
                <AlertDialogTitle>Logout</AlertDialogTitle>
                <AlertDialogDescription>
                  Are you sure you want to logout?
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel className="rounded-xl">{t.common.cancel}</AlertDialogCancel>
                <AlertDialogAction 
                  onClick={handleLogout}
                  className="rounded-xl bg-red-600 hover:bg-red-700"
                >
                  {t.auth.logout}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>

          <div className="h-8" />
        </div>
      </div>
    </div>
  )
}
