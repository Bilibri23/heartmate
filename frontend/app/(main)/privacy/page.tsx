"use client"

import { MobileHeader } from "@/components/layout/mobile-header"

const sections = [
  {
    title: "Overview",
    body: [
      "This Privacy Policy explains how RoomBay collects, uses, stores, and shares information when you use our housing marketplace, dashboards, messaging, verification, payment proof, Room Preview, support, and AI assistant features.",
      "RoomBay is currently built for Cameroon. If people outside Cameroon use the service, we may apply additional privacy protections where required by law.",
    ],
  },
  {
    title: "Information we collect",
    body: [
      "Account information such as name, email, phone number, role, password credentials, profile details, preferences, and verification status.",
      "Marketplace information such as listings, photos, room preview setup, applications, leases, payment proof records, reports, reviews, saved homes, saved searches, messages, notifications, and support requests.",
      "Verification and trust information such as government ID or selfie checks, landlord/property verification details, phone and email verification, moderation decisions, and fraud-prevention signals.",
      "Technical information such as device/browser details, IP address, logs, security events, approximate usage activity, analytics events, cookies or similar local storage, and error reports.",
      "AI assistant information such as questions, role context, retrieved help sources, answer quality signals, and no-answer analytics. Do not submit secrets, passwords, private documents, or sensitive personal information into AI chat unless the product specifically asks for it.",
    ],
  },
  {
    title: "How we use information",
    body: [
      "We use information to operate RoomBay, show listings, process applications, support leases and payment proof workflows, provide messaging and notifications, maintain account security, review listings and users, prevent fraud, improve search and recommendations, support Room Preview, respond to support requests, and improve RoomBay AI.",
      "We also use information for admin operations, platform analytics, legal compliance, dispute handling, abuse investigation, backups, debugging, and service reliability.",
    ],
  },
  {
    title: "How information is shared",
    body: [
      "Tenants, landlords, roommates, and admins see only the information needed for their role and workflow. For example, a landlord may see an application, lease status, payment proof status, and messages related to their listing.",
      "Verification documents are not shown to other ordinary users. RoomBay may show trust indicators such as verified, reviewed, or approved status without exposing the underlying private document.",
      "We may share limited information with service providers that help us host, secure, email, store media, monitor errors, process analytics, or provide AI functionality. These providers should only use the data to support RoomBay services.",
      "We may disclose information if required by law, to protect users, to investigate fraud or abuse, to enforce our Terms, or during a business transfer such as a merger or acquisition.",
    ],
  },
  {
    title: "Cookies, local storage, and analytics",
    body: [
      "RoomBay may use cookies, browser storage, tokens, and similar technologies to keep you signed in, remember preferences, support OAuth login, protect sessions, measure usage, and diagnose errors.",
      "If we later add advertising or broader third-party tracking, we should update this policy and add any required consent controls.",
    ],
  },
  {
    title: "Security and retention",
    body: [
      "We use technical and organizational safeguards designed to protect user data, including authentication, authorization, access controls, logging, monitoring, and restricted admin access. No system is perfectly secure.",
      "We keep information for as long as needed to provide RoomBay, comply with legal obligations, resolve disputes, prevent fraud, maintain audit records, and operate backups. Some records may remain after account closure where retention is necessary for safety, legal, financial, or operational reasons.",
    ],
  },
  {
    title: "Your choices and requests",
    body: [
      "You can update certain account, profile, listing, and notification information in the app. You may contact RoomBay support to request access, correction, deletion, or account closure where available and legally appropriate.",
      "We may need to verify your identity before acting on privacy requests. Some requests may be limited if the data is needed for safety, fraud prevention, legal compliance, dispute handling, or legitimate platform operations.",
    ],
  },
  {
    title: "Children",
    body: [
      "RoomBay is not intended for children. Users should be old enough to create legally meaningful housing, payment, application, or lease-related interactions under applicable law.",
    ],
  },
  {
    title: "Updates and contact",
    body: [
      "We may update this Privacy Policy as RoomBay evolves. The latest version will be posted here with a new last updated date.",
      "Questions or privacy requests should be sent through RoomBay Help, Contact, or the support channel published in the app.",
    ],
  },
]

export default function PrivacyPolicyPage() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <MobileHeader title="Privacy Policy" showBack />
      <article className="mx-auto w-full max-w-3xl space-y-4 p-4 pb-24 text-sm leading-relaxed text-slate-700">
        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Last updated: June 5, 2026</p>
          <h1 className="mt-2 text-2xl font-bold text-slate-950">RoomBay Privacy Policy</h1>
          <p className="mt-3 text-slate-600">
            This policy is written for RoomBay's current web platform and Cameroon-first marketplace. It should be
            reviewed by qualified counsel before broad commercial launch or expansion into heavily regulated markets.
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
