#!/usr/bin/env ruby
#
# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# == Synopsis
# remove unneeded plug-ins to concentrate on important one
#
ScriptDir = File.expand_path(File.dirname( __FILE__))
$: << ScriptDir
$: << File.expand_path(File.dirname( __FILE__))+"/lib"

require 'fileutils'

def rm_unneeded(keepDirs)
	allDirs = Dir.glob('**/.project')
	allDirs.each{|x| x.sub!('/.project', '') }
	keepDirs.each{
		|k|
			puts "keep #{k}" if $VERBOSE
	}
	allDirs.each {
		|x|
			short = File.basename(x)
			next if keepDirs.index(short)
			FileUtils.rm_rf(x, :verbose => true)
	}
end

coreDirs = ['ch.rgw.utility',
            'ch.elexis.core',
            'ch.elexis',
            'ch.elexis.h2.connector',
            'ch.elexis.mysql.connector',
            'ch.elexis.postgresql.connector',
            'ch.elexis.importer.div',
            'ch.elexis.arzttarife_ch',
            ]
# Branch 2.1.7
# Problem 1 with medics
addDirs = ['ch.elexis.laborimport.medics',
           'ch.elexis.ebanking_ch',
           'ch.elexis.artikel_ch',
           'ch.elexis.labortarif.ch2009',
           'ch.elexis.hl7.v2x'
          ]
# Buildr: Does compile if only these
# ant: has problems in ch.elexis.importer.div with org.apache.commons.logging
# Problem 2 with ch.elexis.connector.be
addDirs = ['ch.elexis.connector.be',
           'ch.elexis.ebanking_ch',
           'ch.elexis.artikel_ch',
          ]

org.slf4j.bridge
rm_unneeded(coreDirs + addDirs)


puts <<Rebuild
# commands to rebuild are:
ln -s /opt/elexis-2.1.7-rm/OSGi .
lib/gen_buildfile.rb
time rvm jruby do buildr osgi:clean:dependencies osgi:resolve:dependencies osgi:install:dependencies| tee depend.log
time rvm jruby do buildr test=no clean package 2>&1 | tee build.log
Rebuild

# 53 Sekunden for dependencies
# 