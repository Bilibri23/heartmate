"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import api from "@/lib/api"
import { Flag, CheckCircle, XCircle, ChevronRight } from "lucide-react"
import { toast } from "sonner"

interface ReportRow {
  id: string
  reporterName?: string
  reportedEntityType?: string
  reportedEntityName?: string
  reason?: string
  description?: string
  status?: string
  priority?: string
  createdAt?: string
}

export default function AdminReportsPage() {
  const { user } = useAuth()
  const router = useRouter()
  const [reports, setReports] = useState<ReportRow[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [authLoading, setAuthLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [selected, setSelected] = useState<ReportRow | null>(null)
  const [open, setOpen] = useState(false)
  const [resolveNotes, setResolveNotes] = useState("")
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (user === null && authLoading) {
      const t = setTimeout(() => setAuthLoading(false), 500)
      return () => clearTimeout(t)
    }
    if (user) {
      setAuthLoading(false)
      if (user.role !== "ADMIN") {
        router.push("/for-you")
        return
      }
    } else if (!authLoading) {
      router.push("/login")
    }
  }, [user, router, authLoading])

  const fetchReports = useCallback(async () => {
    setIsLoading(true)
    try {
      const params: Record<string, string | number> = { page: 0, size: 50 }
      if (statusFilter !== "all") {
        params.status = statusFilter.toUpperCase()
      }
      const res = await api.get("/admin/reports", { params })
      const content = res.data?.content ?? res.data ?? []
      setReports(Array.isArray(content) ? content : [])
    } catch (e) {
      console.error(e)
      toast.error("Could not load reports")
      setReports([])
    } finally {
      setIsLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    if (user?.role === "ADMIN") fetchReports()
  }, [user?.role, fetchReports])

  const badge = (status?: string) => {
    const s = status || ""
    const map: Record<string, string> = {
      PENDING: "bg-amber-100 text-amber-800",
      REVIEWING: "bg-blue-100 text-blue-800",
      RESOLVED: "bg-emerald-100 text-emerald-800",
      DISMISSED: "bg-slate-100 text-slate-700",
    }
    return (
      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${map[s] || "bg-slate-100"}`}>
        {s || "—"}
      </span>
    )
  }

  const priBadge = (p?: string) => {
    const x = p || ""
    const map: Record<string, string> = {
      LOW: "text-slate-600",
      MEDIUM: "text-amber-700",
      HIGH: "text-orange-700",
      CRITICAL: "text-red-700",
    }
    return <span className={`text-xs font-medium ${map[x] || ""}`}>{x || "—"}</span>
  }

  const updatePriority = async (reportId: string, priority: string) => {
    setBusy(true)
    try {
      await api.put(`/admin/reports/${reportId}/priority`, {}, {
        params: { priority },
      })
      toast.success("Priority updated")
      fetchReports()
      if (selected?.id === reportId) {
        setSelected((r) => (r ? { ...r, priority } : null))
      }
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Update failed")
    } finally {
      setBusy(false)
    }
  }

  const dismissReport = async (reportId: string) => {
    const reason = prompt("Dismiss reason (optional)") || undefined
    setBusy(true)
    try {
      await api.post(`/admin/reports/${reportId}/dismiss`, null, {
        params: reason ? { reason } : {},
      })
      toast.success("Report dismissed")
      setOpen(false)
      fetchReports()
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Dismiss failed")
    } finally {
      setBusy(false)
    }
  }

  const resolveReport = async (reportId: string, action: string) => {
    setBusy(true)
    try {
      await api.post(`/admin/reports/${reportId}/resolve`, null, {
        params: {
          action,
          notes: resolveNotes || undefined,
        },
      })
      toast.success("Report resolved")
      setResolveNotes("")
      setOpen(false)
      fetchReports()
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Resolve failed")
    } finally {
      setBusy(false)
    }
  }

  if (user?.role !== "ADMIN") return null

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Reports" showBack />

      <div className="p-4 space-y-3">
        <div className="flex items-center gap-2">
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="rounded-xl flex-1">
              <SelectValue placeholder="Filter" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All statuses</SelectItem>
              <SelectItem value="pending">Pending</SelectItem>
              <SelectItem value="reviewing">Reviewing</SelectItem>
              <SelectItem value="resolved">Resolved</SelectItem>
              <SelectItem value="dismissed">Dismissed</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" className="rounded-xl" onClick={() => fetchReports()} disabled={isLoading}>
            Refresh
          </Button>
        </div>

        {isLoading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-24 rounded-2xl" />
            ))}
          </div>
        ) : reports.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-2xl">
            <Flag className="h-12 w-12 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-600">No reports match this filter.</p>
          </div>
        ) : (
          <div className="space-y-2 pb-24">
            {reports.map((r) => (
              <button
                key={r.id}
                type="button"
                className="w-full text-left bg-white rounded-2xl p-4 shadow-sm flex gap-3 items-start"
                onClick={() => {
                  setSelected(r)
                  setOpen(true)
                }}
              >
                <div className="mt-0.5">
                  <Flag className="h-5 w-5 text-amber-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <p className="font-semibold text-slate-900 truncate">
                      {r.reportedEntityType || "Report"} · {r.reportedEntityName || "—"}
                    </p>
                    {badge(r.status)}
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    By {r.reporterName || "—"} · {r.reason || "—"}
                  </p>
                  <p className="text-sm text-slate-600 line-clamp-2 mt-1">{r.description}</p>
                  <div className="flex items-center justify-between mt-2 text-xs text-slate-400">
                    <span>{r.createdAt ? new Date(r.createdAt).toLocaleString() : ""}</span>
                    {priBadge(r.priority)}
                  </div>
                </div>
                <ChevronRight className="h-5 w-5 text-slate-300 flex-shrink-0" />
              </button>
            ))}
          </div>
        )}
      </div>

      <Sheet open={open} onOpenChange={setOpen}>
        <SheetContent side="bottom" className="h-[85vh] rounded-t-3xl overflow-y-auto">
          <SheetHeader>
            <SheetTitle>Report detail</SheetTitle>
          </SheetHeader>
          {selected && (
            <div className="mt-4 space-y-4">
              <div className="flex flex-wrap gap-2 items-center">
                {badge(selected.status)}
                <span className="text-sm text-slate-600">
                  Priority:{" "}
                  <span className="font-medium">{selected.priority || "—"}</span>
                </span>
              </div>
              <div className="rounded-xl bg-slate-50 p-3 text-sm space-y-1">
                <p>
                  <span className="text-slate-500">Entity:</span> {selected.reportedEntityType} —{" "}
                  {selected.reportedEntityName}
                </p>
                <p>
                  <span className="text-slate-500">Reporter:</span> {selected.reporterName}
                </p>
                <p>
                  <span className="text-slate-500">Reason:</span> {selected.reason}
                </p>
              </div>
              <div>
                <p className="text-sm font-medium text-slate-700 mb-1">Description</p>
                <p className="text-sm text-slate-600 whitespace-pre-wrap">{selected.description}</p>
              </div>

              <div className="space-y-2">
                <p className="text-sm font-medium">Priority</p>
                <Select
                  value={selected.priority || "MEDIUM"}
                  onValueChange={(v) => updatePriority(selected.id, v)}
                  disabled={busy}
                >
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">Low</SelectItem>
                    <SelectItem value="MEDIUM">Medium</SelectItem>
                    <SelectItem value="HIGH">High</SelectItem>
                    <SelectItem value="CRITICAL">Critical</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <p className="text-sm font-medium mb-1">Resolution notes</p>
                <Input
                  className="rounded-xl"
                  placeholder="Optional notes for resolve"
                  value={resolveNotes}
                  onChange={(e) => setResolveNotes(e.target.value)}
                />
              </div>

              {selected.status === "PENDING" || selected.status === "REVIEWING" ? (
                <div className="space-y-2 pt-2 border-t">
                  <p className="text-sm font-medium">Resolve with action</p>
                  <div className="grid grid-cols-2 gap-2">
                    {["NO_ACTION", "WARNING", "LISTING_DELETED", "USER_WARNED", "SUSPENDED"].map((a) => (
                      <Button
                        key={a}
                        variant="outline"
                        size="sm"
                        className="rounded-xl text-xs"
                        disabled={busy}
                        onClick={() => resolveReport(selected.id, a)}
                      >
                        {a.replace(/_/g, " ")}
                      </Button>
                    ))}
                  </div>
                  <Button
                    variant="outline"
                    className="w-full rounded-xl text-red-600 border-red-200"
                    disabled={busy}
                    onClick={() => dismissReport(selected.id)}
                  >
                    <XCircle className="h-4 w-4 mr-1" /> Dismiss
                  </Button>
                </div>
              ) : (
                <div className="flex items-center gap-2 text-sm text-slate-600">
                  <CheckCircle className="h-4 w-4 text-emerald-600" /> Closed — no further actions
                </div>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
