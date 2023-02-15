package com.atguigu.gulimall.order.controller;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
@Slf4j
public class RabbitController {
    @Autowired
    RabbitTemplate rabbitTemplate;
    
    @GetMapping("/sendMq")
    public String sendMq(@RequestParam(value = "num",defaultValue = "10") Integer num) {
        OrderReturnReasonEntity entity = new OrderReturnReasonEntity();
        //1.发送消息是个对象的话,我们会使用序列化机制,将对象写出去,对象必须实现Serializable
        for (int i = 0; i < num; i++) {
            if (i % 2 == 0) {
                entity.setId(1L);
                entity.setName("张三" + i);
                entity.setCreateTime(new Date());
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", entity,new CorrelationData(UUID.randomUUID().toString()));
                log.info("消息发送出去了{}");
            } else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                orderEntity.setReceiverName("李四" + i);
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello22.java", orderEntity,new CorrelationData(UUID.randomUUID().toString()));
                log.info("消息发送出去了{}");

            }
        }
        return "ok";
    }
}
