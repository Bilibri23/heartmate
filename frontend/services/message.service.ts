import api from '../lib/api';

export interface SharedListingInfo {
  id: string;
  title: string;
  address: string;
  rentAmount: number;
  primaryPhotoUrl?: string;
  status: string;
}

export interface Message {
  id: string;
  senderId: string;
  receiverId: string;
  content: string;
  createdAt: string;
  isRead: boolean;
  messageType?: 'TEXT' | 'LISTING_SHARE';
  sharedListing?: SharedListingInfo;
}

export interface Conversation {
  userId: string;
  userName: string;
  userAvatar?: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
}

// Backend response format
interface ConversationApiResponse {
  userId: string;
  firstName: string;
  lastName: string;
  profilePhotoUrl?: string;
  lastMessage?: { content: string };
  lastMessageTime: string;
  unreadCount: number;
}

export const messageService = {
  getUnreadCount: async () => {
    const response = await api.get<{ unreadCount: number }>('/messages/unread-count');
    return response.data.unreadCount;
  },

  getConversations: async () => {
    const response = await api.get<ConversationApiResponse[]>('/messages/conversations');
    // Map backend response to frontend format
    return response.data.map((conv) => ({
      userId: conv.userId,
      userName: `${conv.firstName || ''} ${conv.lastName || ''}`.trim() || 'Unknown User',
      userAvatar: conv.profilePhotoUrl,
      lastMessage: conv.lastMessage?.content || '',
      lastMessageTime: conv.lastMessageTime,
      unreadCount: conv.unreadCount || 0,
    }));
  },

  getConversation: async (userId: string) => {
    const response = await api.get<any>(`/messages/conversation/${userId}`);
    // Backend returns Page<MessageResponse> with content array
    const messages = response.data?.content || response.data || [];
    return { messages };
  },

  sendMessage: async (receiverId: string, content: string, sharedListingId?: string) => {
    const payload: { receiverId: string; content: string; messageType?: string; sharedListingId?: string } = {
      receiverId,
      content,
    };
    
    if (sharedListingId) {
      payload.messageType = 'LISTING_SHARE';
      payload.sharedListingId = sharedListingId;
    }
    
    const response = await api.post('/messages', payload);
    return response.data;
  },

  markAsRead: async (senderId: string) => {
    await api.put(`/messages/read/${senderId}`);
  }
};
