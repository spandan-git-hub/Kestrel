import { baseApi } from './baseApi';
import type {
  SimulationRequest,
  SimulationResponse,
  SyncLiveResponse,
  LocationForecastResponse,
} from '../types/api';

export const floodApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    simulateScenario: builder.mutation<SimulationResponse, SimulationRequest>({
      query: (body) => ({
        url: '/flood/simulate',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['AlertStatus', 'Forecast'],
    }),
    syncLiveOpenMeteo: builder.mutation<SyncLiveResponse, void>({
      query: () => ({
        url: '/flood/sync-live',
        method: 'POST',
      }),
      invalidatesTags: ['AlertStatus', 'Forecast'],
    }),
    getFloodForecast: builder.query<LocationForecastResponse, string>({
      query: (locationId) => `/flood/forecast/${locationId}`,
      providesTags: (_result, _error, locationId) => [{ type: 'Forecast', id: locationId }],
    }),
  }),
});

export const {
  useSimulateScenarioMutation,
  useSyncLiveOpenMeteoMutation,
  useGetFloodForecastQuery,
} = floodApi;
