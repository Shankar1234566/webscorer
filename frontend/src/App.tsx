import { Route, Routes } from 'react-router-dom';
import AppLayout from './components/layout/AppLayout';
import CopilotPage from './pages/CopilotPage';
import HistoryPage from './pages/HistoryPage';
import HomePage from './pages/HomePage';
import NotFoundPage from './pages/NotFoundPage';
import ReportPage from './pages/ReportPage';
import ScannerPage from './pages/ScannerPage';

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="/scanner" element={<ScannerPage />} />
        <Route path="/report/:scanId" element={<ReportPage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/copilot" element={<CopilotPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
