"use client";

import type { ReactNode } from "react";
import { Navbar } from "@/components/layout/navbar";
import { ErrorBoundary } from "@/components/error-boundary";

export default function DashboardLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <ErrorBoundary>
      <div className="min-h-screen bg-muted/20">
        <Navbar />
        <main className="container mx-auto p-4 md:p-8">
          {children}
        </main>
      </div>
    </ErrorBoundary>
  );
}