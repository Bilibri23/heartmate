import type { ReactNode } from "react"
import Link from "next/link"
import { ArrowRight } from "lucide-react"
import { Button } from "@/components/ui/button"

type NextStepCardProps = {
  title: string
  body: string
  href?: string
  actionLabel?: string
  icon?: ReactNode
}

export function NextStepCard({ title, body, href, actionLabel, icon }: NextStepCardProps) {
  return (
    <div className="rounded-2xl border border-blue-100 bg-blue-50/80 p-4 shadow-sm">
      <div className="flex items-start gap-3">
        {icon && (
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-blue-700 shadow-sm">
            {icon}
          </div>
        )}
        <div className="min-w-0 flex-1">
          <h2 className="text-sm font-semibold text-slate-900">{title}</h2>
          <p className="mt-1 text-sm leading-relaxed text-slate-600">{body}</p>
          {href && actionLabel && (
            <Button asChild size="sm" className="mt-3 h-9 rounded-xl">
              <Link href={href}>
                {actionLabel}
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
