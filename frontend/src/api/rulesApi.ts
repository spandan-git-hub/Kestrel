import { baseApi } from './baseApi';
import {
  LocationAlertStatusResponse,
  LocationForecastResponse,
  SimulationRequest,
  SimulationResponse,
  normalizeLocationForecastResponse,
} from '../types/api';

export const rulesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAlertStatuses: builder.query<LocationAlertStatusResponse[], void>({
      query: () => '/rules/status',
      providesTags: ['AlertStatus'],
    }),
    getForecastByLocation: builder.query<LocationForecastResponse, string>({
      query: (locationId) => `/rules/forecast/${locationId}`,
      transformResponse: (res: LocationForecastResponse) =>
        normalizeLocationForecastResponse(res) as LocationForecastResponse,
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
