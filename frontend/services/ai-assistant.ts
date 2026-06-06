import api from "@/lib/api"

export type AiPersona = "TENANT" | "LANDLORD" | "ADMIN"

export type AiCitation = {
  chunkId: string
  source?: string
  title?: string
}

export type AiChatRequest = {
  message: string
  persona: AiPersona
  threadId?: string
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
  city?: string
  neighborhood?: string
  propertyType?: string
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
}

export const aiAssistantService = {
  chat: async (request: AiChatRequest): Promise<AiChatResponse> => {
    const res = await api.post<AiChatResponse>("/ai/chat", request)
    return res.data
  },
  trackListingEvent: async (eventType: string, listingId: string): Promise<void> => {
    await api.post("/ai/listing-events", { eventType, listingId })
  },
}
