import type { MetadataRoute } from "next";
import { getSiteUrl } from "@/lib/site-url";

export default function robots(): MetadataRoute.Robots {
  const site = getSiteUrl();

  return {
    rules: [
      {
        userAgent: "*",
        allow: ["/", "/search", "/listings", "/login", "/register"],
        disallow: [
          "/admin/",
          "/landlord/",
          "/dashboard/",
          "/messages/",
          "/onboarding/",
          "/settings/",
          "/profile/",
          "/payments/",
          "/leases/",
          "/applications/",
          "/matches/",
          "/household/",
          "/favorites/",
          "/notifications/",
          "/verification/",
          "/preferences/",
          "/for-you/",
          "/reviews/",
          "/account/",
          "/auth/",
          "/api/",
        ],
      },
    ],
    sitemap: `${site}/sitemap.xml`,
    host: site,
  };
}
