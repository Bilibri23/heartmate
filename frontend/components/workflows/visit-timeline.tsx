"use client"

import { useEffect, useState } from "react"
import { Check, Loader2 } from "lucide-react"
import api from "@/lib/api"
import type { ApplicationTimeline, TimelineStep } from "@/types/visit"

/**
 * Read-only progress stepper for an application, backed by
 * GET /api/applications/{id}/timeline. Mount it only when visible — it fetches once.
 */
export function VisitTimeline({ applicationId }: { applicationId: string }) {
  const [timeline, setTimeline] = useState<ApplicationTimeline | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const res = await api.get(`/applications/${applicationId}/timeline`)
        if (active) setTimeline(res.data)
      } catch (err) {
        console.error("Failed to load timeline:", err)
      } finally {
        if (active) setIsLoading(false)
      }
    }
    load()
    return () => {
      active = false
    }
  }, [applicationId])

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-slate-500 py-2">
        <Loader2 className="h-4 w-4 animate-spin" /> Loading progress…
      </div>
    )
  }

  if (!timeline) {
    return <p className="text-sm text-slate-500 py-2">Progress unavailable right now.</p>
  }

  return (
    <ol className="space-y-3 py-1">
      {timeline.steps.map((step: TimelineStep, idx: number) => {
        const done = step.status === "DONE"
        const current = step.status === "CURRENT"
        return (
          <li key={step.key} className="flex items-start gap-3">
            <div className="flex flex-col items-center">
              <span
                className={`flex h-6 w-6 items-center justify-center rounded-full text-xs ${
                  done
                    ? "bg-emerald-500 text-white"
                    : current
                    ? "bg-blue-500 text-white"
                    : "bg-slate-200 text-slate-400"
                }`}
              >
                {done ? <Check className="h-3.5 w-3.5" /> : idx + 1}
              </span>
              {idx < timeline.steps.length - 1 && (
                <span className={`w-px flex-1 min-h-4 ${done ? "bg-emerald-300" : "bg-slate-200"}`} />
              )}
            </div>
            <div className="pb-1">
              <p className={`text-sm ${done || current ? "font-medium text-slate-900" : "text-slate-500"}`}>
                {step.label}
              </p>
              {step.at && (
                <p className="text-xs text-slate-400">
                  {new Date(step.at).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                </p>
              )}
              {current && <p className="text-xs text-blue-600">In progress</p>}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
