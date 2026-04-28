"use client"

import { useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { ArrowLeft, Loader2, AlertCircle } from "lucide-react"
import api from "@/lib/api"

type ListingVR = {
  id: string
  title: string
  videoTourUrl?: string
  videoTour?: { videoUrl?: string }
  virtualTourProvider?: string
}

function buildVrSrcDoc(panoUrl: string) {
  const safe = JSON.stringify(panoUrl)
  return `<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <script src="https://aframe.io/releases/1.6.0/aframe.min.js"></script>
    <style>
      html, body { margin: 0; height: 100%; background: #000; overflow: hidden; }
      #hint {
        position: fixed; left: 10px; bottom: 10px; z-index: 10;
        color: #fff; font: 12px/1.4 sans-serif;
        background: rgba(0,0,0,0.55); padding: 6px 8px; border-radius: 8px;
      }
    </style>
  </head>
  <body>
    <div id="hint">Drag to look around. Use VR goggles icon for immersive mode.</div>
    <a-scene embedded vr-mode-ui="enabled: true">
      <a-assets>
        <img id="pano" src=${safe} crossorigin="anonymous" />
      </a-assets>
      <a-sky src="#pano" rotation="0 -90 0"></a-sky>
      <a-camera position="0 1.6 0"></a-camera>
    </a-scene>
  </body>
</html>`
}

export default function ListingVrPage() {
  const params = useParams()
  const listingId = String(params?.id || "")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [tourUrl, setTourUrl] = useState<string | null>(null)
  const [title, setTitle] = useState("VR Tour")

  useEffect(() => {
    if (!listingId) return
    const run = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await api.get<ListingVR>(`/listings/${listingId}`)
        const data = res.data
        const url = data?.videoTour?.videoUrl || data?.videoTourUrl
        setTitle(data?.title || "VR Tour")
        if (!url || data?.virtualTourProvider !== "360") {
          setError("This listing does not have a 360 tour ready for VR mode yet.")
          setTourUrl(null)
        } else {
          setTourUrl(url)
        }
      } catch {
        setError("Could not load this listing for VR mode.")
      } finally {
        setLoading(false)
      }
    }
    void run()
  }, [listingId])

  const srcDoc = useMemo(() => (tourUrl ? buildVrSrcDoc(tourUrl) : ""), [tourUrl])

  return (
    <div className="min-h-screen bg-black text-white">
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/10 bg-black/90">
        <Link href={`/listings/${listingId}`} className="inline-flex items-center gap-2 text-sm text-white/90 hover:text-white">
          <ArrowLeft className="h-4 w-4" />
          Back to listing
        </Link>
        <div className="text-sm font-medium truncate max-w-[60vw]">{title}</div>
        <div className="text-xs text-white/70">VR</div>
      </div>

      {loading && (
        <div className="h-[calc(100vh-57px)] flex items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-white/80" />
        </div>
      )}

      {!loading && error && (
        <div className="h-[calc(100vh-57px)] flex items-center justify-center p-6">
          <div className="max-w-md rounded-2xl border border-red-300/20 bg-red-950/30 p-4">
            <div className="flex items-start gap-2">
              <AlertCircle className="h-5 w-5 text-red-300 mt-0.5" />
              <p className="text-sm text-red-100">{error}</p>
            </div>
          </div>
        </div>
      )}

      {!loading && tourUrl && (
        <iframe
          title={`${title} VR`}
          srcDoc={srcDoc}
          className="w-full h-[calc(100vh-57px)] border-0"
          allow="xr-spatial-tracking; gyroscope; accelerometer; fullscreen"
          allowFullScreen
        />
      )}
    </div>
  )
}
