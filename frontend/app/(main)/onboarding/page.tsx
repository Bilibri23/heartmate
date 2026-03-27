"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import { MapPin, Wallet, Home, ArrowRight, ArrowLeft, Check, Sparkles, Users, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Slider } from "@/components/ui/slider"
import api from "@/lib/api"

const CITIES = [
    "Douala", "Yaoundé", "Buea", "Bamenda", "Bafoussam",
    "Limbe", "Kribi", "Garoua", "Maroua", "Ngaoundéré",
    "Bertoua", "Ebolowa",
]

const PROPERTY_TYPES = [
    { value: "STUDIO", label: "Studio", emoji: "🏢" },
    { value: "APARTMENT", label: "Apartment", emoji: "🏠" },
    { value: "HOUSE", label: "House", emoji: "🏡" },
    { value: "SHARED_ROOM", label: "Shared Room", emoji: "🛏️" },
    { value: "PRIVATE_ROOM", label: "Private Room", emoji: "🚪" },
]

const STEPS = [
    { id: "city", title: "Where are you looking?", subtitle: "Pick your city or cities", icon: MapPin },
    { id: "budget", title: "What's your budget?", subtitle: "Monthly rent range in FCFA", icon: Wallet },
    { id: "type", title: "What type of place?", subtitle: "Select one or more", icon: Home },
]

export default function OnboardingPage() {
    const router = useRouter()
    const { user } = useAuth()
    const [currentStep, setCurrentStep] = useState(0)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [showRoommateModal, setShowRoommateModal] = useState(false)

    // Form state
    const [selectedCities, setSelectedCities] = useState<string[]>([])
    const [budgetRange, setBudgetRange] = useState<[number, number]>([15000, 80000])
    const [selectedTypes, setSelectedTypes] = useState<string[]>([])

    const toggleCity = (city: string) => {
        setSelectedCities(prev =>
            prev.includes(city) ? prev.filter(c => c !== city) : [...prev, city]
        )
    }

    const toggleType = (type: string) => {
        setSelectedTypes(prev =>
            prev.includes(type) ? prev.filter(t => t !== type) : [...prev, type]
        )
    }

    const canProceed = () => {
        if (currentStep === 0) return selectedCities.length > 0
        if (currentStep === 1) return true // budget always valid
        if (currentStep === 2) return selectedTypes.length > 0
        return false
    }

    const handleNext = () => {
        if (currentStep < STEPS.length - 1) {
            setCurrentStep(prev => prev + 1)
        } else {
            handleComplete()
        }
    }

    const handleBack = () => {
        if (currentStep > 0) setCurrentStep(prev => prev - 1)
    }

    const handleSkip = () => {
        router.push("/for-you")
    }

    const hasSharedType = selectedTypes.includes("SHARED_ROOM") || selectedTypes.includes("PRIVATE_ROOM")

    const handleComplete = async () => {
        if (!user?.id) return

        setIsSubmitting(true)
        try {
            const preferences = {
                minBudget: budgetRange[0],
                maxBudget: budgetRange[1],
                preferredLocations: selectedCities,
                propertyTypes: selectedTypes,
                maxDistanceFromCampus: null,
            }

            await api.post(`/listing-preferences?userId=${user.id}`, preferences)

            // If they selected shared housing, show roommate matching suggestion
            if (hasSharedType) {
                setShowRoommateModal(true)
            } else {
                router.push("/for-you")
            }
        } catch (err) {
            console.error("Failed to save preferences:", err)
            // Still redirect — they can set preferences later
            if (hasSharedType) {
                setShowRoommateModal(true)
            } else {
                router.push("/for-you")
            }
        } finally {
            setIsSubmitting(false)
        }
    }

    const step = STEPS[currentStep]
    const StepIcon = step.icon

    return (
        <div className="flex min-h-screen flex-col bg-gradient-to-b from-blue-50 to-white">
            {/* Roommate Matching Suggestion Modal */}
            {showRoommateModal && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm animate-in fade-in duration-300">
                    <div className="relative mx-4 w-full max-w-sm bg-white rounded-3xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                        <button
                            onClick={() => { setShowRoommateModal(false); router.push("/for-you"); }}
                            className="absolute right-3 top-3 z-10 p-1.5 rounded-full bg-slate-100 hover:bg-slate-200 transition-colors"
                        >
                            <X className="h-4 w-4 text-slate-500" />
                        </button>

                        <div className="px-6 py-8 text-center">
                            {/* Icon */}
                            <div className="mx-auto mb-5 h-20 w-20 rounded-2xl bg-gradient-to-br from-purple-100 to-blue-100 flex items-center justify-center">
                                <Users className="h-10 w-10 text-purple-600" />
                            </div>

                            {/* Title */}
                            <h3 className="text-xl font-bold text-slate-900 mb-2">
                                Find a Compatible Roommate!
                            </h3>

                            {/* Description */}
                            <p className="text-sm text-slate-500 leading-relaxed max-w-xs mx-auto mb-2">
                                Since you&apos;re looking for shared housing, we can match you with compatible roommates based on:
                            </p>

                            {/* Match criteria chips */}
                            <div className="flex flex-wrap justify-center gap-2 mb-6">
                                {["💰 Budget", "🧹 Lifestyle", "🕐 Schedule", "📍 Location", "🚬 Habits"].map((item) => (
                                    <span key={item} className="px-3 py-1 rounded-full bg-purple-50 text-purple-700 text-xs font-medium">
                                        {item}
                                    </span>
                                ))}
                            </div>
                        </div>

                        {/* Actions */}
                        <div className="px-6 pb-6 space-y-3">
                            <Button
                                onClick={() => router.push("/matches")}
                                className="w-full h-12 rounded-xl bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700"
                            >
                                <Users className="h-4 w-4 mr-2" />
                                Find a Roommate
                            </Button>
                            <Button
                                variant="ghost"
                                onClick={() => { setShowRoommateModal(false); router.push("/for-you"); }}
                                className="w-full h-11 rounded-xl text-slate-500"
                            >
                                Skip for now — browse listings
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* Header */}
            <div className="sticky top-0 z-40 bg-white/90 backdrop-blur-lg border-b border-slate-100">
                <div className="flex items-center justify-between px-4 h-14">
                    <div className="flex items-center gap-2">
                        <Sparkles className="h-5 w-5 text-blue-600" />
                        <span className="font-bold text-slate-900">Quick Setup</span>
                    </div>
                    <button
                        onClick={handleSkip}
                        className="text-sm text-slate-500 hover:text-slate-700"
                    >
                        Skip for now
                    </button>
                </div>
                {/* Progress bar */}
                <div className="h-1 bg-slate-100">
                    <div
                        className="h-full bg-blue-600 transition-all duration-500 ease-out"
                        style={{ width: `${((currentStep + 1) / STEPS.length) * 100}%` }}
                    />
                </div>
            </div>

            {/* Content */}
            <div className="flex-1 flex flex-col px-4 py-8 max-w-lg mx-auto w-full">
                {/* Step indicator */}
                <div className="flex items-center gap-2 mb-2">
                    {STEPS.map((_, i) => (
                        <div
                            key={i}
                            className={`h-2 flex-1 rounded-full transition-colors ${i <= currentStep ? "bg-blue-600" : "bg-slate-200"
                                }`}
                        />
                    ))}
                </div>
                <p className="text-xs text-slate-500 mb-6">
                    Step {currentStep + 1} of {STEPS.length}
                </p>

                {/* Step header */}
                <div className="mb-8">
                    <div className={`h-14 w-14 rounded-2xl bg-blue-100 flex items-center justify-center mb-4`}>
                        <StepIcon className="h-7 w-7 text-blue-600" />
                    </div>
                    <h1 className="text-2xl font-bold text-slate-900">{step.title}</h1>
                    <p className="text-slate-500 mt-1">{step.subtitle}</p>
                </div>

                {/* Step content */}
                <div className="flex-1">
                    {/* Step 1: City selection */}
                    {currentStep === 0 && (
                        <div className="grid grid-cols-2 gap-3">
                            {CITIES.map(city => (
                                <button
                                    key={city}
                                    onClick={() => toggleCity(city)}
                                    className={`px-4 py-3 rounded-xl text-sm font-medium transition-all border-2 ${selectedCities.includes(city)
                                        ? "border-blue-600 bg-blue-50 text-blue-700 shadow-sm"
                                        : "border-slate-200 bg-white text-slate-700 hover:border-slate-300"
                                        }`}
                                >
                                    <MapPin className={`h-4 w-4 inline mr-1.5 ${selectedCities.includes(city) ? "text-blue-600" : "text-slate-400"
                                        }`} />
                                    {city}
                                </button>
                            ))}
                        </div>
                    )}

                    {/* Step 2: Budget range */}
                    {currentStep === 1 && (
                        <div className="space-y-8">
                            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
                                <div className="flex items-center justify-between mb-6">
                                    <div>
                                        <p className="text-sm text-slate-500">Min budget</p>
                                        <p className="text-2xl font-bold text-slate-900">
                                            {budgetRange[0].toLocaleString()} <span className="text-sm font-normal text-slate-500">FCFA</span>
                                        </p>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-sm text-slate-500">Max budget</p>
                                        <p className="text-2xl font-bold text-slate-900">
                                            {budgetRange[1].toLocaleString()} <span className="text-sm font-normal text-slate-500">FCFA</span>
                                        </p>
                                    </div>
                                </div>

                                <Slider
                                    min={5000}
                                    max={500000}
                                    step={5000}
                                    value={budgetRange}
                                    onValueChange={(value) => setBudgetRange(value as [number, number])}
                                    className="mb-4"
                                />

                                <div className="flex justify-between text-xs text-slate-400">
                                    <span>5,000 FCFA</span>
                                    <span>500,000 FCFA</span>
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-2">
                                {[
                                    { label: "Budget", min: 10000, max: 30000 },
                                    { label: "Mid-range", min: 30000, max: 80000 },
                                    { label: "Premium", min: 80000, max: 250000 },
                                ].map(preset => (
                                    <button
                                        key={preset.label}
                                        onClick={() => setBudgetRange([preset.min, preset.max])}
                                        className="px-3 py-2 rounded-xl text-xs font-medium bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors"
                                    >
                                        {preset.label}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Step 3: Property type */}
                    {currentStep === 2 && (
                        <div className="grid grid-cols-1 gap-3">
                            {PROPERTY_TYPES.map(type => (
                                <button
                                    key={type.value}
                                    onClick={() => toggleType(type.value)}
                                    className={`flex items-center gap-4 px-5 py-4 rounded-xl text-left transition-all border-2 ${selectedTypes.includes(type.value)
                                        ? "border-blue-600 bg-blue-50 shadow-sm"
                                        : "border-slate-200 bg-white hover:border-slate-300"
                                        }`}
                                >
                                    <span className="text-2xl">{type.emoji}</span>
                                    <div className="flex-1">
                                        <span className={`font-medium ${selectedTypes.includes(type.value) ? "text-blue-700" : "text-slate-700"
                                            }`}>
                                            {type.label}
                                        </span>
                                        {(type.value === "SHARED_ROOM" || type.value === "PRIVATE_ROOM") && selectedTypes.includes(type.value) && (
                                            <p className="text-xs text-purple-600 mt-0.5 flex items-center gap-1">
                                                <Users className="h-3 w-3" />
                                                We&apos;ll help you find a compatible roommate!
                                            </p>
                                        )}
                                    </div>
                                    {selectedTypes.includes(type.value) && (
                                        <Check className="h-5 w-5 text-blue-600 ml-auto" />
                                    )}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {/* Navigation buttons */}
                <div className="flex gap-3 mt-8 pt-4 border-t border-slate-100">
                    {currentStep > 0 && (
                        <Button
                            variant="outline"
                            onClick={handleBack}
                            className="flex-1 h-12 rounded-xl"
                        >
                            <ArrowLeft className="h-4 w-4 mr-2" />
                            Back
                        </Button>
                    )}
                    <Button
                        onClick={handleNext}
                        disabled={!canProceed() || isSubmitting}
                        className="flex-1 h-12 rounded-xl bg-blue-600 hover:bg-blue-700"
                    >
                        {isSubmitting ? (
                            <div className="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full" />
                        ) : currentStep === STEPS.length - 1 ? (
                            <>
                                <Check className="h-4 w-4 mr-2" />
                                See My Recommendations
                            </>
                        ) : (
                            <>
                                Next
                                <ArrowRight className="h-4 w-4 ml-2" />
                            </>
                        )}
                    </Button>
                </div>
            </div>
        </div>
    )
}
