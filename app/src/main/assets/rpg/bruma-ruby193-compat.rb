# Bruma classic / Ruby 1.8 compatibility.
# This runtime's one-shot Inflate.inflate corrupts unrelated strings in memory.
# An independent stream avoids that path and always releases its native state.
class << Zlib::Inflate
  def inflate(data)
    unless data.kind_of?(String)
      raise TypeError, 'expected a String' unless data.respond_to?(:to_str)
      data = data.to_str
      raise TypeError, 'to_str must return a String' unless data.kind_of?(String)
    end
    stream = new
    begin
      output = stream.inflate(data)
      output << stream.finish unless stream.finished?
      output
    ensure
      stream.close
    end
  end
end

# This legacy battle extension defines a top-level helper, then explicitly
# calls it on a battle scene. Restrict the bridge to that receiver and helper;
# retain private visibility and propagate every other missing-method error.
class Object
  unless private_method_defined?(:bruma_previous_object_method_missing)
    alias_method :bruma_previous_object_method_missing, :method_missing
    def method_missing(name, *arguments, &block)
      if name == :raiseSpeciesStats && defined?(PokeBattle_Scene) &&
          kind_of?(PokeBattle_Scene) && private_methods.include?(name)
        return __send__(name, *arguments, &block)
      end
      bruma_previous_object_method_missing(name, *arguments, &block)
    end
    private :method_missing, :bruma_previous_object_method_missing
  end
end


# encoding: UTF-8
# SPDX-License-Identifier: MIT
# BRUMA-owned Ruby 1.8-to-1.9 adapter. Transforms evaluation copies only.
# Original game files, script archives and saves are never rewritten.
module BrumaRuby193Compat
  def self.normalize_source(source, filename)
    changed = source.gsub(/^(\s*when\s+[0-9]+):(?=\s|$)/, '\1 then')
    # Legacy retry in an unconditional menu iterator restarts the menu iteration.
    # Deliberately do not rewrite retries in exception handlers or other iterators.
    changed = changed.gsub(/^(\s*)retry if deleting\s*==\s*false\s*$/, '\1next if deleting==false')
    # C1 control range in UTF-8 regex literals must use codepoints, not invalid bytes.
    changed = changed.gsub('\\x7f-\\x9f', '\\u007f-\\u009f')
    # The legacy roaming-map listener uses non-local return in a top-level
    # proc. Ruby 1.9 raises LocalJumpError when its early exit is taken.
    # Convert only its two guard exits to block-local next; method returns
    # and other callbacks retain their original semantics.
    changed = changed.gsub(/(Events\.(?:onMapChange|onWildBattleEnd)\s*\+=\s*proc\s*\{\s*\|sender\s*,\s*e\|)([^}]*)(\})/m) do |listener|
      prefix, body, suffix = $1, $2, $3
      if body.include?('$PokemonGlobal.roamHistory')
        body = body.gsub(/(^[ \t]*)return(?=[ \t]+if[ \t]+(?:!\$PokemonGlobal|\$game_map[ \t]*&&[ \t]*mapinfos))/, '\1next')
        prefix + body + suffix
      elsif body.include?('$PokemonTemp.pokeradar') && body.include?('pbPokeRadarCancel')
        # The radar's non-grass battle exit has the same non-local return.
        body = body.gsub(/(^[ \t]*)return([ \t]*\r*)(?=\n|$)/, '\1next\2')
        prefix + body + suffix
      else
        listener
      end
    end
    if changed != source
      warn "BRUMA_RUBY193 normalized legacy syntax: #{filename}"
    end
    changed
  end
end
warn "BRUMA_RUBY193 Ruby=#{RUBY_VERSION}-p#{RUBY_PATCHLEVEL}; Win32API=#{defined?(Win32API)}; frame_rate=#{Graphics.frame_rate}"
# Ruby 1.9 removed Thread.critical. Serialize the legacy protected regions.
# This emulates the game's cache lock, not global suspension of unrelated threads.
class << Thread
  def critical
    @bruma_critical_owner == Thread.current
  end
  def critical=(value)
    @bruma_critical_mutex ||= Mutex.new
    if value
      unless @bruma_critical_owner == Thread.current
        @bruma_critical_mutex.lock
        @bruma_critical_owner = Thread.current
      end
    elsif @bruma_critical_owner == Thread.current
      @bruma_critical_owner = nil
      @bruma_critical_mutex.unlock
    end
    !!value
  end
end
warn 'BRUMA_RUBY193 Thread.critical uses an owned mutex for legacy protected regions'
# RGSS1 byte-index semantics: String[integer] returned an integer in Ruby 1.8.
class String
  # String#[] byte behavior is registered by our native adapter to preserve $~.
  def each(&block)
    each_line(&block)
  end unless method_defined?(:each)
end
warn 'BRUMA_RUBY193 restored String[Integer] byte result and String#each'

# Ruby 1.8 Object#id was the deprecated name of Object#object_id.
class Object
  alias_method :id, :object_id unless method_defined?(:id)
end

# Honor the game's declared RGSS1 canvas before its main loop. Older
# Essentials games draw at these dimensions without resizing mkxp's
# default 640x480 framebuffer, leaving unused black space.
module BrumaRuby193Compat
  def self.sync_display_size
    return unless Object.const_defined?(:DEFAULTSCREENWIDTH) && Object.const_defined?(:DEFAULTSCREENHEIGHT)
    width=Object.const_get(:DEFAULTSCREENWIDTH)
    height=Object.const_get(:DEFAULTSCREENHEIGHT)
    return unless width.kind_of?(Integer) && height.kind_of?(Integer)
    return unless width.between?(128,640) && height.between?(128,480)
    size=[width,height]
    return if @display_size==size
    Graphics.poke_resize_screen(width,height)
    @display_size=size
    warn "BRUMA_RUBY193 game canvas #{width}x#{height}"
  end
end

# Older Essentials explicitly invokes these helpers on Kernel. Ruby 1.9 keeps
# their module-function copies private. pbRgssOpen loads message tables; the
# game rescues its access error and silently returns empty species/move names.
# Forward ONLY these known legacy calls, including the file reader's block;
# preserve every other missing/private-method error and the game's own files.
class << Kernel
  unless private_method_defined?(:bruma_previous_method_missing)
    alias_method :bruma_previous_method_missing, :method_missing
    def method_missing(name, *arguments, &block)
      if (name == :pbAddDependency2 || name == :pbRgssOpen) && private_methods.include?(name)
        return __send__(name, *arguments, &block)
      end
      bruma_previous_method_missing(name, *arguments, &block)
    end
    private :method_missing, :bruma_previous_method_missing
  end
end
