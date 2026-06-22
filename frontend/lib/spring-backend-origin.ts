/**
 * Spring Boot origin (scheme + host + port), without trailing slash or a trailing `/api`.
 * Prevents doubled paths like `/api/api/matches/find` when env vars include `/api`.
 *
 * **Order matters for local dev:** set `BACKEND_INTERNAL_URL=http://127.0.0.1:8082` in `frontend/.env.local`
 * so the Next server always proxies to your local JVM even if `BACKEND_URL` or `NEXT_PUBLIC_BACKEND_URL`
 * points at ngrok. `NEXT_PUBLIC_API_URL` is used before `NEXT_PUBLIC_BACKEND_URL` for the same reason.
 */
export function springBackendOrigin(
  env: NodeJS.ProcessEnv = process.env
): string {
  // BACKEND_INTERNAL_URL first: overrides a user/global BACKEND_URL that might point at ngrok.
  const raw =
    env.BACKEND_INTERNAL_URL?.trim() ||
    env.BACKEND_URL?.trim() ||
    env.SPRING_BOOT_BACKEND_URL?.trim() ||
    env.NEXT_PUBLIC_API_URL?.trim() ||
    env.NEXT_PUBLIC_BACKEND_URL?.trim() ||
    "http://localhost:8082";

  if (!raw || raw.startsWith("/")) {
    return "http://localhost:8082";
  }

  const normalized = raw
    .replace(/\/+$/, "")
    .replace(/\/api\/?$/, "");

  if (!normalized || normalized.startsWith("/")) {
    return "http://localhost:8082";
  }

  return normalized;
}

/**
 * Absolute origin for CSP connect-src when env uses a relative client path (e.g. `/api`).
 */
export function resolveCspApiOrigin(env: NodeJS.ProcessEnv = process.env): string {
  const apiBase =
    env.NEXT_PUBLIC_API_BASE_URL?.trim() ||
    env.NEXT_PUBLIC_API_URL?.trim() ||
    "";

  if (apiBase && !apiBase.startsWith("/")) {
    try {
      const withScheme = apiBase.startsWith("http") ? apiBase : `https://${apiBase}`;
      return new URL(withScheme).origin;
    } catch {
      // fall through to site origin
    }
  }

  const site =
    env.NEXT_PUBLIC_SITE_URL?.trim() ||
    (env.VERCEL_URL
      ? `https://${env.VERCEL_URL.replace(/^https?:\/\//, "")}`
      : "") ||
    "https://roombay.app";

  try {
    return new URL(site.startsWith("http") ? site : `https://${site}`).origin;
  } catch {
    return "https://api.roombay.app";
  }
}
