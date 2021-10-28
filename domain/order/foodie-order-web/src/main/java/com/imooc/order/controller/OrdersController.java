package com.imooc.order.controller;

import com.imooc.controller.BaseController;
import com.imooc.enums.OrderStatusEnum;
import com.imooc.enums.PayMethod;
import com.imooc.order.pojo.OrderStatus;
import com.imooc.order.pojo.bo.PlaceOrderBO;
import com.imooc.order.pojo.bo.SubmitOrderBO;
import com.imooc.order.pojo.vo.MerchantOrdersVO;
import com.imooc.order.service.OrderService;
import com.imooc.pojo.IMOOCJSONResult;
import com.imooc.pojo.ShopcartBO;
import com.imooc.utils.CookieUtils;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.RedisOperator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Api(value = "订单相关", tags = {"订单相关的api接口"})
@RestController
@RequestMapping("orders")
public class OrdersController extends BaseController {
    @Autowired
    private RedisOperator redisOperator;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RestTemplate restTemplate;

    @ApiOperation(value = "用户下单", notes = "用户下单", httpMethod = "POST")
    @PostMapping("/create")
    public IMOOCJSONResult create(
            @RequestBody SubmitOrderBO submitOrderBO,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (submitOrderBO.getPayMethod() != PayMethod.WEIXIN.type
                && submitOrderBO.getPayMethod() != PayMethod.ALIPAY.type) {
            return IMOOCJSONResult.errorMsg("支付方式不支持！");
        }

        String shopcartJson = redisOperator.get("shopcart:" + submitOrderBO.getUserId());
        List<ShopcartBO> shopcartList = new ArrayList<>();
        if (StringUtils.isBlank(shopcartJson)) {
            return IMOOCJSONResult.errorMsg("购物车数据不正确");
        }

        shopcartList = JsonUtils.jsonToList(shopcartJson, ShopcartBO.class);

        // 1. 创建订单
        PlaceOrderBO placeOrderBO = new PlaceOrderBO(submitOrderBO, shopcartList);
        MerchantOrdersVO merchantOrdersVO = orderService.createOrder(placeOrderBO);
        String orderId = merchantOrdersVO.getMerchantOrderId();

        // 2. 创建订单以后，移除购物车中已结算（已提交）的商品
        shopcartList.removeAll(merchantOrdersVO.getToBeRemovedShopcartList());
        redisOperator.set("shopcart:" + submitOrderBO.getUserId(), JsonUtils.objectToJson(shopcartList));
        // 整合redis之后，完善购物车中的已结算商品清除，并且同步到前端的cookie
        CookieUtils.setCookie(request, response, FOODIE_SHOPCART, JsonUtils.objectToJson(shopcartList), true);

        // 3. 向支付中心发送当前订单，用于保存支付中心的订单数据
        merchantOrdersVO.setReturnUrl(payReturnUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("imoocUserId", "1979311-493553064"); // 支付账号userId
        headers.add("password", "ft4w-3rf3-4pio-wrfj"); // 支付账号password

        HttpEntity<MerchantOrdersVO> entity = new HttpEntity<>(merchantOrdersVO, headers);

        ResponseEntity<IMOOCJSONResult> responseEntity = restTemplate.postForEntity(paymentUrl, entity, IMOOCJSONResult.class);
        IMOOCJSONResult paymentResult = responseEntity.getBody();

        if (paymentResult.getStatus() != 200) {
            return IMOOCJSONResult.errorMsg("支付中心订单创建失败！");
        }

        return IMOOCJSONResult.ok(orderId);
    }

    @ApiOperation(value = "订单支付完成后的回调通知", notes = "订单支付完成后的回调通知", httpMethod = "POST")
    @PostMapping("/notifyMerchantOrderPaid")
    public Integer create(@RequestParam String merchantOrderId) {
        orderService.updateOrderStatus(merchantOrderId, OrderStatusEnum.WAIT_DELIVER.type);
        return HttpStatus.OK.value();
    }

    @ApiOperation(value = "查询订单状态", notes = "查询订单状态", httpMethod = "POST")
    @PostMapping("/getPaidOrderInfo")
    public IMOOCJSONResult getPaidOrderInfo(@RequestParam String orderId) {
        OrderStatus orderStatus = orderService.queryOrderStatusInfo(orderId);
        return IMOOCJSONResult.ok(orderStatus);
    }
}
