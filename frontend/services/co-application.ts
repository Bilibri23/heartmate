import api from '../lib/api';

export interface CoApplicationInvitation {
  id: string;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED' | 'CANCELLED' | 'APPLIED';
  message?: string;
  createdAt: string;
  expiresAt: string;
  respondedAt?: string;
  declineReason?: string;
  
  // Listing info
  listingId: string;
  listingTitle: string;
  listingAddress: string;
  listingCity: string;
  listingRent: number;
  listingPhotoUrl?: string;
  
  // Inviter info
  inviterId: string;
  inviterName: string;
  inviterPhotoUrl?: string;
  
  // Invitee info
  inviteeId: string;
  inviteeName: string;
  inviteePhotoUrl?: string;
  
  // Match info
  matchId: string;
  compatibilityScore?: number;
  
  // Context
  isInviter: boolean;
  canRespond: boolean;
}

export interface InviteRoommateRequest {
  listingId: string;
  roommateId: string;
  matchId: string;
  message?: string;
  proposedMoveInDate?: string;
  proposedLeaseDuration?: number;
}

export const coApplicationService = {
  // Invite a roommate to apply together
  inviteRoommate: async (request: InviteRoommateRequest): Promise<CoApplicationInvitation> => {
    const response = await api.post<CoApplicationInvitation>('/co-applications/invite', request);
    return response.data;
  },

  // Accept an invitation
  acceptInvitation: async (invitationId: string): Promise<CoApplicationInvitation> => {
    const response = await api.post<CoApplicationInvitation>(`/co-applications/invitations/${invitationId}/accept`);
    return response.data;
  },

  // Decline an invitation
  declineInvitation: async (invitationId: string, reason?: string): Promise<CoApplicationInvitation> => {
    const response = await api.post<CoApplicationInvitation>(
      `/co-applications/invitations/${invitationId}/decline`,
      { reason }
    );
    return response.data;
  },

  // Cancel an invitation (inviter only)
  cancelInvitation: async (invitationId: string): Promise<void> => {
    await api.delete(`/co-applications/invitations/${invitationId}`);
  },

  // Get pending invitations received
  getReceivedInvitations: async (): Promise<CoApplicationInvitation[]> => {
    const response = await api.get<CoApplicationInvitation[]>('/co-applications/invitations/received');
    return response.data;
  },

  // Get invitations sent
  getSentInvitations: async (): Promise<CoApplicationInvitation[]> => {
    const response = await api.get<CoApplicationInvitation[]>('/co-applications/invitations/sent');
    return response.data;
  },

  // Get count of pending invitations
  getPendingCount: async (): Promise<number> => {
    const response = await api.get<{ count: number }>('/co-applications/invitations/count');
    return response.data.count;
  },

  // Check if can apply together with a roommate for a listing
  checkCanApplyTogether: async (listingId: string, roommateId: string): Promise<boolean> => {
    const response = await api.get<{ canApplyTogether: boolean }>(
      `/co-applications/check/${listingId}/${roommateId}`
    );
    return response.data.canApplyTogether;
  },

  // Submit a joint application from an accepted invitation
  submitCoApplication: async (invitationId: string, applicationData: {
    listingId: string;
    message?: string;
    moveInDate?: string;
    leaseDurationMonths?: number;
  }): Promise<any> => {
    const response = await api.post(`/co-applications/invitations/${invitationId}/apply`, applicationData);
    return response.data;
  },

  // Confirm a co-application (as co-applicant)
  confirmCoApplication: async (applicationId: string): Promise<any> => {
    const response = await api.post(`/co-applications/applications/${applicationId}/confirm`);
    return response.data;
  },

  // Get accepted invitation for a listing
  getAcceptedInvitationForListing: async (listingId: string): Promise<CoApplicationInvitation | null> => {
    try {
      const [received, sent] = await Promise.all([
        api.get<CoApplicationInvitation[]>('/co-applications/invitations/received'),
        api.get<CoApplicationInvitation[]>('/co-applications/invitations/sent')
      ]);
      
      const allInvitations = [...(received.data || []), ...(sent.data || [])];
      const accepted = allInvitations.find(
        i => i.listingId === listingId && i.status === 'ACCEPTED'
      );
      return accepted || null;
    } catch {
      return null;
    }
  },
};

export default coApplicationService;
