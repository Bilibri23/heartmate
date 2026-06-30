"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import { Send, Loader2, ExternalLink, Copy, Volume2, Square, MapPin, ShieldCheck, Home, Bookmark, FileText, MessageCircle, CheckCircle2, AlertTriangle } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import { aiAssistantService, type AiCitation, type AiListingResult, type AiPersona, type AiStreamEvent, type AiSuggestedAction, type AiToolExecution } from "@/services/ai-assistant"
import { ListingAttributeBadges } from "@/components/listings/listing-attribute-badges"
import { displayListingPrice, isSaleListing } from "@/lib/listing-taxonomy"
import { useRouter, usePathname } from "next/navigation"
import { toast } from "sonner"
import { useAuth } from "@/context/auth-context"
import api from "@/lib/api"

type ChatRole = "user" | "assistant"

type ChatMessage = {
  id: string
  role: ChatRole
  content: string
  statusLabel?: string
  isPending?: boolean
  ragGrounded?: boolean
  citations?: AiCitation[]
  suggestedActions?: AiSuggestedAction[]
  listingResults?: AiListingResult[]
  toolExecutions?: AiToolExecution[]
  createdAt: number
}

const UUID_RE = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i

type ApiError = {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
}

function uid() {
  return Math.random().toString(16).slice(2) + Date.now().toString(16)
}

const PROGRESS_LABELS = [
  "Searching RoomBay knowledge base...",
  "Checking role-safe sources...",
  "Reading relevant documents...",
  "Generating grounded response...",
  "Preparing sources and actions...",
]

export function AssistantChat({ persona }: { persona: AiPersona }) {
  const router = useRouter()
  const pathname = usePathname()
  const { user } = useAuth()
  // On a listing detail page (/listings/<id>), let "apply to this"/"save this" resolve the listing.
  const contextListingId = useMemo(() => {
    if (!pathname || !/\/listings?\//.test(pathname)) return undefined
    return pathname.match(UUID_RE)?.[0]
  }, [pathname])
  const threadStorageKey = `rb.ai.thread.${persona}`
  const messagesStorageKey = `rb.ai.messages.${persona}`
  const initialAssistantMessage = {
    id: uid(),
    role: "assistant" as const,
    content:
      persona === "ADMIN"
        ? "Hi! I am RoomBay AI for admins. Ask me about verification queues, listing moderation, disputes, reports, support inquiries, and platform policy."
        : persona === "LANDLORD"
        ? "Hi! I am RoomBay AI for landlords. Ask me about listing approval, applications, leases, payments, or how to use the landlord dashboard."
        : "Hi! I am RoomBay AI. Ask me about searching, applying, verification, roommate matching, leases, or payments.",
    createdAt: Date.now(),
  }
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    initialAssistantMessage,
  ])
  const [threadId, setThreadId] = useState<string | null>(null)
  const [input, setInput] = useState("")
  const [isSending, setIsSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastFailedMessage, setLastFailedMessage] = useState<string | null>(null)
  const [speakingId, setSpeakingId] = useState<string | null>(null)
  const [pendingActionId, setPendingActionId] = useState<string | null>(null)
  const bottomRef = useRef<HTMLDivElement | null>(null)

  const stopSpeaking = () => {
    if (typeof window !== "undefined" && window.speechSynthesis) {
      window.speechSynthesis.cancel()
    }
    setSpeakingId(null)
  }

  const speakMessage = (id: string, text: string) => {
    if (typeof window === "undefined" || !window.speechSynthesis) {
      toast.error("Read aloud is not supported in this browser.")
      return
    }
    const plain = text.replace(/\s+/g, " ").trim()
    if (!plain) return
    window.speechSynthesis.cancel()
    const u = new SpeechSynthesisUtterance(plain)
    u.rate = 1
    u.onend = () => setSpeakingId(null)
    u.onerror = () => setSpeakingId(null)
    setSpeakingId(id)
    window.speechSynthesis.speak(u)
  }

  useEffect(() => () => stopSpeaking(), [])

  useEffect(() => {
    if (typeof window === "undefined") return
    const saved = window.sessionStorage.getItem(threadStorageKey)
    setThreadId(saved && saved.trim().length > 0 ? saved : null)
  }, [threadStorageKey])

  useEffect(() => {
    if (typeof window === "undefined") return
    const saved = window.sessionStorage.getItem(messagesStorageKey)
    if (!saved) return
    try {
      const parsed = JSON.parse(saved) as ChatMessage[]
      if (Array.isArray(parsed) && parsed.length > 0) {
        setMessages(parsed)
      }
    } catch {
      // Ignore corrupt storage and keep default greeting.
    }
  }, [messagesStorageKey])

  useEffect(() => {
    if (typeof window === "undefined") return
    if (threadId && threadId.trim().length > 0) {
      window.sessionStorage.setItem(threadStorageKey, threadId)
    }
  }, [threadId, threadStorageKey])

  useEffect(() => {
    if (typeof window === "undefined") return
    window.sessionStorage.setItem(messagesStorageKey, JSON.stringify(messages))
  }, [messages, messagesStorageKey])

  const canSend = useMemo(() => input.trim().length > 0 && !isSending, [input, isSending])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const send = async (overrideText?: string) => {
    const text = (overrideText ?? input).trim()
    if (!text || isSending) return
    if (!overrideText) setInput("")
    setError(null)
    setLastFailedMessage(null)

    const userMsg: ChatMessage = { id: uid(), role: "user", content: text, createdAt: Date.now() }
    const assistantId = uid()
    const pendingAssistantMsg: ChatMessage = {
      id: assistantId,
      role: "assistant",
      content: "",
      statusLabel: PROGRESS_LABELS[0],
      isPending: true,
      createdAt: Date.now(),
    }
    setMessages((prev) => [...prev, userMsg, pendingAssistantMsg])
    setIsSending(true)

    const request = { message: text, persona, threadId: threadId ?? undefined, contextListingId }
    const updateAssistant = (patch: Partial<ChatMessage>) => {
      setMessages((prev) => prev.map((m) => (m.id === assistantId ? { ...m, ...patch } : m)))
    }
    const applyResponse = (res: Awaited<ReturnType<typeof aiAssistantService.chat>>) => {
      if (res.threadId) {
        setThreadId(res.threadId)
      }
      updateAssistant({
        content: res.answer,
        statusLabel: undefined,
        isPending: false,
        ragGrounded: res.ragGrounded,
        citations: res.citations,
        suggestedActions: res.suggestedActions,
        listingResults: res.listingResults,
        toolExecutions: res.toolExecutions,
      })
    }
    const handleStreamEvent = (event: AiStreamEvent) => {
      if (event.type === "retrieval_started" || event.type === "sources_found" || event.type === "generation_started") {
        updateAssistant({ statusLabel: event.label || PROGRESS_LABELS[0] })
        return
      }
      if (event.type === "token" && event.token) {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === assistantId
              ? { ...m, content: `${m.content}${event.token}`, statusLabel: undefined }
              : m
          )
        )
        return
      }
      if (event.type === "completed") {
        applyResponse(event.response)
      }
      if (event.type === "error") {
        updateAssistant({ statusLabel: event.message || "Preparing the standard response..." })
      }
    }

    try {
      await aiAssistantService.chatStream(request, { onEvent: handleStreamEvent })
    } catch {
      try {
        updateAssistant({ statusLabel: PROGRESS_LABELS[4] })
        const fallback = await aiAssistantService.chat(request)
        applyResponse(fallback)
      } catch (fallbackError: unknown) {
        const apiError = fallbackError as ApiError
        setLastFailedMessage(text)
        setMessages((prev) => prev.filter((m) => m.id !== assistantId))
        if (apiError.response?.status === 429) {
          setError(apiError.response?.data?.message || "Too many assistant requests. Please wait a minute and try again.")
        } else {
          setError(apiError.response?.data?.message || "Assistant is unavailable. Please try again.")
        }
      }
    } finally {
      setIsSending(false)
    }
  }

  // Landlord/admin confirm-button: execute the proposed privileged action, then replace the
  // confirm button on that message with the result chip.
  const confirmAction = async (messageId: string, action: AiSuggestedAction) => {
    if (!action.tool || !action.actionParams || pendingActionId) return
    setPendingActionId(action.id)
    try {
      const result = await aiAssistantService.executeAction(action.tool, action.actionParams)
      setMessages((prev) =>
        prev.map((m) =>
          m.id === messageId
            ? {
                ...m,
                suggestedActions: (m.suggestedActions || []).filter((a) => a.id !== action.id),
                toolExecutions: [...(m.toolExecutions || []), result],
              }
            : m
        )
      )
      if (result.success) toast.success(result.message)
      else toast.error(result.message)
    } catch {
      toast.error("Action could not be completed. Please try again.")
    } finally {
      setPendingActionId(null)
    }
  }

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1 min-h-0 overflow-y-auto p-4 space-y-3 bg-slate-50">
        {messages.map((m) => (
          <div
            key={m.id}
            className={cn(
              "max-w-[92%] rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-sm",
              m.role === "user"
                ? "ml-auto bg-blue-600 text-white"
                : "mr-auto bg-white text-slate-800 border border-slate-200"
            )}
          >
            {m.content.trim().length > 0 && <div className="whitespace-pre-wrap">{m.content}</div>}
            {m.role === "assistant" && m.isPending && m.statusLabel && m.content.trim().length === 0 && (
              <div className="flex items-center gap-2 text-slate-600">
                <Loader2 className="h-4 w-4 animate-spin text-blue-600" />
                <span>{m.statusLabel}</span>
              </div>
            )}
            {m.role === "assistant" && m.isPending && m.content.trim().length > 0 && (
              <span className="inline-block w-2 h-4 ml-0.5 bg-blue-600/70 animate-pulse align-text-bottom" aria-hidden />
            )}
            {m.role === "assistant" && m.content.trim().length > 0 && (
              <div className="mt-2 flex justify-end">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-8 rounded-lg text-slate-500 hover:text-blue-700"
                  aria-label={speakingId === m.id ? "Stop reading" : "Read answer aloud"}
                  onClick={() =>
                    speakingId === m.id ? stopSpeaking() : speakMessage(m.id, m.content)
                  }
                >
                  {speakingId === m.id ? (
                    <>
                      <Square className="h-3.5 w-3.5 mr-1 fill-current" />
                      Stop
                    </>
                  ) : (
                    <>
                      <Volume2 className="h-3.5 w-3.5 mr-1" />
                      Read aloud
                    </>
                  )}
                </Button>
              </div>
            )}
            {m.role === "assistant" && m.toolExecutions && m.toolExecutions.length > 0 && (
              <div className="mt-3 space-y-2">
                {m.toolExecutions.map((t, i) => (
                  <div
                    key={`${t.tool}-${i}`}
                    className={cn(
                      "flex items-start gap-2 rounded-lg border px-3 py-2 text-xs",
                      t.success
                        ? "bg-emerald-50 border-emerald-200/80 text-emerald-800"
                        : "bg-amber-50 border-amber-200/80 text-amber-800"
                    )}
                  >
                    {t.success ? (
                      <CheckCircle2 className="h-4 w-4 mt-0.5 shrink-0" />
                    ) : (
                      <AlertTriangle className="h-4 w-4 mt-0.5 shrink-0" />
                    )}
                    <span>{t.message}</span>
                  </div>
                ))}
              </div>
            )}
            {m.role === "assistant" && m.toolExecutions === undefined && m.ragGrounded === false && (
              <p className="mt-2 text-xs text-amber-800 bg-amber-50 border border-amber-200/80 rounded-lg px-2 py-1.5">
                No matching help docs were found for this question, so this reply is not doc-grounded. Try rephrasing or run admin doc ingest if the knowledge base is empty.
              </p>
            )}
            {m.role === "assistant" && m.listingResults && m.listingResults.length > 0 && (
              <div className="mt-3 space-y-2">
                {m.listingResults.slice(0, 4).map((listing) => (
                  <AiListingCard
                    key={listing.id}
                    listing={listing}
                    isAuthenticated={Boolean(user)}
                    userId={user?.id}
                    onNavigate={(url) => router.push(url)}
                  />
                ))}
              </div>
            )}
            {m.role === "assistant" && m.citations && m.citations.length > 0 && (
              <div className="mt-3 pt-3 border-t border-slate-200/70 space-y-1">
                <p className="text-xs font-medium text-slate-500">Sources</p>
                <div className="flex flex-wrap gap-2">
                  {m.citations.slice(0, 6).map((c) => (
                    <span
                      key={c.chunkId}
                      className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-1 text-[11px] text-slate-600"
                      title={c.title || c.source}
                    >
                      <ExternalLink className="h-3 w-3" />
                      {c.title || c.source || "doc"}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {m.role === "assistant" && m.suggestedActions && m.suggestedActions.length > 0 && (
              <div className="mt-3 pt-3 border-t border-slate-200/70 space-y-2">
                <p className="text-xs font-medium text-slate-500">Suggested next steps</p>
                <div className="flex flex-wrap gap-2">
                  {m.suggestedActions.slice(0, 3).map((a) =>
                    a.type === "CONFIRM_ACTION" ? (
                      <Button
                        key={a.id}
                        size="sm"
                        className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white"
                        disabled={pendingActionId === a.id}
                        onClick={() => confirmAction(m.id, a)}
                      >
                        {pendingActionId === a.id ? (
                          <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
                        ) : (
                          <CheckCircle2 className="h-3.5 w-3.5 mr-1" />
                        )}
                        {a.label}
                      </Button>
                    ) : (
                      <Button
                        key={a.id}
                        size="sm"
                        variant="outline"
                        className="rounded-xl"
                        onClick={() => {
                          if (a.type === "NAVIGATE" && a.actionUrl) {
                            router.push(a.actionUrl)
                            return
                          }
                          if (a.type === "COPY_TEXT" && a.copyText) {
                            navigator.clipboard.writeText(a.copyText).then(
                              () => toast.success("Copied"),
                              () => toast.error("Copy failed")
                            )
                          }
                        }}
                      >
                        {a.type === "COPY_TEXT" ? <Copy className="h-3.5 w-3.5 mr-1" /> : null}
                        {a.label}
                      </Button>
                    )
                  )}
                </div>
              </div>
            )}
          </div>
        ))}
        {error && (
          <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-xl p-3 space-y-2">
            <p>{error}</p>
            {lastFailedMessage && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                className="rounded-xl border-red-200 text-red-700 hover:bg-red-100"
                disabled={isSending}
                onClick={() => send(lastFailedMessage)}
              >
                Retry
              </Button>
            )}
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="p-4 bg-white border-t border-slate-200">
        <form
          className="flex items-center gap-2"
          onSubmit={(e) => {
            e.preventDefault()
            send()
          }}
        >
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask a question…"
            className="h-12 rounded-xl"
          />
          <Button type="submit" className="h-12 rounded-xl" disabled={!canSend}>
            <Send className="h-4 w-4 mr-2" />
            Send
          </Button>
        </form>
      </div>
    </div>
  )
}

function AiListingCard({
  listing,
  isAuthenticated,
  userId,
  onNavigate,
}: {
  listing: AiListingResult
  isAuthenticated: boolean
  userId?: string
  onNavigate: (url: string) => void
}) {
  const listingUrl = `/listings/${listing.id}`
  const location = [listing.neighborhood, listing.city].filter(Boolean).join(", ")
  const statusLabel = listing.available ? "Available" : friendlyStatus(listing.status)
  const priceLabel = displayListingPrice(listing, (amount) =>
    new Intl.NumberFormat("fr-CM", { maximumFractionDigits: 0 }).format(amount) + " XAF"
  )
  const saleListing = isSaleListing(listing)

  const track = (eventType: string) => {
    aiAssistantService.trackListingEvent(eventType, listing.id).catch(() => {})
  }

  const requireLogin = (intent: string) => {
    toast.info(`Please log in to ${intent}.`)
    onNavigate(`/login?redirect=${encodeURIComponent(listingUrl)}`)
  }

  const handleView = () => {
    track("ai_listing_result_clicked")
    onNavigate(listingUrl)
  }

  const handleSave = async () => {
    if (!isAuthenticated) {
      requireLogin("save this listing")
      return
    }
    try {
      await api.post(`/listings/${listing.id}/favorite`, null, {
        params: userId ? { userId } : undefined,
      })
      track("ai_listing_save_clicked")
      toast.success("Listing saved")
    } catch {
      toast.error("Could not save this listing right now.")
    }
  }

  const handleApply = () => {
    if (!isAuthenticated) {
      requireLogin("apply")
      return
    }
    track("ai_listing_apply_clicked")
    onNavigate(`${listingUrl}?intent=apply`)
  }

  const handleMessage = () => {
    if (!listing.landlordId) return
    if (!isAuthenticated) {
      requireLogin("message the landlord")
      return
    }
    onNavigate(`/messages/${listing.landlordId}`)
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex gap-3 p-3">
        <div className="h-24 w-24 shrink-0 overflow-hidden rounded-xl bg-slate-100">
          {listing.thumbnailUrl ? (
            <img
              src={listing.thumbnailUrl}
              alt={listing.title || "Listing photo"}
              className="h-full w-full object-cover"
              loading="lazy"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-slate-400">
              <Home className="h-8 w-8" />
            </div>
          )}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-slate-950">{listing.title || "RoomBay listing"}</p>
              <p className="mt-0.5 text-base font-bold text-slate-950">{priceLabel}</p>
            </div>
            {listing.verified && (
              <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-emerald-50 px-2 py-1 text-[11px] font-semibold text-emerald-700">
                <ShieldCheck className="h-3 w-3" />
                Verified
              </span>
            )}
          </div>
          <div className="mt-2 flex flex-wrap gap-1.5 text-[11px] text-slate-600">
            {location && (
              <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-1">
                <MapPin className="h-3 w-3" />
                {location}
              </span>
            )}
            {statusLabel && <span className="rounded-full bg-blue-50 px-2 py-1 font-medium text-blue-700">{statusLabel}</span>}
          </div>
          <ListingAttributeBadges listing={listing} className="mt-2" maxBadges={5} />
        </div>
      </div>

      <div className="border-t border-slate-100 px-3 py-3">
        <p className="text-xs font-semibold text-slate-700">{listing.matchLabel || "Search match"}</p>
        <p className="mt-1 text-xs text-slate-600">{listing.matchReason || "This matches your current search."}</p>
        {listing.whyThisMatches && listing.whyThisMatches.length > 0 && (
          <div className="mt-2 rounded-xl bg-slate-50 p-2">
            <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Why this matches</p>
            <ul className="mt-1 space-y-1 text-xs text-slate-600">
              {listing.whyThisMatches.slice(0, 4).map((reason) => (
                <li key={reason} className="flex gap-1.5">
                  <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-500" />
                  <span>{reason}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="grid grid-cols-2 gap-2 border-t border-slate-100 p-3 sm:flex sm:flex-wrap">
        <Button type="button" size="sm" className="rounded-xl" onClick={handleView}>
          <ExternalLink className="mr-1 h-3.5 w-3.5" />
          View listing
        </Button>
        <Button type="button" size="sm" variant="outline" className="rounded-xl" onClick={handleSave}>
          <Bookmark className="mr-1 h-3.5 w-3.5" />
          Save
        </Button>
        {!saleListing && (
          <Button type="button" size="sm" variant="outline" className="rounded-xl" onClick={handleApply}>
            <FileText className="mr-1 h-3.5 w-3.5" />
            Apply now
          </Button>
        )}
        {listing.landlordId && (
          <Button type="button" size="sm" variant="outline" className="rounded-xl" onClick={handleMessage}>
            <MessageCircle className="mr-1 h-3.5 w-3.5" />
            Message
          </Button>
        )}
      </div>
    </div>
  )
}

function formatRent(amount?: number) {
  if (typeof amount !== "number") return "Rent not shown"
  return `${new Intl.NumberFormat("fr-CM", { maximumFractionDigits: 0 }).format(amount)} XAF`
}

function friendlyPropertyType(value?: string) {
  if (!value) return ""
  return value.toLowerCase().replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase())
}

function friendlyStatus(value?: string) {
  if (!value) return ""
  if (value === "ACTIVE") return "Available"
  return friendlyPropertyType(value)
}
