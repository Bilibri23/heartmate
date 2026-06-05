"use client"

import { Button } from "@/components/ui/button"
import { LayoutPreset, ROOM_PREVIEW_PRESETS } from "./types"

export function LayoutPresetSelector({
  activePreset,
  onSelect,
}: {
  activePreset?: string
  onSelect: (preset: LayoutPreset) => void
}) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1">
      {ROOM_PREVIEW_PRESETS.map((preset) => (
        <Button
          key={preset.id}
          type="button"
          variant={activePreset === preset.id ? "default" : "outline"}
          className="shrink-0 rounded-xl"
          onClick={() => onSelect(preset)}
        >
          {preset.label}
        </Button>
      ))}
    </div>
  )
}
