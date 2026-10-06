# encoding: UTF-8
# SPDX-License-Identifier: MIT
# Original synthetic RGSS1 test; contains no third-party game content.
results=[]
def assert_native(name)
 raise name unless yield
end
assert_native('exact Ruby'){RUBY_VERSION=='1.9.3' && RUBY_PATCHLEVEL==551}
assert_native('RGSS1 default 40 FPS'){Graphics.frame_rate==40}
assert_native('Win32API available'){defined?(Win32API)}
a=Win32API.new('user32','GetAsyncKeyState','i','i')
assert_native('Win32API key state'){a.call(0x26).kind_of?(Integer)}
# Explicit disposal order plus unreachable object/lazy-GC stress.
60.times do
 b=Bitmap.new(32,32);s=Sprite.new;s.bitmap=b
 b.dispose;Graphics.update;s.dispose
 20.times { t=Sprite.new;t.bitmap=Bitmap.new(16,16) }
 GC.start if (Graphics.frame_count % 10)==0
 Graphics.update
end
value={:name=>'Español',:bytes=>"\x00\xff".force_encoding('ASCII-8BIT'),:color=>Color.new(1,2,3,4)}
File.open('native-test-save.dat','wb'){|f|Marshal.dump(value,f)}
loaded=File.open('native-test-save.dat','rb'){|f|Marshal.load(f)}
File.open('native-test-debug.txt','w'){|f|f.puts [loaded[:color].red,value[:color].red,loaded[:name].inspect,value[:name].inspect,loaded[:bytes].inspect,value[:bytes].inspect,loaded[:bytes].encoding,value[:bytes].encoding].inspect}
assert_native('RGSS Marshal colors'){loaded[:color].red==1 && loaded[:name]==value[:name] && loaded[:bytes]==value[:bytes]}
File.open('native-test-result.txt','w'){|f|f.puts "PASS Ruby=#{RUBY_VERSION}-p#{RUBY_PATCHLEVEL} default_rgss1_fps=#{Graphics.frame_rate} GC_sprites=1200 Win32API Marshal"}
