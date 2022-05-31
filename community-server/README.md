# Conductor Community Server
Server build that builds Conductor server with all the community contributed modules

### FAQ
#### How this is server jar different from the server at Netflix/Conductor repository?
[Netflix/Conductor](https://github.com/Netflix/conductor/tree/main/server) server build does not contain any of the modules from this repository.
The server build in this repository is same as the main Conductor server with all the dependencies from the community 

#### How can I customize the dependencies?
See [build.gradle](build.gradle) file for all the dependencies. You can add/remove any additional dependencies here before running the build.

#### How can I use this binary in my docker builds
Follow the build process for docker at the [main repo](https://github.com/Netflix/conductor/tree/main/docker) and replace the conductor server with the output from this repository.

#### Where do I add the configuration for specific modules?
Conductor server at the startup reads the system  properties from the location specified in <code>CONDUCTOR_CONFIG_FILE</code>.
Use that file to add any module specific configuration (e.g. Jdbc datasource configuration)
