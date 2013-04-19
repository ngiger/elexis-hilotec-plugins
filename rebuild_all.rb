#!/usr/bin/env ruby
# Small ruby script to build elexis with buildr and jruby
# We assume a working eclipse installation pointed by the environment variable P2_EXE
# You may pass additional target using the environment variable BUILDR_ADDED_TARGETS

require 'fileutils'
require 'rbconfig'

# Our default value for ngiger.dyndns.org/jenkins and medelexis.ch/jenkins
# Both are running a GNU/Debian linux squeeze 64-bit OS
ENV['P2_EXE']     ||= '/srv/jenkins/userContent/indigo'
if Dir.glob("#{ENV['P2_EXE']}/plugins/org.eclipse.equinox.launcher_*.jar").size == 0
  puts "Environment variable P2_EXE (actual value #{ENV['P2_EXE']}) must point to an eclipse installation"
  puts "    where we can find a plugins/org.eclipse.equinox.launcher_*.jar"
  exit 2
end

include Config

def os_system
	case CONFIG['host_os']
	   when /mingw|msys|mswin/i
		  return 'windows', false
	   when /linux/i
		 is64bit = true if /x86_64-linux/i.match(RUBY_PLATFORM)
		 if /java/i.match(RUBY_PLATFORM)
		   require 'java'
		   is64bit = true if /amd64/i.match(java.lang.System.getProperty("os.arch"))
		   return 'linux', is64bit
		 end
	   when /darwin/i
		return 'macosx', true
	  else
		puts "Unsupported RUBY_PLATFORM #{RUBY_PLATFORM} host_os #{CONFIG['host_os']}  (jruby??)"
		exit 2
	end
end

def runOneCommand(cmd)
  @@step ||= 0 
  @@step += 1
  logfile = "step_#{@@step}.log"
  FileUtils.rm(logfile) if File.exists?(logfile)
  startTime = Time.now
  log = File.open(logfile, 'w')
  cmd = cmd.gsub('/','\\') if cmd and os_system[0] == 'windows'
  log.puts "executing '#{cmd}'"
  f = open("| #{cmd} 2>&1")
  log.sync = true 
  while out = f.gets
    puts out
    log.puts(out)
  end
  f.close
  res =  $?.success?
  endTime = Time.now
  puts msg = "Step #{@@step}: took #{sprintf('%3s', (endTime-startTime).round.to_s)} seconds to execute '#{cmd}'. #{res ? 'okay' : 'failed'}. Finished at #{Time.now}"
  log.puts msg
  log.close
  sleep 0.5
  exit 2 if !res
end

[
File.join('elexis-addons', 'ch.ngiger.elexis.opensource', 'rsc', '*.html'),
'timestamp',
File.join('target'),
Dir.glob(File.join('*','target')),
Dir.glob(File.join('el*','*','target')),
Dir.glob(File.join('el*','*','bin')),
Dir.glob('step_*.log'),
'p2',
'deploy',
'reports',
 ].each{ |aDir| FileUtils.rm_rf(aDir, :verbose=> true) }

jruby ||= ENV['jruby']
jruby ||= 'jruby'
prefix = "rvm #{jruby} do"
require 'rbconfig'
prefix= 'jruby -S' if /mingw|bccwin|wince|cygwin|mswin32/i.match(RbConfig::CONFIG['host_os'])

globalStartTime = Time.now
# Here are all commands to rebuild elexis. See the comments in
# https://github.com/zdavatz/elexis/src/buildr_howto.textile
commands = [
  "lib/init_buildr4osgi.rb",
  "#{prefix} lib/gen_buildfile.rb", # Adapts the buildfile depending on the installed repositories
  "#{prefix} buildr delta OSGi",  # Needed if the desktop.dev.target or DELTA-Zip changes
  "#{prefix} buildr osgi:clean:dependencies osgi:resolve:dependencies osgi:install:dependencies", # If plugins have different dependencies
  #"cp -p dependencies.yml dependencies.yml.old",
  # e.g export BUILDR_ADDED_TARGETS="elexis:additions elexis:BuildMedelexis:izpack"
  "#{prefix} buildr test=no clean package #{ENV['BUILDR_ADDED_TARGETS']} ", # Clean, then build and package all packages (*.jar)
  # "#{prefix} buildr test integration", # run all junit and integration test, not active at the moment
 ].each{ |cmd| runOneCommand(cmd) }
puts msg = "All #{commands.size} steps successfull: took #{sprintf('%3s', (Time.now-globalStartTime).round.to_s)} seconds. Finished at #{Time.now}"
