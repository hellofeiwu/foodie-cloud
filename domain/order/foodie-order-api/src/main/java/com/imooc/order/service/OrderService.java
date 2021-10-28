package com.imooc.order.service;

import com.imooc.order.pojo.OrderStatus;
import com.imooc.order.pojo.bo.PlaceOrderBO;
import com.imooc.order.pojo.vo.MerchantOrdersVO;
import org.springframework.web.bind.annotation.*;

@RequestMapping("order-api")
public interface OrderService {

    /**
     * 用于创建订单相关信息
     * @param orderBO
     */
    @PostMapping("placeOrder")
    // 这里创建PlaceOrderBO的原因是把原来的参数二合一，方便前端来传参数
    public MerchantOrdersVO createOrder(@RequestBody PlaceOrderBO orderBO);

    /**
     * 修改订单状态
     * @param orderId
     * @param orderStatus
     */
    @PostMapping("updateStatus")
    public void updateOrderStatus(@RequestParam("orderId") String orderId,
                                  @RequestParam("orderStatus") Integer orderStatus);

    /**
     * 查询订单状态
     * @param orderId
     * @return
     */
    @GetMapping("orderStatus")
    public OrderStatus queryOrderStatusInfo(@RequestParam("orderId") String orderId);

    /**
     * 关闭超时未支付订单
     */
    @PostMapping("closePendingOrders")
    public void closeOrder();

}
