"use client"

import { useEffect, useState } from "react"
import dynamic from "next/dynamic"
import { Skeleton } from "@/components/ui/skeleton"
import { getNeighborhoodCoordinates, CITY_CENTERS } from "@/lib/neighborhoods"

// Dynamically import Leaflet components to avoid SSR issues
const MapContainer = dynamic(
  () => import("react-leaflet").then((mod) => mod.MapContainer),
  { ssr: false }
)
const TileLayer = dynamic(
  () => import("react-leaflet").then((mod) => mod.TileLayer),
  { ssr: false }
)
const Marker = dynamic(
  () => import("react-leaflet").then((mod) => mod.Marker),
  { ssr: false }
)
const Popup = dynamic(
  () => import("react-leaflet").then((mod) => mod.Popup),
  { ssr: false }
)

export interface ListingMapItem {
  id: string
  title: string
  neighborhood: string
  city: string
  rentAmount: number
  photoUrl?: string | null
}

interface ListingsMapProps {
  listings: ListingMapItem[]
  selectedCity?: string
  onListingClick?: (listingId: string) => void
  className?: string
  formatCurrency: (amount: number) => string
}

export function ListingsMap({ 
  listings, 
  selectedCity = "Douala",
  onListingClick,
  className = "",
  formatCurrency
}: ListingsMapProps) {
  const [isMounted, setIsMounted] = useState(false)
  const [customIcon, setCustomIcon] = useState<L.Icon | null>(null)

  useEffect(() => {
    setIsMounted(true)
    
    // Create custom icon after mounting (Leaflet needs window)
    if (typeof window !== "undefined") {
      import("leaflet").then((L) => {
        // Fix default marker icon issue with webpack
        delete (L.Icon.Default.prototype as any)._getIconUrl
        L.Icon.Default.mergeOptions({
          iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png",
          iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png",
          shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png",
        })
        
        const icon = new L.Icon({
          iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png",
          iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png",
          shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png",
          iconSize: [25, 41],
          iconAnchor: [12, 41],
          popupAnchor: [1, -34],
          shadowSize: [41, 41],
        })
        setCustomIcon(icon)
      })
    }
  }, [])

  if (!isMounted) {
    return (
      <div className={`relative ${className}`}>
        <Skeleton className="w-full h-full min-h-[300px] rounded-xl" />
      </div>
    )
  }

  // Get city center for initial view
  const cityCenter = CITY_CENTERS[selectedCity] || CITY_CENTERS["Douala"]

  // Group listings by coordinates (to handle multiple listings in same neighborhood)
  const listingsWithCoords = listings
    .map((listing) => {
      const coords = getNeighborhoodCoordinates(listing.neighborhood, listing.city)
      if (!coords) return null
      
      // Add small random offset to prevent exact overlapping
      const offset = () => (Math.random() - 0.5) * 0.002
      return {
        ...listing,
        lat: coords.lat + offset(),
        lng: coords.lng + offset(),
      }
    })
    .filter(Boolean) as (ListingMapItem & { lat: number; lng: number })[]

  return (
    <div className={`relative ${className}`}>
      <MapContainer
        center={[cityCenter.lat, cityCenter.lng]}
        zoom={cityCenter.zoom}
        scrollWheelZoom={true}
        className="w-full h-full min-h-[300px] rounded-xl z-0"
        style={{ minHeight: "300px" }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        
        {customIcon && listingsWithCoords.map((listing) => (
          <Marker
            key={listing.id}
            position={[listing.lat, listing.lng]}
            icon={customIcon}
            eventHandlers={{
              click: () => onListingClick?.(listing.id),
            }}
          >
            <Popup>
              <div className="min-w-[180px]">
                {listing.photoUrl && (
                  <img
                    src={listing.photoUrl}
                    alt={listing.title}
                    className="w-full h-20 object-cover rounded-md mb-2"
                  />
                )}
                <h3 className="font-semibold text-sm line-clamp-1">{listing.title}</h3>
                <p className="text-xs text-slate-500">{listing.neighborhood}, {listing.city}</p>
                <p className="text-sm font-bold text-blue-600 mt-1">
                  {formatCurrency(listing.rentAmount)}/month
                </p>
                {onListingClick && (
                  <button
                    onClick={() => onListingClick(listing.id)}
                    className="mt-2 w-full py-1.5 bg-blue-600 text-white text-xs rounded-md hover:bg-blue-700"
                  >
                    View Details
                  </button>
                )}
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
      
      {/* Listing count overlay */}
      <div className="absolute top-3 left-3 bg-white/95 backdrop-blur-sm px-3 py-1.5 rounded-full shadow-md z-[1000]">
        <span className="text-sm font-medium text-slate-700">
          {listingsWithCoords.length} listing{listingsWithCoords.length !== 1 ? "s" : ""} on map
        </span>
      </div>
    </div>
  )
}
