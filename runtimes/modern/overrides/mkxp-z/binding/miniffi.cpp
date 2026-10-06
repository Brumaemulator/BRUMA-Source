// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Android ARM64 integer/pointer calling contract; not a general-purpose FFI.
#include "miniffi.h"
#include <cassert>
mffi_value miniffi_call_intern(MINIFFI_FUNC function,MiniFFIFuncArgs* arguments,int count) {
    assert(count>=0 && count<=MINIFFI_MAX_ARGS);
    auto& v=arguments->params;
    return function(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9]);
}
