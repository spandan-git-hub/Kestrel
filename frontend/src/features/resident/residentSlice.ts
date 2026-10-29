import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface ResidentState {
  selectedWardId: string;
  isSubscribeOpen: boolean;
  isReportOpen: boolean;
  reportCategory: string;
  reportDescription: string;
  activeSubscriptionToken: string | null;
}

const getStoredToken = (): string | null => {
  try {
    return localStorage.getItem('kestrel_resident_sub_token');
  } catch {
    return null;
  }
};

const initialState: ResidentState = {
  selectedWardId: 'mumbai-kurla',
  isSubscribeOpen: false,
  isReportOpen: false,
  reportCategory: 'WATER_RISING',
  reportDescription: '',
  activeSubscriptionToken: getStoredToken(),
};

export const residentSlice = createSlice({
  name: 'resident',
  initialState,
  reducers: {
    setSelectedWardId: (state, action: PayloadAction<string>) => {
      state.selectedWardId = action.payload;
    },
    setSubscribeOpen: (state, action: PayloadAction<boolean>) => {
      state.isSubscribeOpen = action.payload;
    },
    setReportOpen: (state, action: PayloadAction<boolean>) => {
      state.isReportOpen = action.payload;
    },
    setReportCategory: (state, action: PayloadAction<string>) => {
      state.reportCategory = action.payload;
    },
    setReportDescription: (state, action: PayloadAction<string>) => {
      state.reportDescription = action.payload;
    },
    setActiveSubscriptionToken: (state, action: PayloadAction<string | null>) => {
      state.activeSubscriptionToken = action.payload;
      try {
        if (action.payload) {
          localStorage.setItem('kestrel_resident_sub_token', action.payload);
        } else {
          localStorage.removeItem('kestrel_resident_sub_token');
        }
      } catch {
        // ignore
      }
    },
  },
});

export const {
  setSelectedWardId,
  setSubscribeOpen,
  setReportOpen,
  setReportCategory,
  setReportDescription,
  setActiveSubscriptionToken,
} = residentSlice.actions;
export default residentSlice.reducer;
