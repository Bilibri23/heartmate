import type { MetadataRoute } from "next";
import { getApiBaseUrl, getSiteUrl } from "@/lib/site-url";

export const revalidate = 3600;

type ListingPage = {
  content?: Array<{ id: string; updatedAt?: string }>;
  totalPages?: number;
};

const STATIC_PATHS: Array<{
  path: string;
  changeFrequency: MetadataRoute.Sitemap[number]["changeFrequency"];
  priority: number;
}> = [
  { path: "", changeFrequency: "daily", priority: 1 },
  { path: "/search", changeFrequency: "daily", priority: 0.9 },
  { path: "/listings", changeFrequency: "daily", priority: 0.9 },
  { path: "/login", changeFrequency: "monthly", priority: 0.3 },
  { path: "/register", changeFrequency: "monthly", priority: 0.3 },
];

async function fetchListingIds(apiBase: string): Promise<Array<{ id: string; updatedAt?: string }>> {
  const results: Array<{ id: string; updatedAt?: string }> = [];
  const pageSize = 100;
  const maxPages = 50;

  for (let page = 0; page < maxPages; page++) {
    const url = `${apiBase}/listings?page=${page}&size=${pageSize}&sortBy=updatedAt&sortDir=DESC`;
    const res = await fetch(url, {
      next: { revalidate: 3600 },
      headers: { Accept: "application/json" },
    });
    if (!res.ok) {
      break;
    }
    const data = (await res.json()) as ListingPage;
    const batch = data.content ?? [];
    if (batch.length === 0) {
      break;
    }
    for (const item of batch) {
      if (item.id) {
        results.push({ id: item.id, updatedAt: item.updatedAt });
      }
    }
    const totalPages = data.totalPages ?? page + 1;
    if (page + 1 >= totalPages) {
      break;
    }
  }

  return results;
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const site = getSiteUrl();
  const now = new Date();
  const apiBase = getApiBaseUrl();

  const staticEntries: MetadataRoute.Sitemap = STATIC_PATHS.map(({ path, changeFrequency, priority }) => ({
    url: `${site}${path}`,
    lastModified: now,
    changeFrequency,
    priority,
  }));

  let listingEntries: MetadataRoute.Sitemap = [];
  try {
    const listings = await fetchListingIds(apiBase);
    listingEntries = listings.map((listing) => ({
      url: `${site}/listings/${listing.id}`,
      lastModified: listing.updatedAt ? new Date(listing.updatedAt) : now,
      changeFrequency: "weekly" as const,
      priority: 0.8,
    }));
  } catch {
    // API unreachable at build/request time — still publish static URLs
  }

  return [...staticEntries, ...listingEntries];
}
