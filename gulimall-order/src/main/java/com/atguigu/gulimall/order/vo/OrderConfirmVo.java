package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页需要用的数据
 */
public class OrderConfirmVo {
    //收获地址信息
    @Setter @Getter
    List<MemberAddressVo>address;
    
    //所有选中的购物项
    @Setter @Getter
    List<OrderItemVo> items;
    
    //发票记录...
    
    //优惠卷信息...
    @Setter @Getter
    private Integer integration;
    
    @Setter @Getter
    Map<Long,Boolean>stocks;
    //订单总额
    //private BigDecimal total;

    /**
     * 放重令牌
     */
    @Setter @Getter
    private String orderToken;
    
    public Integer getCount(){
        Integer count = 0;
        if(items!=null){
            for (OrderItemVo item : items) {
                count+=item.getCount();
            }
        }
        return count;
    }
    public BigDecimal getTotal() {
        BigDecimal sum=new BigDecimal("0");
        if(items!=null){
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
              sum =sum.add(multiply);
            }
        }
      
        return sum;
    }

    //应付价格
   // private BigDecimal payPrice;

    public BigDecimal getPayPrice() {
        
        return getTotal();
    }
}
