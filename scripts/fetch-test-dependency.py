"""Fetch the pinned test-only Android JSON implementation from Maven Central."""
from pathlib import Path
import hashlib
import urllib.request
URL = 'https://repo.maven.apache.org/maven2/com/vaadin/external/google/android-json/0.0.20131108.vaadin1/android-json-0.0.20131108.vaadin1.jar'
SHA256 = 'dfb7bae2f404cfe0b72b4d23944698cb716b7665171812a0a4d0f5926c0fac79'
path = Path(__file__).resolve().parents[1] / 'tests' / 'libs' / 'android-json.jar'
if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != SHA256:
    with urllib.request.urlopen(URL, timeout=30) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise SystemExit('Test dependency checksum mismatch; nothing was written.')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
print('Pinned Android JSON test dependency verified.')
