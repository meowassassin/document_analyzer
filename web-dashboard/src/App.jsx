import React, { useState } from 'react'
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import UploadPage from './pages/UploadPage'
import ResultsPage from './pages/ResultsPage'
import HistoryPage from './pages/HistoryPage'
import './App.css'

function App() {
  return (
    <Router>
      <div className="app">
        <nav className="navbar">
          <div className="container">
            <div className="nav-brand">
              <h1>PDF Analyzer</h1>
              <p>Cell-Frequency Hybrid Parser</p>
            </div>
            <div className="nav-links">
              <Link to="/" className="nav-link">업로드</Link>
              <Link to="/history" className="nav-link">분석 이력</Link>
            </div>
          </div>
        </nav>

        <main className="main-content">
          <Routes>
            <Route path="/" element={<UploadPage />} />
            <Route path="/results/:id" element={<ResultsPage />} />
            <Route path="/history" element={<HistoryPage />} />
          </Routes>
        </main>

        <footer className="footer">
          <div className="container">
            <p>&copy; 2024 PDF Analyzer. All rights reserved.</p>
          </div>
        </footer>
      </div>
    </Router>
  )
}

export default App
