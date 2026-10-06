# Optional UI response for Essentials-style games. No movement/turbo changes.
module BrumaMenuRepeat
  def repeat?(key)
    return super unless [2, 4, 6, 8].include?(key) && defined?(Essentials)
    @bruma_repeat ||= {}
    unless press?(key)
      @bruma_repeat.delete(key)
      return false
    end
    now = Process.clock_gettime(Process::CLOCK_MONOTONIC)
    frame = Graphics.frame_count
    state = @bruma_repeat[key]
    if trigger?(key) || !state
      @bruma_repeat[key] = [now, -1, frame, true]
      return true
    end
    return state[3] if state[2] == frame
    elapsed = now - state[0]
    slot = elapsed < 0.25 ? -1 : ((elapsed - 0.25) / 0.07).floor
    result = slot >= 0 && slot > state[1]
    state[1] = slot
    state[2] = frame
    state[3] = result
    result
  end
end
Input.singleton_class.prepend(BrumaMenuRepeat)

module BrumaTextResponse
  def updateInternal
    original_delay = @text_delay
    # Keep instant text instant. Explicit dialogue pauses remain untouched.
    @text_delay = [original_delay, 1.0 / 60].min if original_delay.is_a?(Numeric) && original_delay > 0
    super
  ensure
    @text_delay = original_delay
  end
end
# Install after this game's text class is defined; do not copy game scripts.
bruma_text_hook = TracePoint.new(:end) do |event|
  if event.self.is_a?(Class) && event.self.name == 'Window_AdvancedTextPokemon'
    event.self.prepend(BrumaTextResponse) unless event.self.ancestors.include?(BrumaTextResponse)
    bruma_text_hook.disable
  end
end
bruma_text_hook.enable

# Cache complete shadowed text and formatted glyphs, not just font metrics.
# This avoids repeated TTF rasterization and texture uploads while navigating UI.
module BrumaTextCache
  LIMIT = 16 * 1024 * 1024
  @entries = {}
  @bytes = 0
  @hits = 0
  @misses = 0
  class << self
    attr_reader :hits, :misses
    def color(value)
      value ? [value.red,value.green,value.blue,value.alpha] : nil
    end
    def font(font)
      [font.name,font.size,font.bold,font.italic,color(font.color),
       font.respond_to?(:shadow) ? font.shadow : nil,
       font.respond_to?(:outline) ? font.outline : nil,
       font.respond_to?(:out_color) ? color(font.out_color) : nil]
    end
    # Intercept only private cache tiles. Never prepend Bitmap: games alias
    # Bitmap#draw_text and a global prepend can recurse through those aliases.
    def cache_draw_calls(tile)
      original=Bitmap.instance_method(:draw_text)
      tile.define_singleton_method(:draw_text) do |*args|
        unless args.size>=5 && args[0].is_a?(Numeric) && args[2]>0 && args[3]>0
          next original.bind(self).call(*args)
        end
        x,y,width,height,string,align=args
        align ||= 0
        cached=BrumaTextCache.stamp(self,x,y,width,height,[:text_layer,string,align]) do |layer,px,py|
          original.bind(layer).call(px,py,width,height,string,align)
        end
        original.bind(self).call(*args) unless cached
      end
    end
    def reserve(cost)
      while @bytes+cost>LIMIT || @entries.size>=512
        old=@entries.shift
        break unless old
        @bytes-=old[1][3]
        old[1][0].dispose unless old[1][0].disposed?
      end
    end
    def stamp(bitmap, x, y, width, height, identity, cache_text=false)
      offset=bitmap.respond_to?(:text_offset_y) ? (bitmap.text_offset_y || 0) : 0
      px=8;py=bitmap.font.size.abs+offset.abs+8
      w=width.ceil+2*px;h=height.ceil+2*py
      return false unless w>0 && h>0 && w<=1024 && h<=384 && !bitmap.disposed?
      key=Marshal.dump([identity,font(bitmap.font),offset,width,height])
      entry=@entries.delete(key)
      if entry && !entry[0].disposed?
        @hits+=1
      else
        @bytes-=entry[3] if entry
        @misses+=1
        cost=w*h*4
        reserve(cost)
        tile=Bitmap.new(w,h)
        begin
          tile.font=bitmap.font.clone
          tile.text_offset_y=offset if tile.respond_to?(:text_offset_y=)
          cache_draw_calls(tile) if cache_text
          yield(tile,px,py)
          reserve(cost) # Nested text layers may have filled the cache.
          entry=[tile,tile.rect,tile.font.clone,cost]
          @bytes+=cost
        rescue Exception
          tile.dispose unless tile.disposed?
          raise
        end
      end
      @entries[key]=entry
      bitmap.blt(x-px,y-py,entry[0],entry[1])
      bitmap.font=entry[2].clone
      true
    end
  end
end
module BrumaCachedTextHelpers
  def pbDrawShadowText(bitmap,x,y,width,height,string,base,shadow=nil,align=0)
    return super unless defined?(Essentials) && bitmap && string && width>0 && height>0
    identity=[:shadow,string,BrumaTextCache.color(base),BrumaTextCache.color(shadow),align]
    cached=BrumaTextCache.stamp(bitmap,x,y,width,height,identity,true) do |tile,px,py|
      super(tile,px,py,width,height,string,base,shadow,align)
    end
    super unless cached
  end
  def pbDrawOutlineText(bitmap,x,y,width,height,string,base,shadow=nil,align=0)
    return super unless defined?(Essentials) && bitmap && string && width>0 && height>0
    identity=[:outline,string,BrumaTextCache.color(base),BrumaTextCache.color(shadow),align]
    cached=BrumaTextCache.stamp(bitmap,x,y,width,height,identity,true) do |tile,px,py|
      super(tile,px,py,width,height,string,base,shadow,align)
    end
    super unless cached
  end
  def drawSingleFormattedChar(bitmap,ch)
    return super unless defined?(Essentials) && bitmap && !ch[5] && ch[0] && ch[0].match?(/[^\s\x00-\x02]/)
    identity=ch.each_with_index.map do |value,index|
      if index==1 || index==2 || index==14
        nil
      elsif value.is_a?(Color)
        BrumaTextCache.color(value)
      else
        value
      end
    end
    # These fields are all set by the original glyph helper; normalize before
    # lookup so an unrelated previously drawn letter does not fragment the cache.
    bitmap.font.name=ch[12];bitmap.font.size=ch[13]
    bitmap.font.bold=ch[6];bitmap.font.italic=ch[7];bitmap.font.color=ch[8]
    cached=BrumaTextCache.stamp(bitmap,ch[1],ch[2],ch[3]+8,ch[4],[:glyph,identity],true) do |tile,px,py|
      local=ch.dup;local[1]=px;local[2]=py
      super(tile,local)
    end
    super unless cached
  end
end
Object.prepend(BrumaCachedTextHelpers)


