# What Does This Plugin Do?

```LESS Compiler``` monitors [LESS](http://lesscss.org/) files and automatically compiles them to CSS whenever they change.
It also notifies IntelliJ when the corresponding CSS file changes so that you don't have to manually synchronize
and upload them to your deployment target every time you update a LESS file.  You can configure ```lessc``` to
copy the compiled CSS file to any number of output directories.

```LESS Compiler``` allows you to maintain arbitrarily complex directory structures, unlike external tools such as
[SimpLESS](http://wearekiss.com/simpless) (which can only output to ```./``` or ```../css/```).
For example, suppose we have a project with the following directory structure
([LESS CSS Maven Plugin](https://github.com/marceloverdijk/lesscss-maven-plugin)'s default layout):

    projectRoot/
      +  src/main/
      |  +  less/
      |  |  +  common/
      |  |  |  -  common.less
      |  |  |  -  layout.less
      |  |  |  -  reset.less
      |  |  +  home/
      |  |  |  -  home.less
      |  |  +  checkout/
      |  |  |  -  checkout.less
      |  |  |  -  billing.less
      |  |  |  -  payment.less
      |  +  webapp/
      |  |  +  media/
      |  |  |  +  css/
      |  |  |  |  +  common/
      |  |  |  |  +  home/
      |  |  |  |  +  checkout/
      +  target/
      |  +  media/
      |  |  +  css/
      |  |  |  +  v2/
      |  |  |  |  +  common/
      |  |  |  |  +  home/
      |  |  |  |  +  checkout/

Such a structure would be impossible to maintain using other tools.  With ```LESS Compiler```, it's a breeze.

# Getting Started

1.  ```git clone git://github.com/acdvorak/intellij-lessc-plugin.git```
2.  In IntelliJ, go to File > Open Project... and select ```$PROJECT_DIR/lessc-plugin/lessc-plugin.iml```
3.  Create a Run / Debug configuration for the plugin module
4.  Test the plugin by going to Run > Run lessc-plugin

# Configuring the Plugin

1.  Go to File > Settings (IntelliJ IDEA > Preferences or CMD + , on Mac)
2.  Under Project Settings, select LESS Compiler
3.  Click the "+" button to add a new LESS profile
4.  Select the LESS source directory
5.  Add one or more CSS output directories
6.  Click OK

# Known Issues

*  The first time you update a ```.less``` file it will take several seconds to compile.
   This is because the LESS compiler needs to initialize, so be patient; after the initial compilation
   all future updates will be nearly instantaneous.

# TODO

1.  **Re-compile when an imported file is updated**
2.  Add per-file config options as LESS comments
3.  Add include / exclude filename pattern
4.  Integrate with Maven LESS plugin (extension points and config)

# LESS CSS Compiler Version

This plugin includes the [Official LESS CSS Compiler for Java](https://github.com/marceloverdijk/lesscss-java) version 1.3.0.

# Authors

**Andrew C. Dvorak**

*  https://github.com/acdvorak

# Copyright and License

Copyright 2012 Andrew C. Dvorak.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.