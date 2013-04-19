#!/usr/bin/env ruby
# encoding: utf-8
# Copyright 2012 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Here add support for creating a p2site with all Elexis plugins
#
#-----------------------------------------------------------------------------
# Early init
#-----------------------------------------------------------------------------
require 'buildr4osgi'
require 'buildr4osgi/eclipse'
ENV['P2_EXE'] = ENV['OSGi'] if !ENV['P2_EXE']
puts "Setup: P2_EXE is #{ENV['P2_EXE']}"
errorMsg = "an Eclipse application must be found inside environment variable P2_EXE!"
if !ENV['P2_EXE']
  puts errorMsg
  exit 3
end
fName = File.join(ENV['P2_EXE'], 'eclipse')
if (!File.exists?(fName) && !File.exists?(fName + '.exe'))
  puts errorMsg
  exit 3
end

def addFeatureToSite(short)
  siteName  = short+'.site.feature'
  define siteName do
    puts "P2SiteExtension: addFeatureForSite #{project.id} -> #{short}" if $VERBOSE
    medXml = project(short.to_s)._("medelexis.xml")
    puts "P2SiteExtension: addFeatureForSite medXml #{medXml}" if $VERBOSE
    doc = Document.new(File.new(medXml))
    f = project.package(:feature)
    f.plugins <<  projects(short)
    # TODO: add also dependencies. Or does the plug-in handle it????
    f.label = "#{doc.root.attributes['name'] != nil ? doc.root.attributes['name'] : short}"
    f.provider = "The Elexis community"
    f.description = "#{doc.root.elements['service:description'].text}"
    f.changesURL = "#{doc.root.elements['service:docURL'].text if doc.root.elements['service:docURL']}"
    f.license = "Eclipse Public License Version 1.0"
    f.licenseURL = "http://eclipse.org/legal/epl-v10.html"
    f.update_sites << {:url => "http://www.elexis.ch/update", :name => "Elexis update site"}
    f.discovery_sites = [{:url => "http://www.elexis.ch/update2", :name => "Elexis discovery site"},
      {:url => "http://backup.elexis.ch//backup-update", :name => "Backup update site"}]
  end
  siteName
end


module P2SiteExtension
  include Extension
  @@allFeatures = []
  def P2SiteExtension::getFeatures
    @@allFeatures
    root = @@rootDir
    dirs = []
    ['*/*/.svn/all-wcprops','*/.svn/all-wcprops', '**/.git/config', '**/.hg/hgrc'].each { |vcsType|
	Dir.glob("#{vcsType}").each { |vcs| dirs <<  File.expand_path(File.join(root, File.dirname(File.dirname(vcs)))) }
      }
    medFiles = []
    (dirs + [root]).each { |x| medFiles += Dir.glob("#{x}/*/medelexis.xml") }
    features = []
    medFiles.each{
      |medXml|
	doc = Document.new(File.new(medXml))
	if /feature/i.match(doc.root.attributes['category']) and
	  !/invisible/i.match(doc.root.attributes['category']) and
	  Dir.glob(medXml.sub('medelexis.xml', 'src')).size > 0
	  features << doc.root.attributes['id']
	end
    }
    puts "Setup: P2SiteExtension found #{medFiles.size} medelexis.xml below #{(dirs + [root]).inspect} with #{features.size} features"
    puts "Setup !!! Warning!!!: no features found via medelexis.xml" if medFiles.size == 0
    features
  end

  before_define do |project|
    @@rootDir = Dir.pwd unless project.parent
    if project.parent and !ENV['P2site'].eql?('no')
      medXml = project._("medelexis.xml")
      if File.exists?(medXml)
	short = project.name.sub(project.parent.name+':','')
	doc = Document.new(File.new(medXml))
	if /feature/i.match(doc.root.attributes['category']) and !/invisible/i.match(doc.root.attributes['category'])
	  @@allFeatures << short
	end
      end
    end
  end

end

class Buildr::Project
  include P2SiteExtension
end
