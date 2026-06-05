import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { MapPin, Building2, Search, Star, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { getApiBaseUrl, getSiteUrl } from "@/lib/site-url";
import { createListingSlug } from "@/lib/slug";
import type { LocalBusinessSchema } from "@/lib/schema-markup";

const CITY_DATA: Record<string, {
  name: string;
  region: string;
  headline: string;
  description: string;
  demand: string;
  nearby: string[];
  propertyTypes: string[];
  image: string;
}> = {
  douala: {
    name: "Douala",
    region: "Littoral",
    headline: "Verified Rentals in Douala",
    description:
      "Douala is Cameroon's economic capital and largest city. From Akwa to Bonamoussadi, find verified apartments, studios, and rooms for rent with video tours and AI-powered search.",
    demand:
      "High demand for furnished studios near Bonapriso and Akwa. Family apartments are popular in Logbessou and Bonamoussadi.",
    nearby: [
      "Bonapriso Business District",
      "Akwa Commercial Zone",
      "Douala International Airport",
      "University of Douala",
    ],
    propertyTypes: ["Studios", "2-Bed Apartments", "Shared Rooms", "Furnished Flats"],
    image: "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80",
  },
  buea: {
    name: "Buea",
    region: "Southwest",
    headline: "Housing in Buea & Molyko",
    description:
      "Find student-friendly rooms and studios around Molyko and Sandpit, plus family apartments near Clerk Quarters. Browse verified listings with video tours and landlord chat.",
    demand:
      "Strong student demand around University of Buea and Molyko; growing interest for furnished units near Mile 17 and Sandpit.",
    nearby: [
      "University of Buea",
      "Molyko Stadium",
      "Mile 17 Motor Park",
      "Sandpit / Checkpoint",
    ],
    propertyTypes: ["Studios", "1-2 Bed Apartments", "Shared Rooms", "Furnished Flats"],
    image: "https://images.unsplash.com/photo-1505761671935-60b3a7427bad?auto=format&fit=crop&w=1200&q=80",
  },
  yaounde: {
    name: "Yaoundé",
    region: "Centre",
    headline: "Verified Rentals in Yaoundé",
    description:
      "Yaoundé, the political capital, offers a mix of quiet residential neighborhoods like Bastos and Melen, plus student-friendly areas near Soa. Browse verified listings with video tours.",
    demand:
      "Strong demand near ministries and NGOs in Bastos. Students drive affordable housing demand around Soa and Ngoa-Ekellé.",
    nearby: [
      "Bastos Diplomatic Quarter",
      "Soa University Campus",
      "Ngousso Hospital",
      "Ministry District",
    ],
    propertyTypes: ["Studios", "3-Bed Apartments", "Self-contained Rooms", "Villas"],
    image: "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80",
  },
  soa: {
    name: "Soa",
    region: "Centre",
    headline: "Student Rentals in Soa",
    description:
      "Soa is a fast-growing university town on the outskirts of Yaoundé. Perfect for students and young professionals looking for affordable, verified rooms and studios.",
    demand:
      "Peak demand at the start of academic semesters. Shared flats and single rooms near campus move fastest.",
    nearby: [
      "University of Soa",
      "Yaoundé Ring Road",
      "Melen Market",
      "Olembe District",
    ],
    propertyTypes: ["Single Rooms", "Shared Flats", "Small Studios", "Student Housing"],
    image: "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80",
  },
  bastos: {
    name: "Bastos",
    region: "Yaoundé",
    headline: "Premium Rentals in Bastos, Yaoundé",
    description:
      "Bastos is Yaoundé's most prestigious neighborhood, home to diplomats, NGOs, and executives. Find secure, verified apartments and studios with modern finishes.",
    demand:
      "Consistently high demand for furnished apartments with parking and security. Short-term rentals are also sought after.",
    nearby: [
      "Embassy Quarter",
      "Ministries Complex",
      "Mont Fébé",
      "Yaoundé Central Hospital",
    ],
    propertyTypes: ["Furnished Studios", "3-Bed Apartments", "Luxury Flats", "Serviced Residences"],
    image: "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80",
  },
  logbessou: {
    name: "Logbessou",
    region: "Douala",
    headline: "Family-Friendly Rentals in Logbessou",
    description:
      "Logbessou is one of Douala's fastest-growing neighborhoods, popular with families and professionals seeking newer construction and more space.",
    demand:
      "Growing demand for 2- and 3-bedroom apartments. New developments attract long-term renters who want modern amenities.",
    nearby: [
      "Bonamoussadi Junction",
      "Douala Shopping Malls",
      "Logbessou Market",
      "Schools & Hospitals",
    ],
    propertyTypes: ["2-Bed Apartments", "3-Bed Flats", "Family Homes", "Modern Studios"],
    image: "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1200&q=80",
  },
  bonamoussadi: {
    name: "Bonamoussadi",
    region: "Douala",
    headline: "Modern Rentals in Bonamoussadi",
    description:
      "Bonamoussadi offers a balanced lifestyle with commercial zones, schools, and residential blocks. Ideal for professionals and small families.",
    demand:
      "Steady demand across all property types. Proximity to hospitals and schools makes it a top choice for families.",
    nearby: [
      "Laquintinie Hospital",
      "Bonamoussadi Market",
      "Akwa Business District",
      "Major Schools & Churches",
    ],
    propertyTypes: ["Apartments", "Studios", "Shared Flats", "Duplexes"],
    image: "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80",
  },
};

export async function generateStaticParams() {
  return Object.keys(CITY_DATA).map((city) => ({ city }));
}

export async function generateMetadata({ params }: { params: Promise<{ city: string }> }): Promise<Metadata> {
  const { city } = await params;
  const data = CITY_DATA[city];
  if (!data) return { title: "City not found | RoomBay" };

  const site = getSiteUrl();
  const title = `${data.headline} — RoomBay`;
  const description = data.description;

  return {
    title,
    description,
    alternates: { canonical: `${site}/${city}` },
    openGraph: {
      type: "website",
      locale: "fr_FR",
      siteName: "RoomBay",
      title,
      description,
      url: `${site}/${city}`,
      images: [{ url: data.image, width: 1200, height: 630, alt: title }],
    },
  };
}

interface Listing {
  id: string;
  title: string;
  description?: string;
  rentAmount?: number;
  price?: number;
  city: string;
  neighborhood?: string;
  address?: string;
  bedrooms?: number;
  bathrooms?: number;
  propertyType?: string;
  images?: string[];
  primaryPhotoUrl?: string;
  amenities?: string[];
  availableFrom?: string;
  status?: string;
  landlordId?: string;
  createdAt?: string;
}

async function fetchCityListings(cityName: string): Promise<Listing[]> {
  try {
    const apiBase = getApiBaseUrl();
    const res = await fetch(`${apiBase}/search?city=${encodeURIComponent(cityName)}&size=6`, {
      next: { revalidate: 300 },
    });
    if (!res.ok) return [];
    const data = await res.json();
    const content = data.content ?? data ?? [];
    return Array.isArray(content) ? content : [];
  } catch {
    return [];
  }
}

export default async function CityPage({ params }: { params: Promise<{ city: string }> }) {
  const { city } = await params;
  const data = CITY_DATA[city];
  if (!data) notFound();

  const listings = await fetchCityListings(data.name);
  const site = getSiteUrl();

  const localBusinessSchema: LocalBusinessSchema = {
    "@context": "https://schema.org",
    "@type": "LocalBusiness",
    name: `RoomBay ${data.name}`,
    description: data.description,
    url: `${site}/${city}`,
    image: data.image,
    address: {
      "@type": "PostalAddress",
      addressLocality: data.name,
      addressRegion: data.region,
      addressCountry: "CM",
    },
    areaServed: data.name,
    priceRange: "$$",
  };
  const breadcrumbSchema = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "Home", item: `${site}/` },
      { "@type": "ListItem", position: 2, name: data.name, item: `${site}/${city}` },
    ],
  };
  const faqSchema = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: [
      {
        "@type": "Question",
        name: `How do I find rentals in ${data.name} on RoomBay?`,
        acceptedAnswer: {
          "@type": "Answer",
          text: `Open the ${data.name} city page, compare verified listings, review photos and trust signals, then use RoomBay search or messaging to continue.`,
        },
      },
      {
        "@type": "Question",
        name: `Are RoomBay listings in ${data.name} reviewed?`,
        acceptedAnswer: {
          "@type": "Answer",
          text: "RoomBay shows tenant-facing trust indicators when listings, landlords, identities, or property proofs have been reviewed. These signals do not replace an in-person viewing or final checks before payment.",
        },
      },
    ],
  };

  return (
    <div className="min-h-screen bg-[#050816] text-white">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(localBusinessSchema) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbSchema) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(faqSchema) }}
      />

      <div className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_top_left,rgba(56,189,248,0.18),transparent_24%),radial-gradient(circle_at_80%_20%,rgba(99,102,241,0.18),transparent_26%),linear-gradient(180deg,#050816_0%,#070b1d_60%,#04060f_100%)]" />
        <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 sm:py-20 lg:px-8">
          <div className="grid items-center gap-10 lg:grid-cols-[1fr_0.9fr]">
            <div>
              <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-white/12 bg-white/6 px-3 py-1.5 text-xs font-medium text-cyan-300 backdrop-blur-xl sm:mb-6 sm:px-4 sm:py-2 sm:text-sm">
                <MapPin className="h-3.5 w-3.5" />
                {data.region}, Cameroon
              </div>
              <h1 className="text-[1.6rem] font-semibold leading-[1.15] tracking-[-0.02em] text-white sm:text-[2.25rem] md:text-[2.75rem] lg:text-[3.25rem]">
                {data.headline}
              </h1>
              <p className="mt-4 max-w-2xl text-[0.95rem] leading-7 text-white/65 sm:text-base sm:leading-8 md:text-lg">
                {data.description}
              </p>
              <div className="mt-6 flex flex-col flex-wrap gap-3 sm:flex-row">
                <Link href={`/search?city=${encodeURIComponent(data.name)}`}>
                  <Button size="lg" className="h-12 w-full rounded-full bg-white px-6 text-sm font-semibold text-slate-950 hover:bg-slate-200 sm:h-14 sm:w-auto sm:text-base">
                    <Search className="mr-2 h-5 w-5" />
                    Search {data.name} Rentals
                  </Button>
                </Link>
                <Link href="/register?role=LANDLORD">
                  <Button size="lg" variant="outline" className="h-12 w-full rounded-full border-white/15 bg-white/6 px-6 text-sm text-white hover:bg-white/10 sm:h-14 sm:w-auto sm:text-base">
                    <Building2 className="mr-2 h-5 w-5" />
                    List a Property
                  </Button>
                </Link>
              </div>
            </div>
            <div className="relative mx-auto w-full max-w-lg">
              <div className="absolute -left-4 top-4 hidden h-28 w-28 rounded-full bg-cyan-400/20 blur-3xl md:block" />
              <div className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/[0.05] shadow-[0_40px_120px_rgba(2,6,23,0.55)] sm:rounded-[32px]">
                <div className="relative aspect-[4/3]">
                  <Image src={data.image} alt={data.name} fill className="object-cover" priority />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
                </div>
                <div className="p-4 sm:p-6">
                  <div className="flex flex-wrap gap-2">
                    {data.propertyTypes.map((t) => (
                      <span key={t} className="rounded-full border border-white/10 bg-white/10 px-3 py-1 text-xs font-medium text-white backdrop-blur-xl">
                        {t}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <section className="px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="mb-8 sm:mb-12">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">Market insights</p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl">Rental demand in {data.name}</h2>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 sm:gap-5">
            <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
              <h3 className="text-lg font-semibold text-white">Demand</h3>
              <p className="mt-2 text-sm leading-6 text-white/55 sm:text-base sm:leading-7">{data.demand}</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
              <h3 className="text-lg font-semibold text-white">Nearby</h3>
              <ul className="mt-2 space-y-1.5 text-sm text-white/55 sm:text-base">
                {data.nearby.map((n) => (
                  <li key={n} className="flex items-center gap-2">
                    <MapPin className="h-3.5 w-3.5 shrink-0 text-cyan-300" />
                    {n}
                  </li>
                ))}
              </ul>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
              <h3 className="text-lg font-semibold text-white">Property Types</h3>
              <div className="mt-2 flex flex-wrap gap-2">
                {data.propertyTypes.map((t) => (
                  <span key={t} className="rounded-full border border-white/10 bg-white/10 px-2.5 py-1 text-xs text-white/70">{t}</span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="border-y border-white/8 bg-white/[0.03] px-4 py-12 backdrop-blur-sm sm:px-6 sm:py-16 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="mb-8 flex items-end justify-between sm:mb-12">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">Featured listings</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl">Verified rentals in {data.name}</h2>
            </div>
            <Link href={`/search?city=${encodeURIComponent(data.name)}`} className="hidden text-sm text-cyan-300 hover:text-cyan-200 sm:block">
              View all →
            </Link>
          </div>

          {listings.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 sm:gap-5">
              {listings.map((listing: Listing) => {
                const image = listing.primaryPhotoUrl || listing.images?.[0] || data.image;
                const price = listing.rentAmount ?? listing.price ?? 0;
                const slug = createListingSlug(listing.title, listing.city, listing.id);
                return (
                  <Link key={listing.id} href={`/listing/${slug}`} className="group relative overflow-hidden rounded-2xl border border-white/10 bg-[#0a1023] shadow-[0_24px_80px_rgba(0,0,0,0.38)] sm:rounded-[28px]">
                    <div className="relative aspect-[4/3] overflow-hidden">
                      <Image src={image} alt={listing.title} fill className="object-cover transition duration-700 group-hover:scale-105" />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent" />
                      <div className="absolute left-3 top-3 inline-flex items-center gap-1 rounded-full border border-white/12 bg-black/30 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur-xl">
                        <ShieldCheck className="h-3 w-3 text-emerald-300" /> Verified
                      </div>
                    </div>
                    <div className="p-4 sm:p-5">
                      <p className="text-xs text-white/50">{listing.neighborhood || listing.city}</p>
                      <h3 className="mt-1 text-base font-semibold text-white sm:text-lg">{listing.title}</h3>
                      <div className="mt-2 flex items-center justify-between">
                        <p className="text-sm font-medium text-cyan-300">{price.toLocaleString()} XAF/mo</p>
                        <div className="flex items-center gap-1 text-xs text-white/60">
                          <Star className="h-3.5 w-3.5 text-amber-300" />
                          Trust score
                        </div>
                      </div>
                    </div>
                  </Link>
                );
              })}
            </div>
          ) : (
            <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-8 text-center sm:rounded-[28px] sm:p-12">
              <p className="text-white/60">New verified listings are added daily. Be the first to see them.</p>
              <Link href="/listings" className="mt-4 inline-block text-sm text-cyan-300 hover:text-cyan-200">
                Browse all listings →
              </Link>
            </div>
          )}
        </div>
      </section>

      <section className="px-4 pb-12 pt-6 sm:px-6 sm:pb-20 sm:pt-8 lg:px-8">
        <div className="mx-auto max-w-6xl rounded-2xl border border-white/10 bg-[linear-gradient(135deg,#0f172a_0%,#1d4ed8_62%,#7c3aed_100%)] px-5 py-8 text-center text-white shadow-[0_35px_100px_rgba(37,99,235,0.24)] sm:rounded-[36px] sm:px-10 sm:py-12 md:py-14">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-200 sm:text-sm">Start today</p>
          <h2 className="mx-auto mt-2 max-w-3xl text-2xl font-semibold tracking-tight sm:mt-3 sm:text-3xl md:text-4xl">
            Find your next home in {data.name}
          </h2>
          <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-blue-100/85 sm:mt-4 sm:text-base sm:leading-7">
            Stop paying frais de visite. Browse verified rentals with video tours and AI search, then schedule a visit when you're sure.
          </p>
          <div className="mt-6 flex flex-col justify-center gap-2.5 sm:mt-8 sm:flex-row sm:gap-3">
            <Link href={`/search?city=${encodeURIComponent(data.name)}`}>
              <Button size="lg" className="h-12 w-full rounded-full bg-white px-6 text-sm font-semibold text-slate-950 hover:bg-slate-200 sm:h-14 sm:w-auto sm:text-base">
                Search {data.name} Rentals
              </Button>
            </Link>
            <Link href="/register?role=LANDLORD">
              <Button size="lg" variant="outline" className="h-12 w-full rounded-full border-white/20 bg-white/10 px-6 text-sm text-white hover:bg-white/14 sm:h-14 sm:w-auto sm:text-base">
                List Your Property
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
