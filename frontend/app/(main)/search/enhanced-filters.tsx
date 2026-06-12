"use client"

import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Slider } from "@/components/ui/slider"
import { Input } from "@/components/ui/input"
import { Checkbox } from "@/components/ui/checkbox"
import { Navigation, MapPin } from "lucide-react"
import { useLanguage } from "@/context/language-context"

interface Filters {
  city: string
  propertyType: string
  listingPurpose: string
  stayType: string
  furnishingStatus: string
  sharedAccommodation: boolean
  minPrice: number
  maxPrice: number
  bedrooms: number
  bathrooms: number
  amenities: string[]
  maxDistance: number
  availableFrom: string
}

interface EnhancedFiltersProps {
  filters: Filters
  setFilters: (filters: Filters) => void
  userLocation: { lat: number; lon: number } | null
  onGetLocation: () => void
  isLoadingLocation: boolean
  formatCurrency: (amount: number) => string
}

const CAMEROON_CITIES = [
  "Douala", "Yaoundé", "Bamenda", "Bafoussam", "Garoua", "Maroua",
  "Ngaoundéré", "Bertoua", "Limbe", "Buea", "Kribi", "Ebolowa"
]

const PROPERTY_TYPES = [
  { value: "STUDIO", label: "Studio" },
  { value: "APARTMENT", label: "Apartment" },
  { value: "HOUSE", label: "House" },
  { value: "ROOM", label: "Room / Chambre" },
  { value: "PRIVATE_ROOM", label: "Private Room" },
  { value: "SHARED_ROOM", label: "Shared Room" },
]

const AMENITIES = [
  "WiFi", "Parking", "Security", "Water", "Electricity", "Furnished",
  "Air Conditioning", "Heating", "Balcony", "Garden", "Gym", "Pool",
  "Laundry", "Kitchen", "Elevator", "Pet Friendly"
]

export function EnhancedFilters({
  filters,
  setFilters,
  userLocation,
  onGetLocation,
  isLoadingLocation,
  formatCurrency
}: EnhancedFiltersProps) {
  
  const { t, language } = useLanguage()
  const toggleAmenity = (amenity: string) => {
    const newAmenities = filters.amenities.includes(amenity)
      ? filters.amenities.filter(a => a !== amenity)
      : [...filters.amenities, amenity]
    setFilters({ ...filters, amenities: newAmenities })
  }

  return (
    <div className="space-y-6 max-h-[60vh] overflow-y-auto pb-4">
      {/* Location & Distance */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          📍 {language === "fr" ? "Emplacement & Distance" : "Location & Distance"}
        </label>
        <div className="space-y-3">
          <Button
            variant={userLocation ? "default" : "outline"}
            className="w-full h-11 rounded-xl"
            onClick={onGetLocation}
            disabled={isLoadingLocation}
          >
            <Navigation className="h-4 w-4 mr-2" />
            {isLoadingLocation ? (language === "fr" ? "Localisation..." : "Getting location...") : userLocation ? (language === "fr" ? "Position activée" : "Location enabled") : (language === "fr" ? "Utiliser ma position" : "Use my location")}
          </Button>
          
          {userLocation && (
            <div>
              <label className="text-xs text-slate-600 mb-2 block">
                {language === "fr" ? "Distance max" : "Max Distance"}: {filters.maxDistance || t.common.any} km
              </label>
              <Slider
                value={[filters.maxDistance]}
                onValueChange={([value]) => setFilters({ ...filters, maxDistance: value })}
                min={0}
                max={50}
                step={1}
                className="mt-2"
              />
              <div className="flex justify-between text-xs text-slate-500 mt-1">
                <span>{t.common.any}</span>
                <span>50 km</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* City */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          🏙️ {t.search.city}
        </label>
        <Select
          value={filters.city}
          onValueChange={(value) => setFilters({ ...filters, city: value })}
        >
          <SelectTrigger className="h-11 rounded-xl">
            <SelectValue placeholder={t.landlordForm.selectCity} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{language === "fr" ? "Toutes les villes" : "All cities"}</SelectItem>
            {CAMEROON_CITIES.map((city) => (
              <SelectItem key={city} value={city}>{city}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Property Type */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          🏠 {t.listings.propertyType}
        </label>
        <Select
          value={filters.propertyType}
          onValueChange={(value) => setFilters({ ...filters, propertyType: value })}
        >
          <SelectTrigger className="h-11 rounded-xl">
            <SelectValue placeholder={t.landlordForm.selectType} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t.search.anyType}</SelectItem>
            {PROPERTY_TYPES.map((type) => (
              <SelectItem key={type.value} value={type.value}>{type.label}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-sm font-medium text-slate-700 mb-2 block">
            {language === "fr" ? "Objectif" : "Purpose"}
          </label>
          <Select value={filters.listingPurpose || "all"} onValueChange={(value) => setFilters({ ...filters, listingPurpose: value === "all" ? "" : value })}>
            <SelectTrigger className="h-11 rounded-xl"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t.common.any}</SelectItem>
              <SelectItem value="RENT">{language === "fr" ? "À louer" : "Rent"}</SelectItem>
              <SelectItem value="SALE">{language === "fr" ? "À vendre" : "Sale"}</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div>
          <label className="text-sm font-medium text-slate-700 mb-2 block">
            {language === "fr" ? "Meublé" : "Furnishing"}
          </label>
          <Select value={filters.furnishingStatus || "all"} onValueChange={(value) => setFilters({ ...filters, furnishingStatus: value === "all" ? "" : value })}>
            <SelectTrigger className="h-11 rounded-xl"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t.common.any}</SelectItem>
              <SelectItem value="FURNISHED">{language === "fr" ? "Meublé" : "Furnished"}</SelectItem>
              <SelectItem value="SEMI_FURNISHED">{language === "fr" ? "Semi-meublé" : "Semi-furnished"}</SelectItem>
              <SelectItem value="UNFURNISHED">{language === "fr" ? "Non meublé" : "Unfurnished"}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <Checkbox
          id="shared-accommodation"
          checked={filters.sharedAccommodation}
          onCheckedChange={(checked) => setFilters({ ...filters, sharedAccommodation: Boolean(checked) })}
        />
        <label htmlFor="shared-accommodation" className="text-sm text-slate-700">
          {language === "fr" ? "Colocation uniquement" : "Shared accommodation only"}
        </label>
      </div>

      {/* Bedrooms */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          🛏️ {t.listings.bedrooms} (min)
        </label>
        <div className="grid grid-cols-5 gap-2">
          {[0, 1, 2, 3, 4].map((num) => (
            <Button
              key={num}
              variant={filters.bedrooms === num ? "default" : "outline"}
              className="h-11 rounded-xl"
              onClick={() => setFilters({ ...filters, bedrooms: num })}
            >
              {num === 0 ? t.common.any : num}
            </Button>
          ))}
        </div>
      </div>

      {/* Bathrooms */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          🚿 {t.listings.bathrooms} (min)
        </label>
        <div className="grid grid-cols-4 gap-2">
          {[0, 1, 2, 3].map((num) => (
            <Button
              key={num}
              variant={filters.bathrooms === num ? "default" : "outline"}
              className="h-11 rounded-xl"
              onClick={() => setFilters({ ...filters, bathrooms: num })}
            >
              {num === 0 ? t.common.any : num}
            </Button>
          ))}
        </div>
      </div>

      {/* Price Range */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-4 block">
          💰 {t.search.priceRange}: {formatCurrency(filters.minPrice)} - {formatCurrency(filters.maxPrice)}
        </label>
        <Slider
          value={[filters.minPrice, filters.maxPrice]}
          onValueChange={([min, max]) => setFilters({ ...filters, minPrice: min, maxPrice: max })}
          min={0}
          max={500000}
          step={10000}
          className="mt-2"
        />
        <div className="flex justify-between text-xs text-slate-500 mt-1">
          <span>0</span>
          <span>500K</span>
        </div>
      </div>

      {/* Amenities */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-3 block">
          ✨ {t.listings.amenities} ({filters.amenities.length} {language === "fr" ? "sélectionnés" : "selected"})
        </label>
        <div className="grid grid-cols-2 gap-2">
          {AMENITIES.map((amenity) => (
            <div
              key={amenity}
              className="flex items-center space-x-2 p-2 rounded-lg border border-slate-200 hover:bg-slate-50 cursor-pointer"
              onClick={() => toggleAmenity(amenity)}
            >
              <Checkbox
                checked={filters.amenities.includes(amenity)}
                onCheckedChange={() => toggleAmenity(amenity)}
              />
              <label className="text-sm cursor-pointer flex-1">
                {amenity}
              </label>
            </div>
          ))}
        </div>
      </div>

      {/* Availability Date */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          📅 {language === "fr" ? "Disponible à partir du" : "Available From"}
        </label>
        <Input
          type="date"
          value={filters.availableFrom}
          onChange={(e) => setFilters({ ...filters, availableFrom: e.target.value })}
          className="h-11 rounded-xl"
        />
      </div>
    </div>
  )
}
