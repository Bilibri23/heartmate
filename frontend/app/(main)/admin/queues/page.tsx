"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import Link from "next/link"
import {
  AlertTriangle,
  CheckCircle2,
  ExternalLink,
  Filter,
  RefreshCw,
  ShieldCheck,
} from "lucide-react"
import api from "@/lib/api"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Textarea } from "@/components/ui/textarea"

type QueueItem = {
  id: string
  queueType: string
  priority: string
  title: string
  description: string
  targetType: string
  targetId?: string | null
  relatedUserId?: string | null
  status: string
  createdAt?: string
  age: string
  suggestedAction: string
  availableActions?: string[]
  href?: string
}

type ActionState = {
  item: QueueItem
  action: string
  note: string
}

const queueTypes = [
  "all",
  "CRITICAL_ALERT",
  "TENANT_VERIFICATION",
  "LANDLORD_VERIFICATION",
  "LISTING_REVIEW",
  "PAYMENT_PROOF",
  "REPORT",
  "AI_UNANSWERED",
  "STALE_APPLICATION",
]

const priorities = ["all", "CRITICAL", "HIGH", "WARNING", "MEDIUM", "INFO"]
const statuses = ["all", "PENDING", "OPEN", "SUBMITTED"]

export default function AdminQueuesPage() {
  const [items, setItems] = useState<QueueItem[]>([])
  const [queueType, setQueueType] = useState("all")
  const [priority, setPriority] = useState("all")
  const [status, setStatus] = useState("all")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<string | null>(null)
  const [actionState, setActionState] = useState<ActionState | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const loadQueues = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams()
      if (queueType !== "all") params.set("queueType", queueType)
      if (priority !== "all") params.set("priority", priority)
      if (status !== "all") params.set("status", status)
      params.set("limit", "150")
      const response = await api.get(`/admin/queues?${params.toString()}`)
      setItems(response.data || [])
      setLastUpdated(new Date().toISOString())
    } catch (err) {
      console.error("Failed to load admin queues", err)
      setError("Queue data could not be loaded.")
    } finally {
      setLoading(false)
    }
  }, [priority, queueType, status])

  useEffect(() => {
    loadQueues()
  }, [loadQueues])

  const summary = useMemo(() => {
    return items.reduce(
      (acc, item) => {
        acc.total += 1
        if (item.priority === "CRITICAL") acc.critical += 1
        if (item.priority === "HIGH") acc.high += 1
        if ((item.availableActions || []).length > 0) acc.actionable += 1
        return acc
      },
      { total: 0, critical: 0, high: 0, actionable: 0 }
    )
  }, [items])

  const submitAction = async () => {
    if (!actionState?.item.targetId) return
    const needsNote = ["REJECT", "RESOLVE", "DISMISS"].includes(actionState.action)
    if (needsNote && !actionState.note.trim()) {
      setError("A note is required for this action.")
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await api.post(`/admin/queues/${actionState.item.queueType}/${actionState.item.targetId}/actions`, {
        action: actionState.action,
        note: actionState.note,
      })
      setActionState(null)
      await loadQueues()
    } catch (err) {
      console.error("Queue action failed", err)
      setError("Queue action could not be completed.")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <MobileHeader title="Control Tower" />
      <main className="mx-auto flex w-full max-w-7xl flex-col gap-5 p-4 pb-24">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-950">Control Tower</h1>
            <p className="text-sm text-slate-500">
              Unified admin triage for reviews, alerts, reports, payments, and AI knowledge gaps.
            </p>
          </div>
          <Button onClick={loadQueues} disabled={loading} className="rounded-xl bg-blue-600 hover:bg-blue-700">
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>

        {error && <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}

        <section className="grid gap-3 md:grid-cols-4">
          <SummaryCard label="Total Items" value={summary.total} />
          <SummaryCard label="Critical" value={summary.critical} tone="red" />
          <SummaryCard label="High Priority" value={summary.high} tone="amber" />
          <SummaryCard label="Actionable" value={summary.actionable} tone="blue" />
        </section>

        <section className="rounded-2xl bg-white p-4 shadow-sm">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <Filter className="h-4 w-4 text-slate-500" />
                <h2 className="font-semibold text-slate-950">Queue Filters</h2>
              </div>
              <p className="mt-1 text-xs text-slate-500">
                {loading ? "Refreshing..." : `Last updated ${formatTime(lastUpdated)}`}
              </p>
            </div>
            <Link href="/admin/ops" className="inline-flex items-center gap-2 text-sm font-medium text-blue-700">
              Operations Center <ExternalLink className="h-4 w-4" />
            </Link>
          </div>
          <div className="grid gap-3 md:grid-cols-3">
            <Select label="Queue type" value={queueType} values={queueTypes} onChange={setQueueType} />
            <Select label="Priority" value={priority} values={priorities} onChange={setPriority} />
            <Select label="Status" value={status} values={statuses} onChange={setStatus} />
          </div>
        </section>

        <section className="rounded-2xl bg-white p-4 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-semibold text-slate-950">Triage Queue</h2>
            <span className="text-sm text-slate-500">{items.length} items</span>
          </div>

          {loading ? (
            <div className="flex items-center justify-center gap-2 rounded-xl bg-slate-50 p-10 text-sm text-slate-500">
              <RefreshCw className="h-4 w-4 animate-spin" />
              Loading queues...
            </div>
          ) : items.length === 0 ? (
            <div className="flex items-center justify-center gap-2 rounded-xl bg-slate-50 p-10 text-sm text-slate-500">
              <CheckCircle2 className="h-5 w-5" />
              No queue items match these filters.
            </div>
          ) : (
            <div className="space-y-3">
              {items.map((item) => (
                <QueueRow key={item.id} item={item} onAction={(action) => setActionState({ item, action, note: "" })} />
              ))}
            </div>
          )}
        </section>
      </main>

      <Dialog open={!!actionState} onOpenChange={(open) => !open && setActionState(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{actionState?.action} queue item</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <p className="text-sm text-slate-600">{actionState?.item.title}</p>
            <Textarea
              value={actionState?.note || ""}
              onChange={(event) => setActionState((current) => current ? { ...current, note: event.target.value } : current)}
              placeholder="Add an admin note or reason"
              className="min-h-28"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setActionState(null)} disabled={submitting}>Cancel</Button>
            <Button onClick={submitAction} disabled={submitting} className="bg-blue-600 hover:bg-blue-700">
              {submitting ? "Working..." : "Confirm"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function QueueRow({ item, onAction }: { item: QueueItem; onAction: (action: string) => void }) {
  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <Severity value={item.priority} />
            <span className="rounded-full bg-white px-2 py-1 text-xs font-medium text-slate-600">{item.queueType}</span>
            <span className="text-xs text-slate-500">{item.age}</span>
          </div>
          <h3 className="font-semibold text-slate-950">{item.title}</h3>
          <p className="mt-1 text-sm text-slate-600">{item.description}</p>
          <p className="mt-2 text-xs text-slate-500">{item.suggestedAction}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {item.href && (
            <Link href={item.href} className="inline-flex h-9 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-100">
              Open <ExternalLink className="h-4 w-4" />
            </Link>
          )}
          {(item.availableActions || []).map((action) => (
            <Button key={action} size="sm" variant={action === "REJECT" || action === "DISMISS" ? "outline" : "default"} onClick={() => onAction(action)}>
              <ShieldCheck className="mr-2 h-4 w-4" />
              {action}
            </Button>
          ))}
        </div>
      </div>
    </div>
  )
}

function Select({
  label,
  value,
  values,
  onChange,
}: {
  label: string
  value: string
  values: string[]
  onChange: (value: string) => void
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none focus:border-blue-500"
      >
        {values.map((item) => (
          <option key={item} value={item}>{item}</option>
        ))}
      </select>
    </label>
  )
}

function SummaryCard({ label, value, tone = "slate" }: { label: string; value: number; tone?: "slate" | "red" | "amber" | "blue" }) {
  const toneClass = {
    slate: "text-slate-950",
    red: "text-red-700",
    amber: "text-amber-700",
    blue: "text-blue-700",
  }[tone]
  return (
    <div className="rounded-2xl bg-white p-4 shadow-sm">
      <p className={`text-3xl font-bold ${toneClass}`}>{value}</p>
      <p className="text-sm text-slate-500">{label}</p>
    </div>
  )
}

function Severity({ value }: { value: string }) {
  const normalized = value?.toUpperCase()
  const tone = normalized === "CRITICAL"
    ? "bg-red-100 text-red-700"
    : normalized === "HIGH" || normalized === "WARNING"
      ? "bg-amber-100 text-amber-700"
      : "bg-blue-100 text-blue-700"
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${tone}`}>
      {normalized === "CRITICAL" && <AlertTriangle className="h-3 w-3" />}
      {normalized || "INFO"}
    </span>
  )
}

function formatTime(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "-"
  return date.toLocaleString()
}
