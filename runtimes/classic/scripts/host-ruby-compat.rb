# Build-host compatibility only; not loaded by the Android runtime.
TRUE = true unless defined?(TRUE)
FALSE = false unless defined?(FALSE)
# Cache the host standard library before mkconfig changes RUBY_VERSION.
host_abi = RUBY_VERSION.split('.')[0,2].join('.') + '.0'
require '/usr/lib/ruby/' + host_abi + '/' + RUBY_PLATFORM + '/rbconfig.rb'
require 'fileutils'
