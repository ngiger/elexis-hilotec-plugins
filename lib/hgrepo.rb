# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

require 'hg_util'

class HgRepo
  attr_reader :id, :baseDir, :url, :forceBranch
  @@allRepos ||= {}

  def HgRepo::allRepos
    return @@allRepos
  end

  def HgRepo::readConfiguration
  end

  def getUrlFromDefaultPush
    hgRcName = "#{@baseDir}/.hg/hgrc"
    File.readlines(hgRcName).each{
    |x|
      x.chomp!.gsub!(' ','')
      name= x.split('=')[0]
      if name.eql?('default-push')
        @url = x.split('=')[1]
        puts "getUrlFromDefaultPush: #{@url}" if $VERBOSE
        return @url
      end
    } if File.exists?(hgRcName)
    return nil
  end

  def getUrlFromDefault
    hgRcName = "#{@baseDir}/.hg/hgrc"
    File.readlines(hgRcName).each{
    |x|
      x.chomp!.gsub!(' ','')
      name= x.split('=')[0]
      if name.eql?('default')
        @url = x.split('=')[1]
        puts "getUrlFromDefault: #{@url}" if $VERBOSE
        return @url
      end
    } if File.exists?(hgRcName)
    return nil
  end
  
  def getUrlFromHgsub
    if File.exists?("#{Dir.pwd}/.hgsub") then
      File.readlines("#{Dir.pwd}/.hgsub").each{
      |x|
        x.chomp!.gsub!(' ','')
        name= x.split('=')[0]
        if name.eql?(@id)
          @url = x.split('=')[1]
          puts "getUrlFromHgsub: #{@url}" if $VERBOSE
          return @url
        end
      }
    end
  end

  def initialize(where=Dir.pwd, url=nil, forceBranch=nil)
    puts "HgRepo.new #{where} #{url} #{forceBranch}"  if $VERBOSE
    @url     = url
    @baseDir = File.expand_path(where)
    @id      = File.basename(@baseDir)
    if @@allRepos[@id]
      puts "id #{@id} schon definiert as #{@@allRepos[@id].inspect}" if $VERBSOE
      return @@allRepos[@id]
    else
      @forceBranch = forceBranch
      puts "#{@id}: @forceBranch ist #{@forceBranch}" if @forceBranch and $VERBOSE
      @@allRepos[@id] = self
      unless getUrlFromDefaultPush
        unless getUrlFromDefault
          getUrlFromHgsub
        end
      end
      return self if File.directory?(@baseDir)
    end
  end

  def clone
    cmd = "hg clone #{@url} #{@baseDir}"
    system(cmd)
  end

  def update(branch=nil, args=nil)
    Dir.chdir(@baseDir) if File.directory?(@baseDir)
		hasTags = tags
    if hasTags     and     hasTags.index(branch) then
      i =  hasTags.index(branch)
      revision = `hg tags `.split[i*2+1].split(':')[0]
      branch = "-r #{revision}"
    else
      branch = nil if branch and branches and branches.index(branch) == nil
			branch = @forceBranch if branch == nil && @forceBranch != nil
			puts "#{@id}: @forceBranch to #{@forceBranch.inspect} branch is now #{branch}" if @forceBranch and $VERBOSE
    end
    system("hg update #{branch} #{args}")
  end

  def branches()
    all=[]
    Dir.chdir(@baseDir) if File.directory?(@baseDir)
    b = `hg branches`.split("\n")
    b.each do |x|
      all << x.split[0]
    end
    all
  end

  # return the current revision and branch 
  def getRevisionAndBranch
    b = `hg identify`.chomp
    b.split(' ')
  end
  
  def tags()
    all=[]
    Dir.chdir(@baseDir) if File.directory?(@baseDir)
    b = `hg tags`.split("\n")
    b.each do |x|
      all << x.split[0]
    end
    all
  end

  def branch(name=nil)
    unless  name
      `hg branch`.chomp
    else
      return if branch == name
      system("hg branch #{name}")
      myBranch=branch
      commit("Branch #{name}: created based on #{myBranch}")
    end
  end

  def tag(tag, comment=nil, options = nil)
    if @forceBranch
      puts "#{@baseDir}: forceBranch #{@forceBranch} overrides new tag #{tag}"
      return
    end if false
    options = nil if options.class == FalseClass
    msg="Tag #{tag}: #{comment}"
    system("hg tag -m '#{msg}' #{tag} #{options}")
  end

  def commit(msg, mayFail=false, user=nil)
    cmd = "hg commit -m '#{msg}'"
    cmd += " --user #{user}" if user 
    system(cmd, nil, mayFail)
  end

  def pull
    puts "Pulling #{@id}" if $VERBOSE
    begin
      system("hg pull")
    rescue Exception => ex
      puts "Pulling #{@id}: Failed Still continuing"
      puts ex
    end
  end

  def add(name)
    system("hg add #{name}")
  end

  def checkout(name, options = nil)
    system("hg checkout #{name} #{options}")
  end

  def rename(alt, neu)
    system("hg rename #{alt} #{neu}")
  end

  def merge(name)
    system("hg merge #{name}")
  end

  def transplant(what)
    system("hg transplant #{what}")
  end

  def import(what)
    system("hg import #{what}")
  end

  def applyTarGz(tarFile)
    system("tar -zxvf #{tarFile}")
  end

  def addSubRepo(cloneFrom, name=nil, isSvn=false)
    name = File.basename(cloneFrom) unless name
    if isSvn
      hgsubSetDir(name, "[svn]#{cloneFrom}")
    else
      hgsubSetDir(name, cloneFrom)
    end
    subRepo = nil
    dir = "#{@baseDir}/#{name}"
    if isSvn
      then
      return if File.directory?(dir)
      system("svn checkout --quiet #{cloneFrom} #{dir}")
    else
    subRepo = HgRepo.new(dir, cloneFrom)
    end
    subRepo
  end

  private

  def hgsubSetDir(name, cloneFrom)
    return if (defined?(DryRun) and DryRun)
    hgsubName="#{@baseDir}/.hgsub"
    neu = []
    needsAdd = true unless File.exists?(hgsubName)
    neu = File.readlines(hgsubName) if File.exists?(hgsubName)
    neu << "#{name} = #{cloneFrom}\n"
    neu.sort!.uniq!
    hgsub=File.open( hgsubName, 'w+')
    hgsub.puts(neu)
    hgsub.close
    add(hgsubName) if needsAdd
  end

  def system(cmd, where = @baseDir, mayFail=false)
    @@savedDir = Dir.pwd
    if where && File.directory?(where) && where != Dir.pwd  then
      Dir.chdir(where)
    else
      where = Dir.pwd
    end
    puts "cd #{where} && #{cmd}"  # mayFail #{mayFail}"
    return if defined?(DryRun) and DryRun
    res =Kernel.system(cmd)
    if !res and !mayFail then
      puts "running #{cmd} #{mayFail} failed"
      exit
    end
    Dir.chdir(@@savedDir)
  end

end
