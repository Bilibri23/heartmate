"use client"

import { useState } from "react"
import Link from "next/link"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { submitSupportInquiry } from "@/services/support.service"
import { toast } from "sonner"
import { MessageCircle, BookOpen, Mail } from "lucide-react"

const CATEGORIES = [
  { value: "ACCOUNT", label: "Account & login" },
  { value: "LISTING", label: "Listings & search" },
  { value: "TRUST_SAFETY", label: "Trust, verification & safety" },
  { value: "PAYMENTS", label: "Payments & leases" },
  { value: "BUG", label: "Bug or error" },
  { value: "OTHER", label: "Other" },
]

export default function HelpPage() {
  const [category, setCategory] = useState("OTHER")
  const [subject, setSubject] = useState("")
  const [message, setMessage] = useState("")
  const [sending, setSending] = useState(false)

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const sub = subject.trim()
    const msg = message.trim()
    if (sub.length < 3) {
      toast.error("Please enter a short subject.")
      return
    }
    if (msg.length < 10) {
      toast.error("Please describe your concern in a bit more detail (at least 10 characters).")
      return
    }
    setSending(true)
    try {
      const res = await submitSupportInquiry({ category, subject: sub, message: msg })
      toast.success(
        res.message ||
          "Message sent. Our team can read it in Admin under Support messages and reply by email."
      )
      setSubject("")
      setMessage("")
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } }
      toast.error(ax?.response?.data?.message || "Could not send. Try again or email support.")
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Help & Support" showBack />

      <div className="p-4 space-y-6 pb-24 max-w-lg mx-auto w-full">
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <p className="text-sm text-slate-600">
            Get answers from the in-app assistant, or send a message to our team. Your account email is attached
            automatically so we can reply accurately.
          </p>
          <div className="flex flex-col gap-2">
            <Button asChild variant="outline" className="rounded-xl justify-start h-auto py-3">
              <Link href="/for-you" className="flex items-center gap-2">
                <MessageCircle className="h-5 w-5 text-blue-600 shrink-0" />
                <span className="text-left text-sm">Open RoomBay AI (chat bubble on most screens)</span>
              </Link>
            </Button>
            <Button asChild variant="outline" className="rounded-xl justify-start h-auto py-3">
              <Link href="/search" className="flex items-center gap-2">
                <BookOpen className="h-5 w-5 text-violet-600 shrink-0" />
                <span className="text-left text-sm">Browse listings & verification badges on cards</span>
              </Link>
            </Button>
          </div>
        </div>

        <form onSubmit={onSubmit} className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-4">
          <div className="flex items-center gap-2 text-slate-900 font-semibold">
            <Mail className="h-5 w-5 text-slate-500" />
            Contact us
          </div>

          <div className="space-y-1">
            <label htmlFor="support-category" className="text-xs font-medium text-slate-500">
              Topic
            </label>
            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger className="rounded-xl">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CATEGORIES.map((c) => (
                  <SelectItem key={c.value} value={c.value}>
                    {c.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1">
            <label htmlFor="support-subject" className="text-xs font-medium text-slate-500">
              Subject
            </label>
            <Input
              id="support-subject"
              className="rounded-xl"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="Short summary"
              maxLength={500}
            />
          </div>

          <div className="space-y-1">
            <label htmlFor="support-message" className="text-xs font-medium text-slate-500">
              Message
            </label>
            <textarea
              id="support-message"
              className="w-full min-h-[140px] rounded-xl border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/30"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Describe what happened, what you expected, and any listing or user involved."
              maxLength={8000}
            />
          </div>

          <Button type="submit" className="w-full rounded-xl" disabled={sending}>
            {sending ? "Sending…" : "Send message"}
          </Button>
        </form>
      </div>
    </div>
  )
}
