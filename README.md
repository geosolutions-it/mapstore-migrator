mapstore-migrator
=================

MapStore-Migrator is an application that manages MapStore's configuration migration between different versions.

This application is Maven based and runs in Windows and Unix systems using the mojo's appassembler-maven-plugin.

In order to build and run MapStore-Migrator follow the steps below:

1) Go to the MapStore-Migrator root and run the following command from the command line:

	mvn install 

2) Then build the package:

	mvn package appassembler:assemble
	
3) The package will be available at the following path:

	%MapStore-Migrator%/target/mapstore-migrator
	
4) In order to run the application use the scripts in:

	%MapStore-Migrator%/target/mapstore-migrator/bin
	
	with name:
	
	- migrator (in Unix)
	- migrator.bat (in Windows)
