All sources are embedded in the Sfx-1.0.0-all.jar in case we need to modify the source. For all practical purposes
we treat this as a downloaded utility.

To build
Linux/Windows
```bash
./gradlew clean build
```

Windows
```dos
gradlew.bat clean build
```

Building produces an install directory `./build/install/Sfx-shadow` and the bin folder contains the executable.
Running the following command without any arguments will list the command help.
Linux/Mac
```bash
./build/install/Sfx-shadow/bin/Sfx
```

Windows
```dos
.\build\installSfx-shadow\bin\Sfx
```

The options are as shown below.
```asciii

Option (* = required)            Description
---------------------            -----------
--an, --artifactName [String]    Name of the artifact to create (without the
                                   extension). Defaults to name of the name of
                                   the leaf input directory
* -d, --directory <File>         Directory to add to the zip archive
--wd, --workingDirectory [File]  Working directory. Zip file entries are
                                   created relative to this. Defaults to
                                   directory to be archived
```

For e.g.

Running the following

Linux/Mac
```bash
./build/install/Sfx-shadow/bin/Sfx -an test -d <full-path-to>/src
```

Windows
```dos
.\build\installSfx-shadow\bin\Sfx -an test -d <full-path-to>\src
```

will produce a self extracting `test.exe` (compatible with the Windows CE 6/7 platform).