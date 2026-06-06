"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { ListingCard } from "@/components/cards/listing-card"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import {
  Search, X, ChevronLeft, ChevronRight, List, Map,
  Navigation, Bookmark, BookmarkPlus, Bell, Sparkles,
  SlidersHorizontal, ChevronDown, MapPin, BedDouble,
  Home, DollarSign, Check, Loader2, Clapperboard, Armchair,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { ReelsFeed } from "@/components/ui/reels-feed"
import dynamic from "next/dynamic"
import api from "@/lib/api"

const ListingsMap = dynamic(
  () => import("@/components/ui/listings-map").then((mod) => mod.ListingsMap),
  {
    ssr: false,
    loading: () => (
      <div className="w-full h-[calc(100vh-220px)] bg-slate-100 rounded-xl flex items-center justify-center">
        <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full" />
      </div>
    )
  }
)

// ─── Constants ────────────────────────────────────────────────────────────────
const CAMEROON_CITIES = [
  "Douala", "Yaoundé", "Bamenda", "Bafoussam", "Garoua",
  "Maroua", "Ngaoundéré", "Bertoua", "Limbe", "Buea", "Kribi", "Ebolowa",
]

const NEIGHBORHOODS: Record<string, string[]> = {
  Douala: ["Bonapriso", "Bonanjo", "Akwa", "Bali", "Makepe", "Logbessou", "Ndokoti", "Deïdo"],
  Yaoundé: ["Bastos", "Melen", "Nlongkak", "Mvog-Ada", "Essos", "Nsimeyong", "Biyem-Assi", "Mfandena"],
  Bamenda: ["Up Station", "Down Town", "Mile 4", "Nkwen"],
}

const PROPERTY_TYPES = [
  { value: "STUDIO", label: "Studio" },
  { value: "APARTMENT", label: "Apartment" },
  { value: "HOUSE", label: "House" },
  { value: "PRIVATE_ROOM", label: "Room" },
  { value: "SHARED_ROOM", label: "Shared" },
]

const AMENITY_KEYWORDS: Record<string, string> = {
  furnished: "Furnished", meublé: "Furnished", meuble: "Furnished",
  wifi: "WiFi", internet: "WiFi",
  parking: "Parking",
  pool: "Swimming Pool", piscine: "Swimming Pool",
  gym: "Gym",
  generator: "Generator", groupe: "Generator",
  security: "Security", gardien: "Security",
  ac: "Air Conditioning", "air conditioning": "Air Conditioning", climatisé: "Air Conditioning",
  kitchen: "Kitchen", cuisine: "Kitchen",
}

// ─── NLP Parser ───────────────────────────────────────────────────────────────
interface ParsedQuery {
  city?: string
  neighborhood?: string
  propertyType?: string
  bedrooms?: number
  maxPrice?: number
  minPrice?: number
  amenities?: string[]
  nearMe?: boolean
  remainingText?: string
}

function parseNaturalQuery(raw: string): ParsedQuery {
  const q = raw.toLowerCase().trim()
  const result: ParsedQuery = { amenities: [] }
  let text = q

  // Near me
  if (/near me|près de moi|pres de moi|nearby/i.test(text)) {
    result.nearMe = true
    text = text.replace(/near me|près de moi|pres de moi|nearby/gi, "")
  }

  // Price range  "50k–150k" / "50000 to 150000"
  const rangeMatch = text.match(/(\d+)\s*k?\s*[-–to]+\s*(\d+)\s*k?/i)
  if (rangeMatch) {
    result.minPrice = parseInt(rangeMatch[1]) * (rangeMatch[1].length <= 3 ? 1000 : 1)
    result.maxPrice = parseInt(rangeMatch[2]) * (rangeMatch[2].length <= 3 ? 1000 : 1)
    text = text.replace(rangeMatch[0], "")
  }

  // Price ceiling "under 80k" / "max 150000" / "moins de 100k"
  if (!result.maxPrice) {
    const ceilMatch = text.match(/(?:under|max|moins de|<)\s*(\d+)\s*k?/i)
    if (ceilMatch) {
      result.maxPrice = parseInt(ceilMatch[1]) * (ceilMatch[1].length <= 3 ? 1000 : 1)
      text = text.replace(ceilMatch[0], "")
    }
  }

  // Price floor "from 50k" / "min 50000"
  if (!result.minPrice) {
    const floorMatch = text.match(/(?:from|min|à partir de|>)\s*(\d+)\s*k?/i)
    if (floorMatch) {
      result.minPrice = parseInt(floorMatch[1]) * (floorMatch[1].length <= 3 ? 1000 : 1)
      text = text.replace(floorMatch[0], "")
    }
  }

  // Bedrooms "2 bedroom" / "2-bedroom" / "3 bed" / "2 chambres"
  const bedMatch = text.match(/(\d+)\s*[-]?\s*(?:bedroom|bed|chambre|pièce)s?/i)
  if (bedMatch) {
    result.bedrooms = parseInt(bedMatch[1])
    text = text.replace(bedMatch[0], "")
  }

  // Property type
  if (/\bstudio\b/i.test(text)) {
    result.propertyType = "STUDIO"
    text = text.replace(/\bstudio\b/gi, "")
  } else if (/\bapartment|appartement\b/i.test(text)) {
    result.propertyType = "APARTMENT"
    text = text.replace(/\bapartment|appartement\b/gi, "")
  } else if (/\bhouse|maison|villa\b/i.test(text)) {
    result.propertyType = "HOUSE"
    text = text.replace(/\bhouse|maison|villa\b/gi, "")
  } else if (/\bshared\b/i.test(text)) {
    result.propertyType = "SHARED_ROOM"
    text = text.replace(/\bshared\b/gi, "")
  } else if (/\broom|chambre\b/i.test(text)) {
    result.propertyType = "PRIVATE_ROOM"
    text = text.replace(/\broom|chambre\b/gi, "")
  }

  // Amenities
  for (const [keyword, amenity] of Object.entries(AMENITY_KEYWORDS)) {
    const re = new RegExp(`\\b${keyword}\\b`, "i")
    if (re.test(text)) {
      if (!result.amenities!.includes(amenity)) result.amenities!.push(amenity)
      text = text.replace(re, "")
    }
  }

  // City  (case-insensitive match)
  for (const city of CAMEROON_CITIES) {
    const re = new RegExp(`\\b${city.replace(/[éèê]/g, "[eéèê]")}\\b`, "i")
    if (re.test(raw)) {
      result.city = city
      text = text.replace(re, "")
      break
    }
  }

  // Neighborhood — word(s) after "in|à|a|dans|de" that aren't a city
  const neighMatch = text.match(/(?:in|à|a|dans|de|at)\s+([A-ZÀ-ÿa-z\-]+(?:\s+[A-ZÀ-ÿa-z\-]+){0,2})/i)
  if (neighMatch) {
    const candidate = neighMatch[1].trim()
    const isCity = CAMEROON_CITIES.some(c => c.toLowerCase() === candidate.toLowerCase())
    if (!isCity && !result.city) {
      result.neighborhood = candidate
        .split(" ")
        .map(w => w.charAt(0).toUpperCase() + w.slice(1))
        .join(" ")
      text = text.replace(neighMatch[0], "")
    }
  }
  // Fallback: match known neighborhood names (e.g. "bastos" without "in")
  if (!result.neighborhood) {
    const allNeighborhoods = Object.values(NEIGHBORHOODS).flat()
    for (const n of allNeighborhoods) {
      const re = new RegExp(`\\b${n.replace(/[éèê]/g, "[eéèê]")}\\b`, "i")
      if (re.test(text)) {
        result.neighborhood = n
        text = text.replace(re, "")
        break
      }
    }
  }

  result.remainingText = text.replace(/[,\s]+/g, " ").trim()
  return result
}

// ─── Types ────────────────────────────────────────────────────────────────────
interface Listing {
  id: string; title: string; rentAmount: number; city: string
  neighborhood: string; propertyType: string
  latitude?: number | string | null
  longitude?: number | string | null
  photos: { photoUrl: string; isPrimary: boolean }[]
  bedrooms: number; bathrooms: number; verified: boolean
  trustTier?: string | null
  featured: boolean; isFavorited: boolean
  videoTourUrl?: string
  virtualTourProvider?: string
}

interface Filters {
  city: string; neighborhood: string; propertyType: string
  minPrice: number; maxPrice: number
  bedrooms: number; bathrooms: number
  amenities: string[]; maxDistance: number; availableFrom: string
  sortBy: string; sortDir: string
}

interface UserLocation { lat: number; lon: number }
// API returns flat fields (city, neighborhood, etc.), not nested filters
interface SavedSearch {
  id: string; name: string
  city?: string; neighborhood?: string; propertyType?: string
  minPrice?: number; maxPrice?: number; bedrooms?: number; bathrooms?: number
  amenities?: string[]; maxDistance?: number; availableFrom?: string
  userLat?: number; userLon?: number
  notifyNewListings: boolean
}

const DEFAULT_FILTERS: Filters = {
  city: "", neighborhood: "", propertyType: "",
  minPrice: 0, maxPrice: 500000,
  bedrooms: 0, bathrooms: 0, amenities: [],
  maxDistance: 0, availableFrom: "",
  sortBy: "createdAt", sortDir: "DESC",
}

// ─── Component ────────────────────────────────────────────────────────────────
export default function SearchPage() {
  const { formatCurrency, language, t } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()

  // Search bar
  const [rawQuery, setRawQuery] = useState("")
  const [parsedQuery, setParsedQuery] = useState<ParsedQuery>({})
  const [showAutocomplete, setShowAutocomplete] = useState(false)
  const [autocompleteItems, setAutocompleteItems] = useState<string[]>([])
  const inputRef = useRef<HTMLInputElement>(null)

  // Filters
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS)
  const [userLocation, setUserLocation] = useState<UserLocation | null>(null)
  const [isLoadingLocation, setIsLoadingLocation] = useState(false)

  // Active filter sheet state
  const [activeSheet, setActiveSheet] = useState<"price" | "beds" | "type" | "amenities" | null>(null)

  // Results
  const [listings, setListings] = useState<Listing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [viewMode, setViewMode] = useState<"list" | "map" | "reels">("list")

  // Saved searches
  const [savedSearches, setSavedSearches] = useState<SavedSearch[]>([])
  const [showSaveSearch, setShowSaveSearch] = useState(false)
  const [saveSearchName, setSaveSearchName] = useState("")

  const safeFilters = filters ?? DEFAULT_FILTERS

  // ── Autocomplete ─────────────────────────────────────────────────────────
  useEffect(() => {
    if (!rawQuery.trim()) { setAutocompleteItems([]); return }
    const q = rawQuery.toLowerCase()
    const items: string[] = []
    CAMEROON_CITIES.forEach(c => { if (c.toLowerCase().includes(q)) items.push(c) })
    Object.values(NEIGHBORHOODS).flat().forEach(n => {
      if (n.toLowerCase().includes(q) && !items.includes(n)) items.push(n)
    })
    setAutocompleteItems(items.slice(0, 6))
    setShowAutocomplete(items.length > 0)
  }, [rawQuery])

  // ── NLP parse on Enter / Search button ───────────────────────────────────
  const handleSearch = useCallback(() => {
    setShowAutocomplete(false)
    if (!rawQuery.trim()) { setParsedQuery({}); setFilters(DEFAULT_FILTERS); return }
    const parsed = parseNaturalQuery(rawQuery)
    setParsedQuery(parsed)
    setFilters(prev => ({
      ...prev,
      city: parsed.city ?? prev.city,
      neighborhood: parsed.neighborhood ?? "",
      propertyType: parsed.propertyType ?? prev.propertyType,
      bedrooms: parsed.bedrooms ?? prev.bedrooms,
      maxPrice: parsed.maxPrice ?? prev.maxPrice,
      minPrice: parsed.minPrice ?? prev.minPrice,
      amenities: parsed.amenities?.length ? parsed.amenities : prev.amenities,
    }))
    if (parsed.nearMe) getUserLocation()
  }, [rawQuery])

  const handleAutocompleteSelect = (item: string) => {
    const isCity = CAMEROON_CITIES.includes(item)
    if (isCity) {
      setFilters(prev => ({ ...prev, city: item, neighborhood: "" }))
      setParsedQuery(prev => ({ ...prev, city: item }))
    } else {
      setFilters(prev => ({ ...prev, neighborhood: item }))
      setParsedQuery(prev => ({ ...prev, neighborhood: item }))
    }
    setRawQuery(item)
    setShowAutocomplete(false)
    inputRef.current?.blur()
  }

  // ── Fetch listings ────────────────────────────────────────────────────────
  const fetchListings = useCallback(async (page = 0) => {
    const f = filters ?? DEFAULT_FILTERS
    setIsLoading(true)
    try {
      const params: Record<string, any> = {
        size: 12, page, sortBy: f.sortBy, sortDir: f.sortDir,
      }
      if (f.city && f.city !== "all") params.city = f.city
      if (f.neighborhood) params.neighborhood = f.neighborhood
      if (f.propertyType && f.propertyType !== "all") params.propertyType = f.propertyType
      if (f.minPrice > 0) params.minPrice = f.minPrice
      if (f.maxPrice < 500000) params.maxPrice = f.maxPrice
      if (f.bedrooms > 0) params.bedrooms = f.bedrooms
      if (f.bathrooms > 0) params.bathrooms = f.bathrooms
      if ((f.amenities ?? []).length > 0) params.amenities = f.amenities
      if (f.maxDistance > 0 && userLocation) {
        params.maxDistance = f.maxDistance
        params.userLat = userLocation.lat
        params.userLon = userLocation.lon
      }
      if (user?.id) params.userId = user.id
      if (rawQuery?.trim()) params.query = rawQuery
      params.lang = language === "fr" ? "fr" : "en"

      const res = await api.get("/search", { params })
      const data = res.data
      const content = data?.content || data || []
      setListings(content)
      setTotalPages(data?.totalPages || 1)
      setTotalElements(data?.totalElements || content.length)
      setCurrentPage(data?.number || 0)
    } catch (e) {
      console.error("Failed to fetch listings:", e)
    } finally { setIsLoading(false) }
  }, [filters, userLocation, user?.id, rawQuery, language])

  useEffect(() => { fetchListings() }, [fetchListings])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({ onRefresh: fetchListings })

  // ── Geolocation ───────────────────────────────────────────────────────────
  const getUserLocation = () => {
    setIsLoadingLocation(true)
    navigator.geolocation?.getCurrentPosition(
      pos => { setUserLocation({ lat: pos.coords.latitude, lon: pos.coords.longitude }); setIsLoadingLocation(false) },
      () => setIsLoadingLocation(false)
    )
  }

  // ── Saved searches ────────────────────────────────────────────────────────
  const fetchSavedSearches = async () => {
    if (!user?.id) return
    try { const r = await api.get(`/saved-searches/user/${user.id}`); setSavedSearches(r.data) } catch { }
  }
  useEffect(() => { if (user?.id) fetchSavedSearches() }, [user?.id])

  const saveCurrentSearch = async () => {
    if (!user?.id || !saveSearchName.trim()) return
    try {
      await api.post(`/saved-searches?userId=${user.id}`, {
        name: saveSearchName, ...safeFilters,
        userLat: userLocation?.lat, userLon: userLocation?.lon,
        notifyNewListings: true, notifyPriceDrops: false,
      })
      setShowSaveSearch(false); setSaveSearchName(""); fetchSavedSearches()
    } catch { }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  const clearAll = () => { setFilters(DEFAULT_FILTERS); setUserLocation(null); setRawQuery(""); setParsedQuery({}) }

  const handleFavoriteToggle = async (id: string) => {
    if (!user?.id) return
    try { await api.post(`/listings/${id}/favorite`, null, { params: { userId: user.id } }) } catch { }
  }

  const activeFiltersCount = [
    safeFilters.city, safeFilters.neighborhood, safeFilters.propertyType,
    (safeFilters.minPrice ?? 0) > 0, (safeFilters.maxPrice ?? 500000) < 500000,
    (safeFilters.bedrooms ?? 0) > 0, (safeFilters.bathrooms ?? 0) > 0,
    (safeFilters.amenities ?? []).length > 0,
  ].filter(Boolean).length

  const priceLabel = () => {
    if ((safeFilters.minPrice ?? 0) > 0 && (safeFilters.maxPrice ?? 500000) < 500000)
      return `${Math.round((safeFilters.minPrice ?? 0) / 1000)}k–${Math.round((safeFilters.maxPrice ?? 500000) / 1000)}k`
    if ((safeFilters.maxPrice ?? 500000) < 500000) return `≤${Math.round((safeFilters.maxPrice ?? 500000) / 1000)}k`
    if (safeFilters.minPrice && safeFilters.minPrice > 0) return `≥${Math.round(safeFilters.minPrice / 1000)}k`
    return t.listings.price
  }
  const bedsLabel = () => (safeFilters.bedrooms ?? 0) > 0 ? `${safeFilters.bedrooms}+ ${t.search.beds}` : t.search.beds
  const typeLabel = () => {
    const pt = PROPERTY_TYPES.find(p => p.value === safeFilters.propertyType)
    return pt ? pt.label : t.search.type
  }
  const amenLabel = () => (safeFilters.amenities ?? []).length > 0 ? `${(safeFilters.amenities ?? []).length} ${t.listings.amenities.toLowerCase()}` : t.listings.amenities

  // location label for results header
  const locationLabel = safeFilters.neighborhood || safeFilters.city || (parsedQuery.remainingText ?? "")

  // ── Mini sheets ───────────────────────────────────────────────────────────
  const renderSheet = () => {
    if (!activeSheet) return null
    const close = () => setActiveSheet(null)

    const backdrop = (
      <div
        className="fixed inset-0 bg-black/40 z-40 backdrop-blur-sm"
        onClick={close}
      />
    )

    const panel = (children: React.ReactNode) => (
      <div className="fixed bottom-0 left-0 right-0 bg-white rounded-t-3xl z-50 shadow-2xl animate-in slide-in-from-bottom duration-200 flex flex-col max-h-[75vh]">
        {/* Scrollable content */}
        <div className="overflow-y-auto flex-1 p-6 pb-2">
          {children}
        </div>
        {/* Sticky Apply button — always visible */}
        <div className="p-4 pt-2 border-t border-slate-100 bg-white">
          <Button className="w-full h-12 rounded-xl" onClick={close}>
            {t.common.apply}
          </Button>
        </div>
      </div>
    )

    if (activeSheet === "price") return (
      <>{backdrop}{panel(
        <div>
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-bold text-slate-900">{t.search.priceRange}</h3>
            <button onClick={() => setFilters(prev => ({ ...prev, minPrice: 0, maxPrice: 500000 }))}
              className="text-sm text-blue-600 font-medium">{t.common.clear}</button>
          </div>
          <div className="space-y-4">
            {[
              { label: t.search.anyPrice, min: 0, max: 500000 },
              { label: language === "fr" ? "Moins de 50 000" : "Under 50,000", min: 0, max: 50000 },
              { label: "50k – 100k", min: 50000, max: 100000 },
              { label: "100k – 200k", min: 100000, max: 200000 },
              { label: "200k – 350k", min: 200000, max: 350000 },
              { label: "350k+", min: 350000, max: 500000 },
            ].map(opt => {
              const active = safeFilters.minPrice === opt.min && safeFilters.maxPrice === opt.max
              return (
                <button key={opt.label}
                  onClick={() => setFilters(prev => ({ ...prev, minPrice: opt.min, maxPrice: opt.max }))}
                  className={`w-full flex items-center justify-between p-4 rounded-xl border-2 transition-colors ${active ? "border-blue-600 bg-blue-50" : "border-slate-200 hover:border-slate-300"}`}>
                  <span className={`font-medium ${active ? "text-blue-700" : "text-slate-700"}`}>{opt.label}</span>
                  {active && <Check className="h-5 w-5 text-blue-600" />}
                </button>
              )
            })}
          </div>
        </div>
      )}</>
    )

    if (activeSheet === "beds") return (
      <>{backdrop}{panel(
        <div>
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-bold text-slate-900">{t.listings.bedrooms}</h3>
            <button onClick={() => setFilters(prev => ({ ...prev, bedrooms: 0 }))}
              className="text-sm text-blue-600 font-medium">{t.common.clear}</button>
          </div>
          <div className="grid grid-cols-5 gap-2">
            {[0, 1, 2, 3, 4].map(n => (
              <button key={n}
                onClick={() => setFilters(prev => ({ ...prev, bedrooms: n }))}
                className={`py-3 rounded-xl border-2 font-semibold text-sm transition-colors ${safeFilters.bedrooms === n ? "border-blue-600 bg-blue-600 text-white" : "border-slate-200 text-slate-700 hover:border-slate-300"}`}>
                {n === 0 ? t.common.any : `${n}+`}
              </button>
            ))}
          </div>
        </div>
      )}</>
    )

    if (activeSheet === "type") return (
      <>{backdrop}{panel(
        <div>
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-bold text-slate-900">{t.listings.propertyType}</h3>
            <button onClick={() => setFilters(prev => ({ ...prev, propertyType: "" }))}
              className="text-sm text-blue-600 font-medium">{t.common.clear}</button>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {[{ value: "", label: t.search.anyType, emoji: "🏘️" }, ...PROPERTY_TYPES.map(p => ({
              value: p.value, label: p.label,
              emoji: p.value === "STUDIO" ? "🏢" : p.value === "APARTMENT" ? "🏬" : p.value === "HOUSE" ? "🏠" : "🛏️"
            }))].map(opt => {
              const active = safeFilters.propertyType === opt.value
              return (
                <button key={opt.value}
                  onClick={() => setFilters(prev => ({ ...prev, propertyType: opt.value }))}
                  className={`p-4 rounded-xl border-2 text-left transition-colors ${active ? "border-blue-600 bg-blue-50" : "border-slate-200 hover:border-slate-300"}`}>
                  <span className="text-2xl mb-2 block">{opt.emoji}</span>
                  <span className={`font-medium text-sm ${active ? "text-blue-700" : "text-slate-700"}`}>{opt.label}</span>
                </button>
              )
            })}
          </div>
        </div>
      )}</>
    )

    if (activeSheet === "amenities") {
      const QUICK = ["WiFi", "Furnished", "Parking", "Air Conditioning", "Generator", "Security",
        "Swimming Pool", "Gym", "Kitchen", "Hot Water", "Water Tank"]
      return (
        <>{backdrop}{panel(
          <div>
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-bold text-slate-900">{t.listings.amenities}</h3>
              <button onClick={() => setFilters(prev => ({ ...prev, amenities: [] }))}
                className="text-sm text-blue-600 font-medium">{t.common.clearAll}</button>
            </div>
            <div className="grid grid-cols-2 gap-2">
              {QUICK.map(a => {
                const active = (safeFilters.amenities ?? []).includes(a)
                return (
                  <button key={a}
                    onClick={() => setFilters(prev => ({
                      ...prev,
                      amenities: active ? prev.amenities.filter(x => x !== a) : [...prev.amenities, a]
                    }))}
                    className={`flex items-center gap-2 p-3 rounded-xl border-2 text-sm font-medium transition-colors ${active ? "border-blue-600 bg-blue-50 text-blue-700" : "border-slate-200 text-slate-700 hover:border-slate-300"}`}>
                    {active && <Check className="h-4 w-4 shrink-0" />}
                    {a}
                  </button>
                )
              })}
            </div>
          </div>
        )}</>
      )
    }

    return null
  }

  // ─── Render ────────────────────────────────────────────────────────────────
  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.common.search} />

      {/* ── Saved search chips ── */}
      {user && savedSearches.length > 0 && (
        <div className="sticky top-14 z-30 bg-gradient-to-r from-blue-50 to-purple-50 border-b border-blue-100 px-4 py-2">
          <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide">
            <Bookmark className="h-4 w-4 text-blue-600 shrink-0" />
            {savedSearches.map(s => (
              <button key={s.id}
                onClick={() => {
                  setFilters({
                    ...DEFAULT_FILTERS,
                    city: s.city ?? "",
                    neighborhood: s.neighborhood ?? "",
                    propertyType: s.propertyType ?? "",
                    minPrice: s.minPrice ?? 0,
                    maxPrice: s.maxPrice ?? 500000,
                    bedrooms: s.bedrooms ?? 0,
                    bathrooms: s.bathrooms ?? 0,
                    amenities: s.amenities ?? [],
                    maxDistance: s.maxDistance ?? 0,
                    availableFrom: s.availableFrom ?? "",
                  })
                  if (s.userLat != null && s.userLon != null) setUserLocation({ lat: s.userLat, lon: s.userLon })
                }}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-white rounded-full text-xs font-medium text-blue-700 hover:bg-blue-100 transition-colors whitespace-nowrap">
                {s.name}
                {s.notifyNewListings && <Bell className="h-3 w-3" />}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* ── Search header ── */}
      <div className="sticky top-14 z-30 bg-white border-b border-slate-200 px-4 pt-3 pb-0">

        {/* NLP search bar */}
        <div className="relative">
          <div className="flex items-center gap-2 h-12 bg-slate-50 rounded-2xl border border-slate-200 px-4 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100 transition-all">
            <Search className="h-4 w-4 text-slate-400 shrink-0" />
            <input
              ref={inputRef}
              value={rawQuery}
              onChange={e => setRawQuery(e.target.value)}
              onKeyDown={e => { if (e.key === "Enter") handleSearch() }}
              onFocus={() => rawQuery && setShowAutocomplete(autocompleteItems.length > 0)}
              placeholder={t.search.searchPlaceholder}
              className="flex-1 bg-transparent text-sm text-slate-800 placeholder:text-slate-400 outline-none"
            />
            {rawQuery ? (
              <button onClick={() => { setRawQuery(""); setShowAutocomplete(false); clearAll() }}>
                <X className="h-4 w-4 text-slate-400" />
              </button>
            ) : (
              <Sparkles className="h-4 w-4 text-purple-400 shrink-0" />
            )}
          </div>

          {/* Autocomplete dropdown */}
          {showAutocomplete && autocompleteItems.length > 0 && (
            <div className="absolute top-14 left-0 right-0 bg-white rounded-2xl border border-slate-200 shadow-lg z-50 overflow-hidden">
              {autocompleteItems.map(item => {
                const isCity = CAMEROON_CITIES.includes(item)
                return (
                  <button key={item}
                    onMouseDown={() => handleAutocompleteSelect(item)}
                    className="w-full flex items-center gap-3 px-4 py-3 hover:bg-slate-50 text-left transition-colors">
                    <div className={`h-7 w-7 rounded-full flex items-center justify-center shrink-0 ${isCity ? "bg-blue-100" : "bg-purple-100"}`}>
                      <MapPin className={`h-3.5 w-3.5 ${isCity ? "text-blue-600" : "text-purple-600"}`} />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-slate-800">{item}</p>
                      <p className="text-xs text-slate-500">{isCity ? "City" : "Neighborhood"}</p>
                    </div>
                  </button>
                )
              })}
            </div>
          )}
        </div>

        {/* Parsed chips (show what NLP understood) */}
        {(Object.values(parsedQuery).some(v => v)) && (
          <div className="flex gap-2 mt-2 overflow-x-auto scrollbar-hide pb-1">
            {parsedQuery.city && <NLPChip label={parsedQuery.city} color="blue" onRemove={() => { setParsedQuery(p => ({ ...p, city: undefined })); setFilters(p => ({ ...p, city: "" })) }} />}
            {parsedQuery.neighborhood && <NLPChip label={parsedQuery.neighborhood} color="purple" onRemove={() => { setParsedQuery(p => ({ ...p, neighborhood: undefined })); setFilters(p => ({ ...p, neighborhood: "" })) }} />}
            {parsedQuery.bedrooms && <NLPChip label={`${parsedQuery.bedrooms}+ beds`} color="green" onRemove={() => { setParsedQuery(p => ({ ...p, bedrooms: undefined })); setFilters(p => ({ ...p, bedrooms: 0 })) }} />}
            {parsedQuery.maxPrice && <NLPChip label={`≤${Math.round(parsedQuery.maxPrice / 1000)}k FCFA`} color="amber" onRemove={() => { setParsedQuery(p => ({ ...p, maxPrice: undefined })); setFilters(p => ({ ...p, maxPrice: 500000 })) }} />}
            {parsedQuery.propertyType && <NLPChip label={PROPERTY_TYPES.find(t => t.value === parsedQuery.propertyType)?.label ?? ""} color="rose" onRemove={() => { setParsedQuery(p => ({ ...p, propertyType: undefined })); setFilters(p => ({ ...p, propertyType: "" })) }} />}
            {parsedQuery.amenities?.map(a => (
              <NLPChip key={a} label={a} color="teal" onRemove={() => { setParsedQuery(p => ({ ...p, amenities: p.amenities?.filter(x => x !== a) })); setFilters(p => ({ ...p, amenities: p.amenities.filter(x => x !== a) })) }} />
            ))}
          </div>
        )}

        {/* Quick-filter pills */}
        <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide py-3">
          {/* Price */}
          <FilterPill
            label={priceLabel()}
            active={(safeFilters.minPrice ?? 0) > 0 || (safeFilters.maxPrice ?? 500000) < 500000}
            icon={<DollarSign className="h-3.5 w-3.5" />}
            onClick={() => setActiveSheet("price")}
          />
          {/* Beds */}
          <FilterPill
            label={bedsLabel()}
            active={(safeFilters.bedrooms ?? 0) > 0}
            icon={<BedDouble className="h-3.5 w-3.5" />}
            onClick={() => setActiveSheet("beds")}
          />
          {/* Type */}
          <FilterPill
            label={typeLabel()}
            active={!!safeFilters.propertyType}
            icon={<Home className="h-3.5 w-3.5" />}
            onClick={() => setActiveSheet("type")}
          />
          {/* Amenities */}
          <FilterPill
            label={amenLabel()}
            active={(safeFilters.amenities ?? []).length > 0}
            icon={<Sparkles className="h-3.5 w-3.5" />}
            onClick={() => setActiveSheet("amenities")}
          />
          {/* Furnished toggle */}
          <FilterPill
            label={(safeFilters.amenities ?? []).includes("Furnished") ? (language === "fr" ? "Meublé" : "Furnished") : (language === "fr" ? "Meublé ?" : "Furnished?")}
            active={(safeFilters.amenities ?? []).includes("Furnished")}
            icon={<Armchair className="h-3.5 w-3.5" />}
            onClick={() => setFilters(prev => ({
              ...prev,
              amenities: prev.amenities.includes("Furnished")
                ? prev.amenities.filter(a => a !== "Furnished")
                : [...prev.amenities, "Furnished"]
            }))}
          />
          {/* Near me */}
          <FilterPill
            label={isLoadingLocation ? t.search.locating : userLocation ? `${t.search.nearMeActive} ✓` : t.search.nearMe}
            active={!!userLocation}
            icon={isLoadingLocation ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Navigation className="h-3.5 w-3.5" />}
            onClick={getUserLocation}
          />
          {/* Clear */}
          {activeFiltersCount > 0 && (
            <button onClick={clearAll}
              className="flex items-center gap-1.5 px-3 py-2 rounded-full text-xs font-medium whitespace-nowrap text-slate-500 hover:text-slate-800 transition-colors shrink-0">
              <X className="h-3 w-3" /> {t.common.clearAll}
            </button>
          )}
        </div>
      </div>

      {/* ── Results ── */}
      <div ref={containerRef} className="flex-1 overflow-y-auto relative">
        <PullToRefreshIndicator pullProgress={pullProgress} isRefreshing={isRefreshing} />

        {/* Results bar: count + sort + view toggle */}
        <div className="flex items-center justify-between px-4 pt-4 pb-2 gap-3">
          <p className="text-sm font-medium text-slate-600 shrink-0">
            {isLoading ? t.search.searching : (
              <>
                <span className="font-bold text-slate-900">{totalElements}</span>
                {" "}{totalElements !== 1 ? t.search.homes : t.search.home}
                {locationLabel ? <> {t.search.inLocation} <span className="text-blue-700">{locationLabel}</span></> : ""}
              </>
            )}
          </p>
            <div className="flex items-center gap-2 shrink-0" data-tour="search-view-modes">
            {/* Sort */}
            <div className="relative">
              <select
                value={`${safeFilters.sortBy}-${safeFilters.sortDir}`}
                onChange={e => {
                  const [sortBy, sortDir] = e.target.value.split("-")
                  setFilters(prev => ({ ...prev, sortBy, sortDir }))
                }}
                className="appearance-none bg-slate-100 text-xs font-medium text-slate-700 pl-3 pr-7 py-1.5 rounded-full focus:outline-none cursor-pointer">
                <option value="createdAt-DESC">{t.search.newest}</option>
                <option value="rentAmount-ASC">{t.search.cheapest}</option>
                <option value="rentAmount-DESC">{t.search.mostExpensive}</option>
              </select>
              <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 h-3 w-3 text-slate-500 pointer-events-none" />
            </div>
            {/* View toggle — List / Map / Reels (reels only when enough video-tour inventory) */}
            <div className="flex bg-slate-100 rounded-full p-0.5">
              <button type="button" onClick={() => setViewMode("list")}
                className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${viewMode === "list" ? "bg-white shadow-sm text-blue-600" : "text-slate-500"}`}>
                <List className="h-3.5 w-3.5" />
              </button>
              <button type="button" onClick={() => setViewMode("map")}
                className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${viewMode === "map" ? "bg-white shadow-sm text-blue-600" : "text-slate-500"}`}>
                <Map className="h-3.5 w-3.5" />
              </button>
              <button
                type="button"
                data-tour="search-reels-btn"
                onClick={() => setViewMode("reels")}
                className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${viewMode === "reels" ? "bg-white shadow-sm text-purple-600" : "text-slate-500"}`}
                title="Property tours"
              >
                <Clapperboard className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Reels view — full screen feed */}
        {viewMode === "reels" && !isLoading && listings.length > 0 && (
          <ReelsFeed
            listings={listings}
            onFavoriteToggle={handleFavoriteToggle}
            onClose={() => setViewMode("list")}
          />
        )}

        {/* Reels loading placeholder */}
        {viewMode === "reels" && isLoading && (
          <div className="h-[calc(100vh-200px)] bg-black rounded-xl flex items-center justify-center">
            <div className="text-center">
              <Clapperboard className="h-12 w-12 text-white/30 mx-auto mb-3" />
              <p className="text-white/50 text-sm">{t.search.loadingFeed}</p>
            </div>
          </div>
        )}

        {/* Map view */}
        {viewMode === "map" && !isLoading && (
          <div className="px-4 pb-4">
            <ListingsMap
              listings={listings.map(l => ({
                id: l.id, title: l.title, neighborhood: l.neighborhood,
                city: l.city, rentAmount: l.rentAmount,
                latitude: l.latitude, longitude: l.longitude,
                photoUrl: l.photos?.find(p => p.isPrimary)?.photoUrl || l.photos?.[0]?.photoUrl,
              }))}
              selectedCity={safeFilters.city || "Douala"}
              onListingClick={id => {
                router.push(`/listings/${id}`)
              }}
              className="h-[calc(100vh-260px)]"
              formatCurrency={formatCurrency}
            />
          </div>
        )}

        {/* List view */}
        {viewMode === "list" && (
          <div className="px-4 pb-6">
            {/* Loading skeletons */}
            {isLoading && (
              <div className="grid gap-4 sm:grid-cols-2">
                {[1, 2, 3, 4].map(i => (
                  <div key={i} className="rounded-2xl bg-white p-3 shadow-sm">
                    <Skeleton className="aspect-[4/3] rounded-xl mb-3" />
                    <Skeleton className="h-5 w-24 mb-2" />
                    <Skeleton className="h-4 w-full mb-2" />
                    <Skeleton className="h-4 w-32" />
                  </div>
                ))}
              </div>
            )}

            {/* Empty */}
            {!isLoading && listings.length === 0 && (
              <div className="text-center py-16">
                <div className="text-6xl mb-4">🔍</div>
                <h3 className="text-lg font-semibold text-slate-900 mb-2">{t.search.noResults}</h3>
                <p className="text-slate-500 text-sm max-w-xs mx-auto mb-6">
                  {t.search.noResultsHint}
                </p>
                <Button variant="outline" onClick={clearAll} className="rounded-xl">
                  {t.search.clearFilters}
                </Button>
              </div>
            )}

            {/* Listings grid */}
            {!isLoading && listings.length > 0 && (
              <>
                <div className="grid gap-4 sm:grid-cols-2">
                  {listings.map(listing => (
                    <ListingCard
                      key={listing.id}
                      id={listing.id}
                      title={listing.title}
                      price={listing.rentAmount}
                      city={listing.city}
                      neighborhood={listing.neighborhood}
                      bedrooms={listing.bedrooms}
                      bathrooms={listing.bathrooms}
                      imageUrl={listing.photos?.find(p => p.isPrimary)?.photoUrl || listing.photos?.[0]?.photoUrl}
                      isVerified={listing.verified}
                      trustTier={listing.trustTier}
                      isFeatured={listing.featured}
                      isFavorited={listing.isFavorited}
                      onFavoriteToggle={handleFavoriteToggle}
                    />
                  ))}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-center gap-3 py-6">
                    <Button variant="outline" size="sm" className="rounded-full"
                      disabled={currentPage === 0} onClick={() => fetchListings(currentPage - 1)}>
                      <ChevronLeft className="h-4 w-4 mr-1" /> {t.search.prev}
                    </Button>
                    <span className="text-sm text-slate-500 font-medium">
                      {currentPage + 1} / {totalPages}
                    </span>
                    <Button variant="outline" size="sm" className="rounded-full"
                      disabled={currentPage >= totalPages - 1} onClick={() => fetchListings(currentPage + 1)}>
                      {t.common.next} <ChevronRight className="h-4 w-4 ml-1" />
                    </Button>
                  </div>
                )}
              </>
            )}

            {/* Save search */}
            {user && activeFiltersCount > 0 && !isLoading && listings.length > 0 && (
              <div className="pt-2 pb-4 text-center">
                <button onClick={() => setShowSaveSearch(true)}
                  className="inline-flex items-center gap-2 text-sm text-blue-600 font-medium hover:text-blue-700">
                  <BookmarkPlus className="h-4 w-4" />
                  {t.search.saveSearch}
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* ── Filter sheets ── */}
      {renderSheet()}

      {/* ── Save search dialog ── */}
      {showSaveSearch && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-end sm:items-center justify-center p-4 sm:pb-4" style={{ paddingBottom: "max(7rem, calc(env(safe-area-inset-bottom, 0px) + 5.5rem))" }}>
          <div className="bg-white rounded-3xl p-6 w-full max-w-md max-h-[80vh] overflow-y-auto animate-in slide-in-from-bottom shadow-xl">
            <h3 className="text-lg font-bold mb-1">{t.search.saveSearch}</h3>
            <p className="text-sm text-slate-500 mb-4">{t.search.saveSearchDesc}</p>
            <input
              placeholder={language === "fr" ? "ex. 2CH à Bastos moins de 80k" : "e.g. 2BR in Bastos under 80k"}
              value={saveSearchName}
              onChange={e => setSaveSearchName(e.target.value)}
              className="w-full h-12 rounded-xl border border-slate-200 px-4 text-sm mb-4 focus:ring-2 focus:ring-blue-100 focus:border-blue-500 outline-none"
            />
            <div className="flex gap-2">
              <Button variant="outline" className="flex-1 h-12 rounded-xl"
                onClick={() => { setShowSaveSearch(false); setSaveSearchName("") }}>
                {t.common.cancel}
              </Button>
              <Button className="flex-1 h-12 rounded-xl" disabled={!saveSearchName.trim()} onClick={saveCurrentSearch}>
                <BookmarkPlus className="h-4 w-4 mr-2" /> {t.common.save}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Small sub-components ──────────────────────────────────────────────────────
function FilterPill({ label, active, icon, onClick }: {
  label: string; active: boolean; icon: React.ReactNode; onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-1.5 px-3 py-2 rounded-full text-xs font-medium whitespace-nowrap border transition-all shrink-0 ${active
        ? "bg-blue-600 text-white border-blue-600 shadow-sm shadow-blue-200"
        : "bg-white text-slate-600 border-slate-200 hover:border-slate-300"
        }`}>
      {icon}
      {label}
      <ChevronDown className={`h-3 w-3 ml-0.5 transition-transform ${active ? "opacity-70" : "opacity-50"}`} />
    </button>
  )
}

function NLPChip({ label, color, onRemove }: {
  label: string; color: string; onRemove: () => void
}) {
  const colorMap: Record<string, string> = {
    blue: "bg-blue-100 text-blue-800",
    purple: "bg-purple-100 text-purple-800",
    green: "bg-green-100 text-green-800",
    amber: "bg-amber-100 text-amber-800",
    rose: "bg-rose-100 text-rose-800",
    teal: "bg-teal-100 text-teal-800",
  }
  return (
    <span className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium shrink-0 ${colorMap[color]}`}>
      {label}
      <button onClick={onRemove} className="opacity-60 hover:opacity-100">
        <X className="h-3 w-3" />
      </button>
    </span>
  )
}
