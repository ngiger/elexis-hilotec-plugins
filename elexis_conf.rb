# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# == Synopsis
# configuration of elexis build
# consider downloading your personal override of this file for repeatable scripts from a git repository,
# e.g via wget https://raw.github.com/ngiger/elexis-builds/elexis-2.1.6.4.1.opensource elexis_conf_override.rb
# an override file can be created by calling lib/gen_override.rb and redirecting the output
BuildCfg['DefaultBranch'] ||= '2.1.7'
BuildCfg['EclipseVers']   ||= "indigo-SR2"
BuildCfg['SkipPlugins']   ||= ',elexis-hilotec-plugins/com.hilotec.elexis.messwerte.v2,'+
    'medshare-licence-generator,'
HgRepo.new('elexis-base',                 "http://elexis.hg.sourceforge.net/hgweb/elexis/elexis-base")
HgRepo.new('elexis-addons',               "http://elexis.hg.sourceforge.net/hgweb/elexis/elexis-addons")
# orig from http://hg.medshare.net/elexis-distrib
HgRepo.new('elexis-distrib',              'http://bitbucket.org/ngiger/elexis-distrib',                'default')
# orig from https://code.google.com/p/archie/
HgRepo.new('svn/ch.unibe.iam.scg.archie', 'https://bitbucket.org/ngiger/ch.unibe.iam.scg.archie',       'elexis-2.1')
# Niklaus removed com.hilotec.elexis.messwerte.v2 git://github.com/FreakyPenguin/elexis-hilotec-plugins.git
HgRepo.new('elexis-hilotec-plugins',      'git://github.com/ngiger/elexis-hilotec-plugins.git',  'default')
# orig from git://github.com/jsigle/com.jsigle.noatext_jsl.git
HgRepo.new('jsigle/noatext',              'git://github.com/ngiger/com.jsigle.noatext_jsl.git',         'default')
