# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Here we find a few helper functions to simplify builing Elexis
# from scratch, e.g. the Jenkins Continuos Integration
#
require 'hgrepo'

BuildCfg = Hash.new
module Elexis
  VersionFiles =     
  { 'ch.elexis/src/ch/elexis/Hub.java' => VersionFile::HubJavaPattern,
    'ch.elexis/META-INF/MANIFEST.MF' =>  VersionFile::BundlePattern,
    'dokumentation/elexis.tex' =>  VersionFile::TexPattern,
    'BuildElexis/build.xml' =>  VersionFile::PropertyPattern,
  }
  @@oldVersion = nil
  def Elexis::oldVersion
    @@oldVersion
  end

  def Elexis::readBuildCfg
    names = [File.join(Dir.pwd, 'elexis_conf_override.rb'),
              File.join(Dir.pwd, 'elexis_conf.rb'),
             ]
    names.each {
      |name|
      if File.exists?(name)
        require name
        break
      end
    }
  end

  def Elexis::patchVersionFile(name, matchPattern, newVersion)
    unless File.exists?(name) then
      puts "File #{name} neither found nor patched"
    return
    end
    v = VersionFile.new(name, matchPattern).setVersion(newVersion)
  end

  def Elexis::updateElexisVersion(newVersion)
    VersionFiles.each {
    |file, pattern|
      old = getVersionFromVersionFile(file)
      if @@oldVersion == nil && old != 'unknown'
        @@oldVersion = old
        puts "oldVersion ist #{@@oldVersion} aus #{file}"
      end
      patchVersionFile(file, pattern, newVersion)
    }
  end

  def Elexis::resetElexisVersion()
    if @@oldVersion == 'unknown' then
      puts "OldVersion not known, cannot continue"
      return nil
    end
    VersionFiles.each {
    |file, pattern|
      patchVersionFile(file, pattern, @@oldVersion)
    }
  end
    
  # Creates a local.properties as recquired by the ant build.xml of Elexis
  # Parameters are:
  # * branchOrTag: The name of the branch or tag
  # * root  : Jenkins-Root, where the different repositories lay
  # * path  : Name of file to generate.
  # * skipPlugins: Names of plug-Ins to ingore. The following are automatically excluded
  # ** All plugins mentioned in root/skipPlugins.add
  # ** all plugins with a name terminating in _test
  # ** all plugins with a name terminating in test.feature
  # ** all Plug-Ins that do not have a valid medelexis.xml
  # * skipPlugins defaults to BuildCfg['SkipPlugins']
  #
  def Elexis::createLocalProperties(branchOrTag, eclipse, root, skipPlugins = BuildCfg['SkipPlugins'])
    addName = "#{root}/skipPlugins.add"
    testPlugins = []
    Dir.glob("#{root}/*/*").each{
    |x|
      next unless File.directory?(x)
      next unless /_test|test\.feature/i.match(x)
      testPlugins << File.basename(x) if false # handled correctly by medelexis-packager-1.0.0.jar
    }

    repositories = "#{root}"
    repositories = "#{root}/repositories" if File.directory?("#{root}/repositories")

    skipPlugins += ','+testPlugins.join(',') if testPlugins.size > 0
    skipPlugins += IO.readlines(addName)[0] if File.exists?(addName)
    uniq = skipPlugins.split(',').uniq.join(',') if skipPlugins.size > 0
    inhalt = ["base=#{root}" ]
    inhalt << "skipPlugins="+uniq if skipPlugins.size > 0
    inhalt << "hg=hg"
    inhalt << "version=#{branchOrTag}"
    inhalt << "repositories=#{repositories}"
    inhalt << "rsc=#{repositories}/elexis-base/BuildElexis/rsc"
    inhalt << "platform-runtime=#{eclipse}"
    inhalt << "output=#{root}/deploy"
    inhalt << '# unplugged=true'
    inhalt << 'texify=texi2pdf'
    inhalt << 'texifyArgs=--silent'
    Dir.glob("#{root}/**/Build*").each{
    |buildDir|
      next if buildDir.index('docx')
      propFile = "#{buildDir}/local.properties"
      next unless File.directory?(File.dirname(propFile))
      properties = File.open(propFile, "w+")
      properties.puts inhalt.join("\n")
      if buildDir.index('BuildMedelexis') then
        properties.puts("medelexis=#{File.dirname(buildDir)}")
      end
    }
  end
end
