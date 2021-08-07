package edu.hust;

import edu.hust.consumer.RpcConsumer;
import edu.hust.controller.NrpcController;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        /**
         * 模拟rpc方法调用者
         */
        UserServiceProto.UserServiceRpc.Stub stub = UserServiceProto.UserServiceRpc.newStub(new RpcConsumer("config.properties"));

        UserServiceProto.LoginRequest.Builder loginBuilder = UserServiceProto.LoginRequest.newBuilder();
        loginBuilder.setName("zhangsan");
        loginBuilder.setPwd("123456");

        NrpcController con = new NrpcController();

        stub.login(con, loginBuilder.build(), response -> {
            /**
             * rpc调用完成后的返回值
             */
            if (con.failed()){   //rpc方法没有调用成功
                System.out.println(con.errorText());
            }else {
                System.out.println("receive rpc call response!");
                if (response.getErrno() == 0) {
                    System.out.println(response.getResult());
                } else {
                    System.out.println(response.getErrinfo());
                }
            }
        });
    }
}
