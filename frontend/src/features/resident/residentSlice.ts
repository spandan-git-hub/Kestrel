import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface ResidentState {
  selectedWardId: string;
  isSubscribeOpen: boolean;
  isReportOpen: boolean;
  reportCategory: string;
  reportDescription: string;
}

const initialState: ResidentState = {
  selectedWardId: 'mumbai-kurla',
  isSubscribeOpen: false,
  isReportOpen: false,
  reportCategory: 'WATER_RISING',
  reportDescription: '',
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
  },
});

export const {
  setSelectedWardId,
  setSubscribeOpen,
  setReportOpen,
  setReportCategory,
  setReportDescription,
} = residentSlice.actions;
export default residentSlice.reducer;
