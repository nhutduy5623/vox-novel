export const API_SUCCESS_CODE = 1000;

export interface ApiResponse<T> {
  code: number;
  message?: string;
  result: T;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  if (response.code !== API_SUCCESS_CODE) {
    throw new ApiError(response.message ?? 'Yêu cầu thất bại', response.code);
  }
  return response.result;
}
