package com.atguigu.common.to.mq;

import lombok.Data;

import java.util.List;

@Data
public class StockLockedTo {
    
    private Long id; //库存工作单的id
    
    private StockDetailTo detailTo; //工作单所以详情数据
    
    
}
