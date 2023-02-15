package com.atguigu.gulimall.product.fegin;

import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoudsTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient("gulimall-coupon")
public interface CouponFeginService {

    /**
     * 1.CouponFeginService.saveSpuBouds(spuBoudsTo);
     * 1)、RequestBody 将这个对象转为json
     * 2)、找到gulimall-coupon服务,给coupon/spubounds/save发送请求 
     *  将上一步转的json放在请求体位置,发送请求
     *  3)、对方服务收到请求。请求体里有json数据
     *      (@RequestBody SpuBoudsEntity spuBouds);将请求体的json转为SSpuBoudsEntity
     *     只要json数据数据模型是兼容的.双方服务无需使用同一个To
     * @param spuBoudsTo
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBouds(@RequestBody SpuBoudsTo spuBoudsTo);
    
    
    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
