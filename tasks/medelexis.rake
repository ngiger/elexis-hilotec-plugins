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
# Create medelexis additions (files for the configurator) if the environment
# variable genMedelexis is set
# Used for the new buildr.apache.org based build system
#-----------------------------------------------------------------------------
# a package has way too many different versions when packed as a Medelexis addition
# we use here the ch.elexis.core plug-in as packaged for 2.1.7.rc2
# 1) in META-INF/MANIFEST.MF   Bundle-Version: 0.0.2
# 2) in the medelexis.xml version="2.0.0"
# 3) the generated addition you find ch.elexis.core_0.0.2.20121004.zip
# 3a) which contains a medelexis.xml with version='0.0.2.20121004'
# 3b)                          and nucleusVersion='2.1.7.20121004'
# 4) inside this zip file you   find again ch.elexis.core-0.0.2.20121004.zip
#    where you find an unchanged version of the medelexis.xml of the source code
# 5) 
puts "8881: #{__FILE__} #{__LINE__}"

require 'zip/zip'

module Medelexis
  include Extension
  @@taskName = 'additions'
  @@qualifier = Time.now.strftime("%Y%m%d")

  before_define do |project|
    if !project.parent
      if ENV['genMedelexis']
        info "Setup: genMedelexis defined #{@@taskName} for #{project} using #{@@qualifier}"
        @@genMedelexis = true
        FileUtils.makedirs(Elexis::get_deploy_dir)
        @@missing = File.open(File.join(Elexis::get_deploy_dir, 'missing_medelexis.lst'), 'w+')
        @@rootProject = project
#        desc "Create Medelexis additions"
#        Project.local_task(@@taskName)
      else
        puts "Setup: genMedelexis not in environment"
        @@genMedelexis = false
      end
    end
  end

  after_define do |project|
    if @@genMedelexis && project.parent && !project.name.index('.site.feature') && !project.name.index(':p2')
      project.name.split(':')[-1]
      project.extend Medelexis
      xmlFile =  project._('medelexis.xml')
      path = File.join(project._, '..', '*medelexis*', project.name.split(':')[-1], 'medelexis.xml')
      xmlFile2 = nil
      xmlFile2 = Dir.glob(path)[0] if Dir.glob(path)
      if !File.exists?(xmlFile) and !xmlFile2
        @@missing.puts "Setup: Not creating an Medelexis addition for #{project.name}"
        trace "      neither #{xmlFile} nor #{path} could be found"
      else
        filename = "#{project.name.split(':')[-1]}_#{project.version}.zip"
        doc = Document.new(File.new(xmlFile))
        doc.root.attributes['version']        = project.version
        doc.root.attributes['nucleusVersion'] = @@rootProject.version
        doc.root.attributes['filename']       = filename
        zipName = File.join(Elexis::get_deploy_dir, "additions", filename)
        medInZipName = File.join(project.path_to(:target), 'medelexis.xml_in_zip')
        task(medInZipName) do
          medelexisInZip = File.open(medInZipName, 'w+')
          doc.write( medelexisInZip, 0 )
          medelexisInZip.close
        end
        jarName = project.package(:plugin).to_s
        zipName2 = File.join(Elexis::get_deploy_dir, 'elexis_plugins', File.basename(jarName))
        task(zipName => [medInZipName, zipName2]) do
          FileUtils.makedirs(File.dirname(zipName))
          puts "888 #{__LINE__}: tap for #{zipName}. #{zipName2} #{File.exists?(zipName2)}"
          if File.exists?(jarName)
            classFiles = jarName.entries.find_all{|item| /.class$/i.match(item) }
            puts "888 #{__LINE__}: tap for #{zipName}. classFiles #{classFiles.size}"
            if classFiles.size > 0
              project.zip(zipName).tap do |task|
                puts "888 #{__LINE__}: tap merge #{zipName}. #{zipName2}"
                task.merge(zipName2)
              end
            else
              project.zip(zipName).tap do |task|
                puts "888 #{__LINE__}: tap for #{zipName} include#{zipName2} #{File.exists?(jarName)}"
                task.include(zipName2, :as => filename)
              end
            end
          end
          task.include(medInZipName, :as => 'medelexis.xml')
          task.merge(zipName2)
        end

        puts "888 #{__LINE__}: Creating an Medelexis addition #{filename} 2 #{zipName2} zipName #{zipName}"
        # project.zip(zipName2).include(jarName, :as => filename)
        # project.zip(zipName).merge(zipName2)
if false
        project.zip(zipName).tap do |task|
          puts "888 #{__LINE__}: tap for #{zipName}. #{jarName} #{File.exists?(jarName)}"
          task.include(jarName, :as => filename)
        end

        project.zip(zipName).tap do |task|
          puts "888 #{__LINE__}: tap for #{zipName}. #{zipName2} #{File.exists?(zipName2)}"
          if File.exists?(jarName)
            classFiles = jarName.entries.find_all{|item| /.class$/i.match(item) }
            puts "888 #{__LINE__}: tap for #{zipName}. classFiles #{classFiles.size}"
          end
          task.merge(zipName2)
        end

        project.package(zipName => [medInZipName, jarName]) do
          puts "888 #{__LINE__}: task doing for #{zipName}. #{jarName} #{File.exists?(jarName)}"
          classNames = project.zip(jarName).entries.find_all{ |x| /\.class$/i.match(x)}
          puts "888 #{__LINE__}: task classNames.size #{classNames.size}"
          project.zip(zipName).include(jarName, :as => filename)
        end if false
end
#        @@rootProject.task(@@taskName => zipName)
#        @@rootProject.task(:package => zipName)
      end
    end
  end
end

class Buildr::Project
  include Medelexis
end
