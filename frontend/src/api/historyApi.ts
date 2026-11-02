import { baseApi } from './baseApi';
import type { Hazard, LocationHistoryResponse } from '../types/api';

export const historyApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getLocationHistory: builder.query<
      LocationHistoryResponse,
      { locationId: string; hazard: Hazard; days?: number }
    >({
      query: ({ locationId, hazard, days = 14 }) =>
        `/history/location/${locationId}?hazard=${hazard}&days=${days}`,
      providesTags: (_result, _err, arg) => [
        { type: 'LocationHistory', id: `${arg.locationId}_${arg.hazard}_${arg.days ?? 14}` },
      ],
    }),
  }),
});

export const { useGetLocationHistoryQuery } = historyApi;

