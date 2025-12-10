import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 120000 // 2분
})

const api = {
  /**
   * 문서 분석
   */
  analyzeDocument: (formData, documentType) => {
    return apiClient.post('/api/v1/analyze', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      params: {
        documentType: documentType
      }
    })
  },

  /**
   * 빠른 분석
   */
  quickAnalyze: (formData) => {
    return apiClient.post('/api/v1/analyze/quick', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 분석 결과 조회
   */
  getAnalysisResult: (id) => {
    return apiClient.get(`/api/v1/storage/results/${id}`)
  },

  /**
   * 모든 결과 조회
   */
  getAllResults: () => {
    return apiClient.get('/api/v1/storage/results')
  },

  /**
   * 최근 결과 조회
   */
  getRecentResults: () => {
    return apiClient.get('/api/v1/storage/results/recent')
  },

  /**
   * 파일명으로 검색
   */
  searchResults: (fileName) => {
    return apiClient.get('/api/v1/storage/results/search', {
      params: { fileName }
    })
  },

  /**
   * 문서 타입별 조회
   */
  getResultsByType: (documentType) => {
    return apiClient.get(`/api/v1/storage/results/type/${documentType}`)
  },

  /**
   * Health Check
   */
  healthCheck: () => {
    return apiClient.get('/api/v1/analyze/health')
  }
}

export default api
