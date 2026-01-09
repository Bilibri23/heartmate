"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import api from "@/lib/api"
import { Settings, Bell, Shield, Database, Mail, Globe, Save, RefreshCw, LogOut, Users } from "lucide-react"
import Link from "next/link"

export default function AdminSettingsPage() {
  const { user, logout } = useAuth()
  const router = useRouter()
  const [isSaving, setIsSaving] = useState(false)
  const [settings, setSettings] = useState({
    maintenanceMode: false,
    allowNewRegistrations: true,
    requireEmailVerification: true,
    requireStudentVerification: true,
    autoApproveListings: false,
    maxListingsPerLandlord: 10,
    platformFeePercent: 5,
    supportEmail: "support@roombuddy.cm",
    notifyOnNewListing: true,
    notifyOnNewVerification: true,
  })

  const [authLoading, setAuthLoading] = useState(true)

  useEffect(() => {
    if (user === null && authLoading) {
      const timer = setTimeout(() => setAuthLoading(false), 500)
      return () => clearTimeout(timer)
    }
    if (user) {
      setAuthLoading(false)
      if (user.role !== "ADMIN") { router.push("/for-you"); return }
    } else if (!authLoading) {
      router.push("/login")
    }
  }, [user, router, authLoading])

  const handleSave = async () => {
    setIsSaving(true)
    try {
      // In a real app, this would save to backend
      await new Promise(resolve => setTimeout(resolve, 1000))
      alert("Settings saved successfully!")
    } catch (err) {
      console.error(err)
    } finally {
      setIsSaving(false)
    }
  }

  if (user?.role !== "ADMIN") return null

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Settings" showBack />
      <div className="p-4 space-y-6 pb-24">
        {/* General Settings */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Settings className="h-5 w-5 text-blue-600" /> General
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-900">Maintenance Mode</p>
                <p className="text-sm text-slate-500">Disable access for non-admin users</p>
              </div>
              <Switch checked={settings.maintenanceMode} onCheckedChange={(v) => setSettings(s => ({...s, maintenanceMode: v}))} />
            </div>
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-900">Allow New Registrations</p>
                <p className="text-sm text-slate-500">Allow new users to register</p>
              </div>
              <Switch checked={settings.allowNewRegistrations} onCheckedChange={(v) => setSettings(s => ({...s, allowNewRegistrations: v}))} />
            </div>
          </div>
        </div>

        {/* Verification Settings */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Shield className="h-5 w-5 text-green-600" /> Verification
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-900">Require Email Verification</p>
                <p className="text-sm text-slate-500">Users must verify email</p>
              </div>
              <Switch checked={settings.requireEmailVerification} onCheckedChange={(v) => setSettings(s => ({...s, requireEmailVerification: v}))} />
            </div>
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-900">Require Student Verification</p>
                <p className="text-sm text-slate-500">Students must verify before applying</p>
              </div>
              <Switch checked={settings.requireStudentVerification} onCheckedChange={(v) => setSettings(s => ({...s, requireStudentVerification: v}))} />
            </div>
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-900">Auto-Approve Listings</p>
                <p className="text-sm text-slate-500">Skip manual review for listings</p>
              </div>
              <Switch checked={settings.autoApproveListings} onCheckedChange={(v) => setSettings(s => ({...s, autoApproveListings: v}))} />
            </div>
          </div>
        </div>

        {/* Limits */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Database className="h-5 w-5 text-purple-600" /> Limits & Fees
          </h3>
          <div className="space-y-4">
            <div>
              <Label>Max Listings per Landlord</Label>
              <Input type="number" value={settings.maxListingsPerLandlord} onChange={(e) => setSettings(s => ({...s, maxListingsPerLandlord: parseInt(e.target.value)}))} className="rounded-xl mt-1" />
            </div>
            <div>
              <Label>Platform Fee (%)</Label>
              <Input type="number" value={settings.platformFeePercent} onChange={(e) => setSettings(s => ({...s, platformFeePercent: parseInt(e.target.value)}))} className="rounded-xl mt-1" />
            </div>
          </div>
        </div>

        {/* Notifications */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Bell className="h-5 w-5 text-amber-600" /> Admin Notifications
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <p className="font-medium text-slate-900">New Listing Notifications</p>
              <Switch checked={settings.notifyOnNewListing} onCheckedChange={(v) => setSettings(s => ({...s, notifyOnNewListing: v}))} />
            </div>
            <div className="flex items-center justify-between">
              <p className="font-medium text-slate-900">New Verification Notifications</p>
              <Switch checked={settings.notifyOnNewVerification} onCheckedChange={(v) => setSettings(s => ({...s, notifyOnNewVerification: v}))} />
            </div>
          </div>
        </div>

        {/* Contact */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Mail className="h-5 w-5 text-red-600" /> Contact
          </h3>
          <div>
            <Label>Support Email</Label>
            <Input value={settings.supportEmail} onChange={(e) => setSettings(s => ({...s, supportEmail: e.target.value}))} className="rounded-xl mt-1" />
          </div>
        </div>

        <Button onClick={handleSave} disabled={isSaving} className="w-full rounded-xl bg-blue-600 hover:bg-blue-700">
          {isSaving ? <RefreshCw className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
          Save Settings
        </Button>

        {/* Quick Links */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Users className="h-5 w-5 text-indigo-600" /> Quick Links
          </h3>
          <div className="space-y-2">
            <Link href="/admin/users" className="flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 transition-colors">
              <span className="font-medium text-slate-700">User Management</span>
              <Users className="h-5 w-5 text-slate-400" />
            </Link>
          </div>
        </div>

        {/* Logout */}
        <Button 
          variant="outline" 
          onClick={logout} 
          className="w-full rounded-xl border-red-200 text-red-600 hover:bg-red-50 hover:text-red-700"
        >
          <LogOut className="h-4 w-4 mr-2" />
          Logout
        </Button>
      </div>
    </div>
  )
}
