"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  Plus, 
  Home, 
  Eye,
  Heart,
  Users,
  MoreVertical,
  Edit,
  Trash2,
  ExternalLink,
  Search,
  Filter
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import Link from "next/link"
import api from "@/lib/api"

interface Listing {
  id: string
  title: string
  city: string
  neighborhood: string
  rentAmount: number
  status: "ACTIVE" | "RENTED" | "INACTIVE" | "DRAFT" | "PENDING"
  viewsCount: number
  favoritesCount: number
  applicationsCount: number
  photos: { photoUrl: string; isPrimary: boolean }[]
  createdAt: string
}

export default function LandlordListingsPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [listings, setListings] = useState<Listing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("ALL")
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fetchListings = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await api.get(`/listings/landlord/${user.id}`)
      setListings(response.data || [])
    } catch (err) {
      console.error("Failed to fetch listings:", err)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchListings()
  }, [fetchListings])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchListings,
  })

  const handleMarkRented = async (listingId: string) => {
    if (!user?.id) return
    try {
      await api.post(`/listings/${listingId}/mark-rented`, null, {
        params: { landlordId: user.id }
      })
      fetchListings()
    } catch (err) {
      console.error("Failed to mark as rented:", err)
    }
  }

  const handleMarkAvailable = async (listingId: string) => {
    if (!user?.id) return
    try {
      await api.post(`/listings/${listingId}/mark-available`, null, {
        params: { landlordId: user.id }
      })
      fetchListings()
    } catch (err) {
      console.error("Failed to mark as available:", err)
    }
  }

  const handleDelete = async () => {
    if (!user?.id || !deleteId) return
    
    setIsDeleting(true)
    try {
      await api.delete(`/listings/${deleteId}`, {
        params: { landlordId: user.id }
      })
      setListings(prev => prev.filter(l => l.id !== deleteId))
      setDeleteId(null)
    } catch (err) {
      console.error("Failed to delete listing:", err)
    } finally {
      setIsDeleting(false)
    }
  }

  const filteredListings = listings.filter(listing => {
    const matchesSearch = listing.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         listing.neighborhood.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesStatus = statusFilter === "ALL" || listing.status === statusFilter
    return matchesSearch && matchesStatus
  })

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="My Listings" />

      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        <div className="p-4 space-y-4">
          {/* Search & Filter */}
          <div className="flex gap-2">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                placeholder="Search listings..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 h-11 rounded-xl"
              />
            </div>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="icon" className="h-11 w-11 rounded-xl">
                  <Filter className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => setStatusFilter("ALL")}>
                  All Status
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setStatusFilter("ACTIVE")}>
                  Active Only
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setStatusFilter("RENTED")}>
                  Rented Only
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => setStatusFilter("INACTIVE")}>
                  Inactive Only
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>

          {/* Add New Button */}
          <Link href="/landlord/listings/new">
            <Button className="w-full h-12 rounded-xl">
              <Plus className="h-5 w-5 mr-2" />
              Add New Listing
            </Button>
          </Link>

          {/* Listings */}
          {isLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-28 rounded-2xl" />
              ))}
            </div>
          ) : filteredListings.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl">
              <div className="mx-auto mb-3 h-14 w-14 rounded-full bg-slate-100 flex items-center justify-center">
                <Home className="h-7 w-7 text-slate-400" />
              </div>
              <p className="text-slate-500 text-sm mb-3">
                {searchQuery || statusFilter !== "ALL" 
                  ? "No listings match your filters" 
                  : "No listings yet"}
              </p>
              {!searchQuery && statusFilter === "ALL" && (
                <Link href="/landlord/listings/new">
                  <Button size="sm">Create Your First Listing</Button>
                </Link>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              {filteredListings.map((listing) => (
                <div key={listing.id} className="bg-white rounded-2xl shadow-sm overflow-hidden">
                  <div className="flex gap-3 p-3">
                    {/* Image */}
                    <Link href={`/listings/${listing.id}`} className="h-24 w-24 rounded-xl bg-slate-100 overflow-hidden flex-shrink-0">
                      {listing.photos?.[0]?.photoUrl ? (
                        <img 
                          src={listing.photos[0].photoUrl} 
                          alt={listing.title}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <div className="h-full w-full flex items-center justify-center text-3xl">
                          🏠
                        </div>
                      )}
                    </Link>

                    {/* Details */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <Link href={`/listings/${listing.id}`}>
                          <h3 className="font-medium text-slate-900 line-clamp-1 hover:text-blue-600">
                            {listing.title}
                          </h3>
                        </Link>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-8 w-8 -mr-2">
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem asChild>
                              <Link href={`/landlord/listings/${listing.id}/edit`} className="flex items-center">
                                <Edit className="h-4 w-4 mr-2" />
                                Edit
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuItem asChild>
                              <Link href={`/listings/${listing.id}`} className="flex items-center">
                                <ExternalLink className="h-4 w-4 mr-2" />
                                View Public Page
                              </Link>
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            {listing.status === "ACTIVE" ? (
                              <DropdownMenuItem onClick={() => handleMarkRented(listing.id)}>
                                Mark as Rented
                              </DropdownMenuItem>
                            ) : listing.status === "DRAFT" ? (
                              <DropdownMenuItem onClick={() => handleMarkAvailable(listing.id)}>
                                Submit for Approval
                              </DropdownMenuItem>
                            ) : listing.status === "PENDING" ? (
                              <DropdownMenuItem disabled className="text-amber-600">
                                Awaiting Admin Approval
                              </DropdownMenuItem>
                            ) : (
                              <DropdownMenuItem onClick={() => handleMarkAvailable(listing.id)}>
                                Re-list Property
                              </DropdownMenuItem>
                            )}
                            <DropdownMenuSeparator />
                            <DropdownMenuItem 
                              onClick={() => setDeleteId(listing.id)}
                              className="text-red-600 focus:text-red-600"
                            >
                              <Trash2 className="h-4 w-4 mr-2" />
                              Delete
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                      
                      <p className="text-sm text-slate-500">
                        {listing.neighborhood}, {listing.city}
                      </p>
                      
                      <p className="text-sm font-semibold text-blue-600 mt-1">
                        {formatCurrency(listing.rentAmount)}/mois
                      </p>

                      {/* Stats */}
                      <div className="flex items-center gap-3 mt-2 text-xs text-slate-500">
                        <span className="flex items-center gap-1">
                          <Eye className="h-3.5 w-3.5" />
                          {listing.viewsCount}
                        </span>
                        <span className="flex items-center gap-1">
                          <Heart className="h-3.5 w-3.5" />
                          {listing.favoritesCount}
                        </span>
                        <span className="flex items-center gap-1">
                          <Users className="h-3.5 w-3.5" />
                          {listing.applicationsCount}
                        </span>
                        <span className={`ml-auto px-2 py-0.5 rounded-full text-[10px] font-medium ${
                          listing.status === "ACTIVE" 
                            ? "bg-emerald-100 text-emerald-700" 
                            : listing.status === "RENTED"
                            ? "bg-blue-100 text-blue-700"
                            : "bg-slate-100 text-slate-600"
                        }`}>
                          {listing.status}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Listing?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the listing
              and all associated data including photos and applications.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-red-600 hover:bg-red-700"
              disabled={isDeleting}
            >
              {isDeleting ? "Deleting..." : "Delete"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
