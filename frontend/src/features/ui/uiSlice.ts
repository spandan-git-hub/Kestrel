import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export type SupportedLanguage = 'en' | 'hi' | 'mr';
export type HazardType = 'FLOOD' | 'HEAT' | 'AIR_QUALITY';

export type AppTab = 'map' | 'point-detail' | 'history' | 'subscribe' | 'manuals' | 'about';

// Legacy tab aliases for backward compatibility
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
  activeTab: AppTab;
  language: SupportedLanguage;
  theme: 'dark' | 'light';
  lastSyncedAt: string | null;
  toasts: ToastMessage[];
  activeHazard: HazardType;
  manualHazard: HazardType;
  manualSeverity: 'WATCH' | 'WARNING' | 'SEVERE';
  sidebarCollapsed: boolean;
  // Legacy fields
  coordinatorTab: CoordinatorTab;
  residentTab: ResidentTab;
}

const initialState: UiState = {
  activeTab: 'map',
  language: 'en',
  theme: 'dark',
  lastSyncedAt: null,
  toasts: [],
  activeHazard: 'FLOOD',
  manualHazard: 'FLOOD',
  manualSeverity: 'WARNING',
  sidebarCollapsed: false,
  coordinatorTab: 'map',
  residentTab: 'status',
};

export const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setActiveTab: (state, action: PayloadAction<AppTab>) => {
      state.activeTab = action.payload;
    },
    setActiveHazard: (state, action: PayloadAction<HazardType>) => {
      state.activeHazard = action.payload;
    },
    setLanguage: (state, action: PayloadAction<SupportedLanguage>) => {
      state.language = action.payload;
    },
    setTheme: (state, action: PayloadAction<'dark' | 'light'>) => {
      state.theme = action.payload;
    },
    toggleSidebar: (state) => {
      state.sidebarCollapsed = !state.sidebarCollapsed;
    },
    setSidebarCollapsed: (state, action: PayloadAction<boolean>) => {
      state.sidebarCollapsed = action.payload;
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
    setManualTarget: (
      state,
      action: PayloadAction<{ hazard?: HazardType; severity?: 'WATCH' | 'WARNING' | 'SEVERE' }>
    ) => {
      if (action.payload.hazard) state.manualHazard = action.payload.hazard;
      if (action.payload.severity) state.manualSeverity = action.payload.severity;
    },

    // Backward compatibility helper reducers
    setResidentTab: (state, action: PayloadAction<ResidentTab | AppTab>) => {
      state.residentTab = action.payload as ResidentTab;
      if (action.payload === 'subscribe') state.activeTab = 'subscribe';
      else if (action.payload === 'about') state.activeTab = 'about';
      else if (action.payload === 'status') state.activeTab = 'map';
      else if (['map', 'point-detail', 'history', 'manuals'].includes(action.payload)) {
        state.activeTab = action.payload as AppTab;
      } else {
        state.activeTab = 'map';
      }
    },
    setCoordinatorTab: (state, action: PayloadAction<CoordinatorTab | AppTab>) => {
      state.coordinatorTab = action.payload as CoordinatorTab;
      if (action.payload === 'map') state.activeTab = 'map';
      else if (action.payload === 'point-detail') state.activeTab = 'point-detail';
      else if (action.payload === 'history') state.activeTab = 'history';
      else if (['subscribe', 'manuals', 'about'].includes(action.payload)) {
        state.activeTab = action.payload as AppTab;
      } else {
        state.activeTab = 'map';
      }
    },
  },
});

export const {
  setActiveTab,
  setActiveHazard,
  setLanguage,
  setTheme,
  toggleSidebar,
  setSidebarCollapsed,
  setLastSyncedAt,
  addToast,
  removeToast,
  setManualTarget,
  setResidentTab,
  setCoordinatorTab,
} = uiSlice.actions;

export default uiSlice.reducer;
