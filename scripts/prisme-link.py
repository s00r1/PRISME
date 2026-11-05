#!/usr/bin/env python3
"""PRISME Link: offline ADB desktop helper for *Android watches*, not a phone port.

No network stack or pairing service is added to PRISME itself.
"""
import argparse
import json
import shutil
import subprocess
from pathlib import Path

PKG = "fr.dm100.prisme/.MainActivity"

def adb(device, *parts):
    cmd = [shutil.which("adb") or "adb"]
    if device:
        cmd.extend(["-s", device])
    return subprocess.run(cmd + list(parts), check=True, text=True, capture_output=True).stdout.strip()

def main():
    cli=argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--device", help="ADB serial of the Android WATCH")
    cli.add_argument("--confirm-watch", action="store_true",
                     help="Confirm target is a rectangular Android watch, not a smartphone")
    tasks=cli.add_subparsers(dest="command",required=True)
    tasks.add_parser("doctor",help="Read Android API and display resolution without modification")
    push=tasks.add_parser("push-profile",help="Send a validated profile to Downloads for manual import")
    push.add_argument("file",type=Path)
    install=tasks.add_parser("install",help="Install an already-signed official PRISME APK")
    install.add_argument("apk",type=Path)
    tasks.add_parser("open",help="Open PRISME on the watch")
    args=cli.parse_args()
    api=int(adb(args.device,"shell","getprop","ro.build.version.sdk") or "0")
    size=adb(args.device,"shell","wm","size")
    if api<25:raise SystemExit("PRISME requires Android API 25 or newer")
    print("Watch inspection: API",api,"·",size)
    if args.command=="doctor":return
    if not args.confirm_watch:raise SystemExit("Supply --confirm-watch after verifying the attached device is a WATCH")
    if args.command=="install":
        if not args.apk.is_file() or args.apk.suffix.lower()!=".apk":
            raise SystemExit("Give an existing signed .apk file")
        print(adb(args.device,"install","-r",str(args.apk)))
    elif args.command=="push-profile":
        if not args.file.is_file() or args.file.stat().st_size>65536:
            raise SystemExit("Profile must be a local JSON file no larger than 64 KiB")
        data=json.loads(args.file.read_text(encoding="utf-8"))
        if not isinstance(data,dict) or data.get("schemaVersion") not in tuple(range(1,8)):
            raise SystemExit("Not a recognized PRISME profile")
        safe=args.file.name.replace(" ","_")
        if not safe.endswith(".json"):raise SystemExit("Profile must end in .json")
        # ADB's push target is fixed to the watch's public Downloads folder.
        print(adb(args.device,"push",str(args.file),"/sdcard/Download/PRISME-Import.json"))
        print("On your watch: PRISME > Réglages > Profils > Importer une configuration.")
    else:
        print(adb(args.device,"shell","am","start","-n",PKG))

if __name__=="__main__":
    main()
