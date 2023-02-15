package com.atguigu.gulimall.product;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Slf4j
class GulimallProductApplicationTests {
    @Autowired
    private BrandService brandService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CategoryService categoryService;
 

   
   // OSSClient ossClient;
    
    @Autowired
    private AttrGroupDao attrGroupDao;
    
    @Autowired
    private SkuSaleAttrValueDao skuSaleAttrValueDao;
    @Test
    void test() {
        List<SpuItemAttrGroupVo> group = attrGroupDao.getAttrGroupWithAttrsBySpuId(100L, 225L);
        System.out.println(group);
    }

    @Test
    void sss() {
        List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(13L);
        System.out.println(saleAttrsBySpuId);
    }

    @Test
    public void  testUpload() throws FileNotFoundException {
       /* String endpoint="oss-cn-beijing.aliyuncs.com";
        // 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建RAM账号。
        String accessKeyId = "LTAI5t8DNYPwwd9yXAZVQdrf";
        String accessKeySecret = "bAU4ctkMHWBgDUrZqQ3p2wRBmnWUO1";

// 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);*/

// 创建PutObjectRequest对象。
      InputStream inputStream=new FileInputStream("C:\\Users\\张立晨\\Pictures\\R672ac3670c1c47b7b215b64d140ad2ee.jpg");
     /*  ossClient.putObject("gulimall-chen6","科比.jpg",inputStream);*/

// 如果需要上传时设置存储类型与访问权限，请参考以下示例代码。
// ObjectMetadata metadata = new ObjectMetadata();
// metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
// metadata.setObjectAcl(CannedAccessControlList.Private);
// putObjectRequest.setMetadata(metadata);

// 上传文件。
       // ossClient.putObject(putObjectRequest);

// 关闭OSSClient。
     /*   ossClient.shutdown();*/
        System.out.println("上传成功!");
    }
    @Test
    void contextLoads() {
        BrandEntity brandEntity=new BrandEntity();
        brandEntity.setName("华为");
        boolean save = brandService.save(brandEntity);
        System.out.println(save);
    }

    @Test
    void select() {
        QueryWrapper<BrandEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("name","华为");
        List<BrandEntity> brandEntities = brandService.list(queryWrapper);
        System.out.println(brandEntities);
    }

    @Test
    void name() {
        Long[] catelogPath = categoryService.findCatelogPath(227L);
        log.info("完整路径:{}", Arrays.asList(catelogPath));
    }

    @Test
    void TestRedis() {
        redisTemplate.opsForValue().set("hell1","word_"+ UUID.randomUUID().toString());
        String hello = (String) redisTemplate.opsForValue().get("hello");

        System.out.println(hello);
    }
    @Autowired
    RedissonClient redissonClient;
    @Test
    void rediss() {
        System.out.println(redissonClient);
    }
}
