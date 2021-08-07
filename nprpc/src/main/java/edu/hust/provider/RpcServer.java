package edu.hust.provider;

import edu.hust.RpcMetaProto;
import edu.hust.callback.INotifyProviderCallBack;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.ref.ReferenceQueue;

/**
 * Rpc服务器端  使用netty
 */
public class RpcServer {

    private INotifyProviderCallBack notifyProvider;

    public RpcServer(INotifyProviderCallBack notify){
        this.notifyProvider = notify;
    }

    public void start(String ip, int port){
        //创建主事件循环  对应I/O线程 主要用来处理新用户的连接事件
        EventLoopGroup mainGroup = new NioEventLoopGroup(1);
        //创建worker工作线程事件循环  处理已连接用户的可读写事件
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);

        //netty网络服务的启动辅助类
        ServerBootstrap b = new ServerBootstrap();

        b.group(mainGroup, workerGroup)
                .channel(NioServerSocketChannel.class) //底层使用Java NIO Selector模型
                .option(ChannelOption.SO_BACKLOG, 1024)  //设置TCP参数
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        /**
                         *设置数据的编码和解码器   网络的字节流  <-> 业务要处理的数据类型   解码我们就需要字节流即可 利用protobuf进行解码
                         * 设置具体的处理器回调
                         */
                        channel.pipeline().addLast(new ObjectEncoder());   //编码
                        channel.pipeline().addLast(new RpcServerChannel()); //设置事件回调处理器
                    }
                });   //注册事件回调 把业务层代码和网络层代码完全分开

        try {
            //阻塞 开启网络服务
            ChannelFuture f = b.bind(ip, port).sync();

            //关闭网络服务
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            mainGroup.shutdownGracefully();
        }
    }

    /**
     * 继承自netty的ChannelInboundHandlerAdapter适配器类   主要提供相应的回调操作
     */
    private class RpcServerChannel extends ChannelInboundHandlerAdapter{
        /**
         * 处理接收到的事件
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //
            /**
             * 类似java nio的ByteBuff
             * request 就是远程发送过来的rpc调用请求包含的所有信息参数
             *  header_size + UserServiceRpcloginzhangsan123456(Login)
             *  20 + UserServiceRpclogin + 参数
             */
            ByteBuf request = (ByteBuf)msg;

            //1 先读取头部信息的长度
            int headSize = request.readInt();

            //2 读取头部信息（服务对象名称和服务方法名称）
            byte[] metaBuf = new byte[headSize];
            request.readBytes(metaBuf);

            //3 反序列化 生成RpcMeta
            RpcMetaProto.RpcMeta rpcMeta = RpcMetaProto.RpcMeta.parseFrom(metaBuf);
            String serviceName = rpcMeta.getServiceName();
            String methodName = rpcMeta.getMethodName();

            //4 读取rpc方法的参数
            byte[] argBuf = new byte[request.readableBytes()];
            request.readBytes(argBuf);

            //5 serviceName methodName argBuf
            byte[] response = notifyProvider.notify(serviceName, methodName, argBuf);

            //6 把rpc方法调用的响应response通过网络发给rpc调用方
            ByteBuf buf = Unpooled.buffer(response.length);   //开辟内存空间
            buf.writeBytes(response);
            ChannelFuture f = ctx.writeAndFlush(buf);
            //7  模拟http响应完成后 直接关闭链接
            if (f.sync().isSuccess()){
                ctx.close();
            }

        }

        /**
         * 连接异常处理
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}
