package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Slf4j
class GulimallOrderApplicationTests {
    
    @Autowired
    AmqpAdmin amqpAdmin;
    
    @Autowired
    RabbitTemplate rabbitTemplate;
    /**
     * 1.如何创建 Exchange、Queue 、Binding
     * 1)、使用AmqpAdmin进行创建
     * 2.如何收发消息
     */
    @Test
    void createExchange() {
        DirectExchange directExchange = new DirectExchange("hello-java-exchange",true,false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功!"+"hello-java-exchange");
    }

    @Test
    void createQueue() {
        Queue queue = new Queue("hello-java-queue",true,false,false);
        amqpAdmin.declareQueue(queue);
        log.info("QUEUE[{}]创建成功!"+"hello-java-queue");
    }
    @Test
    void createBinding() {
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE
                ,"hello-java-exchange"
                ,"hello.java",null );
        amqpAdmin.declareBinding(binding);
    }

    @Test
    void sendMessageTest() {
        OrderReturnReasonEntity entity = new OrderReturnReasonEntity();
        //1.发送消息是个对象的话,我们会使用序列化机制,将对象写出去,对象必须实现Serializable
        for (int i=0;i<10;i++){
            if(i%2==0){
                entity.setId(1L);
                entity.setName("张三"+i);
                entity.setCreateTime(new Date());
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",entity);
                log.info("消息发送出去了{}");
            }else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                orderEntity.setReceiverName("李四"+i);
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",orderEntity);
                log.info("消息发送出去了{}");
            }
           
        }
        //2.发送的对象类型的消息,可以是一个json
        

    }
}
