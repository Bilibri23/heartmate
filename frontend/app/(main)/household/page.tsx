"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/auth-context";
import householdService, { Household } from "@/services/household";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Home,
  Users,
  DollarSign,
  ClipboardList,
  AlertCircle,
  ChevronRight,
  Loader2,
  Search,
  Heart,
  FileText,
} from "lucide-react";
import { toast } from "sonner";

export default function HouseholdPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [households, setHouseholds] = useState<Household[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) {
      fetchHouseholds();
    }
  }, [user]);

  const fetchHouseholds = async () => {
    try {
      const data = await householdService.getMyHouseholds();
      setHouseholds(data);
    } catch (error) {
      console.error("Error fetching households:", error);
      toast.error("Failed to load households");
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("fr-CM", {
      style: "currency",
      currency: "XAF",
      minimumFractionDigits: 0,
    }).format(amount);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-6 max-w-4xl">
      <div
        className="mb-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950"
        role="note"
      >
        <strong className="font-semibold">Not part of the current MVP.</strong> RoomBay focuses on discovery,
        applications, and leases. Shared household expenses may change; use{" "}
        <Link href="/for-you" className="underline font-medium">
          For You
        </Link>{" "}
        and{" "}
        <Link href="/messages" className="underline font-medium">
          Messages
        </Link>{" "}
        for day-to-day coordination.
      </div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900">My Households</h1>
        <p className="text-slate-500 mt-1">
          Manage your shared living spaces with roommates (legacy / optional)
        </p>
      </div>

      {households.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Home className="h-12 w-12 mx-auto text-slate-300 mb-4" />
            <h3 className="text-lg font-medium text-slate-900 mb-2">
              No Households Yet
            </h3>
            <p className="text-slate-500 mb-6 max-w-md mx-auto">
              Households are automatically created when you and your matched roommate 
              sign a shared lease together. Here&apos;s how to get started:
            </p>
            
            <div className="grid gap-4 max-w-sm mx-auto text-left mb-6">
              <div className="flex items-start gap-3 p-3 bg-slate-50 rounded-lg">
                <div className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
                  <Search className="h-4 w-4 text-blue-600" />
                </div>
                <div>
                  <p className="font-medium text-slate-900 text-sm">1. Find Roommates</p>
                  <p className="text-xs text-slate-500">Browse and match with compatible roommates</p>
                </div>
              </div>
              
              <div className="flex items-start gap-3 p-3 bg-slate-50 rounded-lg">
                <div className="h-8 w-8 rounded-full bg-pink-100 flex items-center justify-center flex-shrink-0">
                  <Heart className="h-4 w-4 text-pink-600" />
                </div>
                <div>
                  <p className="font-medium text-slate-900 text-sm">2. Mutual Match</p>
                  <p className="text-xs text-slate-500">When both of you like each other, you&apos;re matched!</p>
                </div>
              </div>
              
              <div className="flex items-start gap-3 p-3 bg-slate-50 rounded-lg">
                <div className="h-8 w-8 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0">
                  <FileText className="h-4 w-4 text-green-600" />
                </div>
                <div>
                  <p className="font-medium text-slate-900 text-sm">3. Sign Shared Lease</p>
                  <p className="text-xs text-slate-500">Apply together for a listing and sign the lease</p>
                </div>
              </div>
              
              <div className="flex items-start gap-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
                <div className="h-8 w-8 rounded-full bg-blue-600 flex items-center justify-center flex-shrink-0">
                  <Home className="h-4 w-4 text-white" />
                </div>
                <div>
                  <p className="font-medium text-slate-900 text-sm">4. Household Created!</p>
                  <p className="text-xs text-slate-500">Manage expenses, tasks, and rules together</p>
                </div>
              </div>
            </div>
            
            <Link href="/matches">
              <Button>
                <Users className="h-4 w-4 mr-2" />
                Find Roommates
              </Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {households.map((household) => (
            <Card
              key={household.id}
              className="cursor-pointer hover:shadow-md transition-shadow"
              onClick={() => router.push(`/household/${household.id}`)}
            >
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-full bg-blue-100 flex items-center justify-center">
                      <Home className="h-6 w-6 text-blue-600" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-slate-900">
                        {household.name}
                      </h3>
                      {household.listingTitle && (
                        <p className="text-sm text-slate-500">
                          {household.listingTitle}
                        </p>
                      )}
                      <div className="flex items-center gap-1 mt-1">
                        <Users className="h-3 w-3 text-slate-400" />
                        <span className="text-xs text-slate-500">
                          {household.members.length} member
                          {household.members.length !== 1 ? "s" : ""}
                        </span>
                      </div>
                    </div>
                  </div>
                  <ChevronRight className="h-5 w-5 text-slate-400" />
                </div>

                <div className="mt-4 grid grid-cols-4 gap-2">
                  <div className="text-center p-2 bg-slate-50 rounded-lg">
                    <DollarSign className="h-4 w-4 mx-auto text-green-600 mb-1" />
                    <p className="text-xs text-slate-500">This Month</p>
                    <p className="text-sm font-medium">
                      {formatCurrency(household.totalExpensesThisMonth)}
                    </p>
                  </div>
                  <div className="text-center p-2 bg-slate-50 rounded-lg">
                    <DollarSign className="h-4 w-4 mx-auto text-red-500 mb-1" />
                    <p className="text-xs text-slate-500">You Owe</p>
                    <p className="text-sm font-medium text-red-600">
                      {formatCurrency(household.myBalanceOwed)}
                    </p>
                  </div>
                  <div className="text-center p-2 bg-slate-50 rounded-lg">
                    <ClipboardList className="h-4 w-4 mx-auto text-blue-600 mb-1" />
                    <p className="text-xs text-slate-500">Tasks</p>
                    <p className="text-sm font-medium">
                      {household.openTasksCount}
                    </p>
                  </div>
                  <div className="text-center p-2 bg-slate-50 rounded-lg">
                    <AlertCircle className="h-4 w-4 mx-auto text-orange-500 mb-1" />
                    <p className="text-xs text-slate-500">Issues</p>
                    <p className="text-sm font-medium">
                      {household.openIssuesCount}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
