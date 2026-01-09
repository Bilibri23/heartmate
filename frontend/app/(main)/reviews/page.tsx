"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { Star, MessageSquare, Plus, X, ThumbsUp, ThumbsDown } from "lucide-react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Input } from "@/components/ui/input"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import api from "@/lib/api"

interface Review {
  id: string
  reviewerName: string
  reviewerPhotoUrl: string | null
  overallRating: number
  communicationRating?: number
  cleanlinessRating?: number
  valueRating?: number
  comment: string
  pros?: string
  cons?: string
  wouldRecommend?: boolean
  response: string | null
  listingTitle?: string
  revieweeName?: string
  createdAt: string
}

interface Lease {
  id: string
  referenceCode: string
  listingTitle: string
  landlordName?: string
  studentName?: string
  status: string
}

export default function ReviewsPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const [reviews, setReviews] = useState<Review[]>([])
  const [receivedReviews, setReceivedReviews] = useState<Review[]>([])
  const [completedLeases, setCompletedLeases] = useState<Lease[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<"written" | "received">("written")
  const [isWriteSheetOpen, setIsWriteSheetOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  
  const [newReview, setNewReview] = useState({
    leaseId: "",
    reviewType: "STUDENT_TO_LANDLORD" as "STUDENT_TO_LANDLORD" | "LANDLORD_TO_STUDENT",
    overallRating: 5,
    communicationRating: 5,
    cleanlinessRating: 5,
    valueRating: 5,
    comment: "",
    pros: "",
    cons: "",
    wouldRecommend: true,
  })

  const fetchReviews = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const [myRes, receivedRes, leasesRes] = await Promise.all([
        api.get("/reviews/my"),
        api.get(`/reviews/user/${user.id}`),
        api.get("/leases/my").catch(() => ({ data: [] }))
      ])
      
      setReviews(myRes.data?.content || myRes.data || [])
      setReceivedReviews(receivedRes.data?.content || receivedRes.data || [])
      
      // Filter leases that are completed and can be reviewed
      const leases = leasesRes.data?.content || leasesRes.data || []
      setCompletedLeases(leases.filter((l: Lease) => 
        l.status === "COMPLETED" || l.status === "TERMINATED"
      ))
    } catch (err) {
      console.error("Failed to fetch reviews:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchReviews()
  }, [fetchReviews])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchReviews,
  })

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("fr-CM", {
      day: "numeric",
      month: "short",
      year: "numeric"
    })
  }

  const handleSubmitReview = async () => {
    if (!newReview.leaseId || !newReview.comment) return
    
    setIsSubmitting(true)
    try {
      await api.post("/reviews", newReview)
      setIsWriteSheetOpen(false)
      setNewReview({
        leaseId: "",
        reviewType: user?.role === "LANDLORD" ? "LANDLORD_TO_STUDENT" : "STUDENT_TO_LANDLORD",
        overallRating: 5,
        communicationRating: 5,
        cleanlinessRating: 5,
        valueRating: 5,
        comment: "",
        pros: "",
        cons: "",
        wouldRecommend: true,
      })
      fetchReviews()
    } catch (err) {
      console.error("Failed to submit review:", err)
    } finally {
      setIsSubmitting(false)
    }
  }

  const renderStars = (rating: number, interactive: boolean = false, onChange?: (r: number) => void) => {
    return (
      <div className="flex gap-0.5">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            onClick={() => interactive && onChange?.(star)}
            className={`h-5 w-5 ${interactive ? "cursor-pointer" : ""} ${
              star <= rating 
                ? "fill-amber-400 text-amber-400" 
                : "text-slate-300"
            }`}
          />
        ))}
      </div>
    )
  }

  const currentReviews = activeTab === "written" ? reviews : receivedReviews

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title={t.reviews.title} />

      {/* Tabs */}
      <div className="bg-white border-b border-slate-100 px-4 py-3">
        <div className="flex gap-2">
          <button
            onClick={() => setActiveTab("written")}
            className={`flex-1 py-2 rounded-xl text-sm font-medium transition-colors ${
              activeTab === "written"
                ? "bg-blue-600 text-white"
                : "bg-slate-100 text-slate-600"
            }`}
          >
            Written ({reviews.length})
          </button>
          <button
            onClick={() => setActiveTab("received")}
            className={`flex-1 py-2 rounded-xl text-sm font-medium transition-colors ${
              activeTab === "received"
                ? "bg-blue-600 text-white"
                : "bg-slate-100 text-slate-600"
            }`}
          >
            Received ({receivedReviews.length})
          </button>
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

        <div className="p-4 space-y-4 pb-24">
          {/* Loading */}
          {isLoading && (
            <>
              {[1, 2, 3].map((i) => (
                <div key={i} className="bg-white rounded-2xl p-4 shadow-sm">
                  <div className="flex items-center gap-3 mb-3">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <div className="space-y-1">
                      <Skeleton className="h-4 w-24" />
                      <Skeleton className="h-3 w-16" />
                    </div>
                  </div>
                  <Skeleton className="h-16 w-full" />
                </div>
              ))}
            </>
          )}

          {/* Empty State */}
          {!isLoading && currentReviews.length === 0 && (
            <div className="text-center py-12">
              <div className="mx-auto mb-4 h-20 w-20 rounded-full bg-amber-100 flex items-center justify-center">
                <Star className="h-10 w-10 text-amber-400" />
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                {activeTab === "written" ? "No Reviews Written" : "No Reviews Received"}
              </h3>
              <p className="text-slate-500 text-sm mb-4">
                {activeTab === "written" 
                  ? "Write a review after completing a lease"
                  : "Reviews from others will appear here"}
              </p>
              {activeTab === "written" && completedLeases.length > 0 && (
                <Button 
                  onClick={() => setIsWriteSheetOpen(true)}
                  className="rounded-xl bg-blue-600 hover:bg-blue-700"
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Write a Review
                </Button>
              )}
            </div>
          )}

          {/* Reviews List */}
          {!isLoading && currentReviews.map((review) => (
            <div key={review.id} className="bg-white rounded-2xl p-4 shadow-sm">
              {/* Header */}
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-3">
                  <Avatar className="h-10 w-10">
                    <AvatarImage src={review.reviewerPhotoUrl || undefined} />
                    <AvatarFallback className="bg-blue-100 text-blue-600">
                      {review.reviewerName?.split(" ").map(n => n[0]).join("").toUpperCase() || "?"}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-medium text-slate-900">{review.reviewerName || "Anonymous"}</p>
                    <p className="text-xs text-slate-500">{formatDate(review.createdAt)}</p>
                  </div>
                </div>
                {renderStars(review.overallRating)}
              </div>

              {/* Listing/Reviewee */}
              {review.listingTitle && (
                <p className="text-xs text-blue-600 mb-2">
                  Re: {review.listingTitle}
                </p>
              )}

              {/* Comment */}
              <p className="text-sm text-slate-700 leading-relaxed">
                {review.comment}
              </p>

              {/* Pros/Cons */}
              {(review.pros || review.cons) && (
                <div className="mt-3 space-y-2">
                  {review.pros && (
                    <div className="flex items-start gap-2">
                      <ThumbsUp className="h-4 w-4 text-green-600 mt-0.5" />
                      <p className="text-sm text-slate-600">{review.pros}</p>
                    </div>
                  )}
                  {review.cons && (
                    <div className="flex items-start gap-2">
                      <ThumbsDown className="h-4 w-4 text-red-500 mt-0.5" />
                      <p className="text-sm text-slate-600">{review.cons}</p>
                    </div>
                  )}
                </div>
              )}

              {/* Would Recommend */}
              {review.wouldRecommend !== undefined && (
                <p className={`text-xs mt-2 ${
                  review.wouldRecommend ? "text-green-600" : "text-red-500"
                }`}>
                  {review.wouldRecommend ? "✓ Would recommend" : "✗ Would not recommend"}
                </p>
              )}

              {/* Response */}
              {review.response && (
                <div className="mt-3 p-3 bg-slate-50 rounded-xl">
                  <p className="text-xs text-slate-500 mb-1 flex items-center gap-1">
                    <MessageSquare className="h-3 w-3" />
                    Response
                  </p>
                  <p className="text-sm text-slate-700">{review.response}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Write Review FAB */}
      {completedLeases.length > 0 && (
        <button
          onClick={() => setIsWriteSheetOpen(true)}
          className="fixed bottom-24 right-4 w-14 h-14 bg-blue-600 text-white rounded-full shadow-lg flex items-center justify-center hover:bg-blue-700 transition-colors"
        >
          <Plus className="h-6 w-6" />
        </button>
      )}

      {/* Write Review Sheet */}
      <Sheet open={isWriteSheetOpen} onOpenChange={setIsWriteSheetOpen}>
        <SheetContent side="bottom" className="h-[90vh] rounded-t-3xl overflow-y-auto">
          <SheetHeader>
            <SheetTitle>Write a Review</SheetTitle>
          </SheetHeader>

          <div className="mt-6 space-y-6 pb-8">
            {/* Select Lease */}
            <div>
              <label className="text-sm font-medium text-slate-700 mb-2 block">
                Select Lease to Review
              </label>
              <Select
                value={newReview.leaseId}
                onValueChange={(value) => setNewReview(prev => ({ ...prev, leaseId: value }))}
              >
                <SelectTrigger className="rounded-xl">
                  <SelectValue placeholder="Select a completed lease" />
                </SelectTrigger>
                <SelectContent>
                  {completedLeases.map((lease) => (
                    <SelectItem key={lease.id} value={lease.id}>
                      {lease.listingTitle} ({lease.referenceCode})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Overall Rating */}
            <div>
              <label className="text-sm font-medium text-slate-700 mb-2 block">
                Overall Rating
              </label>
              <div className="flex items-center gap-2">
                {renderStars(newReview.overallRating, true, (r) => 
                  setNewReview(prev => ({ ...prev, overallRating: r }))
                )}
                <span className="text-sm text-slate-500 ml-2">
                  {newReview.overallRating}/5
                </span>
              </div>
            </div>

            {/* Detailed Ratings */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-xs text-slate-500 mb-1 block">Communication</label>
                {renderStars(newReview.communicationRating, true, (r) => 
                  setNewReview(prev => ({ ...prev, communicationRating: r }))
                )}
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">Cleanliness</label>
                {renderStars(newReview.cleanlinessRating, true, (r) => 
                  setNewReview(prev => ({ ...prev, cleanlinessRating: r }))
                )}
              </div>
              <div>
                <label className="text-xs text-slate-500 mb-1 block">Value for Money</label>
                {renderStars(newReview.valueRating, true, (r) => 
                  setNewReview(prev => ({ ...prev, valueRating: r }))
                )}
              </div>
            </div>

            {/* Comment */}
            <div>
              <label className="text-sm font-medium text-slate-700 mb-2 block">
                Your Review *
              </label>
              <Textarea
                placeholder="Share your experience..."
                value={newReview.comment}
                onChange={(e) => setNewReview(prev => ({ ...prev, comment: e.target.value }))}
                className="rounded-xl resize-none"
                rows={4}
              />
            </div>

            {/* Pros */}
            <div>
              <label className="text-sm font-medium text-slate-700 mb-2 block">
                Pros (Optional)
              </label>
              <Input
                placeholder="What did you like?"
                value={newReview.pros}
                onChange={(e) => setNewReview(prev => ({ ...prev, pros: e.target.value }))}
                className="rounded-xl"
              />
            </div>

            {/* Cons */}
            <div>
              <label className="text-sm font-medium text-slate-700 mb-2 block">
                Cons (Optional)
              </label>
              <Input
                placeholder="What could be improved?"
                value={newReview.cons}
                onChange={(e) => setNewReview(prev => ({ ...prev, cons: e.target.value }))}
                className="rounded-xl"
              />
            </div>

            {/* Would Recommend */}
            <div className="flex items-center justify-between p-4 bg-slate-50 rounded-xl">
              <span className="text-sm font-medium text-slate-700">Would you recommend?</span>
              <div className="flex gap-2">
                <button
                  onClick={() => setNewReview(prev => ({ ...prev, wouldRecommend: true }))}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    newReview.wouldRecommend
                      ? "bg-green-600 text-white"
                      : "bg-white text-slate-600 border border-slate-200"
                  }`}
                >
                  Yes
                </button>
                <button
                  onClick={() => setNewReview(prev => ({ ...prev, wouldRecommend: false }))}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    !newReview.wouldRecommend
                      ? "bg-red-600 text-white"
                      : "bg-white text-slate-600 border border-slate-200"
                  }`}
                >
                  No
                </button>
              </div>
            </div>

            {/* Submit */}
            <Button
              onClick={handleSubmitReview}
              disabled={isSubmitting || !newReview.leaseId || !newReview.comment}
              className="w-full h-12 rounded-xl bg-blue-600 hover:bg-blue-700"
            >
              {isSubmitting ? "Submitting..." : "Submit Review"}
            </Button>
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}
