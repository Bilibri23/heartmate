import type { MetadataRoute } from "next";
import { getSiteUrl } from "@/lib/site-url";

export default function robots(): MetadataRoute.Robots {
  const site = getSiteUrl();

  return {
    rules: [
      {
        userAgent: "*",
        allow: ["/", "/search", "/listings", "/listing", "/douala", "/yaounde", "/soa", "/buea", "/bastos", "/logbessou", "/bonamoussadi"],
        disallow: [
          "/admin",
          "/admin/",
          "/landlord",
          "/landlord/",
          "/dashboard",
          "/dashboard/",
          "/messages",
          "/messages/",
          "/onboarding",
          "/onboarding/",
          "/settings",
          "/settings/",
          "/profile",
          "/profile/",
          "/payments",
          "/payments/",
          "/leases",
          "/leases/",
          "/applications",
          "/applications/",
          "/matches",
          "/matches/",
          "/household",
          "/household/",
          "/favorites",
          "/favorites/",
          "/notifications",
          "/notifications/",
          "/verification",
          "/verification/",
          "/preferences",
          "/preferences/",
          "/for-you",
          "/for-you/",
          "/reviews",
          "/reviews/",
          "/account",
          "/account/",
          "/auth",
          "/auth/",
          "/api",
          "/api/",
          "/login",
          "/register",
          "/forgot-password",
        ],
      },
    ],
    sitemap: `${site}/sitemap.xml`,
    host: site,
  };
}
