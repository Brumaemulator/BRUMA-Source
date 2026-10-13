if (Object.const_defined?("GameData") || Object.const_defined?("PBItems"))
  MKXP.puts("Loading cheats for Pokemon Essentials v19")

  def getPEVersion
    return "19" unless Object.const_defined?("Essentials")

    return Essentials.VERSION
  end

ENABLE_GET_ITEM = Object.const_defined?("PokemonMartScreen")

if ENABLE_GET_ITEM
  class CheatItemsAdapter < PokemonMartAdapter
    def getPrice(item, selling = false)
      return 0
    end
  end

  if Object.const_defined?("PokemonMart_Scene")
    class SceneCheat_Items < PokemonMart_Scene
    end
  else Object.const_defined?("PokemonMartScene")
    class SceneCheat_Items < PokemonMartScene
    end
  end

  class ScreenCheat_Items < PokemonMartScreen
    def initialize(scene,stock)
      @scene=scene
      @stock=stock
      @adapter=CheatItemsAdapter.new
    end
  end
end

  class Scene_Cheat
    def main
      if $wtw
        @wtwString = "Disable WTW"
      else
        @wtwString = "Enable WTW"
      end
      @cviewport=Viewport.new(0,0,Graphics.width,Graphics.height)
      @cviewport.z=99999
      @cheat_window = Window_CommandPokemon.new(["Get Items", "Heal Party",@wtwString,"Cancel"],160)
      @cheat_window.active = true
      @cheat_window.visible = true
      @cheat_window.viewport = @cviewport

      @partyArray = Array.new
      $Trainer.party.each{|pokemon|
        @partyArray.push(pokemon.name)
      }
      @partyArray.push("Cancel")
      Graphics.transition
      loop do
        Graphics.update
        Input.update
        pbUpdateSceneMap
        if update == -1
          break
        end
      end
      @cheat_window.dispose
      @cviewport.dispose
    end

    def update
      @cheat_window.update
      if @cheat_window.active
        return update_cheat
      end
    end

    def update_cheat
      if Input.trigger?(Input::B)
        return -1
      end
      if Input.trigger?(Input::C)
        case @cheat_window.index
        when 0
if ENABLE_GET_ITEM
          @cheat_window.active = false
          @cheat_window.visible = false
          scene=SceneCheat_Items.new
          array = Array.new
          if Object.const_defined?("GameData")
            GameData::Item.each do |i|
              array.push(i) unless i.name.empty?
            end
          else
            (0..PBItems.maxValue).each do |i|
              array.push(i) unless PBItems.getName(i).empty?
            end
          end
          
          screen=ScreenCheat_Items.new(scene,array)
          screen.pbBuyScreen
          pbScrollMap(-6,-5,-5)
end
          return -1
        when 1
          $Trainer.party.each{|pokemon|
            pokemon.heal
          }
          @cheat_window.active = false
          @cheat_window.visible = false
          return -1
        when 2
          if $wtw
            $wtw = false
          else
            $wtw = true
          end
          return -1
        when 3
          return -1
        end
        return
      end
    end

    def return_scene
      #dispose_menu_background
      @cviewport.dispose
    end
    def cheat_cancel
      @cheat_window.close
    end

    def create_menu_background
      @menuback_sprite = Sprite.new
      bm = Graphics.poke_snap_to_bitmap
      @menuback_sprite.bitmap = bm
      update_menu_background
    end
    def dispose_menu_background
      @menuback_sprite.dispose
    end
    def update_menu_background
    end
  end

  class Scene_Map
    alias cheatUpdateMaps updateMaps unless self.method_defined?(:cheatUpdateMaps)
    def updateMaps
      if Input.trigger?(Input::HOME) and $CHEATS
        $game_temp.menu_calling = false
        $game_temp.in_menu = true
        $game_player.straighten
        $game_map.update
        sscene=Scene_Cheat.new
        sscene.main
        $game_temp.in_menu = false
      end

      cheatUpdateMaps
    end
  end
  
  class Game_Player
    alias cheatPassable? passable? unless self.method_defined?(:cheatPassable?)
    def passable?(x, y, d, strict = false)
      if $wtw
	    new_x = x + (d == 6 ? 1 : d == 4 ? -1 : 0)
        new_y = y + (d == 2 ? 1 : d == 8 ? -1 : 0)
        return $game_map.valid?(new_x, new_y)
      else
        begin
          cheatPassable?(x, y, d, strict)
        rescue
          cheatPassable?(x,y,d)
        end
      end
    end
  end
end
