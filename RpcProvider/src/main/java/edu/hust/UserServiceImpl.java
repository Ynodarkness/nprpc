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
