import api from "@/lib/api"

export type SupportInquiryPayload = {
  category?: string
  subject: string
  message: string
}

export async function submitSupportInquiry(payload: SupportInquiryPayload) {
  const res = await api.post<{ success: boolean; message?: string }>("/support/inquiries", payload)
  return res.data
}
