import api from '@/lib/api';

export interface ProfileCompletionStatus {
  role: string;
  completionPercentage: number;
  completedSteps: string[];
  missingSteps: string[];
  operationEligibility: Record<string, boolean>;
}

export const profileCompletionService = {
  async getStatus(): Promise<ProfileCompletionStatus> {
    const response = await api.get<ProfileCompletionStatus>('/auth/me/completion-status');
    return response.data;
  },
};
