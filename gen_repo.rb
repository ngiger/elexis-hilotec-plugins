#!/usr/bin/env ruby
#
# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# == Synopsis
# small script to generate a master repository with subrepos
# for archie, elexis-base, elexis-addons
#
ScriptDir = File.expand_path(File.dirname( __FILE__))
$: << ScriptDir
$: << File.expand_path(File.dirname( __FILE__))+"/lib"
require 'elexis'
Elexis::readBuildCfg
require 'fileutils'
require 'optparse'
MyRepo=HgRepo.new(File.dirname(__FILE__))
options = {}
options[:branch]    = ''
options[:dryRun]    = false
options[:eclipse]   = ENV['GEN_REPO_ECLIPSE']
options[:libraries] = ENV['GEN_REPO_LIBRARIES']
OptionParser.new do |opts|
  opts.banner = "Usage: #{File.basename(__FILE__)} [options] [destdir=.] \n" +
      "Create an elexis source environment with the repositories elexis-base, elexis-addons and archie\n" +
      "  (plus all repositories specified in privateRepos.rb)\n" +
      "If specified, gets also all needed Eclipse images from\n" +
      "If specified, populate lib directory (conserving space)\n"+
      "  Available options are:\n\n"
  opts.on("-n", "--[no-]dry-run", "Don't run commands, just show them") do |v|
    options[:dryRun] = v
  end
  opts.on("-b", "--branch branch", "Branch to use. Default is #{BuildCfg['DefaultBranch']}") do |v|
    options[:branch] = v
  end
  opts.on("-c", "--clean", "Throw away all local changes") do |v|
    options[:clean] = '--clean' if v
  end
  opts.on("-e", "--eclipse URL_or_dir", "Where to get the various eclipse. Default from env GEN_REPO_ECLIPSE  is <#{options[:eclipse] }>") do |v|
    options[:eclipse] = v
  end
  opts.on("-l", "--libraries dir", "If defined, populate lib directory. Default from env GEN_REPO_LIBRARIES  is <#{options[:libraries] }>") do |v|
    options[:libraries] = v
  end
  opts.on("-h", "--help", "Show this help") do |v|
    puts opts
    exit
  end
end.parse!
DryRun=options[:dryRun] if DryRun != options[:dryRun]
if ARGV.length == 1
  Dest = File.expand_path(ARGV.shift)
else
  Dest = Dir.pwd
end
Eclipse = options[:eclipse]
Libraries = options[:libraries]
BranchName=options[:branch]
NeedsClean = options[:clean]
MainRepo = HgRepo.new(Dest)

def addHgRepos
  HgRepo::allRepos.each{
    |id, origin|
    aRepo = nil
    puts "addHgRepos: #{id} #{origin.url} as #{origin.class}" # if $VERBOSE
    if origin.class == String
      aRepo = MainRepo.addSubRepo(origin)
    elsif origin.class == Hash
      next if origin.size < 2
      dir = File.basename(origin[:URL])
      # only supported for mercurial
      aRepo = MainRepo.addSubRepo(origin[:URL], dir, false, origin[:branch])
    elsif origin.class == HgRepo
      aRepo = origin
    else
      puts "definition (class #{origin.class}) must be a String or a Hash"
      exit 3
    end
    aRepo.clone if !File.directory?(aRepo.baseDir)
    Dir.chdir(aRepo.baseDir) if !DryRun
    if aRepo.branches and aRepo.branches.index(BranchName)
      aRepo.pull # fetch
      aRepo.update(BranchName, NeedsClean)
    else
      puts "# #{aRepo.baseDir} from #{aRepo.url} already present" if $VERBOSE
    end
}
end

def getRubyScripts
  return if Dest == ScriptDir
  FileUtils.makedirs(Dest, :noop => DryRun)
  FileUtils.chdir(Dest) if ! DryRun 
 ["#{ScriptDir}/#{File.basename(__FILE__)}",
  "#{ScriptDir}/lib/hg_helper.rb",
  "#{ScriptDir}/lib/hgcommitstat.rb",
  "#{ScriptDir}/lib/hg_util.rb",
  "#{ScriptDir}/lib/elexis.rb",
  "#{ScriptDir}/lib/hgrepo.rb",
  "#{ScriptDir}/elexis_conf.rb",
  "#{ScriptDir}/prepareJenkins.rb"].each {
    |x| 
      FileUtils.cp(x, MainRepo.baseDir, :verbose => true, :preserve => true, :noop => DryRun)
      MainRepo.add(File.basename(x))
  }
end

def copyOrLink(source, dest)
  full = "#{dest}/#{File.basename(source)}"
  if File.exists?(full) then
    puts "File #{full} exists"
  elsif File.writable?(source) then
    FileUtils.ln_s(source, File.expand_path(dest), :verbose => true, :noop => DryRun)
  else
    FileUtils.cp(source, File.expand_path(dest), :verbose => true, :noop => DryRun)
  end 
end

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

MyRepo.update(BranchName, NeedsClean)
getRubyScripts
oneUp = File.expand_path(Dest)+'/../downloads'
if File.directory?(oneUp) and File.writable?(oneUp)
  getEclipseVariants(Eclipse, oneUp)
else
  getEclipseVariants(Eclipse, File.expand_path(Dest)+'/downloads')
end if Eclipse != nil 
elexisLib = File.expand_path(Dest)+'/lib'

FileUtils.makedirs(elexisLib, :verbose => true, :noop => DryRun)
if Libraries && File.directory?(Libraries)
  Dir.glob("#{Libraries}/*").each {
      |file|
	next if File.directory?(file)
	dest = "#{elexisLib}/#{File.basename(file)}"
	next if File.exists?(dest)
	copyOrLink(file, elexisLib)
    }
end
addHgRepos
