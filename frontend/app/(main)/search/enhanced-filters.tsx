"use client"

import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Slider } from "@/components/ui/slider"
import { Input } from "@/components/ui/input"
import { Checkbox } from "@/components/ui/checkbox"
import { Navigation, MapPin } from "lucide-react"

interface Filters {
  city: string
  propertyType: string
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
          📍 Location & Distance
        </label>
        <div className="space-y-3">
          <Button
            variant={userLocation ? "default" : "outline"}
            className="w-full h-11 rounded-xl"
            onClick={onGetLocation}
            disabled={isLoadingLocation}
          >
            <Navigation className="h-4 w-4 mr-2" />
            {isLoadingLocation ? "Getting location..." : userLocation ? "Location enabled" : "Use my location"}
          </Button>
          
          {userLocation && (
            <div>
              <label className="text-xs text-slate-600 mb-2 block">
                Max Distance: {filters.maxDistance || "Any"} km
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
                <span>Any</span>
                <span>50 km</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* City */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          🏙️ City
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
              <SelectItem key={city} value={city}>{city}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Property Type */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          🏠 Property Type
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
              <SelectItem key={type.value} value={type.value}>{type.label}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Bedrooms */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          🛏️ Bedrooms (min)
        </label>
        <div className="grid grid-cols-5 gap-2">
          {[0, 1, 2, 3, 4].map((num) => (
            <Button
              key={num}
              variant={filters.bedrooms === num ? "default" : "outline"}
              className="h-11 rounded-xl"
              onClick={() => setFilters({ ...filters, bedrooms: num })}
            >
              {num === 0 ? "Any" : num}
            </Button>
          ))}
        </div>
      </div>

      {/* Bathrooms */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-2 block">
          🚿 Bathrooms (min)
        </label>
        <div className="grid grid-cols-4 gap-2">
          {[0, 1, 2, 3].map((num) => (
            <Button
              key={num}
              variant={filters.bathrooms === num ? "default" : "outline"}
              className="h-11 rounded-xl"
              onClick={() => setFilters({ ...filters, bathrooms: num })}
            >
              {num === 0 ? "Any" : num}
            </Button>
          ))}
        </div>
      </div>

      {/* Price Range */}
      <div>
        <label className="text-sm font-medium text-slate-700 mb-4 block">
          💰 Price Range: {formatCurrency(filters.minPrice)} - {formatCurrency(filters.maxPrice)}
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
          ✨ Amenities ({filters.amenities.length} selected)
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
          📅 Available From
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
