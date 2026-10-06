# SPDX-License-Identifier: MIT
# Copyright (c) 2026 BRUMA contributors
from pathlib import Path
import json,shutil
r=Path(__file__).resolve().parents[1]; b=r/'build-system'
data=json.loads((b/'arm64-requirements.json').read_text())
lines=['# SPDX-License-Identifier: MIT','# Copyright (c) 2026 BRUMA contributors','# Original build driver based on recorded compiler requirements.','cmake_minimum_required(VERSION 3.22)','project(BrumaModernNative LANGUAGES C CXX)','if(NOT ANDROID OR NOT ANDROID_ABI STREQUAL "arm64-v8a")',' message(FATAL_ERROR "This locked recipe requires Android ARM64")','endif()','set(JNI "${BRUMA_JNI_DIR}")','set(PREFIX "${BRUMA_PREFIX_DIR}")','set(NDK "${CMAKE_ANDROID_NDK}")','set(CMAKE_C_FLAGS_RELEASE "-DNDEBUG")','set(CMAKE_CXX_FLAGS_RELEASE "-DNDEBUG")','set(CMAKE_POSITION_INDEPENDENT_CODE ON)','set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/out")']
def q(t): return '"'+t.replace('\\','\\\\').replace('"','\\"').replace(';','\\;')+'"'
imports={'ruby':'libruby.so','openal':'libopenal.so','iconv':'libiconv.a','pixman':'libpixman-1.a','ssl':'libssl.a','crypto':'libcrypto.a'}
for name,fn in imports.items():
 lines += [f'add_library(dep_{name} '+('SHARED' if fn.endswith('.so') else 'STATIC')+' IMPORTED GLOBAL)',f'set_target_properties(dep_{name} PROPERTIES IMPORTED_LOCATION "${{PREFIX}}/lib/{fn}")']
for m in data['modules']:
 lines += [f'add_library({m["name"]} {("SHARED" if m.get("link") else "STATIC")}']+[ ' '+q(u['source']) for u in m['units']]+[')']
 for u in m['units']:
  lines += ['set_source_files_properties('+q(u['source'])+' PROPERTIES COMPILE_OPTIONS '+q(';'.join(u['flags'])).replace('\\;',';')+')']
for m in data['modules']:
 link=m.get('link')
 if not link:continue
 libs=[]
 for x in link['libraries']:
  if 'module' in x:libs.append(('dep_' if x['module'] in imports else '')+x['module'])
  else:
   fn=Path(x['archive']).name
   hit=[n for n,f in imports.items() if f==fn]
   libs.append('dep_'+hit[0] if hit else q(x['archive']))
 lines += [f'target_link_libraries({m["name"]} PRIVATE '+' '.join(libs+link['system'])+')',f'target_link_options({m["name"]} PRIVATE '+' '.join(q(x) for x in link['flags'])+')']
(b/'CMakeLists.txt').write_text('\n'.join(lines)+'\n')
(b/'LICENSE').write_text('MIT License\n\nCopyright (c) 2026 BRUMA contributors\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n')
print([(m['name'],m.keys()) for m in data['modules']])

