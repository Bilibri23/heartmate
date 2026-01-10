"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { messageService, Conversation } from "@/services/message.service";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import { formatDistanceToNow } from "date-fns"; // I might need to install date-fns or use a utility
import { Loader2 } from "lucide-react";

// Simple time formatter if date-fns is not desired, but let's assume we can use a simple function or install date-fns
function formatTime(dateString: string) {
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  } catch (e) {
    return "";
  }
}

export default function MessagesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchConversations = async () => {
      try {
        const data = await messageService.getConversations();
        setConversations(data);
      } catch (error) {
        console.error("Failed to fetch conversations", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchConversations();
    
    // Optional: Poll for new conversations or updates
    const interval = setInterval(fetchConversations, 30000);
    return () => clearInterval(interval);
  }, []);

  const isMobileChatOpen = pathname.includes("/messages/") && pathname !== "/messages";

  return (
    <div className="flex h-[calc(100vh-4rem)] overflow-hidden">
      {/* Sidebar - Conversation List */}
      <div 
        className={cn(
          "w-full md:w-80 border-r bg-background flex flex-col transition-all",
          isMobileChatOpen ? "hidden md:flex" : "flex"
        )}
      >
        <div className="p-4 border-b font-semibold text-lg">Messages</div>
        <div className="flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="flex justify-center p-4">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : conversations.length === 0 ? (
            <div className="p-4 text-center text-muted-foreground text-sm">
              No conversations yet.
            </div>
          ) : (
            <div className="flex flex-col">
              {conversations.map((conversation) => (
                <Link
                  key={conversation.userId}
                  href={`/messages/${conversation.userId}`}
                  className={cn(
                    "flex items-center gap-3 p-4 hover:bg-muted/50 transition-colors border-b last:border-0",
                    pathname === `/messages/${conversation.userId}` && "bg-muted"
                  )}
                >
                  <Avatar>
                    <AvatarImage src={conversation.userAvatar} />
                    <AvatarFallback>{(conversation.userName || "U").substring(0, 2).toUpperCase()}</AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-baseline mb-1">
                      <span className="font-medium truncate">{conversation.userName || "Unknown User"}</span>
                      <span className="text-xs text-muted-foreground flex-shrink-0">
                        {formatTime(conversation.lastMessageTime)}
                      </span>
                    </div>
                    <p className={cn(
                      "text-sm truncate",
                      conversation.unreadCount > 0 ? "font-semibold text-foreground" : "text-muted-foreground"
                    )}>
                      {conversation.lastMessage}
                    </p>
                  </div>
                  {conversation.unreadCount > 0 && (
                    <div className="h-2 w-2 rounded-full bg-primary flex-shrink-0" />
                  )}
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Main Content - Chat Window */}
      <div className={cn(
        "flex-1 flex flex-col bg-muted/20",
        !isMobileChatOpen ? "hidden md:flex" : "flex"
      )}>
        {children}
      </div>
    </div>
  );
}
