"""Prepare the stable APK, offline editor, exact Git sources and release notes.

This script does not sign, upload, tag, merge or publish anything.
The caller must verify the APK's original signing certificate before using it.
"""

import argparse
import hashlib
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPOSITORY = "s00r1/PRISME"


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def portable_notes(notes, commit):
    """Make image and documentation links work inside a GitHub Release body."""
    raw = f"https://raw.githubusercontent.com/{REPOSITORY}/{commit}/"
    blob = f"https://github.com/{REPOSITORY}/blob/{commit}/docs/"

    def link(match):
        value = match[1]
        if value.startswith("../assets/"):
            value = raw + value[3:]
        elif not re.match(r"\w+://|#", value):
            value = blob + value
        return "](" + value + ")"

    return re.sub(r"\]\(([^)]+)\)", link, notes).replace(
        "Version stable préparée pour revue et publication après fusion de la PR.",
        "Version stable.",
    ).replace(
        "Le nom de version est **6.0.0**, sans suffixe beta. Les liens de téléchargement "
        "`v6.0.0` sont prêts pour la publication ; préparer cette PR ne publie pas la Release.",
        "Version **6.0.0**, sans suffixe beta.",
    )


def prepare(apk, output):
    apk, output = Path(apk).resolve(), Path(output).resolve()
    if not apk.is_file():
        raise ValueError("Signed input APK not found")
    if git("status", "--porcelain"):
        raise ValueError("Commit the source changes before preparing the exact Git archive")
    manifest = ET.parse(ROOT / "AndroidManifest.xml").getroot()
    version = manifest.attrib["{http://schemas.android.com/apk/res/android}versionName"]
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("A stable three-part version is required")
    commit = git("rev-parse", "HEAD")
    output.mkdir(parents=True, exist_ok=True)
    names = [f"PRISME-{version}.apk", f"PRISME-PC-Studio-{version}.zip",
             f"PRISME-{version}-sources.zip"]
    target = output / names[0]
    if apk != target:
        shutil.copyfile(apk, target)
    subprocess.run(
        [sys.executable, str(ROOT / "scripts/package-pc-editor.py"),
         "--output", str(output / names[1])], cwd=ROOT, check=True,
    )
    subprocess.run(
        ["git", "archive", "--format=zip", f"--prefix=PRISME-{version}/",
         f"--output={output / names[2]}", commit], cwd=ROOT, check=True,
    )
    sums = "".join(hashlib.sha256((output / name).read_bytes()).hexdigest()
                   + "  " + name + "\n" for name in names)
    (output / "SHA256SUMS.txt").write_text(sums, encoding="utf-8")
    notes = (ROOT / f"docs/RELEASE-{version}.md").read_text(encoding="utf-8")
    (output / "RELEASE-NOTES.md").write_text(portable_notes(notes, commit), encoding="utf-8")
    print(f"Prepared PRISME {version} from {commit}. No publication performed.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True, help="Previously verified signed APK")
    parser.add_argument("--output", type=Path, default=ROOT / "dist")
    arguments = parser.parse_args()
    prepare(arguments.apk, arguments.output)
