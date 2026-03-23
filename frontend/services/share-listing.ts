import api from "@/lib/api"

export interface ShareListingRequest {
  listingId: string
  roommateId: string
}

export const shareListingService = {
  shareListing: async (request: ShareListingRequest): Promise<void> => {
    await api.post("/share-listing", request)
  },
}
