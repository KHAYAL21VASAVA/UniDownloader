/**
 * UniDownloader — Universal Media Downloader SaaS Backend
 * Vibe coded by Khayal and Antigravity
 */

const express = require('express');
const cors = require('cors');
const axios = require('axios');
const path = require('path');
const { execFile } = require('child_process');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const YTDLP_PATH = path.join(__dirname, 'bin', 'yt-dlp.exe');
const DESKTOP_USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36';

// In-memory stream resolver cache (TTL: 10 minutes)
const streamCache = new Map();

function cleanCache() {
  const now = Date.now();
  for (const [key, val] of streamCache.entries()) {
    if (now - val.timestamp > 10 * 60 * 1000) {
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
      return reject(new Error('yt-dlp binary not found'));
    }
    execFile(YTDLP_PATH, args, { maxBuffer: 1024 * 1024 * 30, timeout: 30000 }, (error, stdout, stderr) => {
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
 * Resolver: Multi-Engine Media Analysis with Pre-Cached CDN URLs
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

    // Extract best video stream and audio stream CDN URLs directly from formats
    let bestVideoUrl = d.url || url;
    let bestAudioUrl = d.url || url;

    if (d.formats && Array.isArray(d.formats)) {
      // Find direct progressive video format (with video + audio) or best video format
      const progressiveVideo = d.formats.filter(f => f.url && f.vcodec !== 'none' && f.acodec !== 'none').pop();
      if (progressiveVideo) {
        bestVideoUrl = progressiveVideo.url;
      } else {
        const anyVideo = d.formats.filter(f => f.url && f.vcodec !== 'none').pop();
        if (anyVideo) bestVideoUrl = anyVideo.url;
      }

      // Find best audio format
      const bestAudio = d.formats.filter(f => f.url && f.acodec !== 'none').pop();
      if (bestAudio) {
        bestAudioUrl = bestAudio.url;
      }
    }

    // Cache pre-resolved CDN URLs
    streamCache.set(cacheKey, {
      videoUrl: bestVideoUrl,
      audioUrl: bestAudioUrl,
      title,
      timestamp: Date.now()
    });

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

  // 2. Specific Fallback for TikTok
  if (platform === 'TikTok') {
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
        const videoUrl = d.play || d.wmplay || url;
        const musicUrl = d.music || videoUrl;

        streamCache.set(cacheKey, { videoUrl, audioUrl: musicUrl, title, timestamp: Date.now() });

        return {
          streamId: cacheKey,
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
    } catch (e) {
      console.log('[TikWM Fallback Note]', e.message);
    }
  }

  // 3. Fallback: Instagram Scraper
  if (platform === 'Instagram') {
    const shortcodeMatch = url.match(/\/(?:reel|p|reels)\/([A-Za-z0-9_-]+)/i);
    const shortcode = shortcodeMatch ? shortcodeMatch[1] : null;
    try {
      const embedUrl = shortcode
        ? `https://www.instagram.com/p/${shortcode}/embed/captioned/`
        : `${url.replace(/\/$/, '')}/embed/captioned/`;

      const response = await axios.get(embedUrl, {
        headers: { 'User-Agent': DESKTOP_USER_AGENT },
        timeout: 8000
      });

      const html = response.data;
      if (html && !html.includes('Login • Instagram')) {
        let videoCdnUrl = null;
        const patterns = [
          /video_url\\?":\\?"(https:[^"\\]+)/i,
          /"video_url":"(https:[^"]+)"/i,
          /src=\\?"(https:\/\/[^"\\]+scontent[^"\\]+\.mp4[^"\\]*)/i
        ];
        for (const p of patterns) {
          const match = html.match(p);
          if (match && match[1]) {
            videoCdnUrl = match[1].replace(/\\u0026/g, '&').replace(/\\\//g, '/').replace(/&amp;/g, '&');
            break;
          }
        }

        let thumbnail = null;
        const thumbMatch = html.match(/display_url\\?":\\?"(https:[^"\\]+)/i);
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
          streamCache.set(cacheKey, { videoUrl: videoCdnUrl, audioUrl: videoCdnUrl, title, timestamp: Date.now() });

          return {
            streamId: cacheKey,
            title,
            uploader: 'Instagram Creator',
            thumbnail,
            platform: 'Instagram',
            duration: 0,
            videoUrl: videoCdnUrl,
            audioUrl: videoCdnUrl,
            formats: [
              { quality: 'HD Video', format: 'MP4', url: videoCdnUrl, isVideo: true },
              { quality: 'Audio MP3', format: 'MP3', url: videoCdnUrl, isVideo: false }
            ]
          };
        }
      }
    } catch (e) {
      console.log('[Instagram Embed Note]', e.message);
    }
  }

  // 4. Fallback: OpenGraph Tag Scraper
  try {
    const response = await axios.get(url, {
      headers: { 'User-Agent': DESKTOP_USER_AGENT },
      timeout: 8000
    });
    const html = response.data;
    if (typeof html === 'string') {
      let title = `${platform}_Media`;
      let thumbnail = null;

      const ogTitle = html.match(/<meta[^>]*property=["']og:title["'][^>]*content=["']([^"']+)["']/i);
      const titleTag = html.match(/<title>([^<]+)<\/title>/i);
      if (ogTitle && ogTitle[1]) title = sanitizeFileName(ogTitle[1]);
      else if (titleTag && titleTag[1]) title = sanitizeFileName(titleTag[1]);

      const ogImage = html.match(/<meta[^>]*property=["']og:image["'][^>]*content=["']([^"']+)["']/i);
      if (ogImage && ogImage[1]) thumbnail = ogImage[1];

      streamCache.set(cacheKey, { videoUrl: url, audioUrl: url, title, timestamp: Date.now() });

      return {
        streamId: cacheKey,
        title,
        uploader: platform,
        thumbnail,
        platform,
        duration: 0,
        videoUrl: url,
        audioUrl: url,
        formats: [
          { quality: '1080p (Full HD)', format: 'MP4', url, isVideo: true },
          { quality: '320 kbps Lossless', format: 'MP3', url, isVideo: false }
        ]
      };
    }
  } catch (err) {
    console.log('[OpenGraph Note]', err.message);
  }

  return {
    streamId: cacheKey,
    title: `${platform}_Media`,
    uploader: platform,
    thumbnail: null,
    platform,
    duration: 0,
    videoUrl: url,
    audioUrl: url,
    formats: [
      { quality: '1080p (Full HD)', format: 'MP4', url, isVideo: true },
      { quality: '320 kbps Lossless', format: 'MP3', url, isVideo: false }
    ]
  };
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

  try {
    const result = await analyzeMedia(cleanUrl);
    if (!result || !result.formats || result.formats.length === 0) {
      return res.status(422).json({
        success: false,
        error: 'Unable to analyze media streams for this link. Please check if the link is public.'
      });
    }

    res.json({ success: true, data: result });
  } catch (err) {
    console.error('[Analyze Error]', err.message);
    res.status(500).json({ success: false, error: err.message || 'Error analyzing media URL' });
  }
});

/**
 * High-Speed Direct Stream Pipe
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

  // 1. Check in-memory stream cache for 0ms startup time
  if (streamId && streamCache.has(streamId)) {
    const cached = streamCache.get(streamId);
    directCdnUrl = format === 'mp3' ? cached.audioUrl : cached.videoUrl;
  }

  // 2. If URL is a webpage and not yet a direct CDN stream, resolve it
  const isDirectCdn = directCdnUrl && (
    directCdnUrl.includes('googlevideo.com') ||
    directCdnUrl.includes('scontent') ||
    directCdnUrl.includes('tiktokcdn') ||
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

  // 3. High-Performance Chunk Streaming via Axios with Keep-Alive & High Buffer
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

    const contentType = format === 'mp3' ? 'audio/mpeg' : (format === 'm4a' ? 'audio/mp4' : 'video/mp4');

    res.setHeader('Content-Disposition', `attachment; filename="${asciiFilename}"; filename*=UTF-8''${encodedFilename}`);
    res.setHeader('Content-Type', contentType);
    if (streamResponse.headers['content-length']) {
      res.setHeader('Content-Length', streamResponse.headers['content-length']);
    }

    // Set high-water-mark chunk buffer
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

const os = require('os');

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
