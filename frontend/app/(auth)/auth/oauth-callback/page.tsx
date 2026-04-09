"use client";

import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import { authService } from "@/services/auth.service";
import type { User } from "@/types/auth";

/**
 * Backend redirects here with URL fragment: access_token, refresh_token, expires_in (not sent to server).
 */
export default function OAuthCallbackPage() {
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const run = async () => {
      const raw = typeof window !== "undefined" ? window.location.hash.replace(/^#/, "") : "";
      if (!raw) {
        setError("Missing OAuth tokens. Try signing in again.");
        return;
      }
      const params = new URLSearchParams(raw);
      const access = params.get("access_token");
      const refresh = params.get("refresh_token");
      if (!access || !refresh) {
        setError("Invalid OAuth response. Try signing in again.");
        return;
      }
      localStorage.setItem("token", access);
      localStorage.setItem("refreshToken", refresh);
      try {
        const me = await authService.getMe();
        const user: User = {
          id: me.userId,
          firstName: me.firstName,
          lastName: me.lastName,
          role: me.role,
          email: me.email,
          emailVerified: me.emailVerified,
          phoneVerified: me.phoneVerified,
        };
        localStorage.setItem("user", JSON.stringify(user));
        window.history.replaceState(null, "", window.location.pathname);
        if (me.role === "ADMIN") {
          window.location.assign("/admin");
        } else if (me.role === "LANDLORD") {
          window.location.assign("/landlord");
        } else {
          window.location.assign("/for-you");
        }
      } catch {
        setError("Could not load your profile. Try again.");
      }
    };
    run();
  }, []);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 p-6">
      {error ? (
        <p className="text-center text-sm text-red-600">{error}</p>
      ) : (
        <div className="flex flex-col items-center gap-3 text-slate-600">
          <Loader2 className="h-8 w-8 animate-spin" />
          <p className="text-sm">Finishing sign-in…</p>
        </div>
      )}
    </div>
  );
}
