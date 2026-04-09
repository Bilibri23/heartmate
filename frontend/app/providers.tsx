"use client";

import { AuthProvider } from "@/context/auth-context";
import { LanguageProvider } from "@/context/language-context";
import { PlatformTourProvider } from "@/components/tour/platform-tour-provider";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <LanguageProvider>
      <AuthProvider>
        <PlatformTourProvider>{children}</PlatformTourProvider>
      </AuthProvider>
    </LanguageProvider>
  );
}
