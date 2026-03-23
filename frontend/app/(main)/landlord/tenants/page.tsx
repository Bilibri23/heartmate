"use client"

import { useState, useEffect, useCallback } from "react"
import { MobileHeader } from "@/components/layout/mobile-header"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { usePullToRefresh } from "@/hooks/use-pull-to-refresh"
import { PullToRefreshIndicator } from "@/components/ui/pull-to-refresh"
import { 
  Users, 
  MessageCircle,
  Phone,
  Mail,
  Home,
  Calendar,
  ChevronRight,
  Search
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import Link from "next/link"
import api from "@/lib/api"

interface Tenant {
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string
  profilePhotoUrl?: string
  listing: {
    id: string
    title: string
    neighborhood: string
    city: string
  }
  lease: {
    id: string
    startDate: string
    endDate: string
    status: string
    rentAmount: number
  }
}

export default function LandlordTenantsPage() {
  const { t, formatCurrency } = useLanguage()
  const { user } = useAuth()
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")

  const fetchTenants = useCallback(async () => {
    if (!user?.id) return
    
    setIsLoading(true)
    try {
      const response = await api.get(`/leases/as-landlord`, { params: { size: 100 } })
      const leases = response.data?.content || response.data || []
      
      // Derive tenants from leases: primary tenant + co-tenant per lease
      const tenantData: Tenant[] = []
      const seen = new Set<string>()
      
      for (const lease of leases) {
        const status = lease.status
        if (!["ACTIVE", "PENDING_SIGNATURES", "PENDING_PAYMENT"].includes(status)) continue
        
        const listing = {
          id: lease.listingId,
          title: lease.listingTitle || "Property",
          neighborhood: "",
          city: ""
        }
        const leaseInfo = {
          id: lease.id,
          startDate: lease.startDate,
          endDate: lease.endDate,
          status,
          rentAmount: lease.monthlyRent || 0
        }
        
        // Primary tenant (student)
        const studentId = lease.studentId
        if (studentId && !seen.has(`${studentId}-${lease.id}`)) {
          seen.add(`${studentId}-${lease.id}`)
          const [firstName = "Unknown", ...rest] = (lease.studentName || "").split(" ")
          tenantData.push({
            id: studentId,
            firstName,
            lastName: rest.join(" ") || "Tenant",
            email: lease.studentEmail || "",
            phone: lease.studentPhone || "",
            profilePhotoUrl: undefined,
            listing,
            lease: leaseInfo
          })
        }
        
        // Co-tenant (if shared lease)
        if (lease.coTenantId && lease.isSharedLease && !seen.has(`${lease.coTenantId}-${lease.id}`)) {
          seen.add(`${lease.coTenantId}-${lease.id}`)
          const [firstName = "Unknown", ...rest] = (lease.coTenantName || "").split(" ")
          tenantData.push({
            id: lease.coTenantId,
            firstName,
            lastName: rest.join(" ") || "Tenant",
            email: lease.coTenantEmail || "",
            phone: lease.coTenantPhone || "",
            profilePhotoUrl: undefined,
            listing,
            lease: leaseInfo
          })
        }
      }
      
      setTenants(tenantData)
    } catch (err) {
      console.error("Failed to fetch tenants:", err)
      setTenants([])
    } finally {
      setIsLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchTenants()
  }, [fetchTenants])

  const { containerRef, isRefreshing, pullProgress } = usePullToRefresh({
    onRefresh: fetchTenants,
  })

  const filteredTenants = tenants.filter(tenant => {
    const fullName = `${tenant.firstName} ${tenant.lastName}`.toLowerCase()
    return fullName.includes(searchQuery.toLowerCase()) ||
           tenant.listing.title.toLowerCase().includes(searchQuery.toLowerCase())
  })

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="My Tenants" />

      <div 
        ref={containerRef}
        className="flex-1 overflow-y-auto relative"
      >
        <PullToRefreshIndicator 
          pullProgress={pullProgress} 
          isRefreshing={isRefreshing} 
        />

        <div className="p-4 space-y-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder="Search tenants..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 h-11 rounded-xl"
            />
          </div>

          {/* Tenants List */}
          {isLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-32 rounded-2xl" />
              ))}
            </div>
          ) : filteredTenants.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl">
              <div className="mx-auto mb-3 h-14 w-14 rounded-full bg-slate-100 flex items-center justify-center">
                <Users className="h-7 w-7 text-slate-400" />
              </div>
              <h3 className="font-semibold text-slate-900 mb-1">No Tenants Yet</h3>
              <p className="text-slate-500 text-sm">
                {searchQuery 
                  ? "No tenants match your search" 
                  : "Your tenants will appear here once you have active leases"}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredTenants.map((tenant, index) => (
                <div key={`${tenant.id}-${tenant.lease.id}-${index}`} className="bg-white rounded-2xl shadow-sm overflow-hidden">
                  <div className="p-4">
                    {/* Tenant Info */}
                    <div className="flex items-start gap-3">
                      <Avatar className="h-12 w-12">
                        <AvatarImage src={tenant.profilePhotoUrl} />
                        <AvatarFallback className="bg-blue-100 text-blue-600">
                          {tenant.firstName[0]}{tenant.lastName[0]}
                        </AvatarFallback>
                      </Avatar>
                      
                      <div className="flex-1 min-w-0">
                        <h3 className="font-semibold text-slate-900">
                          {tenant.firstName} {tenant.lastName}
                        </h3>
                        <p className="text-sm text-slate-500 flex items-center gap-1">
                          <Home className="h-3.5 w-3.5" />
                          {tenant.listing.title}
                        </p>
                      </div>

                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                        tenant.lease.status === "ACTIVE"
                          ? "bg-emerald-100 text-emerald-700"
                          : "bg-slate-100 text-slate-600"
                      }`}>
                        {tenant.lease.status}
                      </span>
                    </div>

                    {/* Lease Details */}
                    <div className="mt-3 pt-3 border-t border-slate-100 flex items-center gap-4 text-sm">
                      <div className="flex items-center gap-1 text-slate-500">
                        <Calendar className="h-4 w-4" />
                        <span>
                          {new Date(tenant.lease.startDate).toLocaleDateString()} - {new Date(tenant.lease.endDate).toLocaleDateString()}
                        </span>
                      </div>
                      <span className="font-medium text-blue-600">
                        {formatCurrency(tenant.lease.rentAmount)}/mo
                      </span>
                    </div>

                    {/* Actions */}
                    <div className="mt-3 flex gap-2">
                      <Link href={`/messages/${tenant.id}`} className="flex-1">
                        <Button variant="outline" size="sm" className="w-full rounded-lg">
                          <MessageCircle className="h-4 w-4 mr-1" />
                          Message
                        </Button>
                      </Link>
                      <Button variant="outline" size="sm" className="rounded-lg" asChild>
                        <a href={`tel:${tenant.phone}`}>
                          <Phone className="h-4 w-4" />
                        </a>
                      </Button>
                      <Button variant="outline" size="sm" className="rounded-lg" asChild>
                        <a href={`mailto:${tenant.email}`}>
                          <Mail className="h-4 w-4" />
                        </a>
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
