"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Camera, X, MapPin, Bed, Bath, Maximize, CheckCircle, ImageIcon, FileText, DollarSign, Sparkles, Check, ArrowLeft, ArrowRight, Video, Image as ImageIcon2, Upload, Trash2, Glasses } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import api, { uploadApi } from "@/lib/api"
import { useProfileCompletion } from "@/hooks/use-profile-completion"
import { CompletionBanner } from "@/components/profile/completion-banner"

const CAMEROON_CITIES = ["Douala", "Yaounde", "Bamenda", "Bafoussam", "Garoua", "Maroua", "Ngaoundere", "Bertoua", "Limbe", "Buea", "Kribi", "Ebolowa"]
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
const ALL_AMENITIES = Object.values(AMENITIES_BY_CATEGORY).flat()
const STEPS = [{ id: 1, title: "Photos", icon: ImageIcon }, { id: 2, title: "Details", icon: FileText }, { id: 3, title: "Pricing", icon: DollarSign }, { id: 4, title: "Amenities", icon: Sparkles }, { id: 5, title: "Virtual Tour", icon: Video }]

interface PhotoItem { file: File; preview: string }
interface VirtualTourItem {
  file: File
  preview: string      // object URL for preview
  type: 'video' | '360'
}

interface ListingPreviewProps {
  photos: PhotoItem[]
  previewPhotoIndex: number
  onSelectPhoto: (index: number) => void
  formData: any
  formatCurrency: (value: number) => string
  t: any
  language: string
  className?: string
}

function ListingPreview({ photos, previewPhotoIndex, onSelectPhoto, formData, formatCurrency, t, language, className }: ListingPreviewProps) {
  return (
    <div className={`bg-white rounded-2xl shadow-sm overflow-hidden ${className ?? ""}`}>
      <div className="relative h-48 bg-slate-200">
        {photos.length > 0 ? (
          <div className="relative h-full">
            <img src={photos[previewPhotoIndex]?.preview} alt="Preview" className="h-full w-full object-cover" />
            {photos.length > 1 && (
              <div className="absolute bottom-2 left-1/2 flex -translate-x-1/2 gap-1">
                {photos.map((_, index) => (
                  <button
                    key={index}
                    onClick={() => onSelectPhoto(index)}
                    className={`h-1.5 rounded-full transition-all ${index === previewPhotoIndex ? "w-4 bg-white" : "w-1.5 bg-white/60"}`}
                    type="button"
                    aria-label={`Show photo ${index + 1}`}
                  />
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="flex h-full flex-col items-center justify-center gap-2">
            <Camera className="h-10 w-10 text-slate-300" />
            <p className="text-xs text-slate-400">{t.landlordForm.addPhotosCta}</p>
          </div>
        )}
        <div className="absolute top-2 left-2">
          <span className="rounded-full bg-blue-600 px-2 py-0.5 text-xs font-medium text-white">{t.landlordForm.livePreview}</span>
        </div>
      </div>
      <div className="space-y-3 p-3">
        <div>
          <p className="text-lg font-bold text-blue-600">
            {formData.rentAmount ? formatCurrency(parseInt(formData.rentAmount)) : "---"}
            {t.listings.perMonth}
          </p>
          <h1 className="line-clamp-1 text-sm font-semibold text-slate-900">{formData.title || t.landlordForm.yourListingTitle}</h1>
          <p className="flex items-center gap-1 text-xs text-slate-500">
            <MapPin className="h-3 w-3" />
            {formData.neighborhood || t.landlordForm.neighborhoodLabel}, {formData.city || t.search.city}
          </p>
        </div>
        <div className="flex gap-3 border-y border-slate-100 py-2 text-xs">
          {formData.amenities?.includes("Furnished") && (
            <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
              {language === "fr" ? "Meublé" : "Furnished"}
            </span>
          )}
          <div className="flex items-center gap-1">
            <Bed className="h-3.5 w-3.5 text-slate-400" />
            <span className="font-medium">{formData.bedrooms || "1"}</span>
          </div>
          <div className="flex items-center gap-1">
            <Bath className="h-3.5 w-3.5 text-slate-400" />
            <span className="font-medium">{formData.bathrooms || "1"}</span>
          </div>
          {formData.size && (
            <div className="flex items-center gap-1">
              <Maximize className="h-3.5 w-3.5 text-slate-400" />
              <span className="font-medium">{formData.size}m2</span>
            </div>
          )}
        </div>
        {formData.amenities?.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {formData.amenities.slice(0, 4).map((amenity: string) => (
              <span key={amenity} className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
                {amenity}
              </span>
            ))}
            {formData.amenities.length > 4 && (
              <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
                +{formData.amenities.length - 4}
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

export default function NewListingPage() {
  const { formatCurrency, language, t } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [currentStep, setCurrentStep] = useState(1)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [previewPhotoIndex, setPreviewPhotoIndex] = useState(0)
  const [photos, setPhotos] = useState<PhotoItem[]>([])
  const [virtualTour, setVirtualTour] = useState<VirtualTourItem | null>(null)
  const [tourUploadProgress, setTourUploadProgress] = useState<number>(0)
  const [formData, setFormData] = useState({ title: "", description: "", propertyType: "", city: "", neighborhood: "", address: "", rentAmount: "", depositAmount: "", bedrooms: "1", bathrooms: "1", size: "", amenities: [] as string[] })
  const { status: completionStatus } = useProfileCompletion()
  const canPublish = completionStatus?.operationEligibility?.LISTING_PUBLISH !== false
  const [showMobilePreview, setShowMobilePreview] = useState(false)

  const handlePhotoAdd = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files) return
    const newPhotos = Array.from(files).map(file => ({ file, preview: URL.createObjectURL(file) }))
    setPhotos(prev => [...prev, ...newPhotos].slice(0, 10))
  }

  const handlePhotoRemove = (index: number) => setPhotos(prev => prev.filter((_, i) => i !== index))

  const handleTourFileSelect = (file: File, type: 'video' | '360') => {
    const preview = URL.createObjectURL(file)
    setVirtualTour({ file, preview, type })
  }

  const handleVirtualTourRemove = () => {
    if (virtualTour?.preview) URL.revokeObjectURL(virtualTour.preview)
    setVirtualTour(null)
    setTourUploadProgress(0)
  }

  const toggleAmenity = (amenity: string) => setFormData(prev => ({ ...prev, amenities: prev.amenities.includes(amenity) ? prev.amenities.filter(a => a !== amenity) : [...prev.amenities, amenity] }))

  const handleSubmit = async () => {
    if (!user?.id) return
    if (!canPublish) return
    setIsSubmitting(true)
    try {
      // Step 1: Create listing (no tour URL yet)
      const listingData = {
        title: formData.title,
        description: formData.description,
        propertyType: formData.propertyType,
        city: formData.city,
        neighborhood: formData.neighborhood,
        address: formData.address,
        rentAmount: parseInt(formData.rentAmount),
        deposit: parseInt(formData.depositAmount) || parseInt(formData.rentAmount),
        bedrooms: parseInt(formData.bedrooms),
        bathrooms: parseInt(formData.bathrooms),
        squareMeters: formData.size ? parseInt(formData.size) : null,
        amenities: formData.amenities,
        virtualTourProvider: virtualTour ? virtualTour.type : undefined,
      }
      const response = await api.post("/listings", listingData, { params: { landlordId: user.id } })
      const listingId = response.data.id

      // Step 2: Upload photos
      for (let i = 0; i < photos.length; i++) {
        const photoFormData = new FormData()
        photoFormData.append("file", photos[i].file)
        await uploadApi.post(`/listings/${listingId}/photos`, photoFormData, {
          params: { landlordId: user.id, isPrimary: i === 0 }
        })
      }

      // Step 3: Upload virtual tour (video walkthrough or 360° image) if present
      if (virtualTour?.file) {
        const tourFormData = new FormData()
        tourFormData.append("file", virtualTour.file)
        try {
          if (virtualTour.type === "video") {
            // Video files go through listing video endpoint (Cloudinary video resource)
            await uploadApi.post(
              `/listings/${listingId}/video-tour`,
              tourFormData,
              {
                params: { landlordId: user.id },
                onUploadProgress: (e) => {
                  if (e.total) setTourUploadProgress(Math.round((e.loaded * 100) / e.total))
                }
              }
            )
            // Persist provider so clients can render proper player UI.
            await api.put(`/listings/${listingId}`, {
              virtualTourProvider: "video",
            }, { params: { landlordId: user.id } })
          } else {
            // 360 images are standard images; upload first, then attach URL to listing.
            const imageRes = await uploadApi.post("/upload/profile-photo", tourFormData, {
              onUploadProgress: (e) => {
                if (e.total) setTourUploadProgress(Math.round((e.loaded * 100) / e.total))
              }
            })
            const imageUrl = imageRes.data?.data ?? imageRes.data?.url
            if (typeof imageUrl !== "string" || !imageUrl.startsWith("http")) {
              throw new Error("360 upload did not return a valid URL")
            }
            await api.put(`/listings/${listingId}`, {
              videoTourUrl: imageUrl,
              virtualTourProvider: "360",
            }, { params: { landlordId: user.id } })
          }
        } catch (tourErr: any) {
          // Non-fatal: listing still created, tour just won't show
          const msg =
            tourErr?.response?.data?.message ||
            tourErr?.response?.data?.error ||
            tourErr?.message ||
            "Virtual tour upload failed"
          console.error("❌ Tour upload failed:", msg)
        }
      }

      router.push(`/landlord?published=${listingId}`)
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || "Failed to create listing. Please try again."
      setSubmitError(msg)
    } finally { setIsSubmitting(false) }
  }

  const canProceed = () => {
    switch (currentStep) {
      case 1: return photos.length > 0
      case 2: return formData.title && formData.propertyType && formData.city && formData.neighborhood
      case 3: return formData.rentAmount
      case 4: return true
      case 5: return true // Virtual tour is optional
      default: return false
    }
  }

  const nextStep = () => { if (currentStep < 5 && canProceed()) setCurrentStep(prev => prev + 1) }
  const prevStep = () => { if (currentStep > 1) setCurrentStep(prev => prev - 1) }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.landlordForm.newListing} />
      <div className="flex-1 flex flex-col lg:flex-row">
        <div className="flex-1 flex flex-col">
          <div className="bg-white border-b px-4 py-3">
            <div className="flex items-center justify-between max-w-lg mx-auto">
              {STEPS.map((step, index) => {
                const Icon = step.icon
                const isActive = currentStep === step.id
                const isCompleted = currentStep > step.id
                return (
                  <div key={step.id} className="flex items-center">
                    <button onClick={() => isCompleted && setCurrentStep(step.id)} className={`flex flex-col items-center ${isCompleted ? "cursor-pointer" : "cursor-default"}`}>
                      <div className={`h-10 w-10 rounded-full flex items-center justify-center transition-all ${isActive ? "bg-blue-600 text-white" : isCompleted ? "bg-emerald-500 text-white" : "bg-slate-200 text-slate-400"}`}>
                        {isCompleted ? <Check className="h-5 w-5" /> : <Icon className="h-5 w-5" />}
                      </div>
                      <span className={`text-xs mt-1 font-medium ${isActive ? "text-blue-600" : isCompleted ? "text-emerald-600" : "text-slate-400"}`}>{step.title}</span>
                    </button>
                    {index < STEPS.length - 1 && <div className={`w-8 lg:w-12 h-0.5 mx-1 ${currentStep > step.id ? "bg-emerald-500" : "bg-slate-200"}`} />}
                  </div>
                )
              })}
            </div>
          </div>
          <div className="flex-1 overflow-y-auto p-4">
            <div className="max-w-lg mx-auto">
              {completionStatus && !canPublish && (
                <div className="mb-4">
                  <CompletionBanner status={completionStatus} />
                </div>
              )}
              <Button
                type="button"
                variant="outline"
                className="mb-4 w-full rounded-xl border-blue-200 bg-blue-50 text-sm font-semibold text-blue-700 hover:bg-blue-100 hover:text-blue-800 lg:hidden"
                onClick={() => setShowMobilePreview(prev => !prev)}
              >
                {showMobilePreview ? "Hide live preview" : "Show live preview"}
              </Button>
              {showMobilePreview && (
                <div className="mb-6 lg:hidden">
                  <ListingPreview
                    photos={photos}
                    previewPhotoIndex={previewPhotoIndex}
                    onSelectPhoto={setPreviewPhotoIndex}
                    formData={formData}
                    formatCurrency={formatCurrency}
                    t={t}
                    language={language}
                  />
                </div>
              )}
              {currentStep === 1 && (
                <div className="space-y-4">
                  <div>
                    <h2 className="text-lg font-semibold text-slate-900">{t.landlordForm.addPhotos}</h2>
                    <p className="text-sm text-slate-500">{t.landlordForm.uploadPhotos}</p>
                    <p className="mt-2 text-xs leading-relaxed text-violet-800/90 bg-violet-50 border border-violet-100 rounded-xl px-3 py-2">
                      {t.landlordForm.photosTrustFuture}
                    </p>
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    {photos.map((photo, index) => (
                      <div key={index} className="relative aspect-square rounded-xl overflow-hidden bg-slate-100">
                        <img src={photo.preview} alt="" className="w-full h-full object-cover" />
                        <button onClick={() => handlePhotoRemove(index)} className="absolute top-1 right-1 h-6 w-6 rounded-full bg-black/50 flex items-center justify-center"><X className="h-3 w-3 text-white" /></button>
                        {index === 0 && <span className="absolute bottom-1 left-1 px-1.5 py-0.5 rounded bg-blue-600 text-white text-xs">Cover</span>}
                      </div>
                    ))}
                    {photos.length < 10 && (
                      <label className="aspect-square rounded-xl border-2 border-dashed border-slate-300 flex flex-col items-center justify-center cursor-pointer hover:border-blue-500 hover:bg-blue-50 transition-colors">
                        <Camera className="h-6 w-6 text-slate-400" /><span className="text-xs text-slate-500 mt-1">Add</span>
                        <input type="file" accept="image/*" multiple onChange={handlePhotoAdd} className="hidden" />
                      </label>
                    )}
                  </div>
                </div>
              )}
              {currentStep === 2 && (
                <div className="space-y-4">
                  <div><h2 className="text-lg font-semibold text-slate-900">{t.landlordForm.propertyDetails}</h2><p className="text-sm text-slate-500">{t.landlordForm.tellUsAbout}</p></div>
                  <div className="space-y-4">
                    <div><Label className="text-slate-700 mb-2 block">{t.landlordForm.titleLabel} *</Label><Input placeholder={t.landlordForm.titlePlaceholder} value={formData.title} onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))} className="h-12 rounded-xl" /></div>
                    <div><Label className="text-slate-700 mb-2 block">{t.listings.propertyType} *</Label>
                      <Select value={formData.propertyType} onValueChange={(value) => setFormData(prev => ({ ...prev, propertyType: value }))}><SelectTrigger className="h-12 rounded-xl"><SelectValue placeholder="Select type" /></SelectTrigger><SelectContent>{PROPERTY_TYPES.map((type) => (<SelectItem key={type.value} value={type.value}>{type.label}</SelectItem>))}</SelectContent></Select>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <div><Label className="text-slate-700 mb-2 block">{t.search.city} *</Label><Select value={formData.city} onValueChange={(value) => setFormData(prev => ({ ...prev, city: value }))}><SelectTrigger className="h-12 rounded-xl"><SelectValue placeholder="Select city" /></SelectTrigger><SelectContent>{CAMEROON_CITIES.map((city) => (<SelectItem key={city} value={city}>{city}</SelectItem>))}</SelectContent></Select></div>
                      <div><Label className="text-slate-700 mb-2 block">{t.landlordForm.neighborhoodLabel} *</Label><Input placeholder={t.landlordForm.neighborhoodPlaceholder} value={formData.neighborhood} onChange={(e) => setFormData(prev => ({ ...prev, neighborhood: e.target.value }))} className="h-12 rounded-xl" /></div>
                    </div>
                    <div><Label className="text-slate-700 mb-2 block">{t.landlordForm.addressLabel}</Label><Input placeholder={t.landlordForm.addressPlaceholder} value={formData.address} onChange={(e) => setFormData(prev => ({ ...prev, address: e.target.value }))} className="h-12 rounded-xl" /></div>
                    <div><Label className="text-slate-700 mb-2 block">{t.landlordForm.descriptionLabel}</Label><Textarea placeholder={t.landlordForm.descriptionPlaceholder} value={formData.description} onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))} className="min-h-[100px] rounded-xl resize-none" /></div>
                    <div>
                      <Label className="text-slate-700 mb-3 block">{language === "fr" ? "Meublé ?" : "Furnished?"} *</Label>
                      <div className="grid grid-cols-2 gap-3">
                        <button
                          type="button"
                          onClick={() => {
                            setFormData(prev => ({
                              ...prev,
                              amenities: prev.amenities.includes("Furnished")
                                ? prev.amenities
                                : [...prev.amenities, "Furnished"]
                            }))
                          }}
                          className={`p-4 rounded-xl border-2 text-center transition-all ${formData.amenities.includes("Furnished") ? "border-blue-600 bg-blue-50" : "border-slate-200 hover:border-slate-300"}`}
                        >
                          <span className="text-2xl block mb-1">🛋️</span>
                          <span className={`font-medium text-sm ${formData.amenities.includes("Furnished") ? "text-blue-700" : "text-slate-700"}`}>
                            {language === "fr" ? "Meublé" : "Furnished"}
                          </span>
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setFormData(prev => ({
                              ...prev,
                              amenities: prev.amenities.filter(a => a !== "Furnished")
                            }))
                          }}
                          className={`p-4 rounded-xl border-2 text-center transition-all ${!formData.amenities.includes("Furnished") ? "border-blue-600 bg-blue-50" : "border-slate-200 hover:border-slate-300"}`}
                        >
                          <span className="text-2xl block mb-1">🏠</span>
                          <span className={`font-medium text-sm ${!formData.amenities.includes("Furnished") ? "text-blue-700" : "text-slate-700"}`}>
                            {language === "fr" ? "Non meublé" : "Unfurnished"}
                          </span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              )}
              {currentStep === 3 && (
                <div className="space-y-4">
                  <div><h2 className="text-lg font-semibold text-slate-900">{t.landlordForm.pricing}</h2><p className="text-sm text-slate-500">{t.landlordForm.setYourRent}</p></div>
                  <div className="space-y-4">
                    <div><Label className="text-slate-700 mb-2 block">{t.landlordForm.monthlyRent} *</Label><Input type="number" placeholder={t.landlordForm.rentPlaceholder} value={formData.rentAmount} onChange={(e) => {
                      const value = e.target.value
                      if (value === '' || parseFloat(value) >= 0) {
                        setFormData(prev => ({ ...prev, rentAmount: value }))
                      }
                    }} className="h-12 rounded-xl text-lg font-semibold" /></div>
                    <div><Label className="text-slate-700 mb-2 block">{t.landlordForm.depositLabel}</Label><Input type="number" placeholder={t.landlordForm.depositPlaceholder} value={formData.depositAmount} onChange={(e) => {
                      const value = e.target.value
                      if (value === '' || parseFloat(value) >= 0) {
                        setFormData(prev => ({ ...prev, depositAmount: value }))
                      }
                    }} className="h-12 rounded-xl" /></div>
                    <div className="grid grid-cols-3 gap-3">
                      <div><Label className="text-slate-700 mb-2 block flex items-center gap-1"><Bed className="h-4 w-4" /> {t.landlordForm.beds}</Label><Select value={formData.bedrooms} onValueChange={(value) => setFormData(prev => ({ ...prev, bedrooms: value }))}><SelectTrigger className="h-12 rounded-xl"><SelectValue /></SelectTrigger><SelectContent>{[1, 2, 3, 4, 5, 6].map((num) => (<SelectItem key={num} value={num.toString()}>{num}</SelectItem>))}</SelectContent></Select></div>
                      <div><Label className="text-slate-700 mb-2 block flex items-center gap-1"><Bath className="h-4 w-4" /> {t.landlordForm.baths}</Label><Select value={formData.bathrooms} onValueChange={(value) => setFormData(prev => ({ ...prev, bathrooms: value }))}><SelectTrigger className="h-12 rounded-xl"><SelectValue /></SelectTrigger><SelectContent>{[1, 2, 3, 4].map((num) => (<SelectItem key={num} value={num.toString()}>{num}</SelectItem>))}</SelectContent></Select></div>
                      <div><Label className="text-slate-700 mb-2 block flex items-center gap-1"><Maximize className="h-4 w-4" /> {t.landlordForm.size}</Label><Input type="number" placeholder="m²" value={formData.size} onChange={(e) => setFormData(prev => ({ ...prev, size: e.target.value }))} className="h-12 rounded-xl" /></div>
                    </div>
                  </div>
                </div>
              )}
              {currentStep === 4 && (
                <div className="space-y-4">
                  <div><h2 className="text-lg font-semibold text-slate-900">{t.landlordForm.amenitiesFeatures}</h2><p className="text-sm text-slate-500">{t.landlordForm.selectAmenities}</p></div>
                  {Object.entries(AMENITIES_BY_CATEGORY).map(([category, amenities]) => (
                    <div key={category} className="space-y-2">
                      <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{category}</h3>
                      <div className="grid grid-cols-2 gap-2">
                        {amenities.map((amenity) => (<button key={amenity} onClick={() => toggleAmenity(amenity)} className={`p-3 rounded-xl text-sm font-medium transition-all flex items-center gap-2 ${formData.amenities.includes(amenity) ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`}>{formData.amenities.includes(amenity) && <CheckCircle className="h-4 w-4" />}{amenity}</button>))}
                      </div>
                    </div>
                  ))}
                  <div className="mt-6 p-4 bg-emerald-50 rounded-xl border border-emerald-200">
                    <h3 className="font-semibold text-emerald-800 mb-2">{t.landlordForm.readyToPublish}</h3>
                    <ul className="text-sm text-emerald-700 space-y-1">
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {photos.length} {t.landlordForm.photos}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.title || t.landlordForm.noTitle}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.rentAmount ? formatCurrency(parseInt(formData.rentAmount)) : t.landlordForm.noPrice}{t.listings.perMonth}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.amenities.length} {t.landlordForm.amenitiesCount}</li>
                    </ul>
                  </div>
                </div>
              )}
              {currentStep === 5 && (
                <div className="space-y-4">
                  <div className="rounded-2xl border border-violet-200 bg-gradient-to-br from-violet-50/90 via-white to-slate-50 p-4 shadow-sm">
                    <div className="mb-3 flex items-center gap-2">
                      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-violet-600 text-white">
                        <Glasses className="h-4 w-4" aria-hidden />
                      </div>
                      <span className="rounded-full bg-violet-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-violet-900">
                        {t.landlordJourney.immersiveBadge}
                      </span>
                    </div>
                    <div className="grid gap-3 sm:grid-cols-2">
                      <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-3">
                        <p className="mb-1 text-[10px] font-bold uppercase tracking-wide text-emerald-800">
                          {t.landlordForm.virtualTourTimelineToday}
                        </p>
                        <p className="text-xs leading-relaxed text-slate-700">{t.landlordForm.virtualTourTimelineTodayBody}</p>
                      </div>
                      <div className="rounded-xl border border-violet-100 bg-white/90 p-3">
                        <p className="mb-1 text-[10px] font-bold uppercase tracking-wide text-violet-800">
                          {t.landlordForm.virtualTourTimelineNext}
                        </p>
                        <p className="text-xs leading-relaxed text-slate-700">{t.landlordForm.virtualTourTimelineNextBody}</p>
                      </div>
                    </div>
                  </div>
                  <div>
                    <h2 className="text-lg font-semibold text-slate-900">{t.landlordForm.virtualTour}</h2>
                    <p className="text-sm text-slate-600">{t.landlordForm.virtualTourDesc}</p>
                  </div>
                  <div className="space-y-4">
                    {virtualTour ? (
                      <div className="space-y-4">
                        <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-xl p-4 border border-blue-200">
                          <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-2 text-blue-800">
                              {virtualTour.type === 'video' ? <Video className="h-5 w-5" /> : <ImageIcon2 className="h-5 w-5" />}
                              <span className="font-medium">
                                {virtualTour.type === 'video'
                                  ? `${t.landlordForm.videoWalkthrough} · ${t.landlordForm.ready}`
                                  : `${t.landlordForm.tour360} · ${t.landlordForm.ready}`}
                              </span>
                            </div>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={handleVirtualTourRemove}
                              className="text-red-600 hover:text-red-700 border-red-200"
                            >
                              <Trash2 className="h-4 w-4 mr-1" /> {t.common.remove}
                            </Button>
                          </div>

                          {/* Preview */}
                          {virtualTour.type === 'video' ? (
                            <video
                              src={virtualTour.preview}
                              className="w-full rounded-xl max-h-48 object-cover bg-black"
                              controls
                              muted
                            />
                          ) : (
                            <img
                              src={virtualTour.preview}
                              alt="360° panorama preview"
                              className="w-full rounded-xl max-h-48 object-cover"
                            />
                          )}

                          <div className="mt-3 grid grid-cols-2 gap-2">
                            <div className="bg-white rounded-lg p-2 text-center">
                              <div className="text-xs text-slate-500">{t.landlordForm.tourType}</div>
                              <div className="text-sm font-medium text-slate-900">
                                {virtualTour.type === 'video' ? `📹 ${t.landlordForm.video}` : `🔮 ${t.landlordForm.photo360}`}
                              </div>
                            </div>
                            <div className="bg-white rounded-lg p-2 text-center">
                              <div className="text-xs text-slate-500">{t.landlordForm.fileSize}</div>
                              <div className="text-sm font-medium text-slate-900">
                                {(virtualTour.file.size / (1024 * 1024)).toFixed(1)} MB
                              </div>
                            </div>
                          </div>
                          <p className="mt-3 text-xs leading-relaxed text-violet-900/90 border-t border-violet-100 pt-3">
                            {t.landlordForm.virtualTourUploadedFooter}
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {/* Option 1: Video Walkthrough */}
                        <label className="block cursor-pointer">
                          <div className="border-2 border-dashed border-blue-300 rounded-xl p-6 bg-blue-50 hover:bg-blue-100 hover:border-blue-400 transition-colors text-center">
                            <Video className="h-10 w-10 text-blue-500 mx-auto mb-3" />
                            <p className="font-semibold text-slate-900 mb-1">📹 {t.landlordForm.videoWalkthrough}</p>
                            <p className="text-sm text-slate-500 mb-2">{t.landlordForm.uploadVideo}</p>
                            <p className="mx-auto mb-3 max-w-md text-left text-xs leading-relaxed text-violet-900/85">
                              {t.landlordForm.videoCardArCaption}
                            </p>
                            <div className="inline-flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-xl text-sm font-medium">
                              <Upload className="h-4 w-4" /> {t.landlordForm.chooseVideoFile}
                            </div>
                            <p className="text-xs text-slate-400 mt-2">{t.landlordForm.videoFormats}</p>
                          </div>
                          <input
                            type="file"
                            accept="video/mp4,video/quicktime,video/avi,video/*"
                            className="hidden"
                            onChange={(e) => {
                              const file = e.target.files?.[0]
                              if (file) handleTourFileSelect(file, 'video')
                            }}
                          />
                        </label>

                        {/* Divider */}
                        <div className="flex items-center gap-3">
                          <div className="flex-1 h-px bg-slate-200" />
                          <span className="text-xs text-slate-400 font-medium">{t.common.or}</span>
                          <div className="flex-1 h-px bg-slate-200" />
                        </div>

                        {/* Option 2: 360° Photo */}
                        <label className="block cursor-pointer">
                          <div className="border-2 border-dashed border-purple-300 rounded-xl p-6 bg-purple-50 hover:bg-purple-100 hover:border-purple-400 transition-colors text-center">
                            <ImageIcon2 className="h-10 w-10 text-purple-500 mx-auto mb-3" />
                            <p className="font-semibold text-slate-900 mb-1">🔮 {t.landlordForm.tour360}</p>
                            <p className="text-sm text-slate-500 mb-2">{t.landlordForm.upload360Desc}</p>
                            <p className="mx-auto mb-3 max-w-md text-left text-xs leading-relaxed text-violet-900/85">
                              {t.landlordForm.photo360CardArCaption}
                            </p>
                            <div className="inline-flex items-center gap-2 bg-purple-600 text-white px-4 py-2 rounded-xl text-sm font-medium">
                              <Upload className="h-4 w-4" /> {t.landlordForm.choose360Photo}
                            </div>
                            <p className="text-xs text-slate-400 mt-2">{t.landlordForm.photo360Formats}</p>
                          </div>
                          <input
                            type="file"
                            accept="image/jpeg,image/png,image/jpg"
                            className="hidden"
                            onChange={(e) => {
                              const file = e.target.files?.[0]
                              if (file) handleTourFileSelect(file, '360')
                            }}
                          />
                        </label>

                        {/* Help tip */}
                        <div className="bg-slate-50 rounded-xl p-4 text-sm text-slate-600">
                          <p className="font-medium text-slate-900 mb-2">💡 {t.landlordForm.howToCapture}</p>
                          <ul className="space-y-1 text-xs">
                            <li>• {t.landlordForm.captureStep1}</li>
                            <li>• {t.landlordForm.captureStep2}</li>
                            <li>• {t.landlordForm.captureStep3}</li>
                            <li>• {t.landlordForm.captureStep4}</li>
                          </ul>
                        </div>
                      </div>
                    )}
                  </div>
                  <div className="mt-6 p-4 bg-emerald-50 rounded-xl border border-emerald-200">
                    <h3 className="font-semibold text-emerald-800 mb-2">{t.landlordForm.readyToPublish}</h3>
                    <ul className="text-sm text-emerald-700 space-y-1">
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {photos.length} {t.landlordForm.photos}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.title || t.landlordForm.noTitle}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.rentAmount ? formatCurrency(parseInt(formData.rentAmount)) : t.landlordForm.noPrice}{t.listings.perMonth}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.amenities.length} {t.landlordForm.amenitiesCount}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {virtualTour ? t.landlordForm.virtualTourAdded : t.landlordForm.noVirtualTour}</li>
                    </ul>
                  </div>
                </div>
              )}
            </div>
          </div>
          <div className="bg-white border-t p-4">
            {submitError && (
              <div className="max-w-lg mx-auto mb-3 px-4 py-2.5 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
                {submitError}
              </div>
            )}
            <div className="max-w-lg mx-auto flex gap-3">
              {currentStep > 1 && <Button variant="outline" onClick={prevStep} className="flex-1 h-12 rounded-xl"><ArrowLeft className="h-4 w-4 mr-2" />{t.common.back}</Button>}
              {currentStep < 5 ? (<Button onClick={() => { setSubmitError(null); nextStep() }} disabled={!canProceed() || !canPublish} className="flex-1 h-12 rounded-xl">{t.common.next}<ArrowRight className="h-4 w-4 ml-2" /></Button>) : (<Button onClick={handleSubmit} disabled={isSubmitting || !canProceed() || !canPublish} className="flex-1 h-12 rounded-xl bg-emerald-600 hover:bg-emerald-700">{isSubmitting ? t.landlordForm.publishing : t.landlordForm.publishListing}</Button>)}
            </div>
          </div>
        </div>
        <div className="hidden w-80 border-l bg-slate-100 p-4 lg:block xl:w-96">
          <div className="sticky top-4">
            <ListingPreview
              photos={photos}
              previewPhotoIndex={previewPhotoIndex}
              onSelectPhoto={setPreviewPhotoIndex}
              formData={formData}
              formatCurrency={formatCurrency}
              t={t}
              language={language}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
