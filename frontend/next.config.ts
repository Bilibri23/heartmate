import type { NextConfig } from "next";
import { springBackendOrigin } from "./lib/spring-backend-origin";

const nextConfig: NextConfig = {
  allowedDevOrigins: [
    "tame-tables-obey.loca.lt",
    "fruity-ghosts-watch.loca.lt",
    "nondisruptingly-unenthusiastic-melody.ngrok-free.dev",
  ],
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'res.cloudinary.com',
        pathname: '/**',
      },
      {
        protocol: 'https',
        hostname: 'images.unsplash.com',
        pathname: '/**',
      },
      {
        protocol: 'https',
        hostname: 'picsum.photos',
        pathname: '/**',
      },
      {
        protocol: 'https',
        hostname: 'via.placeholder.com',
        pathname: '/**',
      },
      {
        protocol: 'https',
        hostname: 'placehold.co',
        pathname: '/**',
      },
    ],
  },
  async headers() {
    const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL || process.env.NEXT_PUBLIC_API_URL || "https://api.roombay.app";
    const apiOrigin = new URL(apiBase).origin;
    const csp = [
      "default-src 'self'",
      "script-src 'self' 'unsafe-eval' 'unsafe-inline' https://accounts.google.com https://apis.google.com",
      "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
      "style-src-elem 'self' 'unsafe-inline' https://fonts.googleapis.com",
      "img-src 'self' data: blob: https://res.cloudinary.com https://images.unsplash.com https://picsum.photos https://via.placeholder.com https://placehold.co https://api.dicebear.com https://cdnjs.cloudflare.com https://*.tile.openstreetmap.org",
      "font-src 'self' data: https://fonts.gstatic.com",
      `connect-src 'self' ${apiOrigin} https://api.roombay.app https://www.roombay.app https://roombay.app wss://api.roombay.app https://*.tile.openstreetmap.org https://*.ingest.us.sentry.io`,
      "media-src 'self' blob: https://res.cloudinary.com",
      "frame-src 'self' https://accounts.google.com https://www.google.com",
      "object-src 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "frame-ancestors 'none'",
      "upgrade-insecure-requests",
    ].join("; ");
    return [
      {
        source: "/:path*",
        headers: [
          { key: "Content-Security-Policy", value: csp },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=(self)" },
          { key: "X-Frame-Options", value: "DENY" },
        ],
      },
    ];
  },
  async rewrites() {
    const backend = springBackendOrigin(process.env);
    if (process.env.NODE_ENV === "development") {
      // eslint-disable-next-line no-console
      console.info("[next.config] /api and /ws rewrite base →", backend);
    }
    return [
      {
        source: '/api/:path*',
        destination: `${backend}/api/:path*`,
      },
      // OAuth: handled by app/oauth2 and app/login/oauth2 route handlers (forward X-Forwarded-* to Spring).
      // SockJS + STOMP (notifications) — same-origin when using ngrok → Next dev
      {
        source: '/ws/:path*',
        destination: `${backend}/ws/:path*`,
      },
    ];
  },
};

export default nextConfig;
