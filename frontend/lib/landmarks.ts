// Reference points for "Near campus" proximity search. Coordinates are approximate campus
// centroids; used to drive the existing radius filter and to label listings by distance.

export interface Landmark {
  id: string
  name: string
  city: string
  lat: number
  lng: number
}

export const CAMPUSES: Landmark[] = [
  { id: "ub-buea", name: "University of Buea (Molyko)", city: "Buea", lat: 4.1537, lng: 9.292 },
  { id: "uy1-yaounde", name: "University of Yaoundé I (Ngoa-Ekelle)", city: "Yaoundé", lat: 3.8616, lng: 11.5009 },
  { id: "ucac-yaounde", name: "Catholic University (UCAC), Yaoundé", city: "Yaoundé", lat: 3.873, lng: 11.517 },
  { id: "udo-douala", name: "University of Douala", city: "Douala", lat: 4.051, lng: 9.708 },
  { id: "uba-bamenda", name: "University of Bamenda (Bambili)", city: "Bamenda", lat: 5.993, lng: 10.247 },
  { id: "udc-dschang", name: "University of Dschang", city: "Dschang", lat: 5.445, lng: 10.064 },
]

export function getCampusById(id: string | null | undefined): Landmark | null {
  if (!id) return null
  return CAMPUSES.find((c) => c.id === id) ?? null
}

/** Haversine distance in kilometers between two coordinates. */
export function distanceKm(aLat: number, aLng: number, bLat: number, bLng: number): number {
  const R = 6371
  const dLat = ((bLat - aLat) * Math.PI) / 180
  const dLng = ((bLng - aLng) * Math.PI) / 180
  const lat1 = (aLat * Math.PI) / 180
  const lat2 = (bLat * Math.PI) / 180
  const h =
    Math.sin(dLat / 2) ** 2 + Math.sin(dLng / 2) ** 2 * Math.cos(lat1) * Math.cos(lat2)
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(h)))
}
