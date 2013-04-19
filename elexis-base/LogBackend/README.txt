Logback back-end Bundle for OSGI
Author: Rodrigo Reyes, 2008

WHAT IS DOES
------------

This bundle provides a bridge between the OSGi Logging Service and the
Logback library. It listens to LogEvent objects emitted by the 
LogReaderService and converts them into slf4j calls backed by
the logback library.

How to use
----------

1. Install and start the bundle, it works out of the box. Logback uses its
   default configuration, sending the logs to standard output, with a default
   formatter.

2. (optional) configure the logback configuration: set the 
   "logback.configurationFile" system property to the location of an xml
   configuration file.
   For instance, start your java with the VM argument 
        -Dlogback.configurationFile=/some/location/logback.xml

That's it.

LICENCE
-------

 This software is licensed under the Apache License, Version 2.0

 you may not use this file except in compliance with the License. 
 You may obtain a copy of the License at 
 
 http://www.apache.org/licenses/LICENSE-2.0 
 
 Unless required by applicable law or agreed to in writing, software 
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 License for the specific language governing permissions and limitations 
 under the License. 

LOGBACK LICENCE
---------------

Logback is licensed under the LGPL. Please visit the Logback homepage:
http://logback.qos.ch/

SLF4J LICENCE
-------------

The SLF4J library is licensed under the slf4j licence, identical to the
MIT license. Please visit their homepage for additional details:
http://www.slf4j.org/

