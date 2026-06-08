"use client"

import Image from "next/image"
import Link from "next/link"
import { Heart, MapPin, Bed, Bath, CheckCircle, Star, Sparkles, Camera, Eye, Share2 } from "lucide-react"
import { useLanguage } from "@/context/language-context"
import { labelDiscoveryReasons } from "@/lib/discovery-reason-labels"
import { cn } from "@/lib/utils"
import { trustTierChipClassName } from "@/lib/listing-trust-tier"
import { useEffect, useRef, useState } from "react"

interface ListingCardProps {
  variant?: "discover" | "grid" | "compact"
  id: string
  title: string
  price: number
  city: string
  neighborhood?: string
  propertyType?: string
  bedrooms: number
  bathrooms: number
  imageUrl?: string
  images?: string[]
  videoUrl?: string
  isVerified?: boolean
  /** From API: HIGH | VERIFIED | REVIEWED | STANDARD */
  trustTier?: string | null
  isFeatured?: boolean
  isFavorited?: boolean
  rating?: number
  matchScore?: number
  status?: string | null
  isAvailable?: boolean | null
  /** Stable codes from API for “why this listing” chips */
  reasonCodes?: string[]
  aiInsight?: string
  roommateTags?: string[]
  roommateCompatibility?: number
  onFavoriteToggle?: (id: string) => void
}

export function ListingCard({
  variant = "grid",
  id,
  title,
  price,
  city,
  neighborhood,
  propertyType,
  bedrooms,
  bathrooms,
  imageUrl,
  images = [],
  videoUrl,
  isVerified = false,
  trustTier = null,
  isFeatured = false,
  isFavorited = false,
  rating,
  matchScore,
  status,
  isAvailable,
  reasonCodes,
  aiInsight,
  roommateTags = [],
  roommateCompatibility,
  onFavoriteToggle,
}: ListingCardProps) {
  const { formatCurrency, t, language } = useLanguage()
  const [favorited, setFavorited] = useState(isFavorited)
  const [imgIndex, setImgIndex] = useState(0)

  // Build gallery from imageUrl + images array
  const gallery = [
    ...(imageUrl ? [imageUrl] : []),
    ...images.filter(u => u !== imageUrl),
  ].filter(Boolean)

  const currentImg = gallery[imgIndex] || imageUrl

  const normalizedStatus = status?.toUpperCase?.()
  const trustChipClass = trustTierChipClassName(trustTier, "card")
  const trustLabel = (() => {
    switch (trustTier) {
      case "HIGH": return t.listings.trustTierHigh
      case "VERIFIED": return t.listings.trustTierVerified
      case "REVIEWED": return t.listings.trustTierReviewed
      default: return null
    }
  })()
  const trustTitle = (() => {
    switch (trustTier) {
      case "HIGH": return t.listings.trustTierTitleHigh
      case "VERIFIED": return t.listings.trustTierTitleVerified
      case "REVIEWED": return t.listings.trustTierTitleReviewed
      default: return null
    }
  })()
  const isUnavailable =
    typeof isAvailable === "boolean"
      ? !isAvailable
      : Boolean(normalizedStatus && normalizedStatus !== "ACTIVE")

  const handleFavorite = (e: React.MouseEvent) => {
    e.preventDefault(); e.stopPropagation()
    setFavorited(f => !f)
    onFavoriteToggle?.(id)
  }

  const cyclePhoto = (dir: 1 | -1) => (e: React.MouseEvent) => {
    e.preventDefault(); e.stopPropagation()
    if (gallery.length <= 1) return
    setImgIndex(i => (i + dir + gallery.length) % gallery.length)
  }

  const reasonLabels = labelDiscoveryReasons(reasonCodes, language === "fr" ? "fr" : "en").slice(0, 2)
  const listingTypeLabel = propertyType
    ? propertyType.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())
    : null
  const locationLabel = neighborhood ? `${neighborhood}, ${city}` : city
  const insight =
    aiInsight ||
    reasonLabels[0] ||
    (roommateTags.length > 0 ? "Good for shared accommodation" : null) ||
    (isVerified ? "Verified owner" : null) ||
    (isFeatured ? `Popular in ${city}` : null)
  const ariaListing =
    reasonLabels.length > 0
      ? `${title}. ${reasonLabels.join(". ")}. ${formatCurrency(price)} per month.`
      : `${title}. ${formatCurrency(price)} per month.`
  const listingHref = `/listings/${id}`
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const cardRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (variant !== "discover" || !videoRef.current || !cardRef.current) return
    const video = videoRef.current
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && entry.intersectionRatio > 0.6) {
          video.play().catch(() => undefined)
        } else {
          video.pause()
        }
      },
      { threshold: [0, 0.6, 1] }
    )
    observer.observe(cardRef.current)
    return () => observer.disconnect()
  }, [variant, videoUrl])

  if (variant === "compact") {
    return (
      <Link href={listingHref} className="group block" aria-label={ariaListing}>
        <article className={cn(
          "flex gap-3 rounded-2xl border border-slate-200 bg-white p-2.5 shadow-sm transition-all",
          "active:scale-[0.99] hover:border-blue-200 hover:shadow-md",
          isUnavailable && "opacity-70"
        )}>
          <div className="relative h-28 w-32 shrink-0 overflow-hidden rounded-xl bg-slate-100">
            {currentImg ? (
              <Image src={currentImg} alt={title} fill className="object-cover" sizes="128px" />
            ) : (
              <div className="flex h-full items-center justify-center text-slate-400">
                <Camera className="h-6 w-6" />
              </div>
            )}
            {isVerified && (
              <span className="absolute left-2 top-2 rounded-full bg-emerald-500 px-2 py-0.5 text-[10px] font-bold text-white">
                Verified
              </span>
            )}
          </div>
          <div className="min-w-0 flex-1 py-1">
            <div className="flex items-start gap-2">
              <div className="min-w-0 flex-1">
                <p className="text-base font-extrabold leading-tight text-slate-950">{formatCurrency(price)}</p>
                <h3 className="mt-1 line-clamp-1 text-sm font-semibold text-slate-900">{title}</h3>
              </div>
              <button
                onClick={handleFavorite}
                className={cn(
                  "flex h-9 w-9 shrink-0 items-center justify-center rounded-full border transition-colors",
                  favorited ? "border-red-100 bg-red-50 text-red-500" : "border-slate-200 bg-white text-slate-500"
                )}
                aria-label={favorited ? "Remove from saved listings" : "Save listing"}
              >
                <Heart className={cn("h-4 w-4", favorited && "fill-current")} />
              </button>
            </div>
            <p className="mt-1 flex items-center gap-1 text-xs text-slate-500">
              <MapPin className="h-3 w-3 shrink-0" />
              <span className="truncate">{locationLabel}</span>
            </p>
            <div className="mt-2 flex flex-wrap gap-1.5 text-[11px] font-medium text-slate-600">
              {listingTypeLabel && <span className="rounded-full bg-slate-100 px-2 py-0.5">{listingTypeLabel}</span>}
              <span className="rounded-full bg-slate-100 px-2 py-0.5">{bedrooms} bed</span>
              <span className="rounded-full bg-slate-100 px-2 py-0.5">{bathrooms} bath</span>
              {roommateCompatibility != null && (
                <span className="rounded-full bg-violet-50 px-2 py-0.5 text-violet-700">{roommateCompatibility}% roommate fit</span>
              )}
            </div>
            {insight && (
              <p className="mt-2 line-clamp-1 text-xs font-medium text-blue-700">
                <Sparkles className="mr-1 inline h-3 w-3" />
                {insight}
              </p>
            )}
          </div>
        </article>
      </Link>
    )
  }

  if (variant === "grid") {
    return (
      <Link href={listingHref} className="group block" aria-label={ariaListing}>
        <article className={cn(
          "overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition-all",
          "hover:border-blue-200 hover:shadow-lg active:scale-[0.99]",
          isUnavailable && "opacity-70"
        )}>
          <div className="relative aspect-video overflow-hidden bg-slate-100">
            {currentImg ? (
              <Image
                src={currentImg}
                alt={title}
                fill
                className="object-cover transition-transform duration-500 group-hover:scale-105"
                sizes="(max-width: 768px) 100vw, 50vw"
              />
            ) : (
              <div className="flex h-full items-center justify-center text-slate-400">
                <Camera className="h-8 w-8" />
              </div>
            )}
            <div className="absolute inset-x-0 top-0 flex items-start justify-between p-2.5">
              <div className="flex flex-wrap gap-1.5">
                {isFeatured && <span className="rounded-full bg-amber-400 px-2 py-1 text-[10px] font-bold text-white">Featured</span>}
                {trustLabel && trustChipClass && (
                  <span title={trustTitle ?? undefined} className={trustChipClass}>
                    <CheckCircle className="h-3 w-3" /> {trustLabel}
                  </span>
                )}
                {!trustLabel && isVerified && (
                  <span className="flex items-center gap-1 rounded-full bg-emerald-500/90 px-2 py-1 text-[10px] font-bold text-white">
                    <CheckCircle className="h-3 w-3" /> {t.listings.verified}
                  </span>
                )}
              </div>
              <button
                onClick={handleFavorite}
                className={cn(
                  "flex h-9 w-9 items-center justify-center rounded-full backdrop-blur transition-all active:scale-95",
                  favorited ? "bg-red-500 text-white" : "bg-black/35 text-white"
                )}
                aria-label={favorited ? "Remove from saved listings" : "Save listing"}
              >
                <Heart className={cn("h-4 w-4", favorited && "fill-current")} />
              </button>
            </div>
          </div>
          <div className="space-y-2 p-3">
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <p className="text-lg font-extrabold leading-tight text-slate-950">{formatCurrency(price)}</p>
                <h3 className="mt-1 line-clamp-1 text-sm font-semibold text-slate-900">{title}</h3>
              </div>
              {matchScore != null && matchScore > 0 && (
                <span className="shrink-0 rounded-full bg-blue-50 px-2 py-1 text-[11px] font-bold text-blue-700">
                  {matchScore}% match
                </span>
              )}
            </div>
            <p className="flex items-center gap-1 text-xs text-slate-500">
              <MapPin className="h-3.5 w-3.5 shrink-0 text-blue-500" />
              <span className="truncate">{locationLabel}</span>
            </p>
            <div className="flex items-center justify-between gap-2">
              <div className="flex min-w-0 items-center gap-2 text-xs text-slate-600">
                {listingTypeLabel && <span className="truncate">{listingTypeLabel}</span>}
                <span className="flex items-center gap-1"><Bed className="h-3.5 w-3.5" /> {bedrooms}</span>
                <span className="flex items-center gap-1"><Bath className="h-3.5 w-3.5" /> {bathrooms}</span>
              </div>
              <span className={cn(
                "rounded-full px-2 py-1 text-[10px] font-bold",
                isUnavailable ? "bg-rose-50 text-rose-600" : "bg-emerald-50 text-emerald-700"
              )}>
                {isUnavailable ? "Unavailable" : "Available"}
              </span>
            </div>
            {(insight || roommateTags.length > 0) && (
              <div className="flex gap-1.5 overflow-hidden">
                {insight && (
                  <span className="inline-flex min-w-0 items-center gap-1 rounded-full bg-blue-50 px-2 py-1 text-[11px] font-semibold text-blue-700">
                    <Sparkles className="h-3 w-3 shrink-0" />
                    <span className="truncate">{insight}</span>
                  </span>
                )}
                {roommateTags.slice(0, 1).map((tag) => (
                  <span key={tag} className="shrink-0 rounded-full bg-violet-50 px-2 py-1 text-[11px] font-semibold text-violet-700">
                    {tag}
                  </span>
                ))}
              </div>
            )}
          </div>
        </article>
      </Link>
    )
  }

  return (
    <Link href={listingHref} className="group block snap-center" aria-label={ariaListing}>
      <div className={cn(
        "relative overflow-hidden rounded-[1.75rem] transition-all duration-300 min-h-[70dvh] max-h-[85dvh]",
        "hover:shadow-2xl motion-safe:hover:-translate-y-1 motion-reduce:hover:translate-y-0 active:scale-[0.98] motion-reduce:active:scale-100",
        isUnavailable && "opacity-70"
      )} ref={cardRef}>
        {/* ── Cinematic photo ── */}
        <div className="relative h-[76dvh] overflow-hidden bg-gradient-to-br from-slate-200 to-slate-300">

          {videoUrl ? (
            <video
              ref={videoRef}
              src={videoUrl}
              className="absolute inset-0 h-full w-full object-cover"
              muted
              loop
              playsInline
              preload="metadata"
            />
          ) : currentImg ? (
            <Image
              src={currentImg}
              alt={title}
              fill
              className="object-cover transition-transform duration-700 motion-safe:group-hover:scale-105"
              sizes="(max-width: 768px) 100vw, 50vw"
            />
          ) : (
            <div className="flex h-full items-center justify-center">
              <span className="text-6xl">🏠</span>
            </div>
          )}

          {/* Bottom gradient overlay */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/35 to-black/10" />

          {/* ── Top row: badges ── */}
          <div className="absolute top-3 left-3 right-3 flex items-start justify-between">
            {/* Left: status + featured */}
            <div className="flex flex-col gap-1.5">
              {isUnavailable && (
                <span className="rounded-full bg-rose-500 px-2.5 py-1 text-[10px] font-bold text-white tracking-wide">
                  RENTED
                </span>
              )}
              {isFeatured && (
                <span className="rounded-full bg-gradient-to-r from-amber-400 to-orange-400 px-2.5 py-1 text-[10px] font-bold text-white tracking-wide">
                  ⭐ FEATURED
                </span>
              )}
              {trustLabel && trustChipClass && (
                <span title={trustTitle ?? undefined} className={trustChipClass}>
                  <CheckCircle className="h-3 w-3" /> {trustLabel}
                </span>
              )}
              {!trustLabel && isVerified && (
                <span className="flex items-center gap-1 rounded-full bg-emerald-500/90 backdrop-blur-sm px-2.5 py-1 text-[10px] font-bold text-white">
                  <CheckCircle className="h-3 w-3" /> {t.listings.verified}
                </span>
              )}
            </div>

            {/* Right: match score */}
            {matchScore != null && matchScore > 0 && (
              <div className={cn(
                "flex items-center gap-1 rounded-2xl px-2.5 py-1.5 shadow-lg backdrop-blur-sm",
                matchScore >= 80
                  ? "bg-emerald-500/90 animate-pulse"
                  : matchScore >= 60
                    ? "bg-blue-500/90"
                    : "bg-amber-500/90"
              )}>
                <Sparkles className="h-3 w-3 text-white" />
                <span className="text-xs font-bold text-white">{matchScore}%</span>
              </div>
            )}
          </div>

          {/* ── Photo navigation dots & arrows ── */}
          {gallery.length > 1 && (
            <>
              {/* Prev/Next tap zones */}
              <button
                onClick={cyclePhoto(-1)}
                className="absolute left-0 top-0 bottom-0 w-1/3 opacity-0"
                aria-label="Previous photo"
              />
              <button
                onClick={cyclePhoto(1)}
                className="absolute right-0 top-0 bottom-0 w-1/3 opacity-0"
                aria-label="Next photo"
              />

              {/* Dot indicators */}
              <div className="absolute top-3 left-1/2 -translate-x-1/2 flex gap-1">
                {gallery.slice(0, 5).map((_, i) => (
                  <div key={i} className={cn(
                    "h-1 rounded-full transition-all duration-300",
                    i === imgIndex ? "w-4 bg-white" : "w-1.5 bg-white/50"
                  )} />
                ))}
                {gallery.length > 5 && (
                  <span className="text-white/70 text-[9px] font-medium">+{gallery.length - 5}</span>
                )}
              </div>

              {/* Photo count chip */}
              <div className="absolute bottom-20 right-3 flex items-center gap-1 bg-black/40 backdrop-blur-sm rounded-full px-2 py-1">
                <Camera className="h-3 w-3 text-white/80" />
                <span className="text-[10px] text-white/80 font-medium">{gallery.length}</span>
              </div>
            </>
          )}

          {/* ── Favourite button ── */}
          <button
            onClick={handleFavorite}
            className={cn(
              "absolute bottom-20 left-3 h-9 w-9 flex items-center justify-center rounded-full",
              "backdrop-blur-sm transition-all duration-200 hover:scale-110 active:scale-95",
              favorited
                ? "bg-red-500 shadow-lg shadow-red-500/40"
                : "bg-black/30 hover:bg-black/50"
            )}
          >
            <Heart className={cn(
              "h-4 w-4 transition-all",
              favorited ? "fill-white text-white" : "text-white"
            )} />
          </button>

          {/* ── Bottom info overlay ── */}
          <div className="absolute right-3 bottom-28 flex flex-col gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-full bg-black/35 text-white backdrop-blur">
              <Eye className="h-5 w-5" />
            </span>
            <button
              onClick={(e) => {
                e.preventDefault()
                e.stopPropagation()
                navigator.share?.({ title, url: `${window.location.origin}${listingHref}` }).catch(() => undefined)
              }}
              className="flex h-11 w-11 items-center justify-center rounded-full bg-black/35 text-white backdrop-blur"
              aria-label="Share listing"
            >
              <Share2 className="h-5 w-5" />
            </button>
          </div>

          <div className="absolute bottom-0 left-0 right-14 p-4 pb-5">
            {insight && (
              <span className="mb-3 inline-flex max-w-full items-center gap-1.5 rounded-full border border-white/20 bg-white/18 px-3 py-1.5 text-xs font-semibold text-white shadow-lg backdrop-blur-md">
                <Sparkles className="h-3.5 w-3.5 shrink-0 text-cyan-200" />
                <span className="truncate">{insight}</span>
              </span>
            )}
            {/* Price */}
            <div className="flex items-baseline gap-1.5 mb-1">
              <span className="text-3xl font-extrabold text-white leading-none">
                {formatCurrency(price)}
              </span>
              <span className="text-white/70 text-xs font-medium">/mo</span>
              {rating && rating > 0 && (
                <div className="ml-auto flex items-center gap-1 bg-white/20 backdrop-blur-sm rounded-full px-2 py-0.5">
                  <Star className="h-3 w-3 fill-amber-400 text-amber-400" />
                  <span className="text-white text-xs font-semibold">{rating.toFixed(1)}</span>
                </div>
              )}
            </div>

            {/* Title */}
            <h3 className="text-white font-bold text-xl line-clamp-1 mb-1.5">
              {title}
            </h3>

            {reasonLabels.length > 0 && (
              <div className="flex flex-wrap gap-1 mb-1.5" aria-label={t.discovery.whyPicked}>
                {reasonLabels.map((lbl) => (
                  <span
                    key={lbl}
                    className="text-[9px] font-medium px-1.5 py-0.5 rounded-md bg-white/20 text-white/95 max-w-[9rem] truncate"
                  >
                    {lbl}
                  </span>
                ))}
              </div>
            )}

            {/* Location + bed/bath */}
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-1 text-white/80 text-xs">
                <MapPin className="h-3 w-3 shrink-0" />
                <span className="line-clamp-1">
                  {locationLabel}
                </span>
              </div>
              <div className="flex items-center gap-2 text-white/80 text-xs shrink-0 ml-2">
                <span className="flex items-center gap-1">
                  <Bed className="h-3 w-3" /> {bedrooms}
                </span>
                <span className="flex items-center gap-1">
                  <Bath className="h-3 w-3" /> {bathrooms}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Link>
  )
}
