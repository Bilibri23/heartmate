"use client"

import Image from "next/image"
import Link from "next/link"
import { Heart, MapPin, Bed, Bath, CheckCircle, Star } from "lucide-react"
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
  onFavoriteToggle,
}: ListingCardProps) {
  const { formatCurrency, t } = useLanguage()
  const [favorited, setFavorited] = useState(isFavorited)

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

          {/* Badges */}
          <div className="absolute left-2 top-2 flex flex-wrap gap-1">
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
            {matchScore && matchScore > 0 && (
              <span className="rounded-full bg-blue-500 px-2 py-0.5 text-[10px] font-semibold text-white">
                {matchScore}% match
              </span>
            )}
          </div>

          {/* Favorite button */}
          <button
            onClick={handleFavorite}
            className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 shadow-sm transition-transform hover:scale-110 active:scale-95"
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
            {rating && (
              <div className="ml-auto flex items-center gap-1">
                <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                <span>{rating.toFixed(1)}</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </Link>
  )
}
