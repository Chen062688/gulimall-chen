package com.atguigu.gulimall.product.Web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    RedisTemplate redisTemplate;

    @GetMapping({"/", "/index.html"})
    public String IndexPage(Model model) {
        //TODO 1.查出我们的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();
        model.addAttribute("categorys", categoryEntities);
        return "index";
    }

    //index/catalog.json
    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2Vo>> getCatelogJson() throws InterruptedException {
        Map<String, List<Catelog2Vo>> catelogJson = categoryService.getCatelogJson();
        return catelogJson;
    }

    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        //1.获取一把锁,只要锁名一样就是同一把锁
        RLock lock = redissonClient.getLock("my-lock");
        //2.加锁
        // lock.lock();//阻塞式等待.默认加的锁都是30s时间
        //1)锁的自动续期,如果业务超长,运行期间自动给锁续上新的30s 不用担心业务时间长,锁自动过期被删掉
        //2)加锁的业务只要运行完成,就不会给当前续期,即使不手动解锁,锁默认在30s后自动删除
        lock.lock(10, TimeUnit.SECONDS); //10秒自动解锁
        try {
            System.out.println("加锁成功!,执行业务...." + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {

        } finally {
            //3.解锁
            System.out.println("释放锁..." + Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }

    //保证一定能读取到最新数据,修改期间,写锁式是一个排他锁(互斥锁)读锁是一个共享锁
    //写锁没释放读就必须等待
    //读+读:相当于无锁,并发读,只会在redis中记录好,所有当前的读锁,他们都会同时加锁成功!
    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        String string = "";
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        RLock writeLock = lock.writeLock();
        try {
            //1.改数据加写锁,读数据加写锁
            writeLock.lock();
            string = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("writeValue", string);
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
        return string;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        //加读锁
        RLock readLock = lock.readLock();
        String value = "";
        readLock.lock();
        try {
            value = (String) redisTemplate.opsForValue().get("writeValue");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
        return value;
    }

    /**
     * 放假、锁门
     * 1班没人了
     * 5个班，全部走完，我们才可以锁大门
     * 分布式闭锁
     */

    @GetMapping(value = "/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {

        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();       //等待闭锁完成

        return "放假了...";
    }

    @GetMapping(value = "/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown();       //计数-1

        return id + "班的人都走了...";
    }

    /**
     * 车库停车
     * 3个车位
     * 信号量也可以用作分布式  限流
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        // park.acquire();//获取一个信号,获取一个值,占一个车位
        boolean b = park.tryAcquire();
        if (b) {
            //执行业务
        } else {
            return "erro";
        }
        return "ok=>" + b;
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();//释放一个车位
        return "ok";
    }
}
