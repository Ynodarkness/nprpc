# nprpc framework

项目基于netty高性能网络库+Protobuf开发，所以命名项目为nprpc 。

## 架构

![image-20210807154549261](C:\Users\yue\AppData\Roaming\Typora\typora-user-images\image-20210807154549261.png)

消费者调用提供者的方式取决于消费者的客户端选择，如选用原生 Socket 则该步调用使用 BIO，如选用 Netty 方式则该步调用使用 NIO。本项目中，消费者使用原生的Socket，服务提供方使用Netty方式提供服务。

## 特性

- 实现了基于 Java 原生 Socket 传输与 Netty 传输两种网络传输方式
- 采用Google Protobuf 方式实现序列化方式，使用了其提供的代码生成机制，性能好/效率高
- 使用 ZooKeeper 作为注册中心，管理服务提供者信息
- 配合protobuf采用极简的传输协议。
- 服务提供侧自动注册服务
- 使用ThreadLocal处理多线程调用方法时的安全性问题
- 利用事件回调机制完成 RpcServer给RpcProvider上报接收到的rpc服务调用相关信息
- 使用ZkClient的watcher机制，设置zNode节点监听，实现数据的发布/订阅功能

## 项目模块概览

- **nprpc**	——	主框架部分，封装调用细节，主要包括架构图中1、2、3实现细节
- **RpcConsumer**	——	服务消费方，基于protobuf，模拟rpc方法调用者
- **RpcProvider**	——	服务提供方，基于protobuf，模拟rpc方法提供者，此模块启动后阻塞等待远程rpc方法调用请求

## 传输协议(npr协议)

调用参数与返回值的传输采用了相同的参数格式，即：  

```
header_size + service_name + method_name + args
```

| 字段         | 解释                           |
| :----------- | :----------------------------- |
| header_size  | 头部的长度   4字节 一个int大小 |
| service_name | 服务对象名字                   |
| method_name  | 服务方法名字                   |
| args         | 方法参数                       |

## 使用

### 在服务提供侧使用proto文件自动代码

```java
syntax = "proto3";
package edu.hust;
option java_outer_classname = "UserServiceProto";
option java_generic_services = true;   //根据下面定义的service类  生成rpc类和方法代理
message LoginRequest {
    string name = 1;
    string pwd = 2;
}
message RegRequest {
    string name = 1;
    string pwd = 2;
    int32 age = 3;
    enum SEX {
        MAN = 0;
        WOMAN = 1;
    }
    SEX sex = 4;
    string phone = 5;
}
message Response {
    int32 errno = 1;   //错误码
    string errinfo = 2;  //错误信息
    bool result = 3;   //rpc调用的返回信息
}
// 定义RPC服务接口类和服务方法
service UserServiceRpc{
    rpc login(LoginRequest) returns (Response);
    rpc reg(RegRequest) returns (Response);
}
```

### 将本地方法发布成RPC方法

```java
package edu.hust;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * 描述：原来是本地服务方法   现在要发布成RPC方法
 */
public class UserServiceImpl extends UserServiceProto.UserServiceRpc {

    /**
     * 登录业务
     * @param name
     * @param pwd
     * @return
     */
    public boolean login(String name, String pwd){
        System.out.println("call UserService -> login");
        System.out.println("name:" + name);
        System.out.println("pwd:" + pwd);
        return true;
    }

    /**
     * 注册业务
     * @param name
     * @param pwd
     * @param age
     * @param sex
     * @param phone
     * @return
     */
    public boolean reg(String name, String pwd, int age, Enum sex, String phone){
        System.out.println("call UserService -> reg");
        System.out.println("name:" + name);
        System.out.println("pwd:" + pwd);
        System.out.println("age:" + age);
        System.out.println("sex:" + sex);
        System.out.println("phone:" + phone);

        return true;
    }


    /**
     * reg的rpc代理方法
     * @param controller   可以接收方法执行状态
     * @param request
     * @param done
     */
    @Override
    public void login(RpcController controller, UserServiceProto.LoginRequest request, RpcCallback<UserServiceProto.Response> done) {
        //1  从request里面读取到远程rpc调用请求的参数了
        String name = request.getName();
        String pwd = request.getPwd();

        //2根据解析的参数 做本地业务
        boolean result = login(name, pwd);

        //3 填写方法的返回值
        UserServiceProto.Response.Builder responseBuilder = UserServiceProto.Response.newBuilder();
        responseBuilder.setErrno(0);
        responseBuilder.setErrinfo("");
        responseBuilder.setResult(result);

        //4 把response对象传递到nprpc框架  由框架负责发送rpc调用响应值
        done.run(responseBuilder.build());
    }

    /**
     * reg的代理方法
     * @param controller
     * @param request
     * @param done
     */
    @Override
    public void reg(RpcController controller, UserServiceProto.RegRequest request, RpcCallback<UserServiceProto.Response> done) {
        String name = request.getName();
        String pwd = request.getPwd();
        int age = request.getAge();
        Enum sex = request.getSex();
        String phone = request.getPhone();

        boolean result = reg(name, pwd, age, sex, phone);

        UserServiceProto.Response.Builder responseBuilder = UserServiceProto.Response.newBuilder();
        responseBuilder.setErrno(0);
        responseBuilder.setErrinfo("");
        responseBuilder.setResult(result);

        done.run(responseBuilder.build());
    }
}
```

### 编写服务提供者

```java
package edu.hust;

import edu.hust.provider.RpcProvider;

/**
 * 模拟rpc提供方
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

```

### 在服务消费侧远程调用

```java
package edu.hust;

import edu.hust.consumer.RpcConsumer;
import edu.hust.controller.NrpcController;

/**
 *模拟rpc消费方
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

```

注：这里消费方仍然需要和服务提供方相同的proto文件生成的代码，利用该代码中的Stub对象完成远程调用。

### 启动

在此之前请确保 Zookeeper 运行在本地 `2181` 端口。

首先启动服务提供者，在服务提供方会输出`rpc server start at:127.0.0.1:6000`。

然后启动消费者，在消费方会输出`receive rpc call response!  true`。