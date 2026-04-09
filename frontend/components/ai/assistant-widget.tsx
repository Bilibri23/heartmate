"use client"

import { useMemo, useState } from "react"
import { usePathname } from "next/navigation"
import { MessageCircle, X, Sparkles } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { useAuth } from "@/context/auth-context"
import { AssistantChat } from "@/components/ai/assistant-chat"

export function AssistantWidget() {
  const pathname = usePathname()
  const { user } = useAuth()
  const [open, setOpen] = useState(false)

  const shouldShow = useMemo(() => {
    if (!user?.id) return false
    if (user.role === "ADMIN") return false
    if (!pathname) return false
    if (pathname === "/") return false
    if (pathname.startsWith("/login") || pathname.startsWith("/register")) return false
    return true
  }, [pathname, user?.id, user?.role])

  const persona = user?.role === "LANDLORD" ? "LANDLORD" : "TENANT"

  if (!shouldShow) return null

  return (
    <>
      <div
        data-tour="roombay-ai-assistant"
        className="fixed bottom-20 right-4 z-50"
      >
        <Button
          onClick={() => setOpen(true)}
          className="h-12 w-12 rounded-full shadow-lg bg-blue-600 hover:bg-blue-700"
          size="icon"
          aria-label="Open assistant"
        >
          <MessageCircle className="h-5 w-5" />
        </Button>
      </div>

      <Sheet open={open} onOpenChange={setOpen}>
        <SheetContent side="bottom" showClose={false} className="h-[85vh] rounded-t-3xl p-0 overflow-hidden">
          <div className="flex h-full flex-col">
            <SheetHeader className="p-4 border-b border-slate-200 bg-white">
              <div className="flex items-center justify-between gap-3">
                <SheetTitle className="flex items-center gap-2">
                  <Sparkles className="h-5 w-5 text-blue-600" />
                  RoomBay Assistant
                </SheetTitle>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-9 w-9"
                  onClick={() => setOpen(false)}
                  aria-label="Close assistant"
                >
                  <X className="h-5 w-5 text-slate-600" />
                </Button>
              </div>
              <p className="text-sm text-slate-500 mt-1">
                Ask anything about RoomBay. I’ll answer using our docs and features.
              </p>
            </SheetHeader>

            <div className="flex-1 min-h-0">
              <AssistantChat persona={persona} />
            </div>
          </div>
        </SheetContent>
      </Sheet>
    </>
  )
}

