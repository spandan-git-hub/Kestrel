import { configureStore } from '@reduxjs/toolkit';
import { baseApi } from '../api/baseApi';
import authReducer from '../features/auth/authSlice';
import uiReducer from '../features/ui/uiSlice';
import mapReducer from '../features/map/mapSlice';
import broadcastDraftReducer from '../features/broadcast/draftSlice';
import residentReducer from '../features/resident/residentSlice';

export const store = configureStore({
  reducer: {
    [baseApi.reducerPath]: baseApi.reducer,
    auth: authReducer,
    ui: uiReducer,
    map: mapReducer,
    broadcastDraft: broadcastDraftReducer,
    resident: residentReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(baseApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
