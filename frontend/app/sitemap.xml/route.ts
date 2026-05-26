import { buildSitemapXml, collectSitemapUrls } from "@/lib/sitemap-xml";

export const revalidate = 3600;

export async function GET(): Promise<Response> {
  try {
    const urls = await collectSitemapUrls();
    const xml = buildSitemapXml(urls);

    return new Response(xml, {
      status: 200,
      headers: {
        "Content-Type": "application/xml; charset=utf-8",
        "Cache-Control": "public, max-age=0, s-maxage=3600, stale-while-revalidate=86400",
      },
    });
  } catch (error) {
    console.error("[sitemap.xml]", error);
    const fallback = buildSitemapXml([
      {
        loc: "https://roombay.app/",
        lastmod: new Date().toISOString(),
        changefreq: "daily",
        priority: 1,
      },
    ]);
    return new Response(fallback, {
      status: 200,
      headers: {
        "Content-Type": "application/xml; charset=utf-8",
        "Cache-Control": "public, max-age=60",
      },
    });
  }
}
