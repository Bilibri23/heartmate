"use client"

import { useState, useEffect } from "react"
import {
    Sparkles, Search, Heart, MessageSquare, User,
    Home, Plus, ClipboardList, BarChart3,
    Users, ArrowRight, X
} from "lucide-react"
import { Button } from "@/components/ui/button"

interface TourStep {
    icon: React.ElementType
    title: string
    description: string
    color: string
}

const STUDENT_STEPS: TourStep[] = [
    {
        icon: Sparkles,
        title: "Your Personalized Feed",
        description: "Listings are ranked by how well they match your budget, location, and preferences. Look for the match percentage badge!",
        color: "bg-blue-100 text-blue-600",
    },
    {
        icon: Search,
        title: "Search & Filter",
        description: "Use the Search tab to filter by city, price range, property type, and more.",
        color: "bg-emerald-100 text-emerald-600",
    },
    {
        icon: Users,
        title: "Find Roommates",
        description: "Looking for shared housing? Swipe through compatible roommates matched by lifestyle, budget, and habits.",
        color: "bg-purple-100 text-purple-600",
    },
    {
        icon: Heart,
        title: "Save Favorites",
        description: "Tap the heart icon to save listings you like. This also helps the AI learn your taste!",
        color: "bg-red-100 text-red-600",
    },
    {
        icon: MessageSquare,
        title: "Message Landlords",
        description: "Apply for listings and chat directly with landlords — all in one place.",
        color: "bg-amber-100 text-amber-600",
    },
]

const LANDLORD_STEPS: TourStep[] = [
    {
        icon: Home,
        title: "Your Dashboard",
        description: "Track your listings, applications, occupancy, and revenue at a glance.",
        color: "bg-blue-100 text-blue-600",
    },
    {
        icon: Plus,
        title: "Add a Listing",
        description: "Create a listing in 5 easy steps: photos, details, pricing, amenities, and optional virtual tour.",
        color: "bg-emerald-100 text-emerald-600",
    },
    {
        icon: ClipboardList,
        title: "Manage Applications",
        description: "Review student applications, accept or reject, and move to lease creation.",
        color: "bg-purple-100 text-purple-600",
    },
    {
        icon: MessageSquare,
        title: "Chat with Tenants",
        description: "Communicate with applicants and tenants directly through in-app messaging.",
        color: "bg-amber-100 text-amber-600",
    },
    {
        icon: BarChart3,
        title: "Track Performance",
        description: "See how your listings perform — views, favorites, and application conversion rates.",
        color: "bg-red-100 text-red-600",
    },
]

interface QuickTourProps {
    role: "STUDENT" | "LANDLORD"
    storageKey?: string
}

export function QuickTour({ role, storageKey = "roombuddy_tour_completed" }: QuickTourProps) {
    const [isVisible, setIsVisible] = useState(false)
    const [currentStep, setCurrentStep] = useState(0)

    const steps = role === "LANDLORD" ? LANDLORD_STEPS : STUDENT_STEPS

    useEffect(() => {
        // Only show if tour hasn't been completed
        const completed = localStorage.getItem(storageKey)
        if (!completed) {
            // Small delay so the page loads first
            const timer = setTimeout(() => setIsVisible(true), 800)
            return () => clearTimeout(timer)
        }
    }, [storageKey])

    const handleNext = () => {
        if (currentStep < steps.length - 1) {
            setCurrentStep(prev => prev + 1)
        } else {
            handleDone()
        }
    }

    const handleSkip = () => {
        handleDone()
    }

    const handleDone = () => {
        localStorage.setItem(storageKey, "true")
        setIsVisible(false)
    }

    if (!isVisible) return null

    const step = steps[currentStep]
    const Icon = step.icon
    const isLast = currentStep === steps.length - 1

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm animate-in fade-in duration-300">
            <div className="relative mx-4 w-full max-w-sm bg-white rounded-3xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-300">
                {/* Close button */}
                <button
                    onClick={handleSkip}
                    className="absolute right-3 top-3 z-10 p-1.5 rounded-full bg-slate-100 hover:bg-slate-200 transition-colors"
                >
                    <X className="h-4 w-4 text-slate-500" />
                </button>

                {/* Progress dots */}
                <div className="flex justify-center gap-1.5 pt-5 pb-2">
                    {steps.map((_, i) => (
                        <div
                            key={i}
                            className={`h-1.5 rounded-full transition-all duration-300 ${i === currentStep
                                    ? "w-6 bg-blue-600"
                                    : i < currentStep
                                        ? "w-1.5 bg-blue-300"
                                        : "w-1.5 bg-slate-200"
                                }`}
                        />
                    ))}
                </div>

                {/* Content */}
                <div className="px-6 py-6 text-center">
                    {/* Icon */}
                    <div className={`mx-auto mb-5 h-16 w-16 rounded-2xl ${step.color} flex items-center justify-center transition-all duration-300`}>
                        <Icon className="h-8 w-8" />
                    </div>

                    {/* Title */}
                    <h3 className="text-xl font-bold text-slate-900 mb-2">
                        {step.title}
                    </h3>

                    {/* Description */}
                    <p className="text-sm text-slate-500 leading-relaxed max-w-xs mx-auto">
                        {step.description}
                    </p>
                </div>

                {/* Actions */}
                <div className="px-6 pb-6 flex gap-3">
                    {!isLast && (
                        <Button
                            variant="ghost"
                            onClick={handleSkip}
                            className="flex-1 h-11 rounded-xl text-slate-500"
                        >
                            Skip
                        </Button>
                    )}
                    <Button
                        onClick={handleNext}
                        className={`h-11 rounded-xl bg-blue-600 hover:bg-blue-700 ${isLast ? "flex-1" : "flex-[2]"}`}
                    >
                        {isLast ? (
                            <>
                                Let's Go!
                                <Sparkles className="h-4 w-4 ml-2" />
                            </>
                        ) : (
                            <>
                                Next
                                <ArrowRight className="h-4 w-4 ml-2" />
                            </>
                        )}
                    </Button>
                </div>

                {/* Step counter */}
                <div className="text-center pb-4">
                    <span className="text-xs text-slate-400">
                        {currentStep + 1} of {steps.length}
                    </span>
                </div>
            </div>
        </div>
    )
}
