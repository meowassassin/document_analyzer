import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Clock, FileText, Search } from 'lucide-react'
import api from '../services/api'
import './HistoryPage.css'

const HistoryPage = () => {
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    fetchHistory()
  }, [])

  const fetchHistory = async () => {
    try {
      const response = await api.getRecentResults()
      setResults(response.data)
    } catch (err) {
      console.error('이력 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = async () => {
    if (!searchQuery.trim()) {
      fetchHistory()
      return
    }

    try {
      const response = await api.searchResults(searchQuery)
      setResults(response.data)
    } catch (err) {
      console.error('검색 실패:', err)
    }
  }

  const filteredResults = results.filter(result =>
    result.fileName.toLowerCase().includes(searchQuery.toLowerCase())
  )

  if (loading) {
    return (
      <div className="container">
        <div className="loading-container">
          <div className="loading"></div>
          <p>분석 이력을 불러오는 중...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="container">
      <div className="history-container">
        <div className="history-header">
          <h2>
            <Clock size={28} />
            분석 이력
          </h2>
          <div className="search-box">
            <Search size={20} />
            <input
              type="text"
              placeholder="파일명으로 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              className="input search-input"
            />
          </div>
        </div>

        <div className="results-list">
          {filteredResults.length === 0 ? (
            <div className="empty-state">
              <FileText size={48} />
              <p>분석 이력이 없습니다.</p>
            </div>
          ) : (
            filteredResults.map((result) => (
              <div
                key={result.id}
                className="result-item card"
                onClick={() => navigate(`/results/${result.id}`)}
              >
                <div className="result-icon">
                  <FileText size={32} />
                </div>
                <div className="result-info">
                  <h3>{result.fileName}</h3>
                  <p>
                    {result.documentType} | {result.totalPages}페이지 | {result.totalCells}셀
                  </p>
                  <p className="result-summary">
                    {result.summary?.substring(0, 100)}...
                  </p>
                </div>
                <div className="result-meta">
                  <div className="result-score">
                    <span className="score-label">구조 점수</span>
                    <span className="score-value">{(result.avgStructuralScore || 0).toFixed(2)}</span>
                  </div>
                  <p className="result-date">
                    {new Date(result.createdAt).toLocaleDateString('ko-KR')}
                  </p>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

export default HistoryPage
