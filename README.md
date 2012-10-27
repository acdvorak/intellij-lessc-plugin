# What Does This Plugin Do?

```LESS Compiler``` monitors [LESS](http://lesscss.org/) files and automatically compiles them to CSS whenever they change.

# Features

1.  **Directory Monitoring**

    Monitors directories (and subdirectories) for changes to LESS files and automatically compiles them to CSS whenever they change.

    You can monitor as many LESS directories as you like.  You can also specify as many output directories as you like
    so that compiled CSS files will be copied to multiple locations (e.g., a ```src``` directory under source control
    and a ```target``` directory on a remote server).

    The directory structure of output CSS directories is the same as that of the source LESS directory.

2.  **Dependency Resolution**

    Re-compiles dependent files that ```@import``` a modified LESS file.

    For example, if ```home.less```, ```about.less```, and ```contact.less``` all ```@import "common.less"```,
    modifying ```common.less``` will cause all three dependent files to be re-compiled as well.

3.  **Context Menu Support**

    If a LESS file is modified when IntelliJ isn't running, it won't be automatically compiled (obviously).
    The next time you run IntelliJ, simply right-click anywhere in the editor or Project tree and select
    "Compile to CSS" to compile it.

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

*   **Slow First Compile**

    The first time you update a ```.less``` file it will take several seconds to compile.
    This is because the LESS compiler uses the [Rhino][rhino] JavaScript engine to run ```less.js```, and Rhino
    takes a while to initialize.  But worry not: after the initial compilation, all future updates will be nearly instantaneous.

# TODO

1.  Allow extension-less import paths (e.g., @import "some-file" = @import "some-file.less")
2.  Include / exclude filename patterns
3.  Per-file config options as LESS comments
4.  Catch circular @imports
5.  Integrate with Maven LESS plugin (extension points and config) (?)

# LESS CSS Compiler Version

This plugin includes a modified version of [asual][asual]'s [LESS CSS Engine][less-css-engine] and uses the official
```less.js``` compiler from [lesscss.org][lesscss-org].

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

[lesscss-org]: http://lesscss.org/
[asual]: http://www.asual.com/lesscss
[less-css-engine]: https://github.com/asual/lesscss-engine
[rhino]: https://developer.mozilla.org/en-US/docs/Rhino