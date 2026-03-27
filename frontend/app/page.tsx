"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import {
  Home,
  Search,
  Building2,
  Shield,
  ArrowRight,
  CheckCircle2,
  MessageSquare,
  FileCheck2,
  Bell,
  Sparkles,
  Users,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import Link from "next/link"

export default function LandingPage() {
  const { user, isLoading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading && user) {
      if (user.role === "ADMIN") {
        router.replace("/admin")
      } else if (user.role === "LANDLORD") {
        router.replace("/landlord")
      } else {
        router.replace("/for-you")
      }
    }
  }, [user, isLoading, router])

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
      </div>
    )
  }

  if (user) {
    return null
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 via-white to-white">
      <header className="sticky top-0 z-50 border-b border-slate-100 bg-white/80 backdrop-blur-lg">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600">
              <Home className="h-5 w-5 text-white" />
            </div>
            <span className="text-xl font-bold text-slate-900">RoomBay</span>
          </div>
          <nav className="hidden items-center gap-6 text-sm text-slate-600 md:flex">
            <a href="#how-it-works" className="transition hover:text-slate-900">How it works</a>
            <a href="#for-tenants" className="transition hover:text-slate-900">For tenants</a>
            <a href="#for-landlords" className="transition hover:text-slate-900">For landlords</a>
          </nav>
          <div className="flex items-center gap-2">
            <Link href="/login">
              <Button variant="ghost" size="sm">Login</Button>
            </Link>
            <Link href="/register">
              <Button size="sm" className="rounded-full">Create account</Button>
            </Link>
          </div>
        </div>
      </header>

      <section className="px-4 pb-10 pt-14 text-center md:pt-20">
        <div className="mx-auto max-w-3xl">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-700">
            <Sparkles className="h-4 w-4" />
            Verified rentals, roommate matching, and guided support
          </div>
          <h1 className="mb-4 text-4xl font-bold leading-tight text-slate-900 md:text-6xl">
            Rent smarter with
            <span className="text-blue-600"> RoomBay</span>
          </h1>
          <p className="mx-auto mb-8 max-w-2xl text-lg text-slate-600">
            Discover trusted listings, share options with matched roommates, and manage applications,
            leases, payments, and support in one place.
          </p>
          <div className="flex flex-col justify-center gap-3 sm:flex-row">
            <Link href="/listings">
              <Button
                size="lg"
                className="w-full rounded-full px-8 sm:w-auto"
                onClick={() => {
                  if (typeof window !== "undefined") {
                    window.dispatchEvent(new CustomEvent("analytics:anonymousBrowseStart"))
                  }
                }}
              >
                <Search className="mr-2 h-5 w-5" />
                Browse listings now
              </Button>
            </Link>
            <Link href="/register?role=STUDENT">
              <Button size="lg" variant="outline" className="w-full rounded-full px-8 sm:w-auto">
                <Search className="mr-2 h-5 w-5" />
                I am looking for a room
              </Button>
            </Link>
            <Link href="/register?role=LANDLORD">
              <Button size="lg" variant="outline" className="w-full rounded-full px-8 sm:w-auto">
                <Building2 className="mr-2 h-5 w-5" />
                I want to list a property
              </Button>
            </Link>
          </div>
          <div className="mt-8 grid gap-3 text-left sm:grid-cols-3">
            <div className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-sm font-semibold text-slate-900">Verification first</p>
              <p className="text-sm text-slate-600">ID and listing reviews reduce risky interactions.</p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-sm font-semibold text-slate-900">Roommate-ready flow</p>
              <p className="text-sm text-slate-600">Share listings with matched roommates instantly.</p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-sm font-semibold text-slate-900">Built-in platform brain</p>
              <p className="text-sm text-slate-600">Ask the AI assistant for guided next steps.</p>
            </div>
          </div>
        </div>
      </section>

      <section id="how-it-works" className="border-y border-slate-100 bg-white px-4 py-14">
        <div className="mx-auto max-w-6xl">
          <h2 className="mb-10 text-center text-2xl font-bold text-slate-900 md:text-3xl">
            How RoomBay works
          </h2>
          <div className="grid gap-5 md:grid-cols-4">
            <div className="rounded-2xl border border-slate-200 p-5">
              <Search className="mb-3 h-6 w-6 text-blue-600" />
              <h3 className="mb-1 font-semibold text-slate-900">1. Discover</h3>
              <p className="text-sm text-slate-600">Search listings that match your budget, city, and move-in needs.</p>
            </div>
            <div className="rounded-2xl border border-slate-200 p-5">
              <Users className="mb-3 h-6 w-6 text-indigo-600" />
              <h3 className="mb-1 font-semibold text-slate-900">2. Share</h3>
              <p className="text-sm text-slate-600">Send listings to matched roommates and coordinate decisions faster.</p>
            </div>
            <div className="rounded-2xl border border-slate-200 p-5">
              <FileCheck2 className="mb-3 h-6 w-6 text-emerald-600" />
              <h3 className="mb-1 font-semibold text-slate-900">3. Apply</h3>
              <p className="text-sm text-slate-600">Submit applications and complete verification and lease steps clearly.</p>
            </div>
            <div className="rounded-2xl border border-slate-200 p-5">
              <Bell className="mb-3 h-6 w-6 text-amber-600" />
              <h3 className="mb-1 font-semibold text-slate-900">4. Track</h3>
              <p className="text-sm text-slate-600">Get updates on approvals, payments, and important actions in-app.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="px-4 py-14">
        <div className="mx-auto grid max-w-6xl gap-6 md:grid-cols-2">
          <div id="for-tenants" className="rounded-2xl border border-slate-200 bg-white p-7">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50">
              <Search className="h-6 w-6 text-blue-700" />
            </div>
            <h3 className="mb-2 text-2xl font-bold text-slate-900">For Tenants</h3>
            <p className="mb-5 text-slate-600">From search to move-in, manage your complete rental journey in one flow.</p>
            <ul className="space-y-3 text-sm text-slate-700">
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-blue-600" /> Verified listings and landlord profiles</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-blue-600" /> Share listing with matched roommate</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-blue-600" /> Application, lease, and payment status tracking</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-blue-600" /> AI assistant for platform guidance</li>
            </ul>
            <div className="mt-6">
              <Link href="/register?role=STUDENT">
                <Button className="rounded-full">Join as tenant</Button>
              </Link>
            </div>
          </div>

          <div id="for-landlords" className="rounded-2xl border border-slate-200 bg-white p-7">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50">
              <Building2 className="h-6 w-6 text-emerald-700" />
            </div>
            <h3 className="mb-2 text-2xl font-bold text-slate-900">For Landlords</h3>
            <p className="mb-5 text-slate-600">List properties, review applications, and monitor lease and payment outcomes.</p>
            <ul className="space-y-3 text-sm text-slate-700">
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-emerald-600" /> Listing approval workflow with image review</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-emerald-600" /> Application management and lease lifecycle</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-emerald-600" /> Payment visibility per lease</li>
              <li className="flex items-center gap-2"><CheckCircle2 className="h-4 w-4 text-emerald-600" /> Basic performance analytics</li>
            </ul>
            <div className="mt-6">
              <Link href="/register?role=LANDLORD">
                <Button variant="outline" className="rounded-full">Start listing</Button>
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-slate-900 px-4 py-14">
        <div className="mx-auto max-w-4xl text-center">
          <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-slate-800 px-3 py-1 text-sm text-slate-200">
            <MessageSquare className="h-4 w-4" />
            Platform Brain
          </div>
          <h2 className="mb-3 text-3xl font-bold text-white">Need help at any step?</h2>
          <p className="mx-auto mb-8 max-w-2xl text-slate-300">
            Use the in-app AI assistant to get context-aware guidance about listings, applications,
            verification, lease, and payment steps.
          </p>
          <Link href="/register">
            <Button size="lg" className="rounded-full bg-white px-8 text-slate-900 hover:bg-slate-100">
              Get started now
              <ArrowRight className="ml-2 h-5 w-5" />
            </Button>
          </Link>
        </div>
      </section>

      <section className="px-4 py-14">
        <div className="mx-auto max-w-4xl text-center">
          <h2 className="mb-3 text-2xl font-bold text-slate-900">Built on trust and transparency</h2>
          <p className="mb-8 text-slate-600">Admin review, document verification, and clear status updates across key flows.</p>
          <div className="grid gap-4 md:grid-cols-3">
            <div className="rounded-xl border border-slate-200 bg-white p-5">
              <Shield className="mx-auto mb-3 h-6 w-6 text-blue-600" />
              <p className="font-medium text-slate-900">Verification-aware</p>
              <p className="text-sm text-slate-600">Tenant and landlord document checks before trust decisions.</p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-white p-5">
              <FileCheck2 className="mx-auto mb-3 h-6 w-6 text-emerald-600" />
              <p className="font-medium text-slate-900">Approval workflow</p>
              <p className="text-sm text-slate-600">Listings and payments move through explicit review steps.</p>
            </div>
            <div className="rounded-xl border border-slate-200 bg-white p-5">
              <Bell className="mx-auto mb-3 h-6 w-6 text-amber-600" />
              <p className="font-medium text-slate-900">Status visibility</p>
              <p className="text-sm text-slate-600">Users can track outcomes rather than guessing what happened.</p>
            </div>
          </div>
          <p className="mt-6 text-sm text-slate-600">
            Need help with a decision? In-app support and dispute pathways are available before payment commitment.
          </p>
        </div>
      </section>

      <section className="bg-blue-600 px-4 py-14">
        <div className="mx-auto max-w-3xl text-center">
          <h2 className="mb-4 text-3xl font-bold text-white">Ready to launch your next rental decision?</h2>
          <p className="mb-8 text-blue-100">
            Create an account in minutes and start with the flow that matches your role.
          </p>
          <div className="flex flex-col justify-center gap-3 sm:flex-row">
            <Link href="/register?role=STUDENT">
              <Button size="lg" variant="secondary" className="rounded-full px-7">Join as tenant</Button>
            </Link>
            <Link href="/register?role=LANDLORD">
              <Button size="lg" variant="secondary" className="rounded-full px-7">Join as landlord</Button>
            </Link>
          </div>
        </div>
      </section>

      <footer className="bg-slate-950 px-4 py-8 text-slate-400">
        <div className="mx-auto max-w-5xl">
          <div className="mb-4 flex items-center justify-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600">
              <Home className="h-4 w-4 text-white" />
            </div>
            <span className="text-lg font-bold text-white">RoomBay</span>
          </div>
          <div className="mb-5 flex flex-wrap justify-center gap-5 text-sm">
            <Link href="/login" className="transition hover:text-white">Login</Link>
            <Link href="/register" className="transition hover:text-white">Register</Link>
            <Link href="/listings" className="transition hover:text-white">Browse listings</Link>
          </div>
          <p className="text-center text-sm">© 2026 RoomBay. All rights reserved.</p>
        </div>
      </footer>
    </div>
  )
}
