import http from "node:http"
import https from "node:https"
import { NextRequest, NextResponse } from "next/server"

function stripTrailingSlash(s: string): string {
  return s.replace(/\/$/, "")
}

function isLoopbackHost(hostname: string): boolean {
  return hostname === "localhost" || hostname === "127.0.0.1" || hostname === "[::1]"
}

/**
 * If NEXT_PUBLIC_API_URL points at the Next dev server itself (e.g. http://localhost:3000/api),
 * the OAuth proxy must not open a TCP connection to that port — it would talk to Next, not Spring.
 */
function isLikelyNextDevServerUrl(u: URL): boolean {
  if (!isLoopbackHost(u.hostname)) {
    return false
  }
  const p = u.port
  if (!p) {
    return false
  }
  const envPort = process.env.PORT
  if (envPort && p === envPort) {
    return true
  }
  return p === "3000" || p === "3001"
}

/**
 * TCP target for this Node proxy → Spring. Must be reachable from the Next server process.
 *
 * Priority: {@code BACKEND_INTERNAL_URL} / {@code BACKEND_URL} / {@code SPRING_BOOT_BACKEND_URL}
 * (internal first so a global {@code BACKEND_URL} pointing at ngrok does not steal OAuth from local Spring).
 * Otherwise, if {@code NEXT_PUBLIC_API_URL} resolves to a loopback URL that is **not** the Next
 * dev port, use it (strip {@code /api}). Else default to {@code http://127.0.0.1:8082}.
 *
 * Uses {@code https} when the origin is https (e.g. {@code BACKEND_URL=https://…ngrok…}).
 */
function backendOrigin(): string {
  const internal = [
    process.env.BACKEND_INTERNAL_URL,
    process.env.BACKEND_URL,
    process.env.SPRING_BOOT_BACKEND_URL,
  ].find((v) => v && String(v).trim())
  if (internal) {
    return stripTrailingSlash(String(internal).trim())
  }

  const raw = (process.env.NEXT_PUBLIC_API_URL || "").trim()
  const pub = stripTrailingSlash(raw.replace(/\/api\/?$/, ""))
  if (!pub) {
    return "http://127.0.0.1:8082"
  }
  try {
    const u = new URL(pub.startsWith("http") ? pub : `http://${pub}`)
    if (isLoopbackHost(u.hostname) && isLikelyNextDevServerUrl(u)) {
      return "http://127.0.0.1:8082"
    }
    if (isLoopbackHost(u.hostname)) {
      return stripTrailingSlash(`${u.protocol}//${u.host}`)
    }
  } catch {
    // ignore
  }

  return "http://127.0.0.1:8082"
}

/**
 * Port the browser used for this request (for {@code X-Forwarded-Port}).
 * Spring Security uses forwarded headers to build {@code redirect_uri}; if we send
 * port 80 for {@code http://localhost:3000}, Google gets {@code redirect_uri_mismatch}.
 */
function clientFacingPort(request: NextRequest, forwardedHost: string, proto: string): string {
  const fromHeader = request.headers.get("x-forwarded-port")?.trim()
  if (fromHeader && /^\d+$/.test(fromHeader)) {
    return fromHeader
  }

  const h = forwardedHost.trim()
  if (h.startsWith("[")) {
    const close = h.indexOf("]")
    if (close !== -1 && h[close + 1] === ":") {
      const p = h.slice(close + 2)
      if (p && /^\d+$/.test(p)) {
        return p
      }
    }
  } else if (h.includes(":")) {
    const last = h.lastIndexOf(":")
    const maybePort = h.slice(last + 1)
    if (/^\d+$/.test(maybePort)) {
      return maybePort
    }
  }

  const nu = request.nextUrl.port
  if (nu) {
    return nu
  }

  return proto === "https" ? "443" : "80"
}

/**
 * Proxy OAuth paths to Spring with X-Forwarded-* and a public Host header.
 *
 * Node's `fetch()` (Undici) does not allow overriding `Host`, so Spring only ever
 * saw `localhost:8082` and OAuth redirect/session handling broke behind ngrok.
 * `http.request` / `https.request` allows setting Host to the public hostname.
 *
 * The {@code Host} value must match what the browser used (including non-default ports
 * like {@code localhost:3000}), and {@code X-Forwarded-Port} must match, or Spring builds
 * the wrong {@code redirect_uri} for Google OAuth.
 */
export async function proxyOAuth(
  request: NextRequest,
  backendPathPrefix: "/oauth2" | "/login/oauth2",
  slug: string[] | undefined
): Promise<NextResponse> {
  const segments = slug && slug.length > 0 ? slug.join("/") : ""
  const path = segments ? `${backendPathPrefix}/${segments}` : backendPathPrefix
  const pathWithQuery = `${path}${request.nextUrl.search}`

  const backendBase = new URL(backendOrigin())
  const useTls = backendBase.protocol === "https:"
  const lib = useTls ? https : http
  let port: number
  if (backendBase.port) {
    port = Number(backendBase.port)
  } else if (useTls) {
    port = 443
  } else {
    port = 80
  }

  const host =
    request.headers.get("x-forwarded-host") ||
    request.headers.get("host") ||
    "localhost:3000"
  const protoHeader = request.headers.get("x-forwarded-proto")
  const proto =
    protoHeader || (request.nextUrl.protocol === "https:" ? "https" : "http")
  const forwardedPort = clientFacingPort(request, host, proto)

  const xff =
    request.headers.get("x-forwarded-for") || request.headers.get("x-real-ip")

  const headers: http.OutgoingHttpHeaders = {
    // Full host[:port] as the browser sent it (do not strip :3000 — breaks OAuth redirect_uri).
    Host: host,
    "X-Forwarded-Host": host,
    "X-Forwarded-Proto": proto,
    "X-Forwarded-Port": forwardedPort,
  }
  if (xff) {
    headers["X-Forwarded-For"] = xff
  }

  const cookie = request.headers.get("cookie")
  if (cookie) {
    headers.Cookie = cookie
  }
  const accept = request.headers.get("accept")
  if (accept) {
    headers.Accept = accept
  }
  const ua = request.headers.get("user-agent")
  if (ua) {
    headers["User-Agent"] = ua
  }
  const lang = request.headers.get("accept-language")
  if (lang) {
    headers["Accept-Language"] = lang
  }

  let bodyBuf: Buffer | undefined
  if (request.method !== "GET" && request.method !== "HEAD") {
    const ab = await request.arrayBuffer()
    if (ab.byteLength > 0) {
      bodyBuf = Buffer.from(ab)
    }
    const ct = request.headers.get("content-type")
    if (ct) {
      headers["Content-Type"] = ct
    }
    if (bodyBuf) {
      headers["Content-Length"] = String(bodyBuf.length)
    }
  }

  return await new Promise<NextResponse>((resolve) => {
    const req = lib.request(
      {
        hostname: backendBase.hostname,
        port,
        path: pathWithQuery,
        method: request.method,
        headers,
        timeout: 120_000,
        ...(useTls ? { servername: backendBase.hostname } : {}),
      },
      (res) => {
        const chunks: Buffer[] = []
        res.on("data", (c: Buffer) => {
          chunks.push(c)
        })
        res.on("end", () => {
          const data = Buffer.concat(chunks)
          const out = new Headers()
          for (const [key, value] of Object.entries(res.headers)) {
            if (!value) continue
            if (key.toLowerCase() === "transfer-encoding") continue
            if (Array.isArray(value)) {
              for (const v of value) {
                out.append(key, v)
              }
            } else {
              out.append(key, value)
            }
          }
          resolve(
            new NextResponse(data, {
              status: res.statusCode ?? 500,
              statusText: res.statusMessage,
              headers: out,
            })
          )
        })
      }
    )
    req.on("error", (err) => {
      console.error("OAuth proxy error", backendBase.origin, err)
      resolve(
        NextResponse.json(
          {
            error: "Failed to reach auth server",
            detail: process.env.NODE_ENV === "development" ? String((err as NodeJS.ErrnoException).code || err.message) : undefined,
          },
          { status: 502 }
        )
      )
    })
    req.on("timeout", () => {
      req.destroy()
      resolve(
        NextResponse.json({ error: "Auth server timeout" }, { status: 504 })
      )
    })
    if (bodyBuf) {
      req.write(bodyBuf)
    }
    req.end()
  })
}
