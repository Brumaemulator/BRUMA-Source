# encoding: UTF-8
# SPDX-License-Identifier: MIT
# BRUMA test-only assertions, not game modifications.
def check(name)
 begin
  raise 'assertion failed' unless yield
  puts "PASS #{name}"
 rescue Exception => e
  puts "FAIL #{name}: #{e.class}: #{e.message}"
  raise
 end
end
check('exact interpreter'){RUBY_VERSION=='1.9.3' && RUBY_PATCHLEVEL==551}
sample=("\x00\xff\x80hola\n".force_encoding('ASCII-8BIT')*10000)
check('Zlib one-shot'){Zlib::Inflate.inflate(Zlib::Deflate.deflate(sample))==sample}
check('Zlib stream'){z=Zlib::Inflate.new;result=z.inflate(Zlib::Deflate.deflate(sample));result << z.finish unless z.finished?;z.close;result==sample}
value={:nested=>[1,2**100,-42,3.25,true,false,nil,'Español',sample]}
check('Marshal 4.8 roundtrip'){Marshal.load(Marshal.dump(value))==value && Marshal.dump(value)[0,2].bytes.to_a==[4,8]}
check('UTF-8 regex'){/Pokémon/.match('¡Pokémon Z!') && 'España'.scan(/./).size==6}
check('Shift_JIS transcode'){s='ポケモン'.encode('Shift_JIS');s.encoding.name=='Shift_JIS' && s.encode('UTF-8')=='ポケモン'}
check('Latin-1 transcode'){'Español'.encode('ISO-8859-1').encode('UTF-8')=='Español'}
check('binary file save/load'){path='/data/local/tmp/bruma-ruby193-save-test';File.open(path,'wb'){|f|f.write(Marshal.dump(value))};read=File.open(path,'rb'){|f|Marshal.load(f)};File.unlink(path);read==value}
check('legacy String integer byte'){'A'[0]==65 && 'A'[-1]==65 && 'A'[2].nil?}
check('regex backref scope'){'abc'[/b/] == 'b' && $~.pre_match == 'a'}
check('String game alias does not recurse'){klass=Class.new(String);klass.class_eval{alias getbyte []};klass.new('A').getbyte(0)==65}
check('Big5 transcode'){'中文'.encode('Big5').encode('UTF-8')=='中文'}
check('GB18030 transcode'){'中文'.encode('GB18030').encode('UTF-8')=='中文'}
if ARGV[0]
 scripts=Marshal.load(File.binread(ARGV[0]));good=0;bad=[]
 scripts.each do |row|
  next unless row && row[2].kind_of?(String)
  text=Zlib::Inflate.inflate(row[2]);text.force_encoding('UTF-8')
  begin
   RubyVM::InstructionSequence.compile(text,row[1].to_s);good+=1
  rescue SyntaxError => e
   bad << [row[1],e.message]
  end
 end
 puts "RGSS_SYNTAX compiled=#{good} rejected=#{bad.size}"
 bad.each{|name,message|puts "RGSS_SYNTAX_FAILURE #{name}: #{message}"}
end
puts 'CORE_SELFTEST_COMPLETE'
