package edu.hust;

import edu.hust.provider.RpcProvider;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        /**
         * 启动一个可以提供rpc远程调用的server
         * 1 需要一个RpcProvider(nprpc提供)对象
         * 2 向RpcProvider注册方法 UserServiceImpl.login  UserServiceImpl.reg
         * 3 启动RpcProvider这个Server站点 阻塞等待远程rpc方法调用请求
         */
        RpcProvider.Builder builder = RpcProvider.newBuilder();
        RpcProvider provider = builder.build("config.properties");

        /**
         * UserServiceImpl ： 服务对象的名称
         * login reg 服务方法的名称
         */
        provider.registerRpcService(new UserServiceImpl());

        /**
         *启动RpcProvider这个Server站点 阻塞等待远程rpc方法调用请求
         */
        provider.start();

    }
}
