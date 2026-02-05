"use client";

import { useEffect, useState, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { messageService, Message, SharedListingInfo } from "@/services/message.service";
import { profileService } from "@/services/profile.service";
import { listingService, Listing } from "@/services/listing.service";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { ScrollArea } from "@/components/ui/scroll-area";
import { ArrowLeft, Send, Loader2, Home, MapPin, Share2, ExternalLink, X } from "lucide-react";
import { useAuth } from "@/context/auth-context";
import { cn } from "@/lib/utils";

export default function ChatPage() {
  const params = useParams();
  const userId = params.userId as string;
  const { user: currentUser } = useAuth();
  const router = useRouter();
  
  const [messages, setMessages] = useState<Message[]>([]);
  const [otherUser, setOtherUser] = useState<{ name: string; avatarUrl?: string | null } | null>(null);
  const [newMessage, setNewMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [showListingPicker, setShowListingPicker] = useState(false);
  const [listings, setListings] = useState<Listing[]>([]);
  const [loadingListings, setLoadingListings] = useState(false);
  const [selectedListing, setSelectedListing] = useState<Listing | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    const initChat = async () => {
      if (!userId || !currentUser) return;
      
      setIsLoading(true);
      try {
        // Fetch conversation history
        const conversationData = await messageService.getConversation(userId);
        setMessages(conversationData.messages || []); // Adjust based on actual API response structure
        
        // Try to resolve participant name/avatar from conversation list
        try {
          const conversations = await messageService.getConversations();
          const match = conversations.find((conv) => conv.userId === userId);
          if (match) {
            setOtherUser({ name: match.userName, avatarUrl: match.userAvatar });
          }
        } catch {
          // Ignore and fallback to profile only
        }

        if (!otherUser) {
          try {
            const profile = await profileService.getProfile(userId);
            setOtherUser({ name: "User", avatarUrl: profile.profilePhotoUrl || null });
          } catch {
            setOtherUser({ name: "User", avatarUrl: null });
          }
        }
        
        // Mark as read
        await messageService.markAsRead(userId);
      } catch (error) {
        console.error("Error loading chat", error);
      } finally {
        setIsLoading(false);
      }
    };

    initChat();
    
    const interval = setInterval(async () => {
        if (!userId) return;
        try {
            const conversationData = await messageService.getConversation(userId);
            // Simple merge or replace strategy
            // Ideally backend sends only new messages or we handle it smarter
            setMessages(conversationData.messages || []);
        } catch (e) {
            // ignore silent update errors
        }
    }, 5000); // Poll every 5s

    return () => clearInterval(interval);
  }, [userId, currentUser]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const fetchListings = async () => {
    setLoadingListings(true);
    try {
      const response = await listingService.searchListings({ size: 20, status: 'ACTIVE' });
      const listingsData = response?.content || response || [];
      setListings(listingsData);
    } catch (error) {
      console.error("Failed to fetch listings", error);
    } finally {
      setLoadingListings(false);
    }
  };

  const handleOpenListingPicker = () => {
    setShowListingPicker(true);
    if (listings.length === 0) {
      fetchListings();
    }
  };

  const handleSelectListing = (listing: Listing) => {
    setSelectedListing(listing);
    setShowListingPicker(false);
  };

  const handleClearSelectedListing = () => {
    setSelectedListing(null);
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if ((!newMessage.trim() && !selectedListing) || !userId) return;

    setIsSending(true);
    try {
      const content = selectedListing 
        ? (newMessage.trim() || `Check out this listing: ${selectedListing.title}`)
        : newMessage;
      
      const sentMessage = await messageService.sendMessage(
        userId, 
        content,
        selectedListing?.id
      );
      setMessages((prev) => [...prev, sentMessage]);
      setNewMessage("");
      setSelectedListing(null);
    } catch (error) {
      console.error("Failed to send message", error);
    } finally {
      setIsSending(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("fr-CM", {
      style: "currency",
      currency: "XAF",
      minimumFractionDigits: 0,
    }).format(amount);
  };

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-background">
      {/* Chat Header */}
      <div className="flex items-center gap-3 p-4 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <Button variant="ghost" size="icon" className="md:hidden" onClick={() => router.push("/messages")} aria-label="Back">
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <Avatar>
          <AvatarImage src={otherUser?.avatarUrl || undefined} />
          <AvatarFallback>{otherUser?.name?.substring(0, 1) || "?"}</AvatarFallback>
        </Avatar>
        <div>
          <h2 className="font-semibold">{otherUser?.name || "User"}</h2>
          <p className="text-xs text-muted-foreground">Tap to start a conversation</p>
        </div>
      </div>

      {/* Messages List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.length === 0 && (
          <div className="text-center text-sm text-muted-foreground py-10">
            No messages yet. Say hello 👋
          </div>
        )}
        {messages.map((msg) => {
          const isMe = msg.senderId === currentUser?.id;
          const hasSharedListing = msg.messageType === 'LISTING_SHARE' && msg.sharedListing;
          
          return (
            <div key={msg.id} className={cn("flex items-end gap-2", isMe ? "justify-end" : "justify-start")}>
              {!isMe && (
                <Avatar className="h-7 w-7">
                  <AvatarImage src={otherUser?.avatarUrl || undefined} />
                  <AvatarFallback>{otherUser?.name?.substring(0, 1) || "?"}</AvatarFallback>
                </Avatar>
              )}
              <div
                className={cn(
                  "max-w-[80%] rounded-2xl shadow-sm overflow-hidden",
                  isMe
                    ? "bg-primary text-primary-foreground rounded-br-md"
                    : "bg-muted rounded-bl-md"
                )}
              >
                {/* Shared Listing Card */}
                {hasSharedListing && msg.sharedListing && (
                  <Link href={`/listings/${msg.sharedListing.id}`} className="block">
                    <div className={cn(
                      "border-b",
                      isMe ? "border-primary-foreground/20" : "border-border"
                    )}>
                      {msg.sharedListing.primaryPhotoUrl ? (
                        <img 
                          src={msg.sharedListing.primaryPhotoUrl} 
                          alt={msg.sharedListing.title}
                          className="w-full h-32 object-cover"
                        />
                      ) : (
                        <div className={cn(
                          "w-full h-32 flex items-center justify-center",
                          isMe ? "bg-primary-foreground/10" : "bg-slate-100"
                        )}>
                          <Home className={cn("h-10 w-10", isMe ? "text-primary-foreground/50" : "text-slate-300")} />
                        </div>
                      )}
                      <div className="p-3">
                        <div className="flex items-center justify-between gap-2">
                          <h4 className="font-semibold text-sm truncate">{msg.sharedListing.title}</h4>
                          <ExternalLink className={cn("h-3.5 w-3.5 flex-shrink-0", isMe ? "text-primary-foreground/70" : "text-muted-foreground")} />
                        </div>
                        <div className={cn("flex items-center gap-1 mt-1 text-xs", isMe ? "text-primary-foreground/80" : "text-muted-foreground")}>
                          <MapPin className="h-3 w-3" />
                          <span className="truncate">{msg.sharedListing.address}</span>
                        </div>
                        <p className={cn("font-bold mt-1", isMe ? "text-primary-foreground" : "text-green-600")}>
                          {formatCurrency(msg.sharedListing.rentAmount)}/mo
                        </p>
                      </div>
                    </div>
                  </Link>
                )}
                
                {/* Message Content */}
                <div className="px-3 py-2">
                  <p className="whitespace-pre-wrap text-sm">{msg.content}</p>
                  <span className={cn("mt-1 block text-[10px]", isMe ? "text-primary-foreground/70" : "text-muted-foreground")}>
                    {new Date(msg.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="border-t bg-background">
        {/* Selected Listing Preview */}
        {selectedListing && (
          <div className="p-2 border-b bg-slate-50">
            <div className="flex items-center gap-3 p-2 bg-white rounded-lg border">
              {(selectedListing.primaryPhotoUrl || selectedListing.images?.[0]) ? (
                <img 
                  src={selectedListing.primaryPhotoUrl || selectedListing.images?.[0]} 
                  alt={selectedListing.title}
                  className="w-12 h-12 object-cover rounded"
                />
              ) : (
                <div className="w-12 h-12 bg-slate-100 rounded flex items-center justify-center">
                  <Home className="h-5 w-5 text-slate-400" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                <p className="font-medium text-sm truncate">{selectedListing.title}</p>
                <p className="text-xs text-muted-foreground truncate">{selectedListing.address}</p>
              </div>
              <Button 
                variant="ghost" 
                size="icon" 
                className="h-8 w-8 flex-shrink-0"
                onClick={handleClearSelectedListing}
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
        
        <div className="p-4">
          <form onSubmit={handleSendMessage} className="flex gap-2">
            {/* Share Listing Button */}
            <Sheet open={showListingPicker} onOpenChange={setShowListingPicker}>
              <SheetTrigger asChild>
                <Button 
                  type="button" 
                  variant="outline" 
                  size="icon"
                  onClick={handleOpenListingPicker}
                  className="flex-shrink-0"
                  aria-label="Share a listing"
                >
                  <Share2 className="h-4 w-4" />
                </Button>
              </SheetTrigger>
              <SheetContent side="bottom" className="h-[70vh]">
                <SheetHeader>
                  <SheetTitle>Share a Listing</SheetTitle>
                </SheetHeader>
                <ScrollArea className="h-full mt-4 pb-8">
                  {loadingListings ? (
                    <div className="flex items-center justify-center py-10">
                      <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                    </div>
                  ) : listings.length === 0 ? (
                    <div className="text-center py-10 text-muted-foreground">
                      <Home className="h-10 w-10 mx-auto mb-2 text-slate-300" />
                      <p>No listings available</p>
                    </div>
                  ) : (
                    <div className="grid gap-3 pr-4">
                      {listings.map((listing) => (
                        <button
                          key={listing.id}
                          type="button"
                          onClick={() => handleSelectListing(listing)}
                          className="flex items-center gap-3 p-3 rounded-lg border hover:bg-slate-50 transition-colors text-left w-full"
                        >
                          {(listing.primaryPhotoUrl || listing.images?.[0]) ? (
                            <img 
                              src={listing.primaryPhotoUrl || listing.images?.[0]}
                              alt={listing.title}
                              className="w-16 h-16 object-cover rounded-lg"
                            />
                          ) : (
                            <div className="w-16 h-16 bg-slate-100 rounded-lg flex items-center justify-center">
                              <Home className="h-6 w-6 text-slate-400" />
                            </div>
                          )}
                          <div className="flex-1 min-w-0">
                            <h4 className="font-medium text-sm truncate">{listing.title}</h4>
                            <div className="flex items-center gap-1 text-xs text-muted-foreground mt-0.5">
                              <MapPin className="h-3 w-3" />
                              <span className="truncate">{listing.city}</span>
                            </div>
                            <p className="text-sm font-semibold text-green-600 mt-1">
                              {formatCurrency(listing.rentAmount || listing.price)}/mo
                            </p>
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                </ScrollArea>
              </SheetContent>
            </Sheet>

            <Input
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              placeholder={selectedListing ? "Add a message about this listing..." : "Type a message..."}
              className="flex-1"
              disabled={isSending}
            />
            <Button 
              type="submit" 
              size="icon" 
              disabled={isSending || (!newMessage.trim() && !selectedListing)} 
              aria-label="Send message"
            >
              <Send className="h-4 w-4" />
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
