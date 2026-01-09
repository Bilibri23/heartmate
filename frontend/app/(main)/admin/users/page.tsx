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
import { Textarea } from "@/components/ui/textarea"
import api from "@/lib/api"
import { 
  Search,
  UserCheck,
  UserX,
  Shield,
  Ban,
  CheckCircle,
  MoreVertical,
  ChevronLeft,
  ChevronRight
} from "lucide-react"

interface User {
  id: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  role: "STUDENT" | "LANDLORD" | "ADMIN"
  status: "ACTIVE" | "SUSPENDED" | "DELETED"
  verified: boolean
  profilePhotoUrl?: string
  createdAt: string
}

const ROLE_STYLES = {
  STUDENT: "bg-blue-100 text-blue-700",
  LANDLORD: "bg-green-100 text-green-700",
  ADMIN: "bg-purple-100 text-purple-700",
}

const STATUS_STYLES = {
  ACTIVE: "bg-green-100 text-green-700",
  SUSPENDED: "bg-red-100 text-red-700",
  DELETED: "bg-slate-100 text-slate-700",
}

export default function AdminUsersPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const router = useRouter()
  
  const [users, setUsers] = useState<User[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [roleFilter, setRoleFilter] = useState<string>("all")
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  
  const [suspendDialogOpen, setSuspendDialogOpen] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [suspendReason, setSuspendReason] = useState("")

  useEffect(() => {
    if (user?.role !== "ADMIN") {
      router.push("/for-you")
    }
  }, [user, router])

  const fetchUsers = useCallback(async () => {
    setIsLoading(true)
    try {
      const params: any = { page, size: 20 }
      if (searchQuery) params.search = searchQuery
      if (roleFilter !== "all") params.role = roleFilter
      if (statusFilter !== "all") params.status = statusFilter
      
      const response = await api.get("/admin/users", { params })
      setUsers(response.data?.content || [])
      setTotalPages(response.data?.totalPages || 0)
    } catch (err) {
      console.error("Failed to fetch users:", err)
      setUsers([])
    } finally {
      setIsLoading(false)
    }
  }, [page, searchQuery, roleFilter, statusFilter])

  useEffect(() => {
    fetchUsers()
  }, [fetchUsers])

  const handleSuspend = async () => {
    if (!selectedUser) return
    
    try {
      await api.post(`/admin/users/${selectedUser.id}/suspend`, {
        reason: suspendReason,
        duration: "INDEFINITE"
      })
      setSuspendDialogOpen(false)
      setSelectedUser(null)
      setSuspendReason("")
      fetchUsers()
    } catch (err) {
      console.error("Failed to suspend user:", err)
    }
  }

  const handleActivate = async (userId: string) => {
    try {
      await api.post(`/admin/users/${userId}/activate`)
      fetchUsers()
    } catch (err) {
      console.error("Failed to activate user:", err)
    }
  }

  if (user?.role !== "ADMIN") {
    return null
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">
      <MobileHeader title="User Management" showBack />

      {/* Search and Filters */}
      <div className="bg-white border-b border-slate-100 p-4 space-y-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
          <Input
            placeholder="Search users..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10 rounded-xl"
          />
        </div>
        
        <div className="flex gap-2">
          <Select value={roleFilter} onValueChange={setRoleFilter}>
            <SelectTrigger className="flex-1 rounded-xl">
              <SelectValue placeholder="Role" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Roles</SelectItem>
              <SelectItem value="STUDENT">Students</SelectItem>
              <SelectItem value="LANDLORD">Landlords</SelectItem>
              <SelectItem value="ADMIN">Admins</SelectItem>
            </SelectContent>
          </Select>
          
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="flex-1 rounded-xl">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Status</SelectItem>
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="SUSPENDED">Suspended</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* User List */}
      <div className="flex-1 overflow-y-auto p-4 pb-24">
        {isLoading ? (
          <div className="space-y-4">
            <Skeleton className="h-20 rounded-2xl" />
            <Skeleton className="h-20 rounded-2xl" />
            <Skeleton className="h-20 rounded-2xl" />
          </div>
        ) : users.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-slate-500">No users found</p>
          </div>
        ) : (
          <div className="space-y-3">
            {users.map((u) => (
              <div key={u.id} className="bg-white rounded-2xl p-4 shadow-sm">
                <div className="flex items-start gap-3">
                  <Avatar className="h-12 w-12">
                    <AvatarImage src={u.profilePhotoUrl} />
                    <AvatarFallback className="bg-blue-100 text-blue-600">
                      {u.firstName[0]}{u.lastName[0]}
                    </AvatarFallback>
                  </Avatar>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-slate-900 truncate">
                        {u.firstName} {u.lastName}
                      </p>
                      {u.verified && (
                        <Shield className="h-4 w-4 text-blue-600 flex-shrink-0" />
                      )}
                    </div>
                    <p className="text-sm text-slate-500 truncate">{u.email}</p>
                    <div className="flex items-center gap-2 mt-2">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${ROLE_STYLES[u.role]}`}>
                        {u.role}
                      </span>
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[u.status]}`}>
                        {u.status}
                      </span>
                    </div>
                  </div>
                  
                  <div className="flex flex-col gap-1">
                    {u.status === "ACTIVE" ? (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-red-600 hover:bg-red-50"
                        onClick={() => {
                          setSelectedUser(u)
                          setSuspendDialogOpen(true)
                        }}
                      >
                        <Ban className="h-4 w-4" />
                      </Button>
                    ) : u.status === "SUSPENDED" ? (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-green-600 hover:bg-green-50"
                        onClick={() => handleActivate(u.id)}
                      >
                        <CheckCircle className="h-4 w-4" />
                      </Button>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-4 mt-6">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded-xl"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-sm text-slate-600">
              Page {page + 1} of {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="rounded-xl"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        )}
      </div>

      {/* Suspend Dialog */}
      <AlertDialog open={suspendDialogOpen} onOpenChange={setSuspendDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Suspend User</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to suspend {selectedUser?.firstName} {selectedUser?.lastName}? 
              They will not be able to access their account.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <Textarea
            placeholder="Reason for suspension..."
            value={suspendReason}
            onChange={(e) => setSuspendReason(e.target.value)}
            className="mt-2"
          />
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleSuspend}
              className="bg-red-600 hover:bg-red-700"
            >
              Suspend
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
