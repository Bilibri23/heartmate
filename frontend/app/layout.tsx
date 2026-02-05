import type { Metadata, Viewport } from "next";
import "./globals.css";
import { Providers } from "./providers";
import { BottomNav } from "@/components/layout/bottom-nav";
import { Toaster } from "sonner";

export const metadata: Metadata = {
  title: "RoomBuddy - Student Housing in Cameroon",
  description: "Find your perfect student accommodation in Cameroon. Search rooms, apartments, and connect with verified landlords.",
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "RoomBuddy",
  },
  formatDetection: {
    telephone: true,
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  themeColor: "#3b82f6",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr" suppressHydrationWarning>
      <head>
        <link rel="apple-touch-icon" href="/icons/icon-192x192.png" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="mobile-web-app-capable" content="yes" />
      </head>
      <body className="antialiased bg-slate-50">
        <Providers>
          <main className="min-h-screen pb-16">
            {children}
          </main>
          <BottomNav />
          <Toaster 
            position="top-center" 
            richColors 
            closeButton
            toastOptions={{
              duration: 5000,
              style: {
                marginTop: '60px', // Below the header
              },
            }}
          />
        </Providers>
      </body>
    </html>
  );
}
