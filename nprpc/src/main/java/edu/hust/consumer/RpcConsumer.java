package edu.hust.consumer;

import com.google.protobuf.*;
import edu.hust.RpcMetaProto;
import edu.hust.provider.RpcProvider;
import edu.hust.util.ZkClientUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

public class RpcConsumer implements RpcChannel {

    private static final String ZK_SERVER = "zookeeper";
    private String zkServer;

    public RpcConsumer(String file) {
        Properties pro = new Properties();
        try {
            pro.load(RpcProvider.Builder.class.getClassLoader().getResourceAsStream(file));
            this.zkServer = pro.getProperty(ZK_SERVER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * stub代理对象需要接收一个实现了RpcChannel的对象  当用stub调用任意方法的时候 全部都是调用了当前这个RpcChannel的callMethod方法
     * @param methodDescriptor
     * @param rpcController
     * @param message
     * @param message1
     * @param rpcCallback
     */
    @Override
    public void callMethod(Descriptors.MethodDescriptor methodDescriptor,
                           RpcController rpcController,
                           Message message, //request
                           Message message1, //response
                           RpcCallback<Message> rpcCallback) {
        /**
         * 打包参数 递交网络发送
         * rpc调用参数格式：header_size + service_name + method_name + args
         */
        Descriptors.ServiceDescriptor sd = methodDescriptor.getService();
        String serviceName = sd.getName();
        String methodName = methodDescriptor.getName();

        // todo... 在zookeeper上查询serviceName methodName在哪台主机上 ip port
        String ip = "";
        int port = 0;
        ZkClientUtils zk = new ZkClientUtils(zkServer);
        String path = "/" + serviceName + "/" + methodName;
        String hostStr = zk.readData(path);
        zk.close();
        if (hostStr == null){
            rpcController.setFailed("read path:" + path + "data from zk is failed");
            rpcCallback.run(message1);
            return;
        }else {
            String[] host = hostStr.split(":");
            ip = host[0];
            port = Integer.parseInt(host[1]);
        }

        //序列化头部信息
        RpcMetaProto.RpcMeta.Builder metaBuilder = RpcMetaProto.RpcMeta.newBuilder();
        metaBuilder.setServiceName(serviceName);
        metaBuilder.setMethodName(methodName);
        byte[] metaBuf = metaBuilder.build().toByteArray();

        //参数
        byte[] argBuf = message.toByteArray();

        //组织rpc参数信息
        ByteBuf buf = Unpooled.buffer(4 + metaBuf.length + argBuf.length);
        buf.writeInt(metaBuf.length);
        buf.writeBytes(metaBuf);
        buf.writeBytes(argBuf);

        //待发送的数据
        byte[] sendBuf = buf.array();

        //通过网络发送rpc调用请求信息
        Socket client = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            client = new Socket();
            client.connect(new InetSocketAddress(ip, port));
            out = client.getOutputStream();
            in = client.getInputStream();

            //发送数据
            out.write(sendBuf);
            out.flush();

            //wait等待rpc调用响应返回
            ByteArrayOutputStream recvBuf = new ByteArrayOutputStream();
            byte[] rBuf = new byte[1024];
            int size = in.read(rBuf);
            /**
             * 这里的size可能是0   因为RpcProvider封装的Response响应参数的时候 如果响应参数的成员变量的值都是默认值
             * 实际上 RpcProvider递给RpcServer就是一个空数据
             */
            if (size > 0){
                recvBuf.write(rBuf, 0, size);
                rpcCallback.run(message1.getParserForType().parseFrom(recvBuf.toByteArray()));
            }else {
                 //rpcCallback.run(message1.getParserForType().parseFrom(new byte[0]));//或者
                 rpcCallback.run(message1);

            }
        } catch (IOException e) {
            rpcController.setFailed("server connect error, check server!");
            rpcCallback.run(message1);
        } finally {
            try {
                if (out != null){
                    out.close();
                }
                if (in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (client != null){
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
