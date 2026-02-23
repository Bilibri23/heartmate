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
import api, { uploadApi } from "@/lib/api"

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
interface VirtualTour {
  tourUrl?: string
  embedCode?: string
  provider?: 'panoee' | 'kuula' | 'zillow' | 'cloudpano' | 'generic'
}

export default function NewListingPage() {
  const { formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [currentStep, setCurrentStep] = useState(1)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [previewPhotoIndex, setPreviewPhotoIndex] = useState(0)
  const [photos, setPhotos] = useState<PhotoItem[]>([])
  const [virtualTour, setVirtualTour] = useState<VirtualTour | null>(null)
  const [formData, setFormData] = useState({ title: "", description: "", propertyType: "", city: "", neighborhood: "", address: "", rentAmount: "", depositAmount: "", bedrooms: "1", bathrooms: "1", size: "", amenities: [] as string[] })

  const handlePhotoAdd = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files) return
    const newPhotos = Array.from(files).map(file => ({ file, preview: URL.createObjectURL(file) }))
    setPhotos(prev => [...prev, ...newPhotos].slice(0, 10))
  }

  const handlePhotoRemove = (index: number) => setPhotos(prev => prev.filter((_, i) => i !== index))

  const handleVirtualTourUrlChange = (url: string) => {
    setVirtualTour(prev => prev ? {
      ...prev,
      tourUrl: url
    } : {
      provider: 'generic',
      tourUrl: url,
      embedCode: ''
    })
  }

  const handleEmbedCodeChange = (embedCode: string) => {
    setVirtualTour(prev => prev ? {
      ...prev,
      embedCode
    } : {
      provider: 'generic',
      tourUrl: '',
      embedCode
    })
  }

  const handleProviderChange = (provider: 'panoee' | 'kuula' | 'zillow' | 'cloudpano' | 'generic') => {
    setVirtualTour(prev => ({
      ...prev,
      provider
    }))
  }

  const handleVirtualTourRemove = () => {
    setVirtualTour(null)
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
        deposit: parseInt(formData.depositAmount) || parseInt(formData.rentAmount),
        bedrooms: parseInt(formData.bedrooms),
        bathrooms: parseInt(formData.bathrooms),
        squareMeters: formData.size ? parseInt(formData.size) : null,
        amenities: formData.amenities,
        videoTourUrl: virtualTour?.tourUrl,
        videoTourEmbedCode: virtualTour?.embedCode,
        virtualTourProvider: virtualTour?.provider || 'generic'
      }
      const response = await api.post("/listings", listingData, { params: { landlordId: user.id } })
      const listingId = response.data.id
      for (let i = 0; i < photos.length; i++) {
        const photoFormData = new FormData()
        photoFormData.append("file", photos[i].file)
        await uploadApi.post(`/listings/${listingId}/photos`, photoFormData, { 
          params: { landlordId: user.id, isPrimary: i === 0 }
          // Don't set Content-Type - let Axios handle it automatically for FormData
        })
      }
      
      router.push("/landlord")
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
                  <div><h2 className="text-lg font-semibold text-slate-900">Amenities & Features</h2><p className="text-sm text-slate-500">Select everything your property offers</p></div>
                  {Object.entries(AMENITIES_BY_CATEGORY).map(([category, amenities]) => (
                    <div key={category} className="space-y-2">
                      <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{category}</h3>
                      <div className="grid grid-cols-2 gap-2">
                        {amenities.map((amenity) => (<button key={amenity} onClick={() => toggleAmenity(amenity)} className={`p-3 rounded-xl text-sm font-medium transition-all flex items-center gap-2 ${formData.amenities.includes(amenity) ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`}>{formData.amenities.includes(amenity) && <CheckCircle className="h-4 w-4" />}{amenity}</button>))}
                      </div>
                    </div>
                  ))}
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
                  <div>
                    <h2 className="text-lg font-semibold text-slate-900">Virtual Tour</h2>
                    <p className="text-sm text-slate-500">Add a video walkthrough or 360° virtual tour (optional)</p>
                  </div>
                  <div className="space-y-4">
                    {virtualTour && (virtualTour.tourUrl || virtualTour.embedCode) ? (
                      <div className="space-y-4">
                        <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-xl p-4 border border-blue-200">
                          <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center gap-2 text-blue-800">
                              <Video className="h-5 w-5" />
                              <span className="font-medium">Virtual Tour Configured</span>
                            </div>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={handleVirtualTourRemove}
                              className="text-slate-600 hover:text-slate-800"
                            >
                              Remove
                            </Button>
                          </div>
                          
                          {virtualTour.tourUrl && (
                            <div className="mb-3">
                              <div className="text-xs text-slate-500 mb-1">Tour URL</div>
                              <div className="text-sm text-slate-900 bg-white rounded p-2 border border-slate-200 break-all">
                                {virtualTour.tourUrl}
                              </div>
                            </div>
                          )}
                          
                          {virtualTour.embedCode && (
                            <div className="mb-3">
                              <div className="text-xs text-slate-500 mb-1">Embed Code</div>
                              <div className="text-sm text-slate-900 bg-white rounded p-2 border border-slate-200 max-h-20 overflow-y-auto">
                                <code className="text-xs">{virtualTour.embedCode.substring(0, 100)}...</code>
                              </div>
                            </div>
                          )}
                          
                          <div className="mt-3 grid grid-cols-2 gap-2">
                            <div className="bg-white rounded-lg p-2 text-center">
                              <div className="text-xs text-slate-500">Provider</div>
                              <div className="text-sm font-medium text-slate-900 capitalize">{virtualTour.provider || 'generic'}</div>
                            </div>
                            <div className="bg-white rounded-lg p-2 text-center">
                              <div className="text-xs text-slate-500">Type</div>
                              <div className="text-sm font-medium text-slate-900">360° Tour</div>
                            </div>
                          </div>
                        </div>
                      </div>
                    ) : (
                      <div className="space-y-6">
                        <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-xl p-6 border border-blue-200">
                          <div className="flex items-center gap-2 mb-4">
                            <Video className="h-6 w-6 text-blue-600" />
                            <h3 className="text-lg font-medium text-slate-900">Add Virtual Tour</h3>
                            <span className="bg-blue-600 text-white px-2 py-1 rounded-full text-xs font-medium">NEW</span>
                          </div>
                          
                          {/* Provider Selection */}
                          <div className="mb-4">
                            <label className="block text-sm font-medium text-slate-700 mb-2">Tour Provider</label>
                            <div className="grid grid-cols-2 gap-2">
                              {[
                                { id: 'panoee', name: 'Panoee', desc: 'Best free option', icon: '🌐' },
                                { id: 'kuula', name: 'Kuula', desc: 'Hotspot support', icon: '🎯' },
                                { id: 'zillow', name: 'Zillow 3D', desc: 'Phone capture', icon: '🏠' },
                                { id: 'cloudpano', name: 'CloudPano', desc: 'Custom branding', icon: '☁️' },
                                { id: 'generic', name: 'Other', desc: 'Any platform', icon: '🎮' }
                              ].map(provider => (
                                <button
                                  key={provider.id}
                                  type="button"
                                  onClick={() => handleProviderChange(provider.id as any)}
                                  className={`p-3 rounded-lg border text-left transition-colors ${
                                    virtualTour?.provider === provider.id
                                      ? 'border-blue-500 bg-blue-50'
                                      : 'border-slate-200 bg-white hover:border-slate-300'
                                  }`}
                                >
                                  <div className="flex items-center gap-2">
                                    <span className="text-lg">{provider.icon}</span>
                                    <div>
                                      <div className="text-sm font-medium text-slate-900">{provider.name}</div>
                                      <div className="text-xs text-slate-500">{provider.desc}</div>
                                    </div>
                                  </div>
                                </button>
                              ))}
                            </div>
                          </div>

                          {/* Tour URL Input */}
                          <div className="mb-4">
                            <label className="block text-sm font-medium text-slate-700 mb-2">Tour URL</label>
                            <Input
                              placeholder="https://your-tour.panoee.com/apartment123"
                              value={virtualTour?.tourUrl || ''}
                              onChange={(e) => handleVirtualTourUrlChange(e.target.value)}
                              className="rounded-xl"
                            />
                            <p className="text-xs text-slate-500 mt-1">
                              Paste the shareable link from your virtual tour provider
                            </p>
                          </div>

                          {/* Embed Code Input */}
                          <div className="mb-4">
                            <label className="block text-sm font-medium text-slate-700 mb-2">Embed Code (Optional)</label>
                            <textarea
                              placeholder='<iframe src="https://tour-url.com/embed" width="800" height="600" frameborder="0" allowfullscreen></iframe>'
                              value={virtualTour?.embedCode || ''}
                              onChange={(e) => handleEmbedCodeChange(e.target.value)}
                              className="w-full p-3 border border-slate-200 rounded-xl resize-none h-20 text-sm font-mono"
                            />
                            <p className="text-xs text-slate-500 mt-1">
                              If you have embed code, paste it here for better integration
                            </p>
                          </div>
                        </div>

                        {/* Help Section */}
                        <div className="bg-slate-50 rounded-xl p-4">
                          <h4 className="font-medium text-slate-900 mb-3">📖 How to Create a Virtual Tour</h4>
                          <div className="space-y-3 text-sm text-slate-600">
                            <div className="flex items-start gap-2">
                              <span className="text-blue-600">1️⃣</span>
                              <div>
                                <strong>Choose a Provider:</strong> We recommend <strong>Panoee</strong> (free) or <strong>Kuula</strong> for best results
                              </div>
                            </div>
                            <div className="flex items-start gap-2">
                              <span className="text-blue-600">2️⃣</span>
                              <div>
                                <strong>Capture Photos:</strong> Use your phone to take 360° photos in each room, or hire a photographer
                              </div>
                            </div>
                            <div className="flex items-start gap-2">
                              <span className="text-blue-600">3️⃣</span>
                              <div>
                                <strong>Create Tour:</strong> Upload photos to the platform, add hotspots, and publish
                              </div>
                            </div>
                            <div className="flex items-start gap-2">
                              <span className="text-blue-600">4️⃣</span>
                              <div>
                                <strong>Get Link:</strong> Copy the shareable URL or embed code and paste it above
                              </div>
                            </div>
                          </div>
                          
                          <div className="mt-4 p-3 bg-white rounded-lg border border-slate-200">
                            <div className="text-xs font-medium text-slate-900 mb-2">💡 Pro Tips:</div>
                            <ul className="text-xs text-slate-600 space-y-1">
                              <li>• Good lighting makes a huge difference</li>
                              <li>• Capture from center of each room for best 360° effect</li>
                              <li>• Add hotspots to highlight features</li>
                              <li>• Most providers offer free plans with basic features</li>
                            </ul>
                          </div>
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
            {submitError && (
              <div className="max-w-lg mx-auto mb-3 px-4 py-2.5 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
                {submitError}
              </div>
            )}
            <div className="max-w-lg mx-auto flex gap-3">
              {currentStep > 1 && <Button variant="outline" onClick={prevStep} className="flex-1 h-12 rounded-xl"><ArrowLeft className="h-4 w-4 mr-2" />Back</Button>}
              {currentStep < 5 ? (<Button onClick={() => { setSubmitError(null); nextStep() }} disabled={!canProceed()} className="flex-1 h-12 rounded-xl">Next<ArrowRight className="h-4 w-4 ml-2" /></Button>) : (<Button onClick={handleSubmit} disabled={isSubmitting || !canProceed()} className="flex-1 h-12 rounded-xl bg-emerald-600 hover:bg-emerald-700">{isSubmitting ? "Publishing..." : "Publish Listing"}</Button>)}
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
