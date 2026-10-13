MKXP.puts("Executing Cheat Script")

class Window_GetItemNumber < Window_Base
  def initialize(x, y)
    super(x, y, 304, 120)
    @item = nil
    @max = 1
    @price = 0
    @number = 1
  end
  def set(item, max, price)
    @item = item
    @max = max
    @price = 0
    @number = 1
    refresh
  end
  def number
    return @number
  end
  def refresh
    y = 0
    self.contents.clear
    draw_item_name(@item, 0, y)
    self.contents.font.color = normal_color
    self.contents.draw_text(212, y, 20, WLH, "×")
    self.contents.draw_text(248, y, 20, WLH, @number, 2)
    self.cursor_rect.set(244, y, 28, WLH)
    draw_currency_value(@price * @number, 4, y + WLH * 2, 264)
  end
  def update
    super
    if self.active
      last_number = @number
      if Input.repeat?(Input::RIGHT) and @number < @max
        @number += 1
      end
      if Input.repeat?(Input::LEFT) and @number > 1
        @number -= 1
      end
      if Input.repeat?(Input::UP) and @number < @max
        @number = [@number + 10, @max].min
      end
      if Input.repeat?(Input::DOWN) and @number > 1
        @number = [@number - 10, 1].max
      end
      if @number != last_number
        Sound.play_cursor
        refresh
      end
    end
  end
end

class Window_GetItem < Window_Selectable
  def initialize
    super(0, 0, 280, 304)
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
    self.contents.font.color = normal_color
    
    x = 4
    y = index * WLH
    rect = Rect.new(x, y, self.width - 32, WLH)
    self.contents.fill_rect(rect, Color.new(0, 0, 0, 0))
    opacity = self.contents.font.color == normal_color ? 255 : 128
    #self.contents.blt(x, y + 4, bitmap, Rect.new(0, 0, 24, 24), opacity)
    self.contents.draw_text(x + 4, y, 212, WLH, item.name, 0)
    self.contents.draw_text(x + 220, y, 88,WLH, item.price.to_s, 2)
  end
end

class Scene_Cheat
  def main
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

    @number_window = Window_GetItemNumber.new(0,0)
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
      number = $game_party.item_number(@item)
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
      $game_party.gain_item(@item, @number_window.number)
      
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
    $game_party.members[0].level_up unless $game_party.members[0].level >= 99
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
      $game_party.gain_gold(9999999 - $game_party.gold)
    else
      $game_party.gain_gold(100)
    end
  end

  def cheat_add_gold1000
    if check_max_gold(1000)
      $game_party.gain_gold(9999999 - $game_party.gold)
    else
      $game_party.gain_gold(1000)
    end
  end

  def cheat_add_gold10000
    if check_max_gold(10000)
      $game_party.gain_gold(9999999 - $game_party.gold)
    else
      $game_party.gain_gold(10000)
    end
  end

  def cheat_add_gold100000
    if check_max_gold(100000)
      $game_party.gain_gold(9999999 - $game_party.gold)
    else
      $game_party.gain_gold(100000)
    end
  end

  def cheat_add_gold1000000
    if check_max_gold(1000000)
      $game_party.gain_gold(9999999 - $game_party.gold)
    else
      $game_party.gain_gold(1000000)
    end
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
MKXP.puts("Cheat script is executed.")
