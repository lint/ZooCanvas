
import java.util.Arrays;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.Children2Callback;
import org.apache.zookeeper.AsyncCallback.Create2Callback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.ZooDefs.Ids;


public class ZooKeeperMonitor implements Watcher, DataCallback, Create2Callback, StatCallback {

    ZooKeeper zk;
    ZooKeeperMonitorListener listener;
    boolean failed;

    public ZooKeeperMonitor(ZooKeeperMonitorListener listener, String serverInfo) throws KeeperException, IOException {
        // System.out.printf("new ZooKeeperMonitor for server: %s\n", serverInfo);
        this.listener = listener;
        this.zk = new ZooKeeper(serverInfo, 3000, this);
        this.failed = false;
    }

    // interface for other classes to implement so that the ZooKeeperMonitor can send any received results
    public interface ZooKeeperMonitorListener {

        // // handle if a node exists or not
        // void handleExists(String path, boolean exists);

        // handle getting data from a node
        void handleGetDataCallback(String path, byte[] data);

        // // handle getting children from a node
        // void handleGetChildren(String path, List<String> children);

        // handle if the ZooKeeper session is no longer valid
        void handleSessionClose(Code reasonCode);

        // let the listener handle when a watched GetChildren call receives an update
        void handleWatchedGetChildren(String path);

        // let the listener handle when a watched GetData call receives an update
        void handleWatchedGetData(String path);

        // updates the listener whenever a session state update occurred
        void handleSessionStateUpdate(String message);
    }

    /* Zookeeper wrapper methods */

    // synchronous call to ZooKeeper to check if a node exists
    public boolean syncExists(String path, boolean watch) throws KeeperException, InterruptedException {
        // System.out.printf("ZKMonitor: starting sync exists call for path: %s\n", path);
        return zk.exists(path, watch) != null;
    }

    // asynchronous call to ZooKeeper to check if a node exists
    // public void asyncExists(String path, boolean watch) throws KeeperException, InterruptedException {
    //     zk.exists(path, watch, this, null);
    // }

    // synchronous call to ZooKeeper to get the data of a node
    public byte[] syncGetData(String path, boolean watch) throws KeeperException, InterruptedException {
        // System.out.printf("ZKMonitor: starting sync get data call for path: %s\n", path);
        return zk.getData(path, watch, null);
    }

    // asynchronous call to ZooKeeper to get the data of a node
    public void asyncGetData(String path, boolean watch) throws KeeperException, InterruptedException {
        zk.getData(path, watch, this, null);
    }

    // synchronous call to ZooKeeper to get the children of a node
    public List<String> syncGetChildren(String path, boolean watch) throws KeeperException, InterruptedException {
        // System.out.printf("ZKMonitor: starting sync get children call for path: %s\n", path);
        return zk.getChildren(path, watch, null);
    }

    // asynchronous call to ZooKeeper to get the children of a node
    // public void asyncGetChildren(String path, boolean watch) throws KeeperException, InterruptedException {
    //     zk.getChildren(path, watch, this, null);
    // }

    // synchronous call to ZooKeeper to create a new node
    public String syncCreate(String path, boolean watch, byte[] data) throws KeeperException, InterruptedException {
        // System.out.printf("ZKMonitor: starting sync create for path: %s\n", path);
        return zk.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    // asynchronous call to ZooKeeper to create a new node
    public void asyncCreate(String path, boolean watch, byte[] data) throws KeeperException, InterruptedException {
        zk.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, this, null);
    }

    // synchronous call to ZooKeeper to set the data of a node
    public void syncSetData(String path, byte[] data) throws KeeperException, InterruptedException {
        // System.out.printf("ZKMonitor: starting sync setData for path: %s\n", path);
        zk.setData(path, data, -1); // returns stat object, but i don't really need that
    }

    // synchronous call to ZooKeeper to set the data of a node
    public void asyncSetData(String path, byte[] data) throws KeeperException, InterruptedException {
        // System.out.printf("ZKMonitor: starting sync setData for path: %s\n", path);
        // zk.setData(path, data, -1); // returns stat object, but it is not needed
        zk.setData(path, data, -1, this, data);
    } 
    
    /* callback methods */

    // Watcher interface implementation - is called whenever a watched node is updated   
    public void process(WatchedEvent event) {

        // if event EventType == null, the state of the connection changed
        if (event.getType() == Event.EventType.None) {

            KeeperState state = event.getState();

            switch (state) {
            case SyncConnected:
                break;
            case Expired:
                listener.handleSessionStateUpdate(state.toString().trim());
                break;
            case AuthFailed:
                listener.handleSessionStateUpdate(state.toString().trim());
                break;
            default:
                failed = true;
                break;
            }
        } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
            listener.handleWatchedGetChildren(event.getPath());
        } else if (event.getType() == Event.EventType.NodeDataChanged) {
            listener.handleWatchedGetData(event.getPath());
        }
    }

    // DataCallback interface implementation - called when ZooKeeper returns a node's data from an async call
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat)  {
        // try {
        //     System.out.printf("ZKMonitor: received datacallback for path: %s, data: %s\n", path, new String(data, 0, data.length, "ASCII"));
        // } catch (UnsupportedEncodingException e) {
        //     System.out.printf("ZKMonitor: received datacallback for path: %s, data: <could not decode ascii>\n", path);
        // }

        Code reasonCode = Code.get(rc);

        // check the resulting reason code
        switch (reasonCode) {
            case OK:
                break;
            case NONODE:
                data = null;
                break;
            default:
                listener.handleSessionClose(Code.get(rc));
                return;
            }

        listener.handleGetDataCallback(path, data);
    }

    // CreateCallback interface implementation - called when ZooKeeper returns the path of a newly created node from an async call
    public void processResult(int rc, String path, Object ctx, String name, Stat stat) {
        // no need to forward results to the client
    }

    // StatCallback interface implementation - called when ZooKeeper returns a Stat object from an async getData call
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        // no need to forward these results to the client as they are not used
    }

    // // ChildrenCallback interface implementation - called when ZooKeeper returns a node's list of children from an async call
    // public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat) {
    //     System.out.printf("received childrencallback for path: %s, children: %s", path, children);
    // }

    // // StatCallback interface implementation - called when ZooKeeper returns if a node exists from an async call
    // public void processResult(int rc, String path, Object ctx, Stat stat) {
    //     System.out.printf("ZKMonitor: received statcallback for path: %s\n", path);

    //     Code reasonCode = Code.get(rc);
    //     boolean exists;
        
    //     // check the resulting reason code
    //     switch (reasonCode) {
    //     case OK:
    //         exists = true;
    //         break;
    //     case NONODE:
    //         exists = false;
    //         break;
    //     default:
    //         listener.handleSessionClose(rc);
    //         return;
    //     }

    //     listener.handleExists(path, exists);
    // }
}