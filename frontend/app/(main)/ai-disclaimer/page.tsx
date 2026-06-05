"use client"

import Link from "next/link"
import { MobileHeader } from "@/components/layout/mobile-header"

const points = [
  {
    title: "RoomBay AI is support, not authority",
    body: "RoomBay AI helps explain platform workflows, listings, applications, leases, payments, verification, safety, support, and admin operations. It is not a lawyer, financial adviser, landlord, tenant representative, emergency service, or final decision maker.",
  },
  {
    title: "Answers must stay grounded",
    body: "The assistant should use RoomBay documentation, role-safe platform context, and available account state. If it does not have documentation or enough context, it should say so instead of guessing.",
  },
  {
    title: "Role boundaries apply",
    body: "Tenants, landlords, and admins may receive different answers because they have different permissions. The assistant must not reveal admin-only docs, private verification files, secrets, passwords, tokens, payment credentials, or another user's private data.",
  },
  {
    title: "No automatic approvals or bypasses",
    body: "AI must not approve or reject listings, payments, applications, verification, reports, or leases without the required human/admin workflow. AI must not help bypass identity checks, payment checks, moderation, or safety controls.",
  },
  {
    title: "Verify important decisions",
    body: "Before signing a lease, sending money, visiting a property, sharing documents, or taking legal/financial action, verify the details through the relevant RoomBay screen, the other party, support, and qualified professional advice where needed.",
  },
  {
    title: "Do not submit sensitive secrets",
    body: "Do not put passwords, JWTs, API keys, bank credentials, private verification documents, full payment secrets, or highly sensitive personal information into AI chat unless a specific RoomBay workflow clearly requests it.",
  },
]

export default function AiDisclaimerPage() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <MobileHeader title="AI Disclaimer" showBack />
      <article className="mx-auto w-full max-w-3xl space-y-4 p-4 pb-24 text-sm leading-relaxed text-slate-700">
        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Last updated: June 5, 2026</p>
          <h1 className="mt-2 text-2xl font-bold text-slate-950">RoomBay AI Disclaimer</h1>
          <p className="mt-3 text-slate-600">
            This disclaimer explains the limits of RoomBay AI. It supplements the{" "}
            <Link href="/terms" className="font-medium text-blue-600 underline">
              Terms of Service
            </Link>{" "}
            and{" "}
            <Link href="/privacy" className="font-medium text-blue-600 underline">
              Privacy Policy
            </Link>
            .
          </p>
        </div>
        {points.map((point) => (
          <section key={point.title} className="space-y-3 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
            <h2 className="text-base font-semibold text-slate-900">{point.title}</h2>
            <p>{point.body}</p>
          </section>
        ))}
      </article>
    </div>
  )
}
