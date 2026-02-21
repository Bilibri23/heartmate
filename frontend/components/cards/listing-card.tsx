"use client"

import Image from "next/image"
import Link from "next/link"
import { Heart, MapPin, Bed, Bath, CheckCircle, Star, Sparkles } from "lucide-react"
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
  
  // Debug logging for match score
  console.log(`ListingCard "${title}":`, {
    matchScore,
    matchScoreType: typeof matchScore,
    matchScoreNull: matchScore == null,
    matchScoreZero: matchScore === 0
  })

  const normalizedStatus = status?.toUpperCase?.()
  const availabilityLabel =
    normalizedStatus === "ACTIVE" ? "Available" :
      normalizedStatus === "RENTED" ? "Rented" :
        status ? "Unavailable" : null
  const isUnavailable =
    typeof isAvailable === "boolean"
      ? !isAvailable
      : Boolean(normalizedStatus && normalizedStatus !== "ACTIVE")
  const showAvailability = availabilityLabel !== null || typeof isAvailable === "boolean"

  const handleFavorite = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setFavorited(!favorited)
    onFavoriteToggle?.(id)
  }

  return (
    <Link href={`/listings/${id}`}>
      <div className="group relative overflow-hidden rounded-2xl bg-white shadow-sm border border-slate-100 transition-all hover:shadow-md active:scale-[0.98]">
        {/* Image */}
        <div className="relative aspect-[4/3] overflow-hidden bg-slate-100">
          {imageUrl ? (
            <Image
              src={imageUrl}
              alt={title}
              fill
              className="object-cover transition-transform group-hover:scale-105"
              sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
            />
          ) : (
            <div className="flex h-full items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200">
              <span className="text-4xl">🏠</span>
            </div>
          )}

          {/* Match Score Badge — prominent, top-right */}
          {matchScore != null && (
            <div className={cn(
              "absolute right-2 top-2 z-10 flex items-center gap-1 rounded-xl px-2.5 py-1.5 shadow-lg",
              matchScore >= 80
                ? "bg-gradient-to-r from-emerald-500 to-green-500 animate-pulse"
                : matchScore >= 60
                  ? "bg-gradient-to-r from-blue-500 to-cyan-500"
                  : "bg-gradient-to-r from-amber-500 to-orange-500"
            )}>
              <Sparkles className="h-3.5 w-3.5 text-white" />
              <span className="text-sm font-bold text-white">{matchScore}%</span>
            </div>
          )}

          {/* Status & Verification Badges — left side */}
          <div className="absolute left-2 top-2 flex flex-wrap gap-1">
            {showAvailability && (
              <span
                className={`rounded-full px-2 py-0.5 text-[10px] font-semibold text-white ${isUnavailable ? "bg-rose-500" : "bg-emerald-500"
                  }`}
              >
                {availabilityLabel || (isUnavailable ? "Unavailable" : "Available")}
              </span>
            )}
            {isFeatured && (
              <span className="rounded-full bg-amber-500 px-2 py-0.5 text-[10px] font-semibold text-white">
                {t.listings.featured}
              </span>
            )}
            {isVerified && (
              <span className="flex items-center gap-0.5 rounded-full bg-emerald-500 px-2 py-0.5 text-[10px] font-semibold text-white">
                <CheckCircle className="h-3 w-3" />
                {t.listings.verified}
              </span>
            )}
          </div>

          {/* Favorite button — below match score or top-right if no score */}
          <button
            onClick={handleFavorite}
            className={cn(
              "absolute flex h-8 w-8 items-center justify-center rounded-full bg-white/90 shadow-sm transition-transform hover:scale-110 active:scale-95",
              matchScore != null && matchScore > 0 ? "right-2 top-12" : "right-2 top-2"
            )}
          >
            <Heart
              className={cn(
                "h-4 w-4 transition-colors",
                favorited ? "fill-red-500 text-red-500" : "text-slate-600"
              )}
            />
          </button>
        </div>

        {/* Content */}
        <div className="p-3">
          {/* Price */}
          <div className="mb-1 flex items-baseline justify-between">
            <span className="text-lg font-bold text-slate-900">
              {formatCurrency(price)}
            </span>
            <span className="text-xs text-slate-500">{t.listings.perMonth}</span>
          </div>

          {/* Title */}
          <h3 className="mb-1 line-clamp-1 text-sm font-medium text-slate-800">
            {title}
          </h3>

          {/* Location */}
          <div className="mb-2 flex items-center gap-1 text-xs text-slate-500">
            <MapPin className="h-3 w-3" />
            <span className="line-clamp-1">
              {neighborhood ? `${neighborhood}, ${city}` : city}
            </span>
          </div>

          {/* Details */}
          <div className="flex items-center gap-3 text-xs text-slate-600">
            <div className="flex items-center gap-1">
              <Bed className="h-3.5 w-3.5" />
              <span>{bedrooms}</span>
            </div>
            <div className="flex items-center gap-1">
              <Bath className="h-3.5 w-3.5" />
              <span>{bathrooms}</span>
            </div>
            {rating && rating > 0 ? (
              <div className="ml-auto flex items-center gap-1">
                <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                <span className="font-medium">{rating.toFixed(1)}</span>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </Link>
  )
}
