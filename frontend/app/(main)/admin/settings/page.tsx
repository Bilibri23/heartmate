"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import api, { uploadApi } from "@/lib/api"
import { toast } from "sonner"
import { Settings, Bell, Shield, Database, Mail, Globe, Save, RefreshCw, LogOut, Users, Search } from "lucide-react"
import Link from "next/link"

export default function AdminSettingsPage() {
  const { user, logout } = useAuth()
  const router = useRouter()
  const [isSaving, setIsSaving] = useState(false)
  const [isAiIngesting, setIsAiIngesting] = useState(false)
  const [settings, setSettings] = useState({
    maintenanceMode: false,
    allowNewRegistrations: true,
    requireEmailVerification: true,
    requireStudentVerification: true,
    autoApproveListings: false,
    maxListingsPerLandlord: 10,
    platformFeePercent: 5,
    supportEmail: "support@roombay.cm",
    notifyOnNewListing: true,
    notifyOnNewVerification: true,
  })

  const [authLoading, setAuthLoading] = useState(true)
  const [isReindexing, setIsReindexing] = useState(false)
  const [searchStatus, setSearchStatus] = useState<{ elasticsearchActive: boolean; indexDocumentCount: number; hint: string } | null>(null)

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

  const fetchSearchStatus = async () => {
    try {
      // Use direct backend URL (same as reindex) - avoids Next.js proxy 404
      const res = await uploadApi.get("/admin/search/status")
      setSearchStatus(res.data)
    } catch {
      setSearchStatus(null)
    }
  }

  useEffect(() => {
    if (user?.role === "ADMIN") fetchSearchStatus()
  }, [user?.role])

  const handleReindexSearch = async () => {
    setIsReindexing(true)
    try {
      const res = await uploadApi.post("/admin/search/reindex")
      const msg = res.data?.message || res.data?.data
      toast.success(msg || "Reindex started. Listings will appear in search within ~15 seconds.")
      setTimeout(() => fetchSearchStatus(), 20000) // Refresh status after indexer runs
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Reindex failed")
    } finally {
      setIsReindexing(false)
    }
  }

  const handleSave = async () => {
    setIsSaving(true)
    try {
      // In a real app, this would save to backend
      await new Promise(resolve => setTimeout(resolve, 1000))
      toast.success("Settings saved successfully!")
    } catch (err) {
      console.error(err)
      toast.error("Failed to save settings")
    } finally {
      setIsSaving(false)
    }
  }

  const handleAiIngest = async (force: boolean) => {
    setIsAiIngesting(true)
    try {
      const res = await uploadApi.post(`/ai/admin/ingest?force=${force ? "true" : "false"}`)
      const data = res.data
      toast.success(
        `AI docs ingested: ${data?.documentsProcessed ?? 0} docs, ${data?.chunksInserted ?? 0} chunks`
      )
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "AI ingest failed")
    } finally {
      setIsAiIngesting(false)
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

        {/* Search Index (Elasticsearch) */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Search className="h-5 w-5 text-blue-600" /> Search Index
          </h3>
          {searchStatus ? (
            <p className={`text-sm mb-2 ${searchStatus.indexDocumentCount > 0 ? "text-green-600" : "text-amber-600"}`}>
              {searchStatus.elasticsearchActive
                ? `${searchStatus.indexDocumentCount} documents in index`
                : "Elasticsearch disabled or unavailable"}
            </p>
          ) : (
            <p className="text-sm mb-2 text-slate-400">Status unavailable — restart backend if you just added this.</p>
          )}
          <p className="text-sm text-slate-500 mb-4">If search shows no results, reindex to sync ACTIVE listings to Elasticsearch.</p>
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleReindexSearch} disabled={isReindexing} className="rounded-xl">
              {isReindexing ? "Reindexing…" : "Reindex Search"}
            </Button>
            <Button variant="ghost" size="sm" onClick={fetchSearchStatus} className="rounded-xl">
              Refresh status
            </Button>
          </div>
        </div>

        {/* AI Assistant (RAG) */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Globe className="h-5 w-5 text-indigo-600" /> AI Assistant
          </h3>
          <p className="text-sm text-slate-500 mb-4">
            Ingest `docs/*.md` into pgvector so the assistant can answer questions with citations.
          </p>
          <div className="flex gap-2 flex-wrap">
            <Button
              variant="outline"
              onClick={() => handleAiIngest(false)}
              disabled={isAiIngesting}
              className="rounded-xl"
            >
              {isAiIngesting ? "Ingesting…" : "Ingest Docs"}
            </Button>
            <Button
              variant="ghost"
              onClick={() => handleAiIngest(true)}
              disabled={isAiIngesting}
              className="rounded-xl"
              title="Re-embed everything even if unchanged"
            >
              Force Re-ingest
            </Button>
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
