import { baseApi } from './baseApi';
import {
  HeatPointSummary,
  HeatForecastDetails,
  HeatSyncResponse,
} from '../types/api';

export const heatApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getHeatPoints: builder.query<HeatPointSummary[], void>({
      query: () => '/heat/points',
      providesTags: ['HeatPoint'],
    }),
    getHeatPointForecast: builder.query<HeatForecastDetails, string>({
      query: (locationId) => `/heat/points/${locationId}/forecast`,
      providesTags: (_res, _err, id) => [{ type: 'HeatPoint', id }],
    }),
    syncLiveHeat: builder.mutation<HeatSyncResponse, string | void>({
      query: (locationId) => ({
        url: locationId ? `/heat/sync-live/${locationId}` : '/heat/sync-live',
        method: 'POST',
      }),
      invalidatesTags: ['HeatPoint', 'AlertStatus'],
    }),
  }),
});

export const {
  useGetHeatPointsQuery,
  useGetHeatPointForecastQuery,
  useSyncLiveHeatMutation,
} = heatApi;

