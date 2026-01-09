"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import { Home, Search, Building2, Shield, Star, ArrowRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import Link from "next/link"

export default function LandingPage() {
  const { user, isLoading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading && user) {
      router.replace("/for-you")
    }
  }, [user, isLoading, router])

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" />
      </div>
    )
  }

  if (user) {
    return null
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-lg border-b border-slate-100">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-9 w-9 rounded-xl bg-blue-600 flex items-center justify-center">
              <Home className="h-5 w-5 text-white" />
            </div>
            <span className="text-xl font-bold text-slate-900">RoomBuddy</span>
          </div>
          <div className="flex items-center gap-3">
            <Link href="/login">
              <Button variant="ghost" size="sm">Login</Button>
            </Link>
            <Link href="/register">
              <Button size="sm" className="rounded-full">Sign Up</Button>
            </Link>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="px-4 py-16 text-center">
        <div className="max-w-2xl mx-auto">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-100 text-blue-700 text-sm font-medium mb-6">
            <Star className="h-4 w-4" />
            #1 Student Housing in Cameroon
          </div>
          <h1 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4 leading-tight">
            Find Your Perfect
            <span className="text-blue-600"> Student Home</span>
          </h1>
          <p className="text-lg text-slate-600 mb-8 max-w-lg mx-auto">
            Discover verified rooms, apartments, and shared spaces near your university. Connect directly with trusted landlords.
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link href="/register?role=STUDENT">
              <Button size="lg" className="w-full sm:w-auto rounded-full px-8">
                <Search className="mr-2 h-5 w-5" />
                Find a Room
              </Button>
            </Link>
            <Link href="/register?role=LANDLORD">
              <Button size="lg" variant="outline" className="w-full sm:w-auto rounded-full px-8">
                <Building2 className="mr-2 h-5 w-5" />
                List Property
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="px-4 py-16 bg-white">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-2xl font-bold text-slate-900 text-center mb-12">
            Why Students Love RoomBuddy
          </h2>
          <div className="grid md:grid-cols-3 gap-8">
            <div className="text-center">
              <div className="mx-auto mb-4 h-14 w-14 rounded-2xl bg-blue-100 flex items-center justify-center">
                <Shield className="h-7 w-7 text-blue-600" />
              </div>
              <h3 className="font-semibold text-slate-900 mb-2">Verified Listings</h3>
              <p className="text-sm text-slate-600">
                All properties and landlords are verified for your safety and peace of mind.
              </p>
            </div>
            <div className="text-center">
              <div className="mx-auto mb-4 h-14 w-14 rounded-2xl bg-emerald-100 flex items-center justify-center">
                <Search className="h-7 w-7 text-emerald-600" />
              </div>
              <h3 className="font-semibold text-slate-900 mb-2">Smart Search</h3>
              <p className="text-sm text-slate-600">
                AI-powered recommendations based on your preferences and budget.
              </p>
            </div>
            <div className="text-center">
              <div className="mx-auto mb-4 h-14 w-14 rounded-2xl bg-amber-100 flex items-center justify-center">
                <Building2 className="h-7 w-7 text-amber-600" />
              </div>
              <h3 className="font-semibold text-slate-900 mb-2">Easy Payments</h3>
              <p className="text-sm text-slate-600">
                Pay rent securely via Mobile Money (MTN, Orange) or card.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Cities */}
      <section className="px-4 py-16">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-2xl font-bold text-slate-900 mb-8">
            Available in Major Cities
          </h2>
          <div className="flex flex-wrap justify-center gap-3">
            {["Douala", "Yaoundé", "Buea", "Bamenda", "Bafoussam", "Limbe"].map((city) => (
              <span
                key={city}
                className="px-4 py-2 rounded-full bg-slate-100 text-slate-700 font-medium"
              >
                {city}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="px-4 py-16 bg-blue-600">
        <div className="max-w-2xl mx-auto text-center">
          <h2 className="text-2xl md:text-3xl font-bold text-white mb-4">
            Ready to Find Your New Home?
          </h2>
          <p className="text-blue-100 mb-8">
            Join thousands of students who found their perfect accommodation through RoomBuddy.
          </p>
          <Link href="/register">
            <Button size="lg" variant="secondary" className="rounded-full px-8">
              Get Started Free
              <ArrowRight className="ml-2 h-5 w-5" />
            </Button>
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="px-4 py-8 bg-slate-900 text-slate-400">
        <div className="max-w-4xl mx-auto text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <div className="h-8 w-8 rounded-lg bg-blue-600 flex items-center justify-center">
              <Home className="h-4 w-4 text-white" />
            </div>
            <span className="text-lg font-bold text-white">RoomBuddy</span>
          </div>
          <p className="text-sm">
            © 2024 RoomBuddy. Made with ❤️ in Cameroon.
          </p>
        </div>
      </footer>
    </div>
  )
}
