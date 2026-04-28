"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useAuth } from "@/context/auth-context"
import { authService } from "@/services/auth.service"
import type { AuthMeResponse } from "@/types/auth"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { BadgeCheck, Mail, MessageCircle, Loader2, ChevronRight } from "lucide-react"
import { toast } from "sonner"

function toCameroonE164(raw: string | null | undefined): string | null {
  if (!raw) return null
  const trimmed = raw.trim().replaceAll(/\s+/g, "")
  if (/^\+237\d{9}$/.test(trimmed)) return trimmed
  const digits = trimmed.replaceAll(/\D/g, "")
  if (digits.length === 12 && digits.startsWith("237")) return `+${digits}`
  if (digits.length === 9) return `+237${digits}`
  return null
}

function profileEditHref(role: string | undefined) {
  if (role === "LANDLORD") return "/landlord/profile/edit"
  return "/profile/edit"
}

export default function AccountVerifyPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { user, isLoading: authLoading, refreshUser } = useAuth()
  const [me, setMe] = useState<AuthMeResponse | null>(null)
  const [loadingMe, setLoadingMe] = useState(true)
  const [emailSending, setEmailSending] = useState(false)
  const [otpSending, setOtpSending] = useState(false)
  const [otpVerifying, setOtpVerifying] = useState(false)
  const [otpResending, setOtpResending] = useState(false)
  const [otpCode, setOtpCode] = useState("")
  const [otpHintSent, setOtpHintSent] = useState(false)
  const emailStatus = searchParams.get("emailStatus")

  const loadMe = useCallback(async () => {
    setLoadingMe(true)
    try {
      const data = await authService.getMe()
      setMe(data)
    } catch {
      setMe(null)
    } finally {
      setLoadingMe(false)
    }
  }, [])

  useEffect(() => {
    if (!authLoading && !user) {
      router.replace("/login")
      return
    }
    if (user) void loadMe()
  }, [authLoading, user, router, loadMe])

  const emailVerified = Boolean(me?.emailVerified)
  const phoneVerified = Boolean(me?.phoneVerified)
  const contactOk = emailVerified || phoneVerified
  const phoneE164 = toCameroonE164(me?.phone ?? undefined)
  const phoneForSend =
    me?.phone && /^\+237\d{9}$/.test(me.phone.trim()) ? me.phone.trim() : phoneE164

  const handleResendEmail = async () => {
    setEmailSending(true)
    try {
      await authService.resendVerificationEmail()
      toast.success("Check your inbox", {
        description: "We sent a new verification link if your email was not verified yet.",
      })
      await loadMe()
      await refreshUser()
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } }
      toast.error(ax.response?.data?.message || "Could not send email")
    } finally {
      setEmailSending(false)
    }
  }

  const handleSendOtp = async () => {
    if (!phoneForSend) {
      toast.error("Add a valid Cameroon phone on your profile first (+237 and 9 digits).")
      return
    }
    setOtpSending(true)
    try {
      await authService.sendPhoneVerificationOtp(phoneForSend)
      setOtpHintSent(true)
      toast.success("Code sent", { description: "Check WhatsApp on the number we have on file." })
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } }
      toast.error(ax.response?.data?.message || "Could not send OTP")
    } finally {
      setOtpSending(false)
    }
  }

  const handleVerifyOtp = async () => {
    if (!/^\d{6}$/.test(otpCode)) {
      toast.error("Enter the 6-digit code")
      return
    }
    setOtpVerifying(true)
    try {
      await authService.verifyPhoneOtp(otpCode)
      toast.success("Phone verified")
      setOtpCode("")
      setOtpHintSent(false)
      await loadMe()
      await refreshUser()
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } }
      toast.error(ax.response?.data?.message || "Invalid or expired code")
    } finally {
      setOtpVerifying(false)
    }
  }

  let contactDetailMessage = "Phone is verified."
  if (emailVerified && phoneVerified) {
    contactDetailMessage = "Both email and phone are verified."
  } else if (emailVerified) {
    contactDetailMessage = "Email is verified."
  }

  const handleResendOtp = async () => {
    setOtpResending(true)
    try {
      await authService.resendPhoneVerificationOtp()
      toast.success("New code sent to WhatsApp")
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } }
      toast.error(ax.response?.data?.message || "Could not resend OTP")
    } finally {
      setOtpResending(false)
    }
  }

  if (authLoading || !user) {
    return (
      <div className="flex min-h-screen flex-col bg-slate-50">
        <MobileHeader title="Verify account" showNotifications={false} showLanguage={false} />
        <div className="flex flex-1 items-center justify-center p-6">
          <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <MobileHeader title="Verify account" showNotifications={false} showLanguage={false} />

      <div className="flex-1 space-y-4 overflow-y-auto p-4 pb-12">
        <p className="text-sm text-slate-600 leading-relaxed">
          RoomBay only needs <span className="font-medium text-slate-800">one</span> verified contact method — email{" "}
          <span className="font-medium">or</span> WhatsApp phone. Pick whichever is easier.
        </p>
        {emailStatus === "success" && (
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
            <p className="font-medium text-emerald-900">Your email has been verified successfully.</p>
            <p className="mt-1 text-sm text-emerald-800">Return back to the app and continue.</p>
          </div>
        )}
        {emailStatus === "error" && (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-4">
            <p className="font-medium text-red-900">Email verification failed.</p>
            <p className="mt-1 text-sm text-red-800">The link may be expired or already used. Request a new verification email.</p>
          </div>
        )}

        {contactOk && (
          <div className="flex items-start gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
            <BadgeCheck className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
            <div>
              <p className="font-medium text-emerald-900">You&apos;re set for contact verification</p>
              <p className="mt-1 text-sm text-emerald-800">{contactDetailMessage}</p>
              <Button variant="link" className="mt-2 h-auto p-0 text-emerald-900" asChild>
                <Link href={user.role === "LANDLORD" ? "/landlord" : "/for-you"}>Back to app</Link>
              </Button>
            </div>
          </div>
        )}

        {loadingMe ? (
          <div className="flex justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
          </div>
        ) : (
          <>
            <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="mb-3 flex items-center gap-2">
                <Mail className="h-5 w-5 text-blue-600" />
                <h2 className="font-semibold text-slate-900">Email</h2>
                {emailVerified && <BadgeCheck className="h-5 w-5 text-emerald-500" aria-label="Verified" />}
              </div>
              {me?.email ? (
                <p className="text-sm text-slate-600 mb-3 break-all">{me.email}</p>
              ) : (
                <p className="text-sm text-slate-600 mb-3">No email on this account.</p>
              )}
              {!emailVerified && me?.email && (
                <>
                  <p className="text-xs text-slate-500 mb-3 leading-relaxed">
                    Open the link in the message we sent at signup. If it expired, resend a fresh link below.
                  </p>
                  <Button
                    className="h-11 w-full rounded-xl"
                    onClick={() => void handleResendEmail()}
                    disabled={emailSending}
                  >
                    {emailSending ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Sending…
                      </>
                    ) : (
                      "Resend verification email"
                    )}
                  </Button>
                </>
              )}
              {!me?.email && (
                <p className="text-xs text-slate-500">
                  Signed in with a provider without a mailbox? Use phone verification instead, or add an email in{" "}
                  <Link href={profileEditHref(me?.role)} className="font-medium text-blue-600 underline">
                    profile settings
                  </Link>
                  .
                </p>
              )}
            </section>

            <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="mb-3 flex items-center gap-2">
                <MessageCircle className="h-5 w-5 text-emerald-600" />
                <h2 className="font-semibold text-slate-900">Phone (WhatsApp OTP)</h2>
                {phoneVerified && <BadgeCheck className="h-5 w-5 text-emerald-500" aria-label="Verified" />}
              </div>
              {!phoneVerified && (
                <>
                  <p className="text-sm text-slate-600 mb-2 leading-relaxed">
                    We send a 6-digit code to the phone on your RoomBay profile. It must match{" "}
                    <span className="font-mono text-xs">+237</span> and nine digits after the country code.
                  </p>
                  {me?.phone ? (
                    <p className="text-sm font-medium text-slate-800 mb-3">
                      On file: <span className="font-mono">{me.phone}</span>
                    </p>
                  ) : (
                    <p className="text-sm text-amber-800 mb-3">
                      Add a phone number in{" "}
                      <Link href={profileEditHref(me?.role)} className="font-medium underline">
                        profile edit
                      </Link>{" "}
                      first.
                    </p>
                  )}
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <Button
                      type="button"
                      variant="secondary"
                      className="h-11 flex-1 rounded-xl"
                      onClick={() => void handleSendOtp()}
                      disabled={otpSending || !phoneForSend}
                    >
                      {otpSending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Send WhatsApp code"}
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      className="h-11 flex-1 rounded-xl"
                      onClick={() => void handleResendOtp()}
                      disabled={otpResending}
                    >
                      {otpResending ? <Loader2 className="h-4 w-4 animate-spin" /> : "Resend code"}
                    </Button>
                  </div>
                  {otpHintSent && (
                    <p className="mt-2 text-xs text-slate-500">Didn&apos;t get it? Try Resend after a minute.</p>
                  )}
                  <div className="mt-4 space-y-2">
                    <Label htmlFor="otp">6-digit code</Label>
                    <Input
                      id="otp"
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      maxLength={6}
                      placeholder="000000"
                      className="h-12 rounded-xl font-mono text-lg tracking-widest"
                      value={otpCode}
                      onChange={(e) => setOtpCode(e.target.value.replaceAll(/\D/g, "").slice(0, 6))}
                    />
                    <Button
                      className="h-11 w-full rounded-xl"
                      onClick={() => void handleVerifyOtp()}
                      disabled={otpVerifying || otpCode.length !== 6}
                    >
                      {otpVerifying ? <Loader2 className="h-4 w-4 animate-spin" /> : "Verify phone"}
                    </Button>
                  </div>
                </>
              )}
            </section>

            <Button variant="ghost" className="w-full justify-between rounded-xl text-slate-600" asChild>
              <Link href={profileEditHref(me?.role)}>
                Update email or phone on profile
                <ChevronRight className="h-4 w-4" />
              </Link>
            </Button>
          </>
        )}
      </div>
    </div>
  )
}
