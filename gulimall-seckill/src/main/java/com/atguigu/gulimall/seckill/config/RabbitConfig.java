package com.atguigu.gulimall.seckill.config;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class RabbitConfig {
    

    /**
     * 使用JSON序列化机制,进行消息转换
     *
     * @return
     */
    @Bean
    public MessageConverter messageConverter() {

        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate
     * 1.服务器收到消息就回调
     * 1）.spring.rabbitmq.publisher-confirm-type=correlated
     * 2）.调用回调
     * 2.消息正确抵达队列进行回调
     * 1）spring.rabbitmq.publisher-returns=true
     * ）spring.rabbitmq.template.mandatory=true
     * 2、设置确认回调ReturnsCallback
     * 3.消费端确认(保证每一个消息被正确消费,此时才可以broker删除这个消息)
     * 1.默认是自动确认的,只要消息接收到,客户端会自动确认,服务端就会移除这个消息
     * 问题:
     * 我们收到很多消息,自动回复给服务器ack,只有一个消息处理成功!,宕机了。发送消息丢失
     * 解决:手动确认模式。只要我们没有明确告诉MQ,货物被签收。没有Ack,
     * 消息就一直是unacked状态。即使Consumer宕机。消息也不会丢失,会重新变为Ready,
     * 下一次有新的Consumer连接进来就发给它。
     * 2.如何签收:
     * 1)   channel.basicAck(deliveryTag,false);签收 业务成功完成就应该签收
     * 1)    channel.basicNack(deliveryTag,false,true); 拒签 业务失败或者宕机就应该拒签
     */
 
}
