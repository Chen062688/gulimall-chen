package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import net.minidev.json.JSONArray;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;

@SpringBootTest
public  class GulimallSearchApplicationTests {
    @Autowired
    private RestHighLevelClient client;
    /**
     * Copyright 2022 json.cn 
     */

    /**
     * Auto-generated: 2022-09-26 15:37:43
     *
     * @author json.cn (i@json.cn)
     * @website http://www.json.cn/java2pojo/
     */
    @Data
    static class Accout {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }

    @Test
    void searchData() throws IOException {

        SearchRequest searchRequest = new SearchRequest("bank");
        //指定检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("address","mill"))
                        //安装年龄值分布进行聚合
                        .aggregation(AggregationBuilders.terms("ageAgg").field("age").size(10)
                         //计算平均薪资
                                );
        AvgAggregationBuilder blanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(blanceAvg);
        searchRequest.source(sourceBuilder);
        System.out.println("检索条件"+sourceBuilder.toString());
        SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //分析结果
        //3.1获取所有查到的结果
        SearchHits searchHits = response.getHits();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String string = hit.getSourceAsString();
            Accout accout = JSON.parseObject(string, Accout.class);
            System.out.println("accout:"+accout.toString());
        }
        Aggregations aggregations = response.getAggregations();
        Terms ageAgg = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄:"+keyAsString+bucket.getDocCount());
        }
        Avg balanceAvg = aggregations.get("balanceAvg"); 
        System.out.println("平均薪资:"+balanceAvg.getValue());
    }

    @Test
    void contextLoads() {
        System.out.println(client);
    }

    /**
     * 存储数据到es里
     * 更新也可以
     */
    @Test
    void indexDate() throws IOException {
        IndexRequest request = new IndexRequest("users");
        request.id("1");//数据的id
       // request.source("userName","zhangsan","age",18,"gender","男");
        User user = new User();
        user.setUserName("张三");
        user.setAge(18);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
        request.source(jsonString, XContentType.JSON); //要保存的内容
        //执行操作
        IndexResponse index = client.index(request, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //提取有用的数据
        System.out.println(index);
        
    }
    @Data
    class User{
        private  String userName;
        private String gender;
        private  Integer age;
    }
}
