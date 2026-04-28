/** Must match {@code RoomBayGoogleOAuth2SuccessHandler#OAUTH_SIGNUP_ROLE_COOKIE} on the backend. */
export const OAUTH_SIGNUP_ROLE_COOKIE = "roombay_oauth_signup_role"

const COOKIE_MAX_AGE_SEC = 600

/**
 * Same-site cookie so Spring sees the chosen role on the OAuth callback (Forwarded Cookie header),
 * even when the session-based hints do not survive the Google redirect.
 */
export function setOAuthSignupRoleCookie(role: "LANDLORD" | "STUDENT") {
  if (typeof document === "undefined") return
  document.cookie = `${OAUTH_SIGNUP_ROLE_COOKIE}=${encodeURIComponent(role)}; Path=/; Max-Age=${COOKIE_MAX_AGE_SEC}; SameSite=Lax`
}

export function startGoogleOAuthWithSignupRole(googleOAuthUrl: string, role: "LANDLORD" | "STUDENT") {
  setOAuthSignupRoleCookie(role)
  window.location.assign(googleOAuthUrl)
}

/** Clears a leftover register-flow cookie so login-page Google is not mis-scoped as landlord. */
export function clearOAuthSignupRoleCookie() {
  if (typeof document === "undefined") return
  document.cookie = `${OAUTH_SIGNUP_ROLE_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`
}
