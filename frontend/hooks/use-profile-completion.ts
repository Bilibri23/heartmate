"use client";

import { useEffect, useState } from "react";
import { profileCompletionService, ProfileCompletionStatus } from "@/services/profile-completion";

export function useProfileCompletion() {
  const [status, setStatus] = useState<ProfileCompletionStatus | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refresh = async () => {
    try {
      const data = await profileCompletionService.getStatus();
      setStatus(data);
    } catch {
      setStatus(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    refresh();
  }, []);

  return { status, isLoading, refresh };
}
