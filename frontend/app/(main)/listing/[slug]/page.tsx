import { notFound, redirect } from "next/navigation";
import { extractListingIdFromSlug } from "@/lib/slug";

export default async function ListingSlugRedirect({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;

  try {
    const listingId = extractListingIdFromSlug(slug);
    redirect(`/listings/${listingId}`);
  } catch {
    notFound();
  }
}
