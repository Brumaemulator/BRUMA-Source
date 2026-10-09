class << Kernel
  def pbAddDependency2(*args,&block);[args,block ? block.call : nil];end
  def unrelated_private_helper;:must_not_be_public;end
  private :pbAddDependency2,:unrelated_private_helper
end
raise 'legacy explicit call failed' unless Kernel.pbAddDependency2(10,'npc'){42}==[[10,'npc'],42]
raise 'visibility changed' unless Kernel.private_methods.include?(:pbAddDependency2)
begin
  Kernel.unrelated_private_helper
  raise 'unrelated private method became callable'
rescue NoMethodError
end
class << Kernel
  remove_method :pbAddDependency2
end
begin
  Kernel.pbAddDependency2(10)
  raise 'undefined helper was swallowed'
rescue NoMethodError
end
puts 'PASS: exact legacy Kernel private call, args/block, unchanged visibility and unrelated errors'

class PokeBattle_Scene; end
def raiseSpeciesStats(value,&block); [value,block.call]; end
scene=PokeBattle_Scene.new
raise 'scene helper failed' unless scene.raiseSpeciesStats(7){9} == [7,9]
raise 'scene visibility changed' unless scene.private_methods.include?(:raiseSpeciesStats)
begin
  Object.new.raiseSpeciesStats(7){9}
  raise 'helper became callable on unrelated receivers'
rescue NoMethodError
end
puts 'PASS: exact legacy battle helper, args/block, receiver restriction and unchanged visibility'

class << Kernel
  def pbRgssOpen(path,mode)
    raise 'wrong reader arguments' unless path=='messages.dat' && mode=='rb'
    yield Marshal.dump([[nil,'Aurora'],[nil,'Spark']])
    nil
  end
  private :pbRgssOpen
end
messages=nil
Kernel.pbRgssOpen('messages.dat','rb'){|data| messages=Marshal.load(data)}
raise 'message reader lost species/move names' unless messages[0][1]=='Aurora' && messages[1][1]=='Spark'
raise 'reader visibility changed' unless Kernel.private_methods.include?(:pbRgssOpen)
puts 'PASS: private message reader preserves arguments/block and Marshal table names'


module Events
  class ListenerList < Array
    def +(callback); push(callback); self; end
  end
  class << self
    attr_accessor :onMapChange, :onWildBattleEnd
  end
end
def pbPokeRadarCancel; nil; end
Events.onMapChange=Events::ListenerList.new
Events.onWildBattleEnd=Events::ListenerList.new
$PokemonGlobal=nil
source="Events.onMapChange+=proc {|sender,e|\n return if !$PokemonGlobal\n # $PokemonGlobal.roamHistory\n}\n"
eval(BrumaRuby193Compat.normalize_source(source,'owned-map-fixture'))
Events.onMapChange.first.call(nil,[0])
source="Events.onWildBattleEnd+=proc {|sender,e|\r\r\n pbPokeRadarCancel\r\r\n return\r\r\n $PokemonTemp.pokeradar\r\r\n}\r\r\n"
eval(BrumaRuby193Compat.normalize_source(source,'owned-battle-fixture'))
Events.onWildBattleEnd.first.call(nil,[0])
source="def ordinary_method\n return 17\nend\n"
raise 'unrelated return changed' unless BrumaRuby193Compat.normalize_source(source,'owned-method-fixture')==source
puts 'PASS: map and battle callbacks exit locally; ordinary returns unchanged'
def bruma_legacy_detection_fixture
  case 1
  when 1: true
  end
end
File.open('bruma-kernel-regression.txt','wb'){|f|f.write("PASS Ruby="+RUBY_VERSION+"-p"+RUBY_PATCHLEVEL.to_s)}
loop do
  Graphics.update
  Input.update
end
