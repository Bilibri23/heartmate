import api from "@/lib/api"

export type AiPersona = "TENANT" | "LANDLORD" | "ADMIN"

// Same-origin proxy keeps SSE auth and CORS consistent in dev and on Vercel.
const STREAM_API_URL = "/api"

export type AiCitation = {
  chunkId: string
  source?: string
  title?: string
}

export type AiChatRequest = {
  message: string
  persona: AiPersona
  threadId?: string
  /** Listing the user is currently viewing, so "apply to this" resolves to a concrete listing. */
  contextListingId?: string
}

export type AiToolExecution = {
  tool: string
  success: boolean
  message: string
  entityId?: string
}

export type AiSuggestedAction = {
  id: string
  label: string
  type: "NAVIGATE" | "COPY_TEXT"
  actionUrl?: string
  copyText?: string
}

export type AiListingResult = {
  id: string
  title?: string
  rentAmount?: number
  salePrice?: number
  city?: string
  neighborhood?: string
  propertyType?: string
  listingPurpose?: string
  stayType?: string
  pricePeriod?: string
  furnishingStatus?: string
  isSharedAccommodation?: boolean
  roommatesWanted?: boolean
  availableRoommateSlots?: number
  roomPreviewEnabled?: boolean
  roomPreviewStatus?: string
  verified?: boolean
  status?: string
  available?: boolean
  thumbnailUrl?: string
  landlordId?: string
  matchLabel?: string
  matchReason?: string
  whyThisMatches?: string[]
  actions?: AiSuggestedAction[]
}

export type AiChatResponse = {
  answer: string
  threadId?: string
  citations: AiCitation[]
  /** From API: false when no RAG chunks matched (run admin ingest or fix embeddings). */
  ragGrounded?: boolean
  suggestedActions?: AiSuggestedAction[]
  listingResults?: AiListingResult[]
  /** Results of any write actions the assistant executed (save / apply / request visit). */
  toolExecutions?: AiToolExecution[]
}

export type AiStreamEvent =
  | { type: "retrieval_started"; label?: string }
  | { type: "sources_found"; label?: string }
  | { type: "generation_started"; label?: string }
  | { type: "token"; token?: string }
  | { type: "completed"; response: AiChatResponse }
  | { type: "error"; message?: string }

type ChatStreamOptions = {
  onEvent: (event: AiStreamEvent) => void
  signal?: AbortSignal
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null

export const aiAssistantService = {
  chat: async (request: AiChatRequest): Promise<AiChatResponse> => {
    const res = await api.post<AiChatResponse>("/ai/chat", request)
    return res.data
  },
  chatStream: async (request: AiChatRequest, options: ChatStreamOptions): Promise<AiChatResponse> => {
    const token = typeof window !== "undefined" ? localStorage.getItem("token") : null
    const response = await fetch(`${STREAM_API_URL}/ai/chat/stream`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/event-stream",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(request),
      signal: options.signal,
    })

    if (!response.ok || !response.body) {
      throw new Error(`AI stream failed with status ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ""
    let completed: AiChatResponse | null = null

    const consumeBlock = (block: string) => {
      const lines = block.split(/\r?\n/)
      let eventName = "message"
      const dataLines: string[] = []

      for (const line of lines) {
        if (line.startsWith("event:")) {
          eventName = line.slice("event:".length).trim()
        } else if (line.startsWith("data:")) {
          dataLines.push(line.slice("data:".length).trimStart())
        }
      }

      if (dataLines.length === 0) return
      const rawData = dataLines.join("\n")
      let data: unknown = rawData
      try {
        data = JSON.parse(rawData)
      } catch {
        // Token events may arrive as plain text if the backend later adds true streaming.
      }

      if (eventName === "completed") {
        const responseData = data as AiChatResponse
        completed = responseData
        options.onEvent({ type: "completed", response: responseData })
        return
      }
      if (eventName === "token") {
        const token = isRecord(data) && typeof data.token === "string" ? data.token : undefined
        options.onEvent({ type: "token", token: typeof data === "string" ? data : token })
        return
      }
      if (eventName === "error") {
        const message = isRecord(data) && typeof data.message === "string" ? data.message : undefined
        options.onEvent({ type: "error", message: typeof data === "string" ? data : message })
        return
      }
      if (eventName === "retrieval_started" || eventName === "sources_found" || eventName === "generation_started") {
        const label = isRecord(data) && typeof data.label === "string" ? data.label : undefined
        options.onEvent({ type: eventName, label: typeof data === "string" ? data : label })
      }
    }

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() || ""
      for (const block of blocks) {
        consumeBlock(block)
      }
    }

    if (buffer.trim().length > 0) {
      consumeBlock(buffer)
    }
    if (!completed) {
      throw new Error("AI stream ended before completion")
    }
    return completed
  },
  trackListingEvent: async (eventType: string, listingId: string): Promise<void> => {
    await api.post("/ai/listing-events", { eventType, listingId })
  },
}
