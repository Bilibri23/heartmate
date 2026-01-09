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

interface Preferences {
  minBudget: number | null
  maxBudget: number | null
  preferredLocations: string[]
  maxDistanceFromCampus: number | null
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
}

const defaultPreferences: Preferences = {
  minBudget: null,
  maxBudget: null,
  preferredLocations: [],
  maxDistanceFromCampus: null,
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
}

const STEPS = [
  { id: "budget", title: "Budget", icon: Wallet },
  { id: "lifestyle", title: "Lifestyle", icon: Sparkles },
  { id: "schedule", title: "Schedule", icon: Moon },
  { id: "habits", title: "Habits", icon: Users },
  { id: "roommate", title: "Roommate", icon: UserPlus },
]

export default function PreferencesPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  
  const [preferences, setPreferences] = useState<Preferences>(defaultPreferences)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [currentStep, setCurrentStep] = useState(0)
  const [hasExisting, setHasExisting] = useState(false)
  const [locationInput, setLocationInput] = useState("")

  useEffect(() => {
    const fetchPreferences = async () => {
      if (!user?.id) return
      
      try {
        const response = await api.get(`/preferences/${user.id}`)
        if (response.data) {
          setPreferences({ ...defaultPreferences, ...response.data })
          setHasExisting(true)
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

  const handleSave = async () => {
    if (!user?.id) return
    
    setIsSaving(true)
    try {
      if (hasExisting) {
        await api.put(`/preferences/${user.id}`, preferences)
      } else {
        await api.post(`/preferences?userId=${user.id}`, preferences)
      }
      router.push("/matches")
    } catch (err) {
      console.error("Failed to save preferences:", err)
    } finally {
      setIsSaving(false)
    }
  }

  const addLocation = () => {
    if (locationInput.trim() && !preferences.preferredLocations.includes(locationInput.trim())) {
      setPreferences(prev => ({
        ...prev,
        preferredLocations: [...prev.preferredLocations, locationInput.trim()]
      }))
      setLocationInput("")
    }
  }

  const removeLocation = (location: string) => {
    setPreferences(prev => ({
      ...prev,
      preferredLocations: prev.preferredLocations.filter(l => l !== location)
    }))
  }

  const nextStep = () => {
    if (currentStep < STEPS.length - 1) {
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
    switch (STEPS[currentStep].id) {
      case "budget":
        return (
          <div className="space-y-6">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Wallet className="h-8 w-8 text-blue-600" />
              </div>
              <h2 className="text-xl font-bold text-slate-900">What's your budget?</h2>
              <p className="text-slate-500 text-sm mt-1">Set your monthly rent range in FCFA</p>
            </div>

            <div className="bg-white rounded-2xl p-4 space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-700 mb-2 block">
                  Minimum Budget (FCFA)
                </label>
                <Input
                  type="number"
                  placeholder="e.g., 25000"
                  value={preferences.minBudget || ""}
                  onChange={(e) => setPreferences(prev => ({
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
                  value={preferences.maxBudget || ""}
                  onChange={(e) => setPreferences(prev => ({
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
                  Preferred Locations
                </label>
              </div>
              
              <div className="flex gap-2">
                <Input
                  placeholder="Add a neighborhood..."
                  value={locationInput}
                  onChange={(e) => setLocationInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && addLocation()}
                  className="rounded-xl flex-1"
                />
                <Button onClick={addLocation} size="sm" className="rounded-xl">
                  Add
                </Button>
              </div>
              
              {preferences.preferredLocations.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {preferences.preferredLocations.map((loc) => (
                    <span
                      key={loc}
                      className="px-3 py-1.5 bg-blue-100 text-blue-700 rounded-full text-sm flex items-center gap-1"
                    >
                      {loc}
                      <button
                        onClick={() => removeLocation(loc)}
                        className="ml-1 hover:text-blue-900"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>
        )

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
                    {preferences.cleanlinessLevel}/5
                  </span>
                </div>
                <Slider
                  value={[preferences.cleanlinessLevel]}
                  onValueChange={(value) => setPreferences(prev => ({
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
                    {preferences.noiseTolerance}/5
                  </span>
                </div>
                <Slider
                  value={[preferences.noiseTolerance]}
                  onValueChange={(value) => setPreferences(prev => ({
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
                    {preferences.socialLevel}/5
                  </span>
                </div>
                <Slider
                  value={[preferences.socialLevel]}
                  onValueChange={(value) => setPreferences(prev => ({
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
                  value={preferences.sleepSchedule}
                  onValueChange={(value) => setPreferences(prev => ({
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
                  value={preferences.studyTimePreference}
                  onValueChange={(value) => setPreferences(prev => ({
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
                  checked={preferences.smoking}
                  onCheckedChange={(checked) => setPreferences(prev => ({
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
                  checked={preferences.drinking}
                  onCheckedChange={(checked) => setPreferences(prev => ({
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
                  checked={preferences.pets}
                  onCheckedChange={(checked) => setPreferences(prev => ({
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
                  checked={preferences.guests}
                  onCheckedChange={(checked) => setPreferences(prev => ({
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
                  checked={preferences.cooking}
                  onCheckedChange={(checked) => setPreferences(prev => ({
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
                value={preferences.dealBreakers}
                onChange={(e) => setPreferences(prev => ({
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
                  checked={preferences.lookingForRoommate}
                  onCheckedChange={(checked) => setPreferences(prev => ({
                    ...prev,
                    lookingForRoommate: checked
                  }))}
                />
              </div>
            </div>

            {preferences.lookingForRoommate && (
              <>
                <div className="bg-white rounded-2xl p-4 space-y-4">
                  <div>
                    <label className="text-sm font-medium text-slate-700 mb-2 block">
                      Preferred Gender
                    </label>
                    <Select
                      value={preferences.preferredGender}
                      onValueChange={(value) => setPreferences(prev => ({
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
                        value={preferences.minAge || ""}
                        onChange={(e) => setPreferences(prev => ({
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
                        value={preferences.maxAge || ""}
                        onChange={(e) => setPreferences(prev => ({
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
                      checked={preferences.sameUniversity}
                      onCheckedChange={(checked) => setPreferences(prev => ({
                        ...prev,
                        sameUniversity: checked
                      }))}
                    />
                  </div>
                  <div className="flex items-center justify-between p-4">
                    <span className="text-slate-700">Same Faculty/Department</span>
                    <Switch
                      checked={preferences.sameFaculty}
                      onCheckedChange={(checked) => setPreferences(prev => ({
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
      <MobileHeader title="Tell Us About You" showBack />

      {/* Progress Steps */}
      <div className="bg-white border-b border-slate-100 px-4 py-3">
        <div className="flex justify-between items-center">
          {STEPS.map((step, index) => {
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

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 pb-32">
        {renderStepContent()}
      </div>

      {/* Navigation */}
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
        
        {currentStep < STEPS.length - 1 ? (
          <Button
            onClick={nextStep}
            className="flex-1 rounded-xl bg-blue-600 hover:bg-blue-700"
          >
            Next
            <ArrowRight className="h-4 w-4 ml-2" />
          </Button>
        ) : (
          <Button
            onClick={handleSave}
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
    </div>
  )
}
