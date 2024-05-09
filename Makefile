
geni1_address = X.X.X.X:X
geni2_address = X.X.X.X:X
geni3_address = X.X.X.X:X

build:
	javac -d bld -cp ".:./zookeeper/lib/*:./src/*" src/*.java

run:
	java -cp ".:./zookeeper/lib/*:./bld" Client localhost:2181

run_geni_1:
	java -cp ".:./zookeeper/lib/*:./bld" Client $(geni1_address)

run_geni_2:
	java -cp ".:./zookeeper/lib/*:./bld" Client $(geni2_address)

run_geni_3:
	java -cp ".:./zookeeper/lib/*:./bld" Client $(geni3_address)

run_geni_all:
	java -cp ".:./zookeeper/lib/*:./bld" Client $(geni1_address),$(geni2_address),$(geni3_address)

run_zk_server_local_standalone:
	./zookeeper/bin/zkServer.sh start-foreground

run_zk_server1_local:
	./zookeeper/bin/zkServer.sh --config ./zookeeper/conf/local_zoo1 start-foreground

run_zk_server2_local:
	./zookeeper/bin/zkServer.sh --config ./zookeeper/conf/local_zoo2 start-foreground

run_zk_server3_local:
	./zookeeper/bin/zkServer.sh --config ./zookeeper/conf/local_zoo3 start-foreground

run_zk_server1_geni:
	./zookeeper/bin/zkServer.sh --config ./zookeeper/conf/geni_zoo1 start-foreground

run_zk_server2_geni:
	./zookeeper/bin/zkServer.sh --config ./zookeeper/conf/geni_zoo2 start-foreground

run_zk_server3_geni:
	./zookeeper/bin/zkServer.sh --config ./zookeeper/conf/geni_zoo3 start-foreground

run_zk_client_local:
	./zookeeper/bin/zkCli.sh -server localhost:2181

run_zk_client_geni:
	./zookeeper/bin/zkCli.sh -server localhost:2181

clean:
	rm -rf bld
#	rm -rf data

clean_data:
	rm -rf data/version-2
	rm -rf data/data1/version-2
	rm -rf data/data2/version-2
	rm -rf data/data3/version-2