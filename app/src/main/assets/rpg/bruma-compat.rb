require 'zlib'

# This bundled Android mkxp-z build returns integer microseconds from uptime.
# Essentials and modern mkxp-z scripts expect seconds. Keep legacy System.delta
# untouched and fix the clock before game scripts alias uptime (e.g. turbo).
if System.uptime.is_a?(Integer)
  module System
    class << self
      unless method_defined?(:bruma_uptime_microseconds)
        alias_method :bruma_uptime_microseconds, :uptime
        def uptime
          bruma_uptime_microseconds / 1_000_000.0
        end
      end
    end
  end
end
