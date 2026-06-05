"use client"

import type { ElementType } from "react"
import { Armchair, BedDouble, BriefcaseBusiness, LampDesk, Monitor, Sofa, Table2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { FURNITURE_LABELS, FurnitureType } from "./types"

const ITEMS: { type: FurnitureType; icon: ElementType }[] = [
  { type: "BED", icon: BedDouble },
  { type: "DESK", icon: LampDesk },
  { type: "SOFA", icon: Sofa },
  { type: "WARDROBE", icon: BriefcaseBusiness },
  { type: "TABLE", icon: Table2 },
  { type: "CHAIR", icon: Armchair },
  { type: "TV_STAND", icon: Monitor },
]

export function FurniturePalette({ onAdd }: { onAdd: (type: FurnitureType) => void }) {
  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
      {ITEMS.map(({ type, icon: Icon }) => (
        <Button key={type} type="button" variant="outline" className="h-11 justify-start rounded-xl" onClick={() => onAdd(type)}>
          <Icon className="mr-2 h-4 w-4 text-sky-600" />
          {FURNITURE_LABELS[type]}
        </Button>
      ))}
    </div>
  )
}
