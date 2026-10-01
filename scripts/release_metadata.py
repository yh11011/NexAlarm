"""Generate update metadata only after a release APK has been signed and verified."""
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

apk,apksigner,aapt,out = sys.argv[1:]
certs = subprocess.check_output([apksigner,'verify','--print-certs',apk],text=True)
signers = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})',certs)
if len(signers) != 1:
    raise SystemExit('Exactly one verified APK signer is required')
badging = subprocess.check_output([aapt,'dump','badging',apk],text=True)
package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
minimum = re.search(r"(?:sdkVersion|minSdkVersion):'(\d+)'",badging)
if not package or not minimum or package[1] != 'com.nexalarm.app':
    raise SystemExit('Unexpected release package')
Path(out).write_text(json.dumps({'package_name':package[1],'version_code':int(package[2]),'version_name':package[3],
    'min_sdk':int(minimum[1]),'size_bytes':Path(apk).stat().st_size,
    'sha256':hashlib.sha256(Path(apk).read_bytes()).hexdigest(),'signer_sha256':signers[0].lower()},indent=2)+'\n')
