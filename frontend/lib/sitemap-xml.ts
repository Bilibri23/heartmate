import { getApiBaseUrl, getSiteUrl } from "@/lib/site-url";

export type SitemapUrl = {
  loc: string;
  lastmod?: string;
  changefreq?: string;
  priority?: number;
};

type ListingPage = {
  content?: Array<{ id: string; updatedAt?: string; createdAt?: string }>;
  totalPages?: number;
};

const STATIC_PATHS: Array<{
  path: string;
  changefreq: string;
  priority: number;
}> = [
  { path: "/", changefreq: "daily", priority: 1 },
  { path: "/search", changefreq: "daily", priority: 0.9 },
  { path: "/listings", changefreq: "daily", priority: 0.9 },
  { path: "/login", changefreq: "monthly", priority: 0.3 },
  { path: "/register", changefreq: "monthly", priority: 0.3 },
];

function escapeXml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

function formatLastmod(iso?: string): string | undefined {
  if (!iso) {
    return undefined;
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return undefined;
  }
  return date.toISOString();
}

export async function collectSitemapUrls(): Promise<SitemapUrl[]> {
  const site = getSiteUrl();
  const now = formatLastmod(new Date().toISOString())!;

  const urls: SitemapUrl[] = STATIC_PATHS.map(({ path, changefreq, priority }) => ({
    loc: path === "/" ? `${site}/` : `${site}${path}`,
    lastmod: now,
    changefreq,
    priority,
  }));

  try {
    const apiBase = getApiBaseUrl();
    const pageSize = 100;
    const maxPages = 50;

    for (let page = 0; page < maxPages; page++) {
      const query = new URLSearchParams({
        page: String(page),
        size: String(pageSize),
        sortBy: "createdAt",
        sortDir: "DESC",
      });
      const res = await fetch(`${apiBase}/listings?${query}`, {
        headers: { Accept: "application/json" },
        next: { revalidate: 3600 },
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
        if (!item.id) {
          continue;
        }
        urls.push({
          loc: `${site}/listings/${item.id}`,
          lastmod: formatLastmod(item.updatedAt ?? item.createdAt) ?? now,
          changefreq: "weekly",
          priority: 0.8,
        });
      }
      const totalPages = data.totalPages ?? page + 1;
      if (page + 1 >= totalPages) {
        break;
      }
    }
  } catch {
    // Keep static URLs when API is unreachable
  }

  return urls;
}

export function buildSitemapXml(urls: SitemapUrl[]): string {
  const body = urls
    .map((entry) => {
      const parts = [`    <url>`, `      <loc>${escapeXml(entry.loc)}</loc>`];
      if (entry.lastmod) {
        parts.push(`      <lastmod>${escapeXml(entry.lastmod)}</lastmod>`);
      }
      if (entry.changefreq) {
        parts.push(`      <changefreq>${escapeXml(entry.changefreq)}</changefreq>`);
      }
      if (entry.priority != null) {
        parts.push(`      <priority>${entry.priority.toFixed(1)}</priority>`);
      }
      parts.push(`    </url>`);
      return parts.join("\n");
    })
    .join("\n");

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    body,
    "</urlset>",
    "",
  ].join("\n");
}
