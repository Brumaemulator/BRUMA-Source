# SPDX-License-Identifier: MIT
# Copyright (c) 2026 BRUMA contributors
from pathlib import Path
import shutil,json,re,os
r=Path(__file__).resolve().parents[1]
upstream=Path(os.environ.get('BRUMA_UPSTREAM_DIR',r/'upstream'))
work=Path(os.environ.get('BRUMA_WORK_DIR',r/'work'))
prefix=Path(os.environ.get('BRUMA_PREFIX_DIR',r/'prefix'))
j=work/'jni';j.mkdir(parents=True,exist_ok=True)
base=upstream/'mkxp-z-android-reworked/app/jni'
# Only licensed engine/configuration inputs; never stage inherited Android recipes.
if any(j.glob('*.mk')): raise RuntimeError('Choose a fresh work directory: inherited recipes found')
for name in ['mkxp-z','preconfigured']:
 source=base/name
 if source.is_dir():shutil.copytree(source,j/name,dirs_exist_ok=True,ignore=shutil.ignore_patterns('android-project-ant','Xcode','Xcode-iOS'))
for n in ['SDL2','SDL2_image','SDL2_ttf','SDL2_sound','libogg','libvorbis','libtheora','uchardet','physfs']:
 shutil.copytree(upstream/n,j/n,dirs_exist_ok=True,ignore=shutil.ignore_patterns("android-project-ant","Xcode","Xcode-iOS"))
for n in ['freetype','harfbuzz']:shutil.copytree(upstream/n,j/'SDL2_ttf/external'/n,dirs_exist_ok=True,ignore=shutil.ignore_patterns("android-project-ant","Xcode","Xcode-iOS"))
# Start from the pinned Ruby tree for a clean cross build and apply only the tracked patch.
ruby_src=upstream/'ruby'; ruby_dst=work/'ruby'; shutil.copytree(ruby_src,ruby_dst,dirs_exist_ok=True)
# Apply patches idempotently; fail rather than silently build a different tree.
def replace_once(path, old, new):
 text=path.read_text(encoding='utf-8')
 if new in text: return
 if old not in text: raise RuntimeError(f'Patch context not found: {path}')
 path.write_text(text.replace(old,new,1),encoding='utf-8',newline='\n')
replace_once(j/'SDL2/src/sensor/android/SDL_androidsensor.c','ALooper_pollAll(','ALooper_pollOnce(')
replace_once(j/'SDL2_ttf/external/harfbuzz/src/hb.hh','diagnostic error   "-Wcast-function-type"','diagnostic warning "-Wcast-function-type"')
replace_once(ruby_dst/'lib/mkmf.rb','end and File.executable?(exe) or return nil','end and (CROSS_COMPILING ? File.file?(exe) : File.executable?(exe)) or return nil')
for folder in ['assets','shader']:
 (j/'mkxp-z/xxd'/folder).mkdir(parents=True,exist_ok=True)
 for f in (j/'mkxp-z'/folder).iterdir():
  if not f.is_file():continue
  ident=re.sub('[^a-zA-Z0-9]','_',folder+'/'+f.name); data=f.read_bytes()
  t='unsigned char '+ident+'[] = {\n'+','.join(str(x) for x in data)+'\n};\nunsigned int '+ident+'_len = '+str(len(data))+';\n'
  (j/'mkxp-z/xxd'/folder/(f.name+'.xxd')).write_text(t)
# The legacy MiniFFI binding carries uncertain Ruby-1.8-derived provenance.
# Use BRUMA's original MIT implementation, fixed by hashes in sources.lock.
for source in sorted((r/'overrides/mkxp-z/binding').glob('miniffi*')):
 shutil.copy2(source,j/'mkxp-z/binding'/source.name)
print('Native source stage ready (BRUMA MIT MiniFFI override)')


