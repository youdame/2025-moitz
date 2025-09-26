/**
 * 환경변수 설정
 * 모든 환경변수는 여기서 중앙 관리
 */

const getApiBaseUrl = (): string => {
  const isProduction = process.env.NODE_ENV === 'production';

  if (isProduction) {
    const prodUrl = process.env.PROD_API_BASE_URL;
    if (!prodUrl) {
      throw new Error('PROD_API_BASE_URL 환경변수가 설정되지 않았습니다.');
    }
    return prodUrl;
  }

  const devUrl = process.env.DEV_API_BASE_URL;
  if (!devUrl) {
    throw new Error('DEV_API_BASE_URL 환경변수가 설정되지 않았습니다.');
  }
  return devUrl;
};

export const config = {
  api: {
    baseUrl: getApiBaseUrl(),
  },
} as const;

export type Config = typeof config;
