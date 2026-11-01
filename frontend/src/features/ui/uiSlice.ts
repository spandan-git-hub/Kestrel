import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export type SupportedLanguage = 'en' | 'hi' | 'mr';
export type HazardType = 'FLOOD' | 'HEAT' | 'AIR_QUALITY';

export type ResidentTab = 'status' | 'subscribe' | 'report' | 'my-alerts' | 'about';

export type CoordinatorTab =
  | 'map'
  | 'point-detail'
  | 'broadcast'
  | 'reports'
  | 'history'
  | 'feeds'
  | 'evaluation'
  | 'settings'
  | 'sign-in';

export interface ToastMessage {
  id: string;
  type: 'info' | 'success' | 'warning' | 'error';
  message: string;
}

export interface UiState {
  language: SupportedLanguage;
  theme: 'dark' | 'light';
  lastSyncedAt: string | null;
  toasts: ToastMessage[];
  residentTab: ResidentTab;
  coordinatorTab: CoordinatorTab;
  activeHazard: HazardType;
}

const initialState: UiState = {
  language: 'en',
  theme: 'dark', // Default to sleek civic dark mode matching coordinator & stitch specs
  lastSyncedAt: null,
  toasts: [],
  residentTab: 'status',
  coordinatorTab: 'map',
  activeHazard: 'FLOOD',
};

export const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setLanguage: (state, action: PayloadAction<SupportedLanguage>) => {
      state.language = action.payload;
    },
    setTheme: (state, action: PayloadAction<'dark' | 'light'>) => {
      state.theme = action.payload;
    },
    setLastSyncedAt: (state, action: PayloadAction<string | null>) => {
      state.lastSyncedAt = action.payload;
    },
    addToast: (state, action: PayloadAction<ToastMessage>) => {
      state.toasts.push(action.payload);
    },
    removeToast: (state, action: PayloadAction<string>) => {
      state.toasts = state.toasts.filter((t) => t.id !== action.payload);
    },
    setResidentTab: (state, action: PayloadAction<ResidentTab>) => {
      state.residentTab = action.payload;
    },
    setCoordinatorTab: (state, action: PayloadAction<CoordinatorTab>) => {
      state.coordinatorTab = action.payload;
    },
    setActiveHazard: (state, action: PayloadAction<HazardType>) => {
      state.activeHazard = action.payload;
    },
  },
});

export const {
  setLanguage,
  setTheme,
  setLastSyncedAt,
  addToast,
  removeToast,
  setResidentTab,
  setCoordinatorTab,
  setActiveHazard,
} = uiSlice.actions;

export default uiSlice.reducer;
