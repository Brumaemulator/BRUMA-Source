# coding: utf-8
MKXP.puts("Executing preload script")

def include(m)
  ObjectSpace.each_object(Class){|ob| ob.include(m)}
end
def numeric?
  return false
end
def >=(a)
  return false
end

$DEBUG = false

class String
  def to_s
    return self.delete("\r")
  end
end

class Float
  def ^(power)
    return self**power
  end
  def &(num)
    return self&&num
  end
  def <<(num)
    return self * (2**num)
  end
  def >>(num)
    return self / (2**num)
  end
end

#Fake DL::CFunc implementation
module DL
  class CFunc
    def initialize(func,type)
      return func
    end
	def initialize(func, type)
	  @called = false
	  @impl = Win32API.new("User32",func,%w(l p),'i')
	end

	def call(*args)
	  if @impl
		return @impl.call(args)
	  end
	  return 0
	end
  end

  def self.dlopen(lib = '')
    if lib.downcase == "user32"
      dll = Hash.new
      dll['GetActiveWindow'] = "GetActiveWindow"
      dll['GetSystemMetrics'] = "GetSystemMetrics"
      dll['GetWindowRect'] = "GetWindowRect"
      dll['SetWindowLong'] = "SetWindowLong"
      dll['SetWindowPos'] = "SetWindowPos"
      return dll
    elsif lib.downcase == "kernel32"
      dll = Hash.new
      dll['GetModuleHandle'] = "GetModuleHandle"
      dll['GetPrivateProfileString'] = "GetPrivateProfileString"
      return dll
    else
      return Hash.new
    end
  end

end

Graphics::PlaneSpeedUp = false

#Set Fake Environment Variables
ENV["ALLUSERSPROFILE"] = "UserData"
ENV["APPDATA"] = "UserData/AppData"
ENV["COMPUTERNAME"] = "JoiPlay"
ENV["HOMEDRIVE"] = ""
ENV["HOMEPATH"] = "UserData"
ENV["LOCALAPPDATA"] = "UserData/AppData"
ENV["NUMBER_OF_PROCESSORS"] = "4"
ENV["OS"] = "Windows_NT"
ENV["PATH"] = ""
ENV["PATHEXT"] = ""
ENV["Platform"] = ""
ENV["PROCESSOR_ARCHITECTURE"] = "x86"
ENV["PROCESSOR_IDENTIFIER"] = "Intel64 Family6"
ENV["PROCESSOR_LEVEL"] = "6"
ENV["PROCESSOR_REVISION"] = "2a07"
ENV["SESSIONNAME"] = "JoiPlay"
ENV["SystemRoot"] = "UserData"
ENV["windir"] = "UserData"
ENV["USERDOMAIN"] = "JoiPlay"
ENV["USERNAME"] = "JoiPlay"
ENV["USERPROFILE"] = "UserData"
ENV["TEMP"] = "UserData/Temp"
ENV["AV_APPDATA"] = "UserData/AppData"

def _mkxp_set_default_font_family(f)
end

#Fix for some Pokemon Essentials games
begin
  class PokemonSystem
    attr_accessor :screensize
    def screensize
      @screensize = 1.0 if !@screensize
      return @screensize
    end
  end
rescue
end

def set_loop_points(a="",b="")
end

#Fix for Sometimes Always Monster
module Input
  module Controller
    class State
      def left_trigger_value
        return 0
      end
      def right_trigger_value
        return 0
      end
      def thumb_left_x
        return 0
      end
      def thumb_left_y
        return 0
      end
      def thumb_right_x
        return 0
      end
      def thumb_right_y
        return 0
      end
      def thumb_left_dir4
        return 0
      end
      def thumb_left_dir8
        return 0
      end
      def thumb_right_dir4
        return 0
      end
      def thumb_right_dir8
        return 0
      end
      def press?(button)
        return false
      end
      def trigger?(button)
        return false
      end
      def repeat?(button)
        return false
      end
      def pressed_buttons
        return []
      end
    end

    def self.states
      return [State.new]
    end
    def self.first_state
      return State.new
    end
  end
end

MKXP.puts("Preload script is executed.")