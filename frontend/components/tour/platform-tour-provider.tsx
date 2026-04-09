"use client"

import { useEffect, useRef } from "react"
import { usePathname, useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import type { Tour } from "shepherd.js"
import "shepherd.js/dist/css/shepherd.css"
import { createLandlordTour, createTenantTour } from "@/lib/tour/build-tours"

/** Bump when adding tour steps (e.g. AI assistant) so users see the new flow once. */
const STORAGE_TENANT = "roombay_shepherd_tenant_v2"
const STORAGE_LANDLORD = "roombay_shepherd_landlord_v2"

/**
 * Shepherd.js runs at the root so route changes (Search → Profile → Applications) keep one tour instance.
 * Replaces the previous Driver.js spotlight.
 */
export function PlatformTourProvider({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const { user, isLoading } = useAuth()
  const tourRef = useRef<Tour | null>(null)
  const startedRef = useRef(false)

  useEffect(() => {
    if (typeof window === "undefined" || isLoading || !user?.id) return
    if (startedRef.current) return
    if (user.role === "ADMIN") return

    const done = (key: string) => {
      try {
        localStorage.setItem(key, "1")
      } catch {
        /* private mode */
      }
      tourRef.current = null
    }

    if (user.role === "LANDLORD") {
      if (pathname !== "/landlord") return
      try {
        if (localStorage.getItem(STORAGE_LANDLORD)) return
      } catch {
        return
      }
      startedRef.current = true
      const tour = createLandlordTour(router)
      tour.on("complete", () => done(STORAGE_LANDLORD))
      tour.on("cancel", () => done(STORAGE_LANDLORD))
      tourRef.current = tour
      void tour.start()
      return
    }

    // Tenant / student
    if (pathname !== "/for-you") return
    try {
      if (localStorage.getItem(STORAGE_TENANT)) return
    } catch {
      return
    }
    startedRef.current = true
    const tour = createTenantTour(router)
    tour.on("complete", () => done(STORAGE_TENANT))
    tour.on("cancel", () => done(STORAGE_TENANT))
    tourRef.current = tour
    void tour.start()
  }, [pathname, router, user?.id, user?.role, isLoading])

  useEffect(() => {
    return () => {
      try {
        tourRef.current?.cancel()
      } catch {
        /* noop */
      }
    }
  }, [])

  return <>{children}</>
}
