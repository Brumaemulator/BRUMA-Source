MKXP.puts("Executing Cheat Script")

class Window_GetItem < Window_Selectable
  def initialize(x, y, height,type)
    super(x, y, window_width, height)
    @shop_goods = []
    if type == 0
      @shop_goods += $data_items
    end
    if type == 1
      @shop_goods += $data_weapons
    end
    if type == 2
      @shop_goods += $data_armors
    end
    @money = 0
    refresh
    select(0)
  end
  def window_width
    return 304
  end
  def item_max
    @data ? @data.size : 1
  end
  def item
    @data[index]
  end
  def money=(money)
    @money = money
    refresh
  end
  def current_item_enabled?
    enable?(@data[index])
  end
  def price(item)
    @price[item]
  end
  def enable?(item)
    item && !$game_party.item_max?(item)
  end
  def refresh
    make_item_list
    create_contents
    draw_all_items
  end
  def make_item_list
    @data = []
    @price = {}
    @shop_goods.each do |good|
      if good && good.name != ""
        @data.push(good)
        @price[good] = 0
      end
    end
  end
  def draw_item(index)
    item = @data[index]
    rect = item_rect(index)
    draw_item_name(item, rect.x, rect.y, enable?(item))
    rect.width -= 4
    draw_text(rect, price(item), 2)
  end
end


class Game_Player
  alias :cheat_update :update
  def update
    cheat_update
    if Input.trigger?(Input::HOME)  and $CHEATS
      Sound.play_ok
      SceneManager.call(Scene_Cheat)
      Window_CheatCommand::init_command_position
    end
  end
end

class Window_CheatCommand < Window_Command
  def make_command_list
    add_command("Level Up", :level)
    add_command("Gain Gold", :gold)
    add_command("Get Items", :getItems)
    add_command("Get Weapons", :getWeapons)
    add_command("Get Armors", :getArmors)
    add_command("Cancel", :cancel)
  end

  def self.init_command_position
    @@last_command_symbol = nil
  end

  def initialize
    super(0,0)
    @@last_command_symbol = self
  end
end

class Window_Cheat_Level < Window_CheatCommand
  def make_command_list
    add_command("1 Level", :levelUp1)
    add_command("5 Level", :levelUp5)
    add_command("10 Level", :levelUp10)
    add_command("100 Level", :levelUp100)
    add_command("Cancel", :cancel)
  end
end

class Window_Cheat_Gold < Window_CheatCommand
  def make_command_list
    add_command("100 G", :addGold100)
    add_command("1K G", :addGold1000)
    add_command("10K G", :addGold10000)
    add_command("100K G", :addGold100000)
    add_command("1M G", :addGold1000000)
    add_command("Cancel", :cancel)
  end
end


class Scene_Cheat < Scene_MenuBase
  def start
    super
    create_command_window
  end


  def create_command_window
    @cheat_window = Window_CheatCommand.new
    @cheat_window.set_handler(:level, method(:cheat_level))
    @cheat_window.set_handler(:gold, method(:cheat_gold))
    @cheat_window.set_handler(:getItems, method(:cheat_items))
    @cheat_window.set_handler(:getWeapons, method(:cheat_weapons))
    @cheat_window.set_handler(:getArmors, method(:cheat_armors))
    @cheat_window.set_handler(:cancel, method(:cheat_cancel))
  end

  def cheat_level
    @cheat_window.close
    @level_window = Window_Cheat_Level.new
    @level_window.set_handler(:levelUp1, method(:cheat_level_up1))
    @level_window.set_handler(:levelUp5, method(:cheat_level_up5))
    @level_window.set_handler(:levelUp10, method(:cheat_level_up10))
    @level_window.set_handler(:levelUp100, method(:cheat_level_up100))
    @level_window.set_handler(:cancel, method(:cheat_level_cancel))
  end

  def cheat_gold
    @cheat_window.close
    @gold_window = Window_Cheat_Gold.new
    @gold_window.set_handler(:addGold100, method(:cheat_add_gold100))
    @gold_window.set_handler(:addGold1000, method(:cheat_add_gold1000))
    @gold_window.set_handler(:addGold10000, method(:cheat_add_gold10000))
    @gold_window.set_handler(:addGold100000, method(:cheat_add_gold100000))
    @gold_window.set_handler(:addGold1000000, method(:cheat_add_gold1000000))
    @gold_window.set_handler(:cancel, method(:cheat_gold_cancel))
  end

  def cheat_getItem(type)
    @cheat_window.close
    @item_window = Window_GetItem.new(0,0,320,type)
    @item_window.set_handler(:ok,     method(:on_buy_ok))
    @item_window.set_handler(:cancel, method(:on_buy_cancel))
    @item_window.show.activate
    @number_window = Window_ShopNumber.new(0, 304, 120)
    @number_window.hide
    @number_window.set_handler(:ok,     method(:on_number_ok))
    @number_window.set_handler(:cancel, method(:on_number_cancel))
  end
  
  def cheat_items
    cheat_getItem(0)
  end
  
  def cheat_weapons
    cheat_getItem(1)
  end
  
  def cheat_armors
    cheat_getItem(2)
  end

  def on_buy_ok
    @item = @item_window.item
    @item_window.hide
    @number_window.set(@item, max_buy, buying_price, "G")
    @number_window.show.activate
  end
  def on_buy_cancel
    @item_window.close
    return_scene
  end
  def on_number_ok
    do_buy(@number_window.number)
    @number_window.hide
    @item_window.show.activate
  end
  def on_number_cancel
    @number_window.hide
    @item_window.show.activate
  end
  def do_buy(number)
    $game_party.gain_item(@item, number)
  end
  def max_buy
    max = $game_party.max_item_number(@item) - $game_party.item_number(@item)
    buying_price == 0 ? max : [max, money / buying_price].min
  end
  def buying_price
    @item_window.price(@item)
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
    $game_player.actor.level_up unless $game_player.actor.max_level?
  end

  def cheat_level_up1
    cheat_level_up
    @level_window.close
    return_scene
    $game_message.add("Player is Level "+$game_player.actor.level.to_s+" now.")
  end

  def cheat_level_up5
    5.times{cheat_level_up}
    @level_window.close
    return_scene
    $game_message.add("Player is Level "+$game_player.actor.level.to_s+" now.")
  end

  def cheat_level_up10
    10.times{cheat_level_up}
    @level_window.close
    return_scene
    $game_message.add("Player is Level "+$game_player.actor.level.to_s+" now.")
  end

  def cheat_level_up100
    100.times{cheat_level_up}
    @level_window.close
    return_scene
    $game_message.add("Player is Level "+$game_player.actor.level.to_s+" now.")
  end

  def check_max_gold(gold)
    if ($game_party.gold + gold) >=  $game_party.max_gold
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
    @gold_window.close
    return_scene
    $game_message.add("Player has "+$game_party.gold.to_s+"G now.")
  end

  def cheat_add_gold1000
    if check_max_gold(1000)
      $game_party.gain_gold($game_party.max_gold - $game_party.gold)
    else
      $game_party.gain_gold(1000)
    end
    @gold_window.close
    return_scene
    $game_message.add("Player has "+$game_party.gold.to_s+"G now.")
  end

  def cheat_add_gold10000
    if check_max_gold(10000)
      $game_party.gain_gold($game_party.max_gold - $game_party.gold)
    else
      $game_party.gain_gold(10000)
    end
    @gold_window.close
    return_scene
    $game_message.add("Player has "+$game_party.gold.to_s+"G now.")
  end

  def cheat_add_gold100000
    if check_max_gold(100000)
      $game_party.gain_gold($game_party.max_gold - $game_party.gold)
    else
      $game_party.gain_gold(100000)
    end
    @gold_window.close
    return_scene
    $game_message.add("Player has "+$game_party.gold.to_s+"G now.")
  end

  def cheat_add_gold1000000
      if check_max_gold(1000000)
        $game_party.gain_gold($game_party.max_gold - $game_party.gold)
      else
        $game_party.gain_gold(1000000)
      end
      @gold_window.close
      return_scene
      $game_message.add("Player has "+$game_party.gold.to_s+"G now.")
    end
end
MKXP.puts("Cheat script is executed.")
