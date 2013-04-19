#!/usr/bin/env ruby
# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Helper class to manage hg repositories and corresponing release management tasks
#
require 'hg_util'
require 'hgcommitstat'
require 'hgrepo'

LogTemplate  = "--template '{branches},{rev},{node|short},\"{desc}\",{author|person},{date|isodate}\\n'"
module HgHelper
  Plugins= Hash.new
  AllMfPath = "*/META-INF/MANIFEST.MF"
  class Plugin
    attr_accessor :name, :oldVersion, :actualVersion
    def initialize(name, old='unknown', act ='unknown')
      @name = name
      @oldVersion = old
      @actualVersion = act
    end
  end
  private

  def HgHelper::readAllManifests(isOld)
    Dir.glob(AllMfPath).each {
    |mf|
      name = File.dirname(File.dirname(mf))
      version = getVersionFromVersionFile(mf)
      unless Plugins[name]
        info = Plugin.new(name)
        Plugins[name] = info
      end
      if isOld then
	puts "mf ist #{mf}"
        Plugins[name].oldVersion = version
      else
        Plugins[name].actualVersion = version
      end
    }
  end

  public

  def HgHelper::getPluginVersionAndHistory(root=Dir.pwd, branch=nil, startRevision=nil, lastRevision=nil, pivot=nil, ignoreRevisions=/999999999999/)
    repo = HgRepo.new(root)
    branch = repo.branch if branch == nil
    Dir.chdir(root)
    curRevision = repo.getRevisionAndBranch[0]
    branchSpec = lastRevision ? " -r #{lastRevision}" : " #{branch}"
    repo.checkout(branchSpec)
    ignoreRevisions = ignoreRevisions.to_s unless ignoreRevisions.class == Regexp
    pluginsMfActual = Dir.glob(AllMfPath)
    # if not specified get the startRevision
    if startRevision == nil then
      cmd = "|hg log -b #{branch} #LogTemplate}"
      f = open(cmd)
      inhalt = f.read.split("\n")
      startRevision = inhalt[-1].split(',')[2]
    end
    puts "getPluginVersionAndHistory of branch #{branch} from #{startRevision} upto #{lastRevision ? lastRevision : "youngest"} curRevision is #{curRevision} in #{root}"
    repo.checkout("-r #{startRevision}")
    HgHelper::readAllManifests(true)
    pluginsMfAtStart = Dir.glob(AllMfPath)
    allPlugins = pluginsMfActual + pluginsMfAtStart
    allPlugins.uniq!
    # checkout the branch given
    repo.checkout(branchSpec)
    HgHelper::readAllManifests(false)
    allPlugins.sort.each {
    |mf|
      puts
      full = File.expand_path(mf)
      plugin = File.basename(File.dirname(File.dirname(full)))
      alteVersion = Plugins[plugin].oldVersion
      aktuelleVersion = Plugins[plugin].actualVersion
      add = (alteVersion == 'unknown' or aktuelleVersion.eql?(alteVersion)) ? "unchanged" : " updated from #{alteVersion.inspect}"
      puts "plug-in #{plugin} actual version #{aktuelleVersion} " + add
      cmd = "|hg log -I #{plugin} -b #{branch} #LogTemplate}"
      f = open(cmd)
      inhalt = f.read.split("\n")
      inhalt.each{ |rev| puts "   #{rev}" if not ignoreRevisions.match(rev) } if inhalt
    }
    # go back to the original checkout
    repo.checkout("-r #{curRevision}")
  end

  def HgHelper::getStat(root, branch)
    Dir.chdir(root)
    ENV['LANGUAGE']='C'
    puts "Collecting statistic on branch #{branch} in #{File.expand_path(root)}"
    cmd = "|hg log --no-merges -b #{branch} --template '{branches},{rev},{node|short},{date|shortdate},{author|person},\\n'"
    f = open(cmd)
    inhalt = f.readlines().each{ |x|
      author   = x.split(',')[4]
      revision = x.split(',')[2]
      #     LANGUAGE=C hg diff --stat --git -c 4854
      # ch.rgw.utility/src/ch/rgw/tools/Money.java |  30 ++++++++++++++++++++++++++++++
      # 1 files changed, 30 insertions(+), 0 deletions(-)
      details = "|hg diff --stat --git -c #{revision}"
      f2 = open(details)

      inhalt = f2.read()
      m = /(\d+) files.* (\d+) insertions.* (\d+) deletions/.match(inhalt)
      if m
        if $VERBOSE
          puts "  analysed revision: #{revision}"
        else
          STDOUT.write("."); STDOUT.flush
        end
      HgCommitStat::addInfo(author, m[1], m[2], m[3])
      else
        puts "  skipped revision: #{revision}"
      end
    }
    puts
    HgCommitStat::showInfo
  end
end
