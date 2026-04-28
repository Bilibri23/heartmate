"use client";

import { usePathname } from "next/navigation";
import { Toaster } from "sonner";
import { AssistantWidget } from "@/components/ai/assistant-widget";

function shouldHidePortals(pathname: string | null): boolean {
  if (!pathname) return false;
  return pathname === "/account/verify" || pathname === "/landlord/verification";
}

export function GlobalOverlays() {
  const pathname = usePathname();
  const hidePortals = shouldHidePortals(pathname);

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
