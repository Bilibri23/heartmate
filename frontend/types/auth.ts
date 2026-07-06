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
  role: 'STUDENT' | 'LANDLORD' | 'REALTOR';
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
  emailVerified?: boolean;
  phoneVerified?: boolean;
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
  emailVerified?: boolean;
  phoneVerified?: boolean;
}

export interface AuthMeResponse {
  userId: string;
  email: string;
  /** Phone on file for OTP (E.164 +237… when valid). */
  phone?: string | null;
  firstName: string;
  lastName: string;
  role: string;
  emailVerified?: boolean;
  phoneVerified?: boolean;
}

/** PATCH /auth/me — omit fields you do not want to change. */
export interface UpdateMeRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
}
