/**
 * UniDownloader — Cross-Platform yt-dlp Auto-Downloader
 * Runs during npm install on Render, Railway, Docker, Linux, and Windows.
 */

const axios = require('axios');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const isWin = process.platform === 'win32';
const binDir = path.join(__dirname, 'bin');
const binaryName = isWin ? 'yt-dlp.exe' : 'yt-dlp';
const binaryPath = path.join(binDir, binaryName);

async function setupBinary() {
  if (!fs.existsSync(binDir)) {
    fs.mkdirSync(binDir, { recursive: true });
  }

  if (fs.existsSync(binaryPath) && fs.statSync(binaryPath).size > 10000000) {
    console.log(`[UniDownloader] ${binaryName} already exists (${(fs.statSync(binaryPath).size / 1024 / 1024).toFixed(1)} MB).`);
    if (!isWin) {
      try { fs.chmodSync(binaryPath, 0o755); } catch (e) {}
    }
    return;
  }

  const downloadUrl = isWin
    ? 'https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe'
    : 'https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp';

  console.log(`[UniDownloader] Downloading latest ${binaryName} for ${process.platform}...`);
  console.log(`[UniDownloader] Source: ${downloadUrl}`);

  try {
    const response = await axios({
      method: 'GET',
      url: downloadUrl,
      responseType: 'stream',
      headers: {
        'User-Agent': 'Mozilla/5.0'
      }
    });

    const writer = fs.createWriteStream(binaryPath);
    response.data.pipe(writer);

    await new Promise((resolve, reject) => {
      writer.on('finish', resolve);
      writer.on('error', reject);
    });

    if (!isWin) {
      try {
        fs.chmodSync(binaryPath, 0o755);
        console.log(`[UniDownloader] Set executable permissions (chmod 755) on ${binaryPath}`);
      } catch (e) {
        console.warn('[UniDownloader] chmod warning:', e.message);
      }
    }

    console.log(`[UniDownloader] ${binaryName} successfully installed! Size: ${(fs.statSync(binaryPath).size / 1024 / 1024).toFixed(1)} MB.`);
  } catch (err) {
    console.error('[UniDownloader] Binary download error:', err.message);
  }
}

setupBinary();
