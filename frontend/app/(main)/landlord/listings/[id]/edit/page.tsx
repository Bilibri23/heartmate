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
  Trash2
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
import api from "@/lib/api"

const CAMEROON_CITIES = [
  "Douala", "Yaoundé", "Bamenda", "Bafoussam", "Garoua",
  "Maroua", "Ngaoundéré", "Bertoua", "Limbe", "Buea", "Kribi", "Ebolowa"
]

const PROPERTY_TYPES = [
  { value: "STUDIO", label: "Studio" },
  { value: "APARTMENT", label: "Apartment" },
  { value: "HOUSE", label: "House" },
  { value: "ROOM", label: "Room" },
  { value: "SHARED", label: "Shared Room" },
]

const AMENITIES = [
  "WiFi", "Air Conditioning", "Furnished", "Kitchen", "Parking",
  "Security", "Water Tank", "Generator", "Balcony", "Laundry"
]

interface ExistingPhoto {
  id: string
  photoUrl: string
  isPrimary: boolean
}

export default function EditListingPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
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
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    propertyType: "",
    city: "",
    neighborhood: "",
    address: "",
    rentAmount: "",
    depositAmount: "",
    bedrooms: "1",
    bathrooms: "1",
    size: "",
    amenities: [] as string[],
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
        rentAmount: listing.rentAmount?.toString() || "",
        depositAmount: listing.depositAmount?.toString() || "",
        bedrooms: listing.bedrooms?.toString() || "1",
        bathrooms: listing.bathrooms?.toString() || "1",
        size: listing.size?.toString() || "",
        amenities: listing.amenities || [],
      })
      
      setExistingPhotos(listing.photos || [])
    } catch (err) {
      console.error("Failed to fetch listing:", err)
      router.push("/landlord/listings")
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
      // Update listing data
      const listingData = {
        title: formData.title,
        description: formData.description,
        propertyType: formData.propertyType,
        city: formData.city,
        neighborhood: formData.neighborhood,
        address: formData.address,
        rentAmount: parseInt(formData.rentAmount),
        depositAmount: parseInt(formData.depositAmount) || parseInt(formData.rentAmount),
        bedrooms: parseInt(formData.bedrooms),
        bathrooms: parseInt(formData.bathrooms),
        size: formData.size ? parseInt(formData.size) : null,
        amenities: formData.amenities,
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
        
        await api.post(`/listings/${listingId}/photos`, photoFormData, {
          params: { 
            landlordId: user.id,
            isPrimary: existingPhotos.length === 0 && photosToDelete.length === existingPhotos.length && i === 0
          },
          headers: { "Content-Type": "multipart/form-data" }
        }).catch(() => {})
      }

      router.push("/landlord/listings")
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
      router.push("/landlord/listings")
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
            <h3 className="font-semibold text-slate-900">Amenities</h3>
            <div className="flex flex-wrap gap-2">
              {AMENITIES.map((amenity) => (
                <button
                  key={amenity}
                  onClick={() => toggleAmenity(amenity)}
                  className={`px-3 py-2 rounded-full text-sm font-medium transition-colors ${
                    formData.amenities.includes(amenity)
                      ? "bg-blue-600 text-white"
                      : "bg-slate-100 text-slate-600"
                  }`}
                >
                  {amenity}
                </button>
              ))}
            </div>
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
