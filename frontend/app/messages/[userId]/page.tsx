"use client";

import { useEffect, useState, useRef } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { messageService, Message, Conversation } from "@/services/message.service";
import { profileService } from "@/services/profile.service";
import { listingService, Listing } from "@/services/listing.service";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { ScrollArea } from "@/components/ui/scroll-area";
import { ArrowLeft, Send, Loader2, Home, MapPin, Share2, ExternalLink, X, Pencil, Trash2, Check, MoreVertical, Search, MessageSquare } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/context/auth-context";
import { cn } from "@/lib/utils";

export default function ChatPage() {
  const params = useParams();
  const userId = params.userId as string;
  const { user: currentUser } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const recipientNameParam = searchParams?.get("recipientName");
  const recipientAvatarParam = searchParams?.get("recipientAvatar");
  
  const [messages, setMessages] = useState<Message[]>([]);
  const [otherUser, setOtherUser] = useState<{ name: string; avatarUrl?: string | null } | null>(null);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [newMessage, setNewMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [showListingPicker, setShowListingPicker] = useState(false);
  const [listings, setListings] = useState<Listing[]>([]);
  const [loadingListings, setLoadingListings] = useState(false);
  const [selectedListing, setSelectedListing] = useState<Listing | null>(null);
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-pre-select listing from Share Room navigation
  useEffect(() => {
    const shareListingId = searchParams?.get("shareListingId");
    const shareListingTitle = searchParams?.get("shareListingTitle");
    if (shareListingId && shareListingTitle) {
      setSelectedListing({ id: shareListingId, title: decodeURIComponent(shareListingTitle) } as Listing);
    }
  }, [searchParams]);

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
        const msgs = conversationData.messages || [];
        setMessages(msgs);
        
        let resolved = false;
        
        // Try to resolve participant name/avatar from conversation list
        try {
          const conversations = await messageService.getConversations();
          setConversations(conversations);
          const match = conversations.find((conv) => conv.userId === userId);
          if (match && match.userName && match.userName !== 'Unknown User') {
            setOtherUser({ name: match.userName, avatarUrl: match.userAvatar });
            resolved = true;
          }
        } catch {
          // Ignore and fallback
        }

        // Fallback: extract name from message sender/receiver info
        if (!resolved && msgs.length > 0) {
          const otherMsg = msgs.find((m: any) => m.senderId === userId);
          if (otherMsg && (otherMsg as any).senderFirstName) {
            const name = `${(otherMsg as any).senderFirstName} ${(otherMsg as any).senderLastName || ''}`.trim();
            setOtherUser({ name, avatarUrl: (otherMsg as any).senderProfilePhoto || null });
            resolved = true;
          } else {
            // Check receiver info from messages we sent
            const myMsg = msgs.find((m: any) => m.receiverId === userId);
            if (myMsg && (myMsg as any).receiverFirstName) {
              const name = `${(myMsg as any).receiverFirstName} ${(myMsg as any).receiverLastName || ''}`.trim();
              setOtherUser({ name, avatarUrl: (myMsg as any).receiverProfilePhoto || null });
              resolved = true;
            }
          }
        }

        // Route-level fallback for brand-new conversations opened from a listing.
        if (!resolved && recipientNameParam) {
          setOtherUser({
            name: recipientNameParam,
            avatarUrl: recipientAvatarParam || null,
          });
          resolved = true;
        }

        // Last resort fallback
        if (!resolved) {
          try {
            const profile = await profileService.getProfile(userId);
            setOtherUser({ name: (profile as any).firstName ? `${(profile as any).firstName} ${(profile as any).lastName || ''}`.trim() : "User", avatarUrl: profile.profilePhotoUrl || null });
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
  }, [userId, currentUser, recipientNameParam, recipientAvatarParam]);

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

  const handleEditMessage = async (messageId: string) => {
    if (!editContent.trim()) return;
    try {
      const updated = await messageService.editMessage(messageId, editContent.trim());
      setMessages((prev) => prev.map((m) => m.id === messageId ? { ...m, content: updated.content, isEdited: true } : m));
      setEditingMessageId(null);
      setEditContent("");
    } catch (error) {
      console.error("Failed to edit message", error);
    }
  };

  const handleDeleteMessage = async (messageId: string) => {
    try {
      await messageService.deleteMessage(messageId);
      setMessages((prev) => prev.filter((m) => m.id !== messageId));
    } catch (error) {
      console.error("Failed to delete message", error);
    }
  };

  const startEditing = (msg: Message) => {
    setEditingMessageId(msg.id);
    setEditContent(msg.content);
  };

  const cancelEditing = () => {
    setEditingMessageId(null);
    setEditContent("");
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("fr-CM", {
      style: "currency",
      currency: "XAF",
      minimumFractionDigits: 0,
    }).format(amount);
  };

  const formatConversationTime = (dateString?: string) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) return "";
    return date.toLocaleTimeString("fr-CM", { hour: "2-digit", minute: "2-digit" });
  };

  const filteredConversations = conversations.filter((conv) => {
    if (!searchQuery.trim()) return true;
    return conv.userName.toLowerCase().includes(searchQuery.toLowerCase());
  });

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="grid h-[calc(100dvh-4rem)] bg-slate-100 lg:grid-cols-[360px_minmax(0,1fr)]">
      <aside className="hidden min-h-0 border-r border-slate-200 bg-white lg:flex lg:flex-col">
        <div className="border-b border-slate-100 p-5">
          <h1 className="text-2xl font-semibold text-slate-950">Chats</h1>
          <div className="relative mt-4">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search conversations"
              className="h-11 rounded-full border-slate-100 bg-slate-100 pl-9"
            />
          </div>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto p-3">
          {filteredConversations.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center px-6 text-center text-sm text-slate-500">
              <MessageSquare className="mb-3 h-9 w-9 text-slate-300" />
              Conversations will appear here after you send or receive messages.
            </div>
          ) : (
            <div className="space-y-1">
              {filteredConversations.map((conv) => {
                const active = conv.userId === userId;
                return (
                  <Link key={conv.userId} href={`/messages/${conv.userId}`}>
                    <div className={cn(
                      "flex items-center gap-3 rounded-2xl p-3 transition-colors",
                      active ? "bg-blue-50" : "hover:bg-slate-50"
                    )}>
                      <Avatar className="h-12 w-12">
                        <AvatarImage src={conv.userAvatar || undefined} />
                        <AvatarFallback className="bg-slate-100 text-slate-700">
                          {conv.userName.split(" ").map((n) => n[0]).join("").slice(0, 2).toUpperCase() || "U"}
                        </AvatarFallback>
                      </Avatar>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <p className="truncate font-medium text-slate-950">{conv.userName}</p>
                          <span className="shrink-0 text-xs text-slate-400">{formatConversationTime(conv.lastMessageTime)}</span>
                        </div>
                        <p className="truncate text-sm text-slate-500">{conv.lastMessage || "No messages yet"}</p>
                      </div>
                      {conv.unreadCount > 0 && (
                        <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-blue-600 px-1 text-xs font-semibold text-white">
                          {conv.unreadCount > 9 ? "9+" : conv.unreadCount}
                        </span>
                      )}
                    </div>
                  </Link>
                );
              })}
            </div>
          )}
        </div>
      </aside>

      <main className="min-h-0 bg-white lg:m-3 lg:flex lg:overflow-hidden lg:rounded-3xl lg:border lg:border-slate-200 lg:shadow-sm">
        <div className="flex min-h-0 flex-1 flex-col bg-white">
      {/* Chat Header */}
      <div className="flex items-center gap-3 border-b bg-white p-4">
        <Button variant="ghost" size="icon" className="md:hidden" onClick={() => router.push("/messages")} aria-label="Back">
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <Avatar>
          <AvatarImage src={otherUser?.avatarUrl || undefined} />
          <AvatarFallback>{otherUser?.name?.substring(0, 1) || "?"}</AvatarFallback>
        </Avatar>
        <div className="min-w-0">
          <h2 className="font-semibold">{otherUser?.name || "User"}</h2>
          <p className="text-xs text-muted-foreground">RoomBay messages</p>
        </div>
      </div>

      {/* Messages List */}
      <div className="flex-1 space-y-4 overflow-y-auto bg-[#f7f4ed] p-4">
        {messages.length === 0 && (
          <div className="text-center text-sm text-muted-foreground py-10">
            No messages yet. Say hello 👋
          </div>
        )}
        {messages.map((msg) => {
          const isMe = msg.senderId === currentUser?.id;
          const hasSharedListing = msg.messageType === 'LISTING_SHARE' && msg.sharedListing;
          
          return (
            <div key={msg.id} className={cn("flex items-end gap-2 group", isMe ? "justify-end" : "justify-start")}>
              {!isMe && (
                <Avatar className="h-7 w-7">
                  <AvatarImage src={otherUser?.avatarUrl || undefined} />
                  <AvatarFallback>{otherUser?.name?.substring(0, 1) || "?"}</AvatarFallback>
                </Avatar>
              )}
              {/* Edit/Delete menu for own messages */}
              {isMe && (
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity">
                      <MoreVertical className="h-3.5 w-3.5" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-32">
                    <DropdownMenuItem onClick={() => startEditing(msg)}>
                      <Pencil className="h-3.5 w-3.5 mr-2" /> Edit
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => handleDeleteMessage(msg.id)} className="text-red-600">
                      <Trash2 className="h-3.5 w-3.5 mr-2" /> Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              )}
              <div
                className={cn(
                  "max-w-[80%] rounded-2xl shadow-sm overflow-hidden",
                  isMe
                    ? "bg-[#d9fdd3] text-slate-950 rounded-br-md"
                    : "bg-white text-slate-950 rounded-bl-md"
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
                  {editingMessageId === msg.id ? (
                    <div className="flex items-center gap-1">
                      <Input
                        value={editContent}
                        onChange={(e) => setEditContent(e.target.value)}
                        className="h-7 text-sm bg-background text-foreground"
                        autoFocus
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') handleEditMessage(msg.id);
                          if (e.key === 'Escape') cancelEditing();
                        }}
                      />
                      <Button size="icon" variant="ghost" className="h-6 w-6" onClick={() => handleEditMessage(msg.id)}>
                        <Check className="h-3.5 w-3.5" />
                      </Button>
                      <Button size="icon" variant="ghost" className="h-6 w-6" onClick={cancelEditing}>
                        <X className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  ) : (
                    <p className="whitespace-pre-wrap text-sm">{msg.content}</p>
                  )}
                  <div className="flex items-center gap-1 mt-1">
                    <span className="block text-[10px] text-slate-500">
                      {new Date(msg.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    </span>
                    {(msg as any).isEdited && (
                      <span className={cn("text-[10px] italic", isMe ? "text-primary-foreground/50" : "text-muted-foreground/70")}>edited</span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="shrink-0 border-t bg-white">
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
        
        <div className="bg-slate-50 p-3">
          <form onSubmit={handleSendMessage} className="flex items-center gap-2">
            {/* Share Listing Button */}
            <Sheet open={showListingPicker} onOpenChange={setShowListingPicker}>
              <SheetTrigger asChild>
                <Button 
                  type="button" 
                  variant="outline" 
                  size="icon"
                  onClick={handleOpenListingPicker}
                  className="h-11 w-11 flex-shrink-0 rounded-full bg-white"
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
              className="h-11 flex-1 rounded-full border-slate-200 bg-white px-4"
              disabled={isSending}
            />
            <Button 
              type="submit" 
              size="icon" 
              className="h-11 w-11 rounded-full"
              disabled={isSending || (!newMessage.trim() && !selectedListing)} 
              aria-label="Send message"
            >
              <Send className="h-4 w-4" />
            </Button>
          </form>
        </div>
      </div>
        </div>
      </main>
    </div>
  );
}
