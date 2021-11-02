package com.imooc.cart.service.impl;

import com.imooc.cart.service.CartService;
import com.imooc.pojo.ShopcartBO;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.RedisOperator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CartServiceImpl implements CartService {
    @Autowired
    private RedisOperator redisOperator;

    @Override
    public boolean addItemToCart(String userId, ShopcartBO shopcartBO) {
        // 前端用户在登陆的情况下，添加商品到购物车，会同时在后端同步购物车到redis缓存
        // 需要判断当前购物车中包含已经存在的商品，如果存在则累加购买数量
        String shopcartJson = redisOperator.get("shopcart:" + userId);
        List<ShopcartBO> shopcartList = new ArrayList<>();
        if (StringUtils.isBlank(shopcartJson)) {
            // redis中没有购物车
            shopcartList.add(shopcartBO);
        }else {
            // redis中已经有购物车了
            shopcartList = JsonUtils.jsonToList(shopcartJson, ShopcartBO.class);
            // 判断购物车中是否存在已有商品，如果有的话counts累加
            boolean isHaving = false;
            for (ShopcartBO sc : shopcartList) {
                String tmpSpecId = sc.getSpecId();
                if (tmpSpecId.equals(shopcartBO.getSpecId())) {
                    sc.setBuyCounts(sc.getBuyCounts() + shopcartBO.getBuyCounts());
                    isHaving = true;
                }
            }
            if (!isHaving) {
                shopcartList.add(shopcartBO);
            }
        }
        redisOperator.set("shopcart:" + userId, JsonUtils.objectToJson(shopcartList));
        return true;
    }

    @Override
    public boolean removeItemFromCart(String userId, String itemSpecId) {
        // 用户在页面删除购物车中的商品数据，如果此时用户已经登陆，则需要同步删除后端购物车中的商品

        String shopcartJson = redisOperator.get("shopcart:" + userId);
        List<ShopcartBO> shopcartList = new ArrayList<>();
        if (StringUtils.isNotBlank(shopcartJson)) {
            shopcartList = JsonUtils.jsonToList(shopcartJson, ShopcartBO.class);
            for (ShopcartBO sc : shopcartList) {
                String tmpSpecId = sc.getSpecId();
                if (tmpSpecId.equals(itemSpecId)) {
                    shopcartList.remove(sc);
                    break;
                }
            }

            redisOperator.set("shopcart:" + userId, JsonUtils.objectToJson(shopcartList));
        }
        return true;
    }

    @Override
    public boolean clearCart(String userId) {
        redisOperator.del("shopcart:" + userId);
        return true;
    }
}
