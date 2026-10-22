import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface BroadcastDraftState {
  isModalOpen: boolean;
  locationId: string;
  hazard: string;
  severity: string;
  customNote: string;
}

const initialState: BroadcastDraftState = {
  isModalOpen: false,
  locationId: 'mumbai-kurla',
  hazard: 'FLOOD',
  severity: 'WARNING',
  customNote: '',
};

export const broadcastDraftSlice = createSlice({
  name: 'broadcastDraft',
  initialState,
  reducers: {
    openBroadcastModal: (
      state,
      action: PayloadAction<{ locationId: string; hazard: string; severity: string }>
    ) => {
      state.isModalOpen = true;
      state.locationId = action.payload.locationId;
      state.hazard = action.payload.hazard;
      state.severity = action.payload.severity;
    },
    closeBroadcastModal: (state) => {
      state.isModalOpen = false;
      state.customNote = '';
    },
    setCustomNote: (state, action: PayloadAction<string>) => {
      state.customNote = action.payload;
    },
  },
});

export const { openBroadcastModal, closeBroadcastModal, setCustomNote } = broadcastDraftSlice.actions;
export default broadcastDraftSlice.reducer;
