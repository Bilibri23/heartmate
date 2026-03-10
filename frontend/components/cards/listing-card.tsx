"use client"

import Image from "next/image"
import Link from "next/link"
import { Heart, MapPin, Bed, Bath, CheckCircle, Star, Sparkles, Camera } from "lucide-react"
import { useLanguage } from "@/context/language-context"
import { cn } from "@/lib/utils"
import { useState } from "react"

interface ListingCardProps {
  id: string
  title: string
  price: number
  city: string
  neighborhood?: string
  bedrooms: number
  bathrooms: number
  imageUrl?: string
  images?: string[]
  isVerified?: boolean
  isFeatured?: boolean
  isFavorited?: boolean
  rating?: number
  matchScore?: number
  status?: string | null
  isAvailable?: boolean | null
  onFavoriteToggle?: (id: string) => void
}

export function ListingCard({
  id,
  title,
  price,
  city,
  neighborhood,
  bedrooms,
  bathrooms,
  imageUrl,
  images = [],
  isVerified = false,
  isFeatured = false,
  isFavorited = false,
  rating,
  matchScore,
  status,
  isAvailable,
  onFavoriteToggle,
}: ListingCardProps) {
  const { formatCurrency, t } = useLanguage()
  const [favorited, setFavorited] = useState(isFavorited)
  const [imgIndex, setImgIndex] = useState(0)

  // Build gallery from imageUrl + images array
  const gallery = [
    ...(imageUrl ? [imageUrl] : []),
    ...images.filter(u => u !== imageUrl),
  ].filter(Boolean)

  const currentImg = gallery[imgIndex] || imageUrl

  const normalizedStatus = status?.toUpperCase?.()
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

  return (
    <Link href={`/listings/${id}`} className="group block">
      <div className={cn(
        "relative overflow-hidden rounded-3xl transition-all duration-300",
        "hover:shadow-2xl hover:-translate-y-1 active:scale-[0.98]",
        isUnavailable && "opacity-70"
      )}>
        {/* ── Cinematic photo ── */}
        <div className="relative aspect-[3/4] overflow-hidden bg-gradient-to-br from-slate-200 to-slate-300">

          {currentImg ? (
            <Image
              src={currentImg}
              alt={title}
              fill
              className="object-cover transition-transform duration-700 group-hover:scale-105"
              sizes="(max-width: 768px) 100vw, 50vw"
            />
          ) : (
            <div className="flex h-full items-center justify-center">
              <span className="text-6xl">🏠</span>
            </div>
          )}

          {/* Bottom gradient overlay */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

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
              {isVerified && (
                <span className="flex items-center gap-1 rounded-full bg-emerald-500/90 backdrop-blur-sm px-2.5 py-1 text-[10px] font-bold text-white">
                  <CheckCircle className="h-3 w-3" /> VERIFIED
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
          <div className="absolute bottom-0 left-0 right-0 p-4">
            {/* Price */}
            <div className="flex items-baseline gap-1.5 mb-1">
              <span className="text-2xl font-extrabold text-white leading-none">
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
            <h3 className="text-white font-semibold text-sm line-clamp-1 mb-1.5">
              {title}
            </h3>

            {/* Location + bed/bath */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1 text-white/80 text-xs">
                <MapPin className="h-3 w-3 shrink-0" />
                <span className="line-clamp-1">
                  {neighborhood ? `${neighborhood}, ${city}` : city}
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
