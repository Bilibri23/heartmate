"use client"

import { useCallback, useEffect, useState } from "react"
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
import api from "@/lib/api"
import { Mail, ChevronRight } from "lucide-react"

interface SupportRow {
  id: string
  userId: string
  email?: string
  category?: string
  subject?: string
  message?: string
  createdAt?: string
}

export default function AdminSupportInquiriesPage() {
  const { user } = useAuth()
  const router = useRouter()
  const [authLoading, setAuthLoading] = useState(true)
  const [rows, setRows] = useState<SupportRow[]>([])
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState<SupportRow | null>(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (user === null && authLoading) {
      const t = setTimeout(() => setAuthLoading(false), 500)
      return () => clearTimeout(t)
    }
    if (user) {
      setAuthLoading(false)
      if (user.role !== "ADMIN") router.push("/for-you")
    } else if (!authLoading) {
      router.push("/login")
    }
  }, [user, router, authLoading])

  const fetchRows = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get("/admin/support-inquiries", { params: { page: 0, size: 100 } })
      const content = res.data?.content ?? []
      setRows(Array.isArray(content) ? content : [])
    } catch (e) {
      console.error(e)
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (user?.role === "ADMIN") fetchRows()
  }, [user?.role, fetchRows])

  if (user?.role !== "ADMIN") return null

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Support messages" showBack />

      <div className="p-4 space-y-3">
        <p className="text-sm text-slate-600 bg-white rounded-xl p-3 border border-slate-100">
          These are submissions from <strong>Help &amp; Support</strong> in the app.{" "}
          <span className="text-slate-500">
            Moderation flags (reported listings/users) live under <strong>Reports triage</strong>.
          </span>
        </p>
        <Button variant="outline" className="rounded-xl" onClick={() => fetchRows()} disabled={loading}>
          Refresh
        </Button>

        {loading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-28 rounded-2xl" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-2xl border border-slate-100">
            <Mail className="h-12 w-12 text-slate-300 mx-auto mb-3" />
            <p className="text-slate-600">No support messages yet.</p>
          </div>
        ) : (
          <div className="space-y-2 pb-24">
            {rows.map((r) => (
              <button
                key={r.id}
                type="button"
                className="w-full text-left bg-white rounded-2xl p-4 shadow-sm border border-slate-100 flex gap-3 items-start"
                onClick={() => {
                  setSelected(r)
                  setOpen(true)
                }}
              >
                <Mail className="h-5 w-5 text-blue-600 mt-0.5 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-slate-900 truncate">{r.subject || "(no subject)"}</p>
                  <p className="text-xs text-slate-500 mt-1">
                    {r.email || r.userId} · {r.category || "OTHER"}
                  </p>
                  <p className="text-sm text-slate-600 line-clamp-2 mt-1">{r.message}</p>
                  <p className="text-xs text-slate-400 mt-2">
                    {r.createdAt ? new Date(r.createdAt).toLocaleString() : ""}
                  </p>
                </div>
                <ChevronRight className="h-5 w-5 text-slate-300 shrink-0" />
              </button>
            ))}
          </div>
        )}
      </div>

      <Sheet open={open} onOpenChange={setOpen}>
        <SheetContent side="bottom" className="h-[85vh] rounded-t-3xl overflow-y-auto">
          <SheetHeader>
            <SheetTitle>Message</SheetTitle>
          </SheetHeader>
          {selected && (
            <div className="mt-4 space-y-3 text-sm">
              <div className="rounded-xl bg-slate-50 p-3 space-y-1">
                <p>
                  <span className="text-slate-500">From:</span> {selected.email}
                </p>
                <p>
                  <span className="text-slate-500">User ID:</span>{" "}
                  <span className="font-mono text-xs break-all">{selected.userId}</span>
                </p>
                <p>
                  <span className="text-slate-500">Topic:</span> {selected.category}
                </p>
                <p>
                  <span className="text-slate-500">When:</span>{" "}
                  {selected.createdAt ? new Date(selected.createdAt).toLocaleString() : "—"}
                </p>
              </div>
              <div>
                <p className="font-medium text-slate-800 mb-1">Subject</p>
                <p className="text-slate-700">{selected.subject}</p>
              </div>
              <div>
                <p className="font-medium text-slate-800 mb-1">Message</p>
                <p className="text-slate-700 whitespace-pre-wrap">{selected.message}</p>
              </div>
              <p className="text-xs text-slate-500 pt-2">
                Reply to the user at the email above (or from your mail client using the stored address).
              </p>
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
