import React, { useState, useEffect } from 'react'
import { useParams, useLocation } from 'react-router-dom'
import { FileText, AlertTriangle, TrendingUp, Hash } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import api from '../services/api'
import './ResultsPage.css'

const ResultsPage = () => {
  const { id } = useParams()
  const location = useLocation()
  const [result, setResult] = useState(location.state?.result || null)
  const [loading, setLoading] = useState(!location.state?.result)
  const [error, setError] = useState('')
  const [selectedKeyword, setSelectedKeyword] = useState(null)
  const [showModal, setShowModal] = useState(false)

  useEffect(() => {
    // state로 결과를 받지 못한 경우에만 API 호출
    if (!location.state?.result) {
      fetchResult()
    }
  }, [id])

  const fetchResult = async () => {
    try {
      const response = await api.getAnalysisResult(id)
      setResult(response.data)
    } catch (err) {
      console.error('결과 조회 실패:', err)
      setError('분석 결과를 불러올 수 없습니다.')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="container">
        <div className="loading-container">
          <div className="loading"></div>
          <p>분석 결과를 불러오는 중...</p>
        </div>
      </div>
    )
  }

  if (error || !result) {
    return (
      <div className="container">
        <div className="error-container">
          <AlertTriangle size={48} />
          <p>{error || '결과를 찾을 수 없습니다.'}</p>
        </div>
      </div>
    )
  }

  const stats = result.statistics || {}
  const validation = result.validation || {}
  const keySections = result.keySections || []

  const chartData = [
    { name: '평균 점수', value: stats.avgStructuralScore || 0 },
    { name: '최대 점수', value: stats.maxStructuralScore || 0 }
  ]

  return (
    <div className="container">
      <div className="results-container">
        <div className="results-header card">
          <FileText size={32} />
          <div>
            <h2>{result.fileName}</h2>
            <p>분석 ID: {result.analysisId} | 총 셀: {result.totalCells}</p>
          </div>
        </div>

        <div className="results-grid">
          <div className="card">
            <h3>
              <FileText size={20} />
              요약
            </h3>
            <p className="summary-text">{result.summary || '요약 정보가 없습니다.'}</p>
          </div>

          <div className="card">
            <h3>
              <Hash size={20} />
              키워드
            </h3>
            <div className="keywords">
              {result.keywords && Array.isArray(result.keywords) && result.keywords.length > 0 ? (
                result.keywords.map((keyword, idx) => (
                  <span
                    key={idx}
                    className="keyword-tag clickable"
                    onClick={() => {
                      setSelectedKeyword(keyword.trim())
                      setShowModal(true)
                    }}
                  >
                    {keyword.trim()}
                  </span>
                ))
              ) : (
                <p>키워드 정보가 없습니다.</p>
              )}
            </div>
          </div>

          <div className="card">
            <h3>
              <TrendingUp size={20} />
              분석 메트릭
            </h3>
            <div className="metrics-grid">
              <div className="metric-item">
                <span className="metric-label">평균 구조 점수</span>
                <span className="metric-value">{(stats.avgStructuralScore || 0).toFixed(3)}</span>
              </div>
              <div className="metric-item">
                <span className="metric-label">최대 구조 점수</span>
                <span className="metric-value">{(stats.maxStructuralScore || 0).toFixed(3)}</span>
              </div>
              <div className="metric-item">
                <span className="metric-label">셀 타입 수</span>
                <span className="metric-value">{Object.keys(stats.typeDistribution || {}).length}</span>
              </div>
              <div className="metric-item">
                <span className="metric-label">유효성</span>
                <span className="metric-value">{validation.isValid ? '✓' : '✗'}</span>
              </div>
            </div>

            <div className="chart-container">
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="value" fill="#667eea" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {validation.warnings && validation.warnings.length > 0 && (
            <div className="card warning-card">
              <h3>
                <AlertTriangle size={20} />
                검증 경고
              </h3>
              <ul>
                {validation.warnings.map((warning, idx) => (
                  <li key={idx}>{warning}</li>
                ))}
              </ul>
            </div>
          )}

          {keySections && keySections.length > 0 && (
            <div className="card">
              <h3>주요 섹션</h3>
              <div>
                {keySections.map((section, idx) => (
                  <div key={idx} style={{ marginBottom: '10px', padding: '10px', background: '#f5f5f5', borderRadius: '4px' }}>
                    <strong>{section.type}</strong> (점수: {section.score})
                    <p style={{ marginTop: '5px' }}>{section.content.substring(0, 150)}...</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="card footer-info">
          <p>분석 완료: {new Date().toLocaleString('ko-KR')}</p>
        </div>
      </div>

      {/* 키워드 관련 내용 모달 */}
      {showModal && selectedKeyword && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>"{selectedKeyword}" 관련 내용</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>
                ×
              </button>
            </div>
            <div className="modal-body">
              {result.keywordLocations && result.keywordLocations[selectedKeyword] &&
               result.keywordLocations[selectedKeyword].length > 0 ? (
                result.keywordLocations[selectedKeyword].map((location, idx) => (
                  <div key={idx} className="location-card">
                    <div className="location-header">
                      <span className="location-page">섹션 {location.pageNumber}</span>
                      <span className="location-score">
                        관련도: {(location.relevanceScore * 100).toFixed(0)}%
                      </span>
                    </div>
                    <div className="location-content">
                      {location.content}
                    </div>
                  </div>
                ))
              ) : (
                <p className="no-locations">이 키워드와 관련된 내용을 찾을 수 없습니다.</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default ResultsPage
