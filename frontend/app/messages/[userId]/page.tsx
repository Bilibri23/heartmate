"use client";

import { useEffect, useState, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import { messageService, Message } from "@/services/message.service";
import { profileService } from "@/services/profile.service";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ArrowLeft, Send, Loader2 } from "lucide-react";
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

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim() || !userId) return;

    setIsSending(true);
    try {
      const sentMessage = await messageService.sendMessage(userId, newMessage);
      setMessages((prev) => [...prev, sentMessage]);
      setNewMessage("");
    } catch (error) {
      console.error("Failed to send message", error);
    } finally {
      setIsSending(false);
    }
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
                  "max-w-[75%] rounded-2xl px-3 py-2 text-sm shadow-sm",
                  isMe
                    ? "bg-primary text-primary-foreground rounded-br-md"
                    : "bg-muted rounded-bl-md"
                )}
              >
                <p className="whitespace-pre-wrap">{msg.content}</p>
                <span className={cn("mt-1 block text-[10px]", isMe ? "text-primary-foreground/70" : "text-muted-foreground")}>
                  {new Date(msg.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                </span>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="p-4 border-t bg-background">
        <form onSubmit={handleSendMessage} className="flex gap-2">
          <Input
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            placeholder="Type a message..."
            className="flex-1"
            disabled={isSending}
          />
          <Button type="submit" size="icon" disabled={isSending || !newMessage.trim()} aria-label="Send message">
            <Send className="h-4 w-4" />
          </Button>
        </form>
      </div>
    </div>
  );
}
