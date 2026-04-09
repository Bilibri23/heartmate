"use client"

import { useState, useRef, useEffect, useCallback } from "react"
import Image from "next/image"
import Link from "next/link"
import {
    Heart, MapPin, Bed, Bath, CheckCircle, Sparkles,
    Share2, MessageSquare, ChevronUp, ChevronDown, X,
    Camera, Play, Pause, Volume2, VolumeX,
} from "lucide-react"
import { useLanguage } from "@/context/language-context"
import { cn } from "@/lib/utils"
import { trustTierChipClassName } from "@/lib/listing-trust-tier"

// Uses API field names directly (rentAmount not price)
interface ReelListing {
    id: string
    title: string
    rentAmount: number          // ← matches backend/API field
    city: string
    neighborhood?: string
    bedrooms: number
    bathrooms: number
    photos?: { photoUrl: string; isPrimary: boolean }[]
    videoTourUrl?: string        // self-hosted Cloudinary MP4
    virtualTourProvider?: string // 'video' | '360' | legacy
    isVerified?: boolean
    trustTier?: string | null
    isFeatured?: boolean
    isFavorited?: boolean
    matchScore?: number
    description?: string
    amenities?: string[]
}

interface ReelsFeedProps {
    listings: ReelListing[]
    onFavoriteToggle?: (id: string) => void
    onClose?: () => void
}

export function ReelsFeed({ listings, onFavoriteToggle, onClose }: ReelsFeedProps) {
    const { formatCurrency, t } = useLanguage()
    const [currentIndex, setCurrentIndex] = useState(0)
    const [favorited, setFavorited] = useState<Record<string, boolean>>(
        Object.fromEntries(listings.map(l => [l.id, l.isFavorited ?? false]))
    )
    const [imgIndex, setImgIndex] = useState(0)
    const [showDesc, setShowDesc] = useState(false)
    const [isPlaying, setIsPlaying] = useState(true)
    const [isMuted, setIsMuted] = useState(true)
    const videoRef = useRef<HTMLVideoElement>(null)
    const touchStartY = useRef<number | null>(null)
    const isAnimating = useRef(false)

    const listing = listings[currentIndex]

    // ── Gallery ───────────────────────────────────────────────────────────────
    const gallery = listing?.photos
        ?.sort((a, b) => (b.isPrimary ? 1 : 0) - (a.isPrimary ? 1 : 0))
        .map(p => p.photoUrl) ?? []
    const mainImg = gallery[imgIndex] || null

    // Show video when available; otherwise fall back to photos
    const hasVideo = !!(listing?.videoTourUrl) &&
        (listing?.virtualTourProvider === 'video' || listing?.virtualTourProvider === undefined)

    // ── Navigation ─────────────────────────────────────────────────────────
    const goTo = useCallback((idx: number) => {
        if (isAnimating.current || idx < 0 || idx >= listings.length) return
        isAnimating.current = true
        setCurrentIndex(idx)
        setImgIndex(0)
        setShowDesc(false)
        setIsPlaying(true)
        setTimeout(() => { isAnimating.current = false }, 350)
    }, [listings.length])

    const goNext = useCallback(() => goTo(currentIndex + 1), [currentIndex, goTo])
    const goPrev = useCallback(() => goTo(currentIndex - 1), [currentIndex, goTo])

    // Keyboard
    useEffect(() => {
        const handler = (e: KeyboardEvent) => {
            if (e.key === "ArrowDown") goNext()
            if (e.key === "ArrowUp") goPrev()
            if (e.key === "Escape") onClose?.()
        }
        window.addEventListener("keydown", handler)
        return () => window.removeEventListener("keydown", handler)
    }, [goNext, goPrev, onClose])

    // Touch swipe
    const onTouchStart = (e: React.TouchEvent) => { touchStartY.current = e.touches[0].clientY }
    const onTouchEnd = (e: React.TouchEvent) => {
        if (touchStartY.current === null) return
        const diff = touchStartY.current - e.changedTouches[0].clientY
        if (diff > 60) goNext()
        if (diff < -60) goPrev()
        touchStartY.current = null
    }

    // Autoplay video on listing change
    useEffect(() => {
        if (videoRef.current) {
            videoRef.current.currentTime = 0
            if (isPlaying) videoRef.current.play().catch(() => { })
            else videoRef.current.pause()
        }
    }, [currentIndex, isPlaying])

    const togglePlay = () => {
        if (!videoRef.current) return
        if (videoRef.current.paused) { videoRef.current.play(); setIsPlaying(true) }
        else { videoRef.current.pause(); setIsPlaying(false) }
    }

    const cyclePhoto = (dir: 1 | -1) => (e: React.MouseEvent) => {
        e.stopPropagation()
        if (gallery.length <= 1) return
        setImgIndex(i => (i + dir + gallery.length) % gallery.length)
    }

    const handleFavorite = (e: React.MouseEvent) => {
        e.stopPropagation()
        setFavorited(prev => ({ ...prev, [listing.id]: !prev[listing.id] }))
        onFavoriteToggle?.(listing.id)
    }

    if (!listing) return null

    // ── Render ────────────────────────────────────────────────────────────────
    return (
        // Fixed full-screen overlay — like Instagram Reels
        <div
            className="fixed inset-0 z-50 bg-black flex flex-col"
            onTouchStart={onTouchStart}
            onTouchEnd={onTouchEnd}
        >
            {/* ── Media layer ── */}
            <div className="relative flex-1 overflow-hidden">

                {/* VIDEO (when landlord uploaded a video tour) */}
                {hasVideo ? (
                    <video
                        ref={videoRef}
                        src={listing.videoTourUrl}
                        className="absolute inset-0 w-full h-full object-cover"
                        loop
                        playsInline
                        muted={isMuted}
                        autoPlay
                        onClick={togglePlay}
                    />
                ) : mainImg ? (
                    /* PHOTO */
                    <Image
                        key={`${listing.id}-${imgIndex}`}
                        src={mainImg}
                        alt={listing.title}
                        fill
                        className="object-cover animate-in fade-in duration-300"
                        sizes="100vw"
                        priority
                    />
                ) : (
                    /* Placeholder */
                    <div className="absolute inset-0 bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center">
                        <span className="text-8xl">🏠</span>
                    </div>
                )}

                {/* Gradient overlay */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-transparent to-black/30 pointer-events-none" />

                {/* ── TOP BAR: progress + close ── */}
                <div className="absolute top-0 left-0 right-0 flex items-center justify-between px-4 pt-4 pb-2 safe-area-top">
                    {/* Photo / video progress dots */}
                    {!hasVideo && gallery.length > 1 && (
                        <div className="flex gap-1">
                            {gallery.slice(0, 7).map((_, i) => (
                                <div key={i} className={cn(
                                    "h-0.5 rounded-full transition-all duration-300",
                                    i === imgIndex ? "w-5 bg-white" : "w-2 bg-white/40"
                                )} />
                            ))}
                        </div>
                    )}
                    {hasVideo && (
                        <div className="flex items-center gap-2">
                            <div className="h-2 w-2 rounded-full bg-red-500 animate-pulse" />
                            <span className="text-white/70 text-xs font-medium">Video Tour</span>
                        </div>
                    )}
                    <div className="ml-auto flex items-center gap-2">
                        <span className="text-white/50 text-xs">{currentIndex + 1}/{listings.length}</span>
                        <button onClick={onClose}
                            className="h-9 w-9 rounded-full bg-black/50 flex items-center justify-center">
                            <X className="h-5 w-5 text-white" />
                        </button>
                    </div>
                </div>

                {/* ── Photo tap zones (prev/next) ── */}
                {!hasVideo && gallery.length > 1 && (
                    <>
                        <button onClick={cyclePhoto(-1)} className="absolute left-0 top-[10%] bottom-[35%] w-1/3" />
                        <button onClick={cyclePhoto(1)} className="absolute left-1/3 top-[10%] bottom-[35%] w-1/3" />
                    </>
                )}

                {/* ── Video controls strip ── */}
                {hasVideo && (
                    <div className="absolute top-16 left-4 flex gap-2">
                        <button onClick={togglePlay}
                            className="h-9 w-9 rounded-full bg-black/50 flex items-center justify-center">
                            {isPlaying
                                ? <Pause className="h-4 w-4 text-white" />
                                : <Play className="h-4 w-4 text-white" />}
                        </button>
                        <button onClick={() => { setIsMuted(m => !m); if (videoRef.current) videoRef.current.muted = !isMuted }}
                            className="h-9 w-9 rounded-full bg-black/50 flex items-center justify-center">
                            {isMuted
                                ? <VolumeX className="h-4 w-4 text-white" />
                                : <Volume2 className="h-4 w-4 text-white" />}
                        </button>
                    </div>
                )}

                {/* ── RIGHT ACTION BAR (TikTok-style) ── */}
                <div className="absolute right-3 bottom-48 flex flex-col items-center gap-5">
                    {/* Favourite */}
                    <div className="flex flex-col items-center gap-1">
                        <button onClick={handleFavorite}
                            className={cn(
                                "h-12 w-12 rounded-full flex items-center justify-center transition-all active:scale-90",
                                favorited[listing.id]
                                    ? "bg-red-500 shadow-lg shadow-red-500/40"
                                    : "bg-black/40"
                            )}>
                            <Heart className={cn("h-6 w-6", favorited[listing.id] ? "fill-white text-white" : "text-white")} />
                        </button>
                        <span className="text-white/70 text-[11px] font-medium">Save</span>
                    </div>

                    {/* Apply */}
                    <div className="flex flex-col items-center gap-1">
                        <Link href={`/listings/${listing.id}`}
                            className="h-12 w-12 rounded-full bg-black/40 flex items-center justify-center">
                            <MessageSquare className="h-6 w-6 text-white" />
                        </Link>
                        <span className="text-white/70 text-[11px] font-medium">Apply</span>
                    </div>

                    {/* Share */}
                    <div className="flex flex-col items-center gap-1">
                        <button
                            onClick={() => navigator.share?.({
                                title: listing.title,
                                url: `${window.location.origin}/listings/${listing.id}`
                            }).catch(() => { })}
                            className="h-12 w-12 rounded-full bg-black/40 flex items-center justify-center">
                            <Share2 className="h-6 w-6 text-white" />
                        </button>
                        <span className="text-white/70 text-[11px] font-medium">Share</span>
                    </div>
                </div>

                {/* ── BOTTOM INFO OVERLAY ── */}
                <div className="absolute bottom-0 left-0 right-16 p-4 pb-6">
                    {/* Badges */}
                    <div className="flex flex-wrap gap-2 mb-3">
                        {listing.matchScore != null && listing.matchScore > 0 && (
                            <span className={cn(
                                "flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold text-white",
                                listing.matchScore >= 80 ? "bg-emerald-500" : listing.matchScore >= 60 ? "bg-blue-500" : "bg-amber-500"
                            )}>
                                <Sparkles className="h-3 w-3" /> {listing.matchScore}% match
                            </span>
                        )}
                        {(() => {
                            const tier = listing.trustTier
                            const chip = trustTierChipClassName(tier, "reel")
                            const label = (() => {
                                switch (tier) {
                                    case "HIGH": return t.listings.trustTierHigh
                                    case "VERIFIED": return t.listings.trustTierVerified
                                    case "REVIEWED": return t.listings.trustTierReviewed
                                    default: return null
                                }
                            })()
                            const title = (() => {
                                switch (tier) {
                                    case "HIGH": return t.listings.trustTierTitleHigh
                                    case "VERIFIED": return t.listings.trustTierTitleVerified
                                    case "REVIEWED": return t.listings.trustTierTitleReviewed
                                    default: return null
                                }
                            })()
                            if (label && chip) {
                                return (
                                    <span title={title ?? undefined} className={cn(chip, "text-white")}>
                                        <CheckCircle className="h-3 w-3" /> {label}
                                    </span>
                                )
                            }
                            if (listing.isVerified) {
                                return (
                                    <span className="flex items-center gap-1 rounded-full bg-emerald-500/80 px-3 py-1 text-xs font-bold text-white">
                                        <CheckCircle className="h-3 w-3" /> {t.listings.verified}
                                    </span>
                                )
                            }
                            return null
                        })()}
                    </div>

                    {/* Price — uses rentAmount */}
                    <div className="flex items-baseline gap-1.5 mb-1">
                        <span className="text-3xl font-extrabold text-white">
                            {formatCurrency(listing.rentAmount ?? 0)}
                        </span>
                        <span className="text-white/60 text-sm font-medium">/mo</span>
                    </div>

                    {/* Title */}
                    <h2 className="text-white font-bold text-base leading-snug mb-1 line-clamp-1">
                        {listing.title}
                    </h2>

                    {/* Location */}
                    <div className="flex items-center gap-1 text-white/70 text-sm mb-3">
                        <MapPin className="h-3.5 w-3.5 shrink-0" />
                        <span className="line-clamp-1">
                            {listing.neighborhood ? `${listing.neighborhood}, ${listing.city}` : listing.city}
                        </span>
                    </div>

                    {/* Bed/bath + CTA */}
                    <div className="flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3 text-white/80 text-sm">
                            <span className="flex items-center gap-1.5">
                                <Bed className="h-4 w-4" /> {listing.bedrooms}
                            </span>
                            <span className="flex items-center gap-1.5">
                                <Bath className="h-4 w-4" /> {listing.bathrooms}
                            </span>
                        </div>
                        <Link href={`/listings/${listing.id}`}
                            className="bg-white text-slate-900 font-bold text-sm px-5 py-2.5 rounded-full active:scale-95 transition-transform shrink-0">
                            View &rarr;
                        </Link>
                    </div>

                    {/* Description */}
                    {listing.description && (
                        <button onClick={() => setShowDesc(s => !s)}
                            className="mt-3 text-white/60 text-xs text-left w-full">
                            {showDesc
                                ? <><span className="text-white/80">{listing.description}</span>{" "}<span className="text-white/40">less</span></>
                                : <span className="line-clamp-2">{listing.description} <span className="text-white/40">more</span></span>}
                        </button>
                    )}

                    {/* Amenity pills */}
                    {listing.amenities && listing.amenities.length > 0 && (
                        <div className="flex gap-2 mt-3 overflow-x-auto scrollbar-hide pb-safe">
                            {listing.amenities.slice(0, 5).map(a => (
                                <span key={a} className="shrink-0 bg-white/15 text-white text-[10px] font-medium px-2.5 py-1 rounded-full">
                                    {a}
                                </span>
                            ))}
                        </div>
                    )}
                </div>

                {/* ── UP/DOWN arrows ── */}
                <div className="absolute right-3 top-1/2 -translate-y-1/2 flex flex-col gap-2">
                    <button onClick={goPrev} disabled={currentIndex === 0}
                        className="h-9 w-9 rounded-full bg-black/40 flex items-center justify-center disabled:opacity-20">
                        <ChevronUp className="h-5 w-5 text-white" />
                    </button>
                    <button onClick={goNext} disabled={currentIndex >= listings.length - 1}
                        className="h-9 w-9 rounded-full bg-black/40 flex items-center justify-center disabled:opacity-20">
                        <ChevronDown className="h-5 w-5 text-white" />
                    </button>
                </div>
            </div>
        </div>
    )
}
