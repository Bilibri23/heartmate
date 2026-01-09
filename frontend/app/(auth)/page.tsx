"use client"

import Link from "next/link"
import { Search, Building2, CheckCircle2 } from "lucide-react"

export default function WelcomePage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-b from-slate-50 to-slate-100 p-6">
      <div className="w-full max-w-4xl">
        <div className="mb-12 text-center">
          <h1 className="text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">
            Welcome to Roombuddy
          </h1>
          <p className="mt-4 text-lg text-slate-600">
            Choose your journey to get started
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-2">
          {/* Tenant Card */}
          <div className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-8 shadow-sm transition-all hover:shadow-lg hover:border-blue-200">
            <div className="mb-6 flex h-14 w-14 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
              <Search className="h-7 w-7" />
            </div>
            
            <h2 className="text-xl font-semibold text-slate-900">
              I&apos;m Looking for a Rental
            </h2>
            <p className="mt-2 text-sm text-slate-500">
              Find your perfect home with AI-powered search and instant applications
            </p>

            <ul className="mt-6 space-y-3">
              <li className="flex items-center gap-2 text-sm text-slate-600">
                <CheckCircle2 className="h-4 w-4 text-blue-500" />
                Personalized property recommendations
              </li>
              <li className="flex items-center gap-2 text-sm text-slate-600">
                <CheckCircle2 className="h-4 w-4 text-blue-500" />
                Instant application submissions
              </li>
              <li className="flex items-center gap-2 text-sm text-slate-600">
                <CheckCircle2 className="h-4 w-4 text-blue-500" />
                Virtual and in-person viewings
              </li>
              <li className="flex items-center gap-2 text-sm text-slate-600">
                <CheckCircle2 className="h-4 w-4 text-blue-500" />
                24/7 tenant support
              </li>
            </ul>

            <Link
              href="/register?role=STUDENT"
              className="mt-8 flex w-full items-center justify-center rounded-xl bg-slate-900 py-3.5 text-sm font-medium text-white transition-colors hover:bg-slate-800"
            >
              Find My Home
            </Link>
          </div>

          {/* Landlord Card */}
          <div className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-8 shadow-sm transition-all hover:shadow-lg hover:border-emerald-200">
            <div className="mb-6 flex h-14 w-14 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
              <Building2 className="h-7 w-7" />
            </div>
            
            <h2 className="text-xl font-semibold text-slate-900">
              I&apos;m a Landlord/Agent
            </h2>
            <p className="mt-2 text-sm text-slate-500">
              List your properties and find reliable tenants with smart matching
            </p>

            <ul className="mt-6 space-y-3">
              <li className="flex items-center gap-2 text-sm text-slate-600">
                <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                Smart tenant matching & screening
              </li>
              <li className="flex items-center gap-2 text-sm text-slate-600">
                <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                Automated application management
              </li>
              <li className="flex items-center gap-2 text-sm text-slate-600">
                <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                Digital lease signing & payments
              </li>
              <li className="flex items-center gap-2 text-sm text-slate-600">
                <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                Property management tools
              </li>
            </ul>

            <Link
              href="/register?role=LANDLORD"
              className="mt-8 flex w-full items-center justify-center rounded-xl bg-emerald-600 py-3.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
            >
              List My Property
            </Link>
          </div>
        </div>

        <p className="mt-8 text-center text-sm text-slate-500">
          Already have an account?{" "}
          <Link href="/login" className="font-medium text-slate-900 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
