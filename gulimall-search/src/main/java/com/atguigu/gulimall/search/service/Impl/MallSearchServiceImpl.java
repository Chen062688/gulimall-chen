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
     * @param param ?????????????????????
     * @return ?????????????????????,??????????????????????????????????????????
     */
    @Override
    public SearchResult search(SearchParam param) {
        //??????????????????????????????DSL??????
        SearchResult result=null;
        //1.??????????????????
        SearchRequest searchRequest =  buildSearchRequest(param);;
       
        try {
            //2.??????????????????
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            //3.????????????????????????????????????????????????
         result = buildSearchResult(response,param);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    /**
     * ???????????????????????????
     * #????????????,??????(????????????,??????,??????,????????????,??????),??????,??????,??????,????????????
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder=new SearchSourceBuilder();//??????DSL??????
        /**
         * ??????(????????????,??????,??????,????????????,??????)
         */
        //1.??????boolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1???must-????????????
        if(!StringUtils.isEmpty(param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",param.getKeyword()));
        }
       //1.2???bool - filter ??????????????????id??????
        if(param.getCatalog3Id()!=null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",param.getCatalog3Id()));
        }
     //1.2???bool-filter ????????????id??????
        if(param.getBrandId()!=null&& param.getBrandId().size()>0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",param.getBrandId()));
        }
     //1.3???bool-filter ??????????????????
        if(param.getAttrs()!=null && param.getAttrs().size()>0){
            for (String attrStr : param.getAttrs()) {
                //attrs=1_5???:8???&attrs=2_16G:8G
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                //attr =1_5???:8???
                String[] split = attrStr.split("_");
                String attrId =  split[0];//?????????????????????id
                String[] attrValues = split[1].split(":");//??????????????????????????????
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId))
                        .must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                //?????????????????????????????????nested?????? 
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
    //1.4 boot-filter ????????????????????????
        if(param.getHasStock()!=null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",param.getHasStock()==1));
        }
       
   //1.5 boot-filter ?????????????????? 
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
                //??????
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
         * ??????,??????,??????
         */
        //2?????????
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
        //2.1????????? pageSize:5
        //pageNum:1 from:0 size:5
        //pageNum:2 from:5 size:5
        //from =pageNum-1*size
        sourceBuilder.from((param.getPageNum()-1)*EsConstance.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstance.PRODUCT_PAGESIZE);
      //2.3?????????
        if(!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle").preTags("<b style='color:red'>").postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }
        /**
         * ????????????
         */
        //1.????????????
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //2.????????????????????????
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        //TODO 1.????????????brand
        sourceBuilder.aggregation(brand_agg);
        //2.???????????? catalog_agg
        TermsAggregationBuilder catelog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catelog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        //TODO 1.????????????catelog
        sourceBuilder.aggregation(catelog_agg);
        //3.????????????
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //3.1???????????????????????????attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //3.2????????????????????????attr_id???????????????
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //3.2????????????????????????attr_id???????????????????????????attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        //TODO 3.????????????attr
        sourceBuilder.aggregation(attr_agg);
        
        
        String s = sourceBuilder.toString();
        //?????????????????????????????????????????????
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstance.PRODUCT_INDEX},sourceBuilder);
        
        return searchRequest;
    }
    
    
    /**
     * ??????????????????
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        SearchResult result = new SearchResult();
        //1.???????????????????????????????????????
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
        //2.????????????????????????????????????????????????
        List<SearchResult.AttrVo>attrVos=new ArrayList<>();
        ParsedNested attr_agg=response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        List<? extends Terms.Bucket> buckets2 = attr_id_agg.getBuckets();
        for (Terms.Bucket bucket : buckets2) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //1.???????????????id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            //2.?????????????????????
          ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            //3.????????????????????????
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
//      //  3.????????????????????????????????????????????????
        List<SearchResult.BrandVo>brandVos=new ArrayList<>(); 
        ParsedLongTerms brand_agg= response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //1.???????????????id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            //2.????????????????????? 
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String band_name = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(band_name);
            //3.?????????????????????
//            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
//            String band_img = brand_name_agg.getBuckets().get(0).getKeyAsString();
            String img_agg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(img_agg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);
//        //4.????????????????????????????????????????????????
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo>catalogVos=new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //???????????????id
            String key = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(key));
            //???????????????
         ParsedStringTerms  catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            List<? extends Terms.Bucket> buckets1 = catalog_name_agg.getBuckets();
            String catalog_name = buckets1.get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
//        =======??????????????????????????????=========
//        //5.?????????????????????
        result.setPageNum(param.getPageNum());
      //6.?????????????????????
        long total=hits.getTotalHits().value;
        result.setTotal(total);
//        //7.?????????????????? ????????????
     int  totalPages = (int)total%EsConstance.PRODUCT_PAGESIZE==0?(int)total/EsConstance.PRODUCT_PAGESIZE:((int)total/EsConstance.PRODUCT_PAGESIZE+1);
        result.setTotalPages(totalPages);
        List<Integer>pageNavs=new ArrayList<>();
        for (int i=1;i<totalPages;i++){
            pageNavs.add(i);
        }   
        result.setPageNavs(pageNavs);
        
        //8.???????????????????????????
        if(param.getAttrs()!=null && param.getAttrs().size()>0){
            List< SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                //1.???????????????attrs???????????????????????????
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
                //2.??????????????????????????????,??????????????????????????????,??????????????????url???????????????url??????
                //??????????????????????????????????????????
                String replace = replaceQueryString(param, attr,"attrs");
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
                return  navVo;
            }).collect(Collectors.toList());
            
            result.setNavs(collect);
        }
            //??????,??????
            if(param.getBrandId()!=null && param.getBrandId().size()>0){
                List<SearchResult.NavVo> navs = result.getNavs();
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                navVo.setNavName("??????");
                //TODO ????????????????????????
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
                //TODO ??????:?????????????????????
                
            }
        return result;
    }

    /**
     * ?????????????????????
     *
     * @param param ??????
     * @param value ??????
     * @param key   ??????
     * @return {@link String}
     */
    private static String replaceQueryString(SearchParam param, String value,String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
          encode = encode.replace("+","%20");//??????????????????????????????java?????????
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String replace = param.get_queryString().replace("&"+key+"="+ encode,"");
        return replace;
    }
}
