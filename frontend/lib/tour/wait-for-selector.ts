import type { AppRouterInstance } from "next/dist/shared/lib/app-router-context.shared-runtime"

/**
 * Wait until an element exists (e.g. after client navigation).
 */
export function waitForSelector(selector: string, timeoutMs = 12000): Promise<Element> {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(selector)
    if (existing) {
      resolve(existing)
      return
    }
    const start = Date.now()
    const id = window.setInterval(() => {
      const el = document.querySelector(selector)
      if (el) {
        window.clearInterval(id)
        resolve(el)
      } else if (Date.now() - start > timeoutMs) {
        window.clearInterval(id)
        reject(new Error(`waitForSelector: ${selector}`))
      }
    }, 40)
  })
}

export async function navigateAndWait(
  router: AppRouterInstance,
  path: string,
  selector: string,
  settleMs = 500
): Promise<void> {
  await Promise.resolve(router.push(path))
  await new Promise((r) => setTimeout(r, settleMs))
  await waitForSelector(selector)
}
