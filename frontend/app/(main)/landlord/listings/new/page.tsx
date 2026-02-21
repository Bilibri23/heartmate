"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Camera, X, MapPin, Bed, Bath, Maximize, CheckCircle, ImageIcon, FileText, DollarSign, Sparkles, Check, ArrowLeft, ArrowRight, Video, Play } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import api from "@/lib/api"

const CAMEROON_CITIES = ["Douala", "Yaounde", "Bamenda", "Bafoussam", "Garoua", "Maroua", "Ngaoundere", "Bertoua", "Limbe", "Buea", "Kribi", "Ebolowa"]
const PROPERTY_TYPES = [
  { value: "STUDIO", label: "Studio" },
  { value: "APARTMENT", label: "Apartment" },
  { value: "HOUSE", label: "House" },
  { value: "PRIVATE_ROOM", label: "Private Room" },
  { value: "SHARED_ROOM", label: "Shared Room" },
]
const AMENITIES = ["WiFi", "Air Conditioning", "Furnished", "Kitchen", "Parking", "Security", "Water Tank", "Generator", "Balcony", "Laundry"]
const STEPS = [{ id: 1, title: "Photos", icon: ImageIcon }, { id: 2, title: "Details", icon: FileText }, { id: 3, title: "Pricing", icon: DollarSign }, { id: 4, title: "Amenities", icon: Sparkles }, { id: 5, title: "Virtual Tour", icon: Video }]

interface PhotoItem { file: File; preview: string }
interface VideoItem { file: File; preview: string; duration?: number }

export default function NewListingPage() {
  const { formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [currentStep, setCurrentStep] = useState(1)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [previewPhotoIndex, setPreviewPhotoIndex] = useState(0)
  const [photos, setPhotos] = useState<PhotoItem[]>([])
  const [virtualTour, setVirtualTour] = useState<VideoItem | null>(null)
  const [formData, setFormData] = useState({ title: "", description: "", propertyType: "", city: "", neighborhood: "", address: "", rentAmount: "", depositAmount: "", bedrooms: "1", bathrooms: "1", size: "", amenities: [] as string[] })

  const handlePhotoAdd = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files) return
    const newPhotos = Array.from(files).map(file => ({ file, preview: URL.createObjectURL(file) }))
    setPhotos(prev => [...prev, ...newPhotos].slice(0, 10))
  }

  const handlePhotoRemove = (index: number) => setPhotos(prev => prev.filter((_, i) => i !== index))

  const handleVideoAdd = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    
    // Check if file is a video
    if (!file.type.startsWith('video/')) {
      alert('Please select a video file')
      return
    }
    
    // Check file size (max 100MB)
    if (file.size > 100 * 1024 * 1024) {
      alert('Video file must be less than 100MB')
      return
    }
    
    // Create video element to get duration
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      setVirtualTour({
        file,
        preview: URL.createObjectURL(file),
        duration: Math.round(video.duration)
      })
    }
    video.src = URL.createObjectURL(file)
  }

  const handleVideoRemove = () => {
    if (virtualTour) {
      URL.revokeObjectURL(virtualTour.preview)
      setVirtualTour(null)
    }
  }

  const toggleAmenity = (amenity: string) => setFormData(prev => ({ ...prev, amenities: prev.amenities.includes(amenity) ? prev.amenities.filter(a => a !== amenity) : [...prev.amenities, amenity] }))

  const handleSubmit = async () => {
    if (!user?.id) return
    setIsSubmitting(true)
    try {
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
        amenities: formData.amenities
      }
      const response = await api.post("/listings", listingData, { params: { landlordId: user.id } })
      const listingId = response.data.id
      for (let i = 0; i < photos.length; i++) {
        const photoFormData = new FormData()
        photoFormData.append("file", photos[i].file)
        await api.post(`/listings/${listingId}/photos`, photoFormData, { 
          params: { landlordId: user.id, isPrimary: i === 0 }
          // Don't set Content-Type - let Axios handle it automatically for FormData
        })
      }
      
      // TODO: Implement video tour upload - backend doesn't support video upload yet
      // Virtual tour upload is disabled for now
      /*
      if (virtualTour) {
        // Video upload functionality to be implemented when backend supports it
        console.log("Virtual tour upload not yet supported by backend")
      }
      */
      
      router.push("/landlord")
    } catch (err) { console.error("Failed to create listing:", err) } finally { setIsSubmitting(false) }
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
      <MobileHeader title="New Listing" />
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
              {currentStep === 1 && (
                <div className="space-y-4">
                  <div><h2 className="text-lg font-semibold text-slate-900">Add Photos</h2><p className="text-sm text-slate-500">Upload up to 10 photos</p></div>
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
                  <div><h2 className="text-lg font-semibold text-slate-900">Property Details</h2><p className="text-sm text-slate-500">Tell us about your property</p></div>
                  <div className="space-y-4">
                    <div><Label className="text-slate-700 mb-2 block">Title *</Label><Input placeholder="e.g., Modern Studio in Bonapriso" value={formData.title} onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))} className="h-12 rounded-xl" /></div>
                    <div><Label className="text-slate-700 mb-2 block">Property Type *</Label>
                      <Select value={formData.propertyType} onValueChange={(value) => setFormData(prev => ({ ...prev, propertyType: value }))}><SelectTrigger className="h-12 rounded-xl"><SelectValue placeholder="Select type" /></SelectTrigger><SelectContent>{PROPERTY_TYPES.map((type) => (<SelectItem key={type.value} value={type.value}>{type.label}</SelectItem>))}</SelectContent></Select>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <div><Label className="text-slate-700 mb-2 block">City *</Label><Select value={formData.city} onValueChange={(value) => setFormData(prev => ({ ...prev, city: value }))}><SelectTrigger className="h-12 rounded-xl"><SelectValue placeholder="Select city" /></SelectTrigger><SelectContent>{CAMEROON_CITIES.map((city) => (<SelectItem key={city} value={city}>{city}</SelectItem>))}</SelectContent></Select></div>
                      <div><Label className="text-slate-700 mb-2 block">Neighborhood *</Label><Input placeholder="e.g., Bonapriso" value={formData.neighborhood} onChange={(e) => setFormData(prev => ({ ...prev, neighborhood: e.target.value }))} className="h-12 rounded-xl" /></div>
                    </div>
                    <div><Label className="text-slate-700 mb-2 block">Address</Label><Input placeholder="Street address" value={formData.address} onChange={(e) => setFormData(prev => ({ ...prev, address: e.target.value }))} className="h-12 rounded-xl" /></div>
                    <div><Label className="text-slate-700 mb-2 block">Description</Label><Textarea placeholder="Describe your property..." value={formData.description} onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))} className="min-h-[100px] rounded-xl resize-none" /></div>
                  </div>
                </div>
              )}
              {currentStep === 3 && (
                <div className="space-y-4">
                  <div><h2 className="text-lg font-semibold text-slate-900">Pricing</h2><p className="text-sm text-slate-500">Set your rent</p></div>
                  <div className="space-y-4">
                    <div><Label className="text-slate-700 mb-2 block">Monthly Rent (FCFA) *</Label><Input type="number" placeholder="e.g., 75000" value={formData.rentAmount} onChange={(e) => setFormData(prev => ({ ...prev, rentAmount: e.target.value }))} className="h-12 rounded-xl text-lg font-semibold" /></div>
                    <div><Label className="text-slate-700 mb-2 block">Deposit (FCFA)</Label><Input type="number" placeholder="Same as rent if empty" value={formData.depositAmount} onChange={(e) => setFormData(prev => ({ ...prev, depositAmount: e.target.value }))} className="h-12 rounded-xl" /></div>
                    <div className="grid grid-cols-3 gap-3">
                      <div><Label className="text-slate-700 mb-2 block flex items-center gap-1"><Bed className="h-4 w-4" /> Beds</Label><Select value={formData.bedrooms} onValueChange={(value) => setFormData(prev => ({ ...prev, bedrooms: value }))}><SelectTrigger className="h-12 rounded-xl"><SelectValue /></SelectTrigger><SelectContent>{[1,2,3,4,5,6].map((num) => (<SelectItem key={num} value={num.toString()}>{num}</SelectItem>))}</SelectContent></Select></div>
                      <div><Label className="text-slate-700 mb-2 block flex items-center gap-1"><Bath className="h-4 w-4" /> Baths</Label><Select value={formData.bathrooms} onValueChange={(value) => setFormData(prev => ({ ...prev, bathrooms: value }))}><SelectTrigger className="h-12 rounded-xl"><SelectValue /></SelectTrigger><SelectContent>{[1,2,3,4].map((num) => (<SelectItem key={num} value={num.toString()}>{num}</SelectItem>))}</SelectContent></Select></div>
                      <div><Label className="text-slate-700 mb-2 block flex items-center gap-1"><Maximize className="h-4 w-4" /> Size</Label><Input type="number" placeholder="m2" value={formData.size} onChange={(e) => setFormData(prev => ({ ...prev, size: e.target.value }))} className="h-12 rounded-xl" /></div>
                    </div>
                  </div>
                </div>
              )}
              {currentStep === 4 && (
                <div className="space-y-4">
                  <div><h2 className="text-lg font-semibold text-slate-900">Amenities</h2><p className="text-sm text-slate-500">Select amenities</p></div>
                  <div className="grid grid-cols-2 gap-2">
                    {AMENITIES.map((amenity) => (<button key={amenity} onClick={() => toggleAmenity(amenity)} className={`p-3 rounded-xl text-sm font-medium transition-all flex items-center gap-2 ${formData.amenities.includes(amenity) ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`}>{formData.amenities.includes(amenity) && <CheckCircle className="h-4 w-4" />}{amenity}</button>))}
                  </div>
                  <div className="mt-6 p-4 bg-emerald-50 rounded-xl border border-emerald-200">
                    <h3 className="font-semibold text-emerald-800 mb-2">Ready to publish?</h3>
                    <ul className="text-sm text-emerald-700 space-y-1">
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {photos.length} photos</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.title || "No title"}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.rentAmount ? formatCurrency(parseInt(formData.rentAmount)) : "No price"}/month</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.amenities.length} amenities</li>
                    </ul>
                  </div>
                </div>
              )}
              {currentStep === 5 && (
                <div className="space-y-4">
                  <div><h2 className="text-lg font-semibold text-slate-900">Virtual Tour</h2><p className="text-sm text-slate-500">Add a video walkthrough (optional)</p></div>
                  <div className="space-y-4">
                    {virtualTour ? (
                      <div className="space-y-4">
                        <div className="relative rounded-xl overflow-hidden bg-black">
                          <video 
                            src={virtualTour.preview} 
                            className="w-full h-64 object-contain"
                            controls
                          />
                          <div className="absolute top-2 right-2">
                            <Button
                              size="sm"
                              variant="destructive"
                              onClick={handleVideoRemove}
                              className="h-8 w-8 rounded-full p-0"
                            >
                              <X className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                        <div className="bg-blue-50 rounded-xl p-4">
                          <div className="flex items-center gap-2 text-blue-800">
                            <Video className="h-5 w-5" />
                            <span className="font-medium">Virtual Tour Added</span>
                          </div>
                          <p className="text-sm text-blue-600 mt-1">
                            Duration: {Math.floor((virtualTour.duration || 0) / 60)}:{((virtualTour.duration || 0) % 60).toString().padStart(2, '0')}
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="border-2 border-dashed border-slate-300 rounded-xl p-8 text-center">
                        <Video className="h-12 w-12 text-slate-400 mx-auto mb-4" />
                        <h3 className="text-lg font-medium text-slate-900 mb-2">Add Virtual Tour</h3>
                        <p className="text-sm text-slate-500 mb-4">
                          Upload a video walkthrough of your property (max 100MB)
                        </p>
                        <div className="space-y-2">
                          <p className="text-xs text-slate-400">
                            Supported formats: MP4, WebM, MOV
                          </p>
                          <label className="inline-block">
                            <input
                              type="file"
                              accept="video/*"
                              onChange={handleVideoAdd}
                              className="hidden"
                            />
                            <Button className="h-12 rounded-xl">
                              <Video className="h-5 w-5 mr-2" />
                              Choose Video
                            </Button>
                          </label>
                        </div>
                      </div>
                    )}
                  </div>
                  <div className="mt-6 p-4 bg-emerald-50 rounded-xl border border-emerald-200">
                    <h3 className="font-semibold text-emerald-800 mb-2">Ready to publish?</h3>
                    <ul className="text-sm text-emerald-700 space-y-1">
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {photos.length} photos</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.title || "No title"}</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.rentAmount ? formatCurrency(parseInt(formData.rentAmount)) : "No price"}/month</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {formData.amenities.length} amenities</li>
                      <li className="flex items-center gap-2"><Check className="h-4 w-4" /> {virtualTour ? "Virtual tour added" : "No virtual tour"}</li>
                    </ul>
                  </div>
                </div>
              )}
            </div>
          </div>
          <div className="bg-white border-t p-4">
            <div className="max-w-lg mx-auto flex gap-3">
              {currentStep > 1 && <Button variant="outline" onClick={prevStep} className="flex-1 h-12 rounded-xl"><ArrowLeft className="h-4 w-4 mr-2" />Back</Button>}
              {currentStep < 5 ? (<Button onClick={nextStep} disabled={!canProceed()} className="flex-1 h-12 rounded-xl">Next<ArrowRight className="h-4 w-4 ml-2" /></Button>) : (<Button onClick={handleSubmit} disabled={isSubmitting || !canProceed()} className="flex-1 h-12 rounded-xl bg-emerald-600 hover:bg-emerald-700">{isSubmitting ? "Publishing..." : "Publish Listing"}</Button>)}
            </div>
          </div>
        </div>
        <div className="hidden lg:block w-80 xl:w-96 border-l bg-slate-100 p-4">
          <div className="sticky top-4">
            <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
              <div className="relative h-48 bg-slate-200">
                {photos.length > 0 ? (<div className="relative h-full"><img src={photos[previewPhotoIndex]?.preview} alt="Preview" className="w-full h-full object-cover" />{photos.length > 1 && (<div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">{photos.map((_, index) => (<button key={index} onClick={() => setPreviewPhotoIndex(index)} className={`h-1.5 rounded-full transition-all ${index === previewPhotoIndex ? "w-4 bg-white" : "w-1.5 bg-white/60"}`} />))}</div>)}</div>) : (<div className="flex h-full items-center justify-center flex-col gap-2"><Camera className="h-10 w-10 text-slate-300" /><p className="text-slate-400 text-xs">Add photos</p></div>)}
                <div className="absolute top-2 left-2"><span className="px-2 py-0.5 rounded-full bg-blue-600 text-white text-xs font-medium">Live Preview</span></div>
              </div>
              <div className="p-3 space-y-3">
                <div><p className="text-lg font-bold text-blue-600">{formData.rentAmount ? formatCurrency(parseInt(formData.rentAmount)) : "---"}/mois</p><h1 className="text-sm font-semibold text-slate-900 line-clamp-1">{formData.title || "Your listing title"}</h1><p className="text-slate-500 text-xs flex items-center gap-1"><MapPin className="h-3 w-3" />{formData.neighborhood || "Neighborhood"}, {formData.city || "City"}</p></div>
                <div className="flex gap-3 py-2 border-y border-slate-100 text-xs"><div className="flex items-center gap-1"><Bed className="h-3.5 w-3.5 text-slate-400" /><span className="font-medium">{formData.bedrooms || "1"}</span></div><div className="flex items-center gap-1"><Bath className="h-3.5 w-3.5 text-slate-400" /><span className="font-medium">{formData.bathrooms || "1"}</span></div>{formData.size && <div className="flex items-center gap-1"><Maximize className="h-3.5 w-3.5 text-slate-400" /><span className="font-medium">{formData.size}m2</span></div>}</div>
                {formData.amenities.length > 0 && (<div className="flex flex-wrap gap-1">{formData.amenities.slice(0, 4).map((amenity) => (<span key={amenity} className="px-2 py-0.5 bg-slate-100 rounded-full text-xs text-slate-600">{amenity}</span>))}{formData.amenities.length > 4 && <span className="px-2 py-0.5 bg-slate-100 rounded-full text-xs text-slate-600">+{formData.amenities.length - 4}</span>}</div>)}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
