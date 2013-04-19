#!/usr/bin/env ruby
# encoding: utf-8
# Copyright 2012 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
#

IZPACK = 'org.codehaus.izpack:izpack-standalone-compiler:jar:4.3.5'

require 'buildrizpack'

module IzPack
  include Extension

  @@macosx_x86_64  =  { 'osgi.os' => 'macosx', 'osgi.ws' => 'cocoa', 'osgi.arch' => 'x86_64',
                      'executable' => '$INSTALL_PATH/elexis.app/Contents/MacOS/starter-mac',
                      'condition' => 'izpack.macinstall+is64bit',
                      'description' => 'MacOSX (64-bit) abhängige Dateien'}
  @@linux_x86_64   =  { 'osgi.os' => 'linux', 'osgi.ws' => 'gtk',    'osgi.arch' => 'x86_64',
                      'executable' => '$INSTALL_PATH/elexis',
                      'condition' => 'izpack.linuxinstall+is64bit',
                      'description' => 'Linux (64-bit) abhängige Dateien'}
  @@linux_x86      =  { 'osgi.os' => 'linux', 'osgi.ws' => 'gtk',    'osgi.arch' => 'x86',
                      'executable' => '$INSTALL_PATH/elexis',
                      'condition' => 'izpack.linuxinstall+is32bit',
                      'description' => 'Linux (32-bit) abhängige Dateien'}
  @@windows_x86_64 =  { 'osgi.os' => 'win32', 'osgi.ws' => 'win32',  'osgi.arch' => 'x86_64',
                      'executable' => '$INSTALL_PATH/elexis.exe',
                      'condition' => 'izpack.windowsinstall+is64bit',
                      'description' => 'Windows (64-bit) abhängige Dateien'}
  @@windows_x86    =  { 'osgi.os' => 'win32', 'osgi.ws' => 'win32',  'osgi.arch' => 'x86',
                      'executable' => '$INSTALL_PATH/elexis.exe',
                      'condition' => 'izpack.windowsinstall+is32bit',
                      'description' => 'Windows (32-bit) abhängige Dateien'}
  @@platforms = [ @@macosx_x86_64, @@linux_x86_64, @@linux_x86, @@windows_x86_64, @@windows_x86 ]

  def IzPack::getPlatforms
    return @@platforms
  end

  def IzPack::getDefaultPlatform()
    values = { 'osgi.os' => RbConfig::CONFIG['target_os'].downcase, # was java.lang.System.get_property('os.name').downcase,
                  'osgi.ws' => 'cocoa',
                  'osgi.arch' =>  RbConfig::CONFIG['target_cpu'], } # was java.lang.System.get_property('os.arch')}
    case values['osgi.os']
      when /linux/i then
        values['osgi.ws'] = 'gtk'
      when /win/i then
        values['osgi.ws'] = 'win32'
        values['osgi.os'] = 'win32'
    end
    values['osgi.arch'] = 'x86_64' if /amd64/.match(values['osgi.arch'])
    # trace "getDefaultPlatform returns #{values.inspect}"
    values
  end

  def IzPack::platform2path(platformHash, separator=File::SEPARATOR)
    res = "#{platformHash['osgi.os']}#{separator}#{platformHash['osgi.ws']}#{separator}#{platformHash['osgi.arch']}"
  end

  def IzPack::addPackIfProject(project, osJars, name, description, projectNames)
    begin
      pack = BuildrIzPack::Pack.new(name, description)
      # TODO: Pack.new add third parameter { 'preselected' => false}
      projectNames.each{ |name|
                  jar = project(name).package(:plugin).to_s
                  pack.addFile(jar, "$INSTALL_PATH/plugins/#{File.basename(jar)}")
                  osJars << jar
                }
      return pack
    rescue => details
      puts "Setup: addPackIfProject skips project #{name}, which is probably not in your workspace"
#      p details
#      p caller
      # probably project not defined
      return nil
    end
  end

  # Find an artifact given by its name
  #   return jar if founc in the array of jars given (e.g. jar produced by project)
  #   return jar found as artifact (e.g. org.eclipse.ui)
  #   return jar found in OSGI (e.g. com.ibm.icu)
  # else raise an error
  #
  def IzPack::getArtefactOrOsgiJar(project, jarname, defPlatform, jars = nil)
    if jars
      indexInJars = jars.index{|x|  x.class != String and x.id.eql?(jarname) }
      if indexInJars
        trace "getArtefactOrOsgiJar found #{jarname} in jars #{jars[indexInJars].inspect}"
        return jars[indexInJars]
      end
    end

    begin
      otherArtifact = artifact(jarname)
      unless  EclipseExtension::jarMatchesPlatformFilter(otherArtifact, defPlatform)
        trace "getArtefactOrOsgiJar skipping 1 #{otherArtifact.to_s} #{defPlatform.inspect} for #{jarname}"
        return nil
      end
      trace "getArtefactOrOsgiJar found otherArtifact #{otherArtifact.to_s}  for #{jarname}"
      return otherArtifact
    rescue
    end

    fileName = File.expand_path(File.join(ENV['OSGi'], 'plugins', "#{jarname}_*"))
    files = Dir.glob(fileName)
    files.each{ |aFile|
      if EclipseExtension::jarMatchesPlatformFilter(aFile, defPlatform)
        trace "getArtefactOrOsgiJar found file #{aFile.inspect} for #{jarname}"
        return aFile
      end
    }
    trace "getArtefactOrOsgiJar:Check your buildfile. Could not find dependency #{jarname} specified in #{project.name}!"
  end

  # MacOSX need some special treatement!!
  def IzPack::handleMacApp(project, destRoot, launcherName, initArgs)
    macApp    = File.join(destRoot, 'Eclipse.app')
    elexisApp = File.join(destRoot, launcherName +'.app')
    info "Macos #{macApp} -> #{elexisApp}"
    if File.exists?(macApp) then
      FileUtils.mv(macApp, elexisApp, :verbose => Buildr.application.options.trace)
      Dir.glob(File.join(elexisApp, '**/*')).each {
        |x|
            dest = x.sub(/launcher/i,  launcherName)
            dest.sub!(   /eclipse/i,  launcherName)
            trace "macos found #{x} => #{dest} #{x.eql?(dest)}"
            FileUtils.mv(x, dest, :verbose => Buildr.application.options.trace) unless x.eql?(dest)
            }
    end
    infoPlist = File.join(elexisApp, 'Contents', 'Info.plist')
    inhalt = IO.readlines(infoPlist)
    toReplace = { '<string>eclipse</string>' => '<string>'+ launcherName+'</string>',
                  '<string>Eclipse.icns</string>' => '<string></string>',
                  '<string>Eclipse</string>' => '<string>'+ launcherName+'</string>',
                  }
    inhalt.each { |x| toReplace.each { |from, to| x.sub!(from, to) } }
    File.open(infoPlist, 'w') {|f| f.write(inhalt) }
    File.open(File.join(elexisApp, 'Contents', 'MacOS', launcherName+'.ini'), 'w') {|f| f.write(initArgs) }
  end

  # Install the launcher exe, configuration, ini-files etc for the desired platform
  #
  def IzPack::installLauncherAndConfiguration(project, destRoot, product, tgtPlatform, configIni=nil)
    tPlugins = File.join(destRoot, 'plugins')
    tConf    = File.join(destRoot, 'configuration')
    unless product['configIni'].eql?('default')
      raise "Don't know how to handle non default configIni section in product definition"
    end

    [tPlugins,tConf].each{ |d| FileUtils.makedirs(d) }
    Buildr::write(File.join(destRoot, '.eclipseproduct'),  %(#Eclipse Product File
# created by buildr.apache.org at #{Time.now}"
version=#{product['version'].sub('qualifier', Buildr::Eclipse.qualifier)}
name=#{product['uid']}
id=#{product['id']}
))
    # copy common launcher jar
    Dir.glob(File.join(ENV['OSGi'], 'plugins', "org.eclipse.equinox.launcher_*.jar")).each {
      |jarFile|
        next if File.basename(jarFile).index('.source_')
        next unless EclipseExtension::jarMatchesPlatformFilter(jarFile, tgtPlatform)
        subDir = File.join(tPlugins, File.basename(jarFile, '.jar'))
        FileUtils.cp(jarFile, tPlugins, :verbose => Buildr.application.options.trace, :preserve => true)
    }
    # unpack correct os specific launcher jar
    Dir.glob(File.join(ENV['OSGi'], 'plugins', "org.eclipse.equinox.launcher.*.jar")).each {
      |jarFile|
        next if File.basename(jarFile).index('.source_')
        next unless EclipseExtension::jarMatchesPlatformFilter(jarFile, tgtPlatform)
        subDir = File.join(tPlugins, File.basename(jarFile, '.jar'))
        unzipper =  Buildr::Unzip.new(subDir=>jarFile)
        unzipper.extract
    }
    # Copy equinox executable
    execJar = Dir.glob(File.join(ENV['OSGi'], 'features', 'org.eclipse.equinox.executable_*jar'))[0]
    # Unzip common stuff (license)
    unzipper =  Buildr::Unzip.new(tPlugins =>execJar)
    unzipper.from_path(".").include('*')
    unzipper.extract

    # Unzip graphical toolkit specific stuff
    unzipper =  Buildr::Unzip.new(destRoot =>execJar)
    unzipper.from_path("#{tgtPlatform['osgi.ws']}_root").include('**')
    unzipper.extract

    # Unzip os specific stuff
    unzipper =  Buildr::Unzip.new(destRoot =>execJar)
    unzipper.from_path("bin/#{IzPack::platform2path(tgtPlatform)}").include('*')
    unzipper.extract

    # Here Eclipse uses ws/os/arch instead of ow/ws/arch in other places (.e.g, destination of install!!)
    pathName = File.join(ENV['DELTA_DEST'], 'features', 'org.eclipse.equinox.executable_*', '*', # we we * as it can be bin or contributed!!
                          tgtPlatform['osgi.ws'],  tgtPlatform['osgi.os'], tgtPlatform['osgi.arch'], '**')
    Dir.glob(pathName).each{
      |x|
        next if /eclipsec/i .match(File.basename(x))
        if File.basename(x).eql?('launcher') || File.basename(x).eql?('launcher.exe')
          tgt = File.join(destRoot, File.basename(x))
          tgt.gsub!('launcher', product['launcher'])
          FileUtils.cp_r(x, tgt, :verbose => Buildr.application.options.trace, :preserve => true)
          FileUtils.chmod(0755, tgt)
        else
          FileUtils.cp_r(x, destRoot, :verbose => Buildr.application.options.trace, :preserve => true)
        end
    }
-      configIni = %(#Product Runtime Configuration File
# created by buildr.apache.org at #{Time.now}"
eclipse.application=#{product['application']}
osgi.bundles.defaultStartLevel=4
eclipse.product=#{product['id']}
osgi.splashPath=platform:/base/plugins/#{product['splash']}
osgi.bundles=#{product['configurations'].sort.join(',')}
) if configIni == nil

    Buildr::write(File.join(tConf, 'config.ini'), configIni)

    # Add config argument
    initArgs  = ''
    initArgs << product['programArgs'] if product['programArgs']
    initArgs << ' -vmargs ' + product['vmArgs'] if product['vmArgs']
    initArgs << ' ' + product['vmArgsMac']   if (tgtPlatform['osgi.os'] && /macosx/.match(tgtPlatform['osgi.os']) != nil && product['vmArgsMac'])
    initArgs = initArgs.split(' ').join("\n")
    initArgs << "\n"
    if /macosx/.match(tgtPlatform['osgi.os'])
      IzPack::handleMacApp(project, destRoot,  product['launcher'], initArgs)
    else
      Buildr::write(File.join(destRoot, product['launcher']+'.ini'), initArgs)
    end
  end

  def IzPack::checkAbsent(project, xml, what)
    project.check project.package, "installer.xml: #{xml} should not contain #{what}" do
      File.should exist(xml)
      content = IO.readlines(xml).join('')
      content.index(what).should be_nil
    end
  end

  def IzPack::checkOne(project, xml, what)
    project.check project.package, "installer.xml: #{xml} should contain #{what}" do
      File.should exist(xml)
      content = IO.readlines(xml).join('')
      content.index(what).should_not be_nil
    end
  end

end


class  BuildrIzPack::Pack

  def addEclipseJar(jarname)
    @@run ||= 0
    @@run += 1
    if !File.exists?(jarname)
      info "addEclipseJar #{jarname} does not exists"
      return
    end
    if Buildr::Eclipse.mustUnpackJar(jarname) then
      trace "addEclipseJar mustUnpackJar #{jarname}"
      baseDir = File.join(Dir.tmpdir, 'buildr', @@run.to_s)
      # tgtDir =  File.join(baseDir, File.basename(jarname, '.jar'))
      tgtDir = baseDir
      FileUtils.rm_rf(tgtDir, :verbose => Buildr.application.options.trace) if File.exists?(tgtDir)  # be sure to remove old things
      Buildr::Eclipse.installPlugin(jarname.to_s, tgtDir)
      files = (Dir.glob(File.join(tgtDir, '**','*')) + Dir.glob(File.join(tgtDir, '**','.*'))).sort.uniq
      files.each{ |x|
                  next if File.directory?(x)
                  path = x.sub(baseDir, File.join('$INSTALL_PATH','plugins'))
                  addFile(x, path)
                }
      return
    end

    base = File.basename(jarname)
    if !Buildr::Eclipse.adaptName(base).eql?(base)
      trace "addEclipseJar not adaptName #{jarname}"
      addFile(jarname, File.join(@defaultPath, Buildr::Eclipse.adaptName(base)))
    else
      trace "addEclipseJar adaptName#{jarname} ist #{Buildr::Eclipse.adaptName(base)}"
      addFile(jarname)
    end
  end

end

def genIzPack(dest, instXml, jars, baseDir, properties=nil)
  raise "File #{instXml} must exist " if !File.exists?(instXml)
  Java.load # needed to load class path for apache logger
  # instXml = 'elexis-addons/ch.ngiger.elexis.opensource/installer.xml'
  # doc = Document.new File.new(instXml)
  # doc.elements['installation/info/appname'].to_a
  #  doc.elements['installation/packs/pack'].attributes['name'] return DemoDB
  # doc.elements['installation/packs/pack/fileset/include'].attributes['name']
  # doc.elements["installation/packs/pack[@name='DemoDB']"]
  # puts root.elements["section/item[@stock='44']"].attributes["upc"]
  desc "Generate Elexis installer"
  task 'izpack' => dest do
    puts "genIzPack #{dest}"
    artifact(IZPACK).invoke
    FileUtils.makedirs(baseDir)
    FileUtils.makedirs(File.join(baseDir, 'plugins2'))
    FileUtils.makedirs(File.join(baseDir, 'plugins'))
    FileUtils.makedirs(File.join(baseDir, 'rsc'))
    jars.uniq.each{ |jar|
                jarDest =  File.join(baseDir, 'plugins', File.basename(jar.to_s))
                if not jar.class == OSGi::BundleTask # it's an artifact
		  factName = artifact(jar).to_s
		  destBase = File.basename(factName).sub(/-(\d)/,'_\1')
		  destName = File.join(baseDir, 'plugins2', destBase)
                  if  !EclipseExtension::getPlatformFilter(factName) && !FileUtils.uptodate?(destName, factName)
			FileUtils.cp(factName, destName, :verbose => false, :preserve=>true)
		  end
		else
		  destBase = File.basename(jar.to_s).sub(/-(\d)/,'_\1')
		  FileUtils.cp(jar.to_s, File.join(baseDir, 'plugins', destBase), :verbose => false, :preserve=>true)
		end
	      }
    Buildr.ant('izpack-ant') do |x|
      msg = "genIzPack: Generating izpack aus #{instXml} #{File.exists?(instXml)} dest ist #{dest}"
      info msg
      x.property(:name => "version", :value => '2.2.jpa') 
      x.property(:name => "jars",    :value => jars.join(',')) 
      x.property(:name => "osgi",    :value => ENV['OSGi']) 
      x.property(:name => "target",     :value => project._('target'))
#      '-DTRACE=TRUE'
      x.property(:name => 'TRACE', :value => 'TRUE')
      if properties
	properties.each{ |name, value|
			  puts "Need added property #{name} with value #{value}"
			x.property(:name => name, :value => value) 
		      }
      end
      x.echo(:message =>msg)
      x.taskdef :name=>'izpack', 
	:classname=>'com.izforge.izpack.ant.IzPackTask', 
	:classpath=> artifact(IZPACK)
      x.izpack :input=> instXml,
		:output => dest,
		:basedir =>baseDir,
		:installerType=>'standard',
		:inheritAll=>"true",
		:compression => 'deflate',
		:compressionLevel => '9' do
      end
    end
  end
end
