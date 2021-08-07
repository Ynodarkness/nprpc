package edu.hust;

import static org.junit.Assert.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * 测试protobuf的序列和反序列化
     */
    @Test
    public void test1(){
        TestProto.LoginRequest.Builder loginBuilder = TestProto.LoginRequest.newBuilder();
        loginBuilder.setName("zhang san");
        loginBuilder.setPwd("1123434");

        TestProto.LoginRequest request = loginBuilder.build();
        System.out.println(request.getName());
        System.out.println(request.getPwd());

        /**
         * 把loginRequest对象序列化成字节流 通过网络发送出去
         * 此处的sendbuf就可以通过网络发送出去
         */
        byte[] sendbuf = request.toByteArray();


        /**
         * Protobuf从字节流反序列化生成LoginRequest对象
         */
        try {
            TestProto.LoginRequest r = TestProto.LoginRequest.parseFrom(sendbuf);
            System.out.println(r.getName());
            System.out.println(r.getPwd());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试properties的加载
     */
    @Test
    public void test2(){
        Properties pro = new Properties();  //key value
        try {
            pro.load(AppTest.class.getClassLoader().getResourceAsStream("config.properties"));
            System.out.println(pro.getProperty("IP"));
            System.out.println(pro.getProperty("PORT"));
            System.out.println(pro.getProperty("ZOOKEEPER"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
