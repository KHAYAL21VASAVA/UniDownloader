"""
UniDownloader — Python Media Downloader
Vibe coded by Khayal and Antigravity
"""

import os
import time
import urllib.request

def download_stream(url: str, output_path: str, progress_callback=None, cancel_checker=None) -> bool:
    """
    Downloads media stream chunk-by-chunk and reports real progress.
    """
    try:
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
                "Accept": "*/*"
            }
        )

        with urllib.request.urlopen(req, timeout=15) as response:
            total_bytes = int(response.headers.get('Content-Length', 0))
            downloaded = 0
            start_time = time.time()
            last_report_time = start_time
            bytes_since_last = 0

            with open(output_path, 'wb') as out_file:
                while True:
                    if cancel_checker and cancel_checker():
                        out_file.close()
                        if os.path.exists(output_path):
                            os.remove(output_path)
                        return False

                    chunk = response.read(64 * 1024)
                    if not chunk:
                        break

                    out_file.write(chunk)
                    downloaded += len(chunk)
                    bytes_since_last += len(chunk)

                    now = time.time()
                    if (now - last_report_time) >= 0.4:
                        elapsed = now - last_report_time
                        speed = bytes_since_last / elapsed if elapsed > 0 else 0
                        percent = (downloaded / total_bytes * 100) if total_bytes > 0 else 0
                        remaining = total_bytes - downloaded
                        eta = int(remaining / speed) if speed > 0 else 0

                        if progress_callback:
                            progress_callback(percent, downloaded, total_bytes, speed, eta)

                        last_report_time = now
                        bytes_since_last = 0

        return os.path.exists(output_path) and os.path.getsize(output_path) > 0
    except Exception as e:
        if os.path.exists(output_path):
            try:
                os.remove(output_path)
            except OSError:
                pass
        return False
