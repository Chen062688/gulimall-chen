package com.atguigu.gulimall.order.service.impl;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListeners;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.service.OrderItemService;


@Service("orderItemService")
@RabbitListener(queues = "hello-java-queue")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );
        return new PageUtils(page);
    }

    /**
     * queues:声明需要监听的所有队列
     * @param msg
     * Channel channel:当前传输数据的通道
     * 
     * Queue：可以很多人都来监听。只要接收到消息,队列就会删除消息,而且只能有一个人收到此消息
     * 场景:
     *  1) 订单服务多个;同一个消息,只有一个客户端收到
     *  2) 只有一个消息完全处理完,方法运行结束,我们就可以接收到下一个消息
     */
    /*@RabbitListener(queues = "hello-java-queue")*/
    @RabbitHandler
    public void reciveMessage(Message msg
            , OrderReturnReasonEntity reasonEntity, 
                              Channel channel)  {
        System.out.println("接收到的消息"+reasonEntity);
        //Thread.sleep(3000);
            System.out.println("消息处理完成=>"+reasonEntity.getName());
          
        //change内按顺序自增的。
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();
        System.out.println("deliveryTag=>"+deliveryTag);
        //签收货物,非批量模式
        try {
            if(deliveryTag%2==0){
                //收获
                channel.basicAck(deliveryTag,false);
                System.out.println("签收了货物..."+deliveryTag);
            }else {
                /**requeue=false 丢弃
                 * requeue=true 发回服务器,服务器重新入队
                 */
                //退货long deliveryTag, boolean multiple, boolean 
                channel.basicNack(deliveryTag,false,true);
                System.out.println("没有签收货物..."+deliveryTag);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @RabbitHandler
    public void reciveMessage2(OrderEntity orderEntity) throws InterruptedException {
  
        System.out.println("消息处理完成=>"+orderEntity);

    }

}