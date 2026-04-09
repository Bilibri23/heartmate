import Shepherd from "shepherd.js"
import type { Tour } from "shepherd.js"
import type { AppRouterInstance } from "next/dist/shared/lib/app-router-context.shared-runtime"
import { navigateAndWait } from "./wait-for-selector"

function backOrExit(this: Tour) {
  const cur = this.getCurrentStep()
  const first = this.steps[0]
  if (cur && first && cur.id === first.id) {
    void this.cancel()
    return
  }
  this.back()
}

function defaults() {
  return [
    { text: "Back", action: backOrExit, secondary: true as const },
    { text: "Next", action: function (this: Tour) { this.next() } },
  ]
}

function donePair() {
  return [
    { text: "Back", action: backOrExit, secondary: true as const },
    { text: "Done", action: function (this: Tour) { void this.complete() } },
  ]
}

export function createTenantTour(router: AppRouterInstance): Tour {
  const tour = new Shepherd.Tour({
    tourName: "roombay-tenant",
    useModalOverlay: true,
    exitOnEsc: true,
    keyboardNavigation: true,
    defaultStepOptions: {
      cancelIcon: { enabled: true },
      scrollTo: { behavior: "smooth", block: "center" },
      classes: "shepherd-roombay-step",
      modalOverlayOpeningPadding: 4,
    },
  })

  const D = defaults()
  const L = donePair()

  tour.addStep({
    id: "tt-for-you",
    title: "Your home feed",
    text: "For You ranks places by how well they match your budget, city, and preferences. Pull down anytime to refresh.",
    attachTo: { element: '[data-tour="tenant-welcome"]', on: "bottom" },
    buttons: [{ text: "Next", action(this: Tour) { this.next() } }],
  })

  tour.addStep({
    id: "tt-filter",
    title: "Refine what you see",
    text: "Tap Filters to open Search with the full filter panel — price, property type, amenities, and more.",
    attachTo: { element: '[data-tour="tenant-filter-bar"]', on: "bottom" },
    buttons: D,
  })

  tour.addStep({
    id: "tt-feed",
    title: "Browse listings",
    text: "Swipe rows sideways. Tap a card for photos, rent, and Apply. Hearts save favorites and tune recommendations.",
    attachTo: { element: '[data-tour="tenant-feed"]', on: "top" },
    buttons: D,
  })

  tour.addStep({
    id: "tt-nav",
    title: "Bottom navigation",
    text: "For You · Search · My Apps · Messages · Profile. Next we’ll open Search (map + video Reels), then Profile, then My Apps.",
    attachTo: { element: '[data-tour="bottom-nav"]', on: "top" },
    buttons: D,
  })

  tour.addStep({
    id: "tt-ai-brain",
    title: "RoomBay Assistant — platform AI",
    text: "Tap the floating chat button (available on most screens). This is the RoomBay platform AI: answers draw on our docs and rental context for Cameroon—not random web guesses. Ask how applying and verification work, what listings mean, leases and payments, Search and Reels, or how to use any screen. Open it whenever you’re stuck.",
    attachTo: { element: '[data-tour="roombay-ai-assistant"]', on: "left" },
    beforeShowPromise() {
      if (window.location.pathname !== "/for-you") {
        return navigateAndWait(router, "/for-you", '[data-tour="roombay-ai-assistant"]')
      }
      return Promise.resolve()
    },
    buttons: D,
  })

  tour.addStep({
    id: "tt-bridge-search",
    title: "Search & video feed",
    text: "We’ll jump to Search so you can see list, map, and the Reels (video) switcher.",
    buttons: [
      {
        text: "Next",
        action(this: Tour) {
          const t = this
          void navigateAndWait(router, "/search", '[data-tour="search-view-modes"]').then(() => t.next()).catch(() => {
            router.push("/search")
            t.next()
          })
        },
      },
    ],
  })

  tour.addStep({
    id: "tt-search-modes",
    title: "List, map, or Reels",
    text: "Sort results (newest, price). Use list or map. When available, the clapper opens a vertical video feed of listings with video tours — like short-video apps.",
    attachTo: { element: '[data-tour="search-view-modes"]', on: "bottom" },
    beforeShowPromise() {
      if (!window.location.pathname.startsWith("/search")) {
        return navigateAndWait(router, "/search", '[data-tour="search-view-modes"]')
      }
      return Promise.resolve()
    },
    buttons: D,
  })

  tour.addStep({
    id: "tt-reels",
    title: "Video Reels",
    text: "Tap the clapper when you see it to browse full-screen. If it’s hidden, it appears once enough listings include video tours — encourage landlords to add tours for better discovery.",
    attachTo: {
      element: () => document.querySelector('[data-tour="search-reels-btn"]') as HTMLElement | null,
      on: "bottom",
    },
    buttons: D,
  })

  tour.addStep({
    id: "tt-bridge-profile",
    title: "Your profile",
    text: "Next: Profile — verification, preferences, and shortcuts to everything account-related.",
    buttons: [
      {
        text: "Next",
        action(this: Tour) {
          const t = this
          void navigateAndWait(router, "/profile", '[data-tour="profile-header"]').then(() => t.next()).catch(() => {
            router.push("/profile")
            t.next()
          })
        },
      },
    ],
  })

  tour.addStep({
    id: "tt-profile-header",
    title: "You & contact info",
    text: "Your name and email live here. Edit profile updates what landlords see.",
    attachTo: { element: '[data-tour="profile-header"]', on: "bottom" },
    beforeShowPromise() {
      if (!window.location.pathname.startsWith("/profile")) {
        return navigateAndWait(router, "/profile", '[data-tour="profile-header"]')
      }
      return Promise.resolve()
    },
    buttons: D,
  })

  tour.addStep({
    id: "tt-profile-verify",
    title: "Trust & verification",
    text: "Verification unlocks applications and payments. When you’re ready, complete the flow from here.",
    attachTo: { element: '[data-tour="profile-verification"]', on: "bottom" },
    buttons: D,
  })

  tour.addStep({
    id: "tt-profile-main-menu",
    title: "Shortcuts menu",
    text: "Edit profile · My Preferences (budget & areas) · Find Roommates · Favorites · Applications · Leases · Payments · Reviews — your housing journey in one place.",
    attachTo: { element: '[data-tour="profile-menu-main"]', on: "top" },
    buttons: D,
  })

  tour.addStep({
    id: "tt-profile-settings",
    title: "Settings & help",
    text: "Language, notifications, and help. Log out lives at the bottom when you’re finished.",
    attachTo: { element: '[data-tour="profile-menu-settings"]', on: "top" },
    buttons: D,
  })

  tour.addStep({
    id: "tt-bridge-apps",
    title: "Application tracking",
    text: "Finally, My Apps — every application you’ve sent, with status filters.",
    buttons: [
      {
        text: "Next",
        action(this: Tour) {
          const t = this
          void navigateAndWait(router, "/applications", '[data-tour="tenant-applications"]').then(() => t.next()).catch(() => {
            router.push("/applications")
            t.next()
          })
        },
      },
    ],
  })

  tour.addStep({
    id: "tt-applications",
    title: "My Apps",
    text: "Filter by pending, accepted, or rejected. Tap a row to reopen the listing and see landlord replies.",
    attachTo: { element: '[data-tour="tenant-applications"]', on: "bottom" },
    beforeShowPromise() {
      if (!window.location.pathname.startsWith("/applications")) {
        return navigateAndWait(router, "/applications", '[data-tour="tenant-applications"]')
      }
      return Promise.resolve()
    },
    buttons: L,
  })

  return tour
}

export function createLandlordTour(router: AppRouterInstance): Tour {
  const tour = new Shepherd.Tour({
    tourName: "roombay-landlord",
    useModalOverlay: true,
    exitOnEsc: true,
    keyboardNavigation: true,
    defaultStepOptions: {
      cancelIcon: { enabled: true },
      scrollTo: { behavior: "smooth", block: "center" },
      classes: "shepherd-roombay-step",
      modalOverlayOpeningPadding: 4,
    },
  })

  const D = defaults()
  const L = donePair()

  tour.addStep({
    id: "ll-stats",
    title: "Landlord dashboard",
    text: "Active listings, pending applications, views, and favorites — quick health checks for your portfolio.",
    attachTo: { element: '[data-tour="landlord-stats"]', on: "bottom" },
    buttons: [{ text: "Next", action(this: Tour) { this.next() } }],
  })

  tour.addStep({
    id: "ll-actions",
    title: "Create & review",
    text: "Post a new listing or open applications to move renters through screening and leases.",
    attachTo: { element: '[data-tour="landlord-quick-actions"]', on: "bottom" },
    buttons: D,
  })

  tour.addStep({
    id: "ll-listings",
    title: "Your properties",
    text: "Edit, mark rented or available, or preview the public page tenants see.",
    attachTo: { element: '[data-tour="landlord-listings"]', on: "top" },
    buttons: D,
  })

  tour.addStep({
    id: "ll-nav",
    title: "How to move around",
    text: "Dashboard · Add (center) · Apps · Messages · Profile. The + button is the fastest new listing path.",
    attachTo: { element: '[data-tour="bottom-nav"]', on: "top" },
    buttons: D,
  })

  tour.addStep({
    id: "ll-ai-brain",
    title: "RoomBay Assistant for landlords",
    text: "Use the blue chat on the right — the same platform AI, tuned for landlords: listings and photos, applications and tenants, verification, payouts, and how renters experience Search and Reels. Answers follow our docs so they match how RoomBay actually works.",
    attachTo: { element: '[data-tour="roombay-ai-assistant"]', on: "left" },
    beforeShowPromise() {
      if (window.location.pathname !== "/landlord") {
        return navigateAndWait(router, "/landlord", '[data-tour="roombay-ai-assistant"]')
      }
      return Promise.resolve()
    },
    buttons: D,
  })

  tour.addStep({
    id: "ll-bridge-search",
    title: "See Search as tenants do",
    text: "We’ll open Search: list, map, and Reels — so you can align photos and video tours with what renters experience.",
    buttons: [
      {
        text: "Next",
        action(this: Tour) {
          const t = this
          void navigateAndWait(router, "/search", '[data-tour="search-view-modes"]').then(() => t.next()).catch(() => {
            router.push("/search")
            t.next()
          })
        },
      },
    ],
  })

  tour.addStep({
    id: "ll-search-modes",
    title: "List · Map · Reels",
    text: "Preview demand in your cities. The clapper tab is the vertical video feed — add video tours to your listings to appear there.",
    attachTo: { element: '[data-tour="search-view-modes"]', on: "bottom" },
    beforeShowPromise() {
      if (!window.location.pathname.startsWith("/search")) {
        return navigateAndWait(router, "/search", '[data-tour="search-view-modes"]')
      }
      return Promise.resolve()
    },
    buttons: D,
  })

  tour.addStep({
    id: "ll-reels",
    title: "Video Reels",
    text: "Tap the clapper when shown to experience the same full-screen feed tenants use. If it’s not visible yet, it unlocks as more listings include video.",
    attachTo: {
      element: () => document.querySelector('[data-tour="search-reels-btn"]') as HTMLElement | null,
      on: "bottom",
    },
    buttons: D,
  })

  tour.addStep({
    id: "ll-bridge-profile",
    title: "Landlord profile",
    text: "Last: your landlord hub — listings, tenants, verification, and payouts.",
    buttons: [
      {
        text: "Next",
        action(this: Tour) {
          const t = this
          void navigateAndWait(router, "/landlord/profile", '[data-tour="ll-profile-hero"]').then(() => t.next()).catch(() => {
            router.push("/landlord/profile")
            t.next()
          })
        },
      },
    ],
  })

  tour.addStep({
    id: "ll-profile-hero",
    title: "Your landlord presence",
    text: "Name, ratings, and listing counts. Use the pencil to refresh photo or details.",
    attachTo: { element: '[data-tour="ll-profile-hero"]', on: "bottom" },
    beforeShowPromise() {
      if (!window.location.pathname.startsWith("/landlord/profile")) {
        return navigateAndWait(router, "/landlord/profile", '[data-tour="ll-profile-hero"]')
      }
      return Promise.resolve()
    },
    buttons: D,
  })

  tour.addStep({
    id: "ll-profile-properties",
    title: "Properties",
    text: "My Listings, Applications, Leases, Tenants, Payments — your end-to-end workflow.",
    attachTo: { element: '[data-tour="ll-profile-section-properties"]', on: "top" },
    buttons: D,
  })

  tour.addStep({
    id: "ll-profile-account",
    title: "Account & growth",
    text: "Verification builds trust. Analytics tracks performance; Notifications and Settings keep you in control.",
    attachTo: { element: '[data-tour="ll-profile-section-account"]', on: "top" },
    buttons: L,
  })

  return tour
}
