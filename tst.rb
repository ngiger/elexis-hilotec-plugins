/opt/elexis-2.1.7-rm/deploy/elexis_plugins/ch.elexis.h2.connector-1.2.1.20121101.jar
 The next example reopens my.zip writes the contents of first.txt to standard out and deletes the entry from the archive.

require 'zip/zip'
tstFile = '/opt/elexis-2.1.7-rm/deploy/elexis_plugins/ch.elexis.h2.connector-1.2.1.20121101.jar'
puts File.size(tstFile)
tstFile = '/opt/elexis-2.1.7-rm/elexis-base/ch.rgw.utility/target/ch.rgw.utility-2.0.8.20121101.jar'
def addition(file)
  Zip::ZipFile.open(file) {
    |zipfile|
    classFiles = zipfile.foreach{ |x| /\.class$/i.match(x) }
    puts zipfile.entries.size
    puts classFiles.size
  }
end

addition(tstFile)
Zip::ZipFile.open(tstFile) do |zipfile|
  zipfile.entries.each do |entry|
    puts entry.name
  end
end

def addition(tstFile)
  classFiles = []
  Zip::ZipFile.foreach(tstFile) do |entry|
      classFiles << entry if /\.class$/i.match(entry.name)
  end
puts classFiles.size
