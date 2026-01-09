import api from '../lib/api';

export interface Message {
  id: string;
  senderId: string;
  receiverId: string;
  content: string;
  createdAt: string;
  isRead: boolean;
}

export interface Conversation {
  userId: string;
  userName: string;
  userAvatar?: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
}

export const messageService = {
  getUnreadCount: async () => {
    const response = await api.get<{ unreadCount: number }>('/messages/unread-count');
    return response.data.unreadCount;
  },

  getConversations: async () => {
    const response = await api.get<Conversation[]>('/messages/conversations');
    return response.data;
  },

  getConversation: async (userId: string) => {
    const response = await api.get<any>(`/messages/conversation/${userId}`);
    return response.data;
  },

  sendMessage: async (receiverId: string, content: string) => {
    const response = await api.post('/messages', { receiverId, content });
    return response.data;
  },

  markAsRead: async (senderId: string) => {
    await api.put(`/messages/read/${senderId}`);
  }
};
