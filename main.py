main.py - Enduring Word Offline App

This app is designed to stay under 10MB. It does NOT bundle the database. On first installation/launch, it downloads the database from Internet Archive once to local app storage, then works completely offline.

Source Code

import os
import sys
import sqlite3
import requests
from pathlib import Path

# CONFIG
DB_URL = "https://archive.org/download/enduring_word_full/enduring_word_full.db"
DB_NAME = "enduring_word_full.db"

def get_db_path():
    """Get persistent storage path for all platforms"""
    if sys.platform == "win32":
        base = Path(os.getenv("APPDATA", Path.home())) / "EnduringWord"
    elif sys.platform == "darwin":
        base = Path.home() / "Library" / "Application Support" / "EnduringWord"
    else:
        base = Path.home() / ".local" / "share" / "EnduringWord"
    
    base.mkdir(parents=True, exist_ok=True)
    return base / DB_NAME

DB_PATH = get_db_path()

def download_db():
    """Download only once - check if exists and valid"""
    if DB_PATH.exists() and DB_PATH.stat().st_size > 1024*1024:
        print(f"DB already exists: {DB_PATH}")
        return str(DB_PATH)

    print(f"First-time setup: Downloading commentary DB...")
    print(f"From: {DB_URL}")
    print(f"To: {DB_PATH}")

    try:
        with requests.get(DB_URL, stream=True, timeout=600) as r:
            r.raise_for_status()
            total = int(r.headers.get('content-length', 0))
            downloaded = 0
            with open(DB_PATH, 'wb') as f:
                for chunk in r.iter_content(chunk_size=1024*256):
                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)
                        if total:
                            print(f"\rDownloading: {downloaded/1024/1024:.1f}MB / {total/1024/1024:.1f}MB ({downloaded*100//total}%)", end='')
            print(f"\nDownload complete!")
            return str(DB_PATH)
    except Exception as e:
        print(f"Download failed: {e}")
        if DB_PATH.exists():
            DB_PATH.unlink()
        sys.exit(1)

def get_commentary(book, chapter, verse):
    """Example offline query - adapt table name to your DB"""
    conn = sqlite3.connect(str(DB_PATH))
    cursor = conn.cursor()
    try:
        cursor.execute(
            "SELECT * FROM commentary WHERE book=? AND chapter=? AND verse=?",
            (book, chapter, verse)
        )
        result = cursor.fetchall()
        return result
    except Exception as e:
        cursor.execute("SELECT sql FROM sqlite_master WHERE type='table'")
        print("DB Schema:", cursor.fetchall())
        raise e
    finally:
        conn.close()

if __name__ == "__main__":
    db_file = download_db()
    print("\nTesting offline access...")
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cur.fetchall()
    print(f"Tables found: {tables}")
    conn.close()
    print(f"\nApp is ready. DB will work offline from: {db_file}")

How to keep app <10MB

1. requirements.txt should only contain: requests
2. Do NOT commit the .db file to GitHub
3. Build with: pyinstaller --onefile --noconsole --name="EnduringWord" app.py
4. App will download DB on first launch to AppData/.local/share and work offline
5. For GitHub Actions, use the build.yml provided earlier.

GitHub Workflow (.github/workflows/build.yml)

name: Build App
on: [push]
jobs:
  build:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - run: pip install -r requirements.txt pyinstaller
      - run: pyinstaller --onefile --noconsole --name="EnduringWord" app.py
      - uses: actions/upload-artifact@v4
        with:
          name: EnduringWord-App
          path: dist/