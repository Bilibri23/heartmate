"use client";

import { AuthProvider } from "@/context/auth-context";
import { Navbar } from "@/components/layout/navbar";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-muted/20">
      <Navbar />
      <main className="container mx-auto p-4 md:p-8">
        {children}
      </main>
    </div>
  );
}
