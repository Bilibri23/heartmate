"use client"

import Link from "next/link"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { MessageCircle } from "lucide-react"

export default function ContactPage() {
  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Contact Us" showBack />
      <div className="p-4 pb-24 max-w-lg mx-auto w-full space-y-4">
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3 text-sm text-slate-600">
          <p>
            The fastest way to reach us is the Help Center: send a message there and we can reply using the email on
            your account.
          </p>
          <Button asChild className="w-full rounded-xl">
            <Link href="/help" className="flex items-center justify-center gap-2">
              <MessageCircle className="h-5 w-5" />
              Open Help &amp; Support
            </Link>
          </Button>
        </div>
      </div>
    </div>
  )
}
