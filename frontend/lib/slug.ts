export function slugify(text: string): string {
  return text
    .toString()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/\s+/g, "-")
    .replace(/[^\w\-]+/g, "")
    .replace(/\-\-+/g, "-");
}

export function createListingSlug(title: string, city: string, id?: string): string {
  const base = slugify(`${title} ${city}`);
  return id ? `${base}-${id}` : base;
}

export function extractListingIdFromSlug(slug: string): string {
  const match = slug.match(/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
  return match?.[0] ?? slug;
}
