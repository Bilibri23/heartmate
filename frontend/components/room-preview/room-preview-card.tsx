"use client"

import { WandSparkles } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import api from "@/lib/api"
import { RoomPreviewStudio } from "./room-preview-studio"

type RoomPreviewCardProps = {
  listingId: string
  enabled?: boolean | null
  roomPreviewStatus?: string | null
  isUnfurnished?: boolean | null
  photoUrl?: string | null
  hasDimensions: boolean
  isAuthenticated: boolean
}

export function RoomPreviewCard({
  listingId,
  enabled,
  roomPreviewStatus,
  isUnfurnished,
  photoUrl,
  hasDimensions,
  isAuthenticated,
}: RoomPreviewCardProps) {
  const isLive = Boolean(enabled && (roomPreviewStatus === "APPROVED" || !roomPreviewStatus))
  if (!enabled && !isUnfurnished) return null

  return (
    <div className="rounded-2xl border border-sky-100 bg-gradient-to-br from-sky-50 via-white to-cyan-50 p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-sky-600 text-white">
          <WandSparkles className="h-5 w-5" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-bold text-slate-900">Preview this room</h2>
            {isLive && (
              <span className="rounded-full bg-sky-100 px-2 py-0.5 text-xs font-semibold text-sky-700">
                Room Preview live
              </span>
            )}
            {enabled && roomPreviewStatus === "PENDING_REVIEW" && (
              <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-700">
                Preview under review
              </span>
            )}
          </div>
          <p className="mt-1 text-sm leading-relaxed text-slate-600">
            See how a bed, desk, sofa, or wardrobe could fit before you apply.
          </p>
          {!enabled && isUnfurnished && (
            <p className="mt-2 text-xs text-slate-500">
              This listing is unfurnished. Room Preview helps you imagine how your furniture could fit.
            </p>
          )}
        </div>
      </div>

      <Sheet>
        <SheetTrigger asChild>
          <Button
            type="button"
            className="mt-4 w-full rounded-xl bg-sky-600 hover:bg-sky-700"
            disabled={!isLive}
            onClick={() => {
              if (isLive) api.post(`/listings/${listingId}/room-preview/opened`).catch(() => {})
            }}
          >
            Try Room Preview
          </Button>
        </SheetTrigger>
        <SheetContent side="bottom" className="h-[92vh] overflow-y-auto rounded-t-3xl">
          <SheetHeader>
            <SheetTitle>Room Preview Studio</SheetTitle>
          </SheetHeader>
          <div className="mt-4">
            <RoomPreviewStudio
              listingId={listingId}
              photoUrl={photoUrl}
              hasDimensions={hasDimensions}
              isAuthenticated={isAuthenticated}
            />
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}
