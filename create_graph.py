import os
import matplotlib.pyplot as plt
import numpy as np
import json


data = {}

# read all files experiment output directory
files = os.listdir("experiment_output")

# iterate over every file
for file in files:

    isUpdateFile = True
    serverId = 0
    experimentNum = 0
    writes = 0
    timestamp = 0

    # check if the file was created by the client sending the write requests or the server which logs their received results
    if "updates" in file:
        parts = file.split("_")
        serverId = int(parts[1])
        experimentNum = int(parts[2])
        writes = int(parts[3].split(".")[0])
    else: 
        isUpdateFile = False
        parts = file.split("_")
        writes = int(parts[1])
        experimentNum = int(parts[2].split(".")[0])
    
    # open each file and extract the relevant timestamp
    with open("experiment_output/" + file, "r") as f:
        fileData = f.readlines()
        
        if isUpdateFile:
            timestamp = int(fileData[-1].strip().split(",")[1])
        else:
            timestamp = int(fileData[-1].strip())

    # write each timestamp to the correct location so that it can be easily graphed

    if writes not in data:
        data[writes] = {}
    
    if isUpdateFile:
        if "last_read_update" not in data[writes]:
            data[writes]["last_read_update"] = {}
        
        if serverId not in data[writes]["last_read_update"]:
            data[writes]["last_read_update"][serverId] = {}
        
        data[writes]["last_read_update"][serverId][experimentNum] = timestamp
    else:

        if "write_time" not in data[writes]:
            data[writes]["write_time"] = {}

        data[writes]["write_time"][experimentNum] = timestamp

# split the data into separate arrays for each server
server1_x = []
server1_y = []
server2_x = []
server2_y = []
server3_x = []
server3_y = []

for num_writes in data:

    # for each experiment (1-3)
    for i in range(1, 4):
        server1_x.append(num_writes)
        server2_x.append(num_writes)
        server3_x.append(num_writes)

        server1_time = data[num_writes]["last_read_update"][1][i] - data[num_writes]["write_time"][i]
        server2_time = data[num_writes]["last_read_update"][2][i] - data[num_writes]["write_time"][i]
        server3_time = data[num_writes]["last_read_update"][3][i] - data[num_writes]["write_time"][i]

        # convert to seconds and add to the array
        server1_y.append(server1_time / 1000)
        server2_y.append(server2_time / 1000)
        server3_y.append(server3_time / 1000)

# generate and save the graph

plt.xlabel("Write requests")
plt.ylabel("Time (seconds)")
plt.title("Number of Write Requests Made \nvs Total Time Taken to Receive All Updates")

plt.scatter(server1_x, server1_y, alpha=.5, color="red", label="Hawaii GENI Server")
plt.scatter(server2_x, server2_y, alpha=.5, color="blue", label="Chicago GENI Server")
plt.scatter(server3_x, server3_y, alpha=.5, color="green", label="Texas GENI Server")
plt.legend(loc="lower right")
plt.savefig("graph.png", dpi=300)
print("saved graph to graph.png")
    
