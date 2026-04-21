"use client"

import { useEffect, useState } from "react"
import Image from "next/image"
import Link from "next/link"
import api from "@/lib/api"
import { labelDiscoveryReasons } from "@/lib/discovery-reason-labels"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"
import { useLanguage } from "@/context/language-context"

export interface SimilarListingItem {
  listingId: string
  title: string
  rentAmount: number
  city: string
  neighborhood?: string | null
  primaryPhotoUrl?: string | null
  reasonCodes?: string[] | null
  bedrooms?: number | null
}

interface SimilarListingsRailProps {
  seedListingId: string
  purpose?: "discover" | "comps"
  userId?: string | null
  title: string
  lang: "en" | "fr"
  className?: string
}

export function SimilarListingsRail({
  seedListingId,
  purpose = "discover",
  userId,
  title,
  lang,
  className,
}: SimilarListingsRailProps) {
  const { formatCurrency } = useLanguage()
  const [items, setItems] = useState<SimilarListingItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      setLoading(true)
      try {
        const res = await api.get(`/listings/${seedListingId}/similar`, {
          params: {
            purpose,
            limit: 12,
            ...(userId ? { userId } : {}),
          },
        })
        const raw = Array.isArray(res.data) ? (res.data as Record<string, unknown>[]) : []
        if (cancelled) return
        setItems(
          raw.map((row: Record<string, unknown>) => ({
            listingId: String(row.listingId ?? row.id ?? ""),
            title: String(row.title ?? ""),
            rentAmount: Number(row.rentAmount ?? 0),
            city: String(row.city ?? ""),
            neighborhood: (row.neighborhood as string) ?? null,
            primaryPhotoUrl: (row.primaryPhotoUrl as string) ?? null,
            reasonCodes: (row.reasonCodes as string[]) || [],
            bedrooms: row.bedrooms != null ? Number(row.bedrooms) : null,
          }))
        )
      } catch {
        if (!cancelled) setItems([])
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    if (seedListingId) run()
    return () => {
      cancelled = true
    }
  }, [seedListingId, purpose, userId])

  if (loading) {
    return (
      <section className={cn("space-y-3", className)} aria-busy="true" aria-label={title}>
        <Skeleton className="h-5 w-48" />
        <div className="flex gap-3 overflow-hidden">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-40 w-36 shrink-0 rounded-2xl" />
          ))}
        </div>
      </section>
    )
  }

  if (items.length === 0) return null

  return (
    <section className={cn("space-y-3", className)} aria-label={title}>
      <h2 className="text-lg font-bold text-slate-900 px-1">{title}</h2>
      <div
        role="list"
        className="flex gap-3 overflow-x-auto pb-2 px-1 scrollbar-hide snap-x snap-mandatory motion-reduce:scroll-auto"
        tabIndex={0}
      >
        {items.map((it) => {
          const chips = labelDiscoveryReasons(it.reasonCodes, lang).slice(0, 2)
          return (
            <Link
              role="listitem"
              key={it.listingId}
              href={`/listings/${it.listingId}`}
              className="snap-start shrink-0 w-36 rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm hover:shadow-md hover:border-blue-200 transition-shadow focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-500"
            >
              <div className="relative aspect-[4/3] bg-slate-100">
                {it.primaryPhotoUrl ? (
                  <Image src={it.primaryPhotoUrl} alt={it.title} fill className="object-cover" sizes="144px" />
                ) : (
                  <div className="flex h-full items-center justify-center text-2xl">🏠</div>
                )}
              </div>
              <div className="p-2 space-y-1">
                <p className="text-xs font-semibold text-slate-900 line-clamp-2 leading-tight">{it.title}</p>
                <p className="text-[11px] font-bold text-blue-600">{formatCurrency(it.rentAmount || 0)}</p>
                {chips.length > 0 && (
                  <div className="flex flex-wrap gap-0.5">
                    {chips.map((c) => (
                      <span
                        key={c}
                        className="text-[9px] px-1.5 py-0.5 rounded-md bg-slate-100 text-slate-600 max-w-[6.5rem] truncate"
                      >
                        {c}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </Link>
          )
        })}
      </div>
    </section>
  )
}
