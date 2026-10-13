#Fixes for MapSaver script
class Map_Saver
  def initialize(map_id=0, x=0, y=0)
  end
  def set_scale(scale)
  end
  def mapshot
  end
  def screenshot
  end
end

module TH
  module Map_Saver
    Mapshot_Button = 3452345
    Screenshot_Button = 3452345
  end
end

#Fixes for MGQP
if MKXP.rpg_version > 2
  module DataManager
    def self.make_thumbnail
      @thumbnails = {}
      @current_thumbnail = nil
      @dummy_thumbnail = nil
      src_bitmap = SceneManager.background_bitmap
      @current_thumbnail = Bitmap.new(160, 120)
      @current_thumbnail.stretch_blt(@current_thumbnail.rect, src_bitmap, src_bitmap.rect)
      @dummy_thumbnail = Bitmap.new(160, 120)
      @dummy_thumbnail.fill_rect(@dummy_thumbnail.rect, Color.new(0, 0, 0))
      Dir.mkdir("Save") unless Dir.exist?("Save")
      Dir.entries("Save").each{|file_name|
        next unless ( file_name.end_with?(".png") & file_name.start_with?("Save") )
        begin
          @thumbnails[$1.to_i - 1] = Bitmap.new("Save/" + file_name)
        rescue
          @thumbnails[$1.to_i - 1] = Bitmap.new(160,120)
        end
      }
    end
  end
end

#Fix for fleeting iris
module Cache
  def self.bitmap_save_ss(hash,index)
    sp = Bitmap.new(205,150)
    sp.gradient_fill_rect(sp.rect, Color.new(*[80]*3), Color.new(*[20]*3), true)
    sp.draw_text(sp.rect,"Save File", 1)
    return sp
  end
  def self.savefile_picture(filename)
      Bitmap.new(160,120)
  end
end

class Bitmap
  def exportBitmap(fn, type, back = nil)
    Graphics.screenshot(fn)
  end
end

#Save fix for
class Game_Temp
  def create_save_preview
    w = 205
    h = 150
    @save_screenshot.dispose if @save_screenshot
    @save_screenshot = Bitmap.new(w, h)
    rect = Rect.new($game_player.screen_x - w/2, $game_player.screen_y - h/2 - 16, w, h)
    @save_screenshot.blt(0, 0, Graphics.snap_to_bitmap, rect)
  end
end

#Fixes for Vitamin Plus
module Wora_NSS
  SCREENSHOT_IMAGE = false
  PREMADE_IMAGE = false
end

#Use letter selection window on Pokemon Essentials
USEKEYBOARDTEXTENTRY = false

#Don't use tktk_bitmap on HN_Light
module TKTK_Bitmap
  def blend_blt(dest_bmp, x, y, src_bmp, rect, blend_type=0, opacity=255)
  end
end

#Fix MOG Anti Lag script
begin
  if Game_Event.method_defined?("anti_lag_initial_setup")
    MKXP.puts("Overriding anti_lag_initial_setup")
    class Game_Event < Game_Character
      def anti_lag_initial_setup
        @can_update = true
        rg = [(Graphics.width / 32) - 1, (Graphics.height / 32) - 1]
        @loop_map = ($game_map.loop_horizontal? or $game_map.loop_vertical?) ? true : false
        out_screen = MOG_ANTI_LAG::UPDATE_OUT_SCREEN_RANGE
        @antilag_range = [-out_screen, rg[0] + out_screen,rg[1] + out_screen]
      end
    end
  end
rescue
  MKXP.puts("Could not override anti_lag_initial_setup")
end

module MessageEnhance
  OK1 = false
  OB1 = false
  OK2 = false
  OB2 = false
  OK3 = false
  OB3 = false
  OB4 = false
  def self.invisible
    false
  end
end

module YSE
  module PATCH_SYSTEM
    if const_defined?(:LOAD_CONFIGURATION)
      LOAD_CONFIGURATION[:quit_fake] = false
    end
  end
end

module KGC
  module BitmapExtension
    DEFAULT_MODE = 0
  end
end

class Bitmap
  if method_defined?(:_draw_text)
    alias draw_text _draw_text
  end
  if method_defined?(:_draw_text)
    alias text_size _text_size
  end
end

module InputMouse
  def self.x
    return @@x
  end
  def self.y
    return @@y
  end
  def self.set_pos(x, y)
    return false
  end
  def self.press?(index)
    return false
  end
  def self.trigger?(index)
    return false
  end
  def self.repeat?(index)
    return false
  end
  def self.input_time(index)
    return -1
  end
  def self.input?
    return false
  end
  def self.wheel_delta
    return 0
  end
  def self.update
    return
  end
  def self.fullscreen?
    return true
  end
end

module ZiifSaveLayoutA
  File_column  = 2
  File_row     = 5
  D_Area       = false
  D_Story      = false
  def self.save_background_bitmap
    return Bitmap.new(48,48)
  end
  def self.load_background_bitmap
    return Bitmap.new(48,48)
  end
end

class Window_ZiifSaveFile
  def draw_save_bitmap
  end
end

begin
  class Scene_Map < Scene_Base
    def make_savedata_bitmap
      $game_temp.save_bitmap = Table.new(1,1,1)
    end
  end
rescue
end

module HN_Light
  class Light
    attr_reader :bitmap
    attr_reader :cells
    attr_reader :width
    attr_reader :height
    attr_reader :ox
    attr_reader :oy
    def initialize(light_type, s_zoom = 1)
    end

    def dispose
    end
  end
end
class Sprite_Dark < Sprite
  @@base_color = Color.new(255,255,255)
  def initialize(viewport = nil)
    super(viewport)
    @width = Graphics.width
    @height = Graphics.height
    @zoom = 1
    @width /= @zoom
    @height /= @zoom
    self.zoom_x = @zoom.to_f
    self.zoom_y = @zoom.to_f
    @light_cache = {}
  end
  def add_light(character)
  end
  def refresh
  end
  def update
    super
  end
  def dispose
    super
  end
end

module CConv
  def s2u8(str)
    return str;
  end
end

class Game_getCSVData
  def csv2ary(str)
    lines = []
    datas = []
    tmp_lines = str.split("\n")
    tmp_data = ""
    tmp_lines.each do |tmp_line|
      tmp_data += tmp_line
      if tmp_data.count("\"") % 2 == 0
        lines.push(tmp_data)
        tmp_data = ""
      else
        tmp_data += "\n"
      end
    end
    lines.each do |line|
      datas.push(csv_split_line(line))
    end
    return datas
  end
  
  def load_csv(filename)
    file = open(filename, "r")
    str = file.read
    file.close
    return SimpleCSV::csv2ary(str)
  end
end

#Fix Zeus Video Player
if defined? Graphics.zeus_play_movie
  module Graphics
    class << self
      def play_movie(filename, cancellable=true, fit_to_screen=true)
        zeus_play_movie(filename)
      end
    end
  end
end

MKXP.puts("Postload script is executed.")
