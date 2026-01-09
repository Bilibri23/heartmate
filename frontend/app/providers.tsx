"use client";

import { AuthProvider } from "@/context/auth-context";
import { LanguageProvider } from "@/context/language-context";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <LanguageProvider>
      <AuthProvider>{children}</AuthProvider>
    </LanguageProvider>
  );
}
