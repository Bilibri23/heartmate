"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import api from "@/lib/api"
import { Search, Shield, CheckCircle, XCircle, IdCard, Calendar, ExternalLink, Building2, User } from "lucide-react"

interface TenantVerification {
  id: string
  userId: string
  userName: string
  userEmail: string
  university?: string
  studentId?: string
  studentIdPhotoUrl?: string
  idType?: string
  idNumber?: string
  idPhotoUrl?: string
  selfiePhotoUrl?: string
  status: string
  rejectionReason?: string
  createdAt: string
}

interface LandlordVerification {
  id: string
  userId: string
  userName: string
  userEmail: string
  idType: string
  idNumber: string
  idFrontPhotoUrl: string
  selfieWithIdUrl: string
  identityStatus: string
  identityRejectionReason?: string
  createdAt: string
}

export default function AdminVerificationsPage() {
  const { user } = useAuth()
  const router = useRouter()
  const [tenantVerifications, setTenantVerifications] = useState<TenantVerification[]>([])
  const [landlordVerifications, setLandlordVerifications] = useState<LandlordVerification[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedTenant, setSelectedTenant] = useState<TenantVerification | null>(null)
  const [selectedLandlord, setSelectedLandlord] = useState<LandlordVerification | null>(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)
  const [rejectionReason, setRejectionReason] = useState("")
  const [authLoading, setAuthLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<"tenants" | "landlords">("tenants")

  useEffect(() => {
    if (user === null && authLoading) {
      const timer = setTimeout(() => setAuthLoading(false), 500)
      return () => clearTimeout(timer)
    }
    if (user) {
      setAuthLoading(false)
      if (user.role !== "ADMIN") { router.push("/for-you"); return }
      fetchData()
    } else if (!authLoading) {
      router.push("/login")
    }
  }, [user, router, authLoading])

  const fetchData = async () => {
    setIsLoading(true)
    try {
      const [studentRes, landlordRes] = await Promise.all([
        api.get("/admin/verifications"),
        api.get("/admin/landlord-verifications")
      ])
      setTenantVerifications(studentRes.data?.content || studentRes.data || [])
      setLandlordVerifications(landlordRes.data?.content || landlordRes.data || [])
    } catch (err) { console.error(err) }
    finally { setIsLoading(false) }
  }

  const handleTenantAction = async (id: string, approved: boolean) => {
    try {
      await api.post(`/admin/verifications/${id}/approve`, {
        status: approved ? "VERIFIED" : "REJECTED",
        rejectionReason: approved ? null : (rejectionReason || "Rejected by admin")
      })
      setRejectionReason("")
      fetchData()
      setIsDetailOpen(false)
    } catch (err) { console.error(err) }
  }

  const handleLandlordAction = async (id: string, approved: boolean) => {
    try {
      await api.post(`/admin/landlord-verifications/${id}/approve`, {
        verificationType: "IDENTITY",
        status: approved ? "VERIFIED" : "REJECTED",
        rejectionReason: approved ? null : (rejectionReason || "Rejected by admin")
      })
      setRejectionReason("")
      fetchData()
      setIsDetailOpen(false)
    } catch (err) { console.error(err) }
  }

  const filteredTenants = tenantVerifications.filter(v =>
    v.userName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    v.userEmail?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const filteredLandlords = landlordVerifications.filter(v =>
    v.userName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    v.userEmail?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const getStatusBadge = (status: string) => {
    const styles: Record<string, string> = {
      VERIFIED: "bg-green-100 text-green-700",
      PENDING: "bg-amber-100 text-amber-700",
      REJECTED: "bg-red-100 text-red-700"
    }
    return <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[status] || "bg-slate-100"}`}>{status}</span>
  }

  if (user?.role !== "ADMIN") return null

  const currentList = activeTab === "tenants" ? filteredTenants : filteredLandlords
  const pendingCount = activeTab === "tenants" 
    ? tenantVerifications.filter(v => v.status === "PENDING").length
    : landlordVerifications.filter(v => v.identityStatus === "PENDING").length
  const verifiedCount = activeTab === "tenants"
    ? tenantVerifications.filter(v => v.status === "VERIFIED").length
    : landlordVerifications.filter(v => v.identityStatus === "VERIFIED").length
  const rejectedCount = activeTab === "tenants"
    ? tenantVerifications.filter(v => v.status === "REJECTED").length
    : landlordVerifications.filter(v => v.identityStatus === "REJECTED").length

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Verifications" showBack />
      <div className="p-4 space-y-4">
        {/* Tab Switcher */}
        <div className="flex gap-2 bg-white rounded-xl p-1">
          <button
            onClick={() => setActiveTab("tenants")}
            className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-2 ${
              activeTab === "tenants" ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            <IdCard className="h-4 w-4" /> Tenants
          </button>
          <button
            onClick={() => setActiveTab("landlords")}
            className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-2 ${
              activeTab === "landlords" ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            <Building2 className="h-4 w-4" /> Landlords
          </button>
        </div>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input placeholder="Search..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="pl-10 rounded-xl" />
        </div>

        <div className="grid grid-cols-3 gap-2">
          <div className="bg-white rounded-xl p-3 text-center">
            <p className="text-lg font-bold text-amber-600">{pendingCount}</p>
            <p className="text-xs text-slate-500">Pending</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center">
            <p className="text-lg font-bold text-green-600">{verifiedCount}</p>
            <p className="text-xs text-slate-500">Verified</p>
          </div>
          <div className="bg-white rounded-xl p-3 text-center">
            <p className="text-lg font-bold text-red-600">{rejectedCount}</p>
            <p className="text-xs text-slate-500">Rejected</p>
          </div>
        </div>

        {isLoading ? (
          <div className="space-y-4">{[1,2,3].map(i => <Skeleton key={i} className="h-24 rounded-2xl" />)}</div>
        ) : currentList.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-2xl">
            <Shield className="h-12 w-12 text-slate-300 mx-auto mb-4" />
            <p className="text-slate-600">No {activeTab} verifications found</p>
          </div>
        ) : activeTab === "tenants" ? (
          <div className="space-y-4 pb-24">
            {filteredTenants.map((v) => (
              <div key={v.id} className="bg-white rounded-2xl p-4 shadow-sm cursor-pointer" onClick={() => { setSelectedTenant(v); setSelectedLandlord(null); setIsDetailOpen(true) }}>
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <p className="font-semibold text-slate-900">{v.userName}</p>
                    <p className="text-sm text-slate-500">{v.userEmail}</p>
                  </div>
                  {getStatusBadge(v.status)}
                </div>
                <div className="flex items-center gap-4 text-sm text-slate-500">
                  <span className="flex items-center gap-1"><IdCard className="h-4 w-4" />{v.idType ? `${v.idType} • ${v.idNumber}` : v.university || "—"}</span>
                  <span className="flex items-center gap-1"><Calendar className="h-4 w-4" />{new Date(v.createdAt).toLocaleDateString()}</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="space-y-4 pb-24">
            {filteredLandlords.map((v) => (
              <div key={v.id} className="bg-white rounded-2xl p-4 shadow-sm cursor-pointer" onClick={() => { setSelectedLandlord(v); setSelectedTenant(null); setIsDetailOpen(true) }}>
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <p className="font-semibold text-slate-900">{v.userName}</p>
                    <p className="text-sm text-slate-500">{v.userEmail}</p>
                  </div>
                  {getStatusBadge(v.identityStatus)}
                </div>
                <div className="flex items-center gap-4 text-sm text-slate-500">
                  <span className="flex items-center gap-1"><Building2 className="h-4 w-4" />{v.idType}</span>
                  <span className="flex items-center gap-1"><Calendar className="h-4 w-4" />{new Date(v.createdAt).toLocaleDateString()}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Tenant Verification Detail Sheet */}
      <Sheet open={isDetailOpen && selectedTenant !== null} onOpenChange={(open) => { if (!open) { setIsDetailOpen(false); setSelectedTenant(null) } }}>
        <SheetContent side="bottom" className="h-[80vh] rounded-t-3xl">
          <SheetHeader><SheetTitle>Tenant Verification Details</SheetTitle></SheetHeader>
          {selectedTenant && (
            <div className="mt-4 space-y-4">
              <div className="flex justify-between items-start">
                <div>
                  <h2 className="text-xl font-bold">{selectedTenant.userName}</h2>
                  <p className="text-slate-500">{selectedTenant.userEmail}</p>
                </div>
                {getStatusBadge(selectedTenant.status)}
              </div>
              <div className="bg-slate-50 rounded-xl p-4 space-y-2">
                {selectedTenant.idType ? (
                  <>
                    <p><span className="text-slate-500">ID Type:</span> {selectedTenant.idType.replace(/_/g, " ")}</p>
                    <p><span className="text-slate-500">ID Number:</span> {selectedTenant.idNumber}</p>
                  </>
                ) : (
                  <>
                    <p><span className="text-slate-500">University (legacy):</span> {selectedTenant.university || "—"}</p>
                    <p><span className="text-slate-500">Campus / student ref (legacy):</span> {selectedTenant.studentId || "—"}</p>
                  </>
                )}
                <p><span className="text-slate-500">Submitted:</span> {new Date(selectedTenant.createdAt).toLocaleString()}</p>
              </div>
              {(selectedTenant.idPhotoUrl || selectedTenant.studentIdPhotoUrl) && (
                <div className="space-y-3">
                  <div className="bg-slate-50 rounded-xl p-4">
                    <p className="text-sm font-medium text-slate-700 mb-2">ID Document</p>
                    <div className="relative">
                      <img 
                        src={selectedTenant.idPhotoUrl || selectedTenant.studentIdPhotoUrl} 
                        alt="ID Document" 
                        className="w-full h-64 object-contain rounded-lg border border-slate-200 bg-slate-50"
                      />
                      <a 
                        href={selectedTenant.idPhotoUrl || selectedTenant.studentIdPhotoUrl} 
                        target="_blank" 
                        rel="noopener noreferrer" 
                        className="absolute top-2 right-2 bg-white/90 backdrop-blur-sm p-2 rounded-lg shadow-sm flex items-center gap-1 text-xs text-slate-600 hover:bg-white"
                      >
                        <ExternalLink className="h-4 w-4" /> Open
                      </a>
                    </div>
                  </div>
                </div>
              )}
              {selectedTenant.selfiePhotoUrl && (
                <div className="space-y-3">
                  <div className="bg-slate-50 rounded-xl p-4">
                    <p className="text-sm font-medium text-slate-700 mb-2">Selfie</p>
                    <div className="relative">
                      <img 
                        src={selectedTenant.selfiePhotoUrl} 
                        alt="Selfie" 
                        className="w-full h-64 object-contain rounded-lg border border-slate-200 bg-slate-50"
                      />
                      <a 
                        href={selectedTenant.selfiePhotoUrl} 
                        target="_blank" 
                        rel="noopener noreferrer" 
                        className="absolute top-2 right-2 bg-white/90 backdrop-blur-sm p-2 rounded-lg shadow-sm flex items-center gap-1 text-xs text-slate-600 hover:bg-white"
                      >
                        <ExternalLink className="h-4 w-4" /> Open
                      </a>
                    </div>
                  </div>
                </div>
              )}
              {selectedTenant.rejectionReason && (
                <div className="bg-red-50 rounded-xl p-4">
                  <p className="text-sm text-red-600"><strong>Rejection Reason:</strong> {selectedTenant.rejectionReason}</p>
                </div>
              )}
              {selectedTenant.status === "PENDING" && (
                <div className="space-y-3 pt-4 border-t">
                  <Input placeholder="Rejection reason (if rejecting)" value={rejectionReason} onChange={(e) => setRejectionReason(e.target.value)} className="rounded-xl" />
                  <div className="flex gap-2">
                    <Button variant="outline" className="flex-1 rounded-xl border-red-200 text-red-600" onClick={() => handleTenantAction(selectedTenant.id, false)}>
                      <XCircle className="h-4 w-4 mr-1" /> Reject
                    </Button>
                    <Button className="flex-1 rounded-xl bg-green-600 hover:bg-green-700" onClick={() => handleTenantAction(selectedTenant.id, true)}>
                      <CheckCircle className="h-4 w-4 mr-1" /> Approve
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>

      {/* Landlord Verification Detail Sheet */}
      <Sheet open={isDetailOpen && selectedLandlord !== null} onOpenChange={(open) => { if (!open) { setIsDetailOpen(false); setSelectedLandlord(null) } }}>
        <SheetContent side="bottom" className="h-[80vh] rounded-t-3xl">
          <SheetHeader><SheetTitle>Landlord Verification Details</SheetTitle></SheetHeader>
          {selectedLandlord && (
            <div className="mt-4 space-y-4">
              <div className="flex justify-between items-start">
                <div>
                  <h2 className="text-xl font-bold">{selectedLandlord.userName}</h2>
                  <p className="text-slate-500">{selectedLandlord.userEmail}</p>
                </div>
                {getStatusBadge(selectedLandlord.identityStatus)}
              </div>
              <div className="bg-slate-50 rounded-xl p-4 space-y-2">
                <p><span className="text-slate-500">ID Type:</span> {selectedLandlord.idType}</p>
                <p><span className="text-slate-500">ID Number:</span> {selectedLandlord.idNumber}</p>
                <p><span className="text-slate-500">Submitted:</span> {new Date(selectedLandlord.createdAt).toLocaleString()}</p>
              </div>
              {selectedLandlord.idFrontPhotoUrl && (
                <div className="space-y-3">
                  <div className="bg-slate-50 rounded-xl p-4">
                    <p className="text-sm font-medium text-slate-700 mb-2">ID Front Photo</p>
                    <div className="relative">
                      <img 
                        src={selectedLandlord.idFrontPhotoUrl} 
                        alt="ID Front Photo" 
                        className="w-full h-64 object-contain rounded-lg border border-slate-200 bg-slate-50"
                      />
                      <a 
                        href={selectedLandlord.idFrontPhotoUrl} 
                        target="_blank" 
                        rel="noopener noreferrer" 
                        className="absolute top-2 right-2 bg-white/90 backdrop-blur-sm p-2 rounded-lg shadow-sm flex items-center gap-1 text-xs text-slate-600 hover:bg-white"
                      >
                        <ExternalLink className="h-4 w-4" /> Open
                      </a>
                    </div>
                  </div>
                </div>
              )}
              {selectedLandlord.selfieWithIdUrl && (
                <div className="space-y-3">
                  <div className="bg-slate-50 rounded-xl p-4">
                    <p className="text-sm font-medium text-slate-700 mb-2">Selfie with ID</p>
                    <div className="relative">
                      <img 
                        src={selectedLandlord.selfieWithIdUrl} 
                        alt="Selfie with ID" 
                        className="w-full h-64 object-contain rounded-lg border border-slate-200 bg-slate-50"
                      />
                      <a 
                        href={selectedLandlord.selfieWithIdUrl} 
                        target="_blank" 
                        rel="noopener noreferrer" 
                        className="absolute top-2 right-2 bg-white/90 backdrop-blur-sm p-2 rounded-lg shadow-sm flex items-center gap-1 text-xs text-slate-600 hover:bg-white"
                      >
                        <ExternalLink className="h-4 w-4" /> Open
                      </a>
                    </div>
                  </div>
                </div>
              )}
              {selectedLandlord.identityRejectionReason && (
                <div className="bg-red-50 rounded-xl p-4">
                  <p className="text-sm text-red-600"><strong>Rejection Reason:</strong> {selectedLandlord.identityRejectionReason}</p>
                </div>
              )}
              {selectedLandlord.identityStatus === "PENDING" && (
                <div className="space-y-3 pt-4 border-t">
                  <Input placeholder="Rejection reason (if rejecting)" value={rejectionReason} onChange={(e) => setRejectionReason(e.target.value)} className="rounded-xl" />
                  <div className="flex gap-2">
                    <Button variant="outline" className="flex-1 rounded-xl border-red-200 text-red-600" onClick={() => handleLandlordAction(selectedLandlord.id, false)}>
                      <XCircle className="h-4 w-4 mr-1" /> Reject
                    </Button>
                    <Button className="flex-1 rounded-xl bg-green-600 hover:bg-green-700" onClick={() => handleLandlordAction(selectedLandlord.id, true)}>
                      <CheckCircle className="h-4 w-4 mr-1" /> Approve
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
