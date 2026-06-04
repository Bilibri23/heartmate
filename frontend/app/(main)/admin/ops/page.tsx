"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import type { ReactNode } from "react"
import Link from "next/link"
import api from "@/lib/api"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import {
  Activity,
  AlertTriangle,
  Bot,
  CheckCircle2,
  Database,
  ExternalLink,
  RefreshCw,
  Server,
  ShieldAlert,
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
  deployVersion?: string
  rateLimitMode?: string
  backupLastVerifiedAt?: string | null
  backupRpoHours?: number
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
  code?: string
  title: string
  message: string
  metric?: string
  threshold?: string | number
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

type Queues = Record<string, number | string>

type NoAnswerQuestion = {
  question: string
  role: string
  count: number
  lastSeen: string
}

export default function AdminOpsPage() {
  const [health, setHealth] = useState<Health | null>(null)
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [funnel, setFunnel] = useState<Funnel | null>(null)
  const [errors, setErrors] = useState<ErrorLog[]>([])
  const [queues, setQueues] = useState<Queues | null>(null)
  const [noAnswerQuestions, setNoAnswerQuestions] = useState<NoAnswerQuestion[]>([])
  const [range, setRange] = useState("7d")
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<string | null>(null)

  const loadOps = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const [healthRes, alertsRes, funnelRes, errorsRes, queuesRes, noAnswerRes] = await Promise.all([
        api.get("/admin/ops/health"),
        api.get("/admin/ops/alerts"),
        api.get(`/admin/ops/funnel?range=${range}`),
        api.get("/admin/ops/errors?limit=50"),
        api.get("/admin/ops/queues"),
        api.get(`/admin/ops/ai/no-answer-questions?range=${range}&limit=10`),
      ])
      setHealth(healthRes.data)
      setAlerts(alertsRes.data || [])
      setFunnel(funnelRes.data)
      setErrors(errorsRes.data || [])
      setQueues(queuesRes.data || null)
      setNoAnswerQuestions(noAnswerRes.data || [])
      setLastUpdated(new Date().toISOString())
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

  const queueCards = useMemo(() => [
    { label: "Tenant Verifications", key: "pendingTenantVerifications", href: "/admin/verifications" },
    { label: "Landlord Verifications", key: "pendingLandlordVerifications", href: "/admin/landlord-verifications" },
    { label: "Listings Review", key: "pendingListings", href: "/admin/listings" },
    { label: "Reports", key: "pendingReports", href: "/admin/reports" },
    { label: "Payment Proofs", key: "pendingPaymentProofs", href: "/admin/payments" },
    { label: "Stale Applications", key: "staleApplications", href: "/admin/applications" },
  ], [])

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

        <SectionShell title="System Health" loading={loading} error={loadError} lastUpdated={lastUpdated}>
          <div className="grid gap-3 md:grid-cols-5">
          {healthCards.map((card) => (
            <HealthCard key={card.label} label={card.label} value={card.value} icon={card.icon} />
          ))}
          </div>
        </SectionShell>

        <SectionShell title="Environment" loading={loading} error={loadError} lastUpdated={lastUpdated}>
          <div className="grid gap-3 md:grid-cols-4">
          <OpsMeta label="Deploy" value={shortVersion(health?.deployVersion)} />
          <OpsMeta label="Rate Limit" value={health?.rateLimitMode || "UNKNOWN"} />
          <OpsMeta label="Backup Verified" value={formatTime(health?.backupLastVerifiedAt)} />
          <OpsMeta label="Backup RPO" value={`${health?.backupRpoHours ?? 24}h`} />
          </div>
        </SectionShell>

        <section className="grid gap-5 xl:grid-cols-[1fr_1.2fr]">
          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <SectionHeader title="Critical Alerts" meta={`${alerts.length} active`} loading={loading} error={loadError} lastUpdated={lastUpdated} />
            <div className="space-y-3">
              {loading ? (
                <LoadingState text="Loading alerts..." />
              ) : alerts.length === 0 ? (
                <EmptyState icon={CheckCircle2} text="No active alerts." />
              ) : (
                alerts.map((alert, index) => <AlertRow key={`${alert.title}-${index}`} alert={alert} />)
              )}
            </div>
          </div>

          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <SectionHeader title="Funnel Analytics" loading={loading} error={loadError} lastUpdated={lastUpdated} />
              <div className="flex rounded-xl bg-slate-100 p-1 text-sm">
                {["24h", "7d", "30d", "90d"].map((item) => (
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
            {loading ? (
              <LoadingState text="Loading funnel..." />
            ) : (
              <>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  <Metric label="Feed Opens" value={counts.feed_opened} />
                  <Metric label="Searches" value={counts.search_performed} />
                  <Metric label="Listing Views" value={counts.listing_view} />
                  <Metric label="Saved Listings" value={counts.save_listing} />
                  <Metric label="Applications Started" value={counts.application_started} />
                  <Metric label="Applications Submitted" value={counts.application_submitted} />
                  <Metric label="Applications Approved" value={counts.application_approved} />
                  <Metric label="Leases Signed" value={counts.lease_signed} />
                  <Metric label="Payment Proofs" value={counts.payment_proof_uploaded} />
                </div>
                <div className="mt-4 grid gap-3 md:grid-cols-5">
                  <Rate label="View to save" value={rates.listing_view_to_save} />
                  <Rate label="View to submitted" value={rates.listing_view_to_application_submitted} />
                  <Rate label="Submitted to approved" value={rates.application_submitted_to_approved} />
                  <Rate label="Approved to signed" value={rates.approved_to_lease_signed} />
                  <Rate label="Signed to proof" value={rates.lease_signed_to_payment_proof_uploaded} />
                </div>
              </>
            )}
          </div>
        </section>

        <SectionShell title="Traffic & Marketplace Activity" loading={loading} error={loadError} lastUpdated={lastUpdated}>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
            <Metric label="Feed Opens" value={counts.feed_opened} />
            <Metric label="Searches" value={counts.search_performed} />
            <Metric label="Listing Views" value={counts.listing_view} />
            <Metric label="Saved Listings" value={counts.save_listing} />
            <Metric label="Listing Approved" value={counts.listing_approved} />
            <Metric label="Listing Rejected" value={counts.listing_rejected} />
          </div>
        </SectionShell>

        <section className="grid gap-5 xl:grid-cols-[1fr_1fr]">
          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <div className="mb-4 flex items-start justify-between gap-3">
              <SectionHeader title="Queue Health" loading={loading} error={loadError} lastUpdated={lastUpdated} />
              <ShieldAlert className="h-5 w-5 text-slate-400" />
            </div>
            {loading ? (
              <LoadingState text="Loading queue health..." />
            ) : (
              <div className="grid gap-3 sm:grid-cols-2">
              {queueCards.map((queue) => (
                <QueueCard
                  key={queue.key}
                  label={queue.label}
                  value={Number(queues?.[queue.key] ?? 0)}
                  href={queue.href}
                />
              ))}
              </div>
            )}
          </div>

          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <SectionHeader title="AI Missing Knowledge" meta={`${noAnswerQuestions.length} topics`} loading={loading} error={loadError} lastUpdated={lastUpdated} />
            <div className="space-y-3">
              {loading ? (
                <LoadingState text="Loading AI gaps..." />
              ) : noAnswerQuestions.length === 0 ? (
                <EmptyState icon={CheckCircle2} text="No repeated missing-doc questions in this range." />
              ) : (
                noAnswerQuestions.map((item, index) => (
                  <div key={`${item.question}-${index}`} className="rounded-xl bg-slate-50 p-3">
                    <div className="mb-1 flex items-center justify-between gap-3">
                      <p className="text-sm font-medium text-slate-900">{item.question}</p>
                      <span className="shrink-0 rounded-full bg-blue-100 px-2 py-1 text-xs font-medium text-blue-700">{item.count}</span>
                    </div>
                    <p className="text-xs text-slate-500">{item.role} - last seen {formatTime(item.lastSeen)}</p>
                  </div>
                ))
              )}
            </div>
          </div>
        </section>

        <section className="grid gap-5 xl:grid-cols-[1fr_1.4fr]">
          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <SectionHeader title="AI / GraphRAG Health" loading={loading} error={loadError} lastUpdated={lastUpdated} />
            {loading ? (
              <LoadingState text="Loading AI health..." />
            ) : (
              <>
                <div className="grid grid-cols-2 gap-3">
                  <Metric label="Docs" value={ai?.docs ?? health?.aiDocsCount} />
                  <Metric label="Chunks" value={ai?.chunks ?? health?.aiChunksCount} />
                  <Metric label="Entities" value={ai?.entities ?? health?.graphRagEntityCount} />
                  <Metric label="Edges" value={ai?.edges ?? health?.graphRagEdgeCount} />
                  <Metric label="No Answer 24h" value={ai?.aiNoAnswerCount} />
                </div>
                <p className="mt-4 text-sm text-slate-500">Last ingest: {formatTime(health?.lastSuccessfulAiIngestTime || ai?.lastIngest?.finishedAt)}</p>
              </>
            )}
          </div>

          <div className="rounded-2xl bg-white p-4 shadow-sm">
            <SectionHeader title="Recent Errors" meta={`${errors.length} rows`} loading={loading} error={loadError} lastUpdated={lastUpdated} />
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
                  {loading ? (
                    <tr><td colSpan={5} className="py-8 text-center text-slate-500">Loading recent errors...</td></tr>
                  ) : errors.length === 0 ? (
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

function SectionShell({
  title,
  loading,
  error,
  lastUpdated,
  children,
}: {
  title: string
  loading: boolean
  error: string | null
  lastUpdated: string | null
  children: ReactNode
}) {
  return (
    <section className="rounded-2xl bg-white p-4 shadow-sm">
      <SectionHeader title={title} loading={loading} error={error} lastUpdated={lastUpdated} />
      {loading ? <LoadingState text={`Loading ${title.toLowerCase()}...`} /> : children}
    </section>
  )
}

function SectionHeader({
  title,
  meta,
  loading,
  error,
  lastUpdated,
}: {
  title: string
  meta?: string
  loading: boolean
  error: string | null
  lastUpdated: string | null
}) {
  return (
    <div className="mb-4 min-w-0">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="font-semibold text-slate-950">{title}</h2>
        {meta && <span className="text-sm text-slate-500">{meta}</span>}
      </div>
      <p className="mt-1 text-xs text-slate-500">
        {error ? "Could not refresh this section." : loading ? "Refreshing..." : `Last updated ${formatTime(lastUpdated)}`}
      </p>
    </div>
  )
}

function LoadingState({ text }: { text: string }) {
  return (
    <div className="flex items-center justify-center gap-2 rounded-xl bg-slate-50 p-8 text-sm text-slate-500">
      <RefreshCw className="h-4 w-4 animate-spin" />
      {text}
    </div>
  )
}

function OpsMeta({ label, value }: { label: string; value?: string }) {
  return (
    <div className="rounded-2xl bg-white p-4 shadow-sm">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-1 truncate text-lg font-semibold text-slate-950">{value || "-"}</p>
    </div>
  )
}

function QueueCard({ label, value, href }: { label: string; value: number; href: string }) {
  const tone = value > 20 ? "text-amber-700" : value > 0 ? "text-blue-700" : "text-green-700"
  return (
    <Link href={href} className="group rounded-xl bg-slate-50 p-3 transition hover:bg-slate-100">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className={`text-2xl font-bold ${tone}`}>{value}</p>
          <p className="text-sm text-slate-500">{label}</p>
        </div>
        <ExternalLink className="h-4 w-4 text-slate-400 transition group-hover:text-blue-600" />
      </div>
    </Link>
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
      {(alert.metric || alert.threshold !== undefined) && (
        <p className="mt-2 text-xs text-slate-500">
          {alert.metric || "metric"} threshold: {alert.threshold ?? "-"}
        </p>
      )}
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

function shortVersion(value?: string) {
  if (!value || value === "unknown") return "unknown"
  return value.length > 10 ? value.slice(0, 10) : value
}
