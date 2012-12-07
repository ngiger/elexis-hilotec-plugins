rem Script for Windows for the Elexis opendocument plugin
rem (c) copyright 2012 by Niklaus Giger
rem a lot less sophisticated as I did not find out how to get the equivalent of lsof / fuser / inotifywait
rem under windows as a non priviledged user
rem therefore MSWord must be closed before starting this application
rem or should I kill it here?
set EXE_TO_USE=%1
echo %EXE_TO_USE% %2 %3 %4 %5 %6 %7 %8 %9