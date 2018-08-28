# ProIO Browser for the HepSim repository

A Java browser for MC events stored in ProIO format tuned to the HepSim repository.

The goal of this repository is to compile the file "browser_proio.jar". 
You need to have updated file "lib/proio-[version].jar" and the apache ant tool.
All dependensies are in the "lib" directory.

To build the jar file, type "ant". Then you can test this file on a ProIO file as:

```
java -cp browser_proio.jar hepsimproio.browser.Main [file] # shows GUI
java -cp browser_proio.jar hepsimproio.Info [file]
java -cp browser_proio.jar hepsimproio.Info [file] 10
```

S.Chekanov (ANL) 
