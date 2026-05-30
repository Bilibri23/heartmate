import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { MapPin, Bed, Bath, Maximize, ShieldCheck, Star, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { getApiBaseUrl, getSiteUrl } from "@/lib/site-url";
import { slugify, createListingSlug } from "@/lib/slug";
import type { RealEstateListingSchema, ProductSchema } from "@/lib/schema-markup";

interface ListingDetail {
  id: string;
  title: string;
  description?: string;
  rentAmount?: number;
  price?: number;
  city: string;
  neighborhood?: string;
  address?: string;
  propertyType?: string;
  bedrooms?: number;
  bathrooms?: number;
  size?: number;
  area?: number;
  images?: string[];
  primaryPhotoUrl?: string;
  amenities?: string[];
  availableFrom?: string;
  status?: string;
  landlordId?: string;
  createdAt?: string;
  updatedAt?: string;
}

async function fetchListingBySlug(slug: string): Promise<ListingDetail | null> {
  try {
    const apiBase = getApiBaseUrl();

    // Attempt 1: try the slug directly as an ID (fallback for future backend slug support)
    try {
      const directRes = await fetch(`${apiBase}/listings/${encodeURIComponent(slug)}`, {
        next: { revalidate: 60 },
      });
      if (directRes.ok) {
        const data = await directRes.json();
        if (data && data.id) return data as ListingDetail;
      }
    } catch {
      // ignore
    }

    // Attempt 2: fetch active listings and match by generated slug
    const searchRes = await fetch(`${apiBase}/listings?size=100&status=ACTIVE`, {
      next: { revalidate: 60 },
    });
    if (!searchRes.ok) return null;

    const payload = await searchRes.json();
    const listings: ListingDetail[] = payload.content ?? payload ?? [];
    if (!Array.isArray(listings)) return null;

    // Try exact match first
    let match = listings.find((l) => createListingSlug(l.title, l.city, l.id) === slug);
    // Fallback: match without ID suffix
    if (!match) {
      match = listings.find((l) => slugify(`${l.title} ${l.city}`) === slug);
    }
    // Fallback: partial slug contains ID
    if (!match) {
      match = listings.find((l) => slug.includes(l.id));
    }
    return match || null;
  } catch {
    return null;
  }
}

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const listing = await fetchListingBySlug(slug);
  if (!listing) {
    return {
      title: "Listing not found | RoomBay",
      robots: { index: false, follow: true },
    };
  }

  const site = getSiteUrl();
  const price = listing.rentAmount ?? listing.price ?? 0;
  const title = `${listing.title} in ${listing.city} — RoomBay`;
  const description = `Rent this ${listing.propertyType || "property"} in ${listing.neighborhood || listing.city} for ${price.toLocaleString()} XAF/month. Verified listing with video tours on RoomBay.`;

  return {
    title,
    description,
    alternates: { canonical: `${site}/listing/${slug}` },
    openGraph: {
      type: "website",
      locale: "fr_FR",
      siteName: "RoomBay",
      title,
      description,
      url: `${site}/listing/${slug}`,
      images: [
        {
          url: listing.primaryPhotoUrl || listing.images?.[0] || `${site}/og-image.jpg`,
          width: 1200,
          height: 630,
          alt: title,
        },
      ],
    },
  };
}

export default async function ListingPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const listing = await fetchListingBySlug(slug);
  if (!listing) notFound();

  const site = getSiteUrl();
  const price = listing.rentAmount ?? listing.price ?? 0;
  const image = listing.primaryPhotoUrl || listing.images?.[0] || `${site}/og-image.jpg`;
  const allImages = listing.images?.length ? listing.images : [image];

  const realEstateSchema: RealEstateListingSchema = {
    "@context": "https://schema.org",
    "@type": "RealEstateListing",
    name: listing.title,
    description: listing.description || `Rent ${listing.title} in ${listing.city}`,
    url: `${site}/listing/${slug}`,
    image: allImages,
    datePosted: listing.createdAt,
    availability: listing.status === "ACTIVE" ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
    price: `${price}`,
    priceCurrency: "XAF",
    address: {
      "@type": "PostalAddress",
      streetAddress: listing.address,
      addressLocality: listing.city,
      addressCountry: "CM",
    },
    numberOfRooms: listing.bedrooms,
    floorSize: listing.size || listing.area ? {
      "@type": "QuantitativeValue",
      value: listing.size ?? listing.area ?? 0,
      unitCode: "MTK",
    } : undefined,
    amenityFeature: (listing.amenities ?? []).map((a) => ({
      "@type": "LocationFeatureSpecification",
      name: a,
      value: true,
    })),
  };

  const productSchema: ProductSchema = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: listing.title,
    description: listing.description || `Rent ${listing.title} in ${listing.city}`,
    image: allImages.slice(0, 3),
    url: `${site}/listing/${slug}`,
    brand: { "@type": "Brand", name: "RoomBay" },
    offers: {
      "@type": "Offer",
      price: `${price}`,
      priceCurrency: "XAF",
      availability: listing.status === "ACTIVE" ? "https://schema.org/InStock" : "https://schema.org/OutOfStock",
      url: `${site}/listing/${slug}`,
    },
  };

  return (
    <div className="min-h-screen bg-[#050816] text-white">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(realEstateSchema) }} />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(productSchema) }} />

      <div className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_top_left,rgba(56,189,248,0.18),transparent_24%),radial-gradient(circle_at_80%_20%,rgba(99,102,241,0.18),transparent_26%),linear-gradient(180deg,#050816_0%,#070b1d_60%,#04060f_100%)]" />
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-12 lg:px-8">
          <div className="flex items-center gap-2 text-sm text-white/50">
            <Link href="/" className="hover:text-white">Home</Link>
            <ChevronRight className="h-3.5 w-3.5" />
            <Link href={`/${listing.city.toLowerCase()}`} className="hover:text-white">{listing.city}</Link>
            <ChevronRight className="h-3.5 w-3.5" />
            <span className="text-white/80">{listing.title}</span>
          </div>

          <div className="mt-6 grid gap-8 lg:grid-cols-[1fr_0.4fr]">
            <div>
              <div className="relative overflow-hidden rounded-2xl border border-white/10 bg-[#0a1023] shadow-[0_24px_80px_rgba(0,0,0,0.38)] sm:rounded-[28px]">
                <div className="relative aspect-[16/10] overflow-hidden">
                  <Image src={image} alt={listing.title} fill className="object-cover" priority />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent" />
                  <div className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full border border-white/12 bg-black/40 px-3 py-1 text-xs font-medium text-white backdrop-blur-xl">
                    <ShieldCheck className="h-3.5 w-3.5 text-emerald-300" /> Verified
                  </div>
                </div>
                {allImages.length > 1 && (
                  <div className="grid grid-cols-4 gap-2 p-3 sm:gap-3 sm:p-4">
                    {allImages.slice(1, 5).map((img, i) => (
                      <div key={i} className="relative aspect-square overflow-hidden rounded-xl border border-white/10">
                        <Image src={img} alt={`${listing.title} ${i + 2}`} fill className="object-cover" />
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="mt-6 sm:mt-8">
                <h1 className="text-2xl font-semibold tracking-tight text-white sm:text-3xl md:text-4xl">{listing.title}</h1>
                <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-white/60">
                  <span className="flex items-center gap-1"><MapPin className="h-4 w-4 text-cyan-300" /> {listing.neighborhood || listing.city}</span>
                  {listing.bedrooms !== undefined && (
                    <span className="flex items-center gap-1"><Bed className="h-4 w-4" /> {listing.bedrooms} Beds</span>
                  )}
                  {listing.bathrooms !== undefined && (
                    <span className="flex items-center gap-1"><Bath className="h-4 w-4" /> {listing.bathrooms} Baths</span>
                  )}
                  {(listing.size || listing.area) && (
                    <span className="flex items-center gap-1"><Maximize className="h-4 w-4" /> {listing.size ?? listing.area} m²</span>
                  )}
                </div>
              </div>

              <div className="mt-6 rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
                <h2 className="text-lg font-semibold text-white">Description</h2>
                <p className="mt-2 text-sm leading-7 text-white/60 sm:text-base sm:leading-8">
                  {listing.description || `A verified ${listing.propertyType || "rental"} in ${listing.city}. Browse with confidence on RoomBay — no more fake listings, no more wasted visits.`}
                </p>
              </div>

              {listing.amenities && listing.amenities.length > 0 && (
                <div className="mt-4 rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
                  <h2 className="text-lg font-semibold text-white">Amenities</h2>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {listing.amenities.map((a) => (
                      <span key={a} className="rounded-full border border-white/10 bg-white/10 px-3 py-1.5 text-xs text-white/80">{a}</span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="space-y-4">
              <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 shadow-[0_18px_50px_rgba(15,23,42,0.18)] backdrop-blur-xl sm:rounded-[28px] sm:p-7">
                <p className="text-sm text-white/50">Monthly rent</p>
                <p className="mt-1 text-3xl font-semibold text-white">{price.toLocaleString()} <span className="text-lg font-normal text-white/60">XAF</span></p>
                {listing.availableFrom && (
                  <p className="mt-1 text-xs text-white/50">Available from {new Date(listing.availableFrom).toLocaleDateString("en-GB")}</p>
                )}
                <div className="mt-4 space-y-2">
                  <Button size="lg" className="h-12 w-full rounded-full bg-white text-sm font-semibold text-slate-950 hover:bg-slate-200">
                    Schedule a Visit
                  </Button>
                  <Link href={`/search?city=${encodeURIComponent(listing.city)}`}>
                    <Button size="lg" variant="outline" className="h-12 w-full rounded-full border-white/15 bg-white/6 text-sm text-white hover:bg-white/10">
                      More in {listing.city}
                    </Button>
                  </Link>
                </div>
                <div className="mt-4 flex items-center gap-2 rounded-xl border border-emerald-400/20 bg-emerald-400/10 p-3">
                  <Star className="h-4 w-4 text-emerald-300" />
                  <span className="text-xs text-emerald-200">Verified by RoomBay admin</span>
                </div>
              </div>

              <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
                <h3 className="text-sm font-semibold text-white">Trust & Verification</h3>
                <ul className="mt-3 space-y-2 text-sm text-white/60">
                  <li className="flex items-center gap-2"><ShieldCheck className="h-4 w-4 text-emerald-300" /> Admin-reviewed listing</li>
                  <li className="flex items-center gap-2"><ShieldCheck className="h-4 w-4 text-emerald-300" /> Landlord ID verified</li>
                  <li className="flex items-center gap-2"><ShieldCheck className="h-4 w-4 text-emerald-300" /> Video tour available</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
