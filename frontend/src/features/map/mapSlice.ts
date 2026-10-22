import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export type HazardType = 'FLOOD' | 'HEAT' | 'AIR_QUALITY';

export interface ViewportState {
  center: [number, number];
  zoom: number;
}

export interface MapState {
  selectedLocationId: string | null;
  activeHazard: HazardType;
  viewport: ViewportState;
}

const initialState: MapState = {
  selectedLocationId: 'mumbai-kurla',
  activeHazard: 'FLOOD',
  viewport: {
    center: [21.5, 82.0],
    zoom: 5,
  },
};

export const mapSlice = createSlice({
  name: 'map',
  initialState,
  reducers: {
    setSelectedLocation: (state, action: PayloadAction<string | null>) => {
      state.selectedLocationId = action.payload;
    },
    setActiveHazard: (state, action: PayloadAction<HazardType>) => {
      state.activeHazard = action.payload;
    },
    setViewport: (state, action: PayloadAction<ViewportState>) => {
      state.viewport = action.payload;
    },
  },
});

export const { setSelectedLocation, setActiveHazard, setViewport } = mapSlice.actions;
export default mapSlice.reducer;
