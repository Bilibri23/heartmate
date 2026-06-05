import type { ReactNode } from "react"
import Link from "next/link"
import { HelpCircle } from "lucide-react"
import { Button } from "@/components/ui/button"

type GuidedEmptyStateProps = {
  icon: ReactNode
  title: string
  body: string
  primaryHref?: string
  primaryLabel?: string
  helpHref?: string
  helpLabel?: string
}

export function GuidedEmptyState({
  icon,
  title,
  body,
  primaryHref,
  primaryLabel,
  helpHref = "/help",
  helpLabel = "Get help",
}: GuidedEmptyStateProps) {
  return (
    <div className="rounded-2xl bg-white px-4 py-10 text-center shadow-sm">
      <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 text-slate-500">
        {icon}
      </div>
      <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
      <p className="mx-auto mt-2 max-w-sm text-sm leading-relaxed text-slate-500">{body}</p>
      <div className="mt-5 flex flex-col justify-center gap-2 sm:flex-row">
        {primaryHref && primaryLabel && (
          <Button asChild className="rounded-xl">
            <Link href={primaryHref}>{primaryLabel}</Link>
          </Button>
        )}
        <Button asChild variant="outline" className="rounded-xl">
          <Link href={helpHref}>
            <HelpCircle className="mr-2 h-4 w-4" />
            {helpLabel}
          </Link>
        </Button>
      </div>
    </div>
  )
}
