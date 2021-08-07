package edu.hust.util;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.omg.PortableServer.POA;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;

/**
 * 和zookeeper通信用的辅助工具类 127.0.0.1：8080  192.0.0.1：8080
 */
public class ZkClientUtils {
    private static String rootPath = "/nprpc";
    private ZkClient zkClient;
    private Map<String, String> ephemeral = new HashMap<>();  //存储临时性节点 以及对应的数据  方便watcher后重新创建该节点  因为是单例模式 一个RpcProvider对应一个Zkclient所以不用担心线程安全问题

    /**
     * 通过zk server字符串信息链接server
     * @param serverList
     */
    public ZkClientUtils(String serverList) {
        this.zkClient = new ZkClient(serverList,3000);

        if (!this.zkClient.exists(rootPath)) {  //root节点不存在才创建
            this.zkClient.createPersistent(rootPath, null);
        }

    }

    /**
     * 关闭和zkserver的链接
     */
    public void close(){
        zkClient.close();
    }

    /**
     * zk上创建临时节点
     * @param path
     * @param data
     */
    public void createEphemeral(String path, String data){
        path = rootPath + path;
        ephemeral.put(path, data);
        if (!this.zkClient.exists(path)) {  //zNode节点不存在才创建
            this.zkClient.createEphemeral(path, data);
        }
    }

    /**
     * 创造永久性节点
     * @param path
     * @param data
     */
    public void createPersistent(String path, String data){
        path = rootPath + path;
        if (!this.zkClient.exists(path)) {  //zNode节点不存在才创建
            this.zkClient.createPersistent(path, data);
        }
    }

    /**
     * 读取zNode节点的值
     * @param path
     * @return
     */
    public String readData(String path){
        return this.zkClient.readData(rootPath+path, null);
    }

    /**
     * 给zk上指定的zNode添加watcher监听
     * @param path
     */
    public void addWatcher(String path){
        this.zkClient.subscribeDataChanges(rootPath + path, new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {

            }

            /**
             * 设置zNode节点监听  因为如果zkClient断掉 由于ZkServer无法及时获知zkClient的关闭状态
             * 所以zkServer会等待session timeout时间以后，会把zkClient创建的临时节点 全部删除掉
             * 但是在session timeout时间内又启动了同样的zkClient，（因为之前创建的还没超时，故无法创建新的临时节点，此时存在的仍然是旧的）
             * 那么等待session timeout超时后，原先创建的临时节点都没了
             * @param path
             * @throws Exception
             */
            @Override
            public void handleDataDeleted(String path) throws Exception {
                System.out.println("watcher -> handleDataDeleted : " + path);
                //把删除掉的zNode临时性节点重新创建一下
                String str = ephemeral.get(path);   // /nprpc/UserServiceRpc/login
                if (str != null) {
                    zkClient.createEphemeral(path, str);   //data为存储的ip+port
                }
            }
        });
    }

    public static String getRootPath() {
        return rootPath;
    }

    public static void setRootPath(String rootPath) {
        ZkClientUtils.rootPath = rootPath;
    }

    /**
     * 测试该工具类
     * @param args
     */
    public static void main(String[] args) {
        ZkClientUtils zk = new ZkClientUtils("127.0.0.1:2181");
        zk.createPersistent("/ProductService", "123456");
        System.out.println(zk.readData("/ProductService"));
        zk.close();
    }
}
