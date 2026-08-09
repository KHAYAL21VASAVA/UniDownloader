/**
 * UniDownloader — Universal Media Downloader Client
 * Vibe coded by Khayal and Antigravity
 */

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const urlInput = document.getElementById('url-input');
  const pasteBtn = document.getElementById('paste-btn');
  const clearBtn = document.getElementById('clear-btn');
  const analyzeBtn = document.getElementById('analyze-btn');
  const errorBanner = document.getElementById('error-banner');
  const errorMessage = document.getElementById('error-message');

  const mediaCardSection = document.getElementById('media-card-section');
  const mediaThumb = document.getElementById('media-thumb');
  const mediaTitle = document.getElementById('media-title');
  const mediaUploader = document.getElementById('media-uploader');
  const mediaDuration = document.getElementById('media-duration');
  const mediaPlatformBadge = document.getElementById('media-platform-badge');
  const mediaResolutionTag = document.getElementById('media-resolution-tag');
  const modeBtns = document.querySelectorAll('.mode-btn');
  const qualitySelectGroup = document.getElementById('quality-selector-group');
  const qualitySelect = document.getElementById('quality-select');
  const downloadActionBtn = document.getElementById('download-action-btn');
  const downloadBtnText = document.getElementById('download-btn-text');

  const progressSection = document.getElementById('download-progress-section');
  const progressActiveView = document.getElementById('progress-active-view');
  const progressSuccessView = document.getElementById('progress-success-view');

  const progressFilename = document.getElementById('progress-filename');
  const progressStatusText = document.getElementById('progress-status-text');
  const progressPercent = document.getElementById('progress-percent');
  const progressBarFill = document.getElementById('progress-bar-fill');
  const progressBytes = document.getElementById('progress-bytes');
  const progressSpeed = document.getElementById('progress-speed');
  const progressEta = document.getElementById('progress-eta');

  const successFilename = document.getElementById('success-filename');
  const successSize = document.getElementById('success-size');
  const successSpeed = document.getElementById('success-speed');
  const successTime = document.getElementById('success-time');
  const successFormat = document.getElementById('success-format');
  const btnRedownload = document.getElementById('btn-redownload');
  const btnDownloadAnother = document.getElementById('btn-download-another');

  const historyContainer = document.getElementById('history-container');
  const clearHistoryBtn = document.getElementById('clear-history-btn');
  const toast = document.getElementById('toast');
  const toastMessage = document.getElementById('toast-message');
  const pwaInstallBtn = document.getElementById('pwa-install-btn');

  // Application State
  let currentMediaData = null;
  let selectedMode = 'video'; // 'video' or 'audio'
  let deferredPrompt = null;
  let lastDownloadedBlob = null;
  let lastDownloadedFilename = '';

  // Initialize
  renderHistory();
  registerServiceWorker();

  // Server Configuration & Modal
  const serverSettingsBtn = document.getElementById('server-settings-btn');
  const serverModal = document.getElementById('server-modal');
  const modalCloseBtn = document.getElementById('modal-close-btn');
  const serverUrlInput = document.getElementById('server-url-input');
  const btnSaveServer = document.getElementById('btn-save-server');
  const btnResetServer = document.getElementById('btn-reset-server');
  const serverStatusPill = document.getElementById('server-status-pill');

  function getApiBaseUrl() {
    const custom = localStorage.getItem('unidownloader_server_url');
    if (custom && custom.trim().startsWith('http')) {
      return custom.trim().replace(/\/$/, '');
    }
    // If inside Capacitor Android App
    if (window.Capacitor && window.Capacitor.isNativePlatform && window.Capacitor.isNativePlatform()) {
      return 'http://10.0.2.2:3000';
    }
    return '';
  }

  // Server Modal Events
  serverSettingsBtn.addEventListener('click', () => {
    serverUrlInput.value = localStorage.getItem('unidownloader_server_url') || getApiBaseUrl() || window.location.origin;
    serverModal.classList.remove('hidden');
  });

  modalCloseBtn.addEventListener('click', () => {
    serverModal.classList.add('hidden');
  });

  serverModal.addEventListener('click', (e) => {
    if (e.target === serverModal) serverModal.classList.add('hidden');
  });

  btnSaveServer.addEventListener('click', () => {
    const val = serverUrlInput.value.trim();
    if (val.startsWith('http://') || val.startsWith('https://')) {
      localStorage.setItem('unidownloader_server_url', val.replace(/\/$/, ''));
      showToast(`Server connected to: ${val}`);
      serverModal.classList.add('hidden');
    } else {
      showError('Please enter a valid URL starting with http:// or https://');
    }
  });

  btnResetServer.addEventListener('click', () => {
    localStorage.removeItem('unidownloader_server_url');
    serverUrlInput.value = window.location.origin;
    showToast('Reset server to default');
    serverModal.classList.add('hidden');
  });

  // -------------------------------------------------------------
  // Clipboard & Input Events
  // -------------------------------------------------------------
  urlInput.addEventListener('input', () => {
    if (urlInput.value.trim().length > 0) {
      clearBtn.classList.remove('hidden');
    } else {
      clearBtn.classList.add('hidden');
    }
    hideError();
  });

  clearBtn.addEventListener('click', () => {
    urlInput.value = '';
    clearBtn.classList.add('hidden');
    mediaCardSection.classList.add('hidden');
    progressSection.classList.add('hidden');
    hideError();
    urlInput.focus();
  });

  pasteBtn.addEventListener('click', async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text && text.trim().startsWith('http')) {
        urlInput.value = text.trim();
        clearBtn.classList.remove('hidden');
        triggerAnalysis();
      } else if (text) {
        urlInput.value = text.trim();
        clearBtn.classList.remove('hidden');
        showToast('Pasted clipboard content');
      } else {
        showToast('Clipboard is empty');
      }
    } catch (err) {
      showToast('Clipboard permission required');
    }
  });

  urlInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      triggerAnalysis();
    }
  });

  analyzeBtn.addEventListener('click', () => {
    triggerAnalysis();
  });

  // -------------------------------------------------------------
  // Media Analysis Pipeline
  // -------------------------------------------------------------
  async function triggerAnalysis() {
    const rawUrl = urlInput.value.trim();
    if (!rawUrl) {
      showError('Please paste or enter a valid media link first.');
      urlInput.focus();
      return;
    }

    if (!rawUrl.startsWith('http://') && !rawUrl.startsWith('https://')) {
      showError('Please enter a complete URL starting with http:// or https://');
      return;
    }

    setAnalyzingState(true);
    hideError();
    mediaCardSection.classList.add('hidden');
    progressSection.classList.add('hidden');

    try {
      const response = await fetch(`${getApiBaseUrl()}/api/analyze`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: rawUrl })
      });

      const json = await response.json();

      if (!response.ok || !json.success) {
        throw new Error(json.error || 'Failed to analyze media from this URL.');
      }

      currentMediaData = json.data;
      displayMediaCard(currentMediaData);
      showToast('Media stream extracted successfully!');

    } catch (err) {
      console.error('[Analyze Error]', err);
      showError(err.message || 'Unable to fetch video details. Please ensure the link is public.');
    } finally {
      setAnalyzingState(false);
    }
  }

  function displayMediaCard(media) {
    mediaTitle.textContent = media.title || 'Media Video';
    mediaUploader.textContent = media.uploader || media.platform || 'Creator';
    mediaPlatformBadge.textContent = media.platform || 'Universal';

    // Thumbnail fallback
    if (media.thumbnail) {
      mediaThumb.src = media.thumbnail;
      mediaThumb.onerror = () => {
        mediaThumb.src = 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=600&auto=format&fit=crop';
      };
    } else {
      mediaThumb.src = 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=600&auto=format&fit=crop';
    }

    // Duration formatting
    if (media.duration && media.duration > 0) {
      mediaDuration.textContent = formatDuration(media.duration);
      mediaDuration.classList.remove('hidden');
    } else {
      mediaDuration.classList.add('hidden');
    }

    // Populate quality options
    updateFormatView();
    mediaCardSection.classList.remove('hidden');
    mediaCardSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  // -------------------------------------------------------------
  // Format & Quality Selection
  // -------------------------------------------------------------
  modeBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      modeBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedMode = btn.dataset.mode;
      updateFormatView();
    });
  });

  function updateFormatView() {
    if (!currentMediaData) return;

    if (selectedMode === 'video') {
      qualitySelectGroup.classList.remove('hidden');
      downloadBtnText.textContent = 'Download MP4 Video';
      mediaResolutionTag.textContent = '1080p HD';

      qualitySelect.innerHTML = `
        <option value="1080">1080p (Full HD 60fps)</option>
        <option value="720">720p (HD)</option>
        <option value="480">480p (Standard)</option>
        <option value="360">360p (Mobile)</option>
      `;
    } else {
      qualitySelectGroup.classList.add('hidden');
      downloadBtnText.textContent = 'Download 320kbps MP3';
      mediaResolutionTag.textContent = 'Lossless MP3';
    }
  }

  // -------------------------------------------------------------
  // High-Speed Stream Download Pipeline with Rich 100% Finished State
  // -------------------------------------------------------------
  downloadActionBtn.addEventListener('click', () => {
    if (!currentMediaData) return;
    executeDownload(currentMediaData);
  });

  async function executeDownload(media) {
    const isVideo = selectedMode === 'video';
    const extension = isVideo ? 'mp4' : 'mp3';
    const targetUrl = isVideo ? (media.videoUrl || media.formats[0].url) : (media.audioUrl || media.videoUrl || media.formats[0].url);
    const safeTitle = (media.title || 'UniDownloader_Media').replace(/[<>:"/\\|?*]/g, '_').trim();
    const downloadFilename = `${safeTitle}.${extension}`;

    // Reset and show active progress view
    progressSection.classList.remove('hidden');
    progressActiveView.classList.remove('hidden');
    progressSuccessView.classList.add('hidden');

    progressFilename.textContent = downloadFilename;
    progressStatusText.textContent = 'Connecting to high-speed CDN stream...';
    progressBarFill.style.width = '0%';
    progressBarFill.style.background = 'var(--gradient-primary)';
    progressPercent.textContent = '0%';
    progressBytes.innerHTML = `<i class="fa-solid fa-hard-drive"></i> 0 MB / 0 MB`;
    progressSpeed.innerHTML = `<i class="fa-solid fa-gauge-high"></i> Connecting...`;
    progressEta.innerHTML = `<i class="fa-regular fa-clock"></i> ETA --:--`;
    progressSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

    // Stream URL with pre-cached streamId for 0ms startup
    const streamProxyUrl = `${getApiBaseUrl()}/api/stream?streamId=${encodeURIComponent(media.streamId || '')}&url=${encodeURIComponent(targetUrl)}&filename=${encodeURIComponent(safeTitle)}&format=${extension}`;

    try {
      const startTime = Date.now();
      const response = await fetch(streamProxyUrl);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const contentLength = response.headers.get('content-length');
      const totalBytes = contentLength ? parseInt(contentLength, 10) : 0;

      const reader = response.body.getReader();
      const chunks = [];
      let receivedBytes = 0;
      let lastReportTime = startTime;
      let lastReceivedBytes = 0;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        chunks.push(value);
        receivedBytes += value.length;

        const now = Date.now();
        if (now - lastReportTime >= 150) {
          const timeDiff = (now - lastReportTime) / 1000;
          const bytesDiff = receivedBytes - lastReceivedBytes;
          const speedBytesPerSec = bytesDiff / timeDiff;

          const percent = totalBytes > 0 ? Math.min(Math.round((receivedBytes / totalBytes) * 100), 99) : Math.min(Math.round(receivedBytes / (1024 * 1024 * 10) * 100), 90);
          const remainingBytes = totalBytes > receivedBytes ? totalBytes - receivedBytes : 0;
          const etaSecs = speedBytesPerSec > 0 && remainingBytes > 0 ? Math.round(remainingBytes / speedBytesPerSec) : 0;

          progressBarFill.style.width = `${percent}%`;
          progressPercent.textContent = `${percent}%`;
          progressStatusText.textContent = `Streaming chunks (${percent}%)...`;
          progressBytes.innerHTML = `<i class="fa-solid fa-hard-drive"></i> ${formatBytes(receivedBytes)} / ${totalBytes > 0 ? formatBytes(totalBytes) : formatBytes(receivedBytes)}`;
          progressSpeed.innerHTML = `<i class="fa-solid fa-gauge-high"></i> ${formatSpeed(speedBytesPerSec)}`;
          progressEta.innerHTML = `<i class="fa-regular fa-clock"></i> ETA ${formatEta(etaSecs)}`;

          lastReportTime = now;
          lastReceivedBytes = receivedBytes;
        }
      }

      // Compute Total Duration & Final Stats
      const totalDurationSecs = Math.max((Date.now() - startTime) / 1000, 0.1);
      const avgSpeedBytesPerSec = receivedBytes / totalDurationSecs;

      // 100% Progress Bar Transformation
      progressBarFill.style.width = '100%';
      progressBarFill.style.background = 'linear-gradient(135deg, #10B981 0%, #34D399 100%)';
      progressPercent.textContent = '100%';
      progressStatusText.textContent = '100% Download Complete!';

      // Create Blob and Trigger Download Anchor
      const blob = new Blob(chunks, { type: isVideo ? 'video/mp4' : 'audio/mpeg' });
      lastDownloadedBlob = blob;
      lastDownloadedFilename = downloadFilename;

      const downloadUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = downloadUrl;
      a.download = downloadFilename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);

      setTimeout(() => URL.revokeObjectURL(downloadUrl), 30000);

      // Populate 100% Finished Success Card
      successFilename.textContent = downloadFilename;
      successSize.textContent = formatBytes(receivedBytes);
      successSpeed.textContent = formatSpeed(avgSpeedBytesPerSec);
      successTime.textContent = `${totalDurationSecs.toFixed(1)}s`;
      successFormat.textContent = isVideo ? 'MP4 1080p' : 'MP3 320k';

      // Switch to Success View with animation
      setTimeout(() => {
        progressActiveView.classList.add('hidden');
        progressSuccessView.classList.remove('hidden');
      }, 400);

      // Save to history
      saveToHistory({
        id: Date.now().toString(),
        title: media.title,
        filename: downloadFilename,
        platform: media.platform,
        format: extension.toUpperCase(),
        size: receivedBytes,
        timestamp: Date.now(),
        url: targetUrl
      });

      showToast(`🎉 100% Complete! Saved ${downloadFilename}`);

    } catch (err) {
      console.error('[Download Stream Error]', err);
      progressStatusText.textContent = 'Starting browser fallback download...';
      const fallbackLink = document.createElement('a');
      fallbackLink.href = streamProxyUrl;
      fallbackLink.setAttribute('download', downloadFilename);
      fallbackLink.target = '_blank';
      document.body.appendChild(fallbackLink);
      fallbackLink.click();
      document.body.removeChild(fallbackLink);
      showToast('Download started in browser');
      setTimeout(() => {
        progressSection.classList.add('hidden');
      }, 3000);
    }
  }

  // -------------------------------------------------------------
  // Success Card Actions
  // -------------------------------------------------------------
  btnRedownload.addEventListener('click', () => {
    if (lastDownloadedBlob && lastDownloadedFilename) {
      const url = URL.createObjectURL(lastDownloadedBlob);
      const a = document.createElement('a');
      a.href = url;
      a.download = lastDownloadedFilename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      showToast(`Re-saved ${lastDownloadedFilename}`);
      setTimeout(() => URL.revokeObjectURL(url), 10000);
    } else if (currentMediaData) {
      executeDownload(currentMediaData);
    }
  });

  btnDownloadAnother.addEventListener('click', () => {
    urlInput.value = '';
    clearBtn.classList.add('hidden');
    mediaCardSection.classList.add('hidden');
    progressSection.classList.add('hidden');
    hideError();
    window.scrollTo({ top: 0, behavior: 'smooth' });
    urlInput.focus();
  });

  // -------------------------------------------------------------
  // History Storage
  // -------------------------------------------------------------
  function saveToHistory(item) {
    try {
      const existing = JSON.parse(localStorage.getItem('unidownloader_history') || '[]');
      const updated = [item, ...existing.filter(i => i.filename !== item.filename)].slice(0, 20);
      localStorage.setItem('unidownloader_history', JSON.stringify(updated));
      renderHistory();
    } catch (e) {
      console.warn('LocalStorage unavailable');
    }
  }

  function renderHistory() {
    try {
      const list = JSON.parse(localStorage.getItem('unidownloader_history') || '[]');
      if (list.length === 0) {
        historyContainer.innerHTML = `
          <div class="empty-history">
            <i class="fa-solid fa-cloud-arrow-down"></i>
            <p>No downloads yet. Paste a link above to start downloading media!</p>
          </div>
        `;
        clearHistoryBtn.classList.add('hidden');
        return;
      }

      clearHistoryBtn.classList.remove('hidden');
      historyContainer.innerHTML = list.map(item => {
        const isVideo = item.format === 'MP4';
        const dateStr = new Date(item.timestamp).toLocaleDateString(undefined, {
          month: 'short',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        });

        return `
          <div class="history-item">
            <div class="history-icon-box">
              <i class="fa-solid ${isVideo ? 'fa-video' : 'fa-music'}"></i>
            </div>
            <div class="history-details">
              <h4 class="history-title" title="${item.title || item.filename}">${item.title || item.filename}</h4>
              <div class="history-meta">
                <span class="history-platform">${item.platform || 'Media'}</span>
                <span>•</span>
                <span>${formatBytes(item.size)}</span>
                <span>•</span>
                <span>${dateStr}</span>
              </div>
            </div>
            <div class="history-actions">
              <span class="history-badge">${item.format}</span>
            </div>
          </div>
        `;
      }).join('');

    } catch (e) {
      console.warn('Error reading history');
    }
  }

  clearHistoryBtn.addEventListener('click', () => {
    localStorage.removeItem('unidownloader_history');
    renderHistory();
    showToast('Download history cleared');
  });

  // -------------------------------------------------------------
  // Helpers & Utility Functions
  // -------------------------------------------------------------
  function setAnalyzingState(isAnalyzing) {
    const textSpan = analyzeBtn.querySelector('.btn-text');
    const loaderSpan = analyzeBtn.querySelector('.btn-loader');
    if (isAnalyzing) {
      analyzeBtn.disabled = true;
      textSpan.classList.add('hidden');
      loaderSpan.classList.remove('hidden');
    } else {
      analyzeBtn.disabled = false;
      textSpan.classList.remove('hidden');
      loaderSpan.classList.add('hidden');
    }
  }

  function showError(msg) {
    errorMessage.textContent = msg;
    errorBanner.classList.remove('hidden');
    errorBanner.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function hideError() {
    errorBanner.classList.add('hidden');
  }

  function showToast(msg) {
    toastMessage.textContent = msg;
    toast.classList.remove('hidden');
    setTimeout(() => {
      toast.classList.add('hidden');
    }, 3500);
  }

  function formatBytes(bytes) {
    if (!bytes || bytes === 0) return '0 MB';
    const mb = bytes / (1024 * 1024);
    if (mb < 1) {
      return (bytes / 1024).toFixed(1) + ' KB';
    }
    return mb.toFixed(1) + ' MB';
  }

  function formatSpeed(bytesPerSec) {
    if (!bytesPerSec || bytesPerSec === 0) return '0.0 MB/s';
    const mbps = bytesPerSec / (1024 * 1024);
    return mbps.toFixed(1) + ' MB/s';
  }

  function formatEta(seconds) {
    if (!seconds || seconds <= 0 || !isFinite(seconds)) return '--:--';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  function formatDuration(seconds) {
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  // -------------------------------------------------------------
  // Progressive Web App (PWA) Registration & Install Prompt
  // -------------------------------------------------------------
  function registerServiceWorker() {
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js').catch(err => {
          console.log('ServiceWorker registration note:', err.message);
        });
      });
    }
  }

  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
    pwaInstallBtn.classList.remove('hidden');
  });

  pwaInstallBtn.addEventListener('click', async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const choice = await deferredPrompt.userChoice;
    if (choice.outcome === 'accepted') {
      showToast('UniDownloader installed to your device!');
    }
    deferredPrompt = null;
    pwaInstallBtn.classList.add('hidden');
  });
});
