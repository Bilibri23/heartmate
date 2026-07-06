export type RealtorVerificationStatus = "PENDING" | "VERIFIED" | "REJECTED" | "SUSPENDED";

export type RealtorDocumentStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface RealtorDocument {
  id: string;
  documentType: string;
  documentUrl: string;
  status: RealtorDocumentStatus | string;
  rejectionReason?: string | null;
  createdAt?: string;
}

export interface RealtorProfile {
  id: string;
  userId: string;
  fullName?: string;
  email?: string;
  agencyName: string;
  businessRegistrationNumber?: string;
  city: string;
  areasCovered: string[];
  phoneNumber?: string;
  whatsappNumber?: string;
  bio?: string;
  verificationStatus: RealtorVerificationStatus | string;
  rejectionReason?: string | null;
  trustScore: number;
  totalListings: number;
  successfulRentals: number;
  complaintCount: number;
  createdAt?: string;
  updatedAt?: string;
  documents: RealtorDocument[];
}

/** Payload for POST /api/realtors/register and PUT /api/realtors/me. */
export interface RealtorProfileForm {
  agencyName: string;
  businessRegistrationNumber?: string;
  city: string;
  areasCovered: string[];
  phoneNumber?: string;
  whatsappNumber?: string;
  bio?: string;
}
