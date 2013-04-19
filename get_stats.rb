#!/usr/bin/ruby
# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
#
require 'fileutils'
require 'optparse'

$: << File.dirname( __FILE__)+"/lib"

options = {}
options[:dryRun] = false
options[:skipUpdate] = false
OptionParser.new do |opts|
  opts.banner = "Usage: #{File.basename(__FILE__)} [options] /path/to/repository
  Tells you for each plugin (in /path/to/repository), which commits where done
  and initial and final version of the plug-in
"  
  opts.on("-b", "--branch branchOrTag", "Branch or tag to use. Default is the active branch in the repository") do |v|
    options[:branch] = v
  end
  opts.on("-r", "--revision revision", "Compare with this revision") do |v|
    options[:revision] = v
  end
  opts.on("-h", "--help", "Show this help") do |v|
    puts opts
    exit
  end
end.parse!

require 'hg_helper'
require 'hgrepo'

root =  ARGV[0] ? ARGV[0] : Dir.pwd
myRepo = HgRepo.new(root)
BranchToUse = options[:branch] ?  options[:branch] : myRepo.branch

HgHelper::getStat(root, BranchToUse)
