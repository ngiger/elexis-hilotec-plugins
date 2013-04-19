#!/usr/bin/env ruby
# encoding: utf-8
# Copyright 2012 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
#-----------------------------------------------------------------------------
# Here we define somme common layout/rules to match the written and
# unwritten laws of the various Elexis developers. We create a directory
# deploy with
# deploy/elexis_plugins # where you find all our plugins
# deploy/dox # where you find the Elexis handbook and the documentation
#            # of more plug-ins
#
#-----------------------------------------------------------------------------
require 'buildr/scala'
require 'rubygems'
require 'buildr4osgi'
require 'buildr4osgi/eclipse/p2'
require 'antwrap'
require 'buildrdeb'
require 'fileutils'
require "buildr/bnd"
repositories.remote << "http://repo2.maven.org/maven2"
repositories.remote << "http://mvnrepository.com/maven2"
repositories.remote << "http://mvnrepository.com"
repositories.remote << "http://download.eclipse.org/rt/eclipselink/maven.repo"
repositories.remote << "http://archive.eclipse.org/rt/eclipselink/maven.repo/"
repositories.remote << Buildr::Bnd.remote_repository
repositories.release_to = 'file:///opt/elexis-release'
puts "Setup: added some repositories to repositories.remote"

module Elexis
  include Extension

  def Elexis::get_deploy_dir
    @@deploy
  end

  before_define do |project|
    if !project.parent # we are in the root project
      @@deploy = File.expand_path(File.join(project.path_to(:target), '..', 'deploy'))
    end
  end

  after_define do |project|
    # Special handling for the Elexis handbook
    if /dokumentation/.match(project.name)
      if !Wikitext::skipDoc
        docName = project._('elexis.pdf')
        tgtName = File.join(Elexis::get_deploy_dir, 'dox', 'Handbuch')
        task(tgtName => docName) do
          FileUtils.makedirs(tgtName)
          FileUtils.cp(docName, tgtName, :verbose => true, :preserve => true)
        end
        project.task(:package => tgtName )
      end
    elsif project.parent && !project.name.index('.site.feature') && !project.name.index(':p2')
      # Copy all plugin-jars into deploy/elexis_plugins
      jarName = project.package(:plugin).to_s
      tgtName = File.join(Elexis::get_deploy_dir, 'elexis_plugins', File.basename(jarName))
      task(tgtName => jarName) do
        FileUtils.makedirs(File.dirname(tgtName))
        FileUtils.cp(jarName, tgtName, :verbose => true, :preserve => true) if File.exists?(jarName)
      end
      project.task(:package => [jarName, tgtName])
    end
  end

  def Elexis::addPackIfProjectExists(project, installerXml, izPackName, izPackDescription, projectname)
    begin
      jarName = project.project(projectname).package(:plugin).to_s
      puts "addPackIfProjectExists #{projectname} #{izPackName} #{jarName}"
      task(project.package => jarName)
      installerXml.pack('name' => izPackName, 'required' => 'no', 'preselected'=>'no') {
        installerXml.file('src'=> jarName, 'targetdir' => File.join('$INSTALL_PATH','plugins'))
        installerXml.description(izPackDescription)
      }
   rescue => details
      puts "izpack rescue addPackIfProjectExists: Skipping #{izPackName} at #{Time.now}"
      p details
      p caller
      # probably project not defined
    end
  end

end

class Buildr::Project
  include Elexis
end
