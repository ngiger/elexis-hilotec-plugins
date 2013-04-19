#!/usr/bin/env ruby
# requires apt-get install ethtool wakeonlan
mac_addr = '40:6c:8f:48:b1:5f'
cmd = "wakeonlan #{mac_addr}"
p cmd
p system(cmd)

# Dies funktioniert nicht, wenn man ihn ganz abschaltet.
# Wenn man in den Ruhezustand geht, braucht es nichts besonders
# gemäss http://support.apple.com/kb/HT3468
# idle 14W Max 85W
# gemäss http://www.macworld.com/article/1161414/mac_mini_mid_2011_review.html
# 9W when idle, 1,5 when sleep
