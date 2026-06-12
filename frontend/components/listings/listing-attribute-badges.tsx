"use client"

import { Users, WandSparkles } from "lucide-react"
import {
  furnishingLabel,
  listingPurposeLabel,
  propertyTypeLabel,
  roomPreviewLive,
  stayTypeLabel,
  type ListingTaxonomy,
} from "@/lib/listing-taxonomy"
import { cn } from "@/lib/utils"

type ListingAttributeBadgesProps = {
  listing: ListingTaxonomy
  language?: "en" | "fr"
  size?: "sm" | "md"
  className?: string
  maxBadges?: number
}

const badgeClass = (size: "sm" | "md") =>
  size === "sm"
    ? "rounded-full px-2 py-0.5 text-[10px] font-semibold"
    : "rounded-full px-2.5 py-1 text-[11px] font-semibold"

export function ListingAttributeBadges({
  listing,
  language = "en",
  size = "sm",
  className,
  maxBadges = 6,
}: ListingAttributeBadgesProps) {
  const badges: { key: string; label: string; className: string; icon?: React.ReactNode }[] = []

  const purpose = listingPurposeLabel(listing.listingPurpose, language)
  if (purpose) {
    badges.push({
      key: "purpose",
      label: purpose,
      className: listing.listingPurpose === "SALE" ? "bg-amber-50 text-amber-800" : "bg-blue-50 text-blue-800",
    })
  }

  const type = propertyTypeLabel(listing.propertyType, language)
  if (type) {
    badges.push({ key: "type", label: type, className: "bg-slate-100 text-slate-700" })
  }

  const stay = stayTypeLabel(listing.stayType, language)
  if (stay && listing.listingPurpose !== "SALE") {
    badges.push({ key: "stay", label: stay, className: "bg-indigo-50 text-indigo-700" })
  }

  const furnishing = furnishingLabel(listing.furnishingStatus, language)
  if (furnishing) {
    badges.push({ key: "furnishing", label: furnishing, className: "bg-emerald-50 text-emerald-700" })
  }

  if (listing.isSharedAccommodation || listing.roommatesWanted) {
    const slots =
      typeof listing.availableRoommateSlots === "number" && listing.availableRoommateSlots > 0
        ? ` · ${listing.availableRoommateSlots} ${language === "fr" ? "places" : "slots"}`
        : ""
    badges.push({
      key: "shared",
      label: `${language === "fr" ? "Colocation" : "Shared"}${slots}`,
      className: "bg-violet-50 text-violet-700",
      icon: <Users className="h-3 w-3" />,
    })
  }

  if (roomPreviewLive(listing)) {
    badges.push({
      key: "preview",
      label: language === "fr" ? "Aperçu chambre" : "Room Preview",
      className: "bg-sky-50 text-sky-700",
      icon: <WandSparkles className="h-3 w-3" />,
    })
  }

  return (
    <div className={cn("flex flex-wrap gap-1.5", className)}>
      {badges.slice(0, maxBadges).map((badge) => (
        <span
          key={badge.key}
          className={cn(
            "inline-flex items-center gap-1",
            badgeClass(size),
            badge.className
          )}
        >
          {badge.icon}
          {badge.label}
        </span>
      ))}
    </div>
  )
}
