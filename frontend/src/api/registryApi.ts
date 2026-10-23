import { baseApi } from './baseApi';
import type {
  LocationDto,
  CreateSubscriptionRequest,
  SubscriptionResponse,
} from '../types/api';

export const registryApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getLocations: builder.query<LocationDto[], void>({
      query: () => '/registry/locations',
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Locations' as const, id })),
              { type: 'Locations', id: 'LIST' },
            ]
          : [{ type: 'Locations', id: 'LIST' }],
    }),
    getLocationById: builder.query<LocationDto, string>({
      query: (id) => `/registry/locations/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Locations', id }],
    }),
    createSubscription: builder.mutation<SubscriptionResponse, CreateSubscriptionRequest>({
      query: (body) => ({
        url: '/registry/subscriptions',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Subscriptions'],
    }),
    getSubscription: builder.query<SubscriptionResponse, string>({
      query: (token) => `/registry/subscriptions/${token}`,
      providesTags: (_result, _error, token) => [{ type: 'Subscriptions', id: token }],
    }),
    deleteSubscription: builder.mutation<void, string>({
      query: (token) => ({
        url: `/registry/subscriptions/${token}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Subscriptions'],
    }),
    syncLocations: builder.mutation<{ status: string; syncedLocationsCount: number }, void>({
      query: () => ({
        url: '/registry/locations/sync',
        method: 'POST',
      }),
      invalidatesTags: [{ type: 'Locations', id: 'LIST' }],
    }),
  }),
});

export const {
  useGetLocationsQuery,
  useGetLocationByIdQuery,
  useCreateSubscriptionMutation,
  useGetSubscriptionQuery,
  useDeleteSubscriptionMutation,
  useSyncLocationsMutation,
} = registryApi;
