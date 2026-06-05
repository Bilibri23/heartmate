export type FurnitureType = "BED" | "DESK" | "SOFA" | "WARDROBE" | "TABLE" | "CHAIR" | "TV_STAND"

export type FurnitureItem = {
  id: string
  type: FurnitureType
  x: number
  y: number
  width: number
  height: number
  rotation: number
}

export type RoomPreviewLayout = {
  id: string
  listingId: string
  layoutName: string
  items: FurnitureItem[]
  createdAt?: string
  updatedAt?: string
}

export type LayoutPreset = {
  id: string
  label: string
  items: Omit<FurnitureItem, "id">[]
}

export const FURNITURE_LABELS: Record<FurnitureType, string> = {
  BED: "Bed",
  DESK: "Desk",
  SOFA: "Sofa",
  WARDROBE: "Wardrobe",
  TABLE: "Table",
  CHAIR: "Chair",
  TV_STAND: "TV stand",
}

export const ROOM_PREVIEW_PRESETS: LayoutPreset[] = [
  {
    id: "student",
    label: "Student setup",
    items: [
      { type: "BED", x: 0.08, y: 0.58, width: 0.34, height: 0.22, rotation: 0 },
      { type: "DESK", x: 0.58, y: 0.58, width: 0.24, height: 0.14, rotation: 0 },
      { type: "CHAIR", x: 0.64, y: 0.74, width: 0.12, height: 0.10, rotation: 0 },
    ],
  },
  {
    id: "work",
    label: "Work-from-home setup",
    items: [
      { type: "BED", x: 0.08, y: 0.52, width: 0.30, height: 0.20, rotation: 0 },
      { type: "DESK", x: 0.52, y: 0.44, width: 0.30, height: 0.15, rotation: 0 },
      { type: "CHAIR", x: 0.61, y: 0.61, width: 0.12, height: 0.10, rotation: 0 },
      { type: "WARDROBE", x: 0.70, y: 0.18, width: 0.20, height: 0.22, rotation: 0 },
    ],
  },
  {
    id: "minimal",
    label: "Minimal setup",
    items: [
      { type: "BED", x: 0.16, y: 0.58, width: 0.36, height: 0.22, rotation: 0 },
      { type: "TABLE", x: 0.62, y: 0.58, width: 0.18, height: 0.14, rotation: 0 },
      { type: "CHAIR", x: 0.65, y: 0.75, width: 0.12, height: 0.10, rotation: 0 },
    ],
  },
  {
    id: "couple",
    label: "Couple setup",
    items: [
      { type: "BED", x: 0.12, y: 0.54, width: 0.42, height: 0.24, rotation: 0 },
      { type: "WARDROBE", x: 0.66, y: 0.22, width: 0.22, height: 0.24, rotation: 0 },
      { type: "SOFA", x: 0.58, y: 0.62, width: 0.28, height: 0.14, rotation: 0 },
    ],
  },
  {
    id: "studio",
    label: "Studio setup",
    items: [
      { type: "BED", x: 0.08, y: 0.56, width: 0.32, height: 0.20, rotation: 0 },
      { type: "SOFA", x: 0.54, y: 0.55, width: 0.28, height: 0.14, rotation: 0 },
      { type: "TV_STAND", x: 0.58, y: 0.28, width: 0.26, height: 0.10, rotation: 0 },
      { type: "TABLE", x: 0.42, y: 0.72, width: 0.16, height: 0.12, rotation: 0 },
    ],
  },
]
