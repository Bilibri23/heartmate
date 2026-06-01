"use client";

import { useAuth } from '@/context/auth-context';
import { useWebSocketNotifications } from '@/hooks/use-websocket-notifications';
import api from '@/lib/api';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Bell, Home, LayoutDashboard, LogOut, MessageSquare, Search, Settings, User } from 'lucide-react';

export function Navbar() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchUnreadCount = useCallback(async () => {
    if (!user?.id) return;
    try {
      const response = await api.get('/notifications/unread-count');
      const count = Number(response.data?.count ?? 0);
      setUnreadCount(Number.isFinite(count) ? count : 0);
    } catch (err) {
      console.error('Failed to fetch unread notifications:', err);
    }
  }, [user?.id]);

  useEffect(() => {
    fetchUnreadCount();
  }, [fetchUnreadCount]);

  useEffect(() => {
    if (!user?.id) return;
    const interval = setInterval(fetchUnreadCount, 60000);
    return () => clearInterval(interval);
  }, [fetchUnreadCount, user?.id]);

  const handleNotification = useCallback((notification: { title: string; message?: string; actionUrl?: string }) => {
    setUnreadCount((prev) => prev + 1);
    toast(notification.title, {
      description: notification.message,
      action: notification.actionUrl ? {
        label: 'View',
        onClick: () => router.push(notification.actionUrl!),
      } : undefined,
    });
  }, [router]);

  useWebSocketNotifications({
    enabled: Boolean(user?.id),
    onNotification: handleNotification,
  });

  if (!user) return null;

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b bg-background px-6">
      <Link href="/dashboard" className="flex items-center gap-2 font-semibold text-lg">
        <Home className="h-6 w-6" />
        <span className="">RoomBay</span>
      </Link>
      <nav className="hidden md:flex items-center gap-6 text-sm font-medium ml-6">
        <Link href="/dashboard" className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors">
          <LayoutDashboard className="h-4 w-4" />
          Dashboard
        </Link>
        <Link href="/listings" className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors">
          <Search className="h-4 w-4" />
          Find Home
        </Link>
        <Link href="/messages" className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors">
          <MessageSquare className="h-4 w-4" />
          Messages
        </Link>
      </nav>
      <div className="ml-auto flex items-center gap-4">
        <Button asChild variant="ghost" size="icon" className="relative">
          <Link href="/notifications" aria-label="Notifications">
            <Bell className="h-5 w-5" />
            {unreadCount > 0 && (
              <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </Link>
        </Button>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="rounded-full">
              <Avatar>
                <AvatarImage src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${user.firstName}`} />
                <AvatarFallback>{user.firstName[0]}{user.lastName[0]}</AvatarFallback>
              </Avatar>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>My Account</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
                <Link href="/profile" className="cursor-pointer w-full flex items-center">
                    <User className="mr-2 h-4 w-4" />
                    Profile
                </Link>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
                <Link href="/settings" className="cursor-pointer w-full flex items-center">
                    <Settings className="mr-2 h-4 w-4" />
                    Settings
                </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={logout} className="text-red-600 cursor-pointer">
              <LogOut className="mr-2 h-4 w-4" />
              Logout
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
