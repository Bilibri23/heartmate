"use client"

import { useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { MessageCircle, CalendarDays, Send, ShieldAlert } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { useProfileCompletion } from "@/hooks/use-profile-completion"
import {
  getExistingApplication,
  getListingActionContext,
  quickApply,
  scheduleViewing,
  sendMessageToLandlord,
  type ExistingApplication,
  type ListingActionContext,
} from "@/services/listing-actions"

type ActionType = "message" | "visit" | "apply"

interface FeedQuickActionsSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  listingId: string | null
  userId?: string
}

function track(eventName: string, payload?: Record<string, unknown>) {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(eventName, { detail: payload || {} }))
  }
}

export function FeedQuickActionsSheet({
  open,
  onOpenChange,
  listingId,
  userId,
}: FeedQuickActionsSheetProps) {
  const router = useRouter()
  const { status: completionStatus } = useProfileCompletion()
  const [activeAction, setActiveAction] = useState<ActionType>("message")
  const [context, setContext] = useState<ListingActionContext | null>(null)
  const [existingApplication, setExistingApplication] = useState<ExistingApplication | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [message, setMessage] = useState("")
  const [viewingDate, setViewingDate] = useState("")
  const [viewingTime, setViewingTime] = useState("")
  const [viewingNote, setViewingNote] = useState("")
  const [moveInDate, setMoveInDate] = useState("")
  const [leaseDurationMonths, setLeaseDurationMonths] = useState(6)
  const [applicationMessage, setApplicationMessage] = useState("")

  const canApply = completionStatus?.operationEligibility?.APPLY !== false
  const canMessage = completionStatus?.operationEligibility?.MESSAGE !== false
  const isActiveListing = context?.status === "ACTIVE"
  const landlordId = context?.landlordId

  const applyDisabledReason = useMemo(() => {
    if (!context) return "Loading listing..."
    if (!isActiveListing) return "This listing is no longer active."
    if (existingApplication) return "You already applied to this listing."
    if (!canApply) return "Complete your profile and verification to apply."
    return null
  }, [context, isActiveListing, existingApplication, canApply])

  useEffect(() => {
    if (!open || !listingId || !userId) return
    const load = async () => {
      setIsLoading(true)
      track("feed_action_open", { listingId })
      try {
        const [ctx, app] = await Promise.all([
          getListingActionContext(listingId, userId),
          getExistingApplication(listingId),
        ])
        setContext(ctx)
        setExistingApplication(app)
      } catch {
        toast.error("Unable to load quick actions for this listing.")
        onOpenChange(false)
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [open, listingId, userId, onOpenChange])

  useEffect(() => {
    if (!open) {
      setContext(null)
      setExistingApplication(null)
      setMessage("")
      setViewingDate("")
      setViewingTime("")
      setViewingNote("")
      setMoveInDate("")
      setLeaseDurationMonths(6)
      setApplicationMessage("")
      setActiveAction("message")
    }
  }, [open])

  const ensureAuth = () => {
    if (userId) return true
    const returnTo = listingId ? `/search?mode=forYou&view=reels&listing=${listingId}` : "/search?mode=forYou&view=reels"
    router.push(`/login?returnTo=${encodeURIComponent(returnTo)}`)
    return false
  }

  const handleMessage = async () => {
    if (!ensureAuth() || !landlordId) return
    if (!canMessage) {
      toast.error("Complete your profile to message landlords.")
      router.push("/verification")
      return
    }
    if (!message.trim()) {
      toast.error("Add a short message before sending.")
      return
    }

    setIsSubmitting(true)
    track("feed_message_landlord_click", { listingId })
    try {
      await sendMessageToLandlord(landlordId, message.trim())
      track("feed_action_success", { action: "message", listingId })
      toast.success("Message sent to landlord.")
      onOpenChange(false)
      router.push(`/messages/${landlordId}`)
    } catch {
      track("feed_action_failure", { action: "message", listingId })
      toast.error("Failed to send message. Please try again.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSchedule = async () => {
    if (!ensureAuth() || !landlordId || !context) return
    if (!canMessage) {
      toast.error("Complete your profile to schedule visits.")
      router.push("/verification")
      return
    }
    if (!viewingDate || !viewingTime) {
      toast.error("Select date and time to request a visit.")
      return
    }

    setIsSubmitting(true)
    track("feed_schedule_visit_submit", { listingId })
    try {
      await scheduleViewing({
        landlordId,
        listingTitle: context.title,
        listingCity: context.city,
        listingNeighborhood: context.neighborhood,
        viewingDate,
        viewingTime,
        note: viewingNote,
      })
      track("feed_action_success", { action: "visit", listingId })
      toast.success("Viewing request sent.")
      onOpenChange(false)
      router.push(`/messages/${landlordId}`)
    } catch {
      track("feed_action_failure", { action: "visit", listingId })
      toast.error("Failed to schedule visit. Please try again.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleQuickApply = async () => {
    if (!ensureAuth() || !listingId) return
    if (applyDisabledReason) {
      toast.error(applyDisabledReason)
      if (!canApply) router.push("/verification")
      if (existingApplication) router.push("/applications")
      return
    }
    if (!moveInDate || applicationMessage.trim().length < 50) {
      toast.error("Move-in date and a 50+ char message are required.")
      return
    }

    setIsSubmitting(true)
    track("feed_quick_apply_submit", { listingId })
    try {
      await quickApply({
        listingId,
        moveInDate,
        message: applicationMessage.trim(),
        leaseDurationMonths,
      })
      track("feed_action_success", { action: "apply", listingId })
      toast.success("Application submitted.")
      onOpenChange(false)
      router.push("/applications")
    } catch (error: unknown) {
      track("feed_action_failure", { action: "apply", listingId })
      const axiosError = error as { response?: { status?: number; data?: { message?: string } } }
      if (axiosError?.response?.status === 403) {
        router.push("/verification")
      } else {
        toast.error(axiosError?.response?.data?.message || "Failed to submit application.")
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="bottom" className="h-[78vh] rounded-t-3xl overflow-y-auto">
        <SheetHeader>
          <SheetTitle>Quick actions</SheetTitle>
        </SheetHeader>

        <div className="mt-4 space-y-4 pb-6">
          <div className="grid grid-cols-3 gap-2">
            <Button variant={activeAction === "message" ? "default" : "outline"} className="rounded-xl" onClick={() => setActiveAction("message")}>
              <MessageCircle className="h-4 w-4 mr-1" /> Message
            </Button>
            <Button variant={activeAction === "visit" ? "default" : "outline"} className="rounded-xl" onClick={() => setActiveAction("visit")}>
              <CalendarDays className="h-4 w-4 mr-1" /> Visit
            </Button>
            <Button variant={activeAction === "apply" ? "default" : "outline"} className="rounded-xl" onClick={() => setActiveAction("apply")}>
              <Send className="h-4 w-4 mr-1" /> Apply
            </Button>
          </div>

          {isLoading && (
            <div className="text-sm text-slate-500">Loading listing actions...</div>
          )}

          {!isLoading && context && (
            <div className="rounded-xl border border-slate-200 p-3 bg-slate-50">
              <p className="font-medium text-slate-900">{context.title}</p>
              <p className="text-sm text-slate-600">{context.neighborhood}, {context.city}</p>
            </div>
          )}

          {!userId && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
              Log in to use quick actions from feed.
            </div>
          )}

          {activeAction === "message" && (
            <div className="space-y-3">
              <Textarea
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="Hi, I am interested in this listing. Is it still available?"
                className="min-h-[110px] rounded-xl"
              />
              <Button className="w-full h-11 rounded-xl" onClick={handleMessage} disabled={isSubmitting || isLoading || !landlordId || !isActiveListing}>
                Send message
              </Button>
            </div>
          )}

          {activeAction === "visit" && (
            <div className="space-y-3">
              <Input
                type="date"
                value={viewingDate}
                onChange={(e) => setViewingDate(e.target.value)}
                min={new Date().toISOString().split("T")[0]}
                className="rounded-xl"
              />
              <Input
                type="time"
                value={viewingTime}
                onChange={(e) => setViewingTime(e.target.value)}
                className="rounded-xl"
              />
              <Textarea
                value={viewingNote}
                onChange={(e) => setViewingNote(e.target.value)}
                placeholder="Optional note to landlord"
                className="min-h-[80px] rounded-xl"
              />
              <Button className="w-full h-11 rounded-xl" onClick={handleSchedule} disabled={isSubmitting || isLoading || !landlordId || !isActiveListing}>
                Request viewing
              </Button>
            </div>
          )}

          {activeAction === "apply" && (
            <div className="space-y-3">
              {applyDisabledReason && (
                <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700 flex items-start gap-2">
                  <ShieldAlert className="h-4 w-4 mt-0.5 shrink-0" />
                  <span>{applyDisabledReason}</span>
                </div>
              )}
              {!canApply && (
                <Link href="/verification" className="text-sm text-blue-600 underline">
                  Complete verification to apply
                </Link>
              )}
              <Input
                type="date"
                value={moveInDate}
                onChange={(e) => setMoveInDate(e.target.value)}
                min={new Date().toISOString().split("T")[0]}
                className="rounded-xl"
              />
              <select
                value={leaseDurationMonths}
                onChange={(e) => setLeaseDurationMonths(Number(e.target.value))}
                className="w-full h-10 px-3 rounded-xl border border-slate-200 bg-white text-sm"
              >
                {[1, 2, 3, 6, 9, 12].map((months) => (
                  <option key={months} value={months}>
                    {months} month{months > 1 ? "s" : ""}
                  </option>
                ))}
              </select>
              <Textarea
                value={applicationMessage}
                onChange={(e) => setApplicationMessage(e.target.value)}
                placeholder="Tell the landlord about yourself (minimum 50 characters)"
                className="min-h-[120px] rounded-xl"
              />
              <p className="text-xs text-slate-500">{applicationMessage.length}/50 minimum</p>
              <Button className="w-full h-11 rounded-xl" onClick={handleQuickApply} disabled={isSubmitting || isLoading || !!applyDisabledReason}>
                Submit quick apply
              </Button>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}
