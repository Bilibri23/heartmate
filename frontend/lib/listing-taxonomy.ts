export type ListingPurpose = "RENT" | "SALE"
export type StayType = "SHORT_TERM" | "LONG_TERM" | "FLEXIBLE"
export type PricePeriod = "NIGHT" | "WEEK" | "MONTH" | "TOTAL"
export type FurnishingStatus = "FURNISHED" | "SEMI_FURNISHED" | "UNFURNISHED"

export type ListingTaxonomy = {
  listingPurpose?: ListingPurpose | string | null
  stayType?: StayType | string | null
  pricePeriod?: PricePeriod | string | null
  salePrice?: number | null
  rentAmount?: number | null
  furnishingStatus?: FurnishingStatus | string | null
  isSharedAccommodation?: boolean | null
  roommatesWanted?: boolean | null
  availableRoommateSlots?: number | null
  roomPreviewEnabled?: boolean | null
  roomPreviewStatus?: string | null
  propertyType?: string | null
}

export function propertyTypeLabel(value?: string | null, language: "en" | "fr" = "en") {
  if (!value) return null
  const labels: Record<string, { en: string; fr: string }> = {
    STUDIO: { en: "Studio", fr: "Studio" },
    APARTMENT: { en: "Apartment", fr: "Appartement" },
    HOUSE: { en: "House", fr: "Maison" },
    ROOM: { en: "Room", fr: "Chambre" },
    PRIVATE_ROOM: { en: "Private room", fr: "Chambre privée" },
    SHARED_ROOM: { en: "Shared room", fr: "Chambre partagée" },
  }
  return labels[value]?.[language] ?? value.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())
}

export function listingPurposeLabel(value?: string | null, language: "en" | "fr" = "en") {
  if (value === "SALE") return language === "fr" ? "À vendre" : "For sale"
  if (value === "RENT") return language === "fr" ? "À louer" : "For rent"
  return null
}

export function stayTypeLabel(value?: string | null, language: "en" | "fr" = "en") {
  if (value === "SHORT_TERM") return language === "fr" ? "Court séjour" : "Short stay"
  if (value === "LONG_TERM") return language === "fr" ? "Long terme" : "Long term"
  if (value === "FLEXIBLE") return language === "fr" ? "Flexible" : "Flexible"
  return null
}

export function furnishingLabel(value?: string | null, language: "en" | "fr" = "en") {
  if (value === "FURNISHED") return language === "fr" ? "Meublé" : "Furnished"
  if (value === "SEMI_FURNISHED") return language === "fr" ? "Semi-meublé" : "Semi-furnished"
  if (value === "UNFURNISHED") return language === "fr" ? "Non meublé" : "Unfurnished"
  return null
}

export function pricePeriodSuffix(value?: string | null, language: "en" | "fr" = "en") {
  switch (value) {
    case "NIGHT":
      return language === "fr" ? "/nuit" : "/night"
    case "WEEK":
      return language === "fr" ? "/sem." : "/week"
    case "TOTAL":
      return ""
    default:
      return language === "fr" ? "/mois" : "/mo"
  }
}

export function displayListingPrice(
  listing: ListingTaxonomy,
  formatCurrency: (amount: number) => string,
  language: "en" | "fr" = "en"
) {
  const isSale = listing.listingPurpose === "SALE"
  const amount = isSale ? listing.salePrice ?? listing.rentAmount : listing.rentAmount
  if (typeof amount !== "number") return language === "fr" ? "Prix non indiqué" : "Price not shown"
  const suffix = isSale ? "" : pricePeriodSuffix(listing.pricePeriod, language)
  return `${formatCurrency(amount)}${suffix}`
}

export function isSaleListing(listing?: ListingTaxonomy | null) {
  return listing?.listingPurpose === "SALE"
}

export function roomPreviewLive(listing?: ListingTaxonomy | null) {
  return Boolean(
    listing?.roomPreviewEnabled &&
      (listing.roomPreviewStatus === "APPROVED" || listing.roomPreviewStatus == null)
  )
}
