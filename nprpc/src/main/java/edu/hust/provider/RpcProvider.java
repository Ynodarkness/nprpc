package edu.hust.provider;

import com.google.protobuf.*;
import edu.hust.callback.INotifyProviderCallBack;
import edu.hust.util.ZkClientUtils;
import org.omg.IOP.TAG_JAVA_CODEBASE;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * rpc方法发布的站点    只需要一个站点就可以发布当前主机所以的rpc方法了   用单例模式设计RpcProvider
 */
public class RpcProvider implements INotifyProviderCallBack {

    private static final String SERVER_IP = "ip";
    private static final String SERVER_PORT = "port";
    private static final String ZK_SERVER = "zookeeper";

    private String serverIp;
    private int serverPort;
    private String zkServer;
    //private byte[] responseBuf;   多线程调用notify不合适选择用ThreadLocal
    private ThreadLocal<byte[]> responseBufLocal;



    /**
     * 服务方法的类型信息
     */
    private class ServiceInfo{
        public ServiceInfo(){
            this.service = null;
            this.methodMap = new HashMap<>();    //只读不涉及修改  不会出现线程安全问题  所以可以使用hashmap
        }
        Service service;
        Map<String, Descriptors.MethodDescriptor> methodMap;
    }

    /**
     * 包含所有的rpc服务对象和服务方法
     */
    private Map<String, ServiceInfo> serviceMap;


    /**
     * 启动rpc站点提供服务
     */
    public void start() {
/*       */

        //  todo... 把service 和method都往zookeeper上注册一下
        ZkClientUtils zk = new ZkClientUtils(zkServer);
        serviceMap.forEach((k,v)->{
            String path = "/" + k;
            zk.createPersistent(path, null);//服务对象名字 永久性节点    节点需要一层一层注册
            v.methodMap.forEach((a, b)->{
                String createPath = path + "/" + a;
                zk.createEphemeral(createPath, serverIp+ ":" + serverPort); //方法节点  临时性节点 存储方法所在的地址ip+port
                //给临时性节点添加监听器
                zk.addWatcher(createPath);
                System.out.println("reg zk ->" + createPath);
            });
        });

        System.out.println("rpc server start at:" + serverIp + ":" + serverPort);
        //启动rpc server 网络服务
        RpcServer s = new RpcServer(this);
        s.start(serverIp, serverPort);

    }

    /**
     * 注册rpc服务方法    只要支持rpc方法的类 都实现了com.google.protobuf.Service接口
     * 服务对象名字 服务对象  服务方法名字 服务方法
     * @param service
     */
    public void registerRpcService(Service service) {
        Descriptors.ServiceDescriptor sd = service.getDescriptorForType();
        String serviceName = sd.getName();  //获得服务对象的名称
        ServiceInfo si = new ServiceInfo();
        si.service = service;
        List<Descriptors.MethodDescriptor> methodList = sd.getMethods(); // 获取服务对象的所有服务方法列表
        methodList.forEach(method->{
            //获取服务方法名字
            String methodName = method.getName();
            si.methodMap.put(methodName, method);
        });
        serviceMap.put(serviceName, si);
    }

    /**
     * notify方法是在多线程环境下被用到的
     * 接收rpc网络模块上报的rpc调用相关信息参数  执行具体的rpc方法调用
     * @param serviceName
     * @param methodName
     * @param args
     * @return  把rpc方法调用完成后的响应值进行返回
     */
    @Override
    public byte[] notify(String serviceName, String methodName, byte[] args) {
        ServiceInfo si = serviceMap.get(serviceName);
        Service service = si.service;   //获取服务对象
        Descriptors.MethodDescriptor method = si.methodMap.get(methodName);  //获取服务方法

        //从args中反序列化出method方法的参数  LoginRequest
        Message request = service.getRequestPrototype(method).toBuilder().build();
        try {
            request = request.getParserForType().parseFrom(args);  //反序列化操作
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        /**
         * rpc对象  service
         * rpc对象的方法  method
         * rpc方法的参数 request
         */
        service.callMethod(method, null, request, response -> {
            //responseBuf = message.toByteArray();
            responseBufLocal.set(response.toByteArray());
        });

        return responseBufLocal.get();
    }


    /**
     * 封装RpcProvider对象创建的细节
     */
    public static class Builder{
        private static RpcProvider INSTANCE = new RpcProvider();

        /**
         * 从配置文件中读取rpc server的ip port 给instance初始化数据
         * 通过builder来创建一个RpcProvider对象
         * @return
         */
        public RpcProvider build(String configFile){
            Properties pro = new Properties();
            try {
                pro.load(Builder.class.getClassLoader().getResourceAsStream(configFile));
                INSTANCE.setServerIp(pro.getProperty(SERVER_IP));
                INSTANCE.setServerPort(Integer.parseInt(pro.getProperty(SERVER_PORT)));
                INSTANCE.setZkServer(pro.getProperty(ZK_SERVER));
                return INSTANCE;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 返回一个对象建造器
     * @return
     */
    public static Builder newBuilder(){
        return new Builder();
    }

    private RpcProvider(){
        this.serviceMap = new HashMap<>();
        this.responseBufLocal = new ThreadLocal<>();
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setZkServer(String zkServer) {
        this.zkServer = zkServer;
    }
}
