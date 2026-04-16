"use client"

import { MobileHeader } from "@/components/layout/mobile-header"

export default function TermsOfServicePage() {
  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Terms of Service" showBack />
      <article className="p-4 pb-24 max-w-lg mx-auto w-full space-y-4 text-sm text-slate-700 leading-relaxed">
        <p className="text-xs text-slate-500">Last updated: April 9, 2026</p>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Agreement</h2>
          <p>
            By using RoomBay, you agree to these terms. If you do not agree, do not use the service. We may update
            these terms; continued use after changes means you accept the revised terms.
          </p>
        </section>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">The platform</h2>
          <p>
            RoomBay helps people discover housing and connect with landlords. We are not a party to rental agreements
            between users. Listings and messages are provided by users; you are responsible for verifying information
            that matters to you before signing a lease or sending money.
          </p>
        </section>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Accounts & conduct</h2>
          <p>
            You must provide accurate information and keep your account secure. Harassment, fraud, illegal content, or
            attempts to circumvent safety or verification features are prohibited and may lead to suspension or removal.
          </p>
        </section>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Disclaimer</h2>
          <p>
            The service is provided &quot;as is&quot; to the extent permitted by law. We do not guarantee availability
            of any listing, outcome of any application, or fitness of a property for your needs.
          </p>
        </section>
        <section className="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Contact</h2>
          <p>
            Questions about these terms: use Help &amp; Support in the app or the contact options we publish in-product.
          </p>
        </section>
      </article>
    </div>
  )
}
