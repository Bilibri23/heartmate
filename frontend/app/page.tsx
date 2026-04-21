"use client"

import { useEffect, useMemo, useState } from "react"
import Image from "next/image"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import {
  ArrowRight,
  Building2,
  CheckCircle2,
  ChevronRight,
  FileCheck2,
  Heart,
  Home,
  MapPin,
  MessageSquare,
  Moon,
  Play,
  Search,
  Shield,
  Sparkles,
  Star,
  Sun,
  Users,
  Bell,
} from "lucide-react"
import { Button } from "@/components/ui/button"

const heroImages = [
  "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80",
]

const propertyShots = [
  "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=900&q=80",
]

const lifestyleShots = [
  "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=900&q=80",
]

const stats = [
  { value: "2 sec", label: "to understand a listing" },
  { value: "Visual", label: "discovery-first browsing" },
  { value: "Trusted", label: "signals before you commit" },
]

const features = [
  {
    icon: Search,
    title: "Scroll into your next home",
    description:
      "Move through homes like content, not like a heavy real-estate search form.",
  },
  {
    icon: Shield,
    title: "Confidence at first glance",
    description:
      "Verified landlords, fit cues, and cleaner listing details help users decide faster.",
  },
  {
    icon: Users,
    title: "Roommate help only when useful",
    description:
      "Split-rent support stays contextual instead of becoming the identity of the product.",
  },
]

const steps = [
  {
    icon: Sparkles,
    title: "Discover",
    copy: "Open RoomBay and instantly browse homes that match your area, timing, and budget.",
  },
  {
    icon: MessageSquare,
    title: "Act",
    copy: "Save, message, or apply directly from discovery without losing momentum.",
  },
  {
    icon: FileCheck2,
    title: "Secure",
    copy: "Move into a more guided flow only when you’re already interested and ready.",
  },
]

const trustItems = [
  {
    icon: CheckCircle2,
    title: "High-clarity listings",
    description: "Price, area, availability, and property vibe are visible immediately.",
  },
  {
    icon: Shield,
    title: "Trust-aware browsing",
    description: "Verification and reputation cues support faster, safer decisions.",
  },
  {
    icon: Bell,
    title: "Clear next steps",
    description: "From first interest to application updates, the journey stays explicit.",
  },
]

export default function LandingPage() {
  const { user, isLoading } = useAuth()
  const router = useRouter()
  const [theme, setTheme] = useState<"dark" | "light">("dark")
  const [heroIndex, setHeroIndex] = useState(0)

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

  useEffect(() => {
    const savedTheme = typeof window !== "undefined" ? localStorage.getItem("roombay-landing-theme") : null
    if (savedTheme === "light" || savedTheme === "dark") {
      setTheme(savedTheme)
    }
  }, [])

  useEffect(() => {
    if (typeof window !== "undefined") {
      localStorage.setItem("roombay-landing-theme", theme)
    }
  }, [theme])

  useEffect(() => {
    const interval = setInterval(() => {
      setHeroIndex((current) => (current + 1) % heroImages.length)
    }, 3500)
    return () => clearInterval(interval)
  }, [])

  const styles = useMemo(() => {
    if (theme === "light") {
      return {
        root: "bg-[#f6f8fc] text-slate-950",
        bgGlow:
          "bg-[radial-gradient(circle_at_top_left,_rgba(56,189,248,0.18),_transparent_24%),radial-gradient(circle_at_80%_20%,_rgba(99,102,241,0.18),_transparent_26%),radial-gradient(circle_at_bottom_right,_rgba(168,85,247,0.15),_transparent_24%),linear-gradient(180deg,#eef4ff_0%,#f8f9fc_40%,#ffffff_100%)]",
        grid: "bg-[linear-gradient(to_right,rgba(15,23,42,0.04)_1px,transparent_1px),linear-gradient(to_bottom,rgba(15,23,42,0.04)_1px,transparent_1px)]",
        header: "border-slate-200/80 bg-white/75",
        logo: "bg-slate-950 text-white shadow-[0_20px_40px_rgba(15,23,42,0.12)]",
        nav: "text-slate-600",
        navHover: "hover:text-slate-950",
        ghostBtn: "text-slate-700 hover:bg-slate-100 hover:text-slate-950",
        primaryBtn: "bg-slate-950 text-white hover:bg-slate-800",
        toggleWrap: "border-slate-200 bg-white/90",
        badge: "border-blue-100 bg-white/90 text-blue-700 shadow-[0_10px_30px_rgba(56,189,248,0.08)]",
        heading: "text-slate-950",
        headlineAccent: "from-cyan-500 via-blue-600 to-violet-600",
        paragraph: "text-slate-600",
        card: "border-white/80 bg-white/85 shadow-[0_18px_50px_rgba(15,23,42,0.08)]",
        cardMuted: "text-slate-600",
        visualShell: "border-white/90 bg-white/80 shadow-[0_40px_120px_rgba(15,23,42,0.12)]",
        visualTop: "border-slate-200 bg-slate-50",
        feedLabel: "text-slate-500",
        feedTitle: "text-slate-950",
        feedChip: "border-cyan-200 bg-cyan-50 text-cyan-700",
        mediaCard: "border-white/80 bg-white shadow-[0_24px_80px_rgba(15,23,42,0.14)]",
        glass: "border-white/80 bg-white/78",
        darkSection: "border-slate-200/70 bg-white",
        sectionCard: "border-slate-200 bg-white shadow-[0_18px_50px_rgba(15,23,42,0.06)]",
        sectionCard2: "border-slate-200 bg-[#f7f8fc]",
        trustHero: "border-slate-200 bg-[linear-gradient(180deg,rgba(255,255,255,0.98),rgba(241,245,249,0.96))] shadow-[0_30px_80px_rgba(15,23,42,0.08)]",
        trustText: "text-slate-600",
        trustPill: "border-cyan-200 bg-cyan-50 text-cyan-700",
        cta: "border-white/80 bg-[linear-gradient(135deg,#0f172a_0%,#1d4ed8_62%,#7c3aed_100%)] text-white shadow-[0_35px_100px_rgba(37,99,235,0.24)]",
        footer: "border-slate-200 bg-white text-slate-500",
        footerLogo: "bg-slate-950 text-white",
      }
    }

    return {
      root: "bg-[#050816] text-white",
      bgGlow:
        "bg-[radial-gradient(circle_at_top_left,_rgba(56,189,248,0.18),_transparent_24%),radial-gradient(circle_at_80%_20%,_rgba(99,102,241,0.18),_transparent_26%),radial-gradient(circle_at_bottom_right,_rgba(168,85,247,0.18),_transparent_24%),linear-gradient(180deg,#050816_0%,#070b1d_36%,#04060f_100%)]",
      grid: "bg-[linear-gradient(to_right,rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.03)_1px,transparent_1px)]",
      header: "border-white/10 bg-[#050816]/70",
      logo: "bg-white text-slate-950 shadow-[0_20px_40px_rgba(255,255,255,0.16)]",
      nav: "text-white/60",
      navHover: "hover:text-white",
      ghostBtn: "text-white hover:bg-white/10 hover:text-white",
      primaryBtn: "bg-white text-slate-950 hover:bg-slate-200",
      toggleWrap: "border-white/10 bg-white/5",
      badge: "border-white/12 bg-white/6 text-cyan-300 shadow-[0_10px_30px_rgba(56,189,248,0.08)]",
      heading: "text-white",
      headlineAccent: "from-cyan-300 via-blue-400 to-violet-400",
      paragraph: "text-white/65",
      card: "border-white/10 bg-white/[0.05] shadow-[0_18px_50px_rgba(15,23,42,0.18)]",
      cardMuted: "text-white/55",
      visualShell: "border-white/10 bg-white/[0.06] shadow-[0_40px_120px_rgba(2,6,23,0.55)]",
      visualTop: "border-white/10 bg-white/[0.04]",
      feedLabel: "text-white/45",
      feedTitle: "text-white",
      feedChip: "border-cyan-400/20 bg-cyan-400/10 text-cyan-300",
      mediaCard: "border-white/10 bg-[#0a1023] shadow-[0_24px_80px_rgba(0,0,0,0.38)]",
      glass: "border-white/10 bg-white/[0.05]",
      darkSection: "border-white/8 bg-white/[0.03]",
      sectionCard: "border-white/10 bg-white/[0.05] shadow-[0_22px_60px_rgba(0,0,0,0.26)]",
      sectionCard2: "border-white/10 bg-[#0a1020]",
      trustHero: "border-white/10 bg-[linear-gradient(180deg,rgba(255,255,255,0.08),rgba(255,255,255,0.03))] shadow-[0_30px_80px_rgba(0,0,0,0.28)]",
      trustText: "text-white/58",
      trustPill: "border-cyan-400/20 bg-cyan-400/10 text-cyan-300",
      cta: "border-white/10 bg-[linear-gradient(135deg,#0f172a_0%,#111c3d_28%,#1d4ed8_62%,#7c3aed_100%)] text-white shadow-[0_35px_100px_rgba(37,99,235,0.28)]",
      footer: "border-white/8 bg-black/10 text-white/45",
      footerLogo: "bg-white text-slate-950",
    }
  }, [theme])

  if (isLoading) {
    return (
      <div className={`flex min-h-screen items-center justify-center ${theme === "light" ? "bg-[#f6f8fc]" : "bg-[#060816]"}`}>
        <div className={`h-8 w-8 animate-spin rounded-full border-4 ${theme === "light" ? "border-blue-600" : "border-cyan-400"} border-t-transparent`} />
      </div>
    )
  }

  if (user) {
    return null
  }

  return (
    <div className={`min-h-screen overflow-x-hidden ${styles.root}`}>
      <div className={`absolute inset-0 -z-20 ${styles.bgGlow}`} />
      <div className={`absolute inset-0 -z-10 ${styles.grid} bg-[size:72px_72px] [mask-image:radial-gradient(circle_at_center,black,transparent_88%)]`} />

      <header
        className={`sticky top-0 z-50 border-b backdrop-blur-2xl ${styles.header}`}
        style={{ paddingTop: "max(0.5rem, env(safe-area-inset-top, 0px))" }}
      >
        <div className="mx-auto flex max-w-7xl flex-col gap-2 px-3 py-2.5 sm:px-6 sm:py-3 lg:px-8">
          <div className="flex items-center justify-between gap-2 lg:gap-6">
            <Link href="/" className="flex min-w-0 flex-1 items-center gap-2.5 sm:max-w-none sm:flex-initial sm:gap-3">
              <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl sm:h-10 sm:w-10 sm:rounded-2xl ${styles.logo}`}>
                <Home className="h-4 w-4 sm:h-5 sm:w-5" />
              </div>
              <div className="min-w-0">
                <p className={`truncate text-base font-semibold tracking-tight sm:text-lg ${styles.heading}`}>RoomBay</p>
                <p className={`hidden text-xs sm:block ${theme === "light" ? "text-slate-500" : "text-white/50"}`}>
                  Fast housing access
                </p>
              </div>
            </Link>

            <nav className={`hidden items-center gap-7 text-sm lg:flex ${styles.nav}`}>
              <a href="#features" className={`transition ${styles.navHover}`}>
                Features
              </a>
              <a href="#how-it-works" className={`transition ${styles.navHover}`}>
                How it works
              </a>
              <a href="#trust" className={`transition ${styles.navHover}`}>
                Trust
              </a>
            </nav>

            <div className="flex shrink-0 items-center gap-1.5 sm:gap-2">
              <div className={`flex shrink-0 items-center rounded-full border p-0.5 sm:p-1 ${styles.toggleWrap}`}>
                <button
                  type="button"
                  aria-label="Use light mode"
                  onClick={() => setTheme("light")}
                  className={`rounded-full p-1.5 transition sm:p-2 ${theme === "light" ? "bg-slate-950 text-white" : theme === "dark" ? "text-white/70 hover:text-white" : "text-slate-500 hover:text-slate-950"}`}
                >
                  <Sun className="h-3.5 w-3.5 sm:h-4 sm:w-4" />
                </button>
                <button
                  type="button"
                  aria-label="Use dark mode"
                  onClick={() => setTheme("dark")}
                  className={`rounded-full p-1.5 transition sm:p-2 ${theme === "dark" ? "bg-white text-slate-950" : theme === "light" ? "text-slate-500 hover:text-slate-950" : "text-white/70 hover:text-white"}`}
                >
                  <Moon className="h-3.5 w-3.5 sm:h-4 sm:w-4" />
                </button>
              </div>

              <Link href="/login" className="shrink-0">
                <Button variant="ghost" size="sm" className={`h-9 rounded-full px-3 text-xs sm:h-10 sm:px-4 sm:text-sm ${styles.ghostBtn}`}>
                  Login
                </Button>
              </Link>
              <Link href="/listings" className="shrink-0">
                <Button size="sm" className={`h-9 rounded-full px-3 text-xs font-semibold sm:h-10 sm:px-5 sm:text-sm ${styles.primaryBtn}`}>
                  <span className="sm:hidden">Browse</span>
                  <span className="hidden sm:inline">Browse now</span>
                </Button>
              </Link>
            </div>
          </div>

          <nav
            className={`flex justify-center gap-1 border-t pt-2 sm:gap-2 sm:pt-2.5 lg:hidden ${theme === "light" ? "border-slate-200/80" : "border-white/10"} ${styles.nav}`}
            aria-label="Page sections"
          >
            <a
              href="#features"
              className={`rounded-full px-3 py-1 text-[11px] font-medium transition sm:text-xs ${styles.navHover}`}
            >
              Features
            </a>
            <a
              href="#how-it-works"
              className={`rounded-full px-3 py-1 text-[11px] font-medium transition sm:text-xs ${styles.navHover}`}
            >
              How it works
            </a>
            <a href="#trust" className={`rounded-full px-3 py-1 text-[11px] font-medium transition sm:text-xs ${styles.navHover}`}>
              Trust
            </a>
          </nav>
        </div>
      </header>

      <main>
        <section className="px-4 pb-10 pt-6 sm:px-6 sm:pb-16 sm:pt-12 lg:px-8 lg:pb-24 lg:pt-16">
          <div className="mx-auto grid max-w-7xl items-center gap-8 sm:gap-10 lg:grid-cols-[1fr_0.95fr] lg:gap-14">
            <div>
              <div
                className={`mb-4 inline-flex max-w-full items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-medium backdrop-blur-xl text-balance sm:mb-6 sm:px-4 sm:py-2 sm:text-sm ${styles.badge}`}
              >
                <Sparkles className="h-3.5 w-3.5 shrink-0 sm:h-4 sm:w-4" />
                <span className="leading-snug">Discovery-first housing with optional roommate intelligence</span>
              </div>

              <h1
                className={`max-w-4xl text-[2.1rem] font-semibold leading-[1.02] tracking-[-0.04em] min-[380px]:text-[2.35rem] sm:text-5xl sm:leading-[0.95] sm:tracking-[-0.05em] md:text-6xl lg:text-7xl xl:text-[5.5rem] ${styles.heading}`}
              >
                Find your next home
                <span className={`block bg-gradient-to-r bg-clip-text text-transparent ${styles.headlineAccent}`}>
                  like you scroll content.
                </span>
              </h1>

              <p className={`mt-4 max-w-2xl text-base leading-7 sm:mt-6 sm:text-lg sm:leading-8 md:text-xl ${styles.paragraph}`}>
                RoomBay turns housing into a fast visual experience — discover listings, understand them instantly,
                and take action without fighting through friction.
              </p>

              <div className="mt-6 flex flex-col gap-3 sm:mt-8 sm:flex-row">
                <Link href="/listings">
                  <Button
                    size="lg"
                    className={`h-12 w-full rounded-full px-6 text-sm sm:h-14 sm:w-auto sm:px-7 sm:text-base ${styles.primaryBtn}`}
                    onClick={() => {
                      if (typeof window !== "undefined") {
                        window.dispatchEvent(new CustomEvent("analytics:anonymousBrowseStart"))
                      }
                    }}
                  >
                    Start browsing
                    <ArrowRight className="ml-2 h-5 w-5" />
                  </Button>
                </Link>

                <Link href="/register?role=LANDLORD">
                  <Button
                    size="lg"
                    variant="outline"
                    className={`h-12 w-full rounded-full px-6 text-sm backdrop-blur-xl sm:h-14 sm:w-auto sm:px-7 sm:text-base ${theme === "light" ? "border-slate-300 bg-white text-slate-950 hover:bg-slate-100" : "border-white/15 bg-white/6 text-white hover:bg-white/10"}`}
                  >
                    <Building2 className="mr-2 h-5 w-5" />
                    List a property
                  </Button>
                </Link>
              </div>

              <div className="mt-6 grid grid-cols-3 gap-2 sm:mt-10 sm:gap-4">
                {stats.map((item) => (
                  <div key={item.label} className={`rounded-2xl border p-3 backdrop-blur-xl sm:rounded-[28px] sm:p-5 ${styles.card}`}>
                    <p className={`text-lg font-semibold tracking-tight sm:text-2xl ${styles.heading}`}>{item.value}</p>
                    <p className={`mt-0.5 text-[11px] leading-tight sm:mt-1 sm:text-sm sm:leading-6 ${styles.cardMuted}`}>{item.label}</p>
                  </div>
                ))}
              </div>
            </div>

            <div className="relative mx-auto w-full max-w-[580px]">
              <div className={`absolute -left-8 top-14 hidden h-32 w-32 rounded-full blur-3xl md:block ${theme === "light" ? "bg-cyan-400/20" : "bg-cyan-400/20"}`} />
              <div className={`absolute -right-8 bottom-8 hidden h-36 w-36 rounded-full blur-3xl md:block ${theme === "light" ? "bg-violet-500/20" : "bg-violet-500/20"}`} />

              <div className={`relative rounded-[34px] border p-4 backdrop-blur-2xl ${styles.visualShell}`}>
                <div className={`mb-4 flex items-center justify-between rounded-2xl border px-4 py-3 ${styles.visualTop}`}>
                  <div>
                    <p className={`text-sm font-medium ${styles.feedLabel}`}>For You</p>
                    <p className={`text-base font-semibold ${styles.feedTitle}`}>Homes that match your budget and vibe</p>
                  </div>
                  <div className={`rounded-full border px-3 py-1 text-xs font-semibold ${styles.feedChip}`}>
                    Scroll feed
                  </div>
                </div>

                <div className="grid gap-3 lg:grid-cols-[1fr_0.34fr] lg:gap-4">
                  <div className={`overflow-hidden rounded-2xl border sm:rounded-[28px] ${styles.mediaCard}`}>
                    <div className="relative aspect-[3/4] overflow-hidden sm:aspect-[4/5]">
                      {heroImages.map((src, index) => (
                        <Image
                          key={src}
                          src={src}
                          alt="RoomBay property preview"
                          fill
                          priority={index === 0}
                          className={`absolute inset-0 h-full w-full object-cover transition-all duration-700 ${heroIndex === index ? "scale-105 opacity-100" : "scale-100 opacity-0"}`}
                        />
                      ))}
                      <div className="absolute inset-0 bg-gradient-to-t from-black via-black/30 to-black/10" />

                      <div className="absolute right-4 top-4 flex gap-2">
                        <button className={`rounded-full border p-2 backdrop-blur-xl ${theme === "light" ? "border-white/70 bg-white/80 text-slate-950 hover:bg-white" : "border-white/12 bg-black/20 text-white hover:bg-white/10"}`}>
                          <Heart className="h-4 w-4" />
                        </button>
                        <button className={`rounded-full border p-2 backdrop-blur-xl ${theme === "light" ? "border-white/70 bg-white/80 text-slate-950 hover:bg-white" : "border-white/12 bg-black/20 text-white hover:bg-white/10"}`}>
                          <Play className="h-4 w-4" />
                        </button>
                      </div>

                      <div className="absolute left-4 top-4 inline-flex items-center gap-2 rounded-full border border-white/12 bg-white/12 px-3 py-1 text-xs font-medium text-white backdrop-blur-xl">
                        <Star className="h-3.5 w-3.5 text-amber-300" />
                        Verified landlord
                      </div>

                      <div className="absolute bottom-0 left-0 right-0 p-5 text-white">
                        <div className="mb-4 rounded-2xl border border-white/10 bg-white/10 p-3 backdrop-blur-xl">
                          <p className="text-xs uppercase tracking-[0.18em] text-cyan-100/80">Why it fits</p>
                          <p className="mt-1 text-sm text-white/90">Within your budget • Near campus • Move-in ready</p>
                        </div>

                        <div className="mb-2 flex items-center gap-2 text-sm text-white/80">
                          <MapPin className="h-4 w-4" />
                          Bastos, Yaoundé
                        </div>
                        <h3 className="text-2xl font-semibold">Bright studio with modern finish</h3>
                        <div className="mt-3 flex items-center justify-between gap-3">
                          <div>
                            <p className="text-3xl font-semibold">250,000 XAF</p>
                            <p className="text-sm text-white/75">Available now</p>
                          </div>
                          <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-3 py-2 text-right backdrop-blur">
                            <p className="text-xs text-emerald-200">Fit score</p>
                            <p className="text-lg font-semibold text-white">89%</p>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className={`grid grid-cols-3 gap-2 border-t p-3 ${theme === "light" ? "border-slate-200 bg-white" : "border-white/10 bg-[#09101f]"}`}>
                      <button className={`rounded-2xl px-3 py-3 text-sm font-medium transition ${theme === "light" ? "bg-slate-100 text-slate-950 hover:bg-slate-200" : "bg-white/5 text-white hover:bg-white/10"}`}>Save</button>
                      <button className={`rounded-2xl px-3 py-3 text-sm font-medium transition ${theme === "light" ? "bg-slate-100 text-slate-950 hover:bg-slate-200" : "bg-white/5 text-white hover:bg-white/10"}`}>Message</button>
                      <button className={`rounded-2xl px-3 py-3 text-sm font-semibold transition ${theme === "light" ? "bg-slate-950 text-white hover:bg-slate-800" : "bg-white text-slate-950 hover:bg-slate-200"}`}>Apply</button>
                    </div>
                  </div>

                  <div className="hidden gap-4 lg:grid">
                    {propertyShots.map((src, index) => (
                      <div key={src} className={`relative overflow-hidden rounded-[24px] border ${styles.glass}`}>
                        <div className="relative aspect-[3/4]">
                          <Image src={src} alt={`RoomBay property ${index + 1}`} fill className="object-cover transition duration-500 hover:scale-105" />
                          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
                          <div className="absolute bottom-3 left-3 right-3 flex items-end justify-between text-white">
                            <div>
                              <p className="text-xs text-white/75">Quick preview</p>
                              <p className="text-sm font-semibold">Move-in ready</p>
                            </div>
                            <div className="rounded-full bg-white/15 px-2.5 py-1 text-xs backdrop-blur">{index === 0 ? "Video" : "Photo"}</div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="px-4 pb-6 sm:px-6 sm:pb-8 lg:px-8">
          <div className="mx-auto grid max-w-7xl gap-5 lg:grid-cols-[0.9fr_1.1fr] lg:gap-6">
            <div className={`rounded-2xl border p-5 sm:rounded-[30px] sm:p-7 ${styles.card}`}>
              <p className={`text-xs font-semibold uppercase tracking-[0.2em] sm:text-sm ${theme === "light" ? "text-blue-700" : "text-cyan-300"}`}>Lifestyle</p>
              <h2 className={`mt-2 text-2xl font-semibold tracking-tight sm:mt-3 sm:text-3xl ${styles.heading}`}>
                Not just listings. A better feeling around finding where you’ll live.
              </h2>
              <p className={`mt-3 text-sm leading-6 sm:mt-4 sm:text-base sm:leading-7 ${styles.cardMuted}`}>
                RoomBay should make people feel like housing is finally easier: faster to browse, easier to understand, and less stressful to act on.
              </p>
              <div className="mt-4 flex flex-wrap gap-2 sm:mt-6 sm:gap-3">
                {[
                  "Fast visual discovery",
                  "Clearer trust signals",
                  "Optional shared accommodation help",
                ].map((item) => (
                  <span
                    key={item}
                    className={`rounded-full border px-3 py-1.5 text-xs sm:px-4 sm:py-2 sm:text-sm ${theme === "light" ? "border-slate-200 bg-white text-slate-700" : "border-white/10 bg-white/5 text-white/75"}`}
                  >
                    {item}
                  </span>
                ))}
              </div>
            </div>

            <div className="-mx-4 flex snap-x snap-mandatory gap-3 overflow-x-auto px-4 pb-1 sm:mx-0 sm:grid sm:grid-cols-3 sm:gap-5 sm:overflow-visible sm:px-0 sm:pb-0">
              {lifestyleShots.map((src, index) => (
                <div
                  key={src}
                  className={`relative w-[min(78vw,280px)] shrink-0 snap-center overflow-hidden rounded-2xl border sm:w-auto sm:rounded-[30px] ${styles.card}`}
                >
                  <div className="relative aspect-[5/4] sm:aspect-[4/5]">
                    <Image src={src} alt={`RoomBay lifestyle ${index + 1}`} fill className="object-cover transition duration-500 hover:scale-105" />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/5 to-transparent" />
                    <div className="absolute bottom-3 left-3 right-3 text-white sm:bottom-4 sm:left-4 sm:right-4">
                      <p className="text-[10px] text-white/70 sm:text-xs">RoomBay experience</p>
                      <p className="mt-0.5 text-xs font-semibold sm:mt-1 sm:text-sm">
                        {index === 0 ? "Comfortable spaces" : index === 1 ? "Modern discovery" : "Better living flow"}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="features" className="px-4 py-10 sm:px-6 sm:py-14 md:py-16 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="mb-6 max-w-2xl sm:mb-10">
              <p className={`text-sm font-semibold uppercase tracking-[0.2em] ${theme === "light" ? "text-blue-700" : "text-cyan-300"}`}>Why RoomBay</p>
              <h2 className={`mt-2 text-2xl font-semibold tracking-tight sm:mt-3 sm:text-3xl md:text-4xl ${styles.heading}`}>
                A housing product that feels premium, fast, and easy to trust.
              </h2>
            </div>

            <div className="grid gap-3 sm:gap-5 lg:grid-cols-3">
              {features.map((feature) => {
                const Icon = feature.icon
                return (
                  <div key={feature.title} className={`rounded-2xl border p-5 backdrop-blur-xl sm:rounded-[28px] sm:p-7 ${styles.sectionCard}`}>
                    <div className={`mb-4 flex h-11 w-11 items-center justify-center rounded-xl sm:mb-5 sm:h-12 sm:w-12 sm:rounded-2xl ${theme === "light" ? "bg-slate-950 text-white" : "bg-white text-slate-950"}`}>
                      <Icon className="h-4 w-4 sm:h-5 sm:w-5" />
                    </div>
                    <h3 className={`text-lg font-semibold sm:text-xl ${styles.heading}`}>{feature.title}</h3>
                    <p className={`mt-2 text-sm leading-6 sm:mt-3 sm:text-base sm:leading-7 ${styles.cardMuted}`}>{feature.description}</p>
                  </div>
                )
              })}
            </div>
          </div>
        </section>

        <section id="how-it-works" className={`border-y px-4 py-10 backdrop-blur-sm sm:px-6 sm:py-14 md:py-16 lg:px-8 ${styles.darkSection}`}>
          <div className="mx-auto max-w-7xl">
            <div className="mb-6 flex flex-col gap-3 sm:mb-10 sm:gap-4 lg:flex-row lg:items-end lg:justify-between">
              <div className="max-w-2xl">
                <p className={`text-sm font-semibold uppercase tracking-[0.2em] ${theme === "light" ? "text-violet-700" : "text-violet-300"}`}>How it works</p>
                <h2 className={`mt-2 text-2xl font-semibold tracking-tight sm:mt-3 sm:text-3xl md:text-4xl ${styles.heading}`}>
                  Built to move users from first scroll to real action fast.
                </h2>
              </div>
              <p className={`max-w-xl text-sm leading-6 sm:text-base ${styles.cardMuted}`}>
                Discovery comes first. Heavier steps only appear when the user is already interested.
              </p>
            </div>

            <div className="grid gap-3 sm:gap-5 md:grid-cols-3">
              {steps.map((step, index) => {
                const Icon = step.icon
                return (
                  <div key={step.title} className={`rounded-2xl border p-5 shadow-[0_20px_60px_rgba(0,0,0,0.05)] sm:rounded-[28px] sm:p-7 ${styles.sectionCard2}`}>
                    <div className="mb-4 flex items-center justify-between sm:mb-5">
                      <div className={`flex h-11 w-11 items-center justify-center rounded-xl shadow-sm sm:h-12 sm:w-12 sm:rounded-2xl ${theme === "light" ? "bg-white text-slate-950" : "bg-white text-slate-950"}`}>
                        <Icon className="h-4 w-4 sm:h-5 sm:w-5" />
                      </div>
                      <span className={theme === "light" ? "text-sm font-semibold text-slate-300" : "text-sm font-semibold text-white/30"}>0{index + 1}</span>
                    </div>
                    <h3 className={`text-lg font-semibold sm:text-xl ${styles.heading}`}>{step.title}</h3>
                    <p className={`mt-2 text-sm leading-6 sm:mt-3 sm:text-base sm:leading-7 ${styles.cardMuted}`}>{step.copy}</p>
                  </div>
                )
              })}
            </div>
          </div>
        </section>

        <section id="trust" className="px-4 py-10 sm:px-6 sm:py-14 md:py-16 lg:px-8">
          <div className="mx-auto grid max-w-7xl gap-4 sm:gap-6 lg:grid-cols-[0.95fr_1.05fr]">
            <div className={`rounded-2xl border p-5 backdrop-blur-2xl sm:rounded-[32px] sm:p-8 md:p-10 ${styles.trustHero}`}>
              <div className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-sm ${styles.trustPill}`}>
                <Shield className="h-4 w-4" />
                Built on trust
              </div>
              <h2 className={`mt-3 text-2xl font-semibold tracking-tight sm:mt-5 sm:text-3xl md:text-4xl ${styles.heading}`}>
                Better signals create faster, more confident decisions.
              </h2>
              <p className={`mt-3 max-w-xl text-sm leading-6 sm:mt-4 sm:text-base sm:leading-7 ${styles.trustText}`}>
                RoomBay gives users cleaner clarity before they commit time: verification-aware browsing, better listing context, and clearer action paths.
              </p>
              <div className="mt-5 space-y-3 sm:mt-8 sm:space-y-4">
                <div className={`rounded-xl border p-3 sm:rounded-2xl sm:p-4 ${styles.glass}`}>
                  <p className={`text-sm font-semibold ${styles.heading}`}>Visual-first browsing</p>
                  <p className={`mt-1 text-sm ${styles.cardMuted}`}>Users understand the place before they dive deeper.</p>
                </div>
                <div className={`rounded-xl border p-3 sm:rounded-2xl sm:p-4 ${styles.glass}`}>
                  <p className={`text-sm font-semibold ${styles.heading}`}>Guided action flow</p>
                  <p className={`mt-1 text-sm ${styles.cardMuted}`}>Messaging, saving, and applying feel more immediate and intentional.</p>
                </div>
              </div>
            </div>

            <div className="grid gap-3 sm:gap-5 md:grid-cols-3 lg:grid-cols-1">
              {trustItems.map((item) => {
                const Icon = item.icon
                return (
                  <div key={item.title} className={`rounded-2xl border p-5 backdrop-blur-xl sm:rounded-[28px] sm:p-7 ${styles.sectionCard}`}>
                    <div className={`mb-3 flex h-11 w-11 items-center justify-center rounded-xl sm:mb-4 sm:h-12 sm:w-12 sm:rounded-2xl ${theme === "light" ? "bg-slate-950 text-white" : "bg-white text-slate-950"}`}>
                      <Icon className="h-4 w-4 sm:h-5 sm:w-5" />
                    </div>
                    <h3 className={`text-base font-semibold sm:text-lg ${styles.heading}`}>{item.title}</h3>
                    <p className={`mt-1.5 text-sm leading-6 sm:mt-2 sm:text-base sm:leading-7 ${styles.trustText}`}>{item.description}</p>
                  </div>
                )
              })}
            </div>
          </div>
        </section>

        <section className="px-4 pb-12 pt-2 sm:px-6 sm:pb-20 sm:pt-4 lg:px-8">
          <div className={`mx-auto max-w-6xl rounded-2xl border px-5 py-8 text-center sm:rounded-[36px] sm:px-10 sm:py-12 md:py-14 ${styles.cta}`}>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-200 sm:text-sm">Start now</p>
            <h2 className="mx-auto mt-2 max-w-3xl text-2xl font-semibold tracking-tight sm:mt-3 sm:text-3xl md:text-5xl">
              Stop digging through listings. Start discovering homes that feel right.
            </h2>
            <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-blue-100/85 sm:mt-4 sm:text-base sm:leading-7 md:text-lg">
              Fast housing access, quick actions, and optional roommate help — all in one modern flow.
            </p>
            <div className="mt-6 flex flex-col justify-center gap-2.5 sm:mt-8 sm:flex-row sm:gap-3">
              <Link href="/listings">
                <Button size="lg" className={`h-12 w-full rounded-full px-6 text-sm sm:h-14 sm:w-auto sm:px-7 sm:text-base ${theme === "light" ? "bg-white text-slate-950 hover:bg-slate-200" : "bg-white text-slate-950 hover:bg-slate-200"}`}>
                  Browse listings
                </Button>
              </Link>
              <Link href="/register?role=LANDLORD">
                <Button size="lg" variant="outline" className={`h-12 w-full rounded-full px-6 text-sm sm:h-14 sm:w-auto sm:px-7 sm:text-base ${theme === "light" ? "border-white/60 bg-white/10 text-white hover:bg-white/16" : "border-white/20 bg-white/10 text-white hover:bg-white/14"}`}>
                  List your property
                  <ChevronRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
            </div>
          </div>
        </section>
      </main>

      <footer className={`border-t px-4 py-6 backdrop-blur-xl sm:px-6 sm:py-8 lg:px-8 ${styles.footer}`}>
        <div className="mx-auto flex max-w-7xl flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className={`flex h-9 w-9 items-center justify-center rounded-2xl ${styles.footerLogo}`}>
              <Home className="h-4 w-4" />
            </div>
            <div>
              <p className={styles.heading + " font-semibold"}>RoomBay</p>
              <p className={theme === "light" ? "text-sm text-slate-500" : "text-sm text-white/45"}>Fast housing access</p>
            </div>
          </div>

          <div className="flex flex-wrap gap-5 text-sm">
            <Link href="/listings" className={theme === "light" ? "transition hover:text-slate-950" : "transition hover:text-white"}>Browse listings</Link>
            <Link href="/login" className={theme === "light" ? "transition hover:text-slate-950" : "transition hover:text-white"}>Login</Link>
            <Link href="/register" className={theme === "light" ? "transition hover:text-slate-950" : "transition hover:text-white"}>Create account</Link>
          </div>

          <p className="text-sm">© 2026 RoomBay. All rights reserved.</p>
        </div>
      </footer>
    </div>
  )
}
