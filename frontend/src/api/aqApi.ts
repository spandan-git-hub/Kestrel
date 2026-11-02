import { baseApi } from './baseApi';
import {
  AqPointSummary,
  AqForecastDetails,
  AqSyncResponse,
  AqFeedStatusResponse,
} from '../types/api';

export const aqApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAqPoints: builder.query<AqPointSummary[], void>({
      query: () => '/aq/points',
      providesTags: ['AqPoint'],
    }),
    getAqPointForecast: builder.query<AqForecastDetails, string>({
      query: (locationId) => `/aq/points/${locationId}/forecast`,
      providesTags: (_res, _err, id) => [{ type: 'AqPoint', id }],
    }),
    syncLiveAq: builder.mutation<AqSyncResponse, string | void>({
      query: (locationId) => ({
        url: locationId ? `/aq/sync-live/${locationId}` : '/aq/sync-live',
        method: 'POST',
      }),
      invalidatesTags: ['AqPoint', 'AlertStatus'],
    }),
    getAqFeedStatus: builder.query<AqFeedStatusResponse, void>({
      query: () => '/aq/feed-status',
      providesTags: ['AqPoint'],
    }),
  }),
});

export const {
  useGetAqPointsQuery,
  useGetAqPointForecastQuery,
  useSyncLiveAqMutation,
  useGetAqFeedStatusQuery,
} = aqApi;

