// src/types.tsx
export interface User {
  username: string;
  accessToken: string;
}

export type AuthState = 
  | { type: 'UNAUTHENTICATED' }
  | { type: 'AUTHENTICATING' }
  | { type: 'AUTHENTICATED'; user: User }
  | { type: 'ERROR'; message: string };
