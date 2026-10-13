MKXP.puts("Executing Cheat Script")

if !$PokemonSystem.nil?
  MKXP.puts("Loading cheats for Pokemon Essentials")
  class CheatItemsAdapter
    def getMoney
      return $Trainer.money
    end
    def setMoney(value)
      $Trainer.money=value
    end
    def getInventory()
      return $PokemonBag
    end
    def getPrice(item,selling=false)
      return 0
    end
    def getItemIcon(item)
      begin
        return item ? pbItemIconFile(item) : nil
      rescue
        return sprintf("Graphics/Icons/itemBack") if item==0
        return !item ? nil : sprintf("Graphics/Icons/item%03d",item)
      end
    end
    def getItemIconRect(item)
      return Rect.new(0,0,48,48)
    end
    def getDisplayName(item)
      itemname=PBItems.getName(item)
      if $ItemData[item][ITEMPOCKET]==3
        machine=$ItemData[item][ITEMMACHINE]
        itemname=_INTL("{1} {2}",itemname,PBMoves.getName(machine))
      elsif $ItemData[item][ITEMPOCKET]==4 #mod
        firstberry=PBItems::CHERIBERRY
        itemname=_ISPRINTF("No{1:02d} {2:s}",item-firstberry+1,itemname)
      end
      return itemname
    end
    def getName(item)
      return PBItems.getName(item)
    end
    def getDisplayPrice(item, selling=false)
      return _ISPRINTF("${1:d}",0)
    end
    def getDescription(item)
      return pbGetMessage(MessageTypes::ItemDescriptions,item)
    end
    def addItem(item)
      return $PokemonBag.pbStoreItem(item)
    end
    def getQuantity(item)
      return $PokemonBag.pbQuantity(item)
    end
    def canSell?(item)
      return getPrice(item)>0 && !pbIsImportantItem?(item)
    end
    def showQuantity?(item)
      return !pbIsImportantItem?(item)
    end
    def removeItem(item)
      return $PokemonBag.pbDeleteItem(item)
    end
  end

  class ScreenCheat_Items < PokemonMartScreen
    def initialize(scene,stock)
      @scene=scene
      @stock=stock
      @adapter=CheatItemsAdapter.new
    end
  end

  if Object.const_defined?("PokemonMartScene")
    class SceneCheat_Items < PokemonMartScene
    end
  else
    class SceneCheat_Items < PokemonMart_Scene
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
      @cheat_window = Window_CommandPokemon.new(["Get Items", "Heal Party", "Level Up",@wtwString,"Cancel"],160)
      @cheat_window.active = true
      @cheat_window.visible = true
      @cheat_window.viewport = @cviewport

      @partyArray = Array.new
      $Trainer.party.each{|pokemon|
        @partyArray.push(pokemon.name)
      }
      @partyArray.push("Cancel")
      @level_window = Window_CommandPokemon.new(@partyArray,160)
      @level_window.active = false
      @level_window.visible = false
      @level_window.viewport = @cviewport
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
      @level_window.dispose
      @cviewport.dispose
    end

    def update
      @cheat_window.update
      @level_window.update
      if @cheat_window.active
        return update_cheat
      end
      if @level_window.active
        return update_level
      end
    end

    def update_cheat
      if Input.trigger?(Input::B)
        return -1
      end
      if Input.trigger?(Input::C)
        case @cheat_window.index
        when 0
          @cheat_window.active = false
          @cheat_window.visible = false
          scene=SceneCheat_Items.new
          tmpArray = Array(0..PBItems.getCount)
          array = Array.new
          tmpArray.each{|item|
            array.push(item) unless PBItems.getName(item).empty?
          }

          screen=ScreenCheat_Items.new(scene,array)
          begin
            screen.pbBuyScreen(false)
          rescue
            screen.pbBuyScreen
          end
          pbScrollMap(-6,-5,-5)
          return -1
        when 1
          $Trainer.party.each{|pokemon|
            pokemon.heal
          }
          @cheat_window.active = false
          @cheat_window.visible = false
          return -1
        when 2
          @cheat_window.active = false
          @cheat_window.visible = false
          @level_window.active = true
          @level_window.visible = true
          @level_window.refresh
        when 3
          if $wtw
            $wtw = false
          else
            $wtw = true
          end
          return -1
        when 4
          return -1
        end
        return
      end
    end

    def update_level
      if Input.trigger?(Input::B)
        return -1
      end
      if Input.trigger?(Input::C)
        if @level_window.index == (@partyArray.size - 1)
          return -1
        else
          pokemon = $Trainer.party[@level_window.index]
          begin
            pbChangeLevel(pokemon, pokemon.level + 1) unless pokemon.level >= PBExperience::MAXLEVEL
          rescue
          end
          begin
            pbChangeLevel(pokemon, pokemon.level + 1,self,false) unless pokemon.level >= PBExperience::MAXLEVEL
            return
          rescue
          end
          begin
            pbChangeLevel(pokemon, pokemon.level + 1,self) unless pokemon.level >= PBExperience::MAXLEVEL
            return
          rescue
          end
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
    def updateMaps
      if Input.trigger?(Input::HOME) and $CHEATS
        $game_temp.menu_calling = false
        $game_player.straighten
        $game_map.update
        sscene=Scene_Cheat.new
        sscene.main
      end
      for map in $MapFactory.maps
        map.update
      end
      $MapFactory.updateMaps(self)
    end
  end
else
  if (defined?(Window_Selectable) == nil) && (defined?(Window_DrawableCommand) != nil)
    class Window_Selectable < Window_DrawableCommand
    end
  end
  
  class Window_GetItem < Window_Selectable
    def initialize
      super(0, 128, 368, 352)
      @shop_goods = []
      @shop_goods += $data_items
      @shop_goods += $data_weapons
      @shop_goods += $data_armors
      refresh
      self.index = 0
    end
    def item
      return @data[self.index]
    end
    def refresh
      if self.contents != nil
        self.contents.dispose
        self.contents = nil
      end
      @data = []
      for item in @shop_goods
        if item != nil && item.name != ""
          @data.push(item)
        end
      end
      @item_max = @data.size
      if @item_max > 0
        self.contents = Bitmap.new(width - 32, row_max * 32)
        for i in 0...@item_max
          draw_item(i)
        end
      end
    end
    def draw_item(index)
      item = @data[index]
      case item
      when RPG::Item
        number = $game_party.item_number(item.id)
      when RPG::Weapon
        number = $game_party.weapon_number(item.id)
      when RPG::Armor
        number = $game_party.armor_number(item.id)
      end
      if number < 99
        self.contents.font.color = normal_color
      else
        self.contents.font.color = disabled_color
      end
      x = 4
      y = index * 32
      rect = Rect.new(x, y, self.width - 32, 32)
      self.contents.fill_rect(rect, Color.new(0, 0, 0, 0))
      bitmap = RPG::Cache.icon(item.icon_name)
      opacity = self.contents.font.color == normal_color ? 255 : 128
      self.contents.blt(x, y + 4, bitmap, Rect.new(0, 0, 24, 24), opacity)
      self.contents.draw_text(x + 28, y, 212, 32, item.name, 0)
      self.contents.draw_text(x + 240, y, 88, 32, item.price.to_s, 2)
    end
  end

  class Scene_Cheat
    def main
      create_menu_background
      @cheat_window = Window_Command.new(160,["Level Up", "Gain Gold", "Get Items","Cancel"])
      @cheat_window.active = true
      @cheat_window.visible = true

      @level_window = Window_Command.new(160,["1 Level", "5 Level", "10 Level","100 Level","Cancel"])
      @level_window.active = false
      @level_window.visible = false

      @gold_window = Window_Command.new(160,["100 G", "1K G", "10K G","100K G","1M G","Cancel"])
      @gold_window.active = false
      @gold_window.visible = false

      @item_window = Window_GetItem.new
      @item_window.active = false
      @item_window.visible = false

      @number_window = Window_ShopNumber.new
      @number_window.active = false
      @number_window.visible = false

      Graphics.transition
      loop do
        Graphics.update
        Input.update
        update
        if $scene != self
          break
        end
      end
      Graphics.freeze
      @cheat_window.dispose
      @level_window.dispose
      @gold_window.dispose
      @item_window.dispose
      @number_window.dispose
    end

    def update
      @cheat_window.update
      @level_window.update
      @gold_window.update
      @item_window.update
      @number_window.update
      if @cheat_window.active
        update_cheat
        return
      end
      if @level_window.active
        update_level
        return
      end
      if @gold_window.active
        update_gold
        return
      end
      if @item_window.active
        update_item
        return
      end
      if @number_window.active
        update_number
        return
      end
    end

    def update_cheat
      if Input.trigger?(Input::B)
        $scene = Scene_Map.new
        return
      end
      if Input.trigger?(Input::C)
        case @cheat_window.index
        when 0
          @cheat_window.active = false
          @cheat_window.visible = false
          @level_window.active = true
          @level_window.visible = true
          @level_window.refresh
        when 1
          @cheat_window.active = false
          @cheat_window.visible = false
          @gold_window.active = true
          @gold_window.visible = true
          @gold_window.refresh
        when 2
          @cheat_window.active = false
          @cheat_window.visible = false
          @item_window.active = true
          @item_window.visible = true
          @item_window.refresh
        when 3
          $scene = Scene_Map.new
        end
        return
      end
    end

    def update_level
      if Input.trigger?(Input::B)
        $scene = Scene_Map.new
        return
      end
      if Input.trigger?(Input::C)
        case @level_window.index
        when 0
          cheat_level_up1
          $scene = Scene_Map.new
        when 1
          cheat_level_up5
          $scene = Scene_Map.new
        when 2
          cheat_level_up10
          $scene = Scene_Map.new
        when 3
          cheat_level_up100
          $scene = Scene_Map.new
        when 4
          $scene = Scene_Map.new
        end
        return
      end
    end

    def update_gold
      if Input.trigger?(Input::B)
        $scene = Scene_Map.new
        return
      end
      if Input.trigger?(Input::C)
        case @gold_window.index
        when 0
          cheat_add_gold100
          $scene = Scene_Map.new
        when 1
          cheat_add_gold1000
          $scene = Scene_Map.new
        when 2
          cheat_add_gold10000
          $scene = Scene_Map.new
        when 3
          cheat_add_gold100000
          $scene = Scene_Map.new
        when 4
          cheat_add_gold1000000
          $scene = Scene_Map.new
        when 5
          $scene = Scene_Map.new
        end
        return
      end
    end

    def update_item
      if Input.trigger?(Input::B)
        $scene = Scene_Map.new
        return
      end
      if Input.trigger?(Input::C)
        @item = @item_window.item
        if @item == nil
          return
        end
        case @item
        when RPG::Item
          number = $game_party.item_number(@item.id)
        when RPG::Weapon
          number = $game_party.weapon_number(@item.id)
        when RPG::Armor
          number = $game_party.armor_number(@item.id)
        end
        if number == 99
          return
        end
        max = 99
        max = [max, 99 - number].min
        @item_window.active = false
        @item_window.visible = false
        @number_window.set(@item, max, @item.price)
        @number_window.active = true
        @number_window.visible = true
      end
    end

    def update_number
      if Input.trigger?(Input::B)
        @number_window.active = false
        @number_window.visible = false
        @item_window.active = true
        @item_window.visible = true
        return
      end
      if Input.trigger?(Input::C)
        @number_window.active = false
        @number_window.visible = false
        case @item
        when RPG::Item
          $game_party.gain_item(@item.id, @number_window.number)
        when RPG::Weapon
          $game_party.gain_weapon(@item.id, @number_window.number)
        when RPG::Armor
          $game_party.gain_armor(@item.id, @number_window.number)
        end
        @item_window.refresh
        @item_window.active = true
        @item_window.visible = true
        return
      end
    end

    def cheat_cancel
      @cheat_window.close
      return_scene
    end
    def cheat_level_cancel
      @level_window.close
      return_scene
    end
    def cheat_gold_cancel
      @gold_window.close
      return_scene
    end

    def cheat_level_up
      $game_party.actors[0].level = $game_party.actors[0].level+1
    end

    def cheat_level_up1
      cheat_level_up
    end

    def cheat_level_up5
      5.times{cheat_level_up}
    end

    def cheat_level_up10
      10.times{cheat_level_up}
   end

    def cheat_level_up100
      100.times{cheat_level_up}
    end

    def check_max_gold(gold)
      if ($game_party.gold + gold) >=  9999999
        return true
      else
        return false
      end
    end
    def cheat_add_gold100
      if check_max_gold(100)
        $game_party.gain_gold($game_party.max_gold - $game_party.gold)
      else
        $game_party.gain_gold(100)
      end
    end

    def cheat_add_gold1000
      if check_max_gold(1000)
        $game_party.gain_gold($game_party.max_gold - $game_party.gold)
      else
        $game_party.gain_gold(1000)
      end
    end

    def cheat_add_gold10000
      if check_max_gold(10000)
        $game_party.gain_gold($game_party.max_gold - $game_party.gold)
      else
        $game_party.gain_gold(10000)
      end
    end

    def cheat_add_gold100000
      if check_max_gold(100000)
        $game_party.gain_gold($game_party.max_gold - $game_party.gold)
      else
        $game_party.gain_gold(100000)
      end
    end

    def cheat_add_gold1000000
      if check_max_gold(1000000)
        $game_party.gain_gold($game_party.max_gold - $game_party.gold)
      else
        $game_party.gain_gold(1000000)
      end
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

  class Game_Player
    alias :cheat_update :update
    def update
      cheat_update
      if Input.trigger?(Input::HOME) and $CHEATS
        $scene = Scene_Cheat.new
      end
    end
  end
end
MKXP.puts("Cheat script is executed.")
