#!/usr/bin/env ruby
$: << File.dirname(Dir.pwd)
$: << File.dirname( __FILE__)
require 'hgrepo'
require 'elexis'
Elexis::readBuildCfg
# require 'elexis_conf'
puts %(# saving this output as elexis_conf_override.rb overrides definitions made in elexis_conf_override
# generated #{Time.now.utc} from #{__FILE__}
)
BuildCfg.each{ |key, value|
                         puts "BuildCfg['#{key}'] ||= '#{value}'"
                       }
                         x = %(                         }
BuildCfg['DefaultBranch'] ||= '#{BuildCfg['DefaultBranch']}'
BuildCfg['EclipseVers']   ||= '#{BuildCfg['EclipseVers']}'
BuildCfg['SkipPlugins']   ||= '#{BuildCfg['SkipPlugins']}'
)
HgRepo::allRepos.each {
  |id, aRepo|
    str = "HgRepo.new('#{aRepo.id}', '#{aRepo.url}'"
    str += ", #{ aRepo.forceBranch}" if aRepo.forceBranch
    str += ')'
    puts str
}