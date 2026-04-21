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
