# browser_proio
A Java browser for MC events stored in ProIO format tuned to the HepSim repository.

The goal of this repository is to compile browser_proio.jar. You need to have updated file lib/proio-*.jar and the ant tool.
All dependensies are in the "lib" directory.

To build the jar file, type "ant". Then you can test this file as:

java -cp browser_proio.jar hepsimproio.HepSim [URL]

java -cp browser_proio.jar hepsimproioInfo [proiofile] 10


