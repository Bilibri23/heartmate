"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  Clock, 
  CheckCircle, 
  XCircle, 
  Star,
  MessageCircle,
  ChevronRight,
  User
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import { Textarea } from "@/components/ui/textarea"
import Link from "next/link"
import api from "@/lib/api"

interface Application {
  id: string
  studentId: string
  studentName: string
  studentPhotoUrl: string | null
  studentEmail: string
  studentPhone: string
  studentVerified: boolean
  listingId: string
  listingTitle: string
  message: string
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "SHORTLISTED"
  createdAt: string
  // Co-application fields
  isCoApplication?: boolean
  coApplicantId?: string
  coApplicantName?: string
  coApplicantEmail?: string
  coApplicantPhone?: string
  coApplicantPhotoUrl?: string | null
  coApplicantVerified?: boolean
  coApplicantConfirmed?: boolean
  coApplicantConfirmedAt?: string
}

type StatusFilter = "all" | "PENDING" | "SHORTLISTED" | "ACCEPTED" | "REJECTED"

export default function LandlordApplicationsPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [applications, setApplications] = useState<Application[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("PENDING")
  const [selectedApp, setSelectedApp] = useState<Application | null>(null)
  const [isReviewOpen, setIsReviewOpen] = useState(false)
  const [responseMessage, setResponseMessage] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const fetchApplications = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const params: Record<string, any> = { size: 50 }
      if (statusFilter !== "all") {
        params.status = statusFilter
      }
      
      const response = await api.get("/applications/landlord/received", { params })
      const content = response.data?.content || response.data || []
      setApplications(content)
    } catch (err) {
      console.error("Failed to fetch applications:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id, statusFilter])

  useEffect(() => {
    fetchApplications()
  }, [fetchApplications])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchApplications,
  })

  const handleReview = async (status: "ACCEPTED" | "REJECTED" | "SHORTLISTED") => {
    if (!selectedApp) return

    setIsSubmitting(true)
    try {
      await api.put(`/applications/${selectedApp.id}/review`, {
        status,
        response: responseMessage || undefined,
      })
      setIsReviewOpen(false)
      setSelectedApp(null)
      setResponseMessage("")
      fetchApplications()
    } catch (err) {
      console.error("Failed to review application:", err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const openReview = (app: Application) => {
    setSelectedApp(app)
    setIsReviewOpen(true)
  }

  const filters: { id: StatusFilter; label: string }[] = [
    { id: "PENDING", label: "Pending" },
    { id: "SHORTLISTED", label: "Shortlisted" },
    { id: "ACCEPTED", label: "Accepted" },
    { id: "REJECTED", label: "Rejected" },
    { id: "all", label: "All" },
  ]

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("fr-CM", {
      day: "numeric",
      month: "short"
    })
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Applications" />

      {/* Status Filters */}
      <div className="sticky top-14 z-30 bg-white border-b border-slate-200">
        <div className="flex px-4 gap-2 py-2 overflow-x-auto scrollbar-hide">
          {filters.map((filter) => (
            <button
              key={filter.id}
              onClick={() => setStatusFilter(filter.id)}
              className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                statusFilter === filter.id
                  ? "bg-blue-600 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        <div className="p-4 space-y-3">
          {/* Loading */}
          {isLoading && (
            <>
              {[1, 2, 3].map((i) => (
                <div key={i} className="bg-white rounded-2xl p-4 shadow-sm">
                  <div className="flex gap-3">
                    <Skeleton className="h-12 w-12 rounded-full" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-3 w-full" />
                    </div>
                  </div>
                </div>
              ))}
            </>
          )}

          {/* Empty State */}
          {!isLoading && applications.length === 0 && (
            <div className="text-center py-12">
              <div className="mx-auto mb-4 h-16 w-16 rounded-full bg-slate-100 flex items-center justify-center">
                <User className="h-8 w-8 text-slate-400" />
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                No applications
              </h3>
              <p className="text-slate-500 text-sm">
                {statusFilter === "PENDING" 
                  ? "No pending applications at the moment"
                  : "No applications match this filter"}
              </p>
            </div>
          )}

          {/* Applications List */}
          {!isLoading && applications.map((app) => (
            <div 
              key={app.id} 
              className="bg-white rounded-2xl shadow-sm overflow-hidden"
              onClick={() => openReview(app)}
            >
              <div className="p-4">
                <div className="flex items-start gap-3">
                  {/* Primary Applicant */}
                  <Avatar className="h-12 w-12">
                    <AvatarImage src={app.studentPhotoUrl || undefined} />
                    <AvatarFallback className="bg-blue-100 text-blue-600">
                      {app.studentName.split(" ").map(n => n[0]).join("").toUpperCase()}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <div>
                        <h3 className="font-semibold text-slate-900">
                          {app.studentName}
                        </h3>
                        {app.isCoApplication && app.coApplicantName && (
                          <p className="text-sm text-slate-600">
                            + {app.coApplicantName}
                          </p>
                        )}
                      </div>
                      <div className="text-right">
                        <span className="text-xs text-slate-500">
                          {formatDate(app.createdAt)}
                        </span>
                        {app.isCoApplication && (
                          <div className="flex items-center gap-1 mt-1">
                            <span className={`text-xs px-2 py-0.5 rounded-full ${
                              app.coApplicantConfirmed 
                                ? "bg-green-100 text-green-700" 
                                : "bg-blue-100 text-blue-700"
                            }`}>
                              {app.coApplicantConfirmed ? "Both Confirmed" : "Joint Application"}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                    
                    <p className="text-xs text-blue-600 mt-0.5">
                      For: {app.listingTitle}
                      {app.isCoApplication && (
                        <span className="ml-1 text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded-full">
                          Joint Application
                        </span>
                      )}
                    </p>
                    
                    <p className="text-sm text-slate-600 mt-2 line-clamp-2">
                      "{app.message}"
                    </p>
                  </div>

                  <ChevronRight className="h-5 w-5 text-slate-400 flex-shrink-0" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Review Sheet */}
      <Sheet open={isReviewOpen} onOpenChange={setIsReviewOpen}>
        <SheetContent side="bottom" className="h-[70vh] rounded-t-3xl overflow-y-auto">
          <SheetHeader>
            <SheetTitle>Review Application</SheetTitle>
          </SheetHeader>

          {selectedApp && (
            <div className="mt-6 space-y-6">
              {/* Applicants Info */}
              <div className="space-y-4">
                {/* Primary Applicant */}
                <div className="flex items-center gap-3 p-4 bg-blue-50 rounded-2xl">
                  <Avatar className="h-14 w-14">
                    <AvatarImage src={selectedApp.studentPhotoUrl || undefined} />
                    <AvatarFallback className="bg-blue-100 text-blue-600 text-lg">
                      {selectedApp.studentName.split(" ").map(n => n[0]).join("").toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-slate-900">{selectedApp.studentName}</p>
                      {selectedApp.studentVerified && (
                        <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">
                          Verified
                        </span>
                      )}
                      <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">
                        Primary Applicant
                      </span>
                    </div>
                    <p className="text-sm text-slate-500">{selectedApp.studentEmail}</p>
                    <p className="text-sm text-slate-500">{selectedApp.studentPhone}</p>
                  </div>
                </div>

                {/* Co-applicant */}
                {selectedApp.isCoApplication && selectedApp.coApplicantName && (
                  <div className="flex items-center gap-3 p-4 bg-purple-50 rounded-2xl">
                    <Avatar className="h-14 w-14">
                      <AvatarImage src={selectedApp.coApplicantPhotoUrl || undefined} />
                      <AvatarFallback className="bg-purple-100 text-purple-600 text-lg">
                        {selectedApp.coApplicantName.split(" ").map(n => n[0]).join("").toUpperCase()}
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <p className="font-semibold text-slate-900">{selectedApp.coApplicantName}</p>
                        {selectedApp.coApplicantVerified && (
                          <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">
                            Verified
                          </span>
                        )}
                        <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded-full">
                          Co-applicant
                        </span>
                        <span className={`text-xs px-2 py-0.5 rounded-full ${
                          selectedApp.coApplicantConfirmed 
                            ? "bg-green-100 text-green-700" 
                            : "bg-blue-100 text-blue-700"
                        }`}>
                          {selectedApp.coApplicantConfirmed ? "Both Confirmed" : "Joint Application Submitted"}
                        </span>
                      </div>
                      <p className="text-sm text-slate-500">{selectedApp.coApplicantEmail}</p>
                      <p className="text-sm text-slate-500">{selectedApp.coApplicantPhone}</p>
                    </div>
                  </div>
                )}
              </div>

              {/* Message */}
              <div>
                <p className="text-sm font-medium text-slate-700 mb-2">Their message:</p>
                <p className="text-sm text-slate-600 bg-slate-50 p-3 rounded-xl">
                  {selectedApp.message}
                </p>
              </div>

              {/* Response */}
              <div>
                <p className="text-sm font-medium text-slate-700 mb-2">Your response (optional):</p>
                <Textarea
                  placeholder="Add a message for the applicant..."
                  value={responseMessage}
                  onChange={(e) => setResponseMessage(e.target.value)}
                  className="min-h-[80px] rounded-xl"
                />
              </div>

              {/* Quick Actions */}
              <div className="space-y-2">
                <Link href={`/messages/${selectedApp.studentId}`} className="flex-1">
                  <Button variant="outline" className="w-full rounded-xl">
                    <MessageCircle className="h-4 w-4 mr-2" />
                    Message {selectedApp.studentName.split(" ")[0]}
                  </Button>
                </Link>
                {selectedApp.isCoApplication && selectedApp.coApplicantId && (
                  <Link href={`/messages/${selectedApp.coApplicantId}`} className="flex-1">
                    <Button variant="outline" className="w-full rounded-xl">
                      <MessageCircle className="h-4 w-4 mr-2" />
                      Message {selectedApp.coApplicantName?.split(" ")[0]}
                    </Button>
                  </Link>
                )}
              </div>

              {/* Decision Buttons */}
              {selectedApp.status === "PENDING" && (
                <div className="space-y-2">
                  <Button
                    className="w-full h-12 rounded-xl bg-amber-500 hover:bg-amber-600"
                    onClick={() => handleReview("SHORTLISTED")}
                    disabled={isSubmitting}
                  >
                    <Star className="h-4 w-4 mr-2" />
                    Shortlist
                  </Button>
                  <div className="grid grid-cols-2 gap-2">
                    <Button
                      variant="outline"
                      className="h-12 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
                      onClick={() => handleReview("REJECTED")}
                      disabled={isSubmitting}
                    >
                      <XCircle className="h-4 w-4 mr-2" />
                      Reject
                    </Button>
                    <Button
                      className="h-12 rounded-xl bg-emerald-600 hover:bg-emerald-700"
                      onClick={() => handleReview("ACCEPTED")}
                      disabled={isSubmitting}
                    >
                      <CheckCircle className="h-4 w-4 mr-2" />
                      Accept
                    </Button>
                  </div>
                </div>
              )}

              {selectedApp.status === "SHORTLISTED" && (
                <div className="grid grid-cols-2 gap-2">
                  <Button
                    variant="outline"
                    className="h-12 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
                    onClick={() => handleReview("REJECTED")}
                    disabled={isSubmitting}
                  >
                    <XCircle className="h-4 w-4 mr-2" />
                    Reject
                  </Button>
                  <Button
                    className="h-12 rounded-xl bg-emerald-600 hover:bg-emerald-700"
                    onClick={() => handleReview("ACCEPTED")}
                    disabled={isSubmitting}
                  >
                    <CheckCircle className="h-4 w-4 mr-2" />
                    Accept
                  </Button>
                </div>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
