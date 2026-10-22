import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export type SupportedLanguage = 'en' | 'hi' | 'mr';

export interface ToastMessage {
  id: string;
  type: 'info' | 'success' | 'warning' | 'error';
  message: string;
}

export interface UiState {
  language: SupportedLanguage;
  theme: 'dark' | 'light';
  simulationMode: string | null;
  toasts: ToastMessage[];
}

const initialState: UiState = {
  language: 'en',
  theme: 'dark',
  simulationMode: 'LIVE_OPEN_METEO',
  toasts: [],
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
    setSimulationMode: (state, action: PayloadAction<string | null>) => {
      state.simulationMode = action.payload;
    },
    addToast: (state, action: PayloadAction<ToastMessage>) => {
      state.toasts.push(action.payload);
    },
    removeToast: (state, action: PayloadAction<string>) => {
      state.toasts = state.toasts.filter((t) => t.id !== action.payload);
    },
  },
});

export const { setLanguage, setTheme, setSimulationMode, addToast, removeToast } = uiSlice.actions;
export default uiSlice.reducer;
