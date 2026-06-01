"use client";

import { usePathname } from "next/navigation";
import { Toaster } from "sonner";
import { AssistantWidget } from "@/components/ai/assistant-widget";
import { useEffect, useState } from "react";

function shouldHidePortals(pathname: string | null): boolean {
  if (!pathname) return false;
  return pathname === "/account/verify" || pathname === "/landlord/verification";
}

export function GlobalOverlays() {
  const pathname = usePathname();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const hidePortals = shouldHidePortals(pathname);

  // Don't render until mounted to prevent hydration mismatch
  if (!mounted) {
    return null;
  }

  return (
    <>
      {!hidePortals && <AssistantWidget />}
      {!hidePortals && (
        <Toaster
          position="top-center"
          richColors
          closeButton
          toastOptions={{
            duration: 5000,
            style: {
              marginTop: "60px",
            },
          }}
        />
      )}
    </>
  );
}
