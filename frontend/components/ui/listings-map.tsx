"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import L from "leaflet"
import "leaflet.markercluster"
import "leaflet.markercluster/dist/MarkerCluster.css"
import "leaflet.markercluster/dist/MarkerCluster.Default.css"
import { MapContainer, TileLayer, useMap, CircleMarker, Tooltip, Marker, Popup } from "react-leaflet"
import { getNeighborhoodCoordinates, getCityCenterCoordinates, CITY_CENTERS } from "@/lib/neighborhoods"
import { distanceKm, type Landmark } from "@/lib/landmarks"

export interface ListingMapItem {
  id: string
  title: string
  neighborhood: string
  city: string
  rentAmount: number
  latitude?: number | string | null
  longitude?: number | string | null
  photoUrl?: string | null
}

export interface AreaStat {
  city: string
  neighborhood: string
  listingCount: number
  avgRent?: number | null
  minRent?: number | null
  maxRent?: number | null
}

export interface MapBounds {
  minLat: number
  maxLat: number
  minLng: number
  maxLng: number
}

interface ListingsMapProps {
  listings: ListingMapItem[]
  selectedCity?: string
  onListingClick?: (listingId: string) => void
  onSearchArea?: (bounds: MapBounds) => void
  areaStats?: AreaStat[]
  campus?: Landmark | null
  className?: string
  formatCurrency: (amount: number) => string
}

function toCoordinate(value: number | string | null | undefined): number | null {
  if (typeof value === "number") return Number.isFinite(value) ? value : null
  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }
  return null
}

function stableOffset(seed: string, axis: "lat" | "lng"): number {
  let hash = axis === "lat" ? 17 : 31
  for (let i = 0; i < seed.length; i += 1) {
    hash = (hash * 33 + seed.charCodeAt(i)) % 997
  }
  return ((hash / 996) - 0.5) * 0.002
}

// Green (cheaper) → red (pricier) scale across the visible neighborhoods.
function rentColor(value: number, min: number, max: number): string {
  if (!Number.isFinite(value) || max <= min) return "#2563eb"
  const t = Math.max(0, Math.min(1, (value - min) / (max - min)))
  const hue = (1 - t) * 120 // 120=green → 0=red
  return `hsl(${hue}, 75%, 45%)`
}

type PositionedListing = ListingMapItem & { lat: number; lng: number }

/** Imperative marker-cluster layer (react-leaflet has no built-in clustering). */
function ClusterLayer({
  listings,
  icon,
  campus,
  onListingClick,
  formatCurrency,
}: {
  listings: PositionedListing[]
  icon: L.Icon
  campus?: Landmark | null
  onListingClick?: (id: string) => void
  formatCurrency: (amount: number) => string
}) {
  const map = useMap()

  useEffect(() => {
    // markerClusterGroup is added to L by the leaflet.markercluster plugin (imported above).
    const group = (L as unknown as {
      markerClusterGroup: (opts?: Record<string, unknown>) => L.LayerGroup
    }).markerClusterGroup({ chunkedLoading: true, maxClusterRadius: 50, showCoverageOnHover: false })

    listings.forEach((listing) => {
      const marker = L.marker([listing.lat, listing.lng], { icon })
      const el = document.createElement("div")
      el.className = "min-w-[180px]"
      const dist = campus ? distanceKm(campus.lat, campus.lng, listing.lat, listing.lng) : null
      el.innerHTML = `
        ${listing.photoUrl ? `<img src="${listing.photoUrl}" alt="" class="w-full h-20 object-cover rounded-md mb-2" />` : ""}
        <h3 class="font-semibold text-sm" style="margin:0">${listing.title}</h3>
        <p class="text-xs" style="color:#64748b;margin:2px 0">${listing.neighborhood || ""}${listing.neighborhood ? ", " : ""}${listing.city || ""}</p>
        <p class="text-sm font-bold" style="color:#2563eb;margin:4px 0">${formatCurrency(listing.rentAmount)}/month</p>
        ${dist != null ? `<p class="text-xs" style="color:#0f766e;margin:2px 0">${dist.toFixed(1)} km from ${campus!.name.split("(")[0].trim()}</p>` : ""}
        <button type="button" data-view class="mt-1 w-full py-1.5 text-white text-xs rounded-md" style="background:#2563eb;border:none;cursor:pointer">View Details</button>
      `
      const btn = el.querySelector("[data-view]")
      if (btn && onListingClick) btn.addEventListener("click", () => onListingClick(listing.id))
      marker.bindPopup(el)
      group.addLayer(marker)
    })

    map.addLayer(group)
    return () => {
      map.removeLayer(group)
    }
  }, [map, listings, icon, campus, onListingClick, formatCurrency])

  return null
}

export function ListingsMap({
  listings,
  selectedCity = "Douala",
  onListingClick,
  onSearchArea,
  areaStats,
  campus,
  className = "",
  formatCurrency,
}: ListingsMapProps) {
  const [isMounted, setIsMounted] = useState(false)
  const [showHeatmap, setShowHeatmap] = useState(false)
  const mapRef = useRef<L.Map | null>(null)

  useEffect(() => {
    setIsMounted(true)
  }, [])

  const customIcon = useMemo(
    () =>
      new L.Icon({
        iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png",
        iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png",
        shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png",
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41],
      }),
    []
  )

  const cityCenter = getCityCenterCoordinates(selectedCity) || CITY_CENTERS["Douala"]
  const initialCenter = campus ? { lat: campus.lat, lng: campus.lng, zoom: 14 } : cityCenter

  const positioned: PositionedListing[] = useMemo(
    () =>
      listings.map((listing) => {
        const explicitLat = toCoordinate(listing.latitude)
        const explicitLng = toCoordinate(listing.longitude)
        const coords =
          explicitLat !== null && explicitLng !== null
            ? { lat: explicitLat, lng: explicitLng }
            : getNeighborhoodCoordinates(listing.neighborhood, listing.city) ||
              getCityCenterCoordinates(listing.city) ||
              cityCenter
        return {
          ...listing,
          lat: coords.lat + stableOffset(listing.id, "lat"),
          lng: coords.lng + stableOffset(listing.id, "lng"),
        }
      }),
    [listings, cityCenter]
  )

  // Heatmap points: place each area's avg rent at its neighborhood centroid.
  const heatPoints = useMemo(() => {
    if (!areaStats?.length) return []
    const pts = areaStats
      .map((s) => {
        const coords = getNeighborhoodCoordinates(s.neighborhood, s.city)
        if (!coords || s.avgRent == null) return null
        return { ...s, lat: coords.lat, lng: coords.lng, avgRent: s.avgRent }
      })
      .filter(Boolean) as (AreaStat & { lat: number; lng: number; avgRent: number })[]
    return pts
  }, [areaStats])

  const rentBounds = useMemo(() => {
    if (!heatPoints.length) return { min: 0, max: 0 }
    const vals = heatPoints.map((p) => p.avgRent)
    return { min: Math.min(...vals), max: Math.max(...vals) }
  }, [heatPoints])

  if (!isMounted) {
    return (
      <div className={`relative ${className}`}>
        <div className="w-full h-full min-h-[300px] rounded-xl bg-slate-100 animate-pulse" />
      </div>
    )
  }

  const handleSearchArea = () => {
    const map = mapRef.current
    if (!map || !onSearchArea) return
    const b = map.getBounds()
    onSearchArea({
      minLat: b.getSouth(),
      maxLat: b.getNorth(),
      minLng: b.getWest(),
      maxLng: b.getEast(),
    })
  }

  return (
    <div className={`relative ${className}`}>
      <MapContainer
        center={[initialCenter.lat, initialCenter.lng]}
        zoom={initialCenter.zoom}
        scrollWheelZoom={true}
        className="w-full h-full min-h-[300px] rounded-xl z-0"
        style={{ minHeight: "300px" }}
        ref={mapRef}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {!showHeatmap && (
          <ClusterLayer
            listings={positioned}
            icon={customIcon}
            campus={campus}
            onListingClick={onListingClick}
            formatCurrency={formatCurrency}
          />
        )}

        {showHeatmap &&
          heatPoints.map((p) => (
            <CircleMarker
              key={`${p.city}-${p.neighborhood}`}
              center={[p.lat, p.lng]}
              radius={Math.min(34, 8 + p.listingCount * 2)}
              pathOptions={{
                color: rentColor(p.avgRent, rentBounds.min, rentBounds.max),
                fillColor: rentColor(p.avgRent, rentBounds.min, rentBounds.max),
                fillOpacity: 0.45,
                weight: 1,
              }}
            >
              <Tooltip direction="top">
                <div className="text-xs">
                  <div className="font-semibold">{p.neighborhood}</div>
                  <div>Avg {formatCurrency(Math.round(p.avgRent))}/mo</div>
                  {p.minRent != null && p.maxRent != null && (
                    <div className="text-slate-500">
                      {formatCurrency(p.minRent)}–{formatCurrency(p.maxRent)}
                    </div>
                  )}
                  <div className="text-slate-500">{p.listingCount} listing{p.listingCount !== 1 ? "s" : ""}</div>
                </div>
              </Tooltip>
            </CircleMarker>
          ))}

        {campus && (
          <Marker
            position={[campus.lat, campus.lng]}
            icon={L.divIcon({
              className: "",
              html: `<div style="background:#0f766e;color:#fff;border-radius:9999px;padding:2px 8px;font-size:11px;font-weight:600;white-space:nowrap;box-shadow:0 1px 4px rgba(0,0,0,.3)">🎓 ${campus.name.split("(")[0].trim()}</div>`,
              iconAnchor: [0, 0],
            })}
          >
            <Popup>{campus.name}</Popup>
          </Marker>
        )}
      </MapContainer>

      {/* Top-left: count + heatmap toggle */}
      <div className="absolute top-3 left-3 z-[1000] flex flex-col gap-2">
        <span className="bg-white/95 backdrop-blur-sm px-3 py-1.5 rounded-full shadow-md text-sm font-medium text-slate-700">
          {showHeatmap
            ? `${heatPoints.length} area${heatPoints.length !== 1 ? "s" : ""}`
            : `${positioned.length} listing${positioned.length !== 1 ? "s" : ""} on map`}
        </span>
        {(areaStats?.length ?? 0) > 0 && (
          <button
            type="button"
            onClick={() => setShowHeatmap((v) => !v)}
            className={`px-3 py-1.5 rounded-full shadow-md text-xs font-medium ${
              showHeatmap ? "bg-blue-600 text-white" : "bg-white/95 text-slate-700"
            }`}
          >
            {showHeatmap ? "Show listings" : "Rent heatmap"}
          </button>
        )}
      </div>

      {/* Top-center: search this area */}
      {onSearchArea && !showHeatmap && (
        <button
          type="button"
          onClick={handleSearchArea}
          className="absolute top-3 left-1/2 -translate-x-1/2 z-[1000] px-4 py-1.5 rounded-full shadow-md bg-blue-600 text-white text-xs font-semibold hover:bg-blue-700"
        >
          Search this area
        </button>
      )}
    </div>
  )
}
