import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

// Connect RTK Query endpoints to CloudFront Function URLs routing to live AWS Lambda services
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1';

export const baseApi = createApi({
  reducerPath: 'kestrelApi',
  baseQuery: fetchBaseQuery({
    baseUrl: apiBaseUrl,
    prepareHeaders: (headers) => {
      headers.set('Accept', 'application/json');
      return headers;
    },
  }),
  tagTypes: ['Locations', 'AlertStatus', 'Forecast', 'RecentDispatches', 'Reports', 'Subscriptions'],
  endpoints: () => ({}),
});
