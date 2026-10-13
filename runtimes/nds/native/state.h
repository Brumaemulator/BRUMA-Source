// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <string>
#include <vector>
#include <cstdint>
struct BrumaState {std::string directory,savePath;std::vector<uint8_t> save;bool dirty=false;int stopReason=-1;};
extern BrumaState state;
bool flushSave();
