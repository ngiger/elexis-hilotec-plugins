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
OptionParser.new do |opts|
  opts.banner = "Usage: #{File.basename(__FILE__)} [options] /path/to/repository
  Zeigt pro Plug-In (in /path/to/repository), ob im aktuellen Branch irgendwelche Änderungen gemacht wurden.
  Wenn Ja, dann zeigt es auch die ursprüngliche und die aktuelle Version, sowie einer Liste der commits an
"
  opts.on("-b", "--branch branchOrTag", "Branch or tag to use. Default is the active branch in the repository") do |v|
    options[:branch] = v
  end
  opts.on("-s", "--start-revision revision", "Start with this revision") do |v|
    options[:start] = v
  end
  opts.on("-l", "--last-revision revision", "only up to this revision") do |v|
    options[:last] = v
  end
  opts.on("-h", "--help", "Show this help") do |v|
    puts opts
    exit
  end
end.parse!

root =  ARGV[0] ? File.expand_path(ARGV[0]) : File.expand_path(Dir.pwd)
require 'hg_helper'
myRepo = HgRepo.new(root)
branch = options[:branch] ?  options[:branch] : myRepo.branch
STDOUT.puts "branch #{branch} root #{root}"
HgHelper::getPluginVersionAndHistory(root, branch, options[:start] , options[:last],  /,(4779|4780),/) 
