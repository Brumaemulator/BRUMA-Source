#!/bin/bash
# SPDX-License-Identifier: MIT
export PATH=/usr/bin:$PATH
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec /usr/bin/ruby --disable-gems -r"$ROOT/scripts/host-ruby-compat.rb" "$@"
