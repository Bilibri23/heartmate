import type { NextConfig } from "next";

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
    const backend = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8082';
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
