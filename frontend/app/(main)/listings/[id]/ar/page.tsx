"use client"

import { useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { ArrowLeft, Loader2, AlertCircle, Save, RotateCcw } from "lucide-react"
import api from "@/lib/api"
import { useAuth } from "@/context/auth-context"
import { Button } from "@/components/ui/button"
import { toast } from "sonner"

type ListingAR = {
  id: string
  title: string
  landlordId: string
  videoTourUrl?: string
  videoTour?: { videoUrl?: string }
  virtualTourProvider?: string
}

type Marker = {
  x: number
  y: number
  z: number
  label?: string
  color?: string
}

function buildArSrcDoc(panoUrl: string, initialMarkers: Marker[]) {
  const safe = JSON.stringify(panoUrl)
  const markersJson = JSON.stringify(initialMarkers)
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
      #top {
        position: fixed; left: 10px; top: 10px; z-index: 10;
        color: #fff; font: 12px/1.4 sans-serif;
        background: rgba(16,185,129,0.4); padding: 6px 8px; border-radius: 8px;
      }
    </style>
  </head>
  <body>
    <div id="top">AR rehearsal mode (beta)</div>
    <div id="hint">Tap/click to drop markers and rehearse furniture positions.</div>
    <a-scene embedded vr-mode-ui="enabled: true" webxr="optionalFeatures: hit-test,local-floor,dom-overlay; overlayElement: body">
      <a-assets>
        <img id="pano" src=${safe} crossorigin="anonymous" />
      </a-assets>
      <a-sky id="sky" src="#pano" rotation="0 -90 0"></a-sky>
      <a-entity id="camRig" position="0 1.6 0">
        <a-camera id="cam"></a-camera>
      </a-entity>
      <a-entity id="cursor" cursor="rayOrigin: mouse" raycaster="objects: #sky"></a-entity>
    </a-scene>
    <script>
      (function () {
        const scene = document.querySelector("a-scene");
        const sky = document.getElementById("sky");
        const markers = ${markersJson};
        let count = markers.length;
        if (!scene || !sky) return;
        function broadcast() {
          try { window.parent.postMessage({ type: "AR_MARKERS", markers: markers }, "*"); } catch (_) {}
        }
        function addMarker(m) {
          const marker = document.createElement("a-sphere");
          marker.setAttribute("position", m.x + " " + m.y + " " + m.z);
          marker.setAttribute("radius", "0.18");
          marker.setAttribute("color", m.color || "#22c55e");
          marker.setAttribute("opacity", "0.9");
          marker.setAttribute("animation", "property: scale; dir: alternate; dur: 900; loop: true; to: 1.2 1.2 1.2");
          scene.appendChild(marker);
        }
        markers.forEach(addMarker);
        scene.addEventListener("click", function (evt) {
          const p = evt && evt.detail && evt.detail.intersection && evt.detail.intersection.point;
          if (!p) return;
          const m = { x: p.x, y: p.y, z: p.z, color: "#22c55e" };
          markers.push(m);
          count += 1;
          addMarker(m);
          const hint = document.getElementById("hint");
          if (hint) hint.textContent = "Markers: " + count + " · Tap more points or use headset icon for immersive view.";
          broadcast();
        });
        broadcast();
      })();
    </script>
  </body>
</html>`;
}

export default function ListingArPage() {
  const params = useParams()
  const { user } = useAuth()
  const listingId = String(params?.id || "")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [tourUrl, setTourUrl] = useState<string | null>(null)
  const [title, setTitle] = useState("AR Rehearsal")
  const [landlordId, setLandlordId] = useState<string>("")
  const [initialMarkers, setInitialMarkers] = useState<Marker[]>([])
  const [markers, setMarkers] = useState<Marker[]>([])
  const [isSaving, setIsSaving] = useState(false)
  const [resetKey, setResetKey] = useState(0)

  useEffect(() => {
    if (!listingId) return
    const run = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await api.get<ListingAR>(`/listings/${listingId}`)
        const data = res.data
        const url = data?.videoTour?.videoUrl || data?.videoTourUrl
        setTitle(data?.title || "AR Rehearsal")
        setLandlordId(data?.landlordId || "")
        if (!url || data?.virtualTourProvider !== "360") {
          setError("This listing does not have a 360 tour ready for AR rehearsal yet.")
          setTourUrl(null)
        } else {
          setTourUrl(url)
          const markerRes = await api.get<Marker[]>(`/listings/${listingId}/ar-markers`)
          const loaded = Array.isArray(markerRes.data) ? markerRes.data : []
          setInitialMarkers(loaded)
          setMarkers(loaded)
        }
      } catch {
        setError("Could not load this listing for AR rehearsal.")
      } finally {
        setLoading(false)
      }
    }
    void run()
  }, [listingId])

  useEffect(() => {
    const onMessage = (event: MessageEvent) => {
      if (!event.data || event.data.type !== "AR_MARKERS") return
      if (!Array.isArray(event.data.markers)) return
      setMarkers(event.data.markers)
    }
    window.addEventListener("message", onMessage)
    return () => window.removeEventListener("message", onMessage)
  }, [])

  const srcDoc = useMemo(
    () => (tourUrl ? buildArSrcDoc(tourUrl, initialMarkers) : ""),
    [tourUrl, initialMarkers, resetKey]
  )
  const canEdit = Boolean(user?.id && landlordId && user.id === landlordId)

  const saveMarkers = async () => {
    if (!canEdit) return
    setIsSaving(true)
    try {
      await api.put(`/listings/${listingId}/ar-markers`, { markers })
      setInitialMarkers(markers)
      toast.success("AR markers saved")
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Could not save AR markers")
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="min-h-screen bg-black text-white">
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/10 bg-black/90">
        <Link href={`/listings/${listingId}`} className="inline-flex items-center gap-2 text-sm text-white/90 hover:text-white">
          <ArrowLeft className="h-4 w-4" />
          Back to listing
        </Link>
        <div className="text-sm font-medium truncate max-w-[60vw]">{title}</div>
        <div className="text-xs text-white/70">AR</div>
      </div>
      <div className="flex items-center justify-between gap-3 px-4 py-2 border-b border-white/10 bg-black/80">
        <p className="text-xs text-white/80">Markers: {markers.length}</p>
        {canEdit ? (
          <div className="flex items-center gap-2">
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="border-white/20 bg-transparent text-white hover:bg-white/10"
              onClick={() => {
                setInitialMarkers([])
                setMarkers([])
                setResetKey((k) => k + 1)
              }}
            >
              <RotateCcw className="h-4 w-4 mr-1" />
              Clear
            </Button>
            <Button
              type="button"
              size="sm"
              className="bg-emerald-600 hover:bg-emerald-700"
              onClick={saveMarkers}
              disabled={isSaving}
            >
              <Save className="h-4 w-4 mr-1" />
              {isSaving ? "Saving..." : "Save markers"}
            </Button>
          </div>
        ) : (
          <p className="text-xs text-white/70">Read-only markers</p>
        )}
      </div>

      {loading && (
        <div className="h-[calc(100vh-97px)] flex items-center justify-center">
          <Loader2 className="h-8 w-8 animate-spin text-white/80" />
        </div>
      )}

      {!loading && error && (
        <div className="h-[calc(100vh-97px)] flex items-center justify-center p-6">
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
          key={resetKey}
          title={`${title} AR`}
          srcDoc={srcDoc}
          className="w-full h-[calc(100vh-97px)] border-0"
          allow="xr-spatial-tracking; gyroscope; accelerometer; fullscreen"
          allowFullScreen
        />
      )}
    </div>
  )
}
