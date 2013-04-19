#!/usr/bin/env ruby
# require 'git'
require 'fileutils'

begin
  j = 1;
  k = 0;
  m = k/j;
  raise 'xasdf'
  p m
rescue => details
  puts "rescue"
  p details
  p caller
end

