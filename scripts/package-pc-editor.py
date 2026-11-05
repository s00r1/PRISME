"""Package the official PC editors as a standalone, dependency-free offline ZIP."""
from pathlib import Path
import argparse, hashlib, json, zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = "6.0.1"
FILES = {
    "index.html": "home-studio.html",
    "home-model.js": "home-model.js",
    "profile-model.js": "profile-model.js",
    "home-studio.js": "home-studio.js",
    "classic-studio.html": "studio.html",
    "studio.js": "studio.js",
    "style.css": "style.css",
}
# The widget composer uses local reference captures for all 22 desktop styles.
# Bundle them as well so file:// users never depend on the published website.
FILES.update({f"assets/localized/fr/styles/style_{style:02d}.png":
              f"assets/localized/fr/styles/style_{style:02d}.png" for style in range(22)})

def package(output):
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    prefix = "PRISME-PC-Studio-" + VERSION + "/"
    payload = {}
    for destination, source in FILES.items():
        data = (ROOT / "website" / source).read_bytes()
        if destination.endswith(".html"):
            text = data.decode("utf-8")
            text = text.replace('href="home-studio.html"', 'href="index.html"')
            text = text.replace('href="studio.html"', 'href="classic-studio.html"')
            if destination == "index.html":
                text = text.replace('href="index.html"', 'href="https://s00r1.github.io/PRISME/"')
                text = text.replace("<main>", '<main><p><a href="classic-studio.html">Éditeur des 22 bureaux classiques ↗</a></p>', 1)
            data = text.encode("utf-8")
        payload[destination] = data
    payload["LANCER-EDITEUR.cmd"] = b'@echo off\r\nstart "" "%~dp0index.html"\r\n'
    payload["README.md"] = (ROOT / "docs" / "PC-STUDIO.md").read_bytes()
    payload["LICENSE.txt"] = (ROOT / "LICENSE").read_bytes()
    payload["VERSION.txt"] = ("PRISME PC Studio " + VERSION + "\nWidgets for all 22 desktops, optional cards and the classic style editor.\nOpen index.html in a modern browser. No server, installation or network required.\n").encode()
    hashes = {name: hashlib.sha256(data).hexdigest() for name, data in payload.items()}
    payload["FILES-SHA256.json"] = json.dumps(hashes, indent=2).encode()
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, data in payload.items():
            # Fixed timestamp makes the archive reproducible from the same sources.
            info = zipfile.ZipInfo(prefix + name, (2026, 11, 5, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info, data)
    print("PC editor packaged:", output)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "dist" / ("PRISME-PC-Studio-" + VERSION + ".zip"))
    package(parser.parse_args().output)
