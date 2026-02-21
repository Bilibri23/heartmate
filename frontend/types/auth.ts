export interface LoginRequest {
  emailOrPhone: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone: string;
  role: 'STUDENT' | 'LANDLORD';
  gender?: 'MALE' | 'FEMALE';
}

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  role: string;
  verificationStatus?: string;
}

export interface AuthResponse {
  userId: string;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: string;
  firstName: string;
  lastName: string;
}
