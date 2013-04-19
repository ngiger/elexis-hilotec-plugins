#!/usr/bin/env ruby
# encoding: utf-8
# Copyright 2012 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Generates PDFs from all *.textile & doc/*.textile files in each project
# Places a copy under the top level doc/<plugin-name> directory
#

module Wikitext
  include Extension

  def Wikitext::skipDoc
    return @@skipDoc
  end

  def Wikitext::getRootDoku
    File.join(@@rootPath, 'target', 'doc')
  end
  def Wikitext::getRootProject
    @@rootProject
  end

  def Wikitext::HtmlFromTextile(dest, src)
    raise " Wikitext::HtmlFromTextile expects a texile file as input. given #{src}" if !File.basename(src).index('.textile')
    Java.load # needed to load class path for apache logger
    Buildr.ant('wikitext_to_html ') do |wikitext|
        wikitext.echo(:message => "wikitext_to_html #{dest}")
        wikitext.taskdef(:name=>'wikitext_to_html',
        :classname=>'org.eclipse.mylyn.wikitext.core.util.anttask.MarkupToHtmlTask',
        :classpath=> @@wikitextJars.join(File::PATH_SEPARATOR))
      wikitext.wikitext_to_html :validate => 'false',
        :formatOutput => true,
        :overwrite => true,
        :sourceEncoding => 'UTF-8',
        :multipleOutputFiles => true,
        :markupLanguage => 'Textile' do
        wikitext.fileset(:dir => File.dirname(src), :includes => File.basename(src))
      end
      FileUtils.makedirs(File.dirname(dest))
      # Remove wrong header at top of the file
      inhalt = IO.read(src.sub('.textile','.html')).sub("<?xml version='1.0' encoding='utf-8' ?>",'')
      File.open(dest,"w+") { |x| x.puts inhalt }
    end
  end

  def Wikitext::foFromTextile(dest, src)
    raise " Wikitext::foFromTextile expects a texile file as input. given #{src}" if !File.basename(src).index('.textile')
    Java.load # needed to load class path for apache logger
    Buildr.ant('wikitext_to_xslfo') do |wikitext|
        wikitext.echo(:message => "wikitext_to_xslfo #{dest}")
        wikitext.taskdef(:name=>'wikitext_to_xslfo',
        :classname=>'org.eclipse.mylyn.wikitext.core.util.anttask.MarkupToXslfoTask',
        :classpath=> @@wikitextJars.join(File::PATH_SEPARATOR))
      FileUtils.makedirs(File.dirname(dest))
      wikitext.wikitext_to_xslfo :targetdir=>File.dirname(dest),
        :validate => 'false',
        :sourceEncoding => 'UTF-8',
        :markupLanguage => 'Textile' do
        wikitext.fileset(:dir => File.dirname(src), :includes => File.basename(src))
      end
    end
  end

  def Wikitext::pdfFromFo(dest, src)
    raise " Wikitext::pdfFromFo expects a fo file as input. given #{src}" if !File.basename(src).index('.fo')
    cmd = "fop #{src} #{dest}"
    if !@@skipDoc
      res= system(cmd, true)
      if !res || !File.exists?(dest)
        puts "FOP: failed running #{cmd}. Generate invalid pdf"
        File.open(dest, 'w+') {|f| f.write("# #{__FILE__} #{Time.now}\n# Could not convert #{src} -> *.pdf") }
      end
    end
  end

  def Wikitext::pdfFromTextile(dest, src)
    raise " Wikitext::pdfFromTextile expects a texile file as input. given #{src}" if !File.basename(src).index('.textile')
    foFile = dest.sub('.pdf','.fo')
    file dest => src do
      Wikitext::foFromTextile(foFile, src)
      Wikitext::pdfFromFo(dest, foFile)
    end
  end

  first_time do
    # Define task not specific to any projet.
    # Under Debian squeeze fop 0.95 is installed, which will not respond correctly to fop -version
    require 'rbconfig'
    include RbConfig
    @@skipDoc = false
    /linux/i.match( CONFIG['host_os']) ? fopCmd = 'which fop' : fopCmd = 'fop -version'
    [ fopCmd, 'texi2pdf --version'].each {
      |cmd|
      if !system(cmd) # an easy way to check whether fop works or not
        puts "Setup: #{cmd.split(' ')[0]} is not installed. Skip generating documentation"
        @@skipDoc = true
      end
    }
    [ 'OSGi', ENV['OSGi'], ENV['P2_EXE'] ].each {
      |path|
      next if !path
      jarName = File.join(File.expand_path(path),'plugins','org.eclipse.mylyn.wikitext.*core*jar')
      if Dir.glob(jarName).size > 0
        @@wikitextJars = Dir.glob(jarName)
        puts "Setup: wikitextJar found using #{jarName}"
        break
      end
                                              }
    if !defined?(@@wikitextJars)
    raise %(Could not find wikitextJar via environment variables OSGi, P2_EXE or directory OSGi.
In one of these environments we must find a plugins/org.eclipse.mylyn.wikitext.*core*jar.
The org.eclipse.mylyn.wikitext.*core*jar is included if you install in your Eclipse the MyLyn Wikitext
)
    end unless @@skipDoc
    puts "Setup: texi2pdf and fop are installed. Will generate documentation" if !@@skipDoc 
    Project.local_task('doc') if !@@skipDoc
  end

  before_define do |project|
    if !project.parent
      desc 'create PDF from tex/textile' 
      @@rootPath = project._.clone
      @@rootProject = project
    end
    # Define the docx task for this particular project.
    Project.local_task('doc')
  end
  
  after_define do |project|
    if !@@skipDoc 
      project.extend Wikitext
      files = (Dir.glob(File.join(project._, '*.textile')) + Dir.glob(File.join(project._, 'doc', '*.textile')))
      if files.size > 0
        files.each do |src|
          dest = File.join(project.path_to(:target), 'doc', "#{File.basename(src, '.textile')}.pdf")
          project.package(:plugin).include(dest)
          Wikitext::pdfFromTextile(dest, src)
        task 'doc' => [ dest ]
        if project.parent
          copyInTopName = File.join(Elexis::get_deploy_dir, 'dox', project.name.sub(project.parent.name+':', ''), File.basename(dest))
          file  copyInTopName => dest do
            FileUtils.makedirs(File.dirname(copyInTopName))
            FileUtils.cp(dest, copyInTopName, :preserve => true,:verbose => true)
            project.parent.package(:plugin).include(copyInTopName)
          end
        project.task('doc' => copyInTopName)
        @@rootProject.task('doc' => copyInTopName)
          end
        Rake::Task.define_task 'doc'
        end
      end
    end
  end
end

class Buildr::Project
  include Wikitext
end


def runCmdInProjectDir(cmd, mayFail=false)
  saved = Dir.pwd
  Dir.chdir(project.base_dir)
  trace "cd #{base_dir} && #{cmd}"
  res = Kernel::system(cmd)
  if !res 
    puts "cmd #{cmd} failed"
    exit(2) if !mayFail
  end
ensure
  Dir.chdir(saved)
  res
end

def genDoku(restrictTo = '*.tex')
  puts "genDoku skip is #{Wikitext::skipDoc} #{project}"
  return if Wikitext::skipDoc

  texFiles = Dir.glob(_(restrictTo))
  texFiles.each{ 
    |f| 
      pdf =  f.sub('.tex','.pdf')
      short = "#{name.split(':')[-1]}"
      dest = File.join(Wikitext::getRootDoku, short)
      dest = Wikitext::getRootDoku if short.eql?('dokumentation')
      dest = File.join(dest, File.basename(pdf))
      file dest => f do
        runCmdInProjectDir("texi2pdf --silent #{File.basename(f)}")
        FileUtils.makedirs(File.dirname(dest))
        FileUtils.cp(pdf, dest, :preserve => true,:verbose => true)
      end
      project.package(:plugin).include(dest)
      task 'doc' => [ dest ]
      Wikitext::getRootProject.task('doc' => dest)
  }
end

