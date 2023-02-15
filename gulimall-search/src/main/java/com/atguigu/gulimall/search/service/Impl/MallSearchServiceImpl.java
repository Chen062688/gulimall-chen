package com.atguigu.gulimall.search.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constance.EsConstance;
import com.atguigu.gulimall.search.fegin.ProductFeginService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient client;
    
    @Autowired
    private ProductFeginService productFeginService;
    /**
     * 
     * @param param 检索的所有参数
     * @return 返回检索的结果,里面包含页面所需要的所有信息
     */
    @Override
    public SearchResult search(SearchParam param) {
        //动态构造出查询需要的DSL语句
        SearchResult result=null;
        //1.准备检索请求
        SearchRequest searchRequest =  buildSearchRequest(param);;
       
        try {
            //2.执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            //3.分析响应数据封装成我们需要的格式
         result = buildSearchResult(response,param);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    /**
     * 准备我们的检索请求
     * #模糊匹配,过滤(按照属性,分类,品牌,价格区间,库存),排序,分页,高亮,聚合分析
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder=new SearchSourceBuilder();//构建DSL语句
        /**
         * 过滤(按照属性,分类,品牌,价格区间,库存)
         */
        //1.构建boolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1、must-模糊匹配
        if(!StringUtils.isEmpty(param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",param.getKeyword()));
        }
       //1.2、bool - filter 按照三级分类id查询
        if(param.getCatalog3Id()!=null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",param.getCatalog3Id()));
        }
     //1.2、bool-filter 按照品牌id查询
        if(param.getBrandId()!=null&& param.getBrandId().size()>0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",param.getBrandId()));
        }
     //1.3、bool-filter 按照属性查询
        if(param.getAttrs()!=null && param.getAttrs().size()>0){
            for (String attrStr : param.getAttrs()) {
                //attrs=1_5寸:8寸&attrs=2_16G:8G
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                //attr =1_5寸:8寸
                String[] split = attrStr.split("_");
                String attrId =  split[0];//这是检索的属性id
                String[] attrValues = split[1].split(":");//这个属性的检索用的值
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId))
                        .must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                //每一个必须都得生成一个nested查询 
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
    //1.4 boot-filter 按照库存进行查询
        if(param.getHasStock()!=null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",param.getHasStock()==1));
        }
       
   //1.5 boot-filter 按照价格区间 
        if(!StringUtils.isEmpty(param.getSkuPrice())){
            //1_500/_500/500_
            /**
             *   "range": {
             *             "skuPrice": {
             *               "gte": 0,
             *               "lte": 15000
             *             }
             */
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] split = param.getSkuPrice().split("_"); 
            if(split.length==2){
                //区间
                rangeQuery.gte(split[0]).lt(split[1]);
            }else  if(split.length==1){
                if(param.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(split[0]);
                }
                if(param.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(split[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }
        sourceBuilder.query(boolQuery);
        /**
         * 排序,分页,高亮
         */
        //2、排序
        if(!StringUtils.isEmpty(param.getSort())){
            /**
             * sort=saleCount_asc
             * sort=skuPrice_asc/desc
             * sort=hotScore_asc/desc
             */
            String sort= param.getSort();
            String[] s = sort.split("_");
            SortOrder order=s[1].equalsIgnoreCase("asc")?SortOrder.ASC:SortOrder.DESC;
            sourceBuilder.sort(s[0],order);
        }
        //2.1、分页 pageSize:5
        //pageNum:1 from:0 size:5
        //pageNum:2 from:5 size:5
        //from =pageNum-1*size
        sourceBuilder.from((param.getPageNum()-1)*EsConstance.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstance.PRODUCT_PAGESIZE);
      //2.3、高亮
        if(!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle").preTags("<b style='color:red'>").postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }
        /**
         * 聚合分析
         */
        //1.品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //2.品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        //TODO 1.聚合品牌brand
        sourceBuilder.aggregation(brand_agg);
        //2.分类聚合 catalog_agg
        TermsAggregationBuilder catelog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catelog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        //TODO 1.聚合分类catelog
        sourceBuilder.aggregation(catelog_agg);
        //3.属性聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //3.1、聚合出当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //3.2、聚合分析出当前attr_id对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //3.2、聚合分析出当前attr_id对应的所有的属性值attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        //TODO 3.聚合属性attr
        sourceBuilder.aggregation(attr_agg);
        
        
        String s = sourceBuilder.toString();
        //把以前的所有条件都拿来进行封装
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstance.PRODUCT_INDEX},sourceBuilder);
        
        return searchRequest;
    }
    
    
    /**
     * 构建结果数据
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        SearchResult result = new SearchResult();
        //1.返回的所有查询到的所有商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels=new ArrayList<>();
        if(hits.getHits()!=null &&  hits.getHits().length>0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if(!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.fragments()[0].string();
                    esModel.setSkuTitle(string);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);
        //2.当前所有商品涉及到的所有属性信息
        List<SearchResult.AttrVo>attrVos=new ArrayList<>();
        ParsedNested attr_agg=response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        List<? extends Terms.Bucket> buckets2 = attr_id_agg.getBuckets();
        for (Terms.Bucket bucket : buckets2) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //1.得到属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            //2.得到属性的名字
          ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            //3.得到属性的所有值
            List<String> attr_value_agg = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attr_value_agg);
            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);
//      //  3.当前所有商品涉及到的所有商品信息
        List<SearchResult.BrandVo>brandVos=new ArrayList<>(); 
        ParsedLongTerms brand_agg= response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //1.得到品牌的id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //2.得到品牌的名子 
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String band_name = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(band_name);
            //3.得到品牌的图片
//            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
//            String band_img = brand_name_agg.getBuckets().get(0).getKeyAsString();
            String img_agg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(img_agg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);
//        //4.当前所有商品涉及到的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo>catalogVos=new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类的id
            String key = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(key));
            //得到分类名
         ParsedStringTerms  catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            List<? extends Terms.Bucket> buckets1 = catalog_name_agg.getBuckets();
            String catalog_name = buckets1.get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
//        =======以上从聚合信息中获取=========
//        //5.分页信息的页码
        result.setPageNum(param.getPageNum());
      //6.分页的总记录数
        long total=hits.getTotalHits().value;
        result.setTotal(total);
//        //7.分页的总页码 计算得到
     int  totalPages = (int)total%EsConstance.PRODUCT_PAGESIZE==0?(int)total/EsConstance.PRODUCT_PAGESIZE:((int)total/EsConstance.PRODUCT_PAGESIZE+1);
        result.setTotalPages(totalPages);
        List<Integer>pageNavs=new ArrayList<>();
        for (int i=1;i<totalPages;i++){
            pageNavs.add(i);
        }   
        result.setPageNavs(pageNavs);
        
        //8.构建面包屑导航功能
        if(param.getAttrs()!=null && param.getAttrs().size()>0){
            List< SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                //1.分析每一个attrs传过来的查询参数值
                SearchResult.NavVo navVo=new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeginService.attrInfo(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if(r.getCode()==0){
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                }else {
                    navVo.setNavName(s[0]);
                }
                //2.取消了这个面包屑以后,我们要跳转到那个地方,将请求地址的url里面的当前url置空
                //拿到所有的查到条件去掉当前。
                String replace = replaceQueryString(param, attr,"attrs");
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
                return  navVo;
            }).collect(Collectors.toList());
            
            result.setNavs(collect);
        }
            //品牌,分类
            if(param.getBrandId()!=null && param.getBrandId().size()>0){
                List<SearchResult.NavVo> navs = result.getNavs();
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                navVo.setNavName("品牌");
                //TODO 远程查询所有品牌
                R r = productFeginService.brandsInfo(param.getBrandId());
                if(r.getCode()==0){
                    List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>() {
                    });
                    StringBuffer buffer=new StringBuffer();
                    String replace="";
                    for (BrandVo brandVo : brand) {
                     buffer.append(brandVo.getBrandName()+";");
                        replace = replaceQueryString(param, brandVo.getBrandId()+"","brandId");
                    }
                    navVo.setNavValue(buffer.toString());
                    navVo.setLink("http://search.gulimall.com?"+replace);
                }
                navs.add(navVo);
                //TODO 分类:不需要导航取消
                
            }
        return result;
    }

    /**
     * 查询字符串替换
     *
     * @param param 参数
     * @param value 价值
     * @param key   关键
     * @return {@link String}
     */
    private static String replaceQueryString(SearchParam param, String value,String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
          encode = encode.replace("+","%20");//浏览器对空格的编码和java不一样
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String replace = param.get_queryString().replace("&"+key+"="+ encode,"");
        return replace;
    }
}
