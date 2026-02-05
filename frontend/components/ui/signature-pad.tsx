"use client"

import { useRef, useState, useEffect } from "react"
import SignatureCanvas from "react-signature-canvas"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Pencil, Type, RotateCcw, Check } from "lucide-react"

interface SignaturePadProps {
  onSignatureComplete: (signatureData: SignatureData) => void
  signerName: string
  disabled?: boolean
}

export interface SignatureData {
  type: "drawn" | "typed"
  data: string // Base64 for drawn, plain text for typed
  timestamp: string
  signerName: string
}

export function SignaturePad({ onSignatureComplete, signerName, disabled = false }: SignaturePadProps) {
  const signatureRef = useRef<SignatureCanvas>(null)
  const [activeTab, setActiveTab] = useState<"draw" | "type">("draw")
  const [typedSignature, setTypedSignature] = useState("")
  const [hasDrawn, setHasDrawn] = useState(false)
  const [canvasSize, setCanvasSize] = useState({ width: 300, height: 150 })

  // Adjust canvas size based on container
  useEffect(() => {
    const updateSize = () => {
      const container = document.getElementById("signature-container")
      if (container) {
        const width = Math.min(container.clientWidth - 32, 400)
        setCanvasSize({ width, height: 150 })
      }
    }
    
    updateSize()
    window.addEventListener("resize", updateSize)
    return () => window.removeEventListener("resize", updateSize)
  }, [])

  const clearSignature = () => {
    if (signatureRef.current) {
      signatureRef.current.clear()
      setHasDrawn(false)
    }
  }

  const handleDrawEnd = () => {
    if (signatureRef.current && !signatureRef.current.isEmpty()) {
      setHasDrawn(true)
    }
  }

  const handleConfirm = () => {
    const timestamp = new Date().toISOString()
    
    if (activeTab === "draw" && signatureRef.current && !signatureRef.current.isEmpty()) {
      const signatureData: SignatureData = {
        type: "drawn",
        data: signatureRef.current.toDataURL("image/png"),
        timestamp,
        signerName,
      }
      onSignatureComplete(signatureData)
    } else if (activeTab === "type" && typedSignature.trim()) {
      // Create a canvas with typed signature
      const canvas = document.createElement("canvas")
      canvas.width = canvasSize.width
      canvas.height = canvasSize.height
      const ctx = canvas.getContext("2d")
      
      if (ctx) {
        ctx.fillStyle = "white"
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        ctx.fillStyle = "#1e293b"
        ctx.font = "italic 32px 'Brush Script MT', cursive, Georgia, serif"
        ctx.textAlign = "center"
        ctx.textBaseline = "middle"
        ctx.fillText(typedSignature, canvas.width / 2, canvas.height / 2)
        
        const signatureData: SignatureData = {
          type: "typed",
          data: canvas.toDataURL("image/png"),
          timestamp,
          signerName,
        }
        onSignatureComplete(signatureData)
      }
    }
  }

  const isValid = activeTab === "draw" ? hasDrawn : typedSignature.trim().length > 0

  return (
    <div id="signature-container" className="w-full">
      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as "draw" | "type")}>
        <TabsList className="grid w-full grid-cols-2 mb-4">
          <TabsTrigger value="draw" disabled={disabled} className="gap-2">
            <Pencil className="h-4 w-4" />
            Draw
          </TabsTrigger>
          <TabsTrigger value="type" disabled={disabled} className="gap-2">
            <Type className="h-4 w-4" />
            Type
          </TabsTrigger>
        </TabsList>
        
        <TabsContent value="draw" className="mt-0">
          <div className="border-2 border-dashed border-slate-300 rounded-xl p-4 bg-white">
            <p className="text-xs text-slate-500 text-center mb-2">
              Sign with your finger or mouse below
            </p>
            <div className="relative bg-slate-50 rounded-lg overflow-hidden" style={{ height: canvasSize.height }}>
              <SignatureCanvas
                ref={signatureRef}
                canvasProps={{
                  width: canvasSize.width,
                  height: canvasSize.height,
                  className: "w-full h-full cursor-crosshair",
                  style: { touchAction: "none" },
                }}
                backgroundColor="rgb(248 250 252)" // slate-50
                penColor="#1e293b" // slate-800
                onEnd={handleDrawEnd}
              />
              {/* Signature line */}
              <div className="absolute bottom-8 left-4 right-4 border-b border-slate-300" />
              <span className="absolute bottom-2 left-4 text-xs text-slate-400">Sign here</span>
            </div>
            
            <Button
              variant="outline"
              size="sm"
              onClick={clearSignature}
              disabled={disabled || !hasDrawn}
              className="mt-3 w-full"
            >
              <RotateCcw className="h-4 w-4 mr-2" />
              Clear & Redo
            </Button>
          </div>
        </TabsContent>
        
        <TabsContent value="type" className="mt-0">
          <div className="border-2 border-dashed border-slate-300 rounded-xl p-4 bg-white">
            <p className="text-xs text-slate-500 text-center mb-2">
              Type your full name as your signature
            </p>
            <Input
              value={typedSignature}
              onChange={(e) => setTypedSignature(e.target.value)}
              placeholder="Your full name"
              disabled={disabled}
              className="text-center text-lg h-12 rounded-xl"
            />
            
            {/* Preview of typed signature */}
            {typedSignature && (
              <div className="mt-4 p-4 bg-slate-50 rounded-lg text-center">
                <p className="text-xs text-slate-500 mb-2">Preview:</p>
                <p 
                  className="text-2xl text-slate-800"
                  style={{ fontFamily: "'Brush Script MT', cursive, Georgia, serif", fontStyle: "italic" }}
                >
                  {typedSignature}
                </p>
              </div>
            )}
          </div>
        </TabsContent>
      </Tabs>
      
      {/* Signing as info */}
      <div className="mt-4 p-3 bg-blue-50 rounded-xl">
        <p className="text-xs text-blue-700">
          <strong>Signing as:</strong> {signerName}
        </p>
        <p className="text-xs text-blue-600 mt-1">
          By signing, you agree to the terms of this lease agreement. This signature is legally binding.
        </p>
      </div>
      
      {/* Confirm button */}
      <Button
        onClick={handleConfirm}
        disabled={disabled || !isValid}
        className="w-full mt-4 h-12 rounded-xl bg-green-600 hover:bg-green-700"
      >
        <Check className="h-4 w-4 mr-2" />
        Confirm Signature
      </Button>
    </div>
  )
}
