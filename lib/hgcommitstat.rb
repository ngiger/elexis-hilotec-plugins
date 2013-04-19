# Copyright 2011 by Niklaus Giger <niklaus.giger@member.fsf.org
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

module HgCommitStat
  @@allAuthors = Hash.new
  Commit = Struct.new("CommitInfo", :commits, :files, :inserts, :deletes)
  def HgCommitStat::addInfo(author, files, inserts, deletes)
    if @@allAuthors[author] then
      @@allAuthors[author].commits += 1
      @@allAuthors[author].files   += files.to_i
      @@allAuthors[author].inserts += inserts.to_i
      @@allAuthors[author].deletes += deletes.to_i
    else
      @@allAuthors[author] = Commit.new(1, files.to_i, inserts.to_i, deletes.to_i)
    end
  end

  def HgCommitStat::showInfo
    puts "Show condensed info"
    @@allAuthors.sort.reverse.each{
    |author, info|
      puts "#{sprintf("%15s",author)} #{sprintf("%4d",info.commits)} commits #{sprintf("%6d", info.files)} "+
      "files changed. lines added #{sprintf("%6d",info.inserts)} deleted #{sprintf("%6d",info.deletes)}"
    }
  end

end
