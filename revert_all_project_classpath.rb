#!/usr/bin/env ruby

Dir.glob(File.join(Dir.pwd, '**', '.hg','hgrc')).each {
  |x|
   base_dir = File.dirname(File.dirname(x))
  puts base_dir
  Dir.chdir(base_dir)
  system("hg revert */.project */.classpath")
}
Dir.glob('**/*.orig').each{ |x| puts x }
 # system('find . -name *.orig | xargs rm')
