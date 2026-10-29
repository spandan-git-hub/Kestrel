import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export type UserRole = 'COORDINATOR' | 'RESIDENT';

export interface AuthState {
  role: UserRole;
  coordinatorToken: string | null;
}

const initialState: AuthState = {
  role: 'COORDINATOR',
  coordinatorToken: null,
};

export const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setRole: (state, action: PayloadAction<UserRole>) => {
      state.role = action.payload;
    },
    toggleRole: (state) => {
      state.role = state.role === 'COORDINATOR' ? 'RESIDENT' : 'COORDINATOR';
    },
    setCoordinatorToken: (state, action: PayloadAction<string | null>) => {
      state.coordinatorToken = action.payload;
    },
  },
});

export const { setRole, toggleRole, setCoordinatorToken } = authSlice.actions;
export default authSlice.reducer;
