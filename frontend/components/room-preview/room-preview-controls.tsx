"use client"

import { RotateCcw, RotateCw, Save, Trash2, ZoomIn, ZoomOut } from "lucide-react"
import { Button } from "@/components/ui/button"

type RoomPreviewControlsProps = {
  hasSelection: boolean
  isSaving: boolean
  onResize: (delta: number) => void
  onRotate: (delta: number) => void
  onRemove: () => void
  onReset: () => void
  onSave: () => void
}

export function RoomPreviewControls({
  hasSelection,
  isSaving,
  onResize,
  onRotate,
  onRemove,
  onReset,
  onSave,
}: RoomPreviewControlsProps) {
  return (
    <div className="flex flex-wrap gap-2">
      <Button type="button" size="sm" variant="outline" className="rounded-xl" disabled={!hasSelection} onClick={() => onResize(0.04)}>
        <ZoomIn className="mr-1 h-4 w-4" /> Bigger
      </Button>
      <Button type="button" size="sm" variant="outline" className="rounded-xl" disabled={!hasSelection} onClick={() => onResize(-0.04)}>
        <ZoomOut className="mr-1 h-4 w-4" /> Smaller
      </Button>
      <Button type="button" size="sm" variant="outline" className="rounded-xl" disabled={!hasSelection} onClick={() => onRotate(-15)}>
        <RotateCcw className="mr-1 h-4 w-4" /> Left
      </Button>
      <Button type="button" size="sm" variant="outline" className="rounded-xl" disabled={!hasSelection} onClick={() => onRotate(15)}>
        <RotateCw className="mr-1 h-4 w-4" /> Right
      </Button>
      <Button type="button" size="sm" variant="outline" className="rounded-xl text-red-600 hover:text-red-700" disabled={!hasSelection} onClick={onRemove}>
        <Trash2 className="mr-1 h-4 w-4" /> Remove
      </Button>
      <Button type="button" size="sm" variant="outline" className="rounded-xl" onClick={onReset}>
        Reset
      </Button>
      <Button type="button" size="sm" className="rounded-xl" disabled={isSaving} onClick={onSave}>
        <Save className="mr-1 h-4 w-4" /> {isSaving ? "Saving..." : "Save layout"}
      </Button>
    </div>
  )
}
