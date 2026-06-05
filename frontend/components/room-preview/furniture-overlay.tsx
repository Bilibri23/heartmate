"use client"

import { useRef, type PointerEvent as ReactPointerEvent } from "react"
import { FURNITURE_LABELS, FurnitureItem } from "./types"
import { cn } from "@/lib/utils"

type FurnitureOverlayProps = {
  item: FurnitureItem
  selected: boolean
  onSelect: () => void
  onChange: (item: FurnitureItem) => void
}

export function FurnitureOverlay({ item, selected, onSelect, onChange }: FurnitureOverlayProps) {
  const dragRef = useRef<{ startX: number; startY: number; itemX: number; itemY: number } | null>(null)

  const handlePointerDown = (event: ReactPointerEvent<HTMLButtonElement>) => {
    event.preventDefault()
    onSelect()
    const target = event.currentTarget.parentElement
    const bounds = target?.getBoundingClientRect()
    if (!bounds) return
    dragRef.current = { startX: event.clientX, startY: event.clientY, itemX: item.x, itemY: item.y }

    const handleMove = (moveEvent: globalThis.PointerEvent) => {
      if (!dragRef.current) return
      const dx = (moveEvent.clientX - dragRef.current.startX) / bounds.width
      const dy = (moveEvent.clientY - dragRef.current.startY) / bounds.height
      onChange({
        ...item,
        x: clamp(dragRef.current.itemX + dx, 0, 1 - item.width),
        y: clamp(dragRef.current.itemY + dy, 0, 1 - item.height),
      })
    }
    const handleUp = () => {
      dragRef.current = null
      window.removeEventListener("pointermove", handleMove)
      window.removeEventListener("pointerup", handleUp)
    }
    window.addEventListener("pointermove", handleMove)
    window.addEventListener("pointerup", handleUp)
  }

  return (
    <button
      type="button"
      onPointerDown={handlePointerDown}
      className={cn(
        "absolute flex touch-none select-none items-center justify-center rounded-lg border bg-sky-500/55 text-[10px] font-semibold text-white shadow-sm backdrop-blur-sm transition",
        selected ? "border-white ring-2 ring-sky-300" : "border-white/70 hover:bg-sky-500/70"
      )}
      style={{
        left: `${item.x * 100}%`,
        top: `${item.y * 100}%`,
        width: `${item.width * 100}%`,
        height: `${item.height * 100}%`,
        transform: `rotate(${item.rotation}deg)`,
      }}
      aria-label={`Move ${FURNITURE_LABELS[item.type]}`}
    >
      <span className="truncate px-1">{FURNITURE_LABELS[item.type]}</span>
    </button>
  )
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value))
}
