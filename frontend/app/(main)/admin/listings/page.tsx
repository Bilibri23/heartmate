"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { NextStepCard } from "@/components/workflows/next-step-card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import api from "@/lib/api"
import { 
  Search,
  Home,
  CheckCircle,
  XCircle,
  MapPin,
  User,
  Calendar,
  Star,
  ExternalLink,
  ImageIcon,
  AlertTriangle
} from "lucide-react"

interface ListingPhoto {
  id: string
  photoUrl: string
  isPrimary?: boolean
  displayOrder?: number
}

interface Listing {
  id: string
  title: string
  description: string
  address: string
  city: string
  rentAmount: number
  salePrice?: number
  listingPurpose?: string
  propertyType?: string
  furnishingStatus?: string
  isSharedAccommodation?: boolean
  status: string
  landlordId: string
  landlordName: string
  landlordEmail?: string
  photos?: ListingPhoto[]
  images?: string[]
  primaryPhotoUrl?: string
  bedrooms: number
  bathrooms: number
  amenities: string[]
  isUnfurnished?: boolean
  roomPreviewEnabled?: boolean
  roomLengthMeters?: number
  roomWidthMeters?: number
  roomHeightMeters?: number
  roomPreviewPhotoUrl?: string
  roomPreviewStatus?: string
  createdAt: string
  featured: boolean
}

export default function AdminListingsPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  
  const [listings, setListings] = useState<Listing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [selectedListing, setSelectedListing] = useState<Listing | null>(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)
  const [rejectionReason, setRejectionReason] = useState("")
  const [authLoading, setAuthLoading] = useState(true)

  useEffect(() => {
    if (user === null && authLoading) {
      const timer = setTimeout(() => setAuthLoading(false), 500)
      return () => clearTimeout(timer)
    }
    if (user) {
      setAuthLoading(false)
      if (user.role !== "ADMIN") { router.push("/for-you"); return }
      fetchListings()
    } else if (!authLoading) {
      router.push("/login")
    }
  }, [user, router, authLoading])

  const fetchListings = async () => {
    setIsLoading(true)
    try {
      let response
      
      // Fetch all listings with optional status filter
      if (statusFilter === "all") {
        response = await api.get("/admin/listings")
      } else {
        response = await api.get(`/admin/listings?status=${statusFilter}`)
      }
      
      let data = response.data || []
      setListings(data)
    } catch (err) {
      console.error("Failed to fetch listings:", err)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    if (user?.role === "ADMIN") {
      fetchListings()
    }
  }, [statusFilter])

  const handleApprove = async (listingId: string) => {
    try {
      await api.post(`/admin/listings/${listingId}/approve`, {
        status: "ACTIVE",
        rejectionReason: null
      })
      fetchListings()
      setIsDetailOpen(false)
    } catch (err) {
      console.error("Failed to approve listing:", err)
    }
  }

  const handleReject = async (listingId: string) => {
    try {
      await api.post(`/admin/listings/${listingId}/approve`, {
        status: "REJECTED",
        rejectionReason: rejectionReason || "Listing rejected by admin"
      })
      setRejectionReason("")
      fetchListings()
      setIsDetailOpen(false)
    } catch (err) {
      console.error("Failed to reject listing:", err)
    }
  }

  const handleFeature = async (listingId: string, featured: boolean) => {
    try {
      await api.post(`/admin/listings/${listingId}/approve`, {
        status: "ACTIVE",
        featured: featured
      })
      fetchListings()
    } catch (err) {
      console.error("Failed to update featured status:", err)
    }
  }

  const handleRoomPreviewStatus = async (listingId: string, roomPreviewStatus: "APPROVED" | "NEEDS_BETTER_PHOTO") => {
    try {
      await api.post(`/admin/listings/${listingId}/room-preview-status`, { roomPreviewStatus })
      fetchListings()
      setSelectedListing(prev => prev ? { ...prev, roomPreviewStatus } : prev)
    } catch (err) {
      console.error("Failed to update Room Preview status:", err)
    }
  }

  const filteredListings = listings.filter(listing => {
    const matchesSearch = listing.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      listing.city.toLowerCase().includes(searchQuery.toLowerCase()) ||
      listing.landlordName?.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesStatus = statusFilter === "all" || listing.status === statusFilter.toUpperCase()
    return matchesSearch && matchesStatus
  })

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return <span className="px-2 py-1 bg-green-100 text-green-700 rounded-full text-xs font-medium">Active</span>
      case "PENDING":
        return <span className="px-2 py-1 bg-amber-100 text-amber-700 rounded-full text-xs font-medium">Pending</span>
      case "REJECTED":
        return <span className="px-2 py-1 bg-red-100 text-red-700 rounded-full text-xs font-medium">Rejected</span>
      case "RENTED":
        return <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded-full text-xs font-medium">Rented</span>
      default:
        return <span className="px-2 py-1 bg-slate-100 text-slate-700 rounded-full text-xs font-medium">{status}</span>
    }
  }

  if (user?.role !== "ADMIN") return null

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Listings Management" showBack />

      <div className="p-4 space-y-4">
        {/* Search and Filter */}
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder="Search listings..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 rounded-xl"
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-32 rounded-xl">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All</SelectItem>
              <SelectItem value="pending">Pending</SelectItem>
              <SelectItem value="active">Active</SelectItem>
              <SelectItem value="rejected">Rejected</SelectItem>
              <SelectItem value="rented">Rented</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-2">
          <div className="bg-white rounded-xl p-3 text-center">
            <p className="text-lg font-bold text-slate-900">{listings.length}</p>
            <p className="text-xs text-slate-500">Total</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center">
            <p className="text-lg font-bold text-amber-600">{listings.filter(l => l.status === "PENDING").length}</p>
            <p className="text-xs text-slate-500">Pending</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center">
            <p className="text-lg font-bold text-green-600">{listings.filter(l => l.status === "ACTIVE").length}</p>
            <p className="text-xs text-slate-500">Active</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center">
            <p className="text-lg font-bold text-red-600">{listings.filter(l => l.status === "REJECTED").length}</p>
            <p className="text-xs text-slate-500">Rejected</p>
          </div>
        </div>

        <NextStepCard
          title="What happens next?"
          body="Open pending listings, confirm photos, location, price, trust signals, and policy fit, then approve or reject with a clear reason. Approved listings become visible to tenants."
          icon={<CheckCircle className="h-5 w-5" />}
        />

        {/* Listings */}
        {isLoading ? (
          <div className="space-y-4">
            {[1, 2, 3].map(i => (
              <Skeleton key={i} className="h-32 rounded-2xl" />
            ))}
          </div>
        ) : filteredListings.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-2xl">
            <Home className="h-12 w-12 text-slate-300 mx-auto mb-4" />
            <h3 className="font-semibold text-slate-900">No listings found</h3>
            <p className="mx-auto mt-2 max-w-sm text-sm leading-relaxed text-slate-500">
              Listing review items appear here when landlords submit or update properties for marketplace visibility.
            </p>
          </div>
        ) : (
          <div className="space-y-4 pb-24">
            {filteredListings.map((listing) => (
              <div 
                key={listing.id} 
                className="bg-white rounded-2xl p-4 shadow-sm cursor-pointer"
                onClick={() => {
                  setSelectedListing(listing)
                  setIsDetailOpen(true)
                }}
              >
                <div className="flex gap-4">
                  {(listing.primaryPhotoUrl || listing.photos?.[0]?.photoUrl || listing.images?.[0]) ? (
                    <img 
                      src={listing.primaryPhotoUrl || listing.photos?.[0]?.photoUrl || listing.images?.[0]} 
                      alt={listing.title}
                      className="w-24 h-24 rounded-xl object-cover"
                    />
                  ) : (
                    <div className="w-24 h-24 rounded-xl bg-slate-100 flex items-center justify-center">
                      <Home className="h-8 w-8 text-slate-400" />
                    </div>
                  )}
                  <div className="flex-1">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-semibold text-slate-900">{listing.title}</h3>
                        <p className="text-sm text-slate-500 flex items-center gap-1">
                          <MapPin className="h-3 w-3" /> {listing.city}
                        </p>
                      </div>
                      {getStatusBadge(listing.status)}
                    </div>
                    <p className="text-blue-600 font-semibold mt-1">
                      {listing.listingPurpose === "SALE"
                        ? formatCurrency(listing.salePrice ?? listing.rentAmount)
                        : `${formatCurrency(listing.rentAmount)}/mo`}
                    </p>
                    <div className="mt-1 flex flex-wrap gap-1 text-[10px] font-semibold text-slate-600">
                      {listing.listingPurpose && <span className="rounded-full bg-slate-100 px-2 py-0.5">{listing.listingPurpose}</span>}
                      {listing.propertyType && <span className="rounded-full bg-slate-100 px-2 py-0.5">{listing.propertyType.replace(/_/g, " ")}</span>}
                      {listing.furnishingStatus && <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-emerald-700">{listing.furnishingStatus.replace(/_/g, " ")}</span>}
                      {listing.isSharedAccommodation && <span className="rounded-full bg-violet-50 px-2 py-0.5 text-violet-700">Shared</span>}
                    </div>
                    <div className="flex items-center gap-2 mt-2 text-xs text-slate-500">
                      <User className="h-3 w-3" />
                      <span>{listing.landlordName || "Unknown"}</span>
                      {listing.featured && (
                        <span className="flex items-center gap-1 text-amber-600">
                          <Star className="h-3 w-3 fill-amber-500" /> Featured
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Listing Detail Sheet */}
      <Sheet open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <SheetContent side="bottom" className="h-[90vh] rounded-t-3xl">
          <SheetHeader>
            <SheetTitle>Listing Details</SheetTitle>
          </SheetHeader>
          
          {selectedListing && (
            <div className="mt-4 space-y-4 overflow-y-auto max-h-[calc(90vh-120px)] pb-8">
              {/* Listing Photos - Admin must view all before approve/reject */}
              {(() => {
                const fromPhotos = selectedListing.photos?.map(p => p.photoUrl).filter(Boolean) || []
                const fromLegacy = selectedListing.images || []
                const merged = [...fromPhotos, ...fromLegacy]
                if (selectedListing.primaryPhotoUrl) merged.unshift(selectedListing.primaryPhotoUrl)
                const photoUrls = [...new Set(merged.filter(Boolean) as string[])]
                return photoUrls.length > 0 ? (
                  <div className="space-y-3">
                    <p className="text-sm font-medium text-slate-700 flex items-center gap-2">
                      <ImageIcon className="h-4 w-4" /> Uploaded Photos ({photoUrls.length})
                    </p>
                    <div className="flex gap-2 overflow-x-auto pb-2">
                      {photoUrls.map((url, idx) => (
                        <div key={idx} className="relative flex-shrink-0">
                          <img 
                            src={url} 
                            alt={`${selectedListing.title} ${idx + 1}`}
                            className="w-40 h-28 rounded-xl object-cover border border-slate-200"
                          />
                          <a 
                            href={url} 
                            target="_blank" 
                            rel="noopener noreferrer" 
                            className="absolute top-1 right-1 bg-white/90 backdrop-blur-sm p-1.5 rounded-lg shadow-sm flex items-center gap-1 text-xs text-slate-600 hover:bg-white"
                          >
                            <ExternalLink className="h-3 w-3" />
                          </a>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="bg-amber-50 rounded-xl p-4 flex items-center gap-2 text-amber-700">
                    <AlertTriangle className="h-5 w-5 flex-shrink-0" />
                    <p className="text-sm">No photos uploaded. Consider rejecting or asking landlord to add photos.</p>
                  </div>
                )
              })()}

              {/* Info */}
              <div className="space-y-3">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-xl font-bold text-slate-900">{selectedListing.title}</h2>
                    <p className="text-slate-500 flex items-center gap-1">
                      <MapPin className="h-4 w-4" /> {selectedListing.address}, {selectedListing.city}
                    </p>
                  </div>
                  {getStatusBadge(selectedListing.status)}
                </div>

                <p className="text-2xl font-bold text-blue-600">{formatCurrency(selectedListing.rentAmount)}/month</p>

                <div className="grid grid-cols-2 gap-4 bg-slate-50 rounded-xl p-4">
                  <div>
                    <p className="text-sm text-slate-500">Bedrooms</p>
                    <p className="font-semibold">{selectedListing.bedrooms}</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-500">Bathrooms</p>
                    <p className="font-semibold">{selectedListing.bathrooms}</p>
                  </div>
                </div>

                <div>
                  <p className="text-sm text-slate-500 mb-2">Description</p>
                  <p className="text-slate-700">{selectedListing.description}</p>
                </div>

                {selectedListing.amenities?.length > 0 && (
                  <div>
                    <p className="text-sm text-slate-500 mb-2">Amenities</p>
                    <div className="flex flex-wrap gap-2">
                      {selectedListing.amenities.map((amenity, idx) => (
                        <span key={idx} className="px-3 py-1 bg-slate-100 rounded-full text-sm">
                          {amenity}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <div className="rounded-xl border border-sky-100 bg-sky-50/70 p-4">
                  <p className="text-sm font-semibold text-slate-900">Room Preview</p>
                  <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
                    <div>
                      <p className="text-slate-500">Enabled</p>
                      <p className="font-semibold">{selectedListing.roomPreviewEnabled ? "Yes" : "No"}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Unfurnished</p>
                      <p className="font-semibold">{selectedListing.isUnfurnished ? "Yes" : "No"}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Dimensions</p>
                      <p className="font-semibold">
                        {selectedListing.roomLengthMeters && selectedListing.roomWidthMeters
                          ? `${selectedListing.roomLengthMeters}m x ${selectedListing.roomWidthMeters}m`
                          : "Approximate only"}
                      </p>
                    </div>
                    <div>
                      <p className="text-slate-500">Status</p>
                      <p className="font-semibold">{selectedListing.roomPreviewStatus || "NOT_ENABLED"}</p>
                    </div>
                  </div>
                  {selectedListing.roomPreviewPhotoUrl && (
                    <a href={selectedListing.roomPreviewPhotoUrl} target="_blank" rel="noopener noreferrer" className="mt-3 block overflow-hidden rounded-xl border border-sky-100">
                      <img src={selectedListing.roomPreviewPhotoUrl} alt="Room Preview asset" className="h-40 w-full object-cover" />
                    </a>
                  )}
                  {selectedListing.roomPreviewEnabled && (
                    <div className="mt-3 flex gap-2">
                      <Button type="button" variant="outline" className="flex-1 rounded-xl" onClick={() => handleRoomPreviewStatus(selectedListing.id, "NEEDS_BETTER_PHOTO")}>
                        Needs better photo
                      </Button>
                      <Button type="button" className="flex-1 rounded-xl bg-sky-600 hover:bg-sky-700" onClick={() => handleRoomPreviewStatus(selectedListing.id, "APPROVED")}>
                        Approve preview
                      </Button>
                    </div>
                  )}
                </div>

                <div className="bg-slate-50 rounded-xl p-4">
                  <p className="text-sm text-slate-500 mb-2">Landlord</p>
                  <p className="font-semibold">{selectedListing.landlordName}</p>
                  {selectedListing.landlordEmail && (
                    <p className="text-sm text-slate-500">{selectedListing.landlordEmail}</p>
                  )}
                </div>

                <div className="text-sm text-slate-500">
                  <Calendar className="h-4 w-4 inline mr-1" />
                  Submitted: {new Date(selectedListing.createdAt).toLocaleDateString()}
                </div>
              </div>

              {/* Actions */}
              {selectedListing.status === "PENDING" && (
                <div className="space-y-3 pt-4 border-t">
                  <Input
                    placeholder="Rejection reason (optional)"
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    className="rounded-xl"
                  />
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      className="flex-1 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
                      onClick={() => handleReject(selectedListing.id)}
                    >
                      <XCircle className="h-4 w-4 mr-1" />
                      Reject
                    </Button>
                    <Button
                      className="flex-1 rounded-xl bg-green-600 hover:bg-green-700"
                      onClick={() => handleApprove(selectedListing.id)}
                    >
                      <CheckCircle className="h-4 w-4 mr-1" />
                      Approve
                    </Button>
                  </div>
                </div>
              )}

              {selectedListing.status === "ACTIVE" && (
                <div className="pt-4 border-t">
                  <Button
                    variant="outline"
                    className="w-full rounded-xl"
                    onClick={() => handleFeature(selectedListing.id, !selectedListing.featured)}
                  >
                    <Star className={`h-4 w-4 mr-1 ${selectedListing.featured ? "fill-amber-500 text-amber-500" : ""}`} />
                    {selectedListing.featured ? "Remove from Featured" : "Mark as Featured"}
                  </Button>
                </div>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
