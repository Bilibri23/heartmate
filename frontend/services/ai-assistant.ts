import api from "@/lib/api"

export type AiPersona = "TENANT" | "LANDLORD"

export type AiCitation = {
  chunkId: string
  source?: string
  title?: string
}

export type AiChatRequest = {
  message: string
  persona: AiPersona
}

export type AiChatResponse = {
  answer: string
  citations: AiCitation[]
  /** From API: false when no RAG chunks matched (run admin ingest or fix embeddings). */
  ragGrounded?: boolean
  suggestedActions?: {
    id: string
    label: string
    type: "NAVIGATE" | "COPY_TEXT"
    actionUrl?: string
    copyText?: string
  }[]
}

export const aiAssistantService = {
  chat: async (request: AiChatRequest): Promise<AiChatResponse> => {
    const res = await api.post<AiChatResponse>("/ai/chat", request)
    return res.data
  },
}

