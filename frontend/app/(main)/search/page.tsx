"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { ListingCard } from "@/components/cards/listing-card"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { Search, SlidersHorizontal, MapPin, X, ChevronLeft, ChevronRight, List, Map } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import dynamic from "next/dynamic"

// Dynamically import map to avoid SSR issues
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
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Slider } from "@/components/ui/slider"
import api from "@/lib/api"

interface Listing {
  id: string
  title: string
  rentAmount: number
  city: string
  neighborhood: string
  propertyType: string
  photos: { photoUrl: string; isPrimary: boolean }[]
  bedrooms: number
  bathrooms: number
  verified: boolean
  featured: boolean
  isFavorited: boolean
}

interface Filters {
  city: string
  propertyType: string
  minPrice: number
  maxPrice: number
}

const CAMEROON_CITIES = [
  "Douala",
  "Yaoundé",
  "Bamenda",
  "Bafoussam",
  "Garoua",
  "Maroua",
  "Ngaoundéré",
  "Bertoua",
  "Limbe",
  "Buea",
  "Kribi",
  "Ebolowa",
]

const PROPERTY_TYPES = [
  { value: "STUDIO", label: "Studio" },
  { value: "APARTMENT", label: "Apartment" },
  { value: "HOUSE", label: "House" },
  { value: "PRIVATE_ROOM", label: "Private Room" },
  { value: "SHARED_ROOM", label: "Shared Room" },
]

export default function SearchPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [searchQuery, setSearchQuery] = useState("")
  const [listings, setListings] = useState<Listing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [filters, setFilters] = useState<Filters>({
    city: "",
    propertyType: "",
    minPrice: 0,
    maxPrice: 500000,
  })
  const [isFilterOpen, setIsFilterOpen] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [viewMode, setViewMode] = useState<"list" | "map">("list")
  const PAGE_SIZE = 12

  const fetchListings = useCallback(async (page: number = 0) => {
    setIsLoading(true)
    try {
      const params: Record<string, any> = {
        size: PAGE_SIZE,
        page: page,
        sortBy: "createdAt",
        sortDir: "DESC",
      }
      
      if (filters.city && filters.city !== "all") params.city = filters.city
      if (filters.propertyType && filters.propertyType !== "all") params.propertyType = filters.propertyType
      if (filters.minPrice > 0) params.minPrice = filters.minPrice
      if (filters.maxPrice < 500000) params.maxPrice = filters.maxPrice
      if (user?.id) params.userId = user.id

      const response = await api.get("/listings", { params })
      const data = response.data
      const content = data?.content || data || []
      setListings(content)
      setTotalPages(data?.totalPages || 1)
      setTotalElements(data?.totalElements || content.length)
      setCurrentPage(data?.number || 0)
    } catch (err) {
      console.error("Failed to fetch listings:", err)
    } finally {
      setIsLoading(false)
    }
  }, [filters, user?.id])

  useEffect(() => {
    fetchListings()
  }, [fetchListings])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchListings,
  })

  const handleFavoriteToggle = async (listingId: string) => {
    if (!user?.id) return
    try {
      await api.post(`/listings/${listingId}/favorite`, null, {
        params: { userId: user.id }
      })
    } catch (err) {
      console.error("Failed to toggle favorite:", err)
    }
  }

  const clearFilters = () => {
    setFilters({
      city: "",
      propertyType: "",
      minPrice: 0,
      maxPrice: 500000,
    })
  }

  const activeFiltersCount = [
    filters.city,
    filters.propertyType,
    filters.minPrice > 0,
    filters.maxPrice < 500000,
  ].filter(Boolean).length

  const filteredListings = listings.filter((listing) => {
    if (!searchQuery) return true
    const query = searchQuery.toLowerCase()
    return (
      listing.title.toLowerCase().includes(query) ||
      listing.city.toLowerCase().includes(query) ||
      listing.neighborhood?.toLowerCase().includes(query)
    )
  })

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.nav.search} />

      {/* Search Bar */}
      <div className="sticky top-14 z-30 bg-white border-b border-slate-200 p-4">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder={t.listings.searchPlaceholder}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 h-11 rounded-xl border-slate-200 bg-slate-50"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                className="absolute right-3 top-1/2 -translate-y-1/2"
              >
                <X className="h-4 w-4 text-slate-400" />
              </button>
            )}
          </div>
          
          <Sheet open={isFilterOpen} onOpenChange={setIsFilterOpen}>
            <SheetTrigger asChild>
              <Button 
                variant="outline" 
                size="icon" 
                className="h-11 w-11 rounded-xl relative"
              >
                <SlidersHorizontal className="h-5 w-5" />
                {activeFiltersCount > 0 && (
                  <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-blue-600 text-white text-xs flex items-center justify-center">
                    {activeFiltersCount}
                  </span>
                )}
              </Button>
            </SheetTrigger>
            <SheetContent side="bottom" className="h-[80vh] rounded-t-3xl">
              <SheetHeader>
                <SheetTitle className="flex items-center justify-between">
                  <span>{t.common.filter}</span>
                  <Button variant="ghost" size="sm" onClick={clearFilters}>
                    Clear all
                  </Button>
                </SheetTitle>
              </SheetHeader>
              
              <div className="mt-6 space-y-6">
                {/* City */}
                <div>
                  <label className="text-sm font-medium text-slate-700 mb-2 block">
                    City
                  </label>
                  <Select
                    value={filters.city}
                    onValueChange={(value) => setFilters({ ...filters, city: value })}
                  >
                    <SelectTrigger className="h-11 rounded-xl">
                      <SelectValue placeholder="Select city" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All cities</SelectItem>
                      {CAMEROON_CITIES.map((city) => (
                        <SelectItem key={city} value={city}>
                          {city}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Property Type */}
                <div>
                  <label className="text-sm font-medium text-slate-700 mb-2 block">
                    {t.listings.propertyType}
                  </label>
                  <Select
                    value={filters.propertyType}
                    onValueChange={(value) => setFilters({ ...filters, propertyType: value })}
                  >
                    <SelectTrigger className="h-11 rounded-xl">
                      <SelectValue placeholder="Select type" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All types</SelectItem>
                      {PROPERTY_TYPES.map((type) => (
                        <SelectItem key={type.value} value={type.value}>
                          {type.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Price Range */}
                <div>
                  <label className="text-sm font-medium text-slate-700 mb-4 block">
                    Price Range: {formatCurrency(filters.minPrice)} - {formatCurrency(filters.maxPrice)}
                  </label>
                  <Slider
                    value={[filters.minPrice, filters.maxPrice]}
                    onValueChange={([min, max]) => setFilters({ ...filters, minPrice: min, maxPrice: max })}
                    min={0}
                    max={500000}
                    step={10000}
                    className="mt-2"
                  />
                </div>

                <Button 
                  className="w-full h-12 rounded-xl"
                  onClick={() => {
                    setIsFilterOpen(false)
                    fetchListings()
                  }}
                >
                  Apply Filters
                </Button>
              </div>
            </SheetContent>
          </Sheet>
        </div>

        {/* Quick City Filters + View Toggle */}
        <div className="flex items-center gap-2 mt-3">
          <div className="flex gap-2 flex-1 overflow-x-auto scrollbar-hide pb-1">
            {CAMEROON_CITIES.slice(0, 5).map((city) => (
              <button
                key={city}
                onClick={() => setFilters({ ...filters, city: filters.city === city ? "" : city })}
                className={`flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-colors ${
                  filters.city === city
                    ? "bg-blue-600 text-white"
                    : "bg-slate-100 text-slate-600"
                }`}
              >
                <MapPin className="h-3 w-3" />
                {city}
              </button>
            ))}
          </div>
          
          {/* View Toggle */}
          <div className="flex bg-slate-100 rounded-lg p-0.5 flex-shrink-0">
            <button
              onClick={() => setViewMode("list")}
              className={`px-3 py-1.5 rounded-md transition-colors flex items-center gap-1 text-xs font-medium ${
                viewMode === "list" ? "bg-white shadow-sm text-blue-600" : "text-slate-500"
              }`}
            >
              <List className="h-4 w-4" />
              List
            </button>
            <button
              onClick={() => setViewMode("map")}
              className={`px-3 py-1.5 rounded-md transition-colors flex items-center gap-1 text-xs font-medium ${
                viewMode === "map" ? "bg-white shadow-sm text-blue-600" : "text-slate-500"
              }`}
            >
              <Map className="h-4 w-4" />
              Map
            </button>
          </div>
        </div>
      </div>

      {/* Results */}
      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        {/* Map View */}
        {viewMode === "map" && !isLoading && (
          <div className="p-4">
            <ListingsMap
              listings={filteredListings.map(l => ({
                id: l.id,
                title: l.title,
                neighborhood: l.neighborhood,
                city: l.city,
                rentAmount: l.rentAmount,
                photoUrl: l.photos?.find(p => p.isPrimary)?.photoUrl || l.photos?.[0]?.photoUrl,
              }))}
              selectedCity={filters.city || "Douala"}
              onListingClick={(id) => router.push(`/listings/${id}`)}
              className="h-[calc(100vh-220px)]"
              formatCurrency={formatCurrency}
            />
          </div>
        )}

        {/* List View */}
        {viewMode === "list" && (
        <div className="p-4">
          {/* Results count */}
          {!isLoading && (
            <div className="flex items-center justify-between mb-4">
              <p className="text-sm text-slate-500">
                {totalElements} {totalElements === 1 ? "result" : "results"}
                {totalPages > 1 && ` • Page ${currentPage + 1} of ${totalPages}`}
              </p>
            </div>
          )}

          {/* Loading */}
          {isLoading && (
            <div className="grid gap-4 sm:grid-cols-2">
              {[1, 2, 3, 4].map((i) => (
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
          {!isLoading && filteredListings.length === 0 && (
            <div className="text-center py-12">
              <div className="text-6xl mb-4">🔍</div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                {t.listings.noResults}
              </h3>
              <p className="text-slate-500 text-sm">
                Try adjusting your filters or search query
              </p>
            </div>
          )}

          {/* Listings */}
          {!isLoading && filteredListings.length > 0 && (
            <>
              <div className="grid gap-4 sm:grid-cols-2">
                {filteredListings.map((listing) => (
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
                    isFeatured={listing.featured}
                    isFavorited={listing.isFavorited}
                    onFavoriteToggle={handleFavoriteToggle}
                  />
                ))}
              </div>

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-4 py-6">
                  <Button
                    variant="outline"
                    size="sm"
                    className="rounded-full"
                    disabled={currentPage === 0}
                    onClick={() => fetchListings(currentPage - 1)}
                  >
                    <ChevronLeft className="h-4 w-4 mr-1" />
                    Previous
                  </Button>
                  <div className="flex items-center gap-2">
                    {/* Page numbers */}
                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                      let pageNum: number
                      if (totalPages <= 5) {
                        pageNum = i
                      } else if (currentPage < 3) {
                        pageNum = i
                      } else if (currentPage > totalPages - 4) {
                        pageNum = totalPages - 5 + i
                      } else {
                        pageNum = currentPage - 2 + i
                      }
                      return (
                        <button
                          key={pageNum}
                          onClick={() => fetchListings(pageNum)}
                          className={`w-8 h-8 rounded-full text-sm font-medium transition-colors ${
                            currentPage === pageNum
                              ? "bg-blue-600 text-white"
                              : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                          }`}
                        >
                          {pageNum + 1}
                        </button>
                      )
                    })}
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    className="rounded-full"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => fetchListings(currentPage + 1)}
                  >
                    Next
                    <ChevronRight className="h-4 w-4 ml-1" />
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
        )}
      </div>
    </div>
  )
}
