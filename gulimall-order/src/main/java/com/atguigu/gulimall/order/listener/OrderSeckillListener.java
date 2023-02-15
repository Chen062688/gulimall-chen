package com.atguigu.gulimall.order.listener;

import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RabbitListener(queues = {"order.seckill.order.queue"})
@Component
@Slf4j
public class OrderSeckillListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void Listener(SeckillOrderTo entity, Channel channel, Message message) throws IOException {

        try {
            log.info("准备创建秒杀单的详细信息。。。");
            orderService.createSeckillOrder(entity);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }
    
}
