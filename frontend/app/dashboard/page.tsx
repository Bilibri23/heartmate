"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/context/auth-context";
import { listingService, Listing } from "@/services/listing.service";
import { messageService } from "@/services/message.service";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Plus, Home, MessageSquare, TrendingUp, Users } from "lucide-react";
import Link from "next/link";
import { formatCurrency } from "@/lib/utils";

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState<any>(null);
  const [activeListings, setActiveListings] = useState<Listing[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      if (!user) return;
      setIsLoading(true);
      try {
        const unread = await messageService.getUnreadCount();
        setUnreadCount(unread);

        if (user.role === "LANDLORD") {
          const statistics = await listingService.getLandlordStatistics(user.id);
          const listings = await listingService.getLandlordListings(user.id);
          setStats(statistics);
          setActiveListings(listings.filter(l => l.status === 'ACTIVE'));
        } else {
          // Tenant logic: maybe get favorites or recommended
          // const favorites = await listingService.getFavorites(user.id);
          // setActiveListings(favorites); 
        }
      } catch (error) {
        console.error("Error fetching dashboard data:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [user]);

  if (!user) return null;

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground">
            Welcome back, {user.firstName}! Here&apos;s an overview of your account.
          </p>
        </div>
        {user.role === "LANDLORD" && (
          <Button asChild>
            <Link href="/listings/create">
              <Plus className="mr-2 h-4 w-4" /> Add Property
            </Link>
          </Button>
        )}
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Unread Messages</CardTitle>
            <MessageSquare className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{unreadCount}</div>
            <p className="text-xs text-muted-foreground">
              Across all conversations
            </p>
          </CardContent>
        </Card>
        
        {user.role === "LANDLORD" ? (
          <>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Active Listings</CardTitle>
                <Home className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{activeListings.length}</div>
                <p className="text-xs text-muted-foreground">
                  Properties currently listed
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Views</CardTitle>
                <Users className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats?.totalViews || 0}</div>
                <p className="text-xs text-muted-foreground">
                  +19% from last month
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Revenue</CardTitle>
                <TrendingUp className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">$0.00</div>
                <p className="text-xs text-muted-foreground">
                  Projected monthly revenue
                </p>
              </CardContent>
            </Card>
          </>
        ) : (
          <>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Saved Homes</CardTitle>
                <Home className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">0</div>
                <p className="text-xs text-muted-foreground">
                  Properties you&apos;ve favorited
                </p>
              </CardContent>
            </Card>
          </>
        )}
      </div>

      {/* Recent Activity / Listings */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
        <Card className="col-span-4">
          <CardHeader>
            <CardTitle>{user.role === "LANDLORD" ? "Your Properties" : "Recommended for You"}</CardTitle>
            <CardDescription>
              {user.role === "LANDLORD" 
                ? "Manage your active property listings." 
                : "Properties that match your preferences."}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {activeListings.length > 0 ? (
              <div className="space-y-4">
                {activeListings.slice(0, 5).map((listing) => (
                  <div key={listing.id} className="flex items-center gap-4 rounded-md border p-4">
                    <div className="h-12 w-12 rounded bg-muted">
                        {/* Image placeholder */}
                        {listing.images && listing.images.length > 0 && (
                            <img src={listing.images[0]} alt={listing.title} className="h-full w-full object-cover rounded" />
                        )}
                    </div>
                    <div className="flex-1">
                      <h3 className="font-medium">{listing.title}</h3>
                      <p className="text-sm text-muted-foreground">{listing.address}</p>
                    </div>
                    <div className="font-bold">{formatCurrency(listing.price)}</div>
                    <Button variant="ghost" size="sm" asChild>
                        <Link href={`/listings/${listing.id}`}>View</Link>
                    </Button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex h-[200px] items-center justify-center rounded-md border border-dashed text-muted-foreground">
                No listings found.
              </div>
            )}
          </CardContent>
        </Card>
        
        <Card className="col-span-3">
          <CardHeader>
            <CardTitle>Recent Messages</CardTitle>
            <CardDescription>
              Your latest conversations.
            </CardDescription>
          </CardHeader>
          <CardContent>
             <div className="flex h-[200px] items-center justify-center rounded-md border border-dashed text-muted-foreground">
                No recent messages.
             </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
