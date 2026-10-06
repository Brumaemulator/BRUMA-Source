// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
#pragma once
#include <cstdint>
using mffi_value = unsigned long;
constexpr long MINIFFI_MAX_ARGS = 10;
using MINIFFI_FUNC = mffi_value (*)(mffi_value,mffi_value,mffi_value,mffi_value,mffi_value,
                                   mffi_value,mffi_value,mffi_value,mffi_value,mffi_value);
struct MiniFFIFuncArgs { mffi_value params[MINIFFI_MAX_ARGS]; };
mffi_value miniffi_call_intern(MINIFFI_FUNC,MiniFFIFuncArgs*,int);
