#!/usr/bin/env ruby
# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# == Synopsis
# small script to create a tag in various subRepositories
#
# It must be run inside the master repository
#
$: << File.dirname( __FILE__)+"/lib" 
myRoot = File.dirname(File.expand_path( __FILE__))
myName = File.basename(myRoot)

require 'hgrepo'
require 'optparse'
require 'elexis'
Elexis::readBuildCfg
MyRepo = HgRepo.new(myRoot)

savedDir = File.expand_path(Dir.pwd)
allRepos = HgRepo::allRepos
allRepos.each { |name, r| puts "will work in #{r.baseDir}"} if $VERBOSE

options = {}
options[:branch] = BuildCfg['DefaultBranch']
options[:dryRun] = false
options[:tag] = nil
options[:force] = false
options[:comment] = nil
OptionParser.new do |opts|
  opts.banner = "Usage: #{File.basename(__FILE__)} [options] tagToMake"
  opts.on("-n", "--[no-]dry-run", "Don't run commands, just show them") do |v|
    options[:dryRun] = v
  end
  opts.on("-c", "--comment comment", "Comment to use for commit") do |v|
    options[:comment] = v
  end
  opts.on("-f", "--force", "Force the commit (a second time)") do |v|
    options[:force] = '--force' if v
  end
  opts.on("-h", "--help", "Show this help") do |v|
    puts opts
    exit
  end
end.parse!
DryRun=options[:dryRun] if DryRun != options[:dryRun]
options[:tag]=ARGV[0]
if !options[:tag]
  puts "A tag must be specified !. See #{__FILE__} -h for details"
  exit 3
end

RegExp = /[^\d\.a-z]/i
if RegExp.match(options[:tag])
  puts "illegal tag #{ options[:tag]}. A tag must consist of alphanumerical characters or '.'"
  exit 3
end
  
allRepos.each{ |name, subRepo|
  next if File.directory?('.svn')
  next if !File.directory?('.hg')
  Dir.chdir(subRepo.baseDir)
  if File.exists? 'ch.elexis/META-INF/MANIFEST.MF' then
    savedBranch = subRepo.branch
    Elexis::updateElexisVersion(options[:tag])
    subRepo.commit("Prepare for tag #{options[:tag]}",true)
  end
  subRepo.tag(options[:tag], "#{options[:comment]} based on #{subRepo.branch}", options[:force])
  if File.exists? 'ch.elexis/META-INF/MANIFEST.MF' then
    Elexis::resetElexisVersion
    subRepo.commit("Switch versions back to #{Elexis::oldVersion}",false)
  end
} 

Dir.chdir(myRoot)
if File.directory?('.hg')
  if MyRepo.tags.index(options[:tag]) and !options[:force]
    puts "Tag #{options[:tag]} exists already in #{MyRepo.baseDir}. Skipping commit & tag"
  else
    MyRepo.commit("Prepare for tag #{options[:tag]}",true) 
    puts "Will create tag for myself in #{MyRepo.baseDir}"
    comment = "Tag #{options[:tag]}: create tag for myself. Branch is #{MyRepo.branch}"
    comment = " forced " + comment if options[:force]
    MyRepo.tag(options[:tag], comment, options[:force])
  end
end