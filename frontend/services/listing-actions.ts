import api from "@/lib/api"

export interface ListingActionContext {
  listingId: string
  title: string
  city: string
  neighborhood: string
  status: string
  landlordId: string | null
}

export interface ExistingApplication {
  id: string
  status: string
}

export interface QuickApplyRequest {
  listingId: string
  moveInDate: string
  message: string
  leaseDurationMonths: number
}

export interface ScheduleVisitRequest {
  landlordId: string
  listingTitle: string
  listingCity: string
  listingNeighborhood: string
  viewingDate: string
  viewingTime: string
  note?: string
}

export async function getListingActionContext(
  listingId: string,
  userId?: string,
): Promise<ListingActionContext> {
  const response = await api.get(`/listings/${listingId}`, {
    params: userId ? { userId } : undefined,
  })
  const listing = response.data
  return {
    listingId: listing.id,
    title: listing.title,
    city: listing.city,
    neighborhood: listing.neighborhood,
    status: listing.status,
    landlordId: listing.landlord?.id || listing.landlordId || null,
  }
}

export async function getExistingApplication(listingId: string): Promise<ExistingApplication | null> {
  const response = await api.get(`/applications/my/listing/${listingId}`, {
    validateStatus: (status) => status < 500,
  })
  if (response.status === 200) {
    return response.data
  }
  return null
}

export async function sendMessageToLandlord(landlordId: string, content: string): Promise<void> {
  await api.post("/messages", {
    receiverId: landlordId,
    content,
  })
}

export async function scheduleViewing(request: ScheduleVisitRequest): Promise<void> {
  const formattedDate = new Date(request.viewingDate).toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  })

  const content = `📅 **Viewing Request**\n\nI would like to schedule a viewing for:\n\n🏠 Property: ${request.listingTitle}\n📍 Location: ${request.listingNeighborhood}, ${request.listingCity}\n📆 Date: ${formattedDate}\n⏰ Time: ${request.viewingTime}\n\n${request.note ? `Message: ${request.note}` : ""}`
  await sendMessageToLandlord(request.landlordId, content)
}

export async function quickApply(request: QuickApplyRequest): Promise<void> {
  await api.post("/applications", request)
}
