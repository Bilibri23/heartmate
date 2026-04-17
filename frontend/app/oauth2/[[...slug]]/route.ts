import { NextRequest } from "next/server"
import { proxyOAuth } from "@/lib/proxy-to-backend-oauth"

type RouteCtx = { params: Promise<{ slug?: string[] }> }

export async function GET(request: NextRequest, ctx: RouteCtx) {
  const { slug } = await ctx.params
  return proxyOAuth(request, "/oauth2", slug)
}

export async function POST(request: NextRequest, ctx: RouteCtx) {
  const { slug } = await ctx.params
  return proxyOAuth(request, "/oauth2", slug)
}
