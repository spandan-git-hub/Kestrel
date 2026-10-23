import { baseApi } from './baseApi';
import type {
  LocationAlertStatusResponse,
  LocationForecastResponse,
  SimulationRequest,
  SimulationResponse,
} from '../types/api';

export const rulesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAlertStatuses: builder.query<LocationAlertStatusResponse[], void>({
      query: () => '/rules/status',
      providesTags: ['AlertStatus'],
    }),
    getForecastByLocation: builder.query<LocationForecastResponse, string>({
      query: (locationId) => `/rules/forecast/${locationId}`,
      providesTags: (_result, _error, id) => [{ type: 'Forecast', id }],
    }),
    simulateViaRules: builder.mutation<SimulationResponse, SimulationRequest>({
      query: (body) => ({
        url: '/rules/simulate',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['AlertStatus', 'Forecast'],
    }),
  }),
});

export const {
  useGetAlertStatusesQuery,
  useGetForecastByLocationQuery,
  useSimulateViaRulesMutation,
} = rulesApi;
