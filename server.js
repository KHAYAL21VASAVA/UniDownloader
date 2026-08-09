/**
 * UniDownloader — Universal Media Downloader SaaS Backend
 * Vibe coded by Khayal and Antigravity
 */

const express = require('express');
const cors = require('cors');
const axios = require('axios');
const path = require('path');
const { execFile, execSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const ytdl = require('@distube/ytdl-core');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Cross-Platform Binary Path Detection
const isWin = process.platform === 'win32';
const binDir = path.join(__dirname, 'bin');
const binaryName = isWin ? 'yt-dlp.exe' : 'yt-dlp';
let YTDLP_PATH = path.join(binDir, binaryName);

// Check if yt-dlp is available in bin/ or globally on system
function checkAndSetupBinary() {
  if (fs.existsSync(YTDLP_PATH)) {
    if (!isWin) {
      try { fs.chmodSync(YTDLP_PATH, 0o755); } catch (e) {}
    }
    return;
  }

  // Check system PATH
  try {
    const whichCmd = isWin ? 'where yt-dlp' : 'which yt-dlp';
    const sysPath = execSync(whichCmd, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
    if (sysPath && fs.existsSync(sysPath)) {
      YTDLP_PATH = sysPath.split('\n')[0].trim();
      console.log(`[UniDownloader] Using system yt-dlp at: ${YTDLP_PATH}`);
      return;
    }
  } catch (e) {}

  // Trigger background download of binary if missing
  try {
    require('./postinstall');
  } catch (e) {
    console.log('[UniDownloader Note] Postinstall script note:', e.message);
  }
}
checkAndSetupBinary();

const DESKTOP_USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36';

// In-memory stream resolver cache (TTL: 15 minutes)
const streamCache = new Map();

function cleanCache() {
  const now = Date.now();
  for (const [key, val] of streamCache.entries()) {
    if (now - val.timestamp > 15 * 60 * 1000) {
      streamCache.delete(key);
    }
  }
}
setInterval(cleanCache, 60 * 1000);

/**
 * Execute yt-dlp wrapper with Promise
 */
function runYtDlp(args) {
  return new Promise((resolve, reject) => {
    if (!fs.existsSync(YTDLP_PATH)) {
      return reject(new Error(`yt-dlp binary not found at ${YTDLP_PATH}`));
    }
    execFile(YTDLP_PATH, args, { maxBuffer: 1024 * 1024 * 30, timeout: 35000 }, (error, stdout, stderr) => {
      if (error) {
        return reject(new Error(stderr || error.message));
      }
      resolve(stdout.trim());
    });
  });
}

/**
 * URL platform detection helper
 */
function detectPlatform(url) {
  const lower = url.toLowerCase();
  if (lower.includes('youtube.com') || lower.includes('youtu.be')) return 'YouTube';
  if (lower.includes('instagram.com')) return 'Instagram';
  if (lower.includes('tiktok.com')) return 'TikTok';
  if (lower.includes('twitter.com') || lower.includes('x.com')) return 'Twitter / X';
  if (lower.includes('facebook.com') || lower.includes('fb.watch')) return 'Facebook';
  if (lower.includes('reddit.com')) return 'Reddit';
  if (lower.includes('pinterest.com') || lower.includes('pin.it')) return 'Pinterest';
  if (lower.includes('soundcloud.com')) return 'SoundCloud';
  if (lower.includes('vimeo.com')) return 'Vimeo';
  return 'Direct Media';
}

/**
 * Sanitize filename helper
 */
function sanitizeFileName(name) {
  if (!name) return 'UniDownloader_Media';
  return name.replace(/[<>:"/\\|?*\x00-\x1F]/g, '_').trim().slice(0, 80);
}

/**
 * Pure JS YouTube Resolver (Fallback if yt-dlp is initializing)
 */
async function extractYouTubeJS(url) {
  try {
    const info = await ytdl.getInfo(url);
    const title = sanitizeFileName(info.videoDetails.title || 'YouTube Video');
    const uploader = info.videoDetails.author?.name || 'YouTube Creator';
    const duration = parseInt(info.videoDetails.lengthSeconds || '0', 10);
    const thumbs = info.videoDetails.thumbnails || [];
    const thumbnail = thumbs.length > 0 ? thumbs[thumbs.length - 1].url : null;

    const audioFormat = ytdl.chooseFormat(info.formats, { quality: 'highestaudio' }) || info.formats.find(f => f.hasAudio);
    const videoFormat = ytdl.chooseFormat(info.formats, { quality: 'highest' }) || info.formats.find(f => f.hasVideo && f.hasAudio);

    const videoUrl = videoFormat?.url || url;
    const audioUrl = audioFormat?.url || videoUrl;

    return {
      title,
      uploader,
      thumbnail,
      platform: 'YouTube',
      duration,
      videoUrl,
      audioUrl,
      formats: [
        { quality: '1080p (Full HD)', format: 'MP4', url: videoUrl, isVideo: true },
        { quality: '720p (HD)', format: 'MP4', url: videoUrl, isVideo: true },
        { quality: '480p (SD)', format: 'MP4', url: videoUrl, isVideo: true },
        { quality: '320 kbps Lossless', format: 'MP3', url: audioUrl, isVideo: false }
      ]
    };
  } catch (e) {
    console.log('[YouTube JS Extractor Note]', e.message);
    return null;
  }
}

/**
 * Enhanced Instagram Video Scraper (Embed & GraphQL)
 */
async function extractInstagram(url) {
  const shortcodeMatch = url.match(/\/(?:reel|p|reels)\/([A-Za-z0-9_-]+)/i);
  const shortcode = shortcodeMatch ? shortcodeMatch[1] : null;

  try {
    const embedUrl = shortcode
      ? `https://www.instagram.com/p/${shortcode}/embed/captioned/`
      : `${url.replace(/\/$/, '')}/embed/captioned/`;

    const response = await axios.get(embedUrl, {
      headers: {
        'User-Agent': DESKTOP_USER_AGENT,
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9'
      },
      timeout: 9000
    });

    const html = response.data;
    if (html && typeof html === 'string') {
      let videoCdnUrl = null;
      const patterns = [
        /video_url\\?":\\?"(https:[^"\\]+)/i,
        /"video_url":"(https:[^"]+)"/i,
        /src=\\?"(https:\/\/[^"\\]+scontent[^"\\]+\.mp4[^"\\]*)/i,
        /"src":"(https:\/\/[^"]+scontent[^"]+\.mp4[^"]*)"/i
      ];

      for (const p of patterns) {
        const match = html.match(p);
        if (match && match[1]) {
          const clean = match[1].replace(/\\u0026/g, '&').replace(/\\\//g, '/').replace(/&amp;/g, '&');
          if (clean.includes('.mp4') || clean.includes('scontent')) {
            videoCdnUrl = clean;
            break;
          }
        }
      }

      let thumbnail = null;
      const thumbMatch = html.match(/display_url\\?":\\?"(https:[^"\\]+)/i) ||
                         html.match(/img class="EmbeddedMediaImage"[^>]*src="([^"]+)"/i);
      if (thumbMatch && thumbMatch[1]) {
        thumbnail = thumbMatch[1].replace(/\\u0026/g, '&').replace(/\\\//g, '/').replace(/&amp;/g, '&');
      }

      let title = shortcode ? `Instagram_Reel_${shortcode}` : 'Instagram_Media';
      const captionMatch = html.match(/<div class="Caption"[^>]*>.*?<span[^>]*>(.*?)<\/span>/is);
      if (captionMatch && captionMatch[1]) {
        const raw = captionMatch[1].replace(/<[^>]+>/g, '').trim();
        if (raw) title = sanitizeFileName(raw.slice(0, 60));
      }

      if (videoCdnUrl) {
        return {
          title,
          uploader: 'Instagram Creator',
          thumbnail,
          platform: 'Instagram',
          duration: 0,
          videoUrl: videoCdnUrl,
          audioUrl: videoCdnUrl,
          formats: [
            { quality: 'HD Video (with Audio)', format: 'MP4', url: videoCdnUrl, isVideo: true },
            { quality: 'Audio MP3', format: 'MP3', url: videoCdnUrl, isVideo: false }
          ]
        };
      }
    }
  } catch (e) {
    console.log('[Instagram Scraper Note]', e.message);
  }

  return null;
}

/**
 * TikTok Resolver via TikWM API
 */
async function extractTikTok(url) {
  try {
    const response = await axios.get(`https://www.tikwm.com/api/?url=${encodeURIComponent(url)}`, {
      headers: { 'User-Agent': DESKTOP_USER_AGENT },
      timeout: 8000
    });
    if (response.data && response.data.data) {
      const d = response.data.data;
      const title = sanitizeFileName(d.title || `TikTok_Video_${d.id || Date.now()}`);
      const author = (d.author && d.author.nickname) || 'TikTok Creator';
      const thumbnail = d.cover || d.origin_cover || null;
      const duration = d.duration || 0;
      const videoUrl = d.play || d.wmplay || null;
      const musicUrl = d.music || videoUrl;

      if (videoUrl) {
        return {
          title,
          uploader: author,
          thumbnail,
          platform: 'TikTok',
          duration,
          videoUrl,
          audioUrl: musicUrl,
          formats: [
            { quality: 'HD No Watermark', format: 'MP4', url: videoUrl, isVideo: true },
            { quality: 'Original Sound', format: 'MP3', url: musicUrl, isVideo: false }
          ]
        };
      }
    }
  } catch (e) {
    console.log('[TikWM Note]', e.message);
  }
  return null;
}

/**
 * Main Media Analysis Function
 */
async function analyzeMedia(url) {
  const platform = detectPlatform(url);
  const cacheKey = Buffer.from(url).toString('base64').slice(0, 32);

  // 1. Try Native Engine via yt-dlp JSON Dump
  try {
    const rawJson = await runYtDlp(['--no-playlist', '--dump-json', url]);
    const d = JSON.parse(rawJson);
    const title = sanitizeFileName(d.title || `${platform}_Media`);
    const uploader = d.uploader || d.channel || d.creator || `${platform} Creator`;
    const thumbnail = d.thumbnail || (d.thumbnails && d.thumbnails.length > 0 ? d.thumbnails[d.thumbnails.length - 1].url : null);
    const duration = Math.round(d.duration || 0);

    let bestVideoUrl = d.url || url;
    let bestAudioUrl = d.url || url;

    if (d.formats && Array.isArray(d.formats)) {
      const progressiveVideo = d.formats.filter(f => f.url && f.vcodec !== 'none' && f.acodec !== 'none').pop();
      if (progressiveVideo) {
        bestVideoUrl = progressiveVideo.url;
      } else {
        const anyVideo = d.formats.filter(f => f.url && f.vcodec !== 'none').pop();
        if (anyVideo) bestVideoUrl = anyVideo.url;
      }

      const bestAudio = d.formats.filter(f => f.url && f.acodec !== 'none').pop();
      if (bestAudio) bestAudioUrl = bestAudio.url;
    }

    streamCache.set(cacheKey, { videoUrl: bestVideoUrl, audioUrl: bestAudioUrl, title, timestamp: Date.now() });

    return {
      streamId: cacheKey,
      title,
      uploader,
      thumbnail,
      platform: d.extractor_key || platform,
      duration,
      videoUrl: bestVideoUrl,
      audioUrl: bestAudioUrl,
      formats: [
        { quality: '1080p (Full HD)', format: 'MP4', url: bestVideoUrl, isVideo: true },
        { quality: '720p (HD)', format: 'MP4', url: bestVideoUrl, isVideo: true },
        { quality: '480p (SD)', format: 'MP4', url: bestVideoUrl, isVideo: true },
        { quality: '320 kbps Lossless', format: 'MP3', url: bestAudioUrl, isVideo: false }
      ]
    };
  } catch (err) {
    console.log('[Native YtDlp Note]', err.message);
  }

  // 2. Specific Platform Fallbacks
  if (platform === 'TikTok') {
    const res = await extractTikTok(url);
    if (res) {
      streamCache.set(cacheKey, { videoUrl: res.videoUrl, audioUrl: res.audioUrl, title: res.title, timestamp: Date.now() });
      return { streamId: cacheKey, ...res };
    }
  }

  if (platform === 'Instagram') {
    const res = await extractInstagram(url);
    if (res) {
      streamCache.set(cacheKey, { videoUrl: res.videoUrl, audioUrl: res.audioUrl, title: res.title, timestamp: Date.now() });
      return { streamId: cacheKey, ...res };
    }
  }

  if (platform === 'YouTube') {
    const res = await extractYouTubeJS(url);
    if (res) {
      streamCache.set(cacheKey, { videoUrl: res.videoUrl, audioUrl: res.audioUrl, title: res.title, timestamp: Date.now() });
      return { streamId: cacheKey, ...res };
    }
  }

  return null;
}

// -------------------------------------------------------------
// REST API Endpoints
// -------------------------------------------------------------

/**
 * Health Check Endpoint
 */
app.get('/api/health', (req, res) => {
  res.json({
    status: 'online',
    app: 'UniDownloader Web SaaS',
    version: '1.0.0',
    platform: process.platform,
    binaryExists: fs.existsSync(YTDLP_PATH),
    credits: 'Vibe coded by Khayal and Antigravity',
    timestamp: new Date().toISOString()
  });
});

/**
 * Analyze Media URL Endpoint
 */
app.post('/api/analyze', async (req, res) => {
  const { url } = req.body;
  if (!url || typeof url !== 'string' || !url.trim().startsWith('http')) {
    return res.status(400).json({ success: false, error: 'Please provide a valid media URL (http:// or https://)' });
  }

  const cleanUrl = url.trim();

  // Strict Media URL Validation
  try {
    const parsed = new URL(cleanUrl);
    const host = parsed.hostname.toLowerCase().replace(/^www\./, '');
    const path = parsed.pathname.toLowerCase();

    // Check root homepages
    if ((path === '' || path === '/') && !parsed.search) {
      return res.status(400).json({
        success: false,
        error: `"${host}" is a homepage link. Please provide a direct link to a video, reel, post, or audio track (e.g. ${host}/reel/... or ${host}/watch?v=...)`
      });
    }

    if (host.includes('instagram.com') && !path.includes('/p/') && !path.includes('/reel/') && !path.includes('/reels/') && !path.includes('/tv/') && !path.includes('/stories/')) {
      return res.status(400).json({
        success: false,
        error: 'Please provide a direct Instagram Reel, Post, or Video link (e.g. instagram.com/reel/C_XvK8_...)'
      });
    }

    if ((host.includes('youtube.com') || host.includes('youtu.be')) && !path.includes('/watch') && !path.includes('/shorts') && !path.includes('/live') && !path.includes('/embed') && !host.includes('youtu.be') && !parsed.searchParams.get('v')) {
      return res.status(400).json({
        success: false,
        error: 'Please provide a direct YouTube video link (e.g. youtube.com/watch?v=... or youtu.be/...)'
      });
    }
  } catch (e) {
    return res.status(400).json({ success: false, error: 'Invalid URL structure' });
  }

  try {
    const result = await analyzeMedia(cleanUrl);
    if (!result || !result.formats || result.formats.length === 0) {
      return res.status(422).json({
        success: false,
        error: 'Unable to extract video streams from this link. Please check if the video is public.'
      });
    }

    res.json({ success: true, data: result });
  } catch (err) {
    console.error('[Analyze Error]', err.message);
    res.status(500).json({ success: false, error: err.message || 'Error analyzing media URL' });
  }
});

/**
 * Direct High-Speed Stream Pipe with Strict HTML Protection
 */
app.get('/api/stream', async (req, res) => {
  const streamUrl = req.query.url;
  const streamId = req.query.streamId;
  const rawFilename = req.query.filename || 'UniDownloader_Media';
  const format = (req.query.format || 'mp4').toLowerCase();

  if (!streamUrl && !streamId) {
    return res.status(400).send('Missing stream URL or streamId');
  }

  const safeFilename = `${sanitizeFileName(rawFilename)}.${format}`;
  const asciiFilename = safeFilename.replace(/[^\x20-\x7E]/g, '_');
  const encodedFilename = encodeURIComponent(safeFilename);

  let directCdnUrl = streamUrl;

  // 1. Check in-memory stream cache
  if (streamId && streamCache.has(streamId)) {
    const cached = streamCache.get(streamId);
    directCdnUrl = format === 'mp3' ? cached.audioUrl : cached.videoUrl;
  }

  // 2. If URL is a webpage and not yet a direct CDN stream, resolve it via yt-dlp
  const isDirectCdn = directCdnUrl && (
    directCdnUrl.includes('googlevideo.com') ||
    directCdnUrl.includes('scontent') ||
    directCdnUrl.includes('tiktokcdn') ||
    directCdnUrl.includes('tikwm') ||
    directCdnUrl.includes('.mp4') ||
    directCdnUrl.includes('.mp3')
  );

  if (!isDirectCdn && directCdnUrl && (directCdnUrl.startsWith('http://') || directCdnUrl.startsWith('https://'))) {
    try {
      const formatFlag = format === 'mp3' ? 'ba/bestaudio/best' : '18/22/best[height<=1080]/best';
      const extractedUrl = await runYtDlp(['-f', formatFlag, '-g', directCdnUrl]);
      if (extractedUrl && extractedUrl.startsWith('http')) {
        directCdnUrl = extractedUrl.split('\n')[0].trim();
      }
    } catch (err) {
      console.log('[Direct URL Extraction Note]', err.message);
    }
  }

  // 3. High-Performance Chunk Streaming via Axios
  try {
    const streamResponse = await axios({
      method: 'GET',
      url: directCdnUrl,
      responseType: 'stream',
      headers: {
        'User-Agent': DESKTOP_USER_AGENT,
        'Accept': '*/*',
        'Accept-Encoding': 'identity',
        'Connection': 'keep-alive'
      },
      timeout: 35000
    });

    const upstreamType = streamResponse.headers['content-type'] || '';

    // STRICT HTML CHECK: Never send HTML as a video/audio file!
    if (upstreamType.includes('text/html')) {
      console.error('[Stream Error] Upstream returned HTML instead of binary media.');
      return res.status(422).send('Error: Upstream media stream expired or protected. Please re-analyze the link.');
    }

    const contentType = format === 'mp3' ? 'audio/mpeg' : (format === 'm4a' ? 'audio/mp4' : 'video/mp4');

    res.setHeader('Content-Disposition', `attachment; filename="${asciiFilename}"; filename*=UTF-8''${encodedFilename}`);
    res.setHeader('Content-Type', contentType);
    if (streamResponse.headers['content-length']) {
      res.setHeader('Content-Length', streamResponse.headers['content-length']);
    }

    streamResponse.data.pipe(res);
  } catch (err) {
    console.error('[Direct Stream Pipe Error]', err.message);
    res.status(500).send('Failed to stream media: ' + err.message);
  }
});

// Fallback all frontend routes to index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

function getLocalIp() {
  const nets = os.networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      if (net.family === 'IPv4' && !net.internal && !net.address.startsWith('169.254')) {
        return net.address;
      }
    }
  }
  return 'localhost';
}

app.listen(PORT, '0.0.0.0', () => {
  const localIp = getLocalIp();
  console.log(`===================================================`);
  console.log(`🚀 UniDownloader Web SaaS running on:`);
  console.log(`   💻 Local:  http://localhost:${PORT}`);
  console.log(`   📱 Phone:  http://${localIp}:${PORT}`);
  console.log(`✨ Vibe coded by Khayal and Antigravity`);
  console.log(`===================================================`);
});
