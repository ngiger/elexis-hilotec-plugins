#!/usr/bin/env ruby
# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
require 'fileutils'
require 'open-uri'
require 'optparse'
require "rexml/document"
include REXML

options = {}
options[:branch] = '2.1.7'
options[:copy]   = false
options[:dest]   = '/srv/www/www.medelexis.ch/plugins_doc'
options[:fix]    = false
options[:link]   = false
options[:url]   = 'http://www.medelexis.ch/plugins_doc'
OptionParser.new do |opts|
  opts.banner = "Usage: #{File.basename(__FILE__)} [options] [destdir=.] \n" +
%(      Helper for the docURL feature of the medelexis.xml.
        FullURL default to 
  )
  opts.on("-f", "--fix", "Fix all docURL in the medelexis.xml") do |v|
    options[:fix] = true
  end
  opts.on("-b", "--branch branch",  "Branch inside URL. Default from env GEN_REPO_LIBRARIES  is <#{options[:branch] }>") do |v|
    options[:branch] = v
  end
  opts.on("-c", "--copy [/path/to]",  "Copy all the documentation to the given path.
                                     Defaults to <#{options[:copy]}") do |v|
    options[:copy] = true
    options[:dest] = v if v
  end
  opts.on("-l", "--link",           "Create logical link from the category") do |v|
    options[:link] = true
  end
  opts.on("-u", "--URL",            "base part of URL to insert in docURL.
                                     Defaults to Defaults to <#{options[:url]}") do |v|
    options[:url] = v
  end
  opts.on("-h", "--help", "Show this help") do |v|
    puts opts
    exit
  end
end.parse!

class DocURL
  attr_reader :url, :logical_url, :file_name, :file_logical_name,
      :name, :type, :main_doc, :invisible

  def initialize(pfad, branch, baseUrl, basePath, main_doc)
    doc        = Document.new(File.new(pfad))
    @name      = doc.root.attributes['name']
    @type      = doc.root.attributes['type']
    @invisible = doc.root.attributes['category'].eql?('invisible')
    @main_doc  = main_doc
    basename   = File.basename(@main_doc)
    @logical_url       = patch("#{baseUrl}/#{branch}/#{@type}/#{@name}/#{basename}")
    @url               = patch("#{baseUrl}/#{branch}/#{@name}/#{basename}")
    @file_logical_name = patch("#{basePath}/#{branch}/#{@type}/#{@name}/#{basename}")
    @file_name         = patch("#{basePath}/#{branch}/#{@name}/#{basename}")
  end

private
  # patch file path to eliminate white space etc
  def patch(string)
    return string.gsub(/ /i, '_')
  end
end

class MedelexisDoc
  attr_reader :missing, :found, :verified, :failed, :modified, :link_added, :copied, :uptodate
  attr_reader :base_url, :branch, :install_dir, :has_no_docs, :mismatched_url, :is_invisible
  @@xml_to_modify = []

  def initialize(base_url, branch, install_dir)
    @base_url    = base_url
    @branch      = branch
    @install_dir = install_dir
    @missing = 0
    @found = 0
    @verified = 0
    @failed = 0
    @modified = 0
    @link_added = 0
    @has_no_docs = 0
    @mismatched_url = 0
    @is_invisible = 0
    @copied = 0
    @uptodate = 0
  end

  def get_attribute(pfad, attribute)
    doc = Document.new(File.new(pfad))
    return doc.root.attributes[attribute]
  end

  def add_docURL_if_necessary(pfad)
    docs = get_docs(pfad)
    if docs.size == 0
      @has_no_docs += 1
      return
    end
    doc_url = DocURL.new(pfad, @branch, @base_url, @install_dir, get_doc_info(pfad, docs))
    doc     = Document.new(File.new(pfad))
    adding  = false
    puts "change docURL in #{pfad} -> #{doc_url.url}"
    @modified += 1
    if  doc.root.elements['service:docURL']
      puts "modify #{pfad} from"
      puts "     #{doc.root.elements['service:docURL'].text}\n --> #{doc_url.url}" if $VERBOSE
      doc.root.elements['service:docURL'].text = doc_url.url
    else
      puts "adding #{pfad} -> #{doc_url.url}"
    adding = true
       doc.root.add_element("service:docURL", doc_url.url)
      doc.root.elements['service:docURL'].text = doc_url.url
    end
    if true
      new_medelexis_xml = File.open(pfad, 'w+')
      doc.write( new_medelexis_xml, -1)
      new_medelexis_xml.close
    else
      doc.write( $stdout, -1) if adding
    end
  end

  def add_logical_link(pfad)
    docs = get_docs(pfad)
    return if docs.size == 0
    doc_url = DocURL.new(pfad, @branch, @base_url, @install_dir, get_doc_info(pfad, docs))
    return if doc_url.invisible
    @link_added += 1
    puts "add_logical_link to #{pfad} for #{doc_url.url}" if $VERBOSE
    target = File.expand_path(doc_url.file_logical_name, @install_dir)
    source = File.expand_path(doc_url.file_name, @install_dir)
    FileUtils.makedirs(File.dirname(source), :verbose => $VERBOSE) unless File.exists?(File.dirname(source))
    FileUtils.makedirs(File.dirname(target), :verbose => $VERBOSE) unless File.exists?(File.dirname(target))
    FileUtils.rm(source, :verbose => $VERBOSE) if File.exists?(source)
    FileUtils.ln_s(target, source, :verbose => $VERBOSE, :force => true)
  end

  def copy_documentation(pfad)
    docs = Dir.glob(File.join(File.dirname(pfad), '**', '*.pdf'))
    return if docs.size == 0
    doc_url = DocURL.new(pfad, @branch, @base_url, @install_dir, get_doc_info(pfad, docs))
    dest_dir = File.dirname(doc_url.file_name)
    puts "copy_documentation #{docs.join(' ')}  #{dest_dir}" if $VERBOSE
    FileUtils.makedirs(dest_dir, :verbose => $VERBOSE) unless File.exists?(dest_dir)
    docs.each{
      |fName|
      dest_name = File.join(dest_dir, File.basename(fName))
      if FileUtils.uptodate?(dest_name, [fName])
        @uptodate += 1
      else
        @copied += 1
        FileUtils.cp(fName, dest_name, :verbose => $VERBOSE)
        if File.size(fName) != File.size(dest_name)
          puts "Size mismatch between #{fName}/#{File.size(fName)} bytes and #{dest_name}/#{File.size(dest_name)} bytes"
        else
          puts "Destination #{dest_name} is #{File.size(dest_name)} bytes long"
        end
    end
    }
  end

  def get_docs(pfad)
    return Dir.glob(File.join(File.dirname(pfad), '**', '*.pdf'))
  end
  
  def get_doc_info(pfad, docs)
    # http://www.medelexis.ch/plugins_doc/2.1.7/KG-Führung/doc/elexis-impfplan.pdf
    info = Hash.new
    main_doc = nil
    docs = Dir.glob(File.join(File.dirname(pfad), '**', '*.pdf'))
    pluginName = File.basename(File.dirname(pfad))
    [/documentation.pdf/i, /readme.pdf/i, /#{pluginName}/i
      ].each {
              |x|
      found = docs.find{ |aDoc| x.match(aDoc) }
      if found
        main_doc = found
        break
      end
             }
    main_doc =docs[0] unless main_doc
    return main_doc
  end

  def medelexis_xml_is_upto_date(pfad, check_url = true)
    doc = Document.new(File.new(pfad))
    # TODO: should check  "#{doc.root.elements['service:docURL'].text
    @type = doc.root.attributes['type']
    if /invisible/i.match(doc.root.attributes['category'])
      puts "Skipping invisible #{pfad}" if $VERBOSE
      docs = Dir.glob(File.join(File.dirname(pfad), '**', '*.pdf'))
    comment = %(
      Found 3 PDF-files in elexis-base/ch.rgw.utility/medelexis.xml
      Found 2 PDF-files in elexis-base/ch.elexis/medelexis.xml
      Found 3 PDF-files in elexis-base/ch.elexis.core/medelexis.xml
      Found 1 PDF-files in elexis-base/ch.elexis.labortarif.ch2009/medelexis.xml
      Found 1 PDF-files in elexis-addons/ch.elexis.scala.runtime/medelexis.xml
  )
      @is_invisible += 1
      return true
    end
    docs = Dir.glob(File.join(File.dirname(pfad), '**', '*.pdf'))
    if docs.size > 0
      puts "   Found #{docs.size} PDF-files in #{pfad}" if $VERBOSE
      if doc.root.elements['service:docURL']
        destination = doc.root.elements['service:docURL'].text
        if /www.medelexis.ch/.match(destination) && !destination.index(base_url)
          @mismatched_url += 1
          puts "Mismatched Medelexis-URL #{destination} in #{pfad}" # if $VERBOSE
          return false
        end
        return true unless check_url
        begin
          res = open(destination)
          @verified += 1
          puts "#{pfad} has correct URL #{destination}"
          return destination
        rescue
          return
          @failed += 1
          puts "Wrong URL #{destination} in #{pfad}" # if $VERBOSE
          return false
        end
        @found += 1
      else
        puts "not uptodate must add docURL to #{pfad}" if $VERBOSE
        @missing += 1
        return false
      end
    end
  end
end

if $0.eql?(__FILE__) # not used as a library
  puts "#{__FILE__} started with #{options[:url]} #{options[:branch]} #{options[:copy]} #{options[:dest]}"
  medelexis_docu = MedelexisDoc.new(options[:url], options[:branch], options[:dest])
  files = Dir.glob('*/medelexis.xml')+Dir.glob('*/*/medelexis.xml')+Dir.glob('*/*/*/medelexis.xml')
  puts " analysing #{files.size} files"
  nr_okays = 0
  files.collect {
    |fName|
      if res = medelexis_docu.medelexis_xml_is_upto_date(fName, !options[:copy])
        if options[:copy]
          medelexis_docu.copy_documentation(fName)
          medelexis_docu.add_logical_link(fName) if options[:link]
          next
        end
        if res == true
          puts "is invisible: #{fName} #{res.inspect}"
        else
          puts "is uptodate: #{fName} #{res}" unless options[:copy]
          nr_okays += 1
        end
        next
      end
      medelexis_docu.add_docURL_if_necessary(fName) if options[:fix]
      medelexis_docu.add_logical_link(fName) if options[:link]
  }

#  puts "found #{medelexis_docu.found} missing #{medelexis_docu.missing} verified #{medelexis_docu.verified} failed #{medelexis_docu.failed}"
  if options[:copy]
    puts "Result: #{medelexis_docu.copied} PDF fils copied. #{medelexis_docu.uptodate} were uptodate"
  else
    puts "Result: #{medelexis_docu.has_no_docs} had no docs"
    puts "        #{medelexis_docu.is_invisible} medelexis.xml were invisible"
    puts "        #{medelexis_docu.verified} URLs verified"
    puts "        #{medelexis_docu.modified} medelexis had no or a wrong docURL" if options[:fix]
    puts "        #{medelexis_docu.mismatched_url} had a wrong URL inside medelexis.ch"
    total = medelexis_docu.has_no_docs    +
            medelexis_docu.is_invisible  +
            medelexis_docu.modified       +
            medelexis_docu.is_invisible
    puts "        Is this really okay??? Should be zero  #{total} - #{files.size} = #{total - files.size}" if (total - files.size) != 0 and options[:fix]
  end
  puts "        #{medelexis_docu.link_added} logical links created" if options[:link]
end