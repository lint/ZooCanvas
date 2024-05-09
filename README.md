
# ZooCanvas

ZooCanvas is a command line application that allows multiple users to collaboratively edit a shared document. Rather than the document storing text, it is essentially an expansive grid of tiles which can be colored, creating a cohesive canvas.

Think of it as r/place, except purely in the shell environment.

ZooCanvas is implemented using ZooKeeper to provide coordination
between distributed processes. There are a few core ideas
that are implemented in order to accomplish this, including
quorums and leader election to manage decision making, a
hierarchical structure of nodes used to store data, and an
atomic broadcast protocol to deliver messages. With these features, ZooCanvas is able to maintain an up-to-date document across all connected users.

## Dependencies

In order to build and run ZooCanvas, there are a few dependencies required:
    
* Java: Use the following command to install the JDK to have access to `java` and `javac`
    * `sudo apt install default-jdk`

* UNIX Shell: ZooCanvas prints to the console in such a way that the current view is updated in place 
    * My implementation of this requires using UNIX specific escape characters which move the cursor position. As such, ZooCanvas needs to be run in a UNIX shell in order to function properly.
        
* Python: The graph generation script "create_graph.py" requires python to be installed. Which if you are using a UNIX shell, should be installed by default

## Building

To build ZooCanvas, type the following command in the project's root directory:

`make build` or `make`

This will compile all included .java files to .class files placed in the ./bld directory

## Running

In order to run ZooCanvas, there needs to be an available ZooKeeper instance running for it to connect to.
Use the following make commands in order to do this:

### Standalone Local ZooKeeper

To run a local standalone instance of ZooKeeper that is only a singular server, use the following command

`run_zk_server_local_standalone`
        
Then, in a separate terminal window, run the following command to start ZooCanvas:

`make run`

### Replicated Local ZooKeeper

To run a local replicated instance of the ZooKeeper service, enter the following commands in three separate terminal windows respectively:
 
```          
make run_zk_server1_local
make run_zk_server2_local
make run_zk_server3_local
```
ZooKeeper will only function when it can reach a majority, which means that at least two ZooKeeper instances must be running

Then, in a separate terminal window, run the following command to start ZooCanvas:

`make run`

### Replicated Distributed ZooKeeper

To run a distributed instance of the ZooKeeper service, such as in GENI, you must copy the project source to each GENI server 

Then you must ensure that the proper host:port addresses are set in the Makefile for the variables `geni1_address`, `geni2_address`, and `geni3_address`

The port is 2181 for each of them as that is the default port that ZooKeeper runs on

Then you must also ensure that the same address for each server is also contained in the files:
`./zookeeper/conf/geni_zoo1/zoo.cfg`
`./zookeeper/conf/geni_zoo2/zoo.cfg`
`./zookeeper/conf/geni_zoo3/zoo.cfg`

Modify the value `server.x` to each respective server address, keeping the ports the same, where x is the server number 1 through 3

The only thing that should be different in each of these files, is the dataDir value, which references a predefined path included in the project directory which allows each server to identify itself using the `myid` file
        
Then ssh into each of the GENI servers, copy the project, cd to the project directory, and execute the following command

* `make run_zk_server1_geni` if currently in server 1
* `make run_zk_server2_geni` if currently in server 2
* `make run_zk_server3_geni` if currently in server 3

Then on your local machine, excute one of the following commands: 

* `make run_geni_1` to connect to solely the ZooKeeper instance at server 1
* `make run_geni_2` to connect to solely the ZooKeeper instance at server 2
* `make run_geni_3` to connect to solely the ZooKeeper instance at server 3
* `make run_geni_all` to be able to connect to any of the ZooKeeper instances 


## Using ZooCanvas

### Before running ZooCanvas

Ensure that the console window is either a large enough size, or that it has been zoomed out

Printing the current chunk relies on moving the cursor around the console window, and also requires the entire chunk to be visible to work properly

If while running ZooCanvas, you notice strange and undesirable bugs related to the viewing of the current chunk, try resizing the window or zooming it out more

### Available Commands
    
* `set x y color`
    * This sets an individual tile in the current chunk at the provided coordinates to a given color

* `rect x1 y1 x2 y2 color`
    * This sets all tiles in the current chunk that are contained in a rectangle whose corners defined by the given coordinate pairs to the given color

* `checker x1 y1 x2 y2 color1 color2` 
    * Similar to the rect command, this colors all tiles in the rectangle defined by the given coordinates except it alternates between color1 and color2 to produce a checkerboard pattern

* `circle x y r fill_type color`
    * This creates a circle centered at the given x and y coordinates with a radius r
    * fill_type can be either “line”, which only colors the outline of the circle, or “fill” which colors in the entire circle

* `move direction`
    * This sets the new chunk to view based off the coordinates of the current chunk direction can be one of the following: up, down, left, right.

* `view x y`
    * This sets the new chunk to the given coordinates

* `write_test experiment_num num_writes`
    * This command is not used for any actual functionality within ZooCanvas, but rather used to perform experiments
    * This performs a basic rect command until the number of tiles written matches the `num_writes` value (an integer), which has a maximum value of the chunk size `experiment_num` is another integer which allows you to store several experiments of the same number of writes to file
    * It writes the current UNIX timestamp to the file `./experiment_output/write_<num_write>_<experiment_num>.txt`

* `store server_id experiment_num updates_expected`
    * This command is not used for any actual functionality within ZooCanvas, but rather used to perform experiments
    * During ZooCanvas operations, whenever a tile update is received, the current UNIX timestamp is added to a log: `./experiment_output/updates_<server_id>_<experiment_num>_<updates_expected>.txt`
    * The different parameters do not change the functionality of the command, just the path that it is stored at so that the data can be properly analyzed to create a an output graph


## Recreating Experiment

Recreating the experiment I performed is unfortunately a tedious manual process as it involves killing ZooKeeper instances, clearing their data, and then performing the next experiment run.
However, I will still describe the process I went through to achieve my results.

This follows the instructions under the "Replicated Distributed ZooKeeper" section to get 3 ZooKeeper replicas running on GENI nodes
    
The current values that are set for `geni1_address`, `geni2_address`, and `geni2_address` in the Makefile are the publicly routable IP addresses provided when creating a new geni slice with three different nodes in three different sites
    
Make sure to also set the address of `server.1`, `server.2`, and `server.3` to the respective addresses of `geni1_address`, `geni2_address`, `geni3_address` in each of the files:

```
./zookeeper/conf/geni_zoo1
./zookeeper/conf/geni_zoo2
./zookeeper/conf/geni_zoo3
```

Each `server.x` should have the following two ports after the addess ':2888:3888' which are used by ZooKeeper for transmitting data updates and managing leader election

Once these values are properly set, you can copy the project directory to each of the GENI nodes.

Then, use three separate terminal windows to ssh into each GENI node. In each of them, then navigate to the directory you copied the project to

Then execute the commands in each respective GENI node
* `make run_zk_server1_geni` if currently in server 1
* `make run_zk_server2_geni` if currently in server 2
* `make run_zk_server3_geni` if currently in server 3
    
This should get ZooKeeper up and running.

Then, open 4 new terminal windows and navigate to the project directory on your local computer in each of them

In 2 of these terminal windows, execute the command `make run_geni_1`

Then, execute the command `make run_geni_2` in one of the remaining terminal windows and the command `make run_geni_3` in the last terminal window

Now, all instances of ZooCanvas should show a blank canvas.

In one of the terminals in which you ran `make run_geni_1`, enter the ZooCanvas command `write 1 64`
This represents updating 64 tiles in the ZooCanvas system with experiment run number 1.

In the other three terminal windows, after view updates have been completed, execute the command `store x 1 64` where x is the respective server number

Now, one "run" of the experiment is complete. There should be four files in the `./experiment_output` directory:
`write_64_1.txt, updates_1_1_64.txt, updates_2_1_64.txt, and updates_3_1_64.txt`
    
To repeat a run with the same number of writes performed, the "1" value should be incremented to "2" in all four terminal instances.

In my experiment, I did 3 runs of each for each of the following number of write requests: 1, 64, 128, 256, 512, 768, 1024, 1280, 1578, 1792, and 2048

One the ideal number of runs have been completed, in the root project directory, execute the command `python create_graph.py`.
This will read the directory `./experiment_output` for all generated files, and produce a graph stored at `./graph.png`
