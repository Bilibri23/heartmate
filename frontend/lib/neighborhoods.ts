// Neighborhood coordinates for major Cameroonian cities
// Coordinates are approximate center points for each neighborhood

export interface NeighborhoodCoordinates {
  name: string
  city: string
  lat: number
  lng: number
}

export const NEIGHBORHOOD_COORDINATES: Record<string, NeighborhoodCoordinates> = {
  // Douala neighborhoods
  "Bonamoussadi": { name: "Bonamoussadi", city: "Douala", lat: 4.0711, lng: 9.7208 },
  "Makepe": { name: "Makepe", city: "Douala", lat: 4.0650, lng: 9.7350 },
  "Logpom": { name: "Logpom", city: "Douala", lat: 4.0580, lng: 9.7420 },
  "Kotto": { name: "Kotto", city: "Douala", lat: 4.0520, lng: 9.7480 },
  "Bonapriso": { name: "Bonapriso", city: "Douala", lat: 4.0167, lng: 9.7000 },
  "Akwa": { name: "Akwa", city: "Douala", lat: 4.0500, lng: 9.7000 },
  "Deido": { name: "Deido", city: "Douala", lat: 4.0600, lng: 9.6900 },
  "Bepanda": { name: "Bepanda", city: "Douala", lat: 4.0450, lng: 9.7350 },
  "Ndokotti": { name: "Ndokotti", city: "Douala", lat: 4.0380, lng: 9.7280 },
  "Bonaberi": { name: "Bonaberi", city: "Douala", lat: 4.0800, lng: 9.6600 },
  "PK8": { name: "PK8", city: "Douala", lat: 4.0200, lng: 9.7600 },
  "PK10": { name: "PK10", city: "Douala", lat: 4.0100, lng: 9.7700 },
  "PK14": { name: "PK14", city: "Douala", lat: 3.9900, lng: 9.7900 },
  "Yassa": { name: "Yassa", city: "Douala", lat: 3.9800, lng: 9.8000 },
  "Logbessou": { name: "Logbessou", city: "Douala", lat: 4.0300, lng: 9.7500 },
  
  // Yaoundé neighborhoods
  "Bastos": { name: "Bastos", city: "Yaoundé", lat: 3.8900, lng: 11.5100 },
  "Nlongkak": { name: "Nlongkak", city: "Yaoundé", lat: 3.8800, lng: 11.5200 },
  "Mvan": { name: "Mvan", city: "Yaoundé", lat: 3.8400, lng: 11.5100 },
  "Nsimeyong": { name: "Nsimeyong", city: "Yaoundé", lat: 3.8300, lng: 11.4900 },
  "Biyem-Assi": { name: "Biyem-Assi", city: "Yaoundé", lat: 3.8350, lng: 11.4800 },
  "Mendong": { name: "Mendong", city: "Yaoundé", lat: 3.8200, lng: 11.4700 },
  "Omnisport": { name: "Omnisport", city: "Yaoundé", lat: 3.8600, lng: 11.5300 },
  "Mimboman": { name: "Mimboman", city: "Yaoundé", lat: 3.8700, lng: 11.5400 },
  "Mvog-Betsi": { name: "Mvog-Betsi", city: "Yaoundé", lat: 3.8500, lng: 11.5000 },
  "Essos": { name: "Essos", city: "Yaoundé", lat: 3.8650, lng: 11.5150 },
  "Ngousso": { name: "Ngousso", city: "Yaoundé", lat: 3.8750, lng: 11.5350 },
  "Odza": { name: "Odza", city: "Yaoundé", lat: 3.8100, lng: 11.5500 },
  "Nkolbisson": { name: "Nkolbisson", city: "Yaoundé", lat: 3.8600, lng: 11.4400 },
  "Soa": { name: "Soa", city: "Yaoundé", lat: 3.9700, lng: 11.5900 },
  
  // Buea neighborhoods
  "Molyko": { name: "Molyko", city: "Buea", lat: 4.1550, lng: 9.2900 },
  "Bonduma": { name: "Bonduma", city: "Buea", lat: 4.1500, lng: 9.2850 },
  "Great Soppo": { name: "Great Soppo", city: "Buea", lat: 4.1600, lng: 9.2800 },
  "Clerks Quarters": { name: "Clerks Quarters", city: "Buea", lat: 4.1580, lng: 9.2750 },
  "Mile 17": { name: "Mile 17", city: "Buea", lat: 4.1400, lng: 9.2700 },
  "Bomaka": { name: "Bomaka", city: "Buea", lat: 4.1450, lng: 9.2950 },
  "Buea Town": { name: "Buea Town", city: "Buea", lat: 4.1550, lng: 9.2400 },
  
  // Bamenda neighborhoods
  "Up Station": { name: "Up Station", city: "Bamenda", lat: 5.9600, lng: 10.1500 },
  "Down Town": { name: "Down Town", city: "Bamenda", lat: 5.9500, lng: 10.1450 },
  "Nkwen": { name: "Nkwen", city: "Bamenda", lat: 5.9800, lng: 10.1600 },
  "Mile 4": { name: "Mile 4", city: "Bamenda", lat: 5.9400, lng: 10.1350 },
  "Mile 3": { name: "Mile 3", city: "Bamenda", lat: 5.9450, lng: 10.1400 },
  "Ntarinkon": { name: "Ntarinkon", city: "Bamenda", lat: 5.9550, lng: 10.1550 },
  "Old Town": { name: "Old Town", city: "Bamenda", lat: 5.9480, lng: 10.1480 },
}

// City center coordinates for default map views
export const CITY_CENTERS: Record<string, { lat: number; lng: number; zoom: number }> = {
  "Douala": { lat: 4.0511, lng: 9.7679, zoom: 12 },
  "Yaoundé": { lat: 3.8480, lng: 11.5021, zoom: 12 },
  "Buea": { lat: 4.1550, lng: 9.2800, zoom: 13 },
  "Bamenda": { lat: 5.9527, lng: 10.1460, zoom: 13 },
}

// Get coordinates for a neighborhood, with fallback to city center
export function getNeighborhoodCoordinates(neighborhood: string, city?: string): { lat: number; lng: number } | null {
  // Try exact match
  // Return null if neighborhood is not provided
  if (!neighborhood) {
    if (city && CITY_CENTERS[city]) {
      return { lat: CITY_CENTERS[city].lat, lng: CITY_CENTERS[city].lng }
    }
    return null
  }
  
  if (NEIGHBORHOOD_COORDINATES[neighborhood]) {
    return {
      lat: NEIGHBORHOOD_COORDINATES[neighborhood].lat,
      lng: NEIGHBORHOOD_COORDINATES[neighborhood].lng,
    }
  }
  
  // Try case-insensitive match
  const normalizedNeighborhood = neighborhood.toLowerCase()
  for (const [key, value] of Object.entries(NEIGHBORHOOD_COORDINATES)) {
    if (key.toLowerCase() === normalizedNeighborhood) {
      return { lat: value.lat, lng: value.lng }
    }
  }
  
  // Fallback to city center if city is provided
  if (city && CITY_CENTERS[city]) {
    return { lat: CITY_CENTERS[city].lat, lng: CITY_CENTERS[city].lng }
  }
  
  return null
}

// Get all neighborhoods for a city
export function getNeighborhoodsForCity(city: string): NeighborhoodCoordinates[] {
  return Object.values(NEIGHBORHOOD_COORDINATES).filter(n => n.city === city)
}
