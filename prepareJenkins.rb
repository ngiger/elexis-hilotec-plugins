#!/usr/bin/env ruby
# Niklaus Giger Copyright 2010-2011 niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Bootstrapping
#
# -b  --branch  Branch to use, default is Release_2.1.1
# -s  --skip-update  Skip pull --update
#
require 'fileutils'
$: << File.dirname( __FILE__)
$: << File.dirname( __FILE__)+"/lib" 
require 'optparse'

options = {}
options[:branch] = '2.1.7'
options[:clean] = true
options[:dryRun] = false
options[:skipUpdate] = false
options[:updateVersion] = false
OptionParser.new do |opts|
  opts.banner = "Usage: #{File.basename(__FILE__)} [options]"
  opts.on("-n", "--[no-]dry-run", "Don't run commands, just show them") do |v|
    options[:dryRun] = v
  end
  opts.on("-b", "--branch branchOrTag", "Branch or tag to use. Default is #{options[:branch]}") do |v|
    options[:branch] = v
  end
  opts.on("-c", "--clean", "add --clean to hg update. Default is #{options[:clean]}") do |v|
    options[:clean] = v
  end
  opts.on("-s", "--skip-update", "Skip pull --update") do |v|
    options[:skipUpdate] = v
  end
  opts.on("-u", "--update-version", "update version info in Hub.java & build.xml") do |v|
    options[:updateVersion] = v
  end
  opts.on("-h", "--help", "Show this help") do |v|
    puts opts
    exit
  end
end.parse!
DryRun = options[:dryRun]
BranchToBuild = options[:branch]
SkipUpdate = options[:skipUpdate] 
UpdateVersion = options[:updateVersion]
require 'elexis'
Elexis::readBuildCfg

JenkinsRoot     = File.expand_path(Dir.pwd)
puts "JenkinsRoot is #{JenkinsRoot} BranchToBuildis #{BranchToBuild}"
allRepos         = HgRepo::allRepos
BuildElexis     = "#{JenkinsRoot}/elexis-base/BuildElexis"
PlatformRuntime = "#{JenkinsRoot}/#{BuildCfg['EclipseVers']}"
[ File.expand_path("#{JenkinsRoot}/../downloads"),
  File.expand_path("#{Dir.pwd}/../../downloads"),
  File.expand_path("#{Dir.pwd}/../downloads"),
  File.expand_path("#{Dir.pwd}/downloads"),
  ].each {
 |dir|
    if File.directory?(dir)
      puts "Using #{dir}"
      Downloads=dir
      break
  end
}
Downloads = File.expand_path("#{JenkinsRoot}/downloads") if !defined?(Downloads)
def getEclipseVariants(from, where)
  FileUtils.makedirs(where, :noop => DryRun)
  FileUtils.chdir(where) if ! DryRun
  puts "Getting eclipse from #{from} into #{File.expand_path(where)}"
  ['linux-gtk-x86_64.tar.gz',
  'linux-gtk.tar.gz',
  'macosx-cocoa.tar.gz',
  'win32.zip'].each do
    |os_variant|
	fname = "eclipse-rcp-#{BuildCfg['EclipseVers']}-#{os_variant}"
	found = false
	full = "#{from}/#{fname}"
	if File.exists?(where+'/'+fname)
	  puts "Already present #{File.expand_path(where+'/'+fname)}"
	elsif from.index('http://')==0
	  puts "Must get from #{from}/#{fname}"
	  system("wget --quiet -c #{from}/#{fname}")
	else
	  copyOrLink(full, where) if !File.exists?(fname)
	end
  end
end

puts "JenkinsRoot is #{JenkinsRoot}" if $VERBOSE
puts "PlatformRuntime ist #{PlatformRuntime}" if $VERBOSE
allRepos.each{
  |name, subRepo|
  next if SkipUpdate
  subRepo.pull
  whichOne = subRepo.forceBranch == nil ? BranchToBuild : subRepo.forceBranch
  if !subRepo.tags.index(whichOne) and !subRepo.branches.index(whichOne) and subRepo.forceBranch == nil
      p subRepo.tags
      p subRepo.branches
      puts "No such tag or branch '#{whichOne}' in #{subRepo.baseDir} force '#{subRepo.forceBranch}'"
      exit 3
  else
      puts "found tag/branch #{whichOne}" if $VERBOSE
  end
  doClean = options[:clean] ? '--clean' : ''
  subRepo.update(BranchToBuild, doClean) # remove outstanding changes!!
}
Dir.chdir(allRepos['elexis-base'].baseDir)
Elexis::updateElexisVersion(BranchToBuild) if UpdateVersion

eclipse = "#{PlatformRuntime}/eclipse"
FileUtils.mkdir_p(PlatformRuntime) if !File.directory?(PlatformRuntime)
Dir.chdir(PlatformRuntime)
from=nil
platformRuntimeTar = nil
cmd = nil

require 'rbconfig'
include Config

case CONFIG['host_os']
   when /mingw|msys|mswin/i
      # Windows
      platformRuntimeTar = "#{Downloads}/eclipse-rcp-#{BuildCfg['EclipseVers']}-win32.zip"
#      cmd = "unzip #{platformRuntimeTar}".gsub('/','\\\\') if !File.exists?(eclipse+".exe")
   when /linux/i
     is64bit = true if /x86_64-linux/i.match(RUBY_PLATFORM)
     if /java/i.match(RUBY_PLATFORM)
       require 'java'
       is64bit = true if /amd64/i.match(java.lang.System.getProperty("os.arch"))
     end
     if is64bit
       platformRuntimeTar = "#{Downloads}/eclipse-rcp-#{BuildCfg['EclipseVers']}-linux-gtk-x86_64.tar.gz"
     else
       platformRuntimeTar = "#{Downloads}/eclipse-rcp-#{BuildCfg['EclipseVers']}-linux-gtk.tar.gz"
     end
     cmd = "tar -zxf #{platformRuntimeTar}" if !File.exists?(eclipse)
   when /darwin/i
    platformRuntimeTar = "#{Downloads}/eclipse-rcp-#{BuildCfg['EclipseVers']}-macosx-cocoa.tar.gz"
    cmd = "tar -zxf #{platformRuntimeTar}" if !File.exists?(eclipse+".exe")
  else
    puts "Unsupported RUBY_PLATFORM #{RUBY_PLATFORM} host_os #{CONFIG['host_os']}  (jruby??)"
    exit 2
end

if !File.exists?(platformRuntimeTar) then
	puts "File to extract #{platformRuntimeTar} missing"
	exit 3
end
if File.directory?(eclipse) then
	puts "Skipping upacking #{eclipse}"
else
	system(cmd)
end

unlessFile = eclipse+"/**/ch.qos.logback.core_*.jar"
pluginURL="http://download.eclipse.org/tools/orbit/downloads/drops/R20120526062928/repository/"
installIUs="ch.qos.logback.classic,ch.qos.logback.core,ch.qos.logback.slf4j,org.slf4j.api,org.slf4j.ext,org.slf4j.jul,org.slf4j.jcl,org.slf4j.log4j,org.apache.commons.logging,org.apache.commons.lang,org.apache.log4j"
unpackQosCmd = "#{eclipse}/eclipse -application org.eclipse.equinox.p2.director -noSplash -repository #{pluginURL} -installIUs #{installIUs}"
if Dir.glob(unlessFile).size == 0
  puts "File #{unlessFile} not found installing ch.qos.logback and other"
  system(unpackQosCmd)
else
  puts "File #{unlessFile} found. Skipping installation of ch.qos.logback"
  puts "# " + unpackQosCmd + "# skipped"
end

exit(0) if DryRun

Elexis::createLocalProperties(BranchToBuild, eclipse, JenkinsRoot)
