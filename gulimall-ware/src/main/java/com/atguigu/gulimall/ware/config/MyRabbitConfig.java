package com.atguigu.gulimall.ware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public  class   MyRabbitConfig {
    
    
    /**
     * 使用JSON序列化机制,进行消息转换
     * @return
     */
    @Bean
    public MessageConverter messageConverter(){

        return new Jackson2JsonMessageConverter();
    }
    
    
    @Bean
    public TopicExchange stockEventExchage(){
        /**
         * String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
         */
        TopicExchange topicExchange = new TopicExchange("stock-event-exchange", true, false);
        return topicExchange;
    }
    
    @Bean
    public Queue stockReleaseStockQueue(){

        Queue queue = new Queue("stock-release-stock-queue", true, false, false);
        return queue;
    }
    @Bean
    public Queue stockDelayQueue(){
        Map<String,Object> arguments =new HashMap<>();
        /**
         * x-dead-letter-exchange: order-event-exchange 
         * x-dead-letter-routing-key: order.release.order  
         * x-message-ttl: 60000
         */
        arguments.put("x-dead-letter-exchange","stock-event-exchange");
        arguments.put("x-dead-letter-routing-key","stock.release");
        arguments.put("x-message-ttl",120000);
        Queue queue = new Queue("stock-delay-queue", true, false, false,arguments);
        return queue;
    }
    
    @Bean
    public Binding StockReleaseBinding(){

        Binding binding = new Binding("stock-release-stock-queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange", 
                "stock.release.#", null);
        return binding;
    }
    @Bean
    public Binding stockLockedBinding(){

        Binding binding = new Binding("stock-delay-queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked", null);
        return binding;
    }
    
}
