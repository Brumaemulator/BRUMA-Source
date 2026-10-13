# SPDX-License-Identifier: MIT
require 'zlib'
root=File.expand_path('../app/src/debug/assets/rpg-regression',File.dirname(__FILE__))
File.binwrite(File.join(root,'Scripts.rxdata'),Marshal.dump([[1,'Original regression',Zlib::Deflate.deflate(File.binread(File.join(root,'regression.rb')))]]))
