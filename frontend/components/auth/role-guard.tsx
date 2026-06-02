"use client";

import type { ReactNode } from "react";
import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";

type Role = "ADMIN" | "LANDLORD" | "STUDENT";

function homeForRole(role?: string | null): string {
  if (role === "ADMIN") return "/admin";
  if (role === "LANDLORD") return "/landlord";
  return "/for-you";
}

function RouteLoading() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
    </div>
  );
}

export function RoleGuard({
  allowedRoles,
  children,
}: {
  allowedRoles: Role[];
  children: ReactNode;
}) {
  const { user, isLoading, mounted } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const authReady = mounted && !isLoading;
  const userRole = user?.role;
  const allowed = Boolean(userRole && allowedRoles.includes(userRole as Role));

  useEffect(() => {
    if (!authReady) return;

    if (!user) {
      const next = pathname ? `?next=${encodeURIComponent(pathname)}` : "";
      router.replace(`/login${next}`);
      return;
    }

    if (!allowed) {
      router.replace(homeForRole(userRole));
    }
  }, [allowed, authReady, pathname, router, user, userRole]);

  if (!authReady || !user || !allowed) {
    return <RouteLoading />;
  }

  return <>{children}</>;
}
