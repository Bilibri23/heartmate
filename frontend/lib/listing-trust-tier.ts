import { cn } from "@/lib/utils"

/** Accent background for trust tier chips on listing media overlays. */
export function trustTierBadgeClassName(tier: string | null | undefined): string | null {
  switch (tier) {
    case "HIGH":
      return "bg-emerald-500/90"
    case "VERIFIED":
      return "bg-sky-600/90"
    case "REVIEWED":
      return "bg-violet-600/90"
    default:
      return null
  }
}

export function trustTierChipClassName(
  tier: string | null | undefined,
  size: "card" | "detail" | "reel" = "card",
) {
  const bg = trustTierBadgeClassName(tier)
  if (!bg) return null
  return cn(
    "flex items-center gap-1 rounded-full backdrop-blur-sm font-bold text-white",
    bg,
    size === "card" && "px-2.5 py-1 text-[10px]",
    size === "detail" && "px-2.5 py-1 text-xs font-medium",
    size === "reel" && "px-3 py-1 text-xs font-bold",
  )
}
