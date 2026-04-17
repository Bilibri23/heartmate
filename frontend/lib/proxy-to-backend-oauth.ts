import http from "node:http"
import { NextRequest, NextResponse } from "next/server"

/** Same origin as NEXT_PUBLIC_API_URL / rewrites (no /api suffix). */
function backendOrigin(): string {
  const fromEnv =
    process.env.BACKEND_URL ||
    process.env.NEXT_PUBLIC_API_URL?.replace(/\/api\/?$/, "") ||
    "http://localhost:8082"
  return fromEnv.replace(/\/$/, "")
}

/**
 * Proxy OAuth paths to Spring with X-Forwarded-* and a public Host header.
 *
 * Node's `fetch()` (Undici) does not allow overriding `Host`, so Spring only ever
 * saw `localhost:8082` and OAuth redirect/session handling broke behind ngrok.
 * `http.request` allows setting Host to the public hostname.
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

  const host =
    request.headers.get("x-forwarded-host") ||
    request.headers.get("host") ||
    "localhost:3000"
  const protoHeader = request.headers.get("x-forwarded-proto")
  const proto =
    protoHeader || (request.nextUrl.protocol === "https:" ? "https" : "http")
  const hostOnly = host.split(":")[0]

  const xff =
    request.headers.get("x-forwarded-for") || request.headers.get("x-real-ip")

  const headers: http.OutgoingHttpHeaders = {
    Host: hostOnly,
    "X-Forwarded-Host": host,
    "X-Forwarded-Proto": proto,
    "X-Forwarded-Port": proto === "https" ? "443" : "80",
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

  let port = backendBase.port ? Number(backendBase.port) : 80
  if (!backendBase.port) {
    port = backendBase.protocol === "https:" ? 443 : 80
  }

  return await new Promise<NextResponse>((resolve, reject) => {
    const req = http.request(
      {
        hostname: backendBase.hostname,
        port,
        path: pathWithQuery,
        method: request.method,
        headers,
        timeout: 120_000,
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
      console.error("OAuth proxy error", err)
      resolve(
        NextResponse.json(
          { error: "Failed to reach auth server" },
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
