"""Compare final DEX framework references with the Android SDK API history.

This static audit does not replace runtime verification on the DM100.
"""
import argparse
import os
import hashlib
import json
import re
import struct
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
_arguments = argparse.ArgumentParser(description=__doc__)
_arguments.add_argument("--history", default=os.environ.get("ANDROID_API_HISTORY"), help="Android SDK data/api-versions.xml path")
_arguments.add_argument("--apk", type=Path, default=PROJECT / "PRISME_DM100.apk")
_options = _arguments.parse_args()
if not _options.history:
    _arguments.error("Provide --history or ANDROID_API_HISTORY")
HISTORY = Path(_options.history)
APK = _options.apk
TARGET = 25
api = {c.attrib["name"]: c for c in ET.parse(HISTORY).getroot().findall("class")}


def introduced(node, parent_since=1):
    return max(parent_since, int(node.attrib.get("since", parent_since)))


def lookup(owner, kind, name, visited=None):
    # A declaration removed from a subclass can still be inherited from its active base.
    # Example: Bundle accessors moved to BaseBundle in API21/22.
    visited = set() if visited is None else visited
    if owner in visited or owner not in api:
        return None
    visited.add(owner)
    cls = api[owner]
    inactive_match = None
    for member in cls.findall(kind):
        if member.attrib["name"] == name:
            candidate = owner, member, introduced(member, introduced(cls))
            if candidate[2] <= TARGET < int(member.attrib.get("removed", "100000")):
                return candidate
            inactive_match = candidate
    if name.startswith("<init>"):
        return inactive_match
    for base in list(cls.findall("extends")) + list(cls.findall("implements")):
        if introduced(base, introduced(cls)) > TARGET or int(base.attrib.get("removed", "100000")) <= TARGET:
            continue
        result = lookup(base.attrib["name"], kind, name, visited)
        if result:
            if result[2] <= TARGET < int(result[1].attrib.get("removed", "100000")):
                return result
            if inactive_match is None:
                inactive_match = result
    return inactive_match


def uleb(data, offset):
    result = shift = 0
    while True:
        value = data[offset]; offset += 1
        result |= (value & 127) << shift
        if value < 128: return result, offset
        shift += 7


def instructions(data, offset, length):
    """Walk standard DEX instructions, including aligned switch/array payloads."""
    units = struct.unpack_from('<' + 'H' * length, data, offset)
    widths = [1] * 256
    for op in (2,5,8,0x13,0x15,0x16,0x19,0x1a,0x1c,0x1f,0x20,0x22,0x23,0x29): widths[op] = 2
    for op in (3,6,9,0x14,0x17,0x1b,0x24,0x25,0x26,0x2a,0x2b,0x2c): widths[op] = 3
    widths[0x18] = 5
    for op in range(0x2d,0x3e): widths[op] = 2
    for op in range(0x44,0x6e): widths[op] = 2
    for op in list(range(0x6e,0x73)) + list(range(0x74,0x79)): widths[op] = 3
    for op in list(range(0x90,0xb0)) + list(range(0xd0,0xe3)): widths[op] = 2
    widths[0xfa] = widths[0xfb] = 4
    widths[0xfc] = widths[0xfd] = 3
    widths[0xfe] = widths[0xff] = 2
    i = 0
    while i < length:
        unit = units[i]; op = unit & 255
        if unit == 0x0100: size = 4 + units[i+1] * 2
        elif unit == 0x0200: size = 2 + units[i+1] * 4
        elif unit == 0x0300:
            count = units[i+2] | units[i+3] << 16
            size = 4 + (units[i+1] * count + 1) // 2
        else: size = widths[op]
        if i + size > length: raise ValueError('Malformed DEX instruction')
        yield op, units[i:i+size]
        i += size


if '--self-test' in sys.argv:
    cases = [
        ('android/os/Bundle','getBoolean(Ljava/lang/String;Z)Z','android/os/BaseBundle',22),
        ('android/os/Bundle','putInt(Ljava/lang/String;I)V','android/os/BaseBundle',21),
        ('android/view/accessibility/AccessibilityEvent','setClassName(Ljava/lang/CharSequence;)V','android/view/accessibility/AccessibilityRecord',14)
    ]
    for owner,name,expected,since in cases:
        found = lookup(owner,'method',name)
        assert found and found[0]==expected and found[2]==since, (owner,name,found)
    assert lookup('android/view/View','method','setTooltipText(Ljava/lang/CharSequence;)V')[2] == 26
    # Payload data contains an invoke-like value: it must never become a fake call site.
    fixture = [0x0071,12,0,0x0100,1,0,0,0x0071,0,0x0077,13,0,0x000e]
    decoded = list(instructions(struct.pack('<'+'H'*len(fixture),*fixture),0,len(fixture)))
    assert [op for op,_ in decoded] == [0x71,0,0x77,0x0e]
    assert decoded[0][1][1]==12 and decoded[2][1][1]==13
    print('PASS: 6 API history and DEX instruction regression checks.')
    sys.exit(0)


references, incompatible, unresolved, callers = set(), [], [], {}
with zipfile.ZipFile(APK) as z:
    dex_names = [n for n in z.namelist() if n.endswith(".dex")]
    for dex_name in dex_names:
        dex = z.read(dex_name)
        u4 = lambda off: struct.unpack_from("<I", dex, off)[0]
        strings = []
        for i in range(u4(56)):
            off = u4(u4(60) + 4*i)
            while dex[off] & 0x80:
                off += 1
            off += 1
            strings.append(dex[off:dex.index(0, off)].decode("utf-8", "replace"))
        types = [strings[u4(u4(68) + 4*i)] for i in range(u4(64))]
        protos = []
        for i in range(u4(72)):
            off = u4(76) + 12*i
            params_off = u4(off + 8)
            params = [] if not params_off else [types[struct.unpack_from("<H", dex, params_off + 4 + 2*n)[0]] for n in range(u4(params_off))]
            protos.append("(" + "".join(params) + ")" + types[u4(off + 4)])
        methods = []
        fields = []
        for i in range(u4(88)):
            owner_index, proto_index, name_index = struct.unpack_from("<HHI", dex, u4(92) + 8*i)
            ref = (types[owner_index], "method", strings[name_index] + protos[proto_index])
            methods.append(ref); references.add(ref)
        for i in range(u4(80)):
            owner_index, _, name_index = struct.unpack_from("<HHI", dex, u4(84) + 8*i)
            ref = (types[owner_index], "field", strings[name_index])
            fields.append(ref); references.add(ref)
        for index in range(u4(96)):
            class_off = u4(100) + 32 * index
            owner = types[u4(class_off)]
            off = u4(class_off + 24)
            if not off: continue
            counts = []
            for _ in range(4): count,off = uleb(dex,off); counts.append(count)
            for _ in range(counts[0] + counts[1]):
                _,off = uleb(dex,off); _,off = uleb(dex,off)
            for count in counts[2:]:
                method_index = 0
                for _ in range(count):
                    delta,off = uleb(dex,off); method_index += delta
                    _,off = uleb(dex,off); code,off = uleb(dex,off)
                    if not code: continue
                    for op, units in instructions(dex,code+16,u4(code+12)):
                        ref = None
                        if 0x6e <= op <= 0x72 or 0x74 <= op <= 0x78: ref = methods[units[1]]
                        elif 0x52 <= op <= 0x6d: ref = fields[units[1]]
                        if ref: callers.setdefault(ref,set()).add(owner)

platform_references = []
for descriptor, kind, name in sorted(references):
    owner = descriptor[1:-1] if descriptor.startswith("L") else descriptor
    if not owner.startswith(("android/", "java/", "javax/", "org/json/", "org/xml/")):
        continue
    platform_references.append((owner, kind, name))
    cls = api.get(owner)
    if cls is None:
        unresolved.append({"owner": owner, "kind": kind, "name": name, "reason": "class absent from API history"})
        continue
    if introduced(cls) > TARGET or int(cls.attrib.get("removed", "100000")) <= TARGET:
        incompatible.append({"owner": owner, "kind": kind, "name": name, "class": dict(cls.attrib)})
        continue
    match = lookup(owner, kind, name)
    if not match:
        unresolved.append({"owner": owner, "kind": kind, "name": name, "reason": "member absent from API history including inheritance"})
        continue
    declared_owner, member, since = match
    if since > TARGET or int(member.attrib.get("removed", "100000")) <= TARGET:
        incompatible.append({"owner": owner, "declared_owner": declared_owner, "kind": kind, "name": name, "since": since, "metadata": dict(member.attrib)})

guarded = []
service_source = PROJECT / 'src/fr/dm100/prisme/volume/FloatingVolumeService.java'
if service_source.exists():
    source = re.sub(r'\s+', ' ', service_source.read_text(encoding='utf-8'))
    guard_checks = [
        'if (Build.VERSION.SDK_INT >= 26) Api26.start(context, intent); else context.startService(intent);',
        'if (Build.VERSION.SDK_INT >= 26) b = Api26.builder(this); else b = new Notification.Builder(this);',
        'return Build.VERSION.SDK_INT >= 26 ? Api26.windowType() : android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;'
    ]
    guard_verified = all(check in source for check in guard_checks)
    helper = 'Lfr/dm100/prisme/volume/FloatingVolumeService$Api26;'
    helpers = {helper}
    timer_source = PROJECT / 'src/fr/dm100/prisme/TimerReceiver.java'
    timer_guard_verified = False
    if timer_source.exists():
        timer_code = re.sub(r'\s+', '', timer_source.read_text(encoding='utf-8'))
        timer_guard_verified = 'Build.VERSION.SDK_INT>=26?Api26.builder(c,m):newNotification.Builder(c)' in timer_code
        if timer_guard_verified:
            helpers.add('Lfr/dm100/prisme/TimerReceiver$Api26;')
    rotation_source = PROJECT / 'src/fr/dm100/prisme/GlobalRotationService.java'
    rotation_guard_verified = False
    if rotation_source.exists():
        rotation_code = re.sub(r'\s+', '', rotation_source.read_text(encoding='utf-8'))
        rotation_guard_verified = all(check in rotation_code for check in [
            'if(Build.VERSION.SDK_INT>=26)Api26.start(c,i);elsec.startService(i);',
            'Build.VERSION.SDK_INT>=26?Api26.builder(this,m):newNotification.Builder(this)',
            'Build.VERSION.SDK_INT>=26?Api26.windowType():WindowManager.LayoutParams.TYPE_SYSTEM_ALERT'
        ])
        if rotation_guard_verified:
            helpers.add('Lfr/dm100/prisme/GlobalRotationService$Api26;')
    allowed = {
        ('android/content/Context','startForegroundService(Landroid/content/Intent;)Landroid/content/ComponentName;'),
        ('android/app/Notification$Builder','<init>(Landroid/content/Context;Ljava/lang/String;)V'),
        ('android/app/NotificationManager','createNotificationChannel(Landroid/app/NotificationChannel;)V'),
        ('android/app/NotificationChannel','<init>(Ljava/lang/String;Ljava/lang/CharSequence;I)V'),
        ('android/app/NotificationChannel','setDescription(Ljava/lang/String;)V'),
        ('android/app/NotificationChannel','setSound(Landroid/net/Uri;Landroid/media/AudioAttributes;)V'),
        ('android/app/NotificationChannel','enableVibration(Z)V')
    }
    for ref in list(incompatible):
        use = callers.get(('L'+ref['owner']+';',ref['kind'],ref['name']),set())
        if guard_verified and ref['kind']=='method' and (ref['owner'],ref['name']) in allowed and use and use.issubset(helpers):
            guarded.append(dict(ref, callers=sorted(use), minimum_guard_api=26))
            incompatible.remove(ref)
else: guard_verified = False

report = {
    "apk_sha256": hashlib.sha256(APK.read_bytes()).hexdigest(),
    "android_api_target": TARGET,
    "metadata_source": str(HISTORY),
    "dex_files": dex_names,
    "platform_member_references": len(platform_references),
    "incompatible_references": incompatible,
    "guarded_newer_api_references": guarded,
    "api26_boundary_source_guards_verified": guard_verified,
    "timer_api26_boundary_source_guard_verified": timer_guard_verified,
    "rotation_api26_boundary_source_guards_verified": rotation_guard_verified,
    "unresolved_references": unresolved,
    "physical_watch_test": False,
    "scope": "Static DEX public API history audit with version-aware inherited declarations. API26 notification/service references are allowed only in the isolated Api26 class, with final DEX caller checks and three exact SDK guards in the delivered source. This is not general control-flow proof and does not verify firmware behavior, inlined constants, resources or application-specific inheritance."
}
destination = PROJECT / "AUDIT_API25.json"
destination.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({k: report[k] for k in ('apk_sha256','platform_member_references','incompatible_references','unresolved_references','api26_boundary_source_guards_verified')}, ensure_ascii=False, indent=2))
print('Guarded API26 references:',len(guarded))
sys.exit(1 if incompatible else 0)
