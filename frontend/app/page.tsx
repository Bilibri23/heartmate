"use client"

import { useEffect, useState } from "react"
import Image from "next/image"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import {
  Building2,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Heart,
  Home,
  MapPin,
  MessageSquare,
  Play,
  Quote,
  Search,
  ShieldCheck,
  Sparkles,
  Star,
  UserCheck,
  Video,
} from "lucide-react"
import { Button } from "@/components/ui/button"

const heroImages = [
  "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80",
]

const painPoints = [
  "No more fake apartment listings.",
  "Stop paying expensive frais de visite.",
  "Browse verified rentals before visiting.",
]

const cities = [
  { name: "Douala", tagline: "Cameroon's economic capital", status: "live", image: "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80" },
  { name: "Yaounde", tagline: "The city of seven hills", status: "live", image: "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80" },
  { name: "Soa", tagline: "Growing university hub", status: "live", image: "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=800&q=80" },
  { name: "Buea", tagline: "The town of legendary hospitality", status: "live", image: "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=800&q=80" },
]

const trustFeatures = [
  { icon: ShieldCheck, title: "Admin Review", description: "Every listing is reviewed by our team before it goes live." },
  { icon: UserCheck, title: "Landlord Verification", description: "We verify landlords with ID and property documents." },
  { icon: CheckCircle2, title: "Scam Prevention", description: "Built-in signals to catch suspicious listings early." },
  { icon: Star, title: "Trust Score", description: "Clear reputation badges so you know who you're dealing with." },
]

const steps = [
  { num: "01", title: "Describe what you want", description: "Type naturally like you're texting a friend. Our AI understands budget, location, style, and amenities." },
  { num: "02", title: "Swipe through verified listings", description: "Browse immersive video tours of real, admin-checked rentals. No more fake photos." },
  { num: "03", title: "Schedule a visit", description: "Message the landlord directly and book a viewing when you're already sure it's the right fit." },
]

const reelCards = [
  { title: "Modern Studio", location: "Bastos, Yaoundé", price: "150,000 XAF/mo", tag: "Verified", image: "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=600&q=80" },
  { title: "Furnished 2-bed", location: "Akwa, Douala", price: "280,000 XAF/mo", tag: "Video Tour", image: "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=600&q=80" },
  { title: "Room in Shared Flat", location: "Soa", price: "75,000 XAF/mo", tag: "New", image: "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80" },
]

const testimonials = [
  {
    name: "Aisha M.",
    role: "Product Designer, Bastos",
    text: "RoomBay AI understood my wish list instantly. Within seconds it pulled three verified matches, complete with fit scores and video tours. I signed my lease in 48 hours.",
    rating: 5,
    city: "Yaoundé",
  },
  {
    name: "Michael & Nadège",
    role: "Roommates matched in Bonamoussadi",
    text: "We both filled out the RoomBay roommate profile and the platform paired us with a verified shared flat. Zero awkward visits—just a perfect match for our budget and lifestyle.",
    rating: 5,
    city: "Douala",
  },
  {
    name: "Fabrice N.",
    role: "Software Engineer, Makepe",
    text: "I listed my preferences and RoomBay matched me to a 2-bed apartment that ticked every box—prepaid meter, coworking nook, reliable landlord. No agents, no surprises.",
    rating: 5,
    city: "Douala",
  },
  {
    name: "Clarisse E.",
    role: "Interior stylist, Buea",
    text: "The AR preview changed everything. I dropped my furniture into an unfurnished listing and could instantly see the layout. It gave me confidence to secure the place remotely.",
    rating: 5,
    city: "Buea",
  },
]

const faqs = [
  { question: "How does RoomBay verify listings?", answer: "Every listing is manually reviewed by our admin team before going live. We verify the landlord's identity, cross-check property documents, and flag suspicious behavior. Listings with video tours get priority placement." },
  { question: "Is RoomBay free for renters?", answer: "Yes, browsing and contacting landlords is completely free for renters. We never charge frais de visite or any hidden fees. Our revenue comes from landlord subscriptions and featured listings." },
  { question: "What cities does RoomBay cover?", answer: "We currently focus on Cameroon's major rental markets: Douala, Yaoundé, Soa, Bastos, Logbessou, and Bonamoussadi. We're actively expanding to Buea and other cities across the country." },
  { question: "Can I list my property if I'm not a professional landlord?", answer: "Absolutely. Whether you have one room or a full apartment building, you can list on RoomBay. We guide you through the verification process to ensure your listing gets maximum visibility." },
  { question: "How does the AI search work?", answer: "Type naturally — like 'furnished studio near University of Douala under 150k with parking.' Our AI extracts location, budget, and amenities, then surfaces the best matches with a fit score. No more endless checkbox filtering." },
  { question: "What if I encounter a suspicious listing?", answer: "Report it instantly through the flag button on any listing. Our team investigates within 24 hours. We also proactively scan for red flags using automated checks and community reports." },
]

const aiDemoConversation = [
  {
    role: "user",
    message: "Find me a furnished studio in Bastos under 150k. I want a female roommate and prepaid utilities.",
  },
  {
    role: "assistant",
    message: "Got it. I extracted your preferences and filtered for verified listings only.",
    tags: ["Bastos", "≤ 150k", "Furnished", "Female roommate", "Prepaid meter"],
  },
  {
    role: "assistant",
    message: "Top match: Bastos Skyline Studio — 145,000 XAF/mo, roommate Nadia (Product Analyst) already verified. Includes 360° video tour and prepaid utilities.",
  },
  {
    role: "assistant",
    message: "Need backups or want to preview the space in AR before visiting?",
  },
]

export default function LandingPage() {
  const { user, isLoading } = useAuth()
  const router = useRouter()
  const [heroIndex, setHeroIndex] = useState(0)
  const [openFaq, setOpenFaq] = useState<number | null>(null)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    // Only redirect after mount to avoid SSR/hydration mismatch and SEO redirect issues
    if (mounted && !isLoading && user) {
      if (user.role === "ADMIN") router.replace("/admin")
      else if (user.role === "LANDLORD") router.replace("/landlord")
      else router.replace("/for-you")
    }
  }, [mounted, user, isLoading, router])

  useEffect(() => {
    const interval = setInterval(() => setHeroIndex(i => (i + 1) % heroImages.length), 4000)
    return () => clearInterval(interval)
  }, [])

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#050816]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-cyan-400 border-t-transparent" />
      </div>
    )
  }
  // Always render landing content; redirect happens client-side after mount

  return (
    <div className="min-h-screen overflow-x-hidden bg-[#050816] text-white">
      <div className="absolute inset-0 -z-20 bg-[radial-gradient(circle_at_top_left,rgba(56,189,248,0.18),transparent_24%),radial-gradient(circle_at_80%_20%,rgba(99,102,241,0.18),transparent_26%),radial-gradient(circle_at_bottom_right,rgba(168,85,247,0.18),transparent_24%),linear-gradient(180deg,#050816_0%,#070b1d_36%,#04060f_100%)]" />
      <div className="absolute inset-0 -z-10 bg-[linear-gradient(to_right,rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:72px_72px] [mask-image:radial-gradient(circle_at_center,black,transparent_88%)]" />

      <header className="sticky top-0 z-50 border-b border-white/10 bg-[#050816]/70 backdrop-blur-2xl" style={{ paddingTop: "max(0.5rem, env(safe-area-inset-top, 0px))" }}>
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-2 px-4 py-3 sm:px-6 lg:px-8">
          <Link href="/" className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-slate-950 shadow-[0_20px_40px_rgba(255,255,255,0.16)] sm:h-10 sm:w-10 sm:rounded-2xl">
              <Home className="h-4 w-4 sm:h-5 sm:w-5" />
            </div>
            <div>
              <p className="text-base font-semibold tracking-tight sm:text-lg">RoomBay</p>
              <p className="hidden text-xs text-white/50 sm:block">Verified rentals in Cameroon</p>
            </div>
          </Link>

          <nav className="hidden items-center gap-7 text-sm text-white/60 lg:flex">
            <a href="#hero" className="transition hover:text-white">AI Demo</a>
            <a href="#trust" className="transition hover:text-white">Trust</a>
            <a href="#how-it-works" className="transition hover:text-white">How it works</a>
            <a href="#cities" className="transition hover:text-white">Cities</a>
          </nav>

          <div className="flex shrink-0 items-center gap-2">
            <Link href="/login">
              <Button variant="ghost" size="sm" className="h-9 rounded-full px-3 text-xs text-white hover:bg-white/10 hover:text-white sm:h-10 sm:px-4 sm:text-sm">
                Login
              </Button>
            </Link>
            <Link href="/listings">
              <Button size="sm" className="h-9 rounded-full bg-white px-3 text-xs font-semibold text-slate-950 hover:bg-slate-200 sm:h-10 sm:px-5 sm:text-sm">
                Explore Rentals
              </Button>
            </Link>
          </div>
        </div>
      </header>

      <main>
        <section id="hero" className="relative px-4 pb-12 pt-8 sm:px-6 sm:pb-20 sm:pt-14 lg:px-8 lg:pb-28 lg:pt-20">
          <div className="mx-auto grid max-w-7xl items-center gap-10 lg:grid-cols-[1fr_0.95fr] lg:gap-14">
            <div className="animate-fade-up">
              <div className="mb-5 inline-flex max-w-full items-center gap-2 rounded-full border border-white/12 bg-white/6 px-3 py-1.5 text-xs font-medium text-cyan-300 shadow-[0_10px_30px_rgba(56,189,248,0.08)] backdrop-blur-xl sm:mb-6 sm:px-4 sm:py-2 sm:text-sm">
                <Sparkles className="h-3.5 w-3.5 shrink-0 sm:h-4 sm:w-4" />
                <span className="leading-snug">AI-powered rental discovery for Cameroon</span>
              </div>

              <h1 className="max-w-4xl text-[1.6rem] font-semibold leading-[1.15] tracking-[-0.02em] text-white sm:text-[2.25rem] md:text-[2.75rem] lg:text-[3.25rem]">
                Swipe Through Verified Rentals in Cameroon
              </h1>
              <p className="mt-4 max-w-2xl text-[0.95rem] leading-7 text-white/65 sm:mt-5 sm:text-base sm:leading-8 md:text-lg">
                Discover apartments, studios, and rooms in Douala & Yaoundé through AI-powered search and immersive video tours.
              </p>

              <div className="mt-6 sm:mt-8">
                <div className="relative rounded-3xl border border-white/10 bg-white/[0.05] p-4 shadow-[0_24px_80px_rgba(2,6,23,0.55)] backdrop-blur-2xl sm:p-6">
                  <div className="mb-3 flex items-center justify-between">
                    <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/10 px-3 py-1 text-xs font-medium text-white/80">
                      <Sparkles className="h-3.5 w-3.5 text-cyan-300" />
                      RoomBay AI in action
                    </div>
                    <span className="text-[11px] text-white/40">Real demo</span>
                  </div>
                  <div className="space-y-3">
                    {aiDemoConversation.map((entry, idx) => (
                      <div
                        key={idx}
                        className={`rounded-2xl border border-white/10 p-3 sm:p-4 ${entry.role === "user" ? "bg-white/10 text-white" : "bg-[#0a1023] text-white/80"}`}
                      >
                        <p className="text-sm leading-6 sm:text-base">{entry.message}</p>
                        {entry.tags && (
                          <div className="mt-3 flex flex-wrap gap-2">
                            {entry.tags.map((tag) => (
                              <span key={tag} className="inline-flex items-center gap-1 rounded-full border border-white/12 bg-white/12 px-2.5 py-1 text-[11px] font-medium text-white/80">
                                {tag}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <div className="mt-6 flex flex-col flex-wrap gap-3 sm:mt-8 sm:flex-row">
                <Link href="/listings">
                  <Button size="lg" className="h-12 w-full rounded-full bg-white px-6 text-sm font-semibold text-slate-950 hover:bg-slate-200 sm:h-14 sm:w-auto sm:text-base">
                    <Search className="mr-2 h-5 w-5" />
                    Browse Listings
                  </Button>
                </Link>
                <Link href="/register">
                  <Button size="lg" variant="outline" className="h-12 w-full rounded-full border-white/15 bg-white/6 px-6 text-sm text-white hover:bg-white/10 sm:h-14 sm:w-auto sm:text-base">
                    Create Account
                  </Button>
                </Link>
                <Link href="/register?role=LANDLORD">
                  <Button size="lg" variant="outline" className="h-12 w-full rounded-full border-white/15 bg-white/6 px-6 text-sm text-white hover:bg-white/10 sm:h-14 sm:w-auto sm:text-base">
                    <Building2 className="mr-2 h-5 w-5" />
                    List Your Property
                  </Button>
                </Link>
              </div>

              <div className="mt-6 flex flex-wrap gap-2 sm:mt-8">
                {painPoints.map((t) => (
                  <span key={t} className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-white/70 sm:px-4 sm:text-sm">{t}</span>
                ))}
              </div>
            </div>

            <div className="relative mx-auto w-full max-w-[540px]">
              <div className="absolute -left-6 top-10 hidden h-32 w-32 rounded-full bg-cyan-400/20 blur-3xl md:block" />
              <div className="absolute -right-6 bottom-8 hidden h-36 w-36 rounded-full bg-violet-500/20 blur-3xl md:block" />

              <div className="relative overflow-hidden rounded-[32px] border border-white/10 bg-white/[0.05] p-3 shadow-[0_40px_120px_rgba(2,6,23,0.55)] backdrop-blur-2xl sm:rounded-[40px] sm:p-4">
                <div className="relative aspect-[4/5] overflow-hidden rounded-2xl border border-white/10 bg-[#0a1023] sm:rounded-[28px]">
                  {heroImages.map((src, i) => (
                    <Image key={src} src={src} alt="RoomBay property" fill priority={i === 0} className={`absolute inset-0 h-full w-full object-cover transition-all duration-1000 ${heroIndex === i ? "scale-105 opacity-100" : "scale-100 opacity-0"}`} />
                  ))}
                  <div className="absolute inset-0 bg-gradient-to-t from-black via-black/30 to-black/10" />

                  <div className="absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-full border border-white/12 bg-white/12 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur-xl sm:left-4 sm:top-4 sm:px-3 sm:text-xs">
                    <Star className="h-3 w-3 text-amber-300" />
                    Verified landlord
                  </div>

                  <div className="absolute right-3 top-3 flex gap-2 sm:right-4 sm:top-4">
                    <button className="rounded-full border border-white/12 bg-black/20 p-2 text-white backdrop-blur-xl transition hover:bg-white/10">
                      <Heart className="h-4 w-4" />
                    </button>
                    <button className="rounded-full border border-white/12 bg-black/20 p-2 text-white backdrop-blur-xl transition hover:bg-white/10">
                      <Play className="h-4 w-4" />
                    </button>
                  </div>

                  <div className="absolute bottom-0 left-0 right-0 p-4 text-white sm:p-5">
                    <div className="mb-3 rounded-xl border border-white/10 bg-white/10 p-2.5 backdrop-blur-xl sm:mb-4 sm:p-3">
                      <p className="text-[10px] uppercase tracking-[0.18em] text-cyan-100/80 sm:text-xs">AI recommended match</p>
                      <p className="mt-1 text-xs text-white/90 sm:text-sm">Furnished • Female roommate • Prepaid meter</p>
                    </div>
                    <div className="mb-1.5 flex items-center gap-1.5 text-xs text-white/80 sm:text-sm">
                      <MapPin className="h-3.5 w-3.5" />
                      Bastos, Yaoundé
                    </div>
                    <h3 className="text-lg font-semibold sm:text-2xl">Bright studio with modern finish</h3>
                    <div className="mt-2 flex items-center justify-between gap-3 sm:mt-3">
                      <div>
                        <p className="text-xl font-semibold sm:text-3xl">150,000 XAF</p>
                        <p className="text-xs text-white/75 sm:text-sm">Available now</p>
                      </div>
                      <div className="rounded-xl border border-emerald-400/20 bg-emerald-400/10 px-3 py-2 text-right backdrop-blur">
                        <p className="text-[10px] text-emerald-200 sm:text-xs">Fit score</p>
                        <p className="text-base font-semibold text-white sm:text-lg">94%</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section id="trust" className="px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="mb-8 text-center sm:mb-12">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">Built for Trust</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl md:text-4xl">No more fake apartment listings.</h2>
              <p className="mx-auto mt-3 max-w-2xl text-sm leading-7 text-white/60 sm:text-base sm:leading-8">
                We review every listing, verify every landlord, and flag suspicious behavior before you ever see it.
              </p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 sm:gap-5">
              {trustFeatures.map((f) => {
                const Icon = f.icon
                return (
                  <div key={f.title} className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 shadow-[0_18px_50px_rgba(15,23,42,0.18)] backdrop-blur-xl sm:rounded-[28px] sm:p-7">
                    <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-white text-slate-950 sm:mb-5 sm:h-12 sm:w-12 sm:rounded-2xl">
                      <Icon className="h-4 w-4 sm:h-5 sm:w-5" />
                    </div>
                    <h3 className="text-lg font-semibold text-white sm:text-xl">{f.title}</h3>
                    <p className="mt-2 text-sm leading-6 text-white/55 sm:mt-3 sm:text-base sm:leading-7">{f.description}</p>
                  </div>
                )
              })}
            </div>
          </div>
        </section>

        <section id="how-it-works" className="border-y border-white/8 bg-white/[0.03] px-4 py-12 backdrop-blur-sm sm:px-6 sm:py-16 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="mb-8 sm:mb-12">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-violet-300">How it works</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl md:text-4xl">The modern way to rent in Cameroon.</h2>
            </div>
            <div className="grid gap-4 sm:grid-cols-3 sm:gap-5">
              {steps.map((step) => (
                <div key={step.num} className="rounded-2xl border border-white/10 bg-[#0a1020] p-5 shadow-[0_20px_60px_rgba(0,0,0,0.05)] sm:rounded-[28px] sm:p-7">
                  <div className="mb-4 flex items-center justify-between sm:mb-5">
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white text-slate-950 shadow-sm sm:h-12 sm:w-12 sm:rounded-2xl">
                      {step.num === "01" ? <Sparkles className="h-4 w-4 sm:h-5 sm:w-5" /> : step.num === "02" ? <Video className="h-4 w-4 sm:h-5 sm:w-5" /> : <MessageSquare className="h-4 w-4 sm:h-5 sm:w-5" />}
                    </div>
                    <span className="text-sm font-semibold text-white/30">{step.num}</span>
                  </div>
                  <h3 className="text-lg font-semibold text-white sm:text-xl">{step.title}</h3>
                  <p className="mt-2 text-sm leading-6 text-white/55 sm:mt-3 sm:text-base sm:leading-7">{step.description}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="reels" className="px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="mb-8 sm:mb-12">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">Video-first discovery</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl md:text-4xl">Browse like you scroll TikTok. Decide like you mean it.</h2>
              <p className="mt-3 max-w-2xl text-sm leading-7 text-white/60 sm:text-base sm:leading-8">
                Discover rentals through immersive video tours. Stop wasting time on static photos that hide the truth.
              </p>
            </div>
            <div className="grid gap-4 sm:grid-cols-3 sm:gap-5">
              {reelCards.map((card) => (
                <div key={card.title} className="group relative overflow-hidden rounded-2xl border border-white/10 bg-[#0a1023] shadow-[0_24px_80px_rgba(0,0,0,0.38)] sm:rounded-[28px]">
                  <div className="relative aspect-[4/5] overflow-hidden">
                    <Image src={card.image} alt={card.title} fill className="object-cover transition duration-700 group-hover:scale-105" />
                    <div className="absolute inset-0 bg-gradient-to-t from-black via-black/20 to-transparent" />
                    <div className="absolute left-3 top-3 rounded-full border border-white/12 bg-black/30 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur-xl">
                      {card.tag}
                    </div>
                    <div className="absolute inset-0 flex items-center justify-center opacity-0 transition group-hover:opacity-100">
                      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-white/20 backdrop-blur-xl">
                        <Play className="h-6 w-6 text-white" />
                      </div>
                    </div>
                    <div className="absolute bottom-0 left-0 right-0 p-4 text-white sm:p-5">
                      <p className="text-xs text-white/70">{card.location}</p>
                      <h3 className="text-lg font-semibold sm:text-xl">{card.title}</h3>
                      <p className="mt-1 text-sm font-medium text-cyan-300">{card.price}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="cities" className="px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="mb-8 sm:mb-12">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">Cities</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl md:text-4xl">Find rentals where you actually live.</h2>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 sm:gap-5">
              {cities.map((city) => (
                <Link key={city.name} href={`/${city.name.toLowerCase()}`} className="group relative overflow-hidden rounded-2xl border border-white/10 bg-white/[0.05] shadow-[0_18px_50px_rgba(15,23,42,0.18)] sm:rounded-[28px]">
                  <div className="relative aspect-[4/3] overflow-hidden">
                    <Image src={city.image} alt={city.name} fill className="object-cover transition duration-700 group-hover:scale-110" />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
                  </div>
                  <div className="p-4 sm:p-5">
                    <div className="flex items-center justify-between">
                      <h3 className="text-lg font-semibold text-white">{city.name}</h3>
                      {city.status === "future" ? (
                        <span className="rounded-full border border-white/10 bg-white/10 px-2.5 py-0.5 text-[10px] font-medium text-white/70">Coming soon</span>
                      ) : (
                        <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-2.5 py-0.5 text-[10px] font-medium text-emerald-300">Live</span>
                      )}
                    </div>
                    <p className="mt-1 text-sm text-white/55">{city.tagline}</p>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </section>

        <section className="px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <div className="mx-auto max-w-7xl rounded-2xl border border-white/10 bg-[linear-gradient(135deg,#0f172a_0%,#111c3d_28%,#1d4ed8_62%,#7c3aed_100%)] p-6 text-white shadow-[0_35px_100px_rgba(37,99,235,0.28)] sm:rounded-[36px] sm:p-10 md:p-14">
            <div className="grid gap-8 lg:grid-cols-2 lg:items-center lg:gap-12">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-200 sm:text-sm">The future of renting</p>
                <h2 className="mt-2 text-2xl font-semibold tracking-tight sm:text-3xl md:text-4xl">Smarter search. Smarter living.</h2>
                <p className="mt-3 max-w-xl text-sm leading-7 text-blue-100/85 sm:text-base sm:leading-8">
                  We're building more than a listing site. RoomBay is an AI-native rental platform designed for how people actually want to find homes.
                </p>
                <ul className="mt-5 space-y-3">
                  {[
                    { label: "AI Assistant", text: "Ask follow-up questions about listings in plain language." },
                    { label: "Smart Search", text: "Filters that understand context, not just checkboxes." },
                    { label: "AR Furniture", text: "Visualize your space before you move." },
                  ].map((item) => (
                    <li key={item.label} className="flex items-start gap-3">
                      <div className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-white/20">
                        <CheckCircle2 className="h-3.5 w-3.5 text-white" />
                      </div>
                      <div>
                        <span className="text-sm font-semibold sm:text-base">{item.label}</span>
                        <span className="ml-2 text-sm text-blue-100/80 sm:text-base">{item.text}</span>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="relative mx-auto w-full max-w-sm">
                <div className="relative overflow-hidden rounded-3xl border border-white/10 bg-white/10 p-4 backdrop-blur-2xl">
                  <div className="space-y-3">
                    <div className="rounded-xl border border-white/10 bg-white/5 p-3">
                      <p className="text-xs text-white/50">AI Assistant</p>
                      <p className="mt-1 text-sm text-white">&ldquo;Does this studio have a prepaid meter?&rdquo;</p>
                    </div>
                    <div className="rounded-xl border border-white/10 bg-white/10 p-3">
                      <p className="text-xs text-cyan-200">RoomBay</p>
                      <p className="mt-1 text-sm text-white">Yes — this listing includes prepaid electricity and water. The landlord also accepts monthly payments.</p>
                    </div>
                    <div className="rounded-xl border border-white/10 bg-white/5 p-3">
                      <p className="text-xs text-white/50">Smart Search</p>
                      <p className="mt-1 text-sm text-white">&ldquo;Show me quiet places near Soa University under 100k&rdquo;</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section id="testimonials" className="px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="mb-8 text-center sm:mb-12">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">Testimonials</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl md:text-4xl">Trusted by renters & landlords across Cameroon.</h2>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 sm:gap-5 lg:grid-cols-4">
              {testimonials.map((t) => (
                <div key={t.name} className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 shadow-[0_18px_50px_rgba(15,23,42,0.18)] backdrop-blur-xl sm:rounded-[28px] sm:p-7">
                  <Quote className="h-6 w-6 text-cyan-300/60" />
                  <p className="mt-3 text-sm leading-7 text-white/75 sm:text-base sm:leading-8">{t.text}</p>
                  <div className="mt-4 flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-cyan-400 to-violet-500 text-xs font-semibold text-white">
                      {t.name.split(" ").map(n => n[0]).join("")}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-white">{t.name}</p>
                      <p className="text-xs text-white/50">{t.role}</p>
                    </div>
                  </div>
                  <div className="mt-3 flex items-center gap-0.5">
                    {Array.from({ length: t.rating }).map((_, i) => (
                      <Star key={i} className="h-3.5 w-3.5 fill-amber-300 text-amber-300" />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="faq" className="border-y border-white/8 bg-white/[0.03] px-4 py-12 backdrop-blur-sm sm:px-6 sm:py-16 lg:px-8">
          <div className="mx-auto max-w-3xl">
            <div className="mb-8 text-center sm:mb-12">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">FAQ</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl md:text-4xl">Questions? We have answers.</h2>
            </div>
            <div className="space-y-3">
              {faqs.map((faq, i) => {
                const isOpen = openFaq === i
                return (
                  <div key={i} className="rounded-2xl border border-white/10 bg-white/[0.05] transition sm:rounded-[24px]">
                    <button
                      onClick={() => setOpenFaq(isOpen ? null : i)}
                      className="flex w-full items-center justify-between gap-4 p-4 text-left sm:p-5"
                    >
                      <span className="text-sm font-semibold text-white sm:text-base">{faq.question}</span>
                      <ChevronDown className={`h-5 w-5 shrink-0 text-white/50 transition-transform ${isOpen ? "rotate-180" : ""}`} />
                    </button>
                    {isOpen && (
                      <div className="px-4 pb-4 text-sm leading-7 text-white/60 sm:px-5 sm:pb-5 sm:text-base sm:leading-8">
                        {faq.answer}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        </section>

        <section id="beta" className="px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="grid gap-8 lg:grid-cols-2 lg:items-center lg:gap-12">
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">Founding users</p>
                <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl md:text-4xl">Help shape the future of housing in Cameroon.</h2>
                <p className="mt-3 max-w-xl text-sm leading-7 text-white/60 sm:text-base sm:leading-8">
                  RoomBay is live — but we&apos;re just getting started. Be among the first to test AR room visualization, AI concierge, and premium landlord tools before anyone else.
                </p>
                <div className="mt-6 flex flex-col gap-3 sm:mt-8 sm:flex-row">
                  <Link href="/register">
                    <Button size="lg" className="h-12 w-full rounded-full bg-white px-6 text-sm font-semibold text-slate-950 hover:bg-slate-200 sm:h-14 sm:w-auto sm:text-base">
                      Become a Beta Tester
                    </Button>
                  </Link>
                  <Link href="/listings">
                    <Button size="lg" variant="outline" className="h-12 w-full rounded-full border-white/15 bg-white/6 px-6 text-sm text-white hover:bg-white/10 sm:h-14 sm:w-auto sm:text-base">
                      Browse Listings
                    </Button>
                  </Link>
                </div>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
                  <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-400/20 to-violet-500/20 sm:mb-5 sm:h-12 sm:w-12 sm:rounded-2xl">
                    <Sparkles className="h-5 w-5 text-cyan-300" />
                  </div>
                  <h3 className="text-lg font-semibold text-white sm:text-xl">AI Housing Concierge Pro</h3>
                  <p className="mt-2 text-sm leading-6 text-white/55 sm:mt-3 sm:text-base sm:leading-7">Natural language search that actually understands context. Early testers get unlimited AI queries.</p>
                </div>
                <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
                  <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-violet-400/20 to-pink-500/20 sm:mb-5 sm:h-12 sm:w-12 sm:rounded-2xl">
                    <Play className="h-5 w-5 text-violet-300" />
                  </div>
                  <h3 className="text-lg font-semibold text-white sm:text-xl">AR Room Visualization</h3>
                  <p className="mt-2 text-sm leading-6 text-white/55 sm:mt-3 sm:text-base sm:leading-7">Place furniture in your future rental before you move. Be among the first 100 AR beta users.</p>
                </div>
                <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
                  <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400/20 to-cyan-500/20 sm:mb-5 sm:h-12 sm:w-12 sm:rounded-2xl">
                    <Building2 className="h-5 w-5 text-emerald-300" />
                  </div>
                  <h3 className="text-lg font-semibold text-white sm:text-xl">Premium Landlord Tools</h3>
                  <p className="mt-2 text-sm leading-6 text-white/55 sm:mt-3 sm:text-base sm:leading-7">Analytics dashboards, automatic tenant screening, and priority listing placement for early adopters.</p>
                </div>
                <div className="rounded-2xl border border-white/10 bg-white/[0.05] p-5 sm:rounded-[28px] sm:p-7">
                  <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-amber-400/20 to-orange-500/20 sm:mb-5 sm:h-12 sm:w-12 sm:rounded-2xl">
                    <MapPin className="h-5 w-5 text-amber-300" />
                  </div>
                  <h3 className="text-lg font-semibold text-white sm:text-xl">New City Launches</h3>
                  <p className="mt-2 text-sm leading-6 text-white/55 sm:mt-3 sm:text-base sm:leading-7">Buea, Limbe, and Bamenda are next. Beta testers get first access to listings in new markets.</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="px-4 pb-12 pt-6 sm:px-6 sm:pb-20 sm:pt-8 lg:px-8">
          <div className="mx-auto max-w-6xl rounded-2xl border border-white/10 bg-[linear-gradient(135deg,#0f172a_0%,#1d4ed8_62%,#7c3aed_100%)] px-5 py-8 text-center text-white shadow-[0_35px_100px_rgba(37,99,235,0.24)] sm:rounded-[36px] sm:px-10 sm:py-12 md:py-14">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-200 sm:text-sm">Start today</p>
            <h2 className="mx-auto mt-2 max-w-3xl text-2xl font-semibold tracking-tight sm:mt-3 sm:text-3xl md:text-4xl">Start exploring verified rentals today.</h2>
            <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-blue-100/85 sm:mt-4 sm:text-base sm:leading-7 md:text-lg">
              Stop paying frais de visite for fake listings. Browse verified rentals with video tours, then schedule a visit only when you're sure.
            </p>
            <div className="mt-6 flex flex-col justify-center gap-2.5 sm:mt-8 sm:flex-row sm:gap-3">
              <Link href="/listings">
                <Button size="lg" className="h-12 w-full rounded-full bg-white px-6 text-sm font-semibold text-slate-950 hover:bg-slate-200 sm:h-14 sm:w-auto sm:px-7 sm:text-base">
                  Explore Rentals
                </Button>
              </Link>
              <Link href="/register?role=LANDLORD">
                <Button size="lg" variant="outline" className="h-12 w-full rounded-full border-white/20 bg-white/10 px-6 text-sm text-white hover:bg-white/14 sm:h-14 sm:w-auto sm:px-7 sm:text-base">
                  List Your Property
                  <ChevronRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-white/8 bg-black/10 px-4 py-6 text-white/45 backdrop-blur-xl sm:px-6 sm:py-8 lg:px-8">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-2xl bg-white text-slate-950">
              <Home className="h-4 w-4" />
            </div>
            <div>
              <p className="font-semibold text-white">RoomBay</p>
              <p className="text-sm">Verified rentals in Cameroon</p>
            </div>
          </div>
          <div className="flex flex-wrap gap-5 text-sm">
            <Link href="/listings" className="transition hover:text-white">Browse listings</Link>
            <Link href="/login" className="transition hover:text-white">Login</Link>
            <Link href="/register" className="transition hover:text-white">Create account</Link>
            <Link href="/contact" className="transition hover:text-white">Contact</Link>
          </div>
          <p className="text-sm">© 2026 RoomBay. All rights reserved.</p>
        </div>
      </footer>
    </div>
  )
}
