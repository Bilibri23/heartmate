"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useAuth } from "@/context/auth-context"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { CalendarClock, MapPin, User as UserIcon } from "lucide-react"
import { toast } from "sonner"
import api from "@/lib/api"
import type { Visit, VisitStatus } from "@/types/visit"

type Filter = "all" | "REQUESTED" | "ACCEPTED" | "COMPLETED"

const STATUS_LABEL: Record<VisitStatus, { label: string; cls: string }> = {
  REQUESTED: { label: "Requested", cls: "bg-amber-50 text-amber-700" },
  ACCEPTED: { label: "Confirmed", cls: "bg-emerald-50 text-emerald-700" },
  RESCHEDULED: { label: "Rescheduled", cls: "bg-blue-50 text-blue-700" },
  COMPLETED: { label: "Completed", cls: "bg-emerald-50 text-emerald-800" },
  CANCELLED: { label: "Cancelled", cls: "bg-red-50 text-red-700" },
  NO_SHOW: { label: "No-show", cls: "bg-slate-100 text-slate-700" },
}

function formatWhen(iso?: string | null) {
  if (!iso) return "—"
  return new Date(iso).toLocaleString("en-GB", {
    weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit",
  })
}

export default function LandlordVisitsPage() {
  const { user } = useAuth()
  const [visits, setVisits] = useState<Visit[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [filter, setFilter] = useState<Filter>("all")
  const [selected, setSelected] = useState<Visit | null>(null)
  const [rescheduleAt, setRescheduleAt] = useState("")
  const [response, setResponse] = useState("")
  const [isSaving, setIsSaving] = useState(false)

  const fetchVisits = useCallback(async () => {
    if (!user?.id) return
    setIsLoading(true)
    try {
      const params: Record<string, string | number> = { size: 50 }
      if (filter !== "all") params.status = filter
      const res = await api.get("/visits/landlord/received", { params })
      setVisits(res.data?.content || res.data || [])
    } catch (err) {
      console.error("Failed to fetch visits:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id, filter])

  useEffect(() => {
    fetchVisits()
  }, [fetchVisits])

  const openManage = (visit: Visit) => {
    setSelected(visit)
    setRescheduleAt("")
    setResponse("")
  }

  const submit = async (status: VisitStatus) => {
    if (!selected) return
    if (status === "RESCHEDULED" && !rescheduleAt) {
      toast.error("Pick a new date and time to reschedule")
      return
    }
    setIsSaving(true)
    try {
      const visitDatetime = rescheduleAt ? `${rescheduleAt}:00`.slice(0, 19) : undefined
      await api.put(`/visits/${selected.id}`, {
        status,
        visitDatetime,
        response: response || undefined,
        reason: response || undefined,
      })
      toast.success("Visit updated")
      setSelected(null)
      fetchVisits()
    } catch (err) {
      const e = err as { response?: { data?: { message?: string } } }
      toast.error(e?.response?.data?.message || "Failed to update visit")
    } finally {
      setIsSaving(false)
    }
  }

  const filters: { id: Filter; label: string }[] = [
    { id: "all", label: "All" },
    { id: "REQUESTED", label: "Requested" },
    { id: "ACCEPTED", label: "Confirmed" },
    { id: "COMPLETED", label: "Completed" },
  ]

  const isConfirmed = selected && (selected.status === "ACCEPTED" || selected.status === "RESCHEDULED")
  const isActive = selected && (selected.status === "REQUESTED" || selected.status === "ACCEPTED" || selected.status === "RESCHEDULED")

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Visit Requests" />

      <div className="sticky top-14 z-30 bg-white border-b border-slate-200">
        <div className="flex px-4 gap-2 py-2 overflow-x-auto scrollbar-hide">
          {filters.map((f) => (
            <button
              key={f.id}
              onClick={() => setFilter(f.id)}
              className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                filter === f.id ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {isLoading && [1, 2, 3].map((i) => (
          <div key={i} className="bg-white rounded-2xl p-4 shadow-sm">
            <Skeleton className="h-5 w-40 mb-2" />
            <Skeleton className="h-4 w-full" />
          </div>
        ))}

        {!isLoading && visits.length === 0 && (
          <div className="text-center py-12">
            <div className="text-6xl mb-4">📅</div>
            <h3 className="text-lg font-semibold text-slate-900 mb-2">No visit requests</h3>
            <p className="text-slate-500 text-sm">Tenants who request a viewing on your listings show up here.</p>
          </div>
        )}

        {!isLoading && visits.map((visit) => {
          const badge = STATUS_LABEL[visit.status]
          return (
            <div key={visit.id} className="bg-white rounded-2xl shadow-sm p-4">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <h3 className="font-semibold text-slate-900 line-clamp-1">{visit.listingTitle}</h3>
                  <p className="text-sm text-slate-500 flex items-center gap-1 mt-0.5">
                    <UserIcon className="h-3.5 w-3.5" /> {visit.tenantName || "Tenant"}
                  </p>
                  <p className="text-sm text-slate-700 mt-1 flex items-center gap-1">
                    <CalendarClock className="h-3.5 w-3.5 text-slate-400" />
                    {formatWhen(visit.visitDatetime || visit.requestedDatetime)}
                  </p>
                  {visit.listingCity && (
                    <p className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                      <MapPin className="h-3 w-3" /> {[visit.listingNeighborhood, visit.listingCity].filter(Boolean).join(", ")}
                    </p>
                  )}
                </div>
                <span className={`text-xs font-medium px-2.5 py-1 rounded-full whitespace-nowrap ${badge.cls}`}>
                  {badge.label}
                </span>
              </div>

              {visit.tenantMessage && (
                <div className="mt-3 p-3 bg-slate-50 rounded-xl">
                  <p className="text-sm text-slate-700">{visit.tenantMessage}</p>
                </div>
              )}

              {visit.isActive && (
                <div className="mt-3">
                  <Button size="sm" className="w-full rounded-xl" onClick={() => openManage(visit)}>
                    Manage request
                  </Button>
                </div>
              )}
            </div>
          )
        })}
      </div>

      <Sheet open={!!selected} onOpenChange={(open) => !open && setSelected(null)}>
        <SheetContent side="bottom" className="rounded-t-3xl">
          <SheetHeader>
            <SheetTitle>Manage visit</SheetTitle>
          </SheetHeader>
          {selected && (
            <div className="space-y-4 py-2">
              <div className="text-sm text-slate-600">
                <p className="font-medium text-slate-900">{selected.listingTitle}</p>
                <p>{selected.tenantName} · requested {formatWhen(selected.requestedDatetime)}</p>
              </div>

              <div>
                <Label htmlFor="response">Note to tenant (optional)</Label>
                <Textarea
                  id="response"
                  value={response}
                  onChange={(e) => setResponse(e.target.value)}
                  placeholder="e.g. See you at the gate, call on arrival."
                  className="mt-1"
                />
              </div>

              <div>
                <Label htmlFor="reschedule">Propose a new time (for reschedule)</Label>
                <Input
                  id="reschedule"
                  type="datetime-local"
                  value={rescheduleAt}
                  onChange={(e) => setRescheduleAt(e.target.value)}
                  className="mt-1"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                {isActive && (
                  <Button disabled={isSaving} className="rounded-xl" onClick={() => submit("ACCEPTED")}>
                    Accept
                  </Button>
                )}
                {isActive && (
                  <Button disabled={isSaving} variant="outline" className="rounded-xl" onClick={() => submit("RESCHEDULED")}>
                    Reschedule
                  </Button>
                )}
                {isConfirmed && (
                  <Button disabled={isSaving} variant="outline" className="rounded-xl" onClick={() => submit("COMPLETED")}>
                    Mark completed
                  </Button>
                )}
                {isConfirmed && (
                  <Button disabled={isSaving} variant="outline" className="rounded-xl" onClick={() => submit("NO_SHOW")}>
                    No-show
                  </Button>
                )}
                {isActive && (
                  <Button
                    disabled={isSaving}
                    variant="outline"
                    className="rounded-xl text-red-600 border-red-200 hover:bg-red-50 col-span-2"
                    onClick={() => submit("CANCELLED")}
                  >
                    Cancel visit
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
