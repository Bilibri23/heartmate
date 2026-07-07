"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter, useParams } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { 
  Camera, 
  X, 
  MapPin,
  Home,
  Bed,
  Bath,
  Maximize,
  ChevronLeft,
  ChevronRight,
  CheckCircle,
  Loader2,
  Trash2,
  Video,
  Glasses,
  Upload,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import api, { uploadApi } from "@/lib/api"
import { SimilarListingsRail } from "@/components/listings/similar-listings-rail"

const CAMEROON_CITIES = [
  "Douala", "Yaoundé", "Bamenda", "Bafoussam", "Garoua",
  "Maroua", "Ngaoundéré", "Bertoua", "Limbe", "Buea", "Kribi", "Ebolowa"
]

const PROPERTY_TYPES = [
  { value: "STUDIO", label: "Studio" },
  { value: "APARTMENT", label: "Apartment" },
  { value: "HOUSE", label: "House" },
  { value: "PRIVATE_ROOM", label: "Private Room" },
  { value: "SHARED_ROOM", label: "Shared Room" },
]

const AMENITIES_BY_CATEGORY: Record<string, string[]> = {
  "Essentials": ["WiFi", "Furnished", "Air Conditioning", "Ceiling Fan", "Hot Water", "Water Tank", "Generator"],
  "Rooms & Spaces": ["Kitchen", "Living Room", "Dining Room", "Balcony", "Terrace", "Garden", "Storage Room", "Wardrobe"],
  "Appliances": ["Refrigerator", "Washing Machine", "Dryer", "Microwave", "Stove/Oven", "TV", "Iron"],
  "Building": ["Parking", "Garage", "Elevator", "Swimming Pool", "Gym", "Rooftop Access"],
  "Safety & Security": ["Security", "CCTV", "Security Guard", "Gated Compound", "Fire Extinguisher", "Smoke Detector"],
  "Utilities": ["Electricity (ENEO)", "Running Water (CDE)", "Solar Panels", "Septic Tank", "Laundry Area"],
  "Nearby": ["Near University", "Near Hospital", "Near Market", "Near Bus Stop", "Near Main Road"],
}

interface ExistingPhoto {
  id: string
  photoUrl: string
  isPrimary: boolean
}

interface VideoTour {
  videoTourUrl?: string
  videoTourThumbnail?: string
  videoTourDuration?: number
}

export default function EditListingPage() {
  const { t, formatCurrency, language } = useLanguage()
  const { user } = useAuth()
  const basePath = user?.role === "REALTOR" ? "/realtor" : "/landlord"
  const router = useRouter()
  const params = useParams()
  const listingId = params.id as string
  
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [existingPhotos, setExistingPhotos] = useState<ExistingPhoto[]>([])
  const [newPhotos, setNewPhotos] = useState<{ file: File; preview: string }[]>([])
  const [photosToDelete, setPhotosToDelete] = useState<string[]>([])
  const [existingVideoTour, setExistingVideoTour] = useState<VideoTour | null>(null)
  const [newVideoTour, setNewVideoTour] = useState<{ file: File; preview: string; duration?: number } | null>(null)
  const [existingOwnershipDocUrl, setExistingOwnershipDocUrl] = useState<string | null>(null)
  const [newOwnershipDoc, setNewOwnershipDoc] = useState<File | null>(null)
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    propertyType: "",
    city: "",
    neighborhood: "",
    address: "",
    distanceToUniversity: "",
    rentAmount: "",
    depositAmount: "",
    bedrooms: "1",
    bathrooms: "1",
    size: "",
    amenities: [] as string[],
    isUnfurnished: true,
    roomPreviewEnabled: false,
    roomLengthMeters: "",
    roomWidthMeters: "",
    roomHeightMeters: "",
    roomPreviewPhotoUrl: "",
  })

  const fetchListing = useCallback(async () => {
    if (!listingId) return
    
    setIsLoading(true)
    try {
      const response = await api.get(`/listings/${listingId}`)
      const listing = response.data
      
      setFormData({
        title: listing.title || "",
        description: listing.description || "",
        propertyType: listing.propertyType || "",
        city: listing.city || "",
        neighborhood: listing.neighborhood || "",
        address: listing.address || "",
        distanceToUniversity: listing.distanceToUniversity?.toString() || "",
        rentAmount: listing.rentAmount?.toString() || "",
        depositAmount: (listing.deposit || listing.depositAmount)?.toString() || "",
        bedrooms: listing.bedrooms?.toString() || "1",
        bathrooms: listing.bathrooms?.toString() || "1",
        size: (listing.squareMeters || listing.size)?.toString() || "",
        amenities: listing.amenities || [],
        isUnfurnished: listing.isUnfurnished ?? !(listing.amenities || []).includes("Furnished"),
        roomPreviewEnabled: Boolean(listing.roomPreviewEnabled),
        roomLengthMeters: listing.roomLengthMeters?.toString() || "",
        roomWidthMeters: listing.roomWidthMeters?.toString() || "",
        roomHeightMeters: listing.roomHeightMeters?.toString() || "",
        roomPreviewPhotoUrl: listing.roomPreviewPhotoUrl || "",
      })
      
      setExistingPhotos(listing.photos || [])
      setExistingOwnershipDocUrl(listing.ownershipDocumentUrl || null)

      // Set existing video tour
      if (listing.videoTourUrl || listing.videoTour) {
        setExistingVideoTour({
          videoTourUrl: listing.videoTourUrl || listing.videoTour?.videoUrl,
          videoTourThumbnail: listing.videoTourThumbnail || listing.videoTour?.thumbnailUrl,
          videoTourDuration: listing.videoTourDuration || listing.videoTour?.duration
        })
      }
    } catch (err) {
      console.error("Failed to fetch listing:", err)
      router.push(`${basePath}/listings`)
    } finally {
      setIsLoading(false)
    }
  }, [listingId, router])

  useEffect(() => {
    fetchListing()
  }, [fetchListing])

  const handlePhotoAdd = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files) return

    const newPhotosList = Array.from(files).map(file => ({
      file,
      preview: URL.createObjectURL(file)
    }))

    const totalPhotos = existingPhotos.length - photosToDelete.length + newPhotos.length + newPhotosList.length
    if (totalPhotos <= 10) {
      setNewPhotos(prev => [...prev, ...newPhotosList])
    }
  }

  const handleExistingPhotoRemove = (photoId: string) => {
    setPhotosToDelete(prev => [...prev, photoId])
  }

  const handleNewPhotoRemove = (index: number) => {
    setNewPhotos(prev => prev.filter((_, i) => i !== index))
  }

  const handleVideoAdd = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      setNewVideoTour({
        file,
        preview: URL.createObjectURL(file),
        duration: Math.round(video.duration)
      })
    }
    video.src = URL.createObjectURL(file)
  }

  const handleVideoRemove = () => {
    if (newVideoTour) {
      URL.revokeObjectURL(newVideoTour.preview)
      setNewVideoTour(null)
    }
  }

  const handleExistingVideoRemove = () => {
    setExistingVideoTour(null)
  }

  const toggleAmenity = (amenity: string) => {
    setFormData(prev => ({
      ...prev,
      amenities: prev.amenities.includes(amenity)
        ? prev.amenities.filter(a => a !== amenity)
        : [...prev.amenities, amenity]
    }))
  }

  const handleSubmit = async () => {
    if (!user?.id || !listingId) return

    setIsSubmitting(true)
    try {
      let ownershipDocumentUrl = existingOwnershipDocUrl || undefined
      if (newOwnershipDoc) {
        const docFormData = new FormData()
        docFormData.append("file", newOwnershipDoc)
        const docRes = await uploadApi.post("/upload/profile-photo", docFormData)
        const docUrl = docRes.data?.data ?? docRes.data?.url
        if (typeof docUrl === "string" && docUrl.startsWith("http")) {
          ownershipDocumentUrl = docUrl
        }
      }

      // Update listing data
      const listingData = {
        title: formData.title,
        description: formData.description,
        propertyType: formData.propertyType,
        city: formData.city,
        neighborhood: formData.neighborhood,
        address: formData.address,
        distanceToUniversity: formData.distanceToUniversity ? parseFloat(formData.distanceToUniversity) : null,
        rentAmount: parseInt(formData.rentAmount),
        deposit: parseInt(formData.depositAmount) || parseInt(formData.rentAmount),
        bedrooms: parseInt(formData.bedrooms),
        bathrooms: parseInt(formData.bathrooms),
        squareMeters: formData.size ? parseInt(formData.size) : null,
        amenities: formData.amenities,
        isUnfurnished: formData.isUnfurnished,
        roomPreviewEnabled: formData.roomPreviewEnabled,
        roomLengthMeters: formData.roomLengthMeters ? parseFloat(formData.roomLengthMeters) : null,
        roomWidthMeters: formData.roomWidthMeters ? parseFloat(formData.roomWidthMeters) : null,
        roomHeightMeters: formData.roomHeightMeters ? parseFloat(formData.roomHeightMeters) : null,
        roomPreviewPhotoUrl: formData.roomPreviewPhotoUrl,
        ownershipDocumentUrl,
      }

      await api.put(`/listings/${listingId}`, listingData, {
        params: { landlordId: user.id }
      })

      // Delete marked photos
      for (const photoId of photosToDelete) {
        await api.delete(`/listings/${listingId}/photos/${photoId}`, {
          params: { landlordId: user.id }
        }).catch(() => {})
      }

      // Upload new photos
      for (let i = 0; i < newPhotos.length; i++) {
        const photoFormData = new FormData()
        photoFormData.append("file", newPhotos[i].file)
        
        await uploadApi.post(`/listings/${listingId}/photos`, photoFormData, {
          params: { 
            landlordId: user.id,
            isPrimary: existingPhotos.length === 0 && photosToDelete.length === existingPhotos.length && i === 0
          }
          // Don't set Content-Type - let Axios handle it automatically for FormData
        }).catch(() => {})
      }

      // Upload new video tour if provided
      if (newVideoTour) {
        const videoFormData = new FormData()
        videoFormData.append("file", newVideoTour.file)
        await uploadApi.post(`/listings/${listingId}/video-tour`, videoFormData, {
          params: { landlordId: user.id }
        }).catch(() => {})
      }

      router.push(`${basePath}/listings`)
    } catch (err) {
      console.error("Failed to update listing:", err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = async () => {
    if (!user?.id || !listingId) return
    
    setIsDeleting(true)
    try {
      await api.delete(`/listings/${listingId}`, {
        params: { landlordId: user.id }
      })
      router.push(`${basePath}/listings`)
    } catch (err) {
      console.error("Failed to delete listing:", err)
    } finally {
      setIsDeleting(false)
    }
  }

  const isValid = formData.title && formData.propertyType && formData.city && 
                  formData.neighborhood && formData.rentAmount

  if (isLoading) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Edit Listing" />
        <div className="p-4 space-y-4">
          <Skeleton className="h-32 rounded-2xl" />
          <Skeleton className="h-12 rounded-xl" />
          <Skeleton className="h-12 rounded-xl" />
          <Skeleton className="h-24 rounded-xl" />
        </div>
      </div>
    )
  }

  const visibleExistingPhotos = existingPhotos.filter(p => !photosToDelete.includes(p.id))
  const totalPhotos = visibleExistingPhotos.length + newPhotos.length

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Edit Listing" />

      <div className="flex-1 overflow-y-auto">
        <div className="p-4 space-y-6">
          {/* Photos */}
          <div>
            <Label className="text-slate-700 mb-3 block">Photos ({totalPhotos}/10)</Label>
            <div className="grid grid-cols-3 gap-2">
              {/* Existing Photos */}
              {visibleExistingPhotos.map((photo, index) => (
                <div key={photo.id} className="relative aspect-square rounded-xl overflow-hidden bg-slate-100">
                  <img src={photo.photoUrl} alt="" className="w-full h-full object-cover" />
                  <button
                    onClick={() => handleExistingPhotoRemove(photo.id)}
                    className="absolute top-1 right-1 h-6 w-6 rounded-full bg-black/50 flex items-center justify-center"
                  >
                    <X className="h-3 w-3 text-white" />
                  </button>
                  {photo.isPrimary && (
                    <span className="absolute bottom-1 left-1 px-1.5 py-0.5 rounded bg-blue-600 text-white text-[10px]">
                      Cover
                    </span>
                  )}
                </div>
              ))}
              
              {/* New Photos */}
              {newPhotos.map((photo, index) => (
                <div key={`new-${index}`} className="relative aspect-square rounded-xl overflow-hidden bg-slate-100">
                  <img src={photo.preview} alt="" className="w-full h-full object-cover" />
                  <button
                    onClick={() => handleNewPhotoRemove(index)}
                    className="absolute top-1 right-1 h-6 w-6 rounded-full bg-black/50 flex items-center justify-center"
                  >
                    <X className="h-3 w-3 text-white" />
                  </button>
                  <span className="absolute bottom-1 left-1 px-1.5 py-0.5 rounded bg-emerald-600 text-white text-[10px]">
                    New
                  </span>
                </div>
              ))}
              
              {/* Add Photo Button */}
              {totalPhotos < 10 && (
                <label className="aspect-square rounded-xl border-2 border-dashed border-slate-300 flex flex-col items-center justify-center cursor-pointer hover:border-blue-500 hover:bg-blue-50 transition-colors">
                  <Camera className="h-6 w-6 text-slate-400" />
                  <span className="text-xs text-slate-500 mt-1">Add</span>
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={handlePhotoAdd}
                    className="hidden"
                  />
                </label>
              )}
            </div>
          </div>

          <div className="bg-white rounded-2xl p-4 shadow-sm space-y-2">
            <Label className="text-slate-700 flex items-center gap-2">Proof of ownership (optional)</Label>
            <p className="text-xs text-slate-500">
              Speeds up admin verification — a title deed, lease, or agency mandate letter.
            </p>
            {existingOwnershipDocUrl && !newOwnershipDoc && (
              <a
                href={existingOwnershipDocUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="block text-sm text-blue-600 underline"
              >
                View current document
              </a>
            )}
            <label className="flex items-center justify-center gap-2 p-4 border-2 border-dashed border-slate-300 rounded-xl cursor-pointer hover:border-blue-500 hover:bg-blue-50 transition-colors">
              <Upload className="h-5 w-5 text-slate-400" />
              <span className="text-sm text-slate-500">
                {newOwnershipDoc ? newOwnershipDoc.name : existingOwnershipDocUrl ? "Replace document" : "Choose a file"}
              </span>
              <input
                type="file"
                accept="image/*,.pdf"
                onChange={(e) => setNewOwnershipDoc(e.target.files?.[0] || null)}
                className="hidden"
              />
            </label>
          </div>

          <p className="text-xs leading-relaxed text-slate-600 bg-white border border-slate-200 rounded-xl px-3 py-2.5">
            {t.landlordJourney.compsCaption}
          </p>

          <SimilarListingsRail
            seedListingId={listingId}
            purpose="comps"
            userId={user?.id ?? null}
            title={t.discovery.compsTitle}
            lang={language === "fr" ? "fr" : "en"}
            className="border border-slate-200 rounded-2xl p-4 bg-slate-50/80"
          />

          {/* Virtual Tour */}
          <div className="space-y-4">
            <div>
              <h3 className="font-semibold text-slate-900">{t.landlordForm.virtualTourOptionalTitle}</h3>
              <p className="mt-1 text-sm text-slate-600">{t.landlordForm.virtualTourDesc}</p>
            </div>
            <div className="flex gap-3 rounded-2xl border border-violet-200 bg-gradient-to-br from-violet-50/90 to-slate-50 p-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-violet-600 text-white">
                <Glasses className="h-5 w-5" aria-hidden />
              </div>
              <div className="min-w-0 space-y-2">
                <p className="text-[10px] font-bold uppercase tracking-wide text-violet-900">
                  {t.landlordJourney.immersiveBadge}
                </p>
                <p className="text-xs leading-relaxed text-slate-700">{t.landlordForm.videoCardArCaption}</p>
              </div>
            </div>

            {/* Existing Video Tour */}
            {existingVideoTour && (
              <div className="space-y-3">
                <div className="relative rounded-xl overflow-hidden bg-black">
                  <video 
                    src={existingVideoTour.videoTourUrl} 
                    className="w-full h-48 object-contain"
                    controls
                    poster={existingVideoTour.videoTourThumbnail}
                  />
                  <div className="absolute top-2 left-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white px-3 py-1 rounded-full text-xs font-medium">
                    Current Virtual Tour
                  </div>
                  <Button
                    size="sm"
                    variant="destructive"
                    className="absolute top-2 right-2"
                    onClick={handleExistingVideoRemove}
                  >
                    Remove
                  </Button>
                </div>
                <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-xl p-3">
                  <div className="flex items-center gap-2 text-blue-800">
                    <Video className="h-4 w-4" />
                    <span className="font-medium text-sm">Current virtual tour</span>
                  </div>
                  {existingVideoTour.videoTourDuration && (
                    <p className="text-xs text-blue-600 mt-1">
                      Duration: {Math.floor(existingVideoTour.videoTourDuration / 60)}:{(existingVideoTour.videoTourDuration % 60).toString().padStart(2, '0')}
                    </p>
                  )}
                </div>
              </div>
            )}

            {/* New Video Tour */}
            {newVideoTour && (
              <div className="space-y-3">
                <div className="relative rounded-xl overflow-hidden bg-black">
                  <video 
                    src={newVideoTour.preview} 
                    className="w-full h-48 object-contain"
                    controls
                  />
                  <div className="absolute top-2 left-2 bg-gradient-to-r from-green-600 to-blue-600 text-white px-3 py-1 rounded-full text-xs font-medium">
                    New Virtual Tour
                  </div>
                  <Button
                    size="sm"
                    variant="destructive"
                    className="absolute top-2 right-2"
                    onClick={handleVideoRemove}
                  >
                    Remove
                  </Button>
                </div>
                <div className="bg-gradient-to-r from-green-50 to-blue-50 rounded-xl p-3">
                  <div className="flex items-center gap-2 text-green-800">
                    <Video className="h-4 w-4" />
                    <span className="font-medium text-sm">New virtual tour ready</span>
                  </div>
                  {newVideoTour.duration && (
                    <p className="text-xs text-green-600 mt-1">
                      Duration: {Math.floor(newVideoTour.duration / 60)}:{(newVideoTour.duration % 60).toString().padStart(2, '0')}
                    </p>
                  )}
                </div>
              </div>
            )}

            {/* Add Video Tour Button */}
            {!existingVideoTour && !newVideoTour && (
              <div className="border-2 border-dashed border-slate-300 rounded-xl p-6 text-center hover:border-blue-400 transition-colors">
                <Video className="h-8 w-8 text-slate-400 mx-auto mb-3" />
                <h4 className="text-sm font-medium text-slate-900 mb-2">{t.landlordForm.videoWalkthrough}</h4>
                <p className="text-xs text-slate-500 mb-4">{t.landlordForm.uploadVideo}</p>
                
                <div className="grid grid-cols-2 gap-2 mb-4">
                  <div className="bg-slate-50 rounded-lg p-2">
                    <div className="text-xs font-medium text-slate-900">📹 Standard Video</div>
                    <div className="text-xs text-slate-500">Traditional walkthrough</div>
                  </div>
                  <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-lg p-2 border border-blue-200">
                    <div className="text-xs font-medium text-slate-900">🌐 360° Tour</div>
                    <div className="text-xs text-slate-500">Interactive 3D experience</div>
                  </div>
                </div>

                <input
                  type="file"
                  accept="video/*,.mp4,.mov,.avi,.360"
                  onChange={handleVideoAdd}
                  className="hidden"
                  id="video-upload"
                />
                <Button asChild variant="outline" size="sm" className="rounded-xl">
                  <label htmlFor="video-upload" className="cursor-pointer">
                    Choose Video
                  </label>
                </Button>
                
                <div className="mt-3 text-xs text-slate-400">
                  <p>✅ Supports MP4, MOV, AVI, 360° videos</p>
                  <p>✅ Maximum file size: 100MB</p>
                </div>
              </div>
            )}
          </div>

          {/* Basic Info */}
          <div className="space-y-4">
            <h3 className="font-semibold text-slate-900">Basic Information</h3>
            
            <div>
              <Label className="text-slate-700 mb-2 block">Title *</Label>
              <Input
                placeholder="e.g., Modern Studio in Bonapriso"
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                className="h-12 rounded-xl"
              />
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block">Description</Label>
              <Textarea
                placeholder="Describe your property..."
                value={formData.description}
                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                className="min-h-[100px] rounded-xl resize-none"
              />
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block">Property Type *</Label>
              <Select
                value={formData.propertyType}
                onValueChange={(value) => setFormData(prev => ({ ...prev, propertyType: value }))}
              >
                <SelectTrigger className="h-12 rounded-xl">
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  {PROPERTY_TYPES.map((type) => (
                    <SelectItem key={type.value} value={type.value}>
                      {type.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Furnished Toggle */}
          <div className="space-y-3">
            <h3 className="font-semibold text-slate-900">{language === "fr" ? "Meublé ?" : "Furnished?"}</h3>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setFormData(prev => ({
                  ...prev,
                  isUnfurnished: false,
                  amenities: prev.amenities.includes("Furnished") ? prev.amenities : [...prev.amenities, "Furnished"]
                }))}
                className={`p-4 rounded-xl border-2 text-center transition-all ${formData.amenities.includes("Furnished") ? "border-blue-600 bg-blue-50" : "border-slate-200 hover:border-slate-300"}`}
              >
                <span className="text-2xl block mb-1">🛋️</span>
                <span className={`font-medium text-sm ${formData.amenities.includes("Furnished") ? "text-blue-700" : "text-slate-700"}`}>
                  {language === "fr" ? "Meublé" : "Furnished"}
                </span>
              </button>
              <button
                type="button"
                onClick={() => setFormData(prev => ({
                  ...prev,
                  isUnfurnished: true,
                  amenities: prev.amenities.filter(a => a !== "Furnished")
                }))}
                className={`p-4 rounded-xl border-2 text-center transition-all ${!formData.amenities.includes("Furnished") ? "border-blue-600 bg-blue-50" : "border-slate-200 hover:border-slate-300"}`}
              >
                <span className="text-2xl block mb-1">🏠</span>
                <span className={`font-medium text-sm ${!formData.amenities.includes("Furnished") ? "text-blue-700" : "text-slate-700"}`}>
                  {language === "fr" ? "Non meublé" : "Unfurnished"}
                </span>
              </button>
            </div>
          </div>

          {/* Room Preview */}
          <div className="space-y-3 rounded-2xl border border-sky-100 bg-sky-50/70 p-4">
            <div>
              <h3 className="font-semibold text-slate-900">Help tenants imagine this empty room</h3>
              <p className="mt-1 text-sm leading-relaxed text-slate-600">
                Room Preview works best with a clear photo of the main room taken from a corner or doorway.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setFormData(prev => ({
                ...prev,
                roomPreviewEnabled: !prev.roomPreviewEnabled,
                roomPreviewPhotoUrl: !prev.roomPreviewEnabled && !prev.roomPreviewPhotoUrl && visibleExistingPhotos[0]
                  ? visibleExistingPhotos[0].photoUrl
                  : prev.roomPreviewPhotoUrl,
              }))}
              className={`w-full rounded-xl border-2 p-3 text-left transition-all ${formData.roomPreviewEnabled ? "border-sky-600 bg-white" : "border-slate-200 bg-white/70"}`}
            >
              <span className="block text-sm font-semibold text-slate-900">Enable Room Preview</span>
              <span className="text-xs text-slate-500">Tenants can preview furniture layouts over your selected room photo.</span>
            </button>
            {formData.roomPreviewEnabled && (
              <div className="space-y-3">
                <div className="grid grid-cols-3 gap-2">
                  <Input type="number" min="0" step="0.1" placeholder="Length m" value={formData.roomLengthMeters} onChange={(e) => setFormData(prev => ({ ...prev, roomLengthMeters: e.target.value }))} className="h-11 rounded-xl" />
                  <Input type="number" min="0" step="0.1" placeholder="Width m" value={formData.roomWidthMeters} onChange={(e) => setFormData(prev => ({ ...prev, roomWidthMeters: e.target.value }))} className="h-11 rounded-xl" />
                  <Input type="number" min="0" step="0.1" placeholder="Height" value={formData.roomHeightMeters} onChange={(e) => setFormData(prev => ({ ...prev, roomHeightMeters: e.target.value }))} className="h-11 rounded-xl" />
                </div>
                <p className="text-xs font-medium text-sky-700">
                  {formData.roomLengthMeters && formData.roomWidthMeters ? "Uses landlord-provided dimensions" : "Approximate layout only"}
                </p>
                {visibleExistingPhotos.length > 0 ? (
                  <div className="space-y-2">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Best room photo for preview</p>
                    <div className="flex gap-2 overflow-x-auto pb-1">
                      {visibleExistingPhotos.map((photo) => (
                        <button
                          key={photo.id}
                          type="button"
                          onClick={() => setFormData(prev => ({ ...prev, roomPreviewPhotoUrl: photo.photoUrl }))}
                          className={`relative h-20 w-20 shrink-0 overflow-hidden rounded-xl border-2 ${formData.roomPreviewPhotoUrl === photo.photoUrl ? "border-sky-600" : "border-transparent"}`}
                        >
                          <img src={photo.photoUrl} alt="" className="h-full w-full object-cover" />
                        </button>
                      ))}
                    </div>
                  </div>
                ) : (
                  <p className="rounded-xl bg-white p-3 text-sm text-slate-500">Upload at least one listing photo before enabling Room Preview.</p>
                )}
              </div>
            )}
          </div>

          {/* Location */}
          <div className="space-y-4">
            <h3 className="font-semibold text-slate-900">Location</h3>
            
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label className="text-slate-700 mb-2 block">City *</Label>
                <Select
                  value={formData.city}
                  onValueChange={(value) => setFormData(prev => ({ ...prev, city: value }))}
                >
                  <SelectTrigger className="h-12 rounded-xl">
                    <SelectValue placeholder="Select city" />
                  </SelectTrigger>
                  <SelectContent>
                    {CAMEROON_CITIES.map((city) => (
                      <SelectItem key={city} value={city}>{city}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-slate-700 mb-2 block">Neighborhood *</Label>
                <Input
                  placeholder="e.g., Bonapriso"
                  value={formData.neighborhood}
                  onChange={(e) => setFormData(prev => ({ ...prev, neighborhood: e.target.value }))}
                  className="h-12 rounded-xl"
                />
              </div>
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block">Address</Label>
              <Input
                placeholder="Street address (optional)"
                value={formData.address}
                onChange={(e) => setFormData(prev => ({ ...prev, address: e.target.value }))}
                className="h-12 rounded-xl"
              />
            </div>

            <div>
              <Label className="text-slate-700 mb-2 block">Distance to Campus (km)</Label>
              <Input
                type="number"
                placeholder="e.g., 2.5"
                value={formData.distanceToUniversity}
                onChange={(e) => setFormData(prev => ({ ...prev, distanceToUniversity: e.target.value }))}
                className="h-12 rounded-xl"
              />
            </div>
          </div>

          {/* Pricing */}
          <div className="space-y-4">
            <h3 className="font-semibold text-slate-900">Pricing & Specs</h3>
            
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label className="text-slate-700 mb-2 block">Rent (FCFA) *</Label>
                <Input
                  type="number"
                  placeholder="75000"
                  value={formData.rentAmount}
                  onChange={(e) => setFormData(prev => ({ ...prev, rentAmount: e.target.value }))}
                  className="h-12 rounded-xl"
                />
              </div>

              <div>
                <Label className="text-slate-700 mb-2 block">Deposit (FCFA)</Label>
                <Input
                  type="number"
                  placeholder="Same as rent"
                  value={formData.depositAmount}
                  onChange={(e) => setFormData(prev => ({ ...prev, depositAmount: e.target.value }))}
                  className="h-12 rounded-xl"
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div>
                <Label className="text-slate-700 mb-2 block flex items-center gap-1">
                  <Bed className="h-4 w-4" /> Beds
                </Label>
                <Select
                  value={formData.bedrooms}
                  onValueChange={(value) => setFormData(prev => ({ ...prev, bedrooms: value }))}
                >
                  <SelectTrigger className="h-12 rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[1, 2, 3, 4, 5, 6].map((num) => (
                      <SelectItem key={num} value={num.toString()}>{num}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-slate-700 mb-2 block flex items-center gap-1">
                  <Bath className="h-4 w-4" /> Baths
                </Label>
                <Select
                  value={formData.bathrooms}
                  onValueChange={(value) => setFormData(prev => ({ ...prev, bathrooms: value }))}
                >
                  <SelectTrigger className="h-12 rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[1, 2, 3, 4].map((num) => (
                      <SelectItem key={num} value={num.toString()}>{num}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label className="text-slate-700 mb-2 block flex items-center gap-1">
                  <Maximize className="h-4 w-4" /> m²
                </Label>
                <Input
                  type="number"
                  placeholder="25"
                  value={formData.size}
                  onChange={(e) => setFormData(prev => ({ ...prev, size: e.target.value }))}
                  className="h-12 rounded-xl"
                />
              </div>
            </div>
          </div>

          {/* Amenities */}
          <div className="space-y-4">
            <h3 className="font-semibold text-slate-900">Amenities & Features</h3>
            {Object.entries(AMENITIES_BY_CATEGORY).map(([category, amenities]) => (
              <div key={category} className="space-y-2">
                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{category}</p>
                <div className="flex flex-wrap gap-2">
                  {amenities.map((amenity) => (
                    <button
                      key={amenity}
                      onClick={() => toggleAmenity(amenity)}
                      className={`px-3 py-2 rounded-full text-sm font-medium transition-colors ${
                        formData.amenities.includes(amenity)
                          ? "bg-blue-600 text-white"
                          : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                      }`}
                    >
                      {amenity}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>

          {/* Actions */}
          <div className="pb-8 space-y-3">
            <Button
              className="w-full h-14 rounded-xl text-base"
              onClick={handleSubmit}
              disabled={isSubmitting || !isValid}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-5 w-5 mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                "Save Changes"
              )}
            </Button>
            
            <Button
              variant="outline"
              className="w-full h-12 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
              onClick={() => setShowDeleteDialog(true)}
            >
              <Trash2 className="h-5 w-5 mr-2" />
              Delete Listing
            </Button>
          </div>
        </div>
      </div>

      {/* Delete Confirmation */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Listing?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the listing
              and all associated data.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-red-600 hover:bg-red-700"
              disabled={isDeleting}
            >
              {isDeleting ? "Deleting..." : "Delete"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
