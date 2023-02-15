package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2021000121680234";

    // 商户私钥，您的PKCS8格式RSA2私钥
    public static String merchant_private_key ="MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCOdlSeifCIQZd8RCO1pF1i6zpq479jexIEjLOrgfRbAblARdJ4eNZji8GTcJvcKFVfXrBxxJR+JWnEJzl8vYcFUMkiEW3/AgGRUkvqGxYwF6vrMgoUB0AqZx9yDjZRy5ta0yuBEz2v6D+uYohkRMlr5FIe/I8zerWTUQ6qIV4FGQxGuCJMZ6c2bWRVhoAprGM6buSHvdBmZy+lYkDsGWZGcQ0AD5DM4cnAnh8SjZS32ZrnY8ylR0gQWRYUWPsDDRXYxwseNA/MvbfqXiGQk02OTQjmkZoAARV0666XzNramju+lxBl2zb3CJemjU6uEGVug7WlRKSpn4YNtfWY9zF1AgMBAAECggEAc9KcpXB8UCsToAI+DhWdnyiji/exyI1sbqp+ALhdFAhotmqN3UhQ9QnQzbBF504bargvN6+dEpoUTVeek137gkTV4Y7OIP9eiumfsqtm9J6qNUOvkez7K/4/QJlxbDrfCY6Z1gzwoC1waLTPMsYCBGfSpEKtoirnqOfDxmnuaLFerzzw2e50rrEiNXGFPIYaavjTl24PNcwWy86DCcbhNg5AvNenzxMIgH7tXKWbGwpmMs1SOkytx5q3pLK8cgytcBPTs+Lfmq9HbbDWWxu7fhuSYxZzmQ9neZnrrPhnl6RZIjCeq/z5QBO89VWlML5qp4cjVtZkKhP/ngCWk5RaGQKBgQD0FXUEjujDMM+kKOIQQPX1hfn0F/JG8QivVYFx9koD4jzajuNNqMFi33SG+1enTK88MkjukkpH5XknDk0C21RWkQj3N3s8lRXieJBswec174MLdihHXlpCjdTAsvjveG904z3QhZkr2501xIsSIiHkT+1xJbIcA7yZ27wn+gABgwKBgQCVatBS2x/SzmcAoWVgw7++tx9Hl2MIhbLZLcmxstpuI5gIh+gP8vsXWJbGaz0nnmURIE7bsWH+VKwukBJ6m0bwDZwtZ9Q+IczH4ZikzuSQHIVbvbkxCjSAAivcl8pCbH9Qsux3+0L9Ht/BRz+2xj4SZHJ8+NUyq/pXguTwUbPnpwKBgQCraQYslU/RA85OMofPck9IhTXrvYVfXJYEy7+EELoZ0B98PSxTtVdZ4CMtSAVeb/QACjA9mm6f3v8d9mUOxIiN4nyxYD0jZm9gA9spBWMh9XnJ6siOliw84O2wBf/b7HAZXhxi++99ZVUaFVS0/1NdyuOsuhRCqD0ir7WLPX4TkQJ/Sh+zOWd7YhkDAE0Q/fAeL36pBdsURiYNk7AjXwRbWEhQI+tINKFV7z8DjWul6wGpIdXlSRe2hXV79Z2DjU964Zve3qejoy8haAeCsj/xI40Cm4CDXtCxOs/Y8pZawJkSj/YzWVJM6UVnu3qdIQ83u+gY4+aN3ZVzm/ZTnOTrWwKBgQDz/KocdUmyqe1I8qUFfFRMdQQRvBhFhNaNy3D9W2TbqJAztE0mifER3NGLNF310oeK8kYUIFKLazdud7wr8aDFNnzO2wO7R7WbK1AreUHZlcZ6TPfmAez7EnpFp93dV6h6NkxGEER+/ocq/JuZIg6ceMTPaKTIsan0M9GXl/4OqA==";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public static String alipay_public_key ="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiaLJmh5sTqafzba7bIpyRAc9yZpJTyScKq0yK73SDelfxpOXonQJVfxGI8xGfv7SByNeSoMM62mWgD2WpDkLGum9TIJ+ZgRc34HWdenzBzQLtxF7t1xf3nMs8JTNRyPItzeDMjnQpZLITl4MMIj0XhAzs9cJisXXFtJxxi9vVOtxwoYARbg7zT8ytzX1F3Jl5wl/ofuC9mllf4RxjOVGZgINpHDXIIVa/8We/x8BywCZZC4xCXyqieCJYyq8nz9qCm5VuH5rVFy6qp2sET9zUfAggDhDHHqFDDNViTyEaBy0ftuH3118qH2Q2Xln0x+iAWguzo+rNJXmqZi8T1Od/wIDAQAB";
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url ="https://h5943e8133.goho.co/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url ="http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    private String timout="30m";
    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";
    

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();
        
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();
        
        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);
        
        
        return result;
    }
}
