#!/usr/bin/env ruby
datei='lowriter_acces.log'

marker = File.basename(__FILE__)
info ="starting with ARGV #{ARGV.inspect}"
system("logger #{marker}: open #{info}")
datei = ARGV[-1]
system("/usr/bin/lowriter '#{datei}'")
info = Dir.glob(File.join(File.dirname(datei), "*#{File.basename(datei)}*"))
system("logger #{marker}: done #{info}")


