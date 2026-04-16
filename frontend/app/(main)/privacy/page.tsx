"use client"

import { MobileHeader } from "@/components/layout/mobile-header"

export default function PrivacyPolicyPage() {
  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Privacy Policy" showBack />
      <article className="p-4 pb-24 max-w-lg mx-auto w-full space-y-4 text-sm text-slate-700 leading-relaxed">
        <p className="text-xs text-slate-500">Last updated: April 9, 2026</p>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Overview</h2>
          <p>
            This policy describes how RoomBay handles personal information when you use our apps and websites. It is
            meant to be clear and practical; specifics may evolve as the product grows.
          </p>
        </section>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">What we collect</h2>
          <p>
            We collect information you provide (for example account details, listing content, messages, and support
            requests), technical data needed to run the service (such as device and log data), and information needed
            for verification or compliance where we offer those features.
          </p>
        </section>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Who sees verification documents</h2>
          <p>
            Identity or verification documents you submit for platform checks are handled according to our security and
            compliance practices. Landlords and other users do not receive copies of those documents through RoomBay;
            they may see status indicators (for example that a check was completed) where the product is designed to
            show that.
          </p>
          <p>
            Lease-related documents you choose to share through lease flows are shared as that flow describes—for
            example between the parties to that lease—not as a blanket disclosure of all verification materials.
          </p>
        </section>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">How we use data</h2>
          <p>
            We use data to operate and improve RoomBay, personalize discovery where applicable, keep accounts safe,
            communicate with you, meet legal obligations, and investigate abuse.
          </p>
        </section>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Retention & choices</h2>
          <p>
            We retain information as long as needed for the purposes above and as required by law. You may access or
            update certain information in your account settings. For other requests, contact us through Help &amp;
            Support.
          </p>
        </section>
      </article>
    </div>
  )
}
