import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export const baseApi = createApi({
  reducerPath: 'kestrelApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/v1' }),
  tagTypes: ['Locations', 'AlertStatus', 'Forecast', 'RecentDispatches'],
  endpoints: () => ({}),
});
