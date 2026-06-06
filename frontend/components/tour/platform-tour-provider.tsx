"use client"

import { useEffect, useRef } from "react"
import { usePathname, useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import type { DriveStep } from "driver.js"

/** Bump suffix when adding tour steps so returning users see the new flow once. */
const STORAGE_TENANT = "roombay_driver_tenant_v1"
const STORAGE_LANDLORD = "roombay_driver_landlord_v1"

/**
 * Driver.js product tour — mobile-first replacement for Shepherd.js.
 * Driver renders compact, touch-friendly tooltips that never cover the full screen.
 */
export function PlatformTourProvider({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router = useRouter()
  const { user, isLoading } = useAuth()
  const startedRef = useRef(false)
  const destroyRef = useRef<(() => void) | null>(null)

  useEffect(() => {
    if (typeof window === "undefined" || isLoading || !user?.id) return
    if (startedRef.current) return
    if (user.role === "ADMIN") return

    const alreadySeen = (key: string) => {
      try { return !!localStorage.getItem(key) } catch { return true }
    }
    const markSeen = (key: string) => {
      try { localStorage.setItem(key, "1") } catch { /* private mode */ }
    }

    // Landlord tour — only on /landlord
    if (user.role === "LANDLORD") {
      if (pathname !== "/landlord") return
      if (alreadySeen(STORAGE_LANDLORD)) return
      startedRef.current = true

      void import("driver.js").then(({ driver }) => {
        void import("driver.js/dist/driver.css" as never)
        const steps: DriveStep[] = [
          {
            element: '[data-tour="landlord-stats"]',
            popover: {
              title: "Landlord dashboard",
              description: "Active listings, pending applications, views, and favourites — quick health checks for your portfolio.",
              side: "bottom",
            },
          },
          {
            element: '[data-tour="landlord-quick-actions"]',
            popover: {
              title: "Create & review",
              description: "Post a new listing or open applications to move renters through screening and leases.",
              side: "bottom",
            },
          },
          {
            element: '[data-tour="landlord-listings"]',
            popover: {
              title: "Your properties",
              description: "Edit, mark rented or available, or preview the public page tenants see.",
              side: "top",
            },
          },
          {
            element: '[data-tour="bottom-nav"]',
            popover: {
              title: "Navigation",
              description: "Dashboard · Add (centre) · Apps · Messages · Profile. The + button is the fastest new listing path.",
              side: "top",
            },
          },
          {
            element: '[data-tour="roombay-ai-assistant"]',
            popover: {
              title: "RoomBay AI",
              description: "Tap the chat button for AI help: listings, applications, verification, and how tenants experience Search.",
              side: "left",
            },
          },
        ]

        const driverObj = driver({
          animate: true,
          showProgress: true,
          allowClose: true,
          overlayOpacity: 0.55,
          popoverClass: "roombay-tour-popover",
          nextBtnText: "Next →",
          prevBtnText: "← Back",
          doneBtnText: "Done",
          onDestroyStarted: () => {
            markSeen(STORAGE_LANDLORD)
            driverObj.destroy()
          },
          steps,
        })

        destroyRef.current = () => driverObj.destroy()
        driverObj.drive()
      })
      return
    }

    // Tenant / student tour — only on /for-you
    if (pathname !== "/for-you") return
    if (alreadySeen(STORAGE_TENANT)) return
    startedRef.current = true

    void import("driver.js").then(({ driver }) => {
      void import("driver.js/dist/driver.css" as never)
      const steps: DriveStep[] = [
        {
          element: '[data-tour="tenant-welcome"]',
          popover: {
            title: "Your home feed",
            description: "For You ranks places by how well they match your budget, city, and preferences. Pull down to refresh.",
            side: "bottom",
          },
        },
        {
          element: '[data-tour="tenant-filter-bar"]',
          popover: {
            title: "Refine what you see",
            description: "Tap Filters to open full Search — price, property type, amenities, and more.",
            side: "bottom",
          },
        },
        {
          element: '[data-tour="tenant-feed"]',
          popover: {
            title: "Browse listings",
            description: "Swipe rows sideways. Tap a card for photos and to Apply. Hearts save favourites.",
            side: "top",
          },
        },
        {
          element: '[data-tour="bottom-nav"]',
          popover: {
            title: "Navigation",
            description: "Discover · Search · My Apps · Messages · Profile — everything you need at the bottom.",
            side: "top",
          },
        },
        {
          element: '[data-tour="roombay-ai-assistant"]',
          popover: {
              title: "RoomBay AI",
            description: "Tap the floating chat button for AI help: how to apply, verification, Search, Reels, and more.",
            side: "left",
          },
        },
      ]

      const driverObj = driver({
        animate: true,
        showProgress: true,
        allowClose: true,
        overlayOpacity: 0.55,
        popoverClass: "roombay-tour-popover",
        nextBtnText: "Next →",
        prevBtnText: "← Back",
        doneBtnText: "Done",
        onDestroyStarted: () => {
          markSeen(STORAGE_TENANT)
          driverObj.destroy()
        },
        steps,
      })

      destroyRef.current = () => driverObj.destroy()
      driverObj.drive()
    })
  }, [pathname, user?.id, user?.role, isLoading, router])

  // Clean up on unmount
  useEffect(() => {
    return () => {
      try { destroyRef.current?.() } catch { /* noop */ }
    }
  }, [])

  return <>{children}</>
}
