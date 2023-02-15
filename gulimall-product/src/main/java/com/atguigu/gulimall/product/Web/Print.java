package com.atguigu.gulimall.product.Web;



public class Print {
    public static void main(String[] args) {
        int zhixing=0;
        int sum=0;
        for (int i = 1; i >0 ; i++) {
            if(i%2==0){
                System.out.println(i/2);
                zhixing++;
                sum+=i/2;
            }
            if(zhixing==100){
                System.out.println(sum);
                break;
            }
            if(i%3==0){
                System.out.println(i/3);
                zhixing++;
                sum+=i/3;
            }
            if(zhixing==100){
                System.out.println(sum);
                break;
            }
            if(i%5==0){
                System.out.println(i/5);
                zhixing++;
                sum+=i/5;
            }
          if(zhixing==100){
              System.out.println(sum);
              break;
          }
        }
    }
}
