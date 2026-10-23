import { baseApi } from './baseApi';
import type {
  DispatchItem,
  BroadcastPreviewRequest,
  BroadcastPreviewResponse,
  BroadcastRequest,
  BroadcastResponse,
  CreateReportRequest,
  ReportResponse,
  CommunityReportDto,
} from '../types/api';

export const fanoutApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getRecentDispatches: builder.query<DispatchItem[], number | void>({
      query: (limit = 50) => `/fanout/recent?limit=${limit}`,
      providesTags: ['RecentDispatches'],
    }),
    previewBroadcast: builder.mutation<BroadcastPreviewResponse, BroadcastPreviewRequest>({
      query: (body) => ({
        url: '/fanout/broadcast/preview',
        method: 'POST',
        body,
      }),
    }),
    sendBroadcast: builder.mutation<BroadcastResponse, BroadcastRequest>({
      query: (body) => ({
        url: '/fanout/broadcast',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['RecentDispatches', 'AlertStatus'],
    }),
    submitGroundReport: builder.mutation<ReportResponse, CreateReportRequest>({
      query: (body) => ({
        url: '/fanout/reports',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['AlertStatus', 'Reports'],
    }),
    getReportsForLocation: builder.query<CommunityReportDto[], string>({
      query: (locationId) => `/fanout/reports/${locationId}`,
      providesTags: (_result, _error, locationId) => [{ type: 'Reports', id: locationId }],
    }),
    getActiveAlert: builder.query<Record<string, unknown>, string>({
      query: (locationId) => `/fanout/active/${locationId}`,
      providesTags: ['AlertStatus'],
    }),
  }),
});

export const {
  useGetRecentDispatchesQuery,
  usePreviewBroadcastMutation,
  useSendBroadcastMutation,
  useSubmitGroundReportMutation,
  useGetReportsForLocationQuery,
  useGetActiveAlertQuery,
} = fanoutApi;
