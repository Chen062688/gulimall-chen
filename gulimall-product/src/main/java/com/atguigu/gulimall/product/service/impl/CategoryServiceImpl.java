package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.checkerframework.checker.units.qual.A;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
        
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    RedissonClient redisson;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override   
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.要查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2.组装成父子的树形结构
        
        //2.1)找到所有的一级分类
        List<CategoryEntity> leve1Menus = entities.stream().filter(categoryEntity -> 
             categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu1.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());
        
        return leve1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检测当前删除的菜单,是否被别的地方引用
        
        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }
    
    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        //通过Collections.revers把参数逆序过来
        Collections.reverse(parentPath);
        return  parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @CacheEvict:失效模式
     * 1.同时进行多种缓存操作
     * 2.指定删除某个分区下的所有数据  @CacheEvict(value = "category",allEntries = true)
     * @param category
     */
  
   /* @Caching(evict = {
            @CacheEvict(value = "category",key = "'getLevel1Categorys'"),
            @CacheEvict(value = "category",key = "'getCatelogJson'")
    })*/
    @CacheEvict(value = "category",allEntries = true) //失效模式
    //@CachePut 双写模式
    @Override
    @Transactional
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    @Override
    @Cacheable(value = "category",key = "#root.methodName")
    public Map<String, List<Catelog2Vo>> getCatelogJson() throws InterruptedException {
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1.查出所有1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            ///每一个的一级分类,查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));
        return parent_cid;
    }

    //每一个需要缓存的数据都来指定要放到那个名字的缓存【缓存分区(按照业务类型分)】
    @Override
    @Cacheable(value = {"category"},key = "#root.methodName",sync = true) //代表当前方法的结果需要缓存,如果缓存中有,方法都不用调用,如果缓存没有,调用方法,最后将方法的结果放入缓存
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("掉用了缓存");
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_cid",0);
        List<CategoryEntity> list = this.baseMapper.selectList(queryWrapper);
        return list;
    }
   // @Override
    public  Map<String, List<Catelog2Vo>> getCatelogJson2() throws InterruptedException {
        /**
         * 性能优化解决 雪崩 缓存击穿 穿透
         * 1.解决缓存击穿:对null值进行缓存,给null值设置过期时间
         * 2.解决雪崩:设置过期时间(加随机值)
         * 3.解决穿透:加分布式事务锁
         */
        //给缓存中放入json字符串,拿出的json字符串,还用逆转为能用的对象类型; 【序列化与饭序列化】
        //1.加入缓存
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
            if (StringUtils.isEmpty(catalogJSON)) {
                System.out.println("缓存不命中...将要查询数据库");
                //2.缓存中没有数据,查询数据库将对象转为json并放入redis缓存中
                Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedisLock();
                //redisTemplate.opsForValue().set("catalogJSON", JSONArray.toJSONString(catelogJsonFromDb));
               
                return catelogJsonFromDb;
            }
            /**
             * 如果缓存中有直接拿到数据转为我们指定接收的对象再给予返回
             */
        System.out.println("缓存命中....直接返回");
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
    }

    /**
     * 缓存里面和数据库如何保持一致
     * 缓存数据一致性
     * 1)、 双写模式
     * 2)、失效模式
     * @return
     * @throws InterruptedException
     */
    public  Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissonLock() throws InterruptedException {
            //1.占锁,锁的名字 锁的粒度 ,越细越快。
            //锁的粒度: 具体缓存的是某个数据, 11-号商品; product-11-lock 
        RLock lock = redisson.getLock("catelogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDb = null;
            try {
                dataFromDb = getDataFromDb();
            } finally {
              lock.unlock();
            }
            return dataFromDb;
    }
    //数据库查询并封装至分类数据
    public  Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock() throws InterruptedException {
        /**
         * 1.占分布式锁.去redis占坑 并且设置过期时间防止死锁
         */
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock",uuid,300,TimeUnit.SECONDS);
        if(lock){
            System.out.println("获取分布式锁成功!......");
            //加锁成功!执行业务
            //2.设置过期时间,必须和加锁是同步的,原子性
//            String lockValue = redisTemplate.opsForValue().get("lock");
            Map<String, List<Catelog2Vo>> dataFromDb=null;
            try {
                dataFromDb = getDataFromDb();
            }finally {
                /**
                 * 删锁的时候,判断是不是我们的锁 使用lua脚本
                 */
                String lua="if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                //删除锁
                Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(lua, Long.class),
                        Arrays.asList("lock"), uuid);
            }
            return dataFromDb;
        }else {
            //加锁失败...重试
            System.out.println("获取分布式锁失败...等待重试");
            try {
                Thread.sleep(200);
            }catch (Exception e){
                
            }
            return  getCatelogJsonFromDbWithRedisLock();//自选的方式
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            //缓存不为null直接返回
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return catelogJsonFromDb;
        }
        System.out.println("查询了数据库!");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1.查出所有1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            ///每一个的一级分类,查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));
        String s=JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
        return parent_cid;
    }

    public  Map<String, List<Catelog2Vo>> getCatelogJsonFromDb() {
        /**
         * 1.将数据库的多次查询变为一次
         */
        synchronized (this) {
            String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
           if (!StringUtils.isEmpty(catalogJSON)) {
               //缓存不为null直接返回
               Map<String, List<Catelog2Vo>> catelogJsonFromDb = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
               });
              return catelogJsonFromDb;
           }
            System.out.println("查询了数据库!");
            List<CategoryEntity> selectList = baseMapper.selectList(null);
            //1.查出所有1级分类
            List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
            //2.封装数据
            Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                ///每一个的一级分类,查到这个一级分类的二级分类
                List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                //封装上面的结果
                List<Catelog2Vo> catelog2Vos = null;
                if (categoryEntities != null) {
                    catelog2Vos = categoryEntities.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //找当前二级分类的三级分类封装成vo
                        List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                        if (level3Catelog != null) {
                            List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                            catelog2Vo.setCatalog3List(collect);
                        }
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }

                return catelog2Vos;
            }));
            String s=JSON.toJSONString(parent_cid);
            redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
            return parent_cid;
        }
    }
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return collect;
        // return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }


    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1.收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
                findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }
    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all){
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2.菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu1.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    } 
}