import { config } from '@config/env';
import { http, HttpResponse } from 'msw';

import { LocationsMock } from './LocationsMock';

export const handlers = [
  http.post(`${config.api.baseUrl}/recommendations`, async () => {
    return HttpResponse.json('test-recommendation-id', { status: 200 });
  }),
  http.get(`${config.api.baseUrl}/recommendations/:id`, async () => {
    return HttpResponse.json(LocationsMock, { status: 200 });
  }),
];
