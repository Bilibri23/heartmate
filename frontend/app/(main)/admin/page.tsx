"use client"

import { useState, useEffect, useCallback } from "react"
import { useRouter } from "next/navigation"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import api from "@/lib/api"
import Link from "next/link"
import { 
  Users, 
  Home, 
  Shield, 
  AlertTriangle,
  CheckCircle,
  XCircle,
  Clock,
  TrendingUp,
  DollarSign,
  FileText,
  Search,
  ChevronRight,
  UserCheck,
  Building,
  Gavel
} from "lucide-react"

interface Statistics {
  totalUsers: number
  totalStudents: number
  totalLandlords: number
  totalListings: number
  activeListings: number
  pendingListings: number
  totalApplications: number
  pendingVerifications: number
  totalLeases: number
  activeLeases: number
  totalPayments: number
  pendingPayments: number
  totalRevenue: number
}

interface PendingVerification {
  id: string
  userId: string
  userName: string
  userEmail: string
  documentUrl: string
  status: string
  createdAt: string
}

interface PendingListing {
  id: string
  title: string
  landlordName: string
  city: string
  rentAmount: number
  createdAt: string
}

export default function AdminDashboardPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  
  const [statistics, setStatistics] = useState<Statistics | null>(null)
  const [pendingVerifications, setPendingVerifications] = useState<PendingVerification[]>([])
  const [pendingListings, setPendingListings] = useState<PendingListing[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<"overview" | "verifications" | "listings" | "users">("overview")
  const [authLoading, setAuthLoading] = useState(true)

  useEffect(() => {
    if (user === null && authLoading) {
      const timer = setTimeout(() => setAuthLoading(false), 500)
      return () => clearTimeout(timer)
    }
    if (user) {
      setAuthLoading(false)
      if (user.role !== "ADMIN") { router.push("/for-you"); return }
      fetchDashboardData()
    } else if (!authLoading) {
      router.push("/login")
    }
  }, [user, router, authLoading])

  const fetchDashboardData = async () => {
    setIsLoading(true)
    try {
      const [statsRes, verificationsRes, listingsRes] = await Promise.all([
        api.get("/admin/statistics"),
        api.get("/admin/verifications/pending"),
        api.get("/admin/listings/pending")
      ])
      
      setStatistics(statsRes.data)
      setPendingVerifications(verificationsRes.data || [])
      setPendingListings(listingsRes.data || [])
    } catch (err) {
      console.error("Failed to fetch admin data:", err)
    } finally {
      setIsLoading(false)
    }
  }

  const handleVerificationAction = async (verificationId: string, approved: boolean, rejectionReason?: string) => {
    try {
      await api.post(`/admin/verifications/${verificationId}/approve`, {
        status: approved ? "VERIFIED" : "REJECTED",
        rejectionReason: rejectionReason || (approved ? null : "Verification rejected by admin")
      })
      fetchDashboardData()
    } catch (err) {
      console.error("Failed to process verification:", err)
    }
  }

  const handleListingAction = async (listingId: string, approved: boolean, rejectionReason?: string) => {
    try {
      await api.post(`/admin/listings/${listingId}/approve`, {
        status: approved ? "ACTIVE" : "REJECTED",
        rejectionReason: rejectionReason || (approved ? null : "Listing rejected by admin")
      })
      fetchDashboardData()
    } catch (err) {
      console.error("Failed to process listing:", err)
    }
  }

  if (user?.role !== "ADMIN") {
    return null
  }

  const StatCard = ({ 
    icon: Icon, 
    label, 
    value, 
    color = "blue",
    subValue 
  }: { 
    icon: any
    label: string
    value: number | string
    color?: string
    subValue?: string
  }) => (
    <div className="bg-white rounded-2xl p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className={`w-12 h-12 rounded-xl bg-${color}-100 flex items-center justify-center`}>
          <Icon className={`h-6 w-6 text-${color}-600`} />
        </div>
        <div>
          <p className="text-2xl font-bold text-slate-900">{value}</p>
          <p className="text-sm text-slate-500">{label}</p>
          {subValue && <p className="text-xs text-slate-400">{subValue}</p>}
        </div>
      </div>
    </div>
  )

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Admin Dashboard" />

      {/* Tabs */}
      <div className="flex gap-2 p-4 overflow-x-auto bg-white border-b border-slate-100">
        {[
          { id: "overview", label: "Overview", icon: TrendingUp },
          { id: "verifications", label: "Verifications", icon: Shield },
          { id: "listings", label: "Listings", icon: Home },
          { id: "users", label: "Users", icon: Users },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors flex items-center gap-1 ${
              activeTab === tab.id
                ? "bg-blue-600 text-white"
                : "bg-slate-100 text-slate-600"
            }`}
          >
            <tab.icon className="h-4 w-4" />
            {tab.label}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-4 pb-24">
        {isLoading ? (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Skeleton className="h-24 rounded-2xl" />
              <Skeleton className="h-24 rounded-2xl" />
              <Skeleton className="h-24 rounded-2xl" />
              <Skeleton className="h-24 rounded-2xl" />
            </div>
          </div>
        ) : (
          <>
            {activeTab === "overview" && statistics && (
              <div className="space-y-6">
                {/* Quick Stats */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-white rounded-2xl p-4 shadow-sm">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center">
                        <Users className="h-6 w-6 text-blue-600" />
                      </div>
                      <div>
                        <p className="text-2xl font-bold text-slate-900">{statistics.totalUsers}</p>
                        <p className="text-sm text-slate-500">Total Users</p>
                      </div>
                    </div>
                  </div>
                  
                  <div className="bg-white rounded-2xl p-4 shadow-sm">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-green-100 flex items-center justify-center">
                        <Home className="h-6 w-6 text-green-600" />
                      </div>
                      <div>
                        <p className="text-2xl font-bold text-slate-900">{statistics.activeListings}</p>
                        <p className="text-sm text-slate-500">Active Listings</p>
                      </div>
                    </div>
                  </div>
                  
                  <div className="bg-white rounded-2xl p-4 shadow-sm">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-amber-100 flex items-center justify-center">
                        <Clock className="h-6 w-6 text-amber-600" />
                      </div>
                      <div>
                        <p className="text-2xl font-bold text-slate-900">{statistics.pendingVerifications}</p>
                        <p className="text-sm text-slate-500">Pending Reviews</p>
                      </div>
                    </div>
                  </div>
                  
                  <div className="bg-white rounded-2xl p-4 shadow-sm">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-purple-100 flex items-center justify-center">
                        <FileText className="h-6 w-6 text-purple-600" />
                      </div>
                      <div>
                        <p className="text-2xl font-bold text-slate-900">{statistics.activeLeases}</p>
                        <p className="text-sm text-slate-500">Active Leases</p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* User Breakdown */}
                <div className="bg-white rounded-2xl p-4 shadow-sm">
                  <h3 className="font-semibold text-slate-900 mb-4">User Breakdown</h3>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <UserCheck className="h-5 w-5 text-blue-600" />
                        <span className="text-slate-700">Students</span>
                      </div>
                      <span className="font-semibold text-slate-900">{statistics.totalStudents}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Building className="h-5 w-5 text-green-600" />
                        <span className="text-slate-700">Landlords</span>
                      </div>
                      <span className="font-semibold text-slate-900">{statistics.totalLandlords}</span>
                    </div>
                  </div>
                </div>

                {/* Quick Actions */}
                <div className="bg-white rounded-2xl p-4 shadow-sm">
                  <h3 className="font-semibold text-slate-900 mb-4">Quick Actions</h3>
                  <div className="space-y-2">
                    <Link href="/admin/users">
                      <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
                        <div className="flex items-center gap-3">
                          <Users className="h-5 w-5 text-blue-600" />
                          <span className="text-slate-700">Manage Users</span>
                        </div>
                        <ChevronRight className="h-5 w-5 text-slate-400" />
                      </div>
                    </Link>
                    <Link href="/admin/disputes">
                      <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
                        <div className="flex items-center gap-3">
                          <Gavel className="h-5 w-5 text-amber-600" />
                          <span className="text-slate-700">Handle Disputes</span>
                        </div>
                        <ChevronRight className="h-5 w-5 text-slate-400" />
                      </div>
                    </Link>
                    <Link href="/admin/payments">
                      <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
                        <div className="flex items-center gap-3">
                          <DollarSign className="h-5 w-5 text-green-600" />
                          <span className="text-slate-700">Payment Management</span>
                        </div>
                        <ChevronRight className="h-5 w-5 text-slate-400" />
                      </div>
                    </Link>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "verifications" && (
              <div className="space-y-4">
                <h3 className="font-semibold text-slate-900">
                  Pending Verifications ({pendingVerifications.length})
                </h3>
                
                {pendingVerifications.length === 0 ? (
                  <div className="text-center py-12 bg-white rounded-2xl">
                    <CheckCircle className="h-12 w-12 text-green-500 mx-auto mb-4" />
                    <p className="text-slate-600">All verifications processed!</p>
                  </div>
                ) : (
                  pendingVerifications.map((verification) => (
                    <div key={verification.id} className="bg-white rounded-2xl p-4 shadow-sm">
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <p className="font-semibold text-slate-900">{verification.userName}</p>
                          <p className="text-sm text-slate-500">{verification.userEmail}</p>
                          <p className="text-xs text-slate-400 mt-1">
                            Submitted: {new Date(verification.createdAt).toLocaleDateString()}
                          </p>
                        </div>
                        <span className="px-2 py-1 bg-amber-100 text-amber-700 rounded-full text-xs font-medium">
                          Pending
                        </span>
                      </div>
                      
                      {verification.documentUrl && (
                        <a 
                          href={verification.documentUrl} 
                          target="_blank" 
                          rel="noopener noreferrer"
                          className="text-blue-600 text-sm hover:underline mb-3 block"
                        >
                          View Document →
                        </a>
                      )}
                      
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          className="flex-1 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
                          onClick={() => handleVerificationAction(verification.id, false)}
                        >
                          <XCircle className="h-4 w-4 mr-1" />
                          Reject
                        </Button>
                        <Button
                          className="flex-1 rounded-xl bg-green-600 hover:bg-green-700"
                          onClick={() => handleVerificationAction(verification.id, true)}
                        >
                          <CheckCircle className="h-4 w-4 mr-1" />
                          Approve
                        </Button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            {activeTab === "listings" && (
              <div className="space-y-4">
                <h3 className="font-semibold text-slate-900">
                  Pending Listings ({pendingListings.length})
                </h3>
                
                {pendingListings.length === 0 ? (
                  <div className="text-center py-12 bg-white rounded-2xl">
                    <CheckCircle className="h-12 w-12 text-green-500 mx-auto mb-4" />
                    <p className="text-slate-600">All listings reviewed!</p>
                  </div>
                ) : (
                  pendingListings.map((listing) => (
                    <div key={listing.id} className="bg-white rounded-2xl p-4 shadow-sm">
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <p className="font-semibold text-slate-900">{listing.title}</p>
                          <p className="text-sm text-slate-500">by {listing.landlordName}</p>
                          <p className="text-sm text-blue-600 font-medium mt-1">
                            {formatCurrency(listing.rentAmount)}/month
                          </p>
                          <p className="text-xs text-slate-400 mt-1">
                            {listing.city} • {new Date(listing.createdAt).toLocaleDateString()}
                          </p>
                        </div>
                        <span className="px-2 py-1 bg-amber-100 text-amber-700 rounded-full text-xs font-medium">
                          Pending
                        </span>
                      </div>
                      
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          className="flex-1 rounded-xl border-red-200 text-red-600 hover:bg-red-50"
                          onClick={() => handleListingAction(listing.id, false)}
                        >
                          <XCircle className="h-4 w-4 mr-1" />
                          Reject
                        </Button>
                        <Button
                          className="flex-1 rounded-xl bg-green-600 hover:bg-green-700"
                          onClick={() => handleListingAction(listing.id, true)}
                        >
                          <CheckCircle className="h-4 w-4 mr-1" />
                          Approve
                        </Button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            {activeTab === "users" && (
              <div className="space-y-4">
                <Link href="/admin/users">
                  <Button className="w-full rounded-xl bg-blue-600 hover:bg-blue-700">
                    <Users className="h-4 w-4 mr-2" />
                    Open User Management
                  </Button>
                </Link>
                
                <div className="bg-white rounded-2xl p-4 shadow-sm">
                  <h3 className="font-semibold text-slate-900 mb-4">User Statistics</h3>
                  {statistics && (
                    <div className="space-y-3">
                      <div className="flex justify-between items-center py-2 border-b border-slate-100">
                        <span className="text-slate-600">Total Users</span>
                        <span className="font-semibold">{statistics.totalUsers}</span>
                      </div>
                      <div className="flex justify-between items-center py-2 border-b border-slate-100">
                        <span className="text-slate-600">Students</span>
                        <span className="font-semibold">{statistics.totalStudents}</span>
                      </div>
                      <div className="flex justify-between items-center py-2 border-b border-slate-100">
                        <span className="text-slate-600">Landlords</span>
                        <span className="font-semibold">{statistics.totalLandlords}</span>
                      </div>
                      <div className="flex justify-between items-center py-2">
                        <span className="text-slate-600">Pending Verifications</span>
                        <span className="font-semibold text-amber-600">{statistics.pendingVerifications}</span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
