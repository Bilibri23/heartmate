import { springBackendOrigin } from "@/lib/spring-backend-origin";

/** Canonical marketing site origin (no trailing slash). */
export function getSiteUrl(env: NodeJS.ProcessEnv = process.env): string {
  const raw =
    env.NEXT_PUBLIC_SITE_URL?.trim() ||
    env.VERCEL_PROJECT_PRODUCTION_URL?.trim() ||
    "https://roombay.app";
  const withScheme = raw.startsWith("http") ? raw : `https://${raw}`;
  return withScheme.replace(/\/+$/, "");
}

/** Spring API origin + `/api` prefix for server-side fetches. */
export function getApiBaseUrl(env: NodeJS.ProcessEnv = process.env): string {
  return `${springBackendOrigin(env)}/api`;
}
