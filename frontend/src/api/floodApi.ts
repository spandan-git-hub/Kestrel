import { baseApi } from './baseApi';
import {
  SyncLiveResponse,
  SyncLiveLocationResponse,
  LocationForecastResponse,
  normalizeLocationForecastResponse,
} from '../types/api';

export const floodApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    syncLiveOpenMeteo: builder.mutation<SyncLiveResponse, void>({
      query: () => ({
        url: '/flood/sync-live',
        method: 'POST',
      }),
      invalidatesTags: ['AlertStatus', 'Forecast'],
    }),
    syncLiveLocation: builder.mutation<SyncLiveLocationResponse, string>({
      query: (locationId) => ({
        url: `/flood/sync-live/${locationId}`,
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
  useSyncLiveOpenMeteoMutation,
  useSyncLiveLocationMutation,
  useGetFloodForecastQuery,
} = floodApi;
