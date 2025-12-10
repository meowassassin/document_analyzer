import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Upload, FileText, AlertCircle } from 'lucide-react'
import api from '../services/api'
import './UploadPage.css'

const UploadPage = () => {
  const [file, setFile] = useState(null)
  const [documentType, setDocumentType] = useState('GENERAL')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const documentTypes = [
    { value: 'RESEARCH_PAPER', label: '논문' },
    { value: 'REPORT', label: '보고서' },
    { value: 'CONTRACT', label: '계약서' },
    { value: 'PRESENTATION', label: '발표자료' },
    { value: 'MANUAL', label: '매뉴얼' },
    { value: 'GENERAL', label: '일반 문서' }
  ]

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0]
    if (selectedFile && selectedFile.type === 'application/pdf') {
      setFile(selectedFile)
      setError('')
    } else {
      setError('PDF 파일만 업로드 가능합니다.')
      setFile(null)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!file) {
      setError('파일을 선택해주세요.')
      return
    }

    setLoading(true)
    setError('')

    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await api.analyzeDocument(formData, documentType)

      if (response.data && response.data.analysisId) {
        // 분석 결과를 state로 전달
        navigate(`/results/${response.data.analysisId}`, {
          state: { result: response.data }
        })
      } else {
        setError('분석 결과를 받지 못했습니다.')
      }
    } catch (err) {
      console.error('분석 실패:', err)
      setError(err.response?.data?.message || err.response?.data?.error || '문서 분석 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container">
      <div className="upload-container">
        <div className="card upload-card">
          <div className="upload-header">
            <Upload size={48} className="upload-icon" />
            <h2>PDF 문서 분석</h2>
            <p>Cell-Frequency Hybrid Parser를 사용한 고급 문서 분석</p>
          </div>

          <form onSubmit={handleSubmit} className="upload-form">
            <div className="form-group">
              <label className="label">
                <FileText size={16} style={{ marginRight: '8px' }} />
                문서 유형
              </label>
              <select
                className="select"
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
              >
                {documentTypes.map(type => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="label">PDF 파일 선택</label>
              <div className="file-input-wrapper">
                <input
                  type="file"
                  accept="application/pdf"
                  onChange={handleFileChange}
                  className="file-input"
                  id="file-upload"
                />
                <label htmlFor="file-upload" className="file-input-label">
                  {file ? file.name : '파일 선택...'}
                </label>
              </div>
            </div>

            {error && (
              <div className="error-message">
                <AlertCircle size={16} />
                <span>{error}</span>
              </div>
            )}

            <button
              type="submit"
              className="button submit-button"
              disabled={loading || !file}
            >
              {loading ? (
                <>
                  <span className="loading"></span>
                  <span>분석 중...</span>
                </>
              ) : (
                '분석 시작'
              )}
            </button>
          </form>

          <div className="info-section">
            <h3>분석 기능</h3>
            <ul>
              <li>FFT 기반 문서 구조 분석</li>
              <li>의미론적 셀 생성 및 토큰 믹싱</li>
              <li>공명 주파수 분석</li>
              <li>AI 기반 요약 및 키워드 추출</li>
              <li>문서 품질 평가</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}

export default UploadPage
