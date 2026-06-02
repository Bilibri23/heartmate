"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import api from "@/lib/api"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import {
  Activity,
  AlertTriangle,
  Bot,
  CheckCircle2,
  Database,
  RefreshCw,
  Server,
  Wifi,
} from "lucide-react"

type Health = {
  backendStatus?: string
  databaseStatus?: string
  websocketStatus?: string
  aiIngestionStatus?: string
  aiDocsCount?: number
  aiChunksCount?: number
  graphRagEntityCount?: number
  graphRagEdgeCount?: number
  lastSuccessfulAiIngestTime?: string | null
  uptimeSeconds?: number
  environment?: string
  timestamp?: string
  aiHealth?: AiHealth
}

type AiHealth = {
  docs?: number
  chunks?: number
  entities?: number
  edges?: number
  aiNoAnswerCount?: number
  lastIngest?: { finishedAt?: string } | null
}

type Alert = {
  severity: "INFO" | "WARNING" | "CRITICAL"
  title: string
  message: string
  createdAt: string
  suggestedAction: string
}

type Funnel = {
  range: string
  counts?: Record<string, number>
  conversionRates?: Record<string, number>
}

type ErrorLog = {
  id: string
  level: string
  source: string
  message: string
  path?: string
  createdAt: string
}

export default function AdminOpsPage() {
  const [health, setHealth] = useState<Health | null>(null)
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [funnel, setFunnel] = useState<Funnel | null>(null)
  const [errors, setErrors] = useState<ErrorLog[]>([])
  const [range, setRange] = useState("7d")
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const loadOps = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const [healthRes, alertsRes, funnelRes, errorsRes] = await Promise.all([
        api.get("/admin/ops/health"),
        api.get("/admin/ops/alerts"),
        api.get(`/admin/ops/funnel?range=${range}`),
        api.get("/admin/ops/errors?limit=50"),
      ])
      setHealth(healthRes.data)
      setAlerts(alertsRes.data || [])
      setFunnel(funnelRes.data)
      setErrors(errorsRes.data || [])
    } catch (err) {
      console.error("Failed to load ops dashboard", err)
      setLoadError("Operations data could not be loaded.")
    } finally {
      setLoading(false)
    }
  }, [range])

  useEffect(() => {
    loadOps()
  }, [loadOps])

  const counts = funnel?.counts || {}
  const rates = funnel?.conversionRates || {}
  const ai = health?.aiHealth

  const healthCards = useMemo(() => [
    { label: "Backend", value: health?.backendStatus || "UNKNOWN", icon: Server },
    { label: "Database", value: health?.databaseStatus || "UNKNOWN", icon: Database },
    { label: "WebSocket", value: health?.websocketStatus || "UNKNOWN", icon: Wifi },
    { label: "AI / GraphRAG", value: health?.aiIngestionStatus || "UNKNOWN", icon: Bot },
    { label: "Environment", value: health?.environment || "unknown", icon: Activity },
  ], [health])

  return (
    <div className="min-h-screen bg-slate-50">
      <MobileHeader title="Operations Center" />

      <main className="mx-auto flex w-full max-w-7xl flex-col gap-5 p-4 pb-24">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-950">Operations Center</h1>
            <p className="text-sm text-slate-500">Platform health, funnel analytics, alerts, and recent errors.</p>
          </div>
          <Button onClick={loadOps} disabled={loading} className="rounded-xl bg-blue-600 hover:bg-blue-700">
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>

        {loadError && (
          <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{loadError}</div>
        )}

        <section className="grid gap-3 md:grid-cols-5">
          {healthCards.map((card) => (
            <HealthCard key={card.label} label={card.label} value={card.value} icon={card.icon} />
          ))}
        </section>

        <section className="grid gap-5 xl:grid-cols-[1fr_1.2fr]">
          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-semibold text-slate-950">Alerts</h2>
              <span className="text-sm text-slate-500">{alerts.length} active</span>
            </div>
            <div className="space-y-3">
              {alerts.length === 0 ? (
                <EmptyState icon={CheckCircle2} text="No active alerts." />
              ) : (
                alerts.map((alert, index) => <AlertRow key={`${alert.title}-${index}`} alert={alert} />)
              )}
            </div>
          </div>

          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <h2 className="font-semibold text-slate-950">Funnel Analytics</h2>
              <div className="flex rounded-xl bg-slate-100 p-1 text-sm">
                {["24h", "7d", "30d"].map((item) => (
                  <button
                    key={item}
                    onClick={() => setRange(item)}
                    className={`rounded-lg px-3 py-1 ${range === item ? "bg-white text-blue-700 shadow-sm" : "text-slate-600"}`}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              <Metric label="Feed Opens" value={counts.feed_opened} />
              <Metric label="Searches" value={counts.search_performed} />
              <Metric label="Listing Views" value={counts.listing_view} />
              <Metric label="Saved Listings" value={counts.save_listing} />
              <Metric label="Applications Submitted" value={counts.application_submitted} />
              <Metric label="Applications Approved" value={counts.application_approved} />
              <Metric label="Leases Signed" value={counts.lease_signed} />
            </div>
            <div className="mt-4 grid gap-3 md:grid-cols-3">
              <Rate label="View to submitted" value={rates.listing_view_to_application_submitted} />
              <Rate label="Submitted to approved" value={rates.application_submitted_to_approved} />
              <Rate label="Approved to signed" value={rates.approved_to_lease_signed} />
            </div>
          </div>
        </section>

        <section className="grid gap-5 xl:grid-cols-[1fr_1.4fr]">
          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <h2 className="mb-4 font-semibold text-slate-950">AI Health</h2>
            <div className="grid grid-cols-2 gap-3">
              <Metric label="Docs" value={ai?.docs ?? health?.aiDocsCount} />
              <Metric label="Chunks" value={ai?.chunks ?? health?.aiChunksCount} />
              <Metric label="Entities" value={ai?.entities ?? health?.graphRagEntityCount} />
              <Metric label="Edges" value={ai?.edges ?? health?.graphRagEdgeCount} />
              <Metric label="No Answer 24h" value={ai?.aiNoAnswerCount} />
            </div>
            <p className="mt-4 text-sm text-slate-500">Last ingest: {formatTime(health?.lastSuccessfulAiIngestTime || ai?.lastIngest?.finishedAt)}</p>
          </div>

          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <h2 className="mb-4 font-semibold text-slate-950">Recent Errors</h2>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead className="border-b border-slate-100 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="py-2 pr-3">Time</th>
                    <th className="py-2 pr-3">Level</th>
                    <th className="py-2 pr-3">Source</th>
                    <th className="py-2 pr-3">Path</th>
                    <th className="py-2">Message</th>
                  </tr>
                </thead>
                <tbody>
                  {errors.length === 0 ? (
                    <tr><td colSpan={5} className="py-8 text-center text-slate-500">No recent errors.</td></tr>
                  ) : (
                    errors.map((error) => (
                      <tr key={error.id} className="border-b border-slate-50">
                        <td className="py-3 pr-3 text-slate-500">{formatTime(error.createdAt)}</td>
                        <td className="py-3 pr-3"><Severity value={error.level} /></td>
                        <td className="py-3 pr-3 text-slate-700">{error.source}</td>
                        <td className="py-3 pr-3 text-slate-500">{error.path || "-"}</td>
                        <td className="py-3 text-slate-700">{error.message}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}

function HealthCard({ label, value, icon: Icon }: { label: string; value: string; icon: any }) {
  const ok = ["UP", "READY", "prod", "production"].includes(String(value).toLowerCase()) || value === "UP" || value === "READY"
  return (
    <div className="rounded-2xl bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className={`flex h-11 w-11 items-center justify-center rounded-xl ${ok ? "bg-green-100 text-green-700" : "bg-amber-100 text-amber-700"}`}>
          <Icon className="h-5 w-5" />
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm text-slate-500">{label}</p>
          <p className="truncate text-lg font-semibold text-slate-950">{value}</p>
        </div>
      </div>
    </div>
  )
}

function AlertRow({ alert }: { alert: Alert }) {
  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50 p-3">
      <div className="mb-2 flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-4 w-4 text-amber-600" />
          <p className="font-medium text-slate-900">{alert.title}</p>
        </div>
        <Severity value={alert.severity} />
      </div>
      <p className="text-sm text-slate-600">{alert.message}</p>
      <p className="mt-2 text-xs text-slate-500">{alert.suggestedAction}</p>
    </div>
  )
}

function Severity({ value }: { value: string }) {
  const normalized = value?.toUpperCase()
  const tone = normalized === "CRITICAL" || normalized === "ERROR"
    ? "bg-red-100 text-red-700"
    : normalized === "WARNING" || normalized === "WARN"
      ? "bg-amber-100 text-amber-700"
      : "bg-blue-100 text-blue-700"
  return <span className={`rounded-full px-2 py-1 text-xs font-medium ${tone}`}>{normalized || "INFO"}</span>
}

function Metric({ label, value }: { label: string; value?: number }) {
  return (
    <div className="rounded-xl bg-slate-50 p-3">
      <p className="text-2xl font-bold text-slate-950">{value ?? 0}</p>
      <p className="text-sm text-slate-500">{label}</p>
    </div>
  )
}

function Rate({ label, value }: { label: string; value?: number }) {
  return (
    <div className="rounded-xl border border-slate-100 p-3">
      <p className="text-xl font-bold text-slate-950">{(value ?? 0).toFixed(2)}%</p>
      <p className="text-sm text-slate-500">{label}</p>
    </div>
  )
}

function EmptyState({ icon: Icon, text }: { icon: any; text: string }) {
  return (
    <div className="flex items-center justify-center gap-2 rounded-xl bg-slate-50 p-8 text-sm text-slate-500">
      <Icon className="h-5 w-5" />
      {text}
    </div>
  )
}

function formatTime(value?: string | null) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "-"
  return date.toLocaleString()
}
