"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import { Slider } from "@/components/ui/slider"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { Skeleton } from "@/components/ui/skeleton"
import api from "@/lib/api"
import { 
  Wallet, 
  MapPin, 
  Sparkles, 
  Moon, 
  Sun, 
  Users, 
  Cigarette, 
  Wine, 
  Dog, 
  UserPlus,
  ChefHat,
  Save,
  ArrowRight,
  ArrowLeft,
  Check
} from "lucide-react"

interface ListingPreferences {
  minBudget: number | null
  maxBudget: number | null
  preferredLocations: string[]
  maxDistanceFromCampus: number | null
  propertyTypes: string[]
}

interface RoommatePreferences {
  cleanlinessLevel: number
  noiseTolerance: number
  socialLevel: number
  sleepSchedule: string
  studyTimePreference: string
  smoking: boolean
  drinking: boolean
  pets: boolean
  guests: boolean
  cooking: boolean
  dealBreakers: string
  preferredGender: string
  minAge: number | null
  maxAge: number | null
  sameUniversity: boolean
  sameFaculty: boolean
  lookingForRoommate: boolean
  minBudget: number | null
  maxBudget: number | null
  preferredLocations: string[]
  maxDistanceFromCampus: number | null
}

const defaultListingPreferences: ListingPreferences = {
  minBudget: null,
  maxBudget: null,
  preferredLocations: [],
  maxDistanceFromCampus: null,
  propertyTypes: [],
}

const defaultRoommatePreferences: RoommatePreferences = {
  cleanlinessLevel: 3,
  noiseTolerance: 3,
  socialLevel: 3,
  sleepSchedule: "FLEXIBLE",
  studyTimePreference: "FLEXIBLE",
  smoking: false,
  drinking: false,
  pets: false,
  guests: true,
  cooking: true,
  dealBreakers: "",
  preferredGender: "ANY",
  minAge: null,
  maxAge: null,
  sameUniversity: false,
  sameFaculty: false,
  lookingForRoommate: true,
  minBudget: null,
  maxBudget: null,
  preferredLocations: [],
  maxDistanceFromCampus: null,
}

const PROPERTY_TYPE_OPTIONS = [
  { value: "STUDIO", label: "Studio" },
  { value: "APARTMENT", label: "Apartment" },
  { value: "HOUSE", label: "House" },
  { value: "SHARED_ROOM", label: "Shared Room" },
  { value: "PRIVATE_ROOM", label: "Private Room" },
]

// Popular neighborhoods in Cameroon organized by city
const NEIGHBORHOOD_OPTIONS = {
  "Douala": [
    "Bonamoussadi",
    "Makepe",
    "Logpom",
    "Kotto",
    "Bonapriso",
    "Akwa",
    "Deido",
    "Bepanda",
    "Ndokotti",
    "Bonaberi",
    "PK8",
    "PK10",
    "PK14",
    "Yassa",
    "Logbessou",
  ],
  "Yaoundé": [
    "Bastos",
    "Nlongkak",
    "Mvan",
    "Nsimeyong",
    "Biyem-Assi",
    "Mendong",
    "Omnisport",
    "Mimboman",
    "Mvog-Betsi",
    "Essos",
    "Ngousso",
    "Odza",
    "Nkolbisson",
    "Soa",
  ],
  "Buea": [
    "Molyko",
    "Bonduma",
    "Great Soppo",
    "Clerks Quarters",
    "Mile 17",
    "Bomaka",
    "Buea Town",
  ],
  "Bamenda": [
    "Up Station",
    "Down Town",
    "Nkwen",
    "Mile 4",
    "Mile 3",
    "Ntarinkon",
    "Old Town",
  ],
}

const ROOMMATE_STEPS = [
  { id: "lifestyle", title: "Lifestyle", icon: Sparkles },
  { id: "schedule", title: "Schedule", icon: Moon },
  { id: "habits", title: "Habits", icon: Users },
  { id: "roommate", title: "Roommate", icon: UserPlus },
]

export default function PreferencesPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  
  const [activeSection, setActiveSection] = useState<"listing" | "roommate">("listing")
  const [listingPreferences, setListingPreferences] = useState<ListingPreferences>(defaultListingPreferences)
  const [roommatePreferences, setRoommatePreferences] = useState<RoommatePreferences>(defaultRoommatePreferences)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [currentStep, setCurrentStep] = useState(0)
  const [hasListing, setHasListing] = useState(false)
  const [hasRoommate, setHasRoommate] = useState(false)
  const [locationInput, setLocationInput] = useState("")
  const [selectedCity, setSelectedCity] = useState<string>("Douala")

  useEffect(() => {
    const fetchPreferences = async () => {
      if (!user?.id) return
      
      try {
        const [listingRes, roommateRes] = await Promise.allSettled([
          api.get(`/listing-preferences/${user.id}`),
          api.get(`/preferences/${user.id}`),
        ])

        if (listingRes.status === "fulfilled" && listingRes.value.data) {
          const data = listingRes.value.data
          setListingPreferences({
            ...defaultListingPreferences,
            ...data,
            // Ensure arrays are never null
            preferredLocations: data.preferredLocations || [],
            propertyTypes: data.propertyTypes || [],
          })
          setHasListing(true)
        }
        if (roommateRes.status === "fulfilled" && roommateRes.value.data) {
          setRoommatePreferences({ ...defaultRoommatePreferences, ...roommateRes.value.data })
          setHasRoommate(true)
        }
      } catch (err: any) {
        if (err.response?.status !== 404) {
          console.error("Failed to fetch preferences:", err)
        }
      } finally {
        setIsLoading(false)
      }
    }
    
    fetchPreferences()
  }, [user?.id])

  // Sync budget and location preferences to roommate preferences
  // Using JSON.stringify to create a stable dependency
  const listingPrefsKey = JSON.stringify({
    minBudget: listingPreferences.minBudget,
    maxBudget: listingPreferences.maxBudget,
    preferredLocations: listingPreferences.preferredLocations,
  })
  
  useEffect(() => {
    setRoommatePreferences((prev) => ({
      ...prev,
      minBudget: listingPreferences.minBudget,
      maxBudget: listingPreferences.maxBudget,
      preferredLocations: listingPreferences.preferredLocations,
    }))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [listingPrefsKey])

  const handleSaveListing = async () => {
    if (!user?.id) return
    
    setIsSaving(true)
    try {
      if (hasListing) {
        await api.put(`/listing-preferences/${user.id}`, listingPreferences)
      } else {
        await api.post(`/listing-preferences?userId=${user.id}`, listingPreferences)
        setHasListing(true)
      }
    } catch (err) {
      console.error("Failed to save listing preferences:", err)
    } finally {
      setIsSaving(false)
    }
  }

  const handleSaveRoommate = async () => {
    if (!user?.id) return
    
    setIsSaving(true)
    try {
      if (hasRoommate) {
        await api.put(`/preferences/${user.id}`, roommatePreferences)
      } else {
        await api.post(`/preferences?userId=${user.id}`, roommatePreferences)
        setHasRoommate(true)
      }
      router.push("/matches")
    } catch (err) {
      console.error("Failed to save roommate preferences:", err)
    } finally {
      setIsSaving(false)
    }
  }

  const addLocation = () => {
    if (locationInput.trim() && !listingPreferences.preferredLocations.includes(locationInput.trim())) {
      setListingPreferences(prev => ({
        ...prev,
        preferredLocations: [...prev.preferredLocations, locationInput.trim()]
      }))
      setLocationInput("")
    }
  }

  const removeLocation = (location: string) => {
    setListingPreferences(prev => ({
      ...prev,
      preferredLocations: prev.preferredLocations.filter(l => l !== location)
    }))
  }

  const toggleNeighborhood = (neighborhood: string) => {
    setListingPreferences(prev => {
      const isSelected = prev.preferredLocations.includes(neighborhood)
      return {
        ...prev,
        preferredLocations: isSelected
          ? prev.preferredLocations.filter(l => l !== neighborhood)
          : [...prev.preferredLocations, neighborhood]
      }
    })
  }

  const nextStep = () => {
    if (currentStep < ROOMMATE_STEPS.length - 1) {
      setCurrentStep(prev => prev + 1)
    }
  }

  const prevStep = () => {
    if (currentStep > 0) {
      setCurrentStep(prev => prev - 1)
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col min-h-screen bg-slate-50">
        <MobileHeader title="Tell Us About You" />
        <div className="flex-1 p-4 space-y-4">
          <Skeleton className="h-32 w-full rounded-2xl" />
          <Skeleton className="h-32 w-full rounded-2xl" />
          <Skeleton className="h-32 w-full rounded-2xl" />
        </div>
      </div>
    )
  }

  const renderStepContent = () => {
    switch (ROOMMATE_STEPS[currentStep].id) {
      case "lifestyle":
        return (
          <div className="space-y-6">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Sparkles className="h-8 w-8 text-purple-600" />
              </div>
              <h2 className="text-xl font-bold text-slate-900">Your Lifestyle</h2>
              <p className="text-slate-500 text-sm mt-1">Help us find compatible roommates</p>
            </div>

            <div className="bg-white rounded-2xl p-4 space-y-6">
              <div>
                <div className="flex justify-between items-center mb-3">
                  <label className="text-sm font-medium text-slate-700">
                    Cleanliness Level
                  </label>
                  <span className="text-sm text-blue-600 font-medium">
                    {roommatePreferences.cleanlinessLevel}/5
                  </span>
                </div>
                <Slider
                  value={[roommatePreferences.cleanlinessLevel]}
                  onValueChange={(value) => setRoommatePreferences(prev => ({
                    ...prev,
                    cleanlinessLevel: value[0]
                  }))}
                  min={1}
                  max={5}
                  step={1}
                  className="w-full"
                />
                <div className="flex justify-between text-xs text-slate-400 mt-1">
                  <span>Relaxed</span>
                  <span>Very Tidy</span>
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center mb-3">
                  <label className="text-sm font-medium text-slate-700">
                    Noise Tolerance
                  </label>
                  <span className="text-sm text-blue-600 font-medium">
                    {roommatePreferences.noiseTolerance}/5
                  </span>
                </div>
                <Slider
                  value={[roommatePreferences.noiseTolerance]}
                  onValueChange={(value) => setRoommatePreferences(prev => ({
                    ...prev,
                    noiseTolerance: value[0]
                  }))}
                  min={1}
                  max={5}
                  step={1}
                  className="w-full"
                />
                <div className="flex justify-between text-xs text-slate-400 mt-1">
                  <span>Need Quiet</span>
                  <span>Don't Mind Noise</span>
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center mb-3">
                  <label className="text-sm font-medium text-slate-700">
                    Social Level
                  </label>
                  <span className="text-sm text-blue-600 font-medium">
                    {roommatePreferences.socialLevel}/5
                  </span>
                </div>
                <Slider
                  value={[roommatePreferences.socialLevel]}
                  onValueChange={(value) => setRoommatePreferences(prev => ({
                    ...prev,
                    socialLevel: value[0]
                  }))}
                  min={1}
                  max={5}
                  step={1}
                  className="w-full"
                />
                <div className="flex justify-between text-xs text-slate-400 mt-1">
                  <span>Introvert</span>
                  <span>Extrovert</span>
                </div>
              </div>
            </div>
          </div>
        )

      case "schedule":
        return (
          <div className="space-y-6">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Moon className="h-8 w-8 text-amber-600" />
              </div>
              <h2 className="text-xl font-bold text-slate-900">Your Schedule</h2>
              <p className="text-slate-500 text-sm mt-1">When do you sleep and study?</p>
            </div>

            <div className="bg-white rounded-2xl p-4 space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-700 mb-2 block">
                  Sleep Schedule
                </label>
                <Select
                  value={roommatePreferences.sleepSchedule}
                  onValueChange={(value) => setRoommatePreferences(prev => ({
                    ...prev,
                    sleepSchedule: value
                  }))}
                >
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="EARLY_BIRD">
                      <div className="flex items-center gap-2">
                        <Sun className="h-4 w-4" />
                        Early Bird (Sleep before 10pm)
                      </div>
                    </SelectItem>
                    <SelectItem value="NIGHT_OWL">
                      <div className="flex items-center gap-2">
                        <Moon className="h-4 w-4" />
                        Night Owl (Sleep after midnight)
                      </div>
                    </SelectItem>
                    <SelectItem value="FLEXIBLE">Flexible</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <label className="text-sm font-medium text-slate-700 mb-2 block">
                  Study Time Preference
                </label>
                <Select
                  value={roommatePreferences.studyTimePreference}
                  onValueChange={(value) => setRoommatePreferences(prev => ({
                    ...prev,
                    studyTimePreference: value
                  }))}
                >
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MORNING">Morning</SelectItem>
                    <SelectItem value="AFTERNOON">Afternoon</SelectItem>
                    <SelectItem value="EVENING">Evening</SelectItem>
                    <SelectItem value="NIGHT">Night</SelectItem>
                    <SelectItem value="FLEXIBLE">Flexible</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>
        )

      case "habits":
        return (
          <div className="space-y-6">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="h-8 w-8 text-green-600" />
              </div>
              <h2 className="text-xl font-bold text-slate-900">Your Habits</h2>
              <p className="text-slate-500 text-sm mt-1">What are you comfortable with?</p>
            </div>

            <div className="bg-white rounded-2xl divide-y divide-slate-100">
              <div className="flex items-center justify-between p-4">
                <div className="flex items-center gap-3">
                  <Cigarette className="h-5 w-5 text-slate-500" />
                  <span className="text-slate-700">Smoking</span>
                </div>
                <Switch
                  checked={roommatePreferences.smoking}
                  onCheckedChange={(checked) => setRoommatePreferences(prev => ({
                    ...prev,
                    smoking: checked
                  }))}
                />
              </div>

              <div className="flex items-center justify-between p-4">
                <div className="flex items-center gap-3">
                  <Wine className="h-5 w-5 text-slate-500" />
                  <span className="text-slate-700">Drinking</span>
                </div>
                <Switch
                  checked={roommatePreferences.drinking}
                  onCheckedChange={(checked) => setRoommatePreferences(prev => ({
                    ...prev,
                    drinking: checked
                  }))}
                />
              </div>

              <div className="flex items-center justify-between p-4">
                <div className="flex items-center gap-3">
                  <Dog className="h-5 w-5 text-slate-500" />
                  <span className="text-slate-700">Pets</span>
                </div>
                <Switch
                  checked={roommatePreferences.pets}
                  onCheckedChange={(checked) => setRoommatePreferences(prev => ({
                    ...prev,
                    pets: checked
                  }))}
                />
              </div>

              <div className="flex items-center justify-between p-4">
                <div className="flex items-center gap-3">
                  <UserPlus className="h-5 w-5 text-slate-500" />
                  <span className="text-slate-700">Guests Allowed</span>
                </div>
                <Switch
                  checked={roommatePreferences.guests}
                  onCheckedChange={(checked) => setRoommatePreferences(prev => ({
                    ...prev,
                    guests: checked
                  }))}
                />
              </div>

              <div className="flex items-center justify-between p-4">
                <div className="flex items-center gap-3">
                  <ChefHat className="h-5 w-5 text-slate-500" />
                  <span className="text-slate-700">Cooking at Home</span>
                </div>
                <Switch
                  checked={roommatePreferences.cooking}
                  onCheckedChange={(checked) => setRoommatePreferences(prev => ({
                    ...prev,
                    cooking: checked
                  }))}
                />
              </div>
            </div>

            <div className="bg-white rounded-2xl p-4">
              <label className="text-sm font-medium text-slate-700 mb-2 block">
                Deal Breakers (Optional)
              </label>
              <Textarea
                placeholder="Anything you absolutely can't live with..."
                value={roommatePreferences.dealBreakers}
                onChange={(e) => setRoommatePreferences(prev => ({
                  ...prev,
                  dealBreakers: e.target.value
                }))}
                className="rounded-xl resize-none"
                rows={3}
              />
            </div>
          </div>
        )

      case "roommate":
        return (
          <div className="space-y-6">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-rose-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <UserPlus className="h-8 w-8 text-rose-600" />
              </div>
              <h2 className="text-xl font-bold text-slate-900">Roommate Preferences</h2>
              <p className="text-slate-500 text-sm mt-1">Who would you like to live with?</p>
            </div>

            <div className="bg-white rounded-2xl p-4 space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-slate-700 font-medium">Looking for a Roommate?</span>
                <Switch
                  checked={roommatePreferences.lookingForRoommate}
                  onCheckedChange={(checked) => setRoommatePreferences(prev => ({
                    ...prev,
                    lookingForRoommate: checked
                  }))}
                />
              </div>
            </div>

            {roommatePreferences.lookingForRoommate && (
              <>
                <div className="bg-white rounded-2xl p-4 space-y-4">
                  <div>
                    <label className="text-sm font-medium text-slate-700 mb-2 block">
                      Preferred Gender
                    </label>
                    <Select
                      value={roommatePreferences.preferredGender}
                      onValueChange={(value) => setRoommatePreferences(prev => ({
                        ...prev,
                        preferredGender: value
                      }))}
                    >
                      <SelectTrigger className="rounded-xl">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ANY">Any</SelectItem>
                        <SelectItem value="MALE">Male</SelectItem>
                        <SelectItem value="FEMALE">Female</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="text-sm font-medium text-slate-700 mb-2 block">
                        Min Age
                      </label>
                      <Input
                        type="number"
                        placeholder="18"
                        value={roommatePreferences.minAge || ""}
                        onChange={(e) => setRoommatePreferences(prev => ({
                          ...prev,
                          minAge: e.target.value ? parseInt(e.target.value) : null
                        }))}
                        className="rounded-xl"
                      />
                    </div>
                    <div>
                      <label className="text-sm font-medium text-slate-700 mb-2 block">
                        Max Age
                      </label>
                      <Input
                        type="number"
                        placeholder="30"
                        value={roommatePreferences.maxAge || ""}
                        onChange={(e) => setRoommatePreferences(prev => ({
                          ...prev,
                          maxAge: e.target.value ? parseInt(e.target.value) : null
                        }))}
                        className="rounded-xl"
                      />
                    </div>
                  </div>
                </div>

                <div className="bg-white rounded-2xl divide-y divide-slate-100">
                  <div className="flex items-center justify-between p-4">
                    <span className="text-slate-700">Same University</span>
                    <Switch
                      checked={roommatePreferences.sameUniversity}
                      onCheckedChange={(checked) => setRoommatePreferences(prev => ({
                        ...prev,
                        sameUniversity: checked
                      }))}
                    />
                  </div>
                  <div className="flex items-center justify-between p-4">
                    <span className="text-slate-700">Same Faculty/Department</span>
                    <Switch
                      checked={roommatePreferences.sameFaculty}
                      onCheckedChange={(checked) => setRoommatePreferences(prev => ({
                        ...prev,
                        sameFaculty: checked
                      }))}
                    />
                  </div>
                </div>
              </>
            )}
          </div>
        )

      default:
        return null
    }
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Preferences" showBack />

      {/* Section Toggle */}
      <div className="bg-white border-b border-slate-100 px-4 py-3">
        <div className="flex gap-2">
          <Button
            variant={activeSection === "listing" ? "default" : "outline"}
            className="rounded-full"
            onClick={() => {
              setActiveSection("listing")
              setCurrentStep(0)
            }}
          >
            Listing Preferences
          </Button>
          <Button
            variant={activeSection === "roommate" ? "default" : "outline"}
            className="rounded-full"
            onClick={() => {
              setActiveSection("roommate")
              setCurrentStep(0)
            }}
          >
            Roommate Preferences
          </Button>
        </div>
      </div>

      {activeSection === "roommate" && (
        <div className="bg-white border-b border-slate-100 px-4 py-3">
          <div className="flex justify-between items-center">
            {ROOMMATE_STEPS.map((step, index) => {
              const Icon = step.icon
              const isActive = index === currentStep
              const isCompleted = index < currentStep
              
              return (
                <button
                  key={step.id}
                  onClick={() => setCurrentStep(index)}
                  className="flex flex-col items-center gap-1"
                >
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${
                    isActive 
                      ? "bg-blue-600 text-white" 
                      : isCompleted 
                        ? "bg-green-100 text-green-600"
                        : "bg-slate-100 text-slate-400"
                  }`}>
                    {isCompleted ? <Check className="h-5 w-5" /> : <Icon className="h-5 w-5" />}
                  </div>
                  <span className={`text-xs ${isActive ? "text-blue-600 font-medium" : "text-slate-400"}`}>
                    {step.title}
                  </span>
                </button>
              )
            })}
          </div>
        </div>
      )}

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 pb-32">
        {activeSection === "listing" ? (
          <div className="space-y-6">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Wallet className="h-8 w-8 text-blue-600" />
              </div>
              <h2 className="text-xl font-bold text-slate-900">Listing Preferences</h2>
              <p className="text-slate-500 text-sm mt-1">What kind of place are you looking for?</p>
            </div>

            <div className="bg-white rounded-2xl p-4 space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-700 mb-2 block">
                  Minimum Budget (FCFA)
                </label>
                <Input
                  type="number"
                  placeholder="e.g., 25000"
                  value={listingPreferences.minBudget || ""}
                  onChange={(e) => setListingPreferences(prev => ({
                    ...prev,
                    minBudget: e.target.value ? parseInt(e.target.value) : null
                  }))}
                  className="rounded-xl"
                />
              </div>
              
              <div>
                <label className="text-sm font-medium text-slate-700 mb-2 block">
                  Maximum Budget (FCFA)
                </label>
                <Input
                  type="number"
                  placeholder="e.g., 75000"
                  value={listingPreferences.maxBudget || ""}
                  onChange={(e) => setListingPreferences(prev => ({
                    ...prev,
                    maxBudget: e.target.value ? parseInt(e.target.value) : null
                  }))}
                  className="rounded-xl"
                />
              </div>
            </div>

            <div className="bg-white rounded-2xl p-4 space-y-4">
              <div className="flex items-center gap-2 mb-2">
                <MapPin className="h-5 w-5 text-blue-600" />
                <label className="text-sm font-medium text-slate-700">
                  Preferred Neighborhoods
                </label>
              </div>

              {/* Selected locations */}
              {listingPreferences.preferredLocations.length > 0 && (
                <div className="flex flex-wrap gap-2 pb-2 border-b border-slate-100">
                  {listingPreferences.preferredLocations.map((loc) => (
                    <span
                      key={loc}
                      className="px-3 py-1.5 bg-blue-600 text-white rounded-full text-sm flex items-center gap-1"
                    >
                      {loc}
                      <button
                        onClick={() => removeLocation(loc)}
                        className="ml-1 hover:text-blue-200"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              )}

              {/* City selector */}
              <div>
                <label className="text-xs text-slate-500 mb-2 block">Select city</label>
                <div className="flex flex-wrap gap-2">
                  {Object.keys(NEIGHBORHOOD_OPTIONS).map((city) => (
                    <button
                      key={city}
                      type="button"
                      onClick={() => setSelectedCity(city)}
                      className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                        selectedCity === city
                          ? "bg-slate-900 text-white"
                          : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                      }`}
                    >
                      {city}
                    </button>
                  ))}
                </div>
              </div>

              {/* Neighborhoods for selected city */}
              <div>
                <label className="text-xs text-slate-500 mb-2 block">
                  Tap to select neighborhoods in {selectedCity}
                </label>
                <div className="flex flex-wrap gap-2">
                  {NEIGHBORHOOD_OPTIONS[selectedCity as keyof typeof NEIGHBORHOOD_OPTIONS]?.map((neighborhood) => {
                    const isSelected = listingPreferences.preferredLocations.includes(neighborhood)
                    return (
                      <button
                        key={neighborhood}
                        type="button"
                        onClick={() => toggleNeighborhood(neighborhood)}
                        className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                          isSelected
                            ? "bg-blue-600 text-white"
                            : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                        }`}
                      >
                        {neighborhood}
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Custom location input */}
              <div className="pt-2 border-t border-slate-100">
                <label className="text-xs text-slate-500 mb-2 block">
                  Or add a custom location
                </label>
                <div className="flex gap-2">
                  <Input
                    placeholder="Type a neighborhood..."
                    value={locationInput}
                    onChange={(e) => setLocationInput(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && addLocation()}
                    className="rounded-xl flex-1"
                  />
                  <Button onClick={addLocation} size="sm" className="rounded-xl">
                    Add
                  </Button>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-2xl p-4 space-y-3">
              <label className="text-sm font-medium text-slate-700 block">
                Preferred Property Types
              </label>
              <div className="flex flex-wrap gap-2">
                {PROPERTY_TYPE_OPTIONS.map((type) => {
                  const isSelected = (listingPreferences.propertyTypes || []).includes(type.value)
                  return (
                    <button
                      key={type.value}
                      type="button"
                      onClick={() =>
                        setListingPreferences(prev => ({
                          ...prev,
                          propertyTypes: isSelected
                            ? (prev.propertyTypes || []).filter(t => t !== type.value)
                            : [...(prev.propertyTypes || []), type.value]
                        }))
                      }
                      className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                        isSelected ? "bg-blue-600 text-white" : "bg-slate-100 text-slate-600"
                      }`}
                    >
                      {type.label}
                    </button>
                  )
                })}
              </div>
            </div>
          </div>
        ) : (
          renderStepContent()
        )}
      </div>

      {activeSection === "roommate" ? (
        <div className="fixed bottom-20 left-0 right-0 bg-white border-t border-slate-100 p-4 flex gap-3">
          {currentStep > 0 && (
            <Button
              variant="outline"
              onClick={prevStep}
              className="flex-1 rounded-xl"
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back
            </Button>
          )}
          
          {currentStep < ROOMMATE_STEPS.length - 1 ? (
            <Button
              onClick={nextStep}
              className="flex-1 rounded-xl bg-blue-600 hover:bg-blue-700"
            >
              Next
              <ArrowRight className="h-4 w-4 ml-2" />
            </Button>
          ) : (
            <Button
              onClick={handleSaveRoommate}
              disabled={isSaving}
              className="flex-1 rounded-xl bg-green-600 hover:bg-green-700"
            >
              {isSaving ? (
                "Saving..."
              ) : (
                <>
                  <Save className="h-4 w-4 mr-2" />
                  Save & Find Matches
                </>
              )}
            </Button>
          )}
        </div>
      ) : (
        <div className="fixed bottom-20 left-0 right-0 bg-white border-t border-slate-100 p-4">
          <Button
            onClick={handleSaveListing}
            disabled={isSaving}
            className="w-full rounded-xl bg-blue-600 hover:bg-blue-700"
          >
            {isSaving ? "Saving..." : "Save Listing Preferences"}
          </Button>
        </div>
      )}
    </div>
  )
}
