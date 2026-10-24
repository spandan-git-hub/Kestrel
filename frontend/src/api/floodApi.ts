import { baseApi } from './baseApi';
import {
  SimulationRequest,
  SimulationResponse,
  SyncLiveResponse,
  LocationForecastResponse,
  normalizeLocationForecastResponse,
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
      transformResponse: (res: LocationForecastResponse) =>
        normalizeLocationForecastResponse(res) as LocationForecastResponse,
      providesTags: (_result, _error, locationId) => [{ type: 'Forecast', id: locationId }],
    }),
  }),
});

export const {
  useSimulateScenarioMutation,
  useSyncLiveOpenMeteoMutation,
  useGetFloodForecastQuery,
} = floodApi;
