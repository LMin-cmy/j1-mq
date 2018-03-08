package com.j1.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.alibaba.rocketmq.common.message.MessageExt;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

import static com.sun.tools.javac.jvm.ByteCodes.ret;


/**
 * Created by tianqi on 16/12/6.
 */
public class ConsumerHrtMQ {
    //private static Logger logger = new Logger();
    static  Properties props = new Properties();

    public String reciver(){
        final String[] result = {""};
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(getProps("hrt.mq.pushconsumer"));
       
        consumer.setNamesrvAddr(getProps("hrt.mq.nameseraddr"));
        consumer.setInstanceName(getProps("hrt.mq.instancename"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        try {
            consumer.subscribe(getProps("hrt.mq.topic"), getProps("hrt.mq.tag")); //接收健一的所有消息队列
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                                ConsumeConcurrentlyContext context) {
                    //接收消息
                    result[0] =     process(msgs);
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });
            consumer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        System.out.println("Consumer Started.");
        return result[0];
    }
    public String process(List<MessageExt> msgs){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String method = "";
        String message = null;
        for(MessageExt msg : msgs){
            message = new String(msg.getBody());
            JSONObject jsonObject = JSON.parseObject(message);
            String sysId = jsonObject.get("Sys_ID").toString();
            if(!getProps("hrt.sys_id").equals(sysId)){
                System.out.println(sysId + "不属于健一的系统编码");
                break;
            }else{
                if (msg.getTopic().equals(getProps("hrt.mq.topic"))) {
                    // 执行Topic-J1的消费逻辑
                    if (msg.getTags() != null){
                        if (msg.getTags().equals(getProps("hrt.mq.tagA008"))) { //接收会员信息更新消息
                            System.out.println(sdf.format(msg.getBornTimestamp()) + "+++++会员信息更新--消息队列：+++++" + message);
                            // 回调查询会员信息接口
                            method = getProps("hrt.callback.method.detail");
                        }else if(msg.getTags().equals(getProps("hrt.mq.tagA018"))) { //接收手机号码变更消息
                            System.out.println(sdf.format(msg.getBornTimestamp()) + "+++++会员手机变更--消息队列：+++++" + message);
                            method = getProps("hrt.callback.method.updateMobile");
                        }else if (msg.getTags().equals(getProps("hrt.mq.tagA025"))) { //接收账户归并消息
                            System.out.println(sdf.format(msg.getBornTimestamp()) + "+++++会员账户归并--消息队列：+++++" + message);
                            // 回调查询会员归并信息接口
                            method = getProps("hrt.callback.method.merge");
                        } else if (msg.getTags().equals(getProps("hrt.mq.tagA027"))) { //接收会员状态变更消息
                            System.out.println(sdf.format(msg.getBornTimestamp()) + "+++++会员状态变更--消息队列：+++++" + message);
                            method = getProps("hrt.callback.method.updateStatus");
                        }
                        //request(method,message);
                    }
                }
            }

        }
        return message;
    }
    public String request(String method,String message){
        // 请求url
        StringBuffer url = new StringBuffer(getProps("hrt.callback.url"));
        url.append(method);
        System.out.println(url);
        HttpPost httpPost = new HttpPost(url.toString());
        CloseableHttpClient httpclient = HttpClients.createDefault();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message",message);
        StringEntity se = new StringEntity(jsonObject.toString(), Charset.forName("UTF-8"));
        se.setContentType("application/json; charset=UTF-8");
        httpPost.setHeader("Accept","application/json");
        httpPost.setEntity(se);
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 解析返结果
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String resStr = null;
            try {
                resStr = EntityUtils.toString(entity, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(resStr);
            return resStr;
        }else
            return null;
    }
    private static void loadConf(String path) throws Exception {
        InputStream in = new FileInputStream(path);
        props.load(in);
        in.close();
    }

    private static String getProps(String key){
        return props.getProperty(key);
    }

    public static void main(String[] args) {
        try {
            loadConf(args[0]);
            new ConsumerHrtMQ().reciver();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
