// Shared types for the Visit Scheduling feature (Phase 9).

export type VisitStatus =
  | "REQUESTED"
  | "ACCEPTED"
  | "RESCHEDULED"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW"

export interface Visit {
  id: string
  listingId: string
  listingTitle: string
  listingCity?: string | null
  listingNeighborhood?: string | null
  listingPrimaryPhotoUrl?: string | null
  tenantId: string
  tenantName?: string
  tenantPhone?: string
  landlordId: string
  landlordName?: string
  applicationId?: string | null
  requestedDatetime: string
  visitDatetime?: string | null
  status: VisitStatus
  tenantMessage?: string | null
  landlordResponse?: string | null
  rescheduleReason?: string | null
  cancellationReason?: string | null
  createdAt: string
  updatedAt: string
  isActive?: boolean
}

export interface TimelineStep {
  key: string
  label: string
  status: "DONE" | "CURRENT" | "PENDING"
  at?: string | null
}

export interface ApplicationTimeline {
  applicationId: string
  steps: TimelineStep[]
}
