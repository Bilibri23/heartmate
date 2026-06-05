"use client"

import Image from "next/image"
import { useEffect, useMemo, useState } from "react"
import { toast } from "sonner"
import api from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { FurnitureOverlay } from "./furniture-overlay"
import { FurniturePalette } from "./furniture-palette"
import { LayoutPresetSelector } from "./layout-preset-selector"
import { RoomPreviewControls } from "./room-preview-controls"
import { FurnitureItem, FurnitureType, LayoutPreset, RoomPreviewLayout } from "./types"

type RoomPreviewStudioProps = {
  listingId: string
  photoUrl?: string | null
  hasDimensions: boolean
  isAuthenticated: boolean
}

export function RoomPreviewStudio({ listingId, photoUrl, hasDimensions, isAuthenticated }: RoomPreviewStudioProps) {
  const [items, setItems] = useState<FurnitureItem[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [activePreset, setActivePreset] = useState<string | undefined>()
  const [savedLayouts, setSavedLayouts] = useState<RoomPreviewLayout[]>([])
  const [layoutName, setLayoutName] = useState("My room preview")
  const [isSaving, setIsSaving] = useState(false)

  const selectedItem = useMemo(() => items.find((item) => item.id === selectedId), [items, selectedId])

  useEffect(() => {
    if (!listingId || !isAuthenticated) return
    api.get(`/listings/${listingId}/room-preview-layouts`)
      .then((res) => setSavedLayouts(res.data || []))
      .catch(() => {})
  }, [listingId, isAuthenticated])

  if (!photoUrl) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-slate-50 p-6 text-center">
        <p className="font-semibold text-slate-900">No preview photo yet</p>
        <p className="mx-auto mt-2 max-w-sm text-sm leading-relaxed text-slate-500">
          This listing is ready for Room Preview, but the landlord still needs to select a clear room photo.
        </p>
      </div>
    )
  }

  const updateItem = (next: FurnitureItem) => {
    setItems((current) => current.map((item) => (item.id === next.id ? next : item)))
  }

  const addItem = (type: FurnitureType) => {
    const item: FurnitureItem = {
      id: crypto.randomUUID(),
      type,
      x: 0.38,
      y: 0.52,
      width: type === "BED" || type === "SOFA" ? 0.28 : 0.18,
      height: type === "BED" ? 0.18 : 0.12,
      rotation: 0,
    }
    setItems((current) => [...current, item])
    setSelectedId(item.id)
    api.post(`/listings/${listingId}/room-preview/events`, { eventType: "room_preview_item_added" }).catch(() => {})
  }

  const applyPreset = (preset: LayoutPreset) => {
    setActivePreset(preset.id)
    setSelectedId(null)
    setItems(preset.items.map((item) => ({ ...item, id: crypto.randomUUID() })))
    api.post(`/listings/${listingId}/room-preview/events`, { eventType: "room_preview_preset_selected" }).catch(() => {})
  }

  const resizeSelected = (delta: number) => {
    if (!selectedItem) return
    updateItem({
      ...selectedItem,
      width: clamp(selectedItem.width + delta, 0.06, 0.8),
      height: clamp(selectedItem.height + delta * 0.65, 0.05, 0.6),
    })
  }

  const rotateSelected = (delta: number) => {
    if (!selectedItem) return
    updateItem({ ...selectedItem, rotation: selectedItem.rotation + delta })
  }

  const removeSelected = () => {
    if (!selectedId) return
    setItems((current) => current.filter((item) => item.id !== selectedId))
    setSelectedId(null)
  }

  const saveLayout = async () => {
    if (!isAuthenticated) {
      toast.error("Sign in to save this Room Preview to your account.")
      return
    }
    setIsSaving(true)
    try {
      const res = await api.post(`/listings/${listingId}/room-preview-layouts`, {
        layoutName,
        items: items.map(({ id: _id, ...item }) => item),
      })
      setSavedLayouts((current) => [res.data, ...current.filter((layout) => layout.id !== res.data.id)])
      toast.success("Room Preview saved.")
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Could not save Room Preview.")
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="text-sm font-semibold text-slate-900">Room Preview Studio</p>
          <p className="text-xs text-slate-500">
            {hasDimensions ? "Uses landlord-provided dimensions" : "Approximate layout only"}
          </p>
        </div>
        <span className="rounded-full bg-sky-50 px-3 py-1 text-xs font-medium text-sky-700">
          AR-ready experience coming soon
        </span>
      </div>

      <div className="relative aspect-[4/3] overflow-hidden rounded-2xl border border-slate-200 bg-slate-100">
        <Image src={photoUrl} alt="Room Preview photo" fill className="object-cover" sizes="(max-width: 768px) 100vw, 768px" />
        <div className="absolute inset-0 bg-black/5" />
        {items.map((item) => (
          <FurnitureOverlay
            key={item.id}
            item={item}
            selected={item.id === selectedId}
            onSelect={() => setSelectedId(item.id)}
            onChange={updateItem}
          />
        ))}
      </div>

      <LayoutPresetSelector activePreset={activePreset} onSelect={applyPreset} />
      <FurniturePalette onAdd={addItem} />
      <RoomPreviewControls
        hasSelection={Boolean(selectedItem)}
        isSaving={isSaving}
        onResize={resizeSelected}
        onRotate={rotateSelected}
        onRemove={removeSelected}
        onReset={() => {
          setItems([])
          setSelectedId(null)
          setActivePreset(undefined)
        }}
        onSave={saveLayout}
      />

      <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
        <Input value={layoutName} onChange={(e) => setLayoutName(e.target.value)} className="rounded-xl" placeholder="Layout name" />
        <Button type="button" variant="outline" className="rounded-xl" onClick={saveLayout} disabled={isSaving}>
          Save
        </Button>
      </div>

      {savedLayouts.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Saved layouts</p>
          <div className="flex gap-2 overflow-x-auto pb-1">
            {savedLayouts.map((layout) => (
              <Button
                key={layout.id}
                type="button"
                variant="outline"
                className="shrink-0 rounded-xl"
                onClick={() => {
                  setLayoutName(layout.layoutName)
                  setItems((layout.items || []).map((item) => ({ ...item, id: crypto.randomUUID() })))
                  setSelectedId(null)
                }}
              >
                {layout.layoutName}
              </Button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value))
}
