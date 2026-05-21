"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import { Send, Loader2, ExternalLink, Copy, Volume2, Square } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import { aiAssistantService, type AiCitation, type AiPersona } from "@/services/ai-assistant"
import { useRouter } from "next/navigation"
import { toast } from "sonner"

type ChatRole = "user" | "assistant"

type ChatMessage = {
  id: string
  role: ChatRole
  content: string
  ragGrounded?: boolean
  citations?: AiCitation[]
  suggestedActions?: {
    id: string
    label: string
    type: "NAVIGATE" | "COPY_TEXT"
    actionUrl?: string
    copyText?: string
  }[]
  createdAt: number
}

function uid() {
  return Math.random().toString(16).slice(2) + Date.now().toString(16)
}

export function AssistantChat({ persona }: { persona: AiPersona }) {
  const router = useRouter()
  const threadStorageKey = `rb.ai.thread.${persona}`
  const messagesStorageKey = `rb.ai.messages.${persona}`
  const initialAssistantMessage = {
    id: uid(),
    role: "assistant" as const,
    content:
      persona === "ADMIN"
        ? "Hi! I’m your RoomBay admin assistant. Ask me about verification queues, listing moderation, disputes, reports, support inquiries, and platform policy."
        : persona === "LANDLORD"
        ? "Hi! I’m your RoomBay assistant. Ask me about listing approval, applications, leases, payments, or how to use the landlord dashboard."
        : "Hi! I’m your RoomBay assistant. Ask me about searching, applying, verification, roommate matching, leases, or payments.",
    createdAt: Date.now(),
  }
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    initialAssistantMessage,
  ])
  const [threadId, setThreadId] = useState<string | null>(null)
  const [input, setInput] = useState("")
  const [isSending, setIsSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [speakingId, setSpeakingId] = useState<string | null>(null)
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
  }, [messages.length])

  const send = async () => {
    const text = input.trim()
    if (!text || isSending) return
    setInput("")
    setError(null)

    const userMsg: ChatMessage = { id: uid(), role: "user", content: text, createdAt: Date.now() }
    setMessages((prev) => [...prev, userMsg])
    setIsSending(true)

    try {
      const res = await aiAssistantService.chat({ message: text, persona, threadId: threadId ?? undefined })
      if (res.threadId) {
        setThreadId(res.threadId)
      }
      const asstMsg: ChatMessage = {
        id: uid(),
        role: "assistant",
        content: res.answer,
        ragGrounded: res.ragGrounded,
        citations: res.citations,
        suggestedActions: res.suggestedActions,
        createdAt: Date.now(),
      }
      setMessages((prev) => [...prev, asstMsg])
    } catch (e: any) {
      if (e?.response?.status === 429) {
        setError(e?.response?.data?.message || "Too many assistant requests. Please wait a minute and try again.")
      } else {
        setError(e?.response?.data?.message || "Assistant is unavailable. Please try again.")
      }
    } finally {
      setIsSending(false)
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
            <div className="whitespace-pre-wrap">{m.content}</div>
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
            {m.role === "assistant" && m.ragGrounded === false && (
              <p className="mt-2 text-xs text-amber-800 bg-amber-50 border border-amber-200/80 rounded-lg px-2 py-1.5">
                No matching help docs were found for this question, so this reply is not doc-grounded. Try rephrasing or run admin doc ingest if the knowledge base is empty.
              </p>
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
                  {m.suggestedActions.slice(0, 3).map((a) => (
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
                  ))}
                </div>
              </div>
            )}
          </div>
        ))}
        {isSending && (
          <div className="mr-auto bg-white border border-slate-200 text-slate-700 max-w-[92%] rounded-2xl px-4 py-3 text-sm shadow-sm inline-flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin" />
            Thinking…
          </div>
        )}
        {error && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl p-3">
            {error}
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

