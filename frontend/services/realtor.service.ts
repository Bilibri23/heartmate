import api from "../lib/api";
import type { RealtorProfile, RealtorProfileForm } from "../types/realtor";

interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface AddDocumentPayload {
  documentType: string;
  documentUrl: string;
}

export const realtorService = {
  // ---- Realtor self-service ----
  registerProfile: async (data: RealtorProfileForm) => {
    const response = await api.post<RealtorProfile>("/realtors/register", data);
    return response.data;
  },

  /** Returns null when the current user has no realtor profile yet (404 / not found). */
  getMe: async (): Promise<RealtorProfile | null> => {
    try {
      const response = await api.get<RealtorProfile>("/realtors/me");
      return response.data;
    } catch (err: unknown) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } };
      const status = ax?.response?.status;
      const msg = String(ax?.response?.data?.message || "").toLowerCase();
      if (status === 404 || (status === 400 && msg.includes("not found"))) {
        return null;
      }
      throw err;
    }
  },

  updateProfile: async (data: RealtorProfileForm) => {
    const response = await api.put<RealtorProfile>("/realtors/me", data);
    return response.data;
  },

  addDocument: async (payload: AddDocumentPayload) => {
    const response = await api.post<RealtorProfile>("/realtors/verification-documents", payload);
    return response.data;
  },

  // ---- Admin verification queue ----
  listByStatus: async (status = "PENDING", page = 0, size = 20) => {
    const response = await api.get<Page<RealtorProfile>>("/admin/realtors", {
      params: { status, page, size },
    });
    return response.data;
  },

  approve: async (id: string) => {
    const response = await api.post<RealtorProfile>(`/admin/realtors/${id}/approve`, {});
    return response.data;
  },

  reject: async (id: string, reason: string) => {
    const response = await api.post<RealtorProfile>(`/admin/realtors/${id}/reject`, { reason });
    return response.data;
  },

  suspend: async (id: string, reason: string) => {
    const response = await api.post<RealtorProfile>(`/admin/realtors/${id}/suspend`, { reason });
    return response.data;
  },
};
