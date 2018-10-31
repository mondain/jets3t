JetS3t
======

JetS3t is a free, open-source Java toolkit and application suite for
Amazon Simple Storage Service (Amazon S3), Amazon CloudFront content
delivery network, and Google Storage for Developers.

For further information, documentation, and links to discussion lists and
other resources, please visit the [JetS3t web site][jets3t]. There are also
[historic release notes][jets3t-releasenotes-hist] summarizing the changes
in past JetS3t releases, and [pending release notes][jets3t-releasenotes-pend]
that include details about the up-coming release.

[jets3t]: http://www.jets3t.org/
[jets3t-toolkit]: http://www.jets3t.org/toolkit/toolkit.html

[jets3t-releasenotes-hist]: http://www.jets3t.org/RELEASE_NOTES.txt
[jets3t-releasenotes-pend]: https://bitbucket.org/jmurty/jets3t/src/tip/RELEASE_NOTES.txt

[jets3t-cockpit]: http://www.jets3t.org/applications/cockpit.html
[jets3t-synchronize]: http://www.jets3t.org/applications/synchronize.html
[jets3t-cockpitlite]: http://www.jets3t.org/applications/cockpitlite.html
[jets3t-uploader]: http://www.jets3t.org/applications/uploader.html


Running Applications
--------------------

Each application can be run using a script in the _bin_ directory.
To run an application, such as Cockpit, run the appropriate script from
the bin directory for your JetS3t version ("x.y.z" in these examples).

Windows:

    cd jets3t-x.y.z\bin
    cockpit.bat

Unixy:

    bash jets3t-x.y.z/bin/cockpit.sh


Configuration files
-------------------

Applications or library components generally read text configuration files,
which must be available in the classpath of a running application to be useful.

Example configuration files are located in the _configs_ directory. The
run scripts in the _bin_ directory automatically include this _configs_
directory in the classpath when running JetS3t apps.

The configuration files include:

 * `jets3t.properties`
    Low-level toolkit configuration.
 * `synchronize.properties`
    Properties for the Synchronize application
 * `uploader.properties`
    Properties for the Uploader application
 * `cockpitlite.properties`
    Properties for the CockpitLite application
 * `mime.types`
    Maps file extensions to the appropriate mime/content type.
    For example, the "txt" extension maps to "text/plain".
 * `commons-logging.properties`
    Defines which logging implementation to use.
 * `log4j.properties`
    When Log4J is the chosen logging implementation,
    these settings control how much logging information is displayed, and
    the way it is displayed.
 * `simplelog.properties`
    When SimpleLog is the chosen logging implementation,
    these settings control the logging information that is displayed.


JAR files
---------

The compiled JetS3t code jar files are available in the _jars_ directory,
and include the following:

 * `jets3t-x.y.z.jar`

   The [JetS3t toolkit][jets3t-toolkit], including the JetS3t service implemention
   which underlies all the other JetS3t applications.

 * `jets3t-gui-x.y.z.jar`

   Graphical user interface components used by JetS3t GUI applications such as
   Cockpit. These components are not required by the command-line Synchronize
   tool, nor by non-graphical programs you may build.

 * `cockpit-x.y.z.jar`

   [Cockpit][jets3t-cockpit], a GUI application/applet for viewing and managing
   the contents of an S3 account.

 * `synchronize-x.y.z.jar`

   [Synchronize][jets3t-synchronize], a console application for synchronizing
   directories on a computer with an Amazon S3 account.

 * `cockpitlite-x.y.z.jar`

   [CockpitLite][jets3t-cockpitlite], a GUI application/applet for viewing and
   managing the contents of an S3 account, where the S3 account is not owned by
   the application's user directly but is made available via the Gatekeeper servlet.

 * `uploader-x.y.z.jar`

   [Uploader][jets3t-uploader], a wizard-based GUI application/applet that S3
   account holders (Service Providers) may provide to clients to allow them to
   upload files to S3 without requiring access to the Service Provider's S3
   credentials


Compatibility and Performance of Distributed Jar files
------------------------------------------------------

The class files in these jars are compiled for compatibility with Sun's
JDK 5 and later, and have debugging turned on to provide more information
if errors occur.

To use JetS3t in high-performance scenarios, the classes should be
recompiled using the latest version of Java available to you and with
debugging turned off.


Building JetS3t from source
---------------------------

The JetS3t distribution package includes an ANT build script (`build.xml`) that
allows you to easily rebuild the project yourself, and a default set of build
properties (`build.properties`) that you may wish to modify.

The following ANT command will recompile the JetS3t library and applications:

    ant rebuild-all

To repackage JetS3t applications or applets with your modifications for
redistribution:

    ant repackage-applets


Servlets
--------

The JetS3t application suite includes a servlet implementation of a Gatekeeper
to offer mediated third-party access to your S3 resources. The deployable WAR
file for this servlet is located in the _servlets/gatekeeper_ directory.
