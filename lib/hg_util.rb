#!/usr/bin/env ruby
# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

DryRun = false unless defined?(DryRun)

# A few helpers to enable writing specs
@@systemHistory = Array.new unless defined?(@@systemHistory)
def getSystemHistory
  @@systemHistory  
end

def resetSystemHistory
  @@systemHistory = Array.new 
end

def system(cmd, mayFail=false)
  cmd2history =  "cd #{Dir.pwd} && #{cmd} # mayFail #{mayFail} #{DryRun ? 'DryRun' : ''}"
  puts cmd2history
  @@systemHistory << cmd2history
  if DryRun then return
  else res =Kernel.system(cmd)
  end
  if !res and !mayFail then
    puts "running #{cmd} #{mayFail} failed"
    exit
  end
end

class VersionFile

  BundlePattern = /(Bundle-Version: )(.+)()/
  HubJavaPattern = /(public static final String Version = ")([^"]+)(.*)/
  TexPattern = /(textbf\{Version )([^\}]+)(.*)/
  PropertyPattern = /(<property name="version" value=")([^"]+)(.*)/
  # \textbf{Version 2.1.5}
  attr_reader :version
  # loads the version file into memory
  # the pattern must define three groups:
  # * the sequence leading to the version
  # * the version
  # * the sequence after the version
  # This allows us to easily change the version
  # The pattern defaults to the version string in an eclipse manifest file
  def initialize(pathOrString, pattern=BundlePattern )
    if pathOrString.class == String && File.exists?(pathOrString)
    @inhalt = IO.read(pathOrString)
    @filePath = pathOrString
    else
    @inhalt = pathOrString
    @isFile = nil
    end
    @pattern = pattern
    getVersion
  end

  # returns the content of the file
  def get
    @inhalt
  end

  # replaces the version with the desired string
  def setVersion(newVersion)
    @inhalt.sub!(@pattern){$1+newVersion+$3}
    if @filePath && !DryRun
      f= File.open(@filePath, "w")
    f.write(@inhalt)
    f.close
    end
    puts "#{File.expand_path(@filePath)}: would update version #{@version} =>  #{newVersion}" if DryRun
    @version = getVersion
  end

  def getVersion
    @version = 'unknown'
    m = @pattern.match(@inhalt)
    @version = m[2] if m != nil
    @version
  end
end

def getVersionFromVersionFile(fileOrPath)
  VersionFile.new(fileOrPath).version
end
