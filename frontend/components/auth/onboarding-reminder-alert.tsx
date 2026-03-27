"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useState } from "react"
import { Sparkles, X } from "lucide-react"
import { useAuth } from "@/context/auth-context"

const DISMISS_KEY = "roombuddy_dismiss_onboarding_reminder_alert"

export function OnboardingReminderAlert() {
  const { user } = useAuth()
  const pathname = usePathname()
  const [dismissed, setDismissed] = useState(() => {
    if (typeof window === "undefined") return false
    return localStorage.getItem(DISMISS_KEY) === "1"
  })

  if (!user || dismissed || pathname === "/onboarding") {
    return null
  }

  const dismiss = () => {
    setDismissed(true)
    if (typeof window !== "undefined") {
      localStorage.setItem(DISMISS_KEY, "1")
    }
  }

  return (
    <div className="sticky top-14 z-30 border-b border-purple-200 bg-purple-50 px-4 py-3">
      <div className="mx-auto flex max-w-lg items-start gap-3">
        <Sparkles className="mt-0.5 h-5 w-5 shrink-0 text-purple-700" />
        <div className="flex-1">
          <p className="text-sm font-semibold text-purple-900">Browse first, personalize later</p>
          <p className="text-xs text-purple-800">
            Quick onboarding improves recommendations. Open it whenever you are ready.
          </p>
          <div className="mt-2">
            <Link href="/onboarding" className="text-xs font-medium text-purple-700 underline">
              Personalize my feed
            </Link>
          </div>
        </div>
        <button onClick={dismiss} className="text-purple-700">
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
