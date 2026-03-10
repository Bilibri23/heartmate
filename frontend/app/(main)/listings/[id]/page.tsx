"use client"

import { useState, useEffect } from "react"
import { useParams, useRouter } from "next/navigation"
import Image from "next/image"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import {
  ArrowLeft,
  Heart,
  Share2,
  MapPin,
  Bed,
  Bath,
  Maximize,
  CheckCircle,
  Star,
  MessageCircle,
  Phone,
  Shield,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Clock,
  CalendarDays,
  Users,
  UserPlus
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import { Textarea } from "@/components/ui/textarea"
import { Input } from "@/components/ui/input"
import { VideoPlayer } from "@/components/ui/video-player"
import { VirtualTour3D } from "@/components/ui/virtual-tour-3d"
import { VirtualTourEmbed } from "@/components/ui/virtual-tour-embed"
import api from "@/lib/api"
import Link from "next/link"
import { toast } from "sonner"

interface ListingDetail {
  id: string
  title: string
  description: string
  rentAmount: number
  depositAmount?: number
  deposit?: number
  city: string
  neighborhood: string
  address: string
  propertyType: string
  bedrooms: number
  bathrooms: number
  size?: number
  squareMeters?: number
  amenities: string[]
  photos: { id: string; photoUrl: string; isPrimary: boolean }[]
  videoTour?: {
    id: string
    videoUrl: string
    thumbnailUrl?: string
    duration?: number
  }
  videoTourUrl?: string
  videoTourEmbedCode?: string
  virtualTourProvider?: string
  videoTourThumbnail?: string
  videoTourDuration?: number
  verified: boolean
  featured: boolean
  status: string
  viewsCount: number
  favoritesCount: number
  isFavorited?: boolean
  isFavorite?: boolean
  // Backend returns flat landlord fields
  landlordId: string
  landlordName: string
  // Also support nested landlord object if present
  landlord?: {
    id: string
    firstName: string
    lastName: string
    profilePhotoUrl?: string
    verified: boolean
  }
  averageRating?: number
  reviewsCount?: number
  reviewCount?: number
  createdAt: string
}

interface ExistingApplication {
  id: string
  status: string
}

export default function ListingDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const [listing, setListing] = useState<ListingDetail | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isFavorited, setIsFavorited] = useState(false)
  const [currentPhotoIndex, setCurrentPhotoIndex] = useState(0)
  const [isApplyOpen, setIsApplyOpen] = useState(false)
  const [isScheduleOpen, setIsScheduleOpen] = useState(false)
  const [applicationMessage, setApplicationMessage] = useState("")
  const [moveInDate, setMoveInDate] = useState("")
  const [leaseDuration, setLeaseDuration] = useState(6)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [viewingDate, setViewingDate] = useState("")
  const [viewingTime, setViewingTime] = useState("")
  const [viewingMessage, setViewingMessage] = useState("")
  const [is3DTourMode, setIs3DTourMode] = useState(false)
  const [isScheduling, setIsScheduling] = useState(false)
  const [existingApplication, setExistingApplication] = useState<ExistingApplication | null>(null)
  const [isCheckingApplication, setIsCheckingApplication] = useState(false)
  const [showVerifyPrompt, setShowVerifyPrompt] = useState(false)

  const MESSAGE_TEMPLATES = [
    {
      label: "🎓 Student",
      text: `Hi, I'm a student and I'm very interested in this property. I'm looking for a comfortable place near my university. When would it be possible to schedule a visit? I'm reliable and respectful of shared spaces. Thank you!`,
    },
    {
      label: "💼 Professional",
      text: `Hello, I'm a young professional looking for quality accommodation. I'm interested in this listing and would love to discuss the details. I have a stable income and can provide references. Is the property still available?`,
    },
    {
      label: "🏠 Quick Move-in",
      text: `Hi there! I'm looking to move in as soon as possible and this property caught my eye. I have all my documents ready and I can meet at your earliest convenience. Could we arrange a viewing soon? Thank you!`,
    },
  ]

  const [mutualMatches, setMutualMatches] = useState<any[]>([])
  const [isShareRoomOpen, setIsShareRoomOpen] = useState(false)
  const [isLoadingMatches, setIsLoadingMatches] = useState(false)

  useEffect(() => {
    const fetchListing = async () => {
      try {
        const response = await api.get(`/listings/${params.id}`, {
          params: user?.id ? { userId: user.id } : undefined
        })
        setListing(response.data)
        setIsFavorited(response.data?.isFavorited || response.data?.isFavorite || false)

        // Debug: Log virtual tour data
        console.log('Virtual Tour Data:', {
          videoTourUrl: response.data?.videoTourUrl,
          virtualTourProvider: response.data?.virtualTourProvider,
          videoTour: response.data?.videoTour,
          videoTourEmbedCode: response.data?.videoTourEmbedCode
        })

        // Track view (don't await, fire and forget)
        api.post(`/listings/${params.id}/view`, null, {
          params: user?.id ? { userId: user.id } : undefined
        }).catch(() => { })
      } catch (err: any) {
        console.error("Failed to fetch listing:", err)
        setListing(null)
      } finally {
        setIsLoading(false)
      }
    }

    if (params.id) {
      fetchListing()
    }
  }, [params.id, user?.id])

  useEffect(() => {
    const fetchExistingApplication = async () => {
      if (!user?.id || !params.id) {
        setExistingApplication(null)
        return
      }

      setIsCheckingApplication(true)
      try {
        const response = await api.get(`/applications/my/listing/${params.id}`, {
          validateStatus: (status) => status < 500,
        })
        if (response.status === 200) {
          setExistingApplication(response.data)
        } else {
          setExistingApplication(null)
        }
      } catch (err) {
        setExistingApplication(null)
      } finally {
        setIsCheckingApplication(false)
      }
    }

    fetchExistingApplication()
  }, [params.id, user?.id])

  const handleFavorite = async () => {
    if (!user?.id) {
      router.push("/login")
      return
    }

    try {
      await api.post(`/listings/${params.id}/favorite`, null, {
        params: { userId: user.id }
      })
      setIsFavorited(!isFavorited)
    } catch (err) {
      console.error("Failed to toggle favorite:", err)
    }
  }

  const fetchMutualMatches = async () => {
    if (!user?.id) return
    setIsLoadingMatches(true)
    try {
      const response = await api.get(`/matches/${user.id}`)
      const matches = response.data || []
      const mutual = matches.filter((m: any) =>
        m.status === "MUTUAL" || m.status === "ACCEPTED" || m.isMutualMatch
      ).map((m: any) => ({
        id: m.id,
        matchedUser: {
          id: m.matchedUserId,
          firstName: m.matchedUserFirstName || "Unknown",
          lastName: m.matchedUserLastName || "",
          profilePhotoUrl: m.matchedUserProfilePhotoUrl,
        },
        compatibilityScore: m.compatibilityScore,
      }))
      setMutualMatches(mutual)
    } catch (err) {
      console.error("Failed to fetch matches:", err)
    } finally {
      setIsLoadingMatches(false)
    }
  }

  const [applicationError, setApplicationError] = useState<string | null>(null)

  const handleApply = async () => {
    if (!user?.id) {
      router.push("/login")
      return
    }

    if (existingApplication) {
      setApplicationError("You have already applied to this listing. Check your applications page to view the status.")
      return
    }

    if (listing?.status && listing.status !== "ACTIVE") {
      setApplicationError("This listing is not available for applications.")
      return
    }

    // Validate required fields
    if (!moveInDate || !applicationMessage || applicationMessage.length < 50) {
      setApplicationError("Please fill in all required fields. Message must be at least 50 characters.")
      return
    }

    setIsSubmitting(true)
    setApplicationError(null)
    try {
      await api.post("/applications", {
        listingId: params.id,
        message: applicationMessage,
        moveInDate: moveInDate,
        leaseDurationMonths: leaseDuration,
      })
      setIsApplyOpen(false)

      // Check if user is verified — if not, show verification nudge
      // PAUSED FOR DEVELOPMENT - always redirect to applications
      router.push("/applications")

      // TODO: Re-enable verification prompt after development
      /*
      try {
        const verRes = await api.get(`/verifications/${user?.id}`)
        const status = verRes.data?.status
        if (status === "VERIFIED" || status === "APPROVED") {
          router.push("/applications")
        } else {
          setShowVerifyPrompt(true)
        }
      } catch {
        // If verification check fails (e.g. 404 = no verification), show nudge
        setShowVerifyPrompt(true)
      }
      */
    } catch (err: any) {
      console.error("Failed to apply:", err)
      const errorMessage = err.response?.data?.message || ""
      // Check for duplicate application
      if (err.response?.status === 400 && (errorMessage.toLowerCase().includes("already") || errorMessage.toLowerCase().includes("exists"))) {
        setApplicationError("You have already applied to this listing. Check your applications page to view the status.")
      } else if (err.response?.status === 403) {
        router.push("/verification")
      } else {
        setApplicationError("Failed to submit application. Please try again.")
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleFlagContent = async (listingId: string) => {
    if (!user?.id) return

    // Show flag dialog
    const reason = prompt("Why are you reporting this content? (Inappropriate, Violence, Fraud, Spam, Copyright, Illegal, Other)")
    if (!reason) return

    const description = prompt("Please provide more details (optional):")

    try {
      await api.post(`/content-moderation/flag/${listingId}`, null, {
        params: {
          reporterId: user.id,
          reason: reason.toUpperCase(),
          description: description || ""
        }
      })

      toast.success("Content reported. Thank you for helping keep our platform safe.")
    } catch (error: any) {
      console.error("Failed to flag content:", error)
      toast.error(error.response?.data?.message || "Failed to report content")
    }
  }

  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: listing?.title,
          text: `Check out this listing on RoomBuddy: ${listing?.title}`,
          url: window.location.href,
        })
      } catch (err) {
        console.error("Share failed:", err)
      }
    }
  }

  const handleScheduleViewing = async () => {
    if (!user?.id) {
      router.push("/login")
      return
    }

    if (existingApplication) {
      return
    }

    if (listing?.status && listing.status !== "ACTIVE") {
      return
    }

    // Get landlord ID from either nested object or flat field
    const landlordId = listing?.landlord?.id || listing?.landlordId
    if (!landlordId) {
      console.error("No landlord ID available")
      return
    }

    if (!viewingDate || !viewingTime) return

    setIsScheduling(true)
    try {
      // Send a message to the landlord with the viewing request
      const formattedDate = new Date(viewingDate).toLocaleDateString("en-GB", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric"
      })

      const message = `📅 **Viewing Request**\n\nI would like to schedule a viewing for:\n\n🏠 Property: ${listing.title}\n📍 Location: ${listing.neighborhood}, ${listing.city}\n📆 Date: ${formattedDate}\n⏰ Time: ${viewingTime}\n\n${viewingMessage ? `Message: ${viewingMessage}` : ""}`

      await api.post("/messages", {
        receiverId: landlordId,
        content: message,
      })

      setIsScheduleOpen(false)
      setViewingDate("")
      setViewingTime("")
      setViewingMessage("")

      // Redirect to messages
      router.push(`/messages/${landlordId}`)
    } catch (err) {
      console.error("Failed to schedule viewing:", err)
    } finally {
      setIsScheduling(false)
    }
  }

  const nextPhoto = () => {
    if (listing?.photos) {
      setCurrentPhotoIndex((prev) =>
        prev === listing.photos.length - 1 ? 0 : prev + 1
      )
    }
  }

  const prevPhoto = () => {
    if (listing?.photos) {
      setCurrentPhotoIndex((prev) =>
        prev === 0 ? listing.photos.length - 1 : prev - 1
      )
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-white">
        <Skeleton className="h-72 w-full" />
        <div className="p-4 space-y-4">
          <Skeleton className="h-8 w-32" />
          <Skeleton className="h-6 w-full" />
          <Skeleton className="h-4 w-48" />
          <Skeleton className="h-24 w-full" />
        </div>
      </div>
    )
  }

  if (!listing) {
    return (
      <div className="min-h-screen bg-white flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4">🏠</div>
          <h2 className="text-xl font-semibold text-slate-900">Listing not found</h2>
          <Button className="mt-4" onClick={() => router.back()}>
            Go Back
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-white pb-24">
      {/* Photo Gallery */}
      <div className="relative h-72 sm:h-96 bg-slate-200 overflow-hidden">
        {listing.photos && listing.photos.length > 0 ? (
          <>
            <Image
              src={listing.photos[currentPhotoIndex].photoUrl}
              alt={listing.title}
              fill
              className="object-cover"
            />

            {listing.photos.length > 1 && (
              <>
                <button
                  onClick={prevPhoto}
                  className="absolute left-2 top-1/2 -translate-y-1/2 h-10 w-10 rounded-full bg-white/90 flex items-center justify-center shadow-lg"
                >
                  <ChevronLeft className="h-6 w-6" />
                </button>
                <button
                  onClick={nextPhoto}
                  className="absolute right-2 top-1/2 -translate-y-1/2 h-10 w-10 rounded-full bg-white/90 flex items-center justify-center shadow-lg"
                >
                  <ChevronRight className="h-6 w-6" />
                </button>

                {/* Photo indicators */}
                <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-1.5">
                  {listing.photos.map((_, index) => (
                    <button
                      key={index}
                      onClick={() => setCurrentPhotoIndex(index)}
                      className={`h-2 rounded-full transition-all ${index === currentPhotoIndex
                        ? "w-6 bg-white"
                        : "w-2 bg-white/60"
                        }`}
                    />
                  ))}
                </div>
              </>
            )}
          </>
        ) : (
          <div className="flex h-full items-center justify-center">
            <span className="text-6xl">🏠</span>
          </div>
        )}

        {/* Top Actions */}
        <div className="absolute top-4 left-4 right-4 flex justify-between">
          <button
            onClick={() => router.back()}
            className="h-10 w-10 rounded-full bg-white/90 flex items-center justify-center shadow-lg"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>

          <div className="flex gap-2">
            <button
              onClick={handleShare}
              className="h-10 w-10 rounded-full bg-white/90 flex items-center justify-center shadow-lg"
            >
              <Share2 className="h-5 w-5" />
            </button>
            <button
              onClick={handleFavorite}
              className="h-10 w-10 rounded-full bg-white/90 flex items-center justify-center shadow-lg"
            >
              <Heart className={`h-5 w-5 ${isFavorited ? "fill-red-500 text-red-500" : ""}`} />
            </button>
          </div>
        </div>

        {/* Badges */}
        <div className="absolute bottom-4 left-4 flex gap-2">
          {(listing.videoTour || listing.videoTourUrl) && (
            <span className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-blue-600 text-white text-xs font-medium">
              <div className="w-2 h-2 bg-white rounded-full animate-pulse" />
              Virtual Tour
            </span>
          )}
          {listing.verified && (
            <span className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-500 text-white text-xs font-medium">
              <CheckCircle className="h-3.5 w-3.5" />
              {t.listings.verified}
            </span>
          )}
          {listing.featured && (
            <span className="px-2.5 py-1 rounded-full bg-amber-500 text-white text-xs font-medium">
              {t.listings.featured}
            </span>
          )}
        </div>
      </div>

      {/* Virtual Tour Section */}
      {(listing.videoTour || listing.videoTourUrl || listing.videoTourEmbedCode) && (
        <div className="px-4 py-4 bg-slate-50">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
              <div className="w-2 h-2 bg-gradient-to-r from-blue-600 to-purple-600 rounded-full animate-pulse" />
              {listing.virtualTourProvider === '360' ? '360° Virtual Tour' : 'Virtual Tour'}
            </h3>
            {/* Only show toggle for legacy listings without a clear provider */}
            {(!listing.virtualTourProvider || (listing.virtualTourProvider !== '360' && listing.virtualTourProvider !== 'video')) && (
              <Button
                size="sm"
                variant={is3DTourMode ? "default" : "outline"}
                onClick={() => setIs3DTourMode(!is3DTourMode)}
                className="rounded-xl"
              >
                {is3DTourMode ? "360° Mode" : "Video Mode"}
              </Button>
            )}
          </div>

          {/* 360° self-hosted tour via Pannellum */}
          {listing.virtualTourProvider === '360' && (
            <VirtualTourEmbed
              tourUrl={listing.videoTour?.videoUrl || listing.videoTourUrl}
              title={`${listing.title} - 360° Tour`}
              className="rounded-xl"
              onFlag={() => handleFlagContent(listing.id)}
              listingId={listing.id}
            />
          )}

          {/* Video walkthrough (self-hosted MP4) */}
          {listing.virtualTourProvider === 'video' && (
            <VideoPlayer
              src={listing.videoTour?.videoUrl || listing.videoTourUrl || undefined}
              thumbnail={listing.videoTour?.thumbnailUrl || listing.videoTourThumbnail}
              title={`${listing.title} - Video Tour`}
              className="h-64 rounded-xl"
              onFlag={() => handleFlagContent(listing.id)}
              onPlay={() => { }}
            />
          )}

          {/* Legacy: listings without a clear provider — keep old toggle behaviour */}
          {(!listing.virtualTourProvider || (listing.virtualTourProvider !== '360' && listing.virtualTourProvider !== 'video')) && (
            is3DTourMode ? (
              <VirtualTourEmbed
                tourUrl={listing.videoTour?.videoUrl || listing.videoTourUrl}
                embedCode={listing.videoTourEmbedCode}
                title={`${listing.title} - 3D Virtual Tour`}
                className="h-96 rounded-xl"
                onFlag={() => handleFlagContent(listing.id)}
                listingId={listing.id}
              />
            ) : (
              <VideoPlayer
                src={listing.videoTour?.videoUrl || listing.videoTourUrl || undefined}
                thumbnail={listing.videoTour?.thumbnailUrl || listing.videoTourThumbnail}
                title={`${listing.title} - Virtual Tour`}
                className="h-64 rounded-xl"
                onFlag={() => handleFlagContent(listing.id)}
                onPlay={() => { }}
              />
            )
          )}

          {/* Tour info chips */}
          <div className="mt-3 grid grid-cols-3 gap-2">
            <div className="bg-white rounded-lg p-2 text-center">
              <div className="text-xs text-slate-500 mb-1">Experience</div>
              <div className="text-sm font-medium text-slate-900">
                {listing.virtualTourProvider === '360' ? '360° View' : 'Standard'}
              </div>
            </div>
            <div className="bg-white rounded-lg p-2 text-center">
              <div className="text-xs text-slate-500 mb-1">Controls</div>
              <div className="text-sm font-medium text-slate-900">
                {listing.virtualTourProvider === '360' ? 'Interactive' : 'Play/Pause'}
              </div>
            </div>
            <div className="bg-white rounded-lg p-2 text-center">
              <div className="text-xs text-slate-500 mb-1">Quality</div>
              <div className="text-sm font-medium text-slate-900">HD</div>
            </div>
          </div>
        </div>
      )}

      {/* Roommate Match Banner */}
      {mutualMatches.length > 0 && listing?.status === "ACTIVE" && (
        <div className="bg-gradient-to-r from-indigo-600 to-blue-600 px-4 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3 text-white">
              <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                <Users className="h-5 w-5" />
              </div>
              <div>
                <p className="font-medium text-sm">{mutualMatches.length} Roommate Match{mutualMatches.length > 1 ? "es" : ""}</p>
                <p className="text-xs text-white/80">Share this listing with a match</p>
              </div>
            </div>
            <Button
              size="sm"
              className="bg-white text-indigo-700 hover:bg-white/90"
              onClick={() => { setIsShareRoomOpen(true); if (mutualMatches.length === 0) fetchMutualMatches() }}
            >
              Share
            </Button>
          </div>
        </div>
      )}

      {/* Content */}
      <div className="p-4 space-y-6">
        {/* Price & Title */}
        <div>
          <div className="flex items-baseline gap-2 mb-2">
            <span className="text-2xl font-bold text-slate-900">
              {formatCurrency(listing.rentAmount)}
            </span>
            <span className="text-slate-500">{t.listings.perMonth}</span>
          </div>
          <h1 className="text-xl font-semibold text-slate-900">{listing.title}</h1>
          <div className="flex items-center gap-1 mt-1 text-slate-500">
            <MapPin className="h-4 w-4" />
            <span className="text-sm">
              {listing.neighborhood}, {listing.city}
            </span>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="flex gap-4 py-4 border-y border-slate-100">
          <div className="flex items-center gap-2">
            <div className="h-10 w-10 rounded-xl bg-blue-50 flex items-center justify-center">
              <Bed className="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">{listing.bedrooms}</p>
              <p className="text-xs text-slate-500">{t.listings.bedrooms}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-10 w-10 rounded-xl bg-blue-50 flex items-center justify-center">
              <Bath className="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">{listing.bathrooms}</p>
              <p className="text-xs text-slate-500">{t.listings.bathrooms}</p>
            </div>
          </div>
          {(listing.squareMeters || listing.size) && (listing.squareMeters || listing.size)! > 0 && (
            <div className="flex items-center gap-2">
              <div className="h-10 w-10 rounded-xl bg-blue-50 flex items-center justify-center">
                <Maximize className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-900">{listing.squareMeters || listing.size}</p>
                <p className="text-xs text-slate-500">m²</p>
              </div>
            </div>
          )}
        </div>

        {/* Description */}
        <div>
          <h2 className="text-lg font-semibold text-slate-900 mb-2">
            {t.listings.description}
          </h2>
          <p className="text-sm text-slate-600 leading-relaxed">
            {listing.description}
          </p>
        </div>

        {/* Amenities */}
        {listing.amenities && listing.amenities.length > 0 && (
          <div>
            <h2 className="text-lg font-semibold text-slate-900 mb-3">
              {t.listings.amenities}
            </h2>
            <div className="flex flex-wrap gap-2">
              {listing.amenities.map((amenity) => (
                <span
                  key={amenity}
                  className="px-3 py-1.5 rounded-full bg-slate-100 text-sm text-slate-700"
                >
                  {amenity}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Landlord */}
        <div className="bg-slate-50 rounded-2xl p-4">
          <h2 className="text-lg font-semibold text-slate-900 mb-3">
            Landlord
          </h2>
          {(listing.landlord || listing.landlordId) ? (
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Avatar className="h-12 w-12">
                  <AvatarImage src={listing.landlord?.profilePhotoUrl || undefined} />
                  <AvatarFallback className="bg-blue-100 text-blue-600">
                    {listing.landlord?.firstName?.[0] || listing.landlordName?.[0] || "L"}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <p className="font-medium text-slate-900">
                    {listing.landlord ? `${listing.landlord.firstName} ${listing.landlord.lastName}` : listing.landlordName}
                  </p>
                  {listing.landlord?.verified && (
                    <p className="text-xs text-emerald-600 flex items-center gap-1">
                      <Shield className="h-3 w-3" />
                      Verified Landlord
                    </p>
                  )}
                </div>
              </div>
              <Link href={`/messages/${listing.landlord?.id || listing.landlordId}`}>
                <Button variant="outline" size="sm" className="rounded-xl">
                  <MessageCircle className="h-4 w-4 mr-1" />
                  Message
                </Button>
              </Link>
            </div>
          ) : (
            <p className="text-slate-500 text-sm">Landlord information unavailable</p>
          )}
        </div>

        {/* Reviews Summary */}
        {listing.reviewsCount && listing.reviewsCount > 0 && (
          <Link href={`/listings/${listing.id}/reviews`}>
            <div className="flex items-center justify-between py-4 border-t border-slate-100">
              <div className="flex items-center gap-2">
                <Star className="h-5 w-5 fill-amber-400 text-amber-400" />
                <span className="font-semibold text-slate-900">
                  {listing.averageRating?.toFixed(1)}
                </span>
                <span className="text-slate-500">
                  ({listing.reviewsCount} {t.reviews.title.toLowerCase()})
                </span>
              </div>
              <ChevronRight className="h-5 w-5 text-slate-400" />
            </div>
          </Link>
        )}
      </div>

      {/* Fixed Bottom CTA */}
      <div className="fixed bottom-16 left-0 right-0 bg-white border-t border-slate-200 p-4 safe-area-bottom">
        <div className="flex gap-3 max-w-lg mx-auto">
          <div className="flex-1">
            <p className="text-xs text-slate-500">Deposit</p>
            <p className="font-semibold text-slate-900">
              {formatCurrency(listing.deposit || listing.depositAmount || listing.rentAmount)}
            </p>
          </div>

          {/* Schedule Viewing Button */}
          <Sheet
            open={isScheduleOpen}
            onOpenChange={(open) => setIsScheduleOpen(open && !existingApplication && listing?.status === "ACTIVE")}
          >
            <SheetTrigger asChild>
              <Button
                variant="outline"
                className="h-12 rounded-xl px-4"
                disabled={Boolean(existingApplication) || listing?.status !== "ACTIVE"}
              >
                <CalendarDays className="h-5 w-5" />
              </Button>
            </SheetTrigger>
            <SheetContent side="bottom" className="h-[70vh] rounded-t-3xl">
              <SheetHeader>
                <SheetTitle className="flex items-center gap-2">
                  <CalendarDays className="h-5 w-5 text-blue-600" />
                  Schedule a Viewing
                </SheetTitle>
              </SheetHeader>

              <div className="mt-6 space-y-4">
                <div className="bg-blue-50 rounded-xl p-4">
                  <p className="text-sm text-blue-800">
                    Request a viewing appointment. The landlord will confirm the time with you via message.
                  </p>
                </div>

                <div>
                  <label className="text-sm font-medium text-slate-700 mb-2 block">
                    Preferred Date *
                  </label>
                  <Input
                    type="date"
                    value={viewingDate}
                    onChange={(e) => setViewingDate(e.target.value)}
                    min={new Date().toISOString().split("T")[0]}
                    className="rounded-xl"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium text-slate-700 mb-2 block">
                    Preferred Time *
                  </label>
                  <Input
                    type="time"
                    value={viewingTime}
                    onChange={(e) => setViewingTime(e.target.value)}
                    className="rounded-xl"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium text-slate-700 mb-2 block">
                    Additional Message (Optional)
                  </label>
                  <Textarea
                    placeholder="Any specific requests or questions..."
                    value={viewingMessage}
                    onChange={(e) => setViewingMessage(e.target.value)}
                    className="min-h-[80px] rounded-xl"
                  />
                </div>

                <Button
                  className="w-full h-12 rounded-xl bg-blue-600 hover:bg-blue-700"
                  onClick={handleScheduleViewing}
                  disabled={isScheduling || !viewingDate || !viewingTime || Boolean(existingApplication) || listing?.status !== "ACTIVE"}
                >
                  {isScheduling ? "Sending Request..." : "Request Viewing"}
                </Button>
              </div>
            </SheetContent>
          </Sheet>

          {/* Share Room with Matched Roommate */}
          <Sheet open={isShareRoomOpen} onOpenChange={setIsShareRoomOpen}>
            <SheetTrigger asChild>
              <Button
                variant="outline"
                className="h-12 rounded-xl px-4"
                onClick={() => { setIsShareRoomOpen(true); if (mutualMatches.length === 0) fetchMutualMatches() }}
                disabled={Boolean(existingApplication) || listing?.status !== "ACTIVE"}
                title="Share with a matched roommate"
              >
                <Users className="h-5 w-5" />
              </Button>
            </SheetTrigger>
            <SheetContent side="bottom" className="h-[70vh] rounded-t-3xl overflow-y-auto">
              <SheetHeader>
                <SheetTitle className="flex items-center gap-2">
                  <UserPlus className="h-5 w-5 text-blue-600" />
                  Share with Roommate
                </SheetTitle>
              </SheetHeader>
              <div className="mt-6 space-y-4">
                <div className="bg-indigo-50 rounded-xl p-4">
                  <p className="text-sm text-indigo-800">
                    Send this listing to one of your matched roommates. They can view it and decide if they want to move in together.
                  </p>
                </div>
                {isLoadingMatches ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="animate-spin h-6 w-6 border-2 border-blue-600 border-t-transparent rounded-full" />
                  </div>
                ) : mutualMatches.length === 0 ? (
                  <div className="text-center py-8">
                    <Users className="h-12 w-12 mx-auto text-slate-300 mb-3" />
                    <p className="text-slate-600 font-medium">No matched roommates yet</p>
                    <p className="text-sm text-slate-500 mt-1">Match with someone first to share a room</p>
                    <Link href="/matches">
                      <Button variant="outline" className="mt-4 rounded-xl">Find Roommates</Button>
                    </Link>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {mutualMatches.map((match) => (
                      <Link
                        key={match.id}
                        href={`/messages/${match.matchedUser?.id}?shareListingId=${listing.id}&shareListingTitle=${encodeURIComponent(listing.title)}`}
                      >
                        <div className="flex items-center gap-3 p-3 rounded-xl border border-slate-200 hover:bg-slate-50 transition-colors">
                          <Avatar className="h-12 w-12">
                            <AvatarImage src={match.matchedUser?.profilePhotoUrl} />
                            <AvatarFallback className="bg-blue-100 text-blue-600">
                              {match.matchedUser?.firstName?.[0] || "?"}
                            </AvatarFallback>
                          </Avatar>
                          <div className="flex-1">
                            <p className="font-medium text-slate-900">
                              {match.matchedUser?.firstName} {match.matchedUser?.lastName}
                            </p>
                            <p className="text-sm text-slate-500">{match.compatibilityScore}% compatible</p>
                          </div>
                          <MessageCircle className="h-5 w-5 text-blue-600" />
                        </div>
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            </SheetContent>
          </Sheet>

          {/* Apply Now Button */}
          <Sheet
            open={isApplyOpen}
            onOpenChange={(open) => setIsApplyOpen(open && !existingApplication && listing?.status === "ACTIVE")}
          >
            <SheetTrigger asChild>
              <Button
                className="flex-1 h-12 rounded-xl text-base"
                disabled={Boolean(existingApplication) || listing?.status !== "ACTIVE" || isCheckingApplication}
              >
                {existingApplication ? "Applied" : t.listings.applyNow}
              </Button>
            </SheetTrigger>
            <SheetContent side="bottom" className="h-[80vh] rounded-t-3xl overflow-y-auto">
              <SheetHeader>
                <SheetTitle>{t.applications.apply}</SheetTitle>
              </SheetHeader>

              <div className="mt-6 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-slate-700 mb-2 block">
                      Move-in Date *
                    </label>
                    <Input
                      type="date"
                      value={moveInDate}
                      onChange={(e) => setMoveInDate(e.target.value)}
                      min={new Date().toISOString().split("T")[0]}
                      className="rounded-xl"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium text-slate-700 mb-2 block">
                      Lease Duration *
                    </label>
                    <select
                      value={leaseDuration}
                      onChange={(e) => setLeaseDuration(Number(e.target.value))}
                      className="w-full h-10 px-3 rounded-xl border border-slate-200 bg-white text-sm"
                    >
                      {[1, 2, 3, 6, 9, 12, 18, 24].map((months) => (
                        <option key={months} value={months}>
                          {months} {months === 1 ? "month" : "months"}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium text-slate-700 mb-2 block">
                    {t.applications.message} * <span className="text-slate-400 font-normal">(min 50 characters)</span>
                  </label>

                  {/* Message Templates */}
                  {!applicationMessage && (
                    <div className="mb-3">
                      <p className="text-xs text-slate-500 mb-2">Quick templates:</p>
                      <div className="flex flex-wrap gap-2">
                        {MESSAGE_TEMPLATES.map((tpl) => (
                          <button
                            key={tpl.label}
                            type="button"
                            onClick={() => setApplicationMessage(tpl.text)}
                            className="px-3 py-1.5 rounded-full bg-blue-50 text-blue-700 text-xs font-medium hover:bg-blue-100 transition-colors border border-blue-100"
                          >
                            {tpl.label}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  <Textarea
                    placeholder="Tell the landlord about yourself, why you're interested in this property, your occupation, etc."
                    value={applicationMessage}
                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setApplicationMessage(e.target.value)}
                    className="min-h-[120px] rounded-xl"
                  />
                  <div className="flex items-center justify-between mt-1">
                    <p className="text-xs text-slate-400">
                      {applicationMessage.length}/50 characters minimum
                    </p>
                    {applicationMessage && (
                      <button
                        type="button"
                        onClick={() => setApplicationMessage("")}
                        className="text-xs text-slate-400 hover:text-slate-600"
                      >
                        Clear
                      </button>
                    )}
                  </div>
                </div>

                {applicationError && (
                  <div className="bg-red-50 border border-red-200 rounded-xl p-3">
                    <p className="text-sm text-red-600">{applicationError}</p>
                    {applicationError.includes("already applied") && (
                      <Button
                        variant="link"
                        className="text-red-600 p-0 h-auto mt-1"
                        onClick={() => router.push("/applications")}
                      >
                        View my applications →
                      </Button>
                    )}
                  </div>
                )}

                {existingApplication && (
                  <div className="bg-blue-50 border border-blue-100 rounded-xl p-3">
                    <p className="text-sm text-blue-700">
                      You already applied to this listing. Track your application status from your applications page.
                    </p>
                    <Button
                      variant="link"
                      className="text-blue-700 p-0 h-auto mt-1"
                      onClick={() => router.push("/applications")}
                    >
                      View my applications →
                    </Button>
                  </div>
                )}

                <Button
                  className="w-full h-12 rounded-xl"
                  onClick={handleApply}
                  disabled={
                    isSubmitting ||
                    !moveInDate ||
                    applicationMessage.length < 50 ||
                    Boolean(existingApplication) ||
                    listing?.status !== "ACTIVE"
                  }
                >
                  {isSubmitting ? t.common.loading : t.applications.apply}
                </Button>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>

      {/* Verification Prompt Modal */}
      {showVerifyPrompt && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl p-6 max-w-sm w-full shadow-xl animate-in fade-in zoom-in-95">
            <div className="text-center">
              <div className="mx-auto w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mb-4">
                <Shield className="h-8 w-8 text-amber-600" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-2">
                Verify your identity to stand out!
              </h3>
              <p className="text-sm text-slate-500 mb-6">
                Your application was submitted successfully! Verified applicants are <span className="font-semibold text-slate-700">3x more likely</span> to be accepted by landlords.
              </p>
              <div className="flex flex-col gap-2">
                <Button
                  className="w-full h-11 rounded-xl bg-blue-600 hover:bg-blue-700"
                  onClick={() => {
                    setShowVerifyPrompt(false)
                    router.push("/verification")
                  }}
                >
                  <Shield className="h-4 w-4 mr-2" />
                  Verify Now
                </Button>
                <Button
                  variant="ghost"
                  className="w-full h-11 rounded-xl text-slate-500"
                  onClick={() => {
                    setShowVerifyPrompt(false)
                    router.push("/applications")
                  }}
                >
                  Maybe Later
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
