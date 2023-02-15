package com.atguigu.gulimall.order.config;


import com.atguigu.gulimall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyMQConfig {
    
    //@Bean Binding Queue Exchange
    
    
 
    /**
     * 容器中的 Binding Queue Exchange 都会自动创建(前提RabbitMQ没有的情况)
     * 一旦创建好队列以后  @Bean属性发送变化也不会覆盖掉
     * @return
     */
    @Bean
    public Queue orderDelayQueue(){
        Map<String,Object>arguments =new HashMap<>();
        /**
         * x-dead-letter-exchange: order-event-exchange 
         * x-dead-letter-routing-key: order.release.order  
         * x-message-ttl: 60000
         */
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);
        Queue queue = new Queue("order.delay.queue", true, false, false,arguments);
       
            
        return queue;
    }
    
    @Bean
    public Queue orderReleaseOrderQueue(){
        Queue queue = new Queue("order.release.order.queue", true, false, false);
    return queue;
    }
    
    @Bean
    public Exchange OrderEventExchange(){
        //String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        TopicExchange topicExchange = new TopicExchange("order-event-exchange", true, false);
        return topicExchange;
    }

    @Bean
    public Binding OrderCreateOrderBinding (){
        //String destination, DestinationType destinationType, String exchange, String routingKey, @Nullable Map<String, Object> arguments
        Binding binding = new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange", "order.create.order", null);
        return binding;
    }
    
    @Bean
    public Binding OrderReleaseOrderBinding (){
        Binding binding = new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange", "order.release.order", null);
        return binding;
    }

    /**
     * 订单释放直接和库存释放进行绑定
     * @return
     */
    @Bean
    public Binding OrderReleaseOtherBinding (){
        Binding binding = new Binding("stock-release-stock-queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange", "order.release.other.#", null);
        return binding;
    }

    @Bean
    public Queue orderSeckillOrderQueue(){
        Queue queue = new Queue("order.seckill.order.queue", true, false, false);
        return queue;
    }
    
    @Bean
    public  Binding orderBIndingSeckillQueue(){
        return new Binding("order.seckill.order.queue", Binding.DestinationType.QUEUE,
                "order-event-exchange","order.seckill.order",null);
    }
}
