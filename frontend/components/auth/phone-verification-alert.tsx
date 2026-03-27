"use client"

import Link from "next/link"
import { useState } from "react"
import { Smartphone, X } from "lucide-react"
import { useAuth } from "@/context/auth-context"

const DISMISS_KEY = "roombuddy_dismiss_phone_verification_alert"

export function PhoneVerificationAlert() {
  const { user } = useAuth()
  const [dismissed, setDismissed] = useState(() => {
    if (typeof window === "undefined") return false
    return localStorage.getItem(DISMISS_KEY) === "1"
  })

  if (!user || user.phoneVerified !== false || dismissed) {
    return null
  }

  const dismiss = () => {
    setDismissed(true)
    if (typeof window !== "undefined") {
      localStorage.setItem(DISMISS_KEY, "1")
    }
  }

  return (
    <div className="sticky top-14 z-30 border-b border-blue-200 bg-blue-50 px-4 py-3">
      <div className="mx-auto flex max-w-lg items-start gap-3">
        <Smartphone className="mt-0.5 h-5 w-5 shrink-0 text-blue-700" />
        <div className="flex-1">
          <p className="text-sm font-semibold text-blue-900">Add phone verification for full access</p>
          <p className="text-xs text-blue-800">
            You can browse now. Messaging, applications, payments, and publishing require a verified phone.
          </p>
          <div className="mt-2 flex items-center gap-2">
            <Link href="/verification" className="text-xs font-medium text-blue-700 underline">
              Continue verification
            </Link>
            <button className="text-xs text-blue-700 underline" onClick={dismiss}>
              Dismiss
            </button>
          </div>
        </div>
        <button onClick={dismiss} className="text-blue-700">
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
