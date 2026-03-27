"use client"

import { useState } from "react"
import { MailWarning, RefreshCw, X } from "lucide-react"
import { toast } from "sonner"
import { useAuth } from "@/context/auth-context"
import api from "@/lib/api"
import { Button } from "@/components/ui/button"

const DISMISS_KEY = "roombuddy_dismiss_email_verification_alert"

export function EmailVerificationAlert() {
  const { user } = useAuth()
  const [isSending, setIsSending] = useState(false)
  const [dismissed, setDismissed] = useState(() => {
    if (typeof window === "undefined") return false
    return localStorage.getItem(DISMISS_KEY) === "1"
  })

  if (!user || user.emailVerified !== false || dismissed) {
    return null
  }

  const dismiss = () => {
    setDismissed(true)
    if (typeof window !== "undefined") {
      localStorage.setItem(DISMISS_KEY, "1")
    }
  }

  const handleResend = async () => {
    setIsSending(true)
    try {
      await api.post("/auth/resend-verification-email")
      toast.success("Verification email sent. Check inbox/spam.")
    } catch {
      toast.error("Could not resend verification email.")
    } finally {
      setIsSending(false)
    }
  }

  return (
    <div className="sticky top-14 z-30 border-b border-amber-200 bg-amber-50 px-4 py-3">
      <div className="mx-auto flex max-w-lg items-start gap-3">
        <MailWarning className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />
        <div className="flex-1">
          <p className="text-sm font-semibold text-amber-900">Verify your email to unlock all actions</p>
          <p className="text-xs text-amber-800">You can browse now, but messaging/applying may be limited until verification.</p>
          <div className="mt-2 flex items-center gap-2">
            <Button size="sm" className="h-8 rounded-lg" onClick={handleResend} disabled={isSending}>
              <RefreshCw className="mr-1 h-3.5 w-3.5" />
              {isSending ? "Sending..." : "Resend email"}
            </Button>
            <button className="text-xs text-amber-700 underline" onClick={dismiss}>
              Dismiss
            </button>
          </div>
        </div>
        <button onClick={dismiss} className="text-amber-700">
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
