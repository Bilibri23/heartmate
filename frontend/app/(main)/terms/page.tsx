"use client"

import Link from "next/link"
import { MobileHeader } from "@/components/layout/mobile-header"

const sections = [
  {
    title: "Agreement",
    body: [
      "By accessing or using RoomBay, you agree to these Terms of Service and the Privacy Policy. If you do not agree, do not use RoomBay.",
      "RoomBay may update these Terms as the product grows. Continued use after an update means you accept the updated Terms.",
    ],
  },
  {
    title: "What RoomBay does",
    body: [
      "RoomBay is a web-based housing marketplace that helps tenants, landlords, roommates, and admins discover listings, communicate, apply, review trust signals, manage leases, upload payment proof, use Room Preview, and get help from RoomBay AI.",
      "RoomBay is not a landlord, tenant, real estate broker, bank, payment processor, insurer, or legal adviser unless a separate written agreement says otherwise. Users are responsible for their own listing, rental, payment, and lease decisions.",
    ],
  },
  {
    title: "Accounts and roles",
    body: [
      "You must provide accurate information, keep your login secure, and use only the role and permissions that apply to you. Tenants, landlords, and admins may have different dashboards, permissions, and responsibilities.",
      "You may not impersonate another person, bypass verification, share accounts, misuse admin or landlord tools, scrape the platform, attack the service, or interfere with another user's experience.",
    ],
  },
  {
    title: "Listings, applications, leases, and payments",
    body: [
      "Landlords are responsible for accurate listings, lawful authority to offer the property, fair communication, truthful photos, safe visits, and honoring commitments they make to tenants.",
      "Tenants are responsible for reviewing listing details, asking questions, visiting or independently verifying the property where appropriate, understanding lease terms, and avoiding payments outside safe, documented workflows.",
      "RoomBay may review, approve, reject, remove, rank, moderate, or flag listings, applications, reports, payment proofs, and user accounts for safety, fraud prevention, quality, or policy reasons.",
      "Payment proof upload does not by itself confirm payment, release funds, or guarantee a lease. Users should verify payment status through the correct parties and available RoomBay workflows.",
    ],
  },
  {
    title: "Verification and trust signals",
    body: [
      "Verification, review, approved, trusted, or similar badges mean RoomBay performed certain checks. They are not guarantees that a person, property, transaction, document, or payment is risk-free.",
      "Users must still use reasonable judgment before visiting a property, signing a lease, sending money, or sharing sensitive information.",
    ],
  },
  {
    title: "Room Preview",
    body: [
      "Room Preview is a lightweight visualization feature that helps tenants imagine furniture layouts over landlord-provided listing photos. It is approximate and not a measurement guarantee.",
      "Landlords are responsible for using normal listing media only and for providing clear, truthful room photos and dimensions where available. Room Preview must not use private verification documents.",
    ],
  },
  {
    title: "AI assistant",
    body: [
      "RoomBay AI is a support and discovery assistant. It may summarize documentation, explain workflows, and help users understand platform actions based on their role.",
      "AI responses may be incomplete, outdated, or wrong. AI does not provide legal, financial, real estate, safety, or payment advice. Do not rely on AI as the only source for lease terms, payment decisions, emergency issues, or legal rights.",
      "AI must not approve, reject, bypass verification, expose private documents, reveal secrets, or invent lease/payment terms. If an AI answer conflicts with official app screens, written agreements, or admin decisions, those official sources control.",
    ],
  },
  {
    title: "User content and prohibited conduct",
    body: [
      "You are responsible for content you submit, including listings, photos, messages, applications, reports, reviews, documents, and AI questions. You confirm that you have the rights needed to submit that content.",
      "Do not submit unlawful, fraudulent, discriminatory, abusive, misleading, unsafe, sexually exploitative, spam, malware, or rights-infringing content. Do not post fake listings, request off-platform payments for fraud, harass users, or abuse reports.",
    ],
  },
  {
    title: "Availability, enforcement, and termination",
    body: [
      "RoomBay may change, pause, limit, or discontinue features. We may suspend or terminate accounts, listings, messages, applications, or access when needed for safety, security, legal compliance, abuse prevention, or Terms enforcement.",
      "The service is provided as available to the extent permitted by law. We do not guarantee uninterrupted access, perfect search results, successful applications, available listings, or any specific housing outcome.",
    ],
  },
  {
    title: "Liability and disputes",
    body: [
      "To the extent permitted by applicable law, RoomBay is not liable for user-provided listings, user conduct, offline meetings, rental disputes, third-party services, failed payments, lost opportunities, or indirect damages.",
      "These Terms are intended for a Cameroon-first web service. If a dispute arises, contact RoomBay support first so we can try to resolve it. Any formal dispute process should follow the applicable law and forum stated by RoomBay or required by law.",
    ],
  },
  {
    title: "Contact",
    body: [
      "Questions about these Terms should be sent through RoomBay Help, Contact, or the support channel published in the app.",
    ],
  },
]

export default function TermsOfServicePage() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <MobileHeader title="Terms of Service" showBack />
      <article className="mx-auto w-full max-w-3xl space-y-4 p-4 pb-24 text-sm leading-relaxed text-slate-700">
        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Last updated: June 5, 2026</p>
          <h1 className="mt-2 text-2xl font-bold text-slate-950">RoomBay Terms of Service</h1>
          <p className="mt-3 text-slate-600">
            These Terms work together with the{" "}
            <Link href="/privacy" className="font-medium text-blue-600 underline">
              Privacy Policy
            </Link>{" "}
            and the{" "}
            <Link href="/ai-disclaimer" className="font-medium text-blue-600 underline">
              AI Disclaimer
            </Link>
            .
          </p>
        </div>
        {sections.map((section) => (
          <section key={section.title} className="space-y-3 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
            <h2 className="text-base font-semibold text-slate-900">{section.title}</h2>
            {section.body.map((paragraph) => (
              <p key={paragraph}>{paragraph}</p>
            ))}
          </section>
        ))}
      </article>
    </div>
  )
}
