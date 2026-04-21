/** Stable API codes from backend (feed + similar listings). */

const EN: Record<string, string> = {
  // Recommendation feed
  WITHIN_BUDGET: "Within your budget",
  PREFERRED_LOCATION: "Preferred area",
  NEAR_UNIVERSITY: "Near university",
  PREFERRED_PROPERTY_TYPE: "Your property type",
  SIMILAR_TO_VIEWED: "Like listings you viewed",
  SIMILAR_STUDENTS_LOVED: "Popular with similar students",
  SIMILAR_STUDENTS_POPULAR: "Trending with peers",
  KEY_AMENITIES: "Key amenities",
  FEATURED: "Featured",
  VERIFIED_ADMIN: "Verified",
  POPULAR_INVENTORY: "Popular picks",
  PERSONALIZED_FEED: "Picked for you",
  // Similar / comps
  SAME_NEIGHBORHOOD: "Same neighborhood",
  SAME_CITY: "Same city",
  SAME_PROPERTY_TYPE: "Same type",
  SAME_BEDROOMS: "Same bedrooms",
  SIMILAR_BEDROOMS: "Similar size",
  SIMILAR_RENT: "Similar rent",
  CLOSE_RENT: "Close rent",
  MARKET_COMPARABLE: "Market comparable",
  ACTIVE_NEAR_MARKET: "Active nearby",
  TRENDING_VIEWS: "Trending",
  NEW_LISTING: "New listing",
  BROWSE_POPULAR: "Popular now",
}

const FR: Record<string, string> = {
  WITHIN_BUDGET: "Dans votre budget",
  PREFERRED_LOCATION: "Zone préférée",
  NEAR_UNIVERSITY: "Près de l’université",
  PREFERRED_PROPERTY_TYPE: "Votre type de logement",
  SIMILAR_TO_VIEWED: "Comme les annonces vues",
  SIMILAR_STUDENTS_LOVED: "Plébiscité par des étudiants proches",
  SIMILAR_STUDENTS_POPULAR: "Tendance chez vos pairs",
  KEY_AMENITIES: "Équipements clés",
  FEATURED: "À la une",
  VERIFIED_ADMIN: "Vérifié",
  POPULAR_INVENTORY: "Sélection populaire",
  PERSONALIZED_FEED: "Pour vous",
  SAME_NEIGHBORHOOD: "Même quartier",
  SAME_CITY: "Même ville",
  SAME_PROPERTY_TYPE: "Même type",
  SAME_BEDROOMS: "Même nombre de chambres",
  SIMILAR_BEDROOMS: "Taille proche",
  SIMILAR_RENT: "Loyer proche",
  CLOSE_RENT: "Loyer comparable",
  MARKET_COMPARABLE: "Comparable au marché",
  ACTIVE_NEAR_MARKET: "Actif à proximité",
  TRENDING_VIEWS: "Tendance",
  NEW_LISTING: "Nouvelle annonce",
  BROWSE_POPULAR: "Populaire",
}

export function labelDiscoveryReason(code: string, lang: "en" | "fr"): string {
  const table = lang === "fr" ? FR : EN
  const hit = table[code]
  if (hit) return hit
  return code.replace(/_/g, " ").toLowerCase()
}

export function labelDiscoveryReasons(codes: string[] | undefined | null, lang: "en" | "fr"): string[] {
  if (!codes?.length) return []
  return codes.map((c) => labelDiscoveryReason(c, lang))
}
