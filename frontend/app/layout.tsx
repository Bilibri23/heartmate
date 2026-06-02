import type { Metadata, Viewport } from "next";
import "./globals.css";
import { Providers } from "./providers";
import { BottomNav } from "@/components/layout/bottom-nav";
import { GlobalOverlays } from "./global-overlays";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL?.replace(/\/+$/, "") || "https://roombay.app";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: "RoomBay — Verified Rentals in Cameroon",
    template: "%s | RoomBay",
  },
  description:
    "RoomBay is a Cameroon housing marketplace that helps tenants find verified rentals and helps landlords manage listings, applications, leases, and rent collection in one platform. Fast browsing, clear trust signals, in-app messaging, protected payments, and guided next steps.",
  keywords: [
    "roombay",
    "cameroon housing marketplace",
    "verified rentals cameroon",
    "apartments in yaoundé",
    "rentals in douala",
    "studios in yaoundé",
    "housing in douala",
    "rooms for rent in soa",
    "furnished apartments cameroon",
    "appartement douala",
    "location yaoundé",
    "landlord platform cameroon",
    "tenant applications cameroon",
  ],
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "RoomBay",
  },
  formatDetection: {
    telephone: true,
  },
  openGraph: {
    type: "website",
    locale: "fr_FR",
    siteName: "RoomBay",
    title: "RoomBay — Verified Rentals in Cameroon",
    description:
      "RoomBay is a Cameroon housing marketplace that helps tenants find verified rentals and helps landlords manage listings, applications, leases, and rent collection in one platform. Fast browsing, clear trust signals, in-app messaging, protected payments, and guided next steps.",
    images: [
      {
        url: `${siteUrl}/og-image.jpg`,
        width: 1200,
        height: 630,
        alt: "RoomBay — Verified Rentals in Cameroon",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "RoomBay — Verified Rentals in Cameroon",
    description:
      "RoomBay is a Cameroon housing marketplace that helps tenants find verified rentals and helps landlords manage listings, applications, leases, and rent collection in one platform. Fast browsing, clear trust signals, in-app messaging, protected payments, and guided next steps.",
    images: [`${siteUrl}/og-image.jpg`],
  },
  alternates: {
    canonical: siteUrl,
  },
  robots: {
    index: true,
    follow: true,
    "max-image-preview": "large",
    "max-snippet": -1,
    "max-video-preview": -1,
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
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="mobile-web-app-capable" content="yes" />
        <meta name="google" content="notranslate" />
      </head>
      <body className="antialiased bg-slate-50">
        <Providers>
          <main className="min-h-screen pb-16">
            {children}
          </main>
          <BottomNav />
          <GlobalOverlays />
        </Providers>
      </body>
    </html>
  );
}
