package edu.hust;

import javax.annotation.PreDestroy;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

/**
 * 事件回调操作
 * 描述：模拟界面类  接收用户发起的事件   处理完成 显示结果
 * 需求：
 * 1. 下载完成后 需要显示信息
 * 2. 下载过程中  需要显示下载进度
 *
 * 代码某处： 该干什么使其了  以及这个事情该怎么做    在此处调用一个函数就可以完成了
 */
public class GuiTestCase implements INotifyCallBack{
    private Download download;

    public GuiTestCase() {
        this.download = new Download(this);
    }


    public void downloadFile(String file){
        System.out.println("begin start download file" + file);
        download.start(file);
    }

    /**
     * 显示下载进度
     * @param file
     * @param progress
     */
    public void progress(String file, int progress){
        System.out.println("download file" + file + "progress " + progress + "%");
    }

    /**
     * 显示文件下载完成
     * @param file
     */
    public void result(String file){
        System.out.println("download file " + file + "over.");
    }

    public static void main(String[] args) {
        GuiTestCase gui = new GuiTestCase();
        gui.downloadFile("开始啦哈哈哈哈哈哈哈");
    }
}


//我依赖你 你依赖我  采用事件回调
//把需要上报的事件都定义在接口中
interface INotifyCallBack{
    void progress(String file, int progress);
    void result(String file);

}


/**
 * 负责下载内容的类
 */
class Download{

    //private GuiTestCase gui;    //没有做到面向接口编程
    private INotifyCallBack cb;    //做到面向接口编程

    public Download(INotifyCallBack cb) {
        this.cb = cb;
    }

    /**
     * 底层执行下载任务的类
     * @param file
     */
    public void start(String file){
        int count = 0;


        try {
            while (count <= 100) {
                cb.progress(file, count);   //上报下载进度
                Thread.sleep(1000);
                count += 20;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cb.result(file);  //上报下载完成

    }
}