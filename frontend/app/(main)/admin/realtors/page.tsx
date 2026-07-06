"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Skeleton } from "@/components/ui/skeleton"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { realtorService } from "@/services/realtor.service"
import type { RealtorProfile } from "@/types/realtor"
import { toast } from "sonner"
import {
  Search,
  Shield,
  CheckCircle,
  XCircle,
  ShieldOff,
  Building2,
  MapPin,
  Calendar,
  ExternalLink,
  FileText,
} from "lucide-react"

const STATUS_TABS = ["PENDING", "VERIFIED", "REJECTED", "SUSPENDED"] as const
type StatusTab = (typeof STATUS_TABS)[number]

function statusBadge(status: string) {
  const styles: Record<string, string> = {
    VERIFIED: "bg-emerald-100 text-emerald-700",
    PENDING: "bg-amber-100 text-amber-700",
    REJECTED: "bg-red-100 text-red-700",
    SUSPENDED: "bg-slate-200 text-slate-700",
  }
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[status] || "bg-slate-100"}`}>
      {status}
    </span>
  )
}

export default function AdminRealtorsPage() {
  const [activeTab, setActiveTab] = useState<StatusTab>("PENDING")
  const [realtors, setRealtors] = useState<RealtorProfile[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [selected, setSelected] = useState<RealtorProfile | null>(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)
  const [reason, setReason] = useState("")
  const [actionInFlight, setActionInFlight] = useState<string | null>(null)

  const fetchData = useCallback(async (status: StatusTab) => {
    setIsLoading(true)
    try {
      const page = await realtorService.listByStatus(status, 0, 50)
      setRealtors(page.content || [])
    } catch (err) {
      console.error("Failed to load realtors:", err)
      setRealtors([])
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void fetchData(activeTab)
  }, [activeTab, fetchData])

  const runAction = async (
    action: "approve" | "reject" | "suspend",
    id: string,
  ) => {
    if (actionInFlight) return
    setActionInFlight(`${action}-${id}`)
    try {
      if (action === "approve") await realtorService.approve(id)
      if (action === "reject") await realtorService.reject(id, reason || "Rejected by admin")
      if (action === "suspend") await realtorService.suspend(id, reason || "Suspended by admin")
      toast.success(`Realtor ${action}d`)
      setReason("")
      setIsDetailOpen(false)
      setSelected(null)
      void fetchData(activeTab)
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } }
      toast.error(ax?.response?.data?.message || `Failed to ${action} realtor`)
    } finally {
      setActionInFlight(null)
    }
  }

  const filtered = realtors.filter(
    (r) =>
      r.agencyName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.fullName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.email?.toLowerCase().includes(searchQuery.toLowerCase()),
  )

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Realtors" showBack />
      <div className="p-4 space-y-4">
        {/* Status tabs */}
        <div className="flex gap-2 bg-white rounded-xl p-1 overflow-x-auto">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex-1 whitespace-nowrap py-2 px-3 rounded-lg text-sm font-medium transition-colors ${
                activeTab === tab ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              {tab.charAt(0) + tab.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input
            placeholder="Search agency, name or email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10 rounded-xl"
          />
        </div>

        {isLoading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-24 rounded-2xl" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-2xl">
            <Shield className="h-12 w-12 text-slate-300 mx-auto mb-4" />
            <p className="text-slate-600">No {activeTab.toLowerCase()} realtors found</p>
          </div>
        ) : (
          <div className="space-y-4 pb-24">
            {filtered.map((r) => (
              <div
                key={r.id}
                className="bg-white rounded-2xl p-4 shadow-sm cursor-pointer"
                onClick={() => {
                  setSelected(r)
                  setReason("")
                  setIsDetailOpen(true)
                }}
              >
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <p className="font-semibold text-slate-900">{r.agencyName || r.fullName}</p>
                    <p className="text-sm text-slate-500">{r.email}</p>
                  </div>
                  {statusBadge(String(r.verificationStatus))}
                </div>
                <div className="flex flex-wrap items-center gap-4 text-sm text-slate-500">
                  <span className="flex items-center gap-1">
                    <MapPin className="h-4 w-4" />
                    {r.city}
                  </span>
                  <span className="flex items-center gap-1">
                    <FileText className="h-4 w-4" />
                    {r.documents?.length || 0} doc(s)
                  </span>
                  {r.createdAt && (
                    <span className="flex items-center gap-1">
                      <Calendar className="h-4 w-4" />
                      {new Date(r.createdAt).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Detail / action sheet */}
      <Sheet open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <SheetContent side="bottom" className="rounded-t-2xl max-h-[90vh] overflow-y-auto">
          <SheetHeader>
            <SheetTitle className="flex items-center gap-2">
              <Building2 className="h-5 w-5" />
              {selected?.agencyName || selected?.fullName}
            </SheetTitle>
          </SheetHeader>

          {selected && (
            <div className="mt-4 space-y-4">
              <div className="flex items-center justify-between">
                {statusBadge(String(selected.verificationStatus))}
                <span className="text-sm text-slate-500">Trust score: {selected.trustScore ?? 0}</span>
              </div>

              <div className="space-y-1.5 text-sm text-slate-700">
                <p>
                  <span className="text-slate-400">Name:</span> {selected.fullName}
                </p>
                <p>
                  <span className="text-slate-400">Email:</span> {selected.email}
                </p>
                <p>
                  <span className="text-slate-400">City:</span> {selected.city}
                </p>
                {selected.areasCovered?.length > 0 && (
                  <p>
                    <span className="text-slate-400">Areas:</span> {selected.areasCovered.join(", ")}
                  </p>
                )}
                {selected.businessRegistrationNumber && (
                  <p>
                    <span className="text-slate-400">Business reg #:</span>{" "}
                    {selected.businessRegistrationNumber}
                  </p>
                )}
                {selected.phoneNumber && (
                  <p>
                    <span className="text-slate-400">Phone:</span> {selected.phoneNumber}
                  </p>
                )}
                {selected.whatsappNumber && (
                  <p>
                    <span className="text-slate-400">WhatsApp:</span> {selected.whatsappNumber}
                  </p>
                )}
                {selected.bio && (
                  <p>
                    <span className="text-slate-400">Bio:</span> {selected.bio}
                  </p>
                )}
              </div>

              {/* Documents */}
              <div>
                <h4 className="font-medium text-slate-900 mb-2">Documents</h4>
                {selected.documents?.length > 0 ? (
                  <div className="space-y-2">
                    {selected.documents.map((d) => (
                      <a
                        key={d.id}
                        href={d.documentUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center justify-between gap-3 p-3 rounded-xl bg-slate-50 text-sm hover:bg-slate-100"
                      >
                        <span className="flex items-center gap-2 text-slate-700">
                          <FileText className="h-4 w-4 text-slate-400" />
                          {d.documentType}
                        </span>
                        <span className="flex items-center gap-2 text-xs text-slate-500">
                          {d.status}
                          <ExternalLink className="h-3.5 w-3.5" />
                        </span>
                      </a>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-slate-500">No documents uploaded.</p>
                )}
              </div>

              {selected.rejectionReason && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
                  <strong>Current reason:</strong> {selected.rejectionReason}
                </div>
              )}

              {/* Reason for reject/suspend */}
              <div>
                <label className="text-sm text-slate-600 mb-1.5 block">
                  Reason (for reject / suspend)
                </label>
                <Textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="e.g. Business registration document unreadable"
                  rows={2}
                />
              </div>

              {/* Actions */}
              <div className="grid grid-cols-1 gap-2 pb-4">
                {selected.verificationStatus !== "VERIFIED" && (
                  <Button
                    className="w-full bg-emerald-600 hover:bg-emerald-700"
                    disabled={!!actionInFlight}
                    onClick={() => runAction("approve", selected.id)}
                  >
                    <CheckCircle className="h-4 w-4 mr-1" /> Approve
                  </Button>
                )}
                {selected.verificationStatus !== "REJECTED" && (
                  <Button
                    variant="outline"
                    className="w-full border-red-200 text-red-600 hover:bg-red-50"
                    disabled={!!actionInFlight}
                    onClick={() => runAction("reject", selected.id)}
                  >
                    <XCircle className="h-4 w-4 mr-1" /> Reject
                  </Button>
                )}
                {selected.verificationStatus !== "SUSPENDED" && (
                  <Button
                    variant="outline"
                    className="w-full border-slate-300 text-slate-600 hover:bg-slate-100"
                    disabled={!!actionInFlight}
                    onClick={() => runAction("suspend", selected.id)}
                  >
                    <ShieldOff className="h-4 w-4 mr-1" /> Suspend
                  </Button>
                )}
              </div>
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
