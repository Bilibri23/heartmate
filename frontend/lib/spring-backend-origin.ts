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
