# Getting Started

1.  Create a new project in IntelliJ with a "Plugin" module
2.  Go to File > Project Structure:

    1.  Under "Libraries", add a new library from Maven: "org.lesscss:lesscss:1.3.0"
    2.  Under "Modules", add that library to the plugin module's dependencies

2.  Add a Run / Debug configuration for the plugin module
3.  Change the hard-coded paths in FileWatcherServiceImpl.java
4.  Test the plugin by going to Run > Run lessc-plugin

# TODO

1.  Make LESS/CSS paths configurable (per module or project if possible)
2.  Show progress indicator during compilation
3.  Move compilation to non-UI thread (if possible - IDEA's threading model may not permit this)
4.  Integrate with Maven LESS plugin (extension points and config)