"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/context/auth-context"
import { useLanguage } from "@/context/language-context"
import { coApplicationService, CoApplicationInvitation } from "@/services/co-application"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import { Textarea } from "@/components/ui/textarea"
import {
  Users,
  MapPin,
  Clock,
  CheckCircle,
  XCircle,
  ChevronRight,
  Home,
  ArrowLeft,
  Send,
  Inbox
} from "lucide-react"
import { toast } from "sonner"

type TabType = "received" | "sent"

export default function CoApplicationInvitationsPage() {
  const router = useRouter()
  const { user } = useAuth()
  const { formatCurrency } = useLanguage()
  
  const [activeTab, setActiveTab] = useState<TabType>("received")
  const [receivedInvitations, setReceivedInvitations] = useState<CoApplicationInvitation[]>([])
  const [sentInvitations, setSentInvitations] = useState<CoApplicationInvitation[]>([])
  const [isLoading, setIsLoading] = useState(true)
  
  // Response modal
  const [selectedInvitation, setSelectedInvitation] = useState<CoApplicationInvitation | null>(null)
  const [isResponding, setIsResponding] = useState(false)
  const [declineReason, setDeclineReason] = useState("")

  const fetchInvitations = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const [received, sent] = await Promise.all([
        coApplicationService.getReceivedInvitations(),
        coApplicationService.getSentInvitations()
      ])
      setReceivedInvitations(received)
      setSentInvitations(sent)
    } catch (err) {
      console.error("Failed to fetch invitations:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchInvitations()
  }, [fetchInvitations])

  const handleAccept = async (invitation: CoApplicationInvitation) => {
    setIsResponding(true)
    try {
      await coApplicationService.acceptInvitation(invitation.id)
      toast.success("Opening listing", {
        description: "Apply on your own when ready—your roommate can apply separately too."
      })
      router.push(`/listings/${invitation.listingId}`)
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to accept invitation")
    } finally {
      setIsResponding(false)
      setSelectedInvitation(null)
    }
  }

  const handleDecline = async () => {
    if (!selectedInvitation) return
    
    setIsResponding(true)
    try {
      await coApplicationService.declineInvitation(selectedInvitation.id, declineReason)
      toast.success("Invitation declined")
      fetchInvitations()
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to decline invitation")
    } finally {
      setIsResponding(false)
      setSelectedInvitation(null)
      setDeclineReason("")
    }
  }

  const handleCancel = async (invitationId: string) => {
    try {
      await coApplicationService.cancelInvitation(invitationId)
      toast.success("Invitation cancelled")
      fetchInvitations()
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to cancel invitation")
    }
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "PENDING":
        return <span className="px-2 py-1 rounded-full bg-amber-100 text-amber-700 text-xs">Pending</span>
      case "ACCEPTED":
        return <span className="px-2 py-1 rounded-full bg-green-100 text-green-700 text-xs">Accepted</span>
      case "DECLINED":
        return <span className="px-2 py-1 rounded-full bg-red-100 text-red-700 text-xs">Declined</span>
      case "EXPIRED":
        return <span className="px-2 py-1 rounded-full bg-slate-100 text-slate-600 text-xs">Expired</span>
      case "CANCELLED":
        return <span className="px-2 py-1 rounded-full bg-slate-100 text-slate-600 text-xs">Cancelled</span>
      case "APPLIED":
        return <span className="px-2 py-1 rounded-full bg-blue-100 text-blue-700 text-xs">Applied</span>
      default:
        return null
    }
  }

  const currentInvitations = activeTab === "received" ? receivedInvitations : sentInvitations

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <div className="sticky top-0 z-40 bg-white border-b border-slate-200">
        <div className="flex items-center gap-3 px-4 py-3">
          <button onClick={() => router.push("/applications")} className="p-1">
            <ArrowLeft className="h-5 w-5 text-slate-700" />
          </button>
          <h1 className="font-semibold text-slate-900">Share with roommate</h1>
        </div>
      </div>

      <div className="mx-4 mt-3 rounded-2xl border border-blue-100 bg-blue-50/90 px-4 py-3 text-sm text-slate-700">
        <p className="font-medium text-slate-900">How it works now</p>
        <p className="mt-1 text-slate-600">
          Each person applies separately. Open a listing, use <strong>Share</strong>, or go to{" "}
          <Link href="/matches" className="text-blue-600 underline">Matches</Link> to nudge a roommate—no joint submission required.
        </p>
      </div>

      {/* Tabs */}
      <div className="sticky top-14 z-30 bg-white border-b border-slate-200">
        <div className="flex">
          <button
            onClick={() => setActiveTab("received")}
            className={`flex-1 py-3 text-sm font-medium border-b-2 transition-colors ${
              activeTab === "received"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-slate-500"
            }`}
          >
            <div className="flex items-center justify-center gap-2">
              <Inbox className="h-4 w-4" />
              Received
              {receivedInvitations.filter(i => i.status === "PENDING").length > 0 && (
                <span className="px-1.5 py-0.5 rounded-full bg-blue-600 text-white text-xs">
                  {receivedInvitations.filter(i => i.status === "PENDING").length}
                </span>
              )}
            </div>
          </button>
          <button
            onClick={() => setActiveTab("sent")}
            className={`flex-1 py-3 text-sm font-medium border-b-2 transition-colors ${
              activeTab === "sent"
                ? "border-blue-600 text-blue-600"
                : "border-transparent text-slate-500"
            }`}
          >
            <div className="flex items-center justify-center gap-2">
              <Send className="h-4 w-4" />
              Sent
            </div>
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 p-4">
        {isLoading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white rounded-2xl p-4 shadow-sm">
                <div className="flex gap-3">
                  <Skeleton className="h-16 w-16 rounded-xl" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-5 w-32" />
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-4 w-24" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : currentInvitations.length === 0 ? (
          <div className="text-center py-12">
            <Users className="h-16 w-16 mx-auto text-slate-300 mb-4" />
            <h3 className="text-lg font-semibold text-slate-900 mb-2">
              {activeTab === "received" ? "No invitations received" : "No invitations sent"}
            </h3>
            <p className="text-slate-500 text-sm mb-4">
              {activeTab === "received"
                ? "Older roommate link invites still show here. New flow: share a listing from Search or Matches."
                : "Sent invites you created earlier appear here. Prefer Share on the listing for new invites."}
            </p>
            <div className="flex flex-wrap justify-center gap-2">
              <Link href="/matches">
                <Button className="rounded-xl">Go to Matches</Button>
              </Link>
              <Link href="/search">
                <Button variant="outline" className="rounded-xl">
                  Browse listings
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {currentInvitations.map((invitation) => (
              <div
                key={invitation.id}
                className="bg-white rounded-2xl shadow-sm overflow-hidden"
              >
                <div className="p-4">
                  <div className="flex gap-3">
                    {/* Listing Image */}
                    <div className="h-16 w-16 rounded-xl bg-slate-100 overflow-hidden flex-shrink-0">
                      {invitation.listingPhotoUrl ? (
                        <img
                          src={invitation.listingPhotoUrl}
                          alt={invitation.listingTitle}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <div className="h-full w-full flex items-center justify-center">
                          <Home className="h-6 w-6 text-slate-400" />
                        </div>
                      )}
                    </div>

                    {/* Details */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <h3 className="font-semibold text-slate-900 line-clamp-1">
                          {invitation.listingTitle}
                        </h3>
                        {getStatusBadge(invitation.status)}
                      </div>
                      <p className="text-sm text-slate-500 flex items-center gap-1 mt-0.5">
                        <MapPin className="h-3.5 w-3.5" />
                        {invitation.listingCity}
                      </p>
                      <p className="text-sm font-semibold text-green-600 mt-1">
                        {formatCurrency(invitation.listingRent)}/mo
                      </p>
                    </div>
                  </div>

                  {/* Roommate Info */}
                  <div className="mt-3 flex items-center gap-3 p-3 bg-slate-50 rounded-xl">
                    <Avatar className="h-10 w-10">
                      <AvatarImage
                        src={
                          activeTab === "received"
                            ? invitation.inviterPhotoUrl
                            : invitation.inviteePhotoUrl
                        }
                      />
                      <AvatarFallback className="bg-blue-100 text-blue-600">
                        {activeTab === "received"
                          ? invitation.inviterName?.[0]
                          : invitation.inviteeName?.[0]}
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-slate-900">
                        {activeTab === "received"
                          ? `From: ${invitation.inviterName}`
                          : `To: ${invitation.inviteeName}`}
                      </p>
                      <p className="text-xs text-slate-500">
                        {invitation.compatibilityScore}% compatible
                      </p>
                    </div>
                  </div>

                  {/* Message if any */}
                  {invitation.message && (
                    <p className="mt-3 text-sm text-slate-600 italic">
                      &ldquo;{invitation.message}&rdquo;
                    </p>
                  )}

                  {/* Expiry info for pending */}
                  {invitation.status === "PENDING" && (
                    <p className="mt-2 text-xs text-slate-500 flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      Expires {new Date(invitation.expiresAt).toLocaleDateString()}
                    </p>
                  )}
                </div>

                {/* Actions */}
                {invitation.status === "PENDING" && (
                  <div className="px-4 pb-4">
                    {activeTab === "received" ? (
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          className="flex-1 rounded-xl"
                          onClick={() => setSelectedInvitation(invitation)}
                        >
                          <XCircle className="h-4 w-4 mr-1" />
                          Decline
                        </Button>
                        <Button
                          className="flex-1 rounded-xl bg-green-600 hover:bg-green-700"
                          onClick={() => handleAccept(invitation)}
                          disabled={isResponding}
                        >
                          <CheckCircle className="h-4 w-4 mr-1" />
                          Accept
                        </Button>
                      </div>
                    ) : (
                      <Button
                        variant="outline"
                        className="w-full rounded-xl text-red-600 border-red-200 hover:bg-red-50"
                        onClick={() => handleCancel(invitation.id)}
                      >
                        Cancel Invitation
                      </Button>
                    )}
                  </div>
                )}

                {/* View listing link for accepted */}
                {invitation.status === "ACCEPTED" && (
                  <div className="px-4 pb-4">
                    <Link href={`/listings/${invitation.listingId}`}>
                      <Button className="w-full rounded-xl">
                        <Home className="h-4 w-4 mr-2" />
                        Open listing
                      </Button>
                    </Link>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Decline Modal */}
      <Sheet open={!!selectedInvitation} onOpenChange={() => setSelectedInvitation(null)}>
        <SheetContent side="bottom" className="h-auto rounded-t-3xl">
          <SheetHeader>
            <SheetTitle>Decline Invitation</SheetTitle>
          </SheetHeader>
          
          <div className="mt-4 space-y-4">
            <p className="text-sm text-slate-600">
              Are you sure you want to decline the invitation from {selectedInvitation?.inviterName}?
            </p>
            
            <div>
              <label className="text-sm font-medium text-slate-700 mb-2 block">
                Reason (optional)
              </label>
              <Textarea
                placeholder="Let them know why you can't apply together..."
                value={declineReason}
                onChange={(e) => setDeclineReason(e.target.value)}
                className="min-h-[80px] rounded-xl"
              />
            </div>
            
            <div className="flex gap-2">
              <Button
                variant="outline"
                className="flex-1 rounded-xl"
                onClick={() => setSelectedInvitation(null)}
              >
                Cancel
              </Button>
              <Button
                className="flex-1 rounded-xl bg-red-600 hover:bg-red-700"
                onClick={handleDecline}
                disabled={isResponding}
              >
                {isResponding ? "Declining..." : "Decline"}
              </Button>
            </div>
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}
