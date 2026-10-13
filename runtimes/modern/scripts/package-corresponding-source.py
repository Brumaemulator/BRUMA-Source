# SPDX-License-Identifier: MIT
# Copyright (c) 2026 BRUMA contributors
"""Package an already reviewed source snapshot; never sweep harnesses or downloads."""
import argparse,hashlib,json,zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--root',required=True);p.add_argument('--manifest',required=True);p.add_argument('--output',required=True);a=p.parse_args()
r=Path(a.root).resolve();files=json.loads(Path(a.manifest).read_text())
with zipfile.ZipFile(a.output,'w',zipfile.ZIP_DEFLATED,compresslevel=5) as z:
 for rel,expected in sorted(files.items()):
  f=(r/rel).resolve()
  if r not in f.parents or not f.is_file():raise RuntimeError('Invalid manifest path: '+rel)
  if hashlib.sha256(f.read_bytes()).hexdigest()!=expected:raise RuntimeError('Hash mismatch: '+rel)
  z.write(f,'BRUMA-Source/'+rel)
