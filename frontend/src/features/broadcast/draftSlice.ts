import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { Severity, Provenance } from '../../types/api';

export interface BroadcastDraftState {
  isModalOpen: boolean;
  locationId: string;
  hazard: string;
  severity: Severity;
  provenance: Provenance;
  customNote: string;
}

const initialState: BroadcastDraftState = {
  isModalOpen: false,
  locationId: 'mumbai-kurla',
  hazard: 'FLOOD',
  severity: 'WARNING',
  provenance: 'MODEL_ONLY',
  customNote: '',
};

export const broadcastDraftSlice = createSlice({
  name: 'broadcastDraft',
  initialState,
  reducers: {
    openBroadcastModal: (
      state,
      action: PayloadAction<{ locationId: string; hazard: string; severity?: Severity; provenance?: Provenance }>
    ) => {
      state.isModalOpen = true;
      state.locationId = action.payload.locationId;
      state.hazard = action.payload.hazard;
      if (action.payload.severity) {
        state.severity = action.payload.severity;
      }
      if (action.payload.provenance) {
        state.provenance = action.payload.provenance;
      }
    },
    closeBroadcastModal: (state) => {
      state.isModalOpen = false;
      state.customNote = '';
    },
    setSeverity: (state, action: PayloadAction<Severity>) => {
      state.severity = action.payload;
    },
    setProvenance: (state, action: PayloadAction<Provenance>) => {
      state.provenance = action.payload;
    },
    setCustomNote: (state, action: PayloadAction<string>) => {
      state.customNote = action.payload;
    },
  },
});

export const {
  openBroadcastModal,
  closeBroadcastModal,
  setSeverity,
  setProvenance,
  setCustomNote,
} = broadcastDraftSlice.actions;

export default broadcastDraftSlice.reducer;
