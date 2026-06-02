"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";

export default function DashboardRedirect() {
  const { user, isLoading, mounted } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!mounted || isLoading) return;

    if (!user) {
      router.replace("/login");
    } else if (user.role === "LANDLORD") {
      router.replace("/landlord");
    } else if (user.role === "ADMIN") {
      router.replace("/admin");
    } else {
      router.replace("/for-you");
    }
  }, [user, isLoading, mounted, router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" />
    </div>
  );
}
