"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { useLanguage } from "@/context/language-context"
import { useAuth } from "@/context/auth-context"
import { MobileHeader } from "@/components/layout/mobile-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import api from "@/lib/api"
import { Search, DollarSign, CheckCircle, XCircle, Clock, User, Calendar, CreditCard, Building } from "lucide-react"

interface Payment {
  id: string
  leaseId: string
  amount: number
  status: string
  paymentMethod: string
  transactionId?: string
  payerName: string
  payerEmail: string
  recipientName: string
  listingTitle: string
  createdAt: string
  paidAt?: string
}

export default function AdminPaymentsPage() {
  const { formatCurrency } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  const [payments, setPayments] = useState<Payment[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [selected, setSelected] = useState<Payment | null>(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)

  const [authLoading, setAuthLoading] = useState(true)

  useEffect(() => {
    // Wait for auth to initialize before checking role
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
      const res = await api.get("/admin/payments/pending")
      setPayments(res.data?.content || res.data || [])
    } catch (err) { console.error(err) }
    finally { setIsLoading(false) }
  }

  const handleApprove = async (id: string) => {
    try {
      await api.post(`/admin/payments/${id}/verify`)
      fetchData()
      setIsDetailOpen(false)
    } catch (err) { console.error(err) }
  }

  const filtered = payments.filter(p => {
    const matchesSearch = p.payerName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.recipientName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.transactionId?.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesStatus = statusFilter === "all" || p.status === statusFilter.toUpperCase()
    return matchesSearch && matchesStatus
  })

  const getStatusBadge = (status: string) => {
    const styles: Record<string, string> = {
      COMPLETED: "bg-green-100 text-green-700",
      PENDING: "bg-amber-100 text-amber-700",
      FAILED: "bg-red-100 text-red-700",
      PENDING_VERIFICATION: "bg-blue-100 text-blue-700"
    }
    return <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[status] || "bg-slate-100"}`}>{status?.replace("_", " ")}</span>
  }

  const totalAmount = payments.reduce((sum, p) => sum + (p.status === "COMPLETED" ? p.amount : 0), 0)
  const pendingAmount = payments.reduce((sum, p) => sum + (p.status === "PENDING" || p.status === "PENDING_VERIFICATION" ? p.amount : 0), 0)

  if (user?.role !== "ADMIN") return null

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="Payments" showBack />
      <div className="p-4 space-y-4">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input placeholder="Search..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="pl-10 rounded-xl" />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-32 rounded-xl"><SelectValue placeholder="Status" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All</SelectItem>
              <SelectItem value="pending">Pending</SelectItem>
              <SelectItem value="pending_verification">Verifying</SelectItem>
              <SelectItem value="completed">Completed</SelectItem>
              <SelectItem value="failed">Failed</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="bg-white rounded-2xl p-4 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-green-100 flex items-center justify-center">
                <DollarSign className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-lg font-bold text-slate-900">{formatCurrency(totalAmount)}</p>
                <p className="text-xs text-slate-500">Total Received</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-2xl p-4 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center">
                <Clock className="h-5 w-5 text-amber-600" />
              </div>
              <div>
                <p className="text-lg font-bold text-slate-900">{formatCurrency(pendingAmount)}</p>
                <p className="text-xs text-slate-500">Pending</p>
              </div>
            </div>
          </div>
        </div>

        {isLoading ? (
          <div className="space-y-4">{[1,2,3].map(i => <Skeleton key={i} className="h-24 rounded-2xl" />)}</div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-2xl">
            <DollarSign className="h-12 w-12 text-slate-300 mx-auto mb-4" />
            <p className="text-slate-600">No payments found</p>
          </div>
        ) : (
          <div className="space-y-4 pb-24">
            {filtered.map((p) => (
              <div key={p.id} className="bg-white rounded-2xl p-4 shadow-sm cursor-pointer" onClick={() => { setSelected(p); setIsDetailOpen(true) }}>
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <p className="font-semibold text-slate-900">{formatCurrency(p.amount)}</p>
                    <p className="text-sm text-slate-500">{p.payerName} → {p.recipientName}</p>
                  </div>
                  {getStatusBadge(p.status)}
                </div>
                <div className="flex items-center gap-4 text-sm text-slate-500">
                  <span className="flex items-center gap-1"><CreditCard className="h-4 w-4" />{p.paymentMethod}</span>
                  <span className="flex items-center gap-1"><Calendar className="h-4 w-4" />{new Date(p.createdAt).toLocaleDateString()}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <Sheet open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <SheetContent side="bottom" className="h-[70vh] rounded-t-3xl">
          <SheetHeader><SheetTitle>Payment Details</SheetTitle></SheetHeader>
          {selected && (
            <div className="mt-4 space-y-4">
              <div className="flex justify-between items-start">
                <p className="text-2xl font-bold text-slate-900">{formatCurrency(selected.amount)}</p>
                {getStatusBadge(selected.status)}
              </div>
              <div className="bg-slate-50 rounded-xl p-4 space-y-2">
                <p><span className="text-slate-500">From:</span> {selected.payerName} ({selected.payerEmail})</p>
                <p><span className="text-slate-500">To:</span> {selected.recipientName}</p>
                <p><span className="text-slate-500">Listing:</span> {selected.listingTitle}</p>
                <p><span className="text-slate-500">Method:</span> {selected.paymentMethod}</p>
                {selected.transactionId && <p><span className="text-slate-500">Transaction ID:</span> {selected.transactionId}</p>}
                <p><span className="text-slate-500">Date:</span> {new Date(selected.createdAt).toLocaleString()}</p>
              </div>
              {selected.status === "PENDING_VERIFICATION" && (
                <Button className="w-full rounded-xl bg-green-600 hover:bg-green-700" onClick={() => handleApprove(selected.id)}>
                  <CheckCircle className="h-4 w-4 mr-2" /> Approve Payment
                </Button>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  )
}
