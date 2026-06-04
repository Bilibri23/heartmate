"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { Eye, RefreshCw, Search, ShieldCheck } from "lucide-react"
import api from "@/lib/api"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"

type AuditLog = {
  id: string
  userId?: string
  userEmail?: string
  userRole?: string
  action: string
  entityType?: string
  entityId?: string
  details?: string
  status?: string
  errorMessage?: string
  createdAt: string
}

type PageResponse = {
  content?: AuditLog[]
  totalElements?: number
  totalPages?: number
  number?: number
  size?: number
}

export default function AdminAuditPage() {
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [keyword, setKeyword] = useState("")
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<AuditLog | null>(null)
  const [lastUpdated, setLastUpdated] = useState<string | null>(null)

  const loadAudit = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams({ page: String(page), size: "30" })
      const endpoint = keyword.trim() ? "/admin/audit-logs/search" : "/admin/audit-logs"
      if (keyword.trim()) params.set("keyword", keyword.trim())
      const response = await api.get(`${endpoint}?${params.toString()}`)
      const data = response.data as PageResponse
      setLogs(data.content || [])
      setTotalPages(data.totalPages || 0)
      setTotalElements(data.totalElements || 0)
      setLastUpdated(new Date().toISOString())
    } catch (err) {
      console.error("Failed to load audit logs", err)
      setError("Audit logs could not be loaded.")
    } finally {
      setLoading(false)
    }
  }, [keyword, page])

  useEffect(() => {
    loadAudit()
  }, [loadAudit])

  const actionCounts = useMemo(() => {
    return logs.reduce<Record<string, number>>((acc, log) => {
      acc[log.action] = (acc[log.action] || 0) + 1
      return acc
    }, {})
  }, [logs])

  return (
    <div className="min-h-screen bg-slate-50">
      <MobileHeader title="Audit Trail" />
      <main className="mx-auto flex w-full max-w-7xl flex-col gap-5 p-4 pb-24">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-950">Audit Trail</h1>
            <p className="text-sm text-slate-500">Searchable record of admin and operational actions.</p>
          </div>
          <Button onClick={loadAudit} disabled={loading} className="rounded-xl bg-blue-600 hover:bg-blue-700">
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>

        {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}

        <section className="grid gap-3 md:grid-cols-3">
          <Summary label="Rows Loaded" value={logs.length} />
          <Summary label="Total Matches" value={totalElements} />
          <Summary label="Action Types" value={Object.keys(actionCounts).length} />
        </section>

        <section className="rounded-2xl bg-white p-4 shadow-sm">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="font-semibold text-slate-950">Filters</h2>
              <p className="mt-1 text-xs text-slate-500">
                {loading ? "Refreshing..." : `Last updated ${formatTime(lastUpdated)}`}
              </p>
            </div>
            <div className="flex w-full gap-2 md:w-auto">
              <div className="relative flex-1 md:w-80">
                <Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <Input
                  value={keyword}
                  onChange={(event) => {
                    setPage(0)
                    setKeyword(event.target.value)
                  }}
                  placeholder="Search action"
                  className="pl-9"
                />
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="border-b border-slate-100 text-xs uppercase text-slate-500">
                <tr>
                  <th className="py-2 pr-3">Time</th>
                  <th className="py-2 pr-3">Status</th>
                  <th className="py-2 pr-3">Admin</th>
                  <th className="py-2 pr-3">Action</th>
                  <th className="py-2 pr-3">Target</th>
                  <th className="py-2">Details</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan={6} className="py-10 text-center text-slate-500">Loading audit logs...</td></tr>
                ) : logs.length === 0 ? (
                  <tr><td colSpan={6} className="py-10 text-center text-slate-500">No audit logs match this filter.</td></tr>
                ) : (
                  logs.map((log) => (
                    <tr key={log.id} className="border-b border-slate-50">
                      <td className="py-3 pr-3 text-slate-500">{formatTime(log.createdAt)}</td>
                      <td className="py-3 pr-3"><Status value={log.status || "SUCCESS"} /></td>
                      <td className="py-3 pr-3 text-slate-700">{log.userEmail || log.userId || "-"}</td>
                      <td className="py-3 pr-3 font-medium text-slate-900">{log.action}</td>
                      <td className="py-3 pr-3 text-slate-600">{log.entityType || "-"} {shortId(log.entityId)}</td>
                      <td className="py-3">
                        <Button variant="outline" size="sm" onClick={() => setSelected(log)}>
                          <Eye className="mr-2 h-4 w-4" />
                          View
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex items-center justify-between">
            <Button variant="outline" disabled={page <= 0 || loading} onClick={() => setPage((value) => Math.max(value - 1, 0))}>
              Previous
            </Button>
            <span className="text-sm text-slate-500">Page {page + 1} of {Math.max(totalPages, 1)}</span>
            <Button variant="outline" disabled={page >= totalPages - 1 || loading} onClick={() => setPage((value) => value + 1)}>
              Next
            </Button>
          </div>
        </section>
      </main>

      <Dialog open={!!selected} onOpenChange={(open) => !open && setSelected(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{selected?.action}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 text-sm">
            <Row label="Admin" value={selected?.userEmail || selected?.userId} />
            <Row label="Target" value={`${selected?.entityType || "-"} ${selected?.entityId || ""}`} />
            <Row label="Status" value={selected?.status} />
            <div>
              <p className="mb-1 font-medium text-slate-700">Details</p>
              <pre className="max-h-72 overflow-auto rounded-xl bg-slate-50 p-3 text-xs text-slate-700 whitespace-pre-wrap">
                {selected?.details || selected?.errorMessage || "-"}
              </pre>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function Summary({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-100 text-blue-700">
          <ShieldCheck className="h-5 w-5" />
        </div>
        <div>
          <p className="text-2xl font-bold text-slate-950">{value}</p>
          <p className="text-sm text-slate-500">{label}</p>
        </div>
      </div>
    </div>
  )
}

function Status({ value }: { value: string }) {
  const normalized = value.toUpperCase()
  const tone = normalized === "FAILURE" ? "bg-red-100 text-red-700" : "bg-green-100 text-green-700"
  return <span className={`rounded-full px-2 py-1 text-xs font-medium ${tone}`}>{normalized}</span>
}

function Row({ label, value }: { label: string; value?: string }) {
  return (
    <div>
      <p className="font-medium text-slate-700">{label}</p>
      <p className="text-slate-600">{value || "-"}</p>
    </div>
  )
}

function formatTime(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "-"
  return date.toLocaleString()
}

function shortId(value?: string) {
  if (!value) return ""
  return value.length > 8 ? value.slice(0, 8) : value
}
