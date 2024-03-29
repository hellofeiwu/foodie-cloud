package com.imooc.order.controller.center;

import com.imooc.controller.BaseController;
import com.imooc.enums.YesOrNo;
import com.imooc.order.pojo.OrderItems;
import com.imooc.order.pojo.Orders;
import com.imooc.order.pojo.bo.center.OrderItemsCommentBO;
import com.imooc.order.service.center.MyCommentsService;

import com.imooc.order.service.center.MyOrdersService;
import com.imooc.pojo.IMOOCJSONResult;
import com.imooc.pojo.PagedGridResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Api(value = "用户中心评价模块", tags = {"用户中心评价模块相关接口"})
@RestController
@RequestMapping("mycomments")
public class MyCommentsController extends BaseController {
    public static final Logger log = LoggerFactory.getLogger(MyCommentsController.class);

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MyOrdersService myOrdersService;

    @Autowired
    private MyCommentsService myCommentsService;

    @ApiOperation(value = "查询订单列表", notes = "查询订单列表", httpMethod = "POST")
    @PostMapping("/pending")
    public IMOOCJSONResult delete(
            @ApiParam(name = "orderId", value = "订单id", required = true)
            @RequestParam String orderId,
            @ApiParam(name = "userId", value = "用户id", required = true)
            @RequestParam String userId
    ) {
        // 判断用户和订单是否关联
        IMOOCJSONResult checkResult = myOrdersService.checkUserOrder(userId, orderId);
        if (checkResult.getStatus() != HttpStatus.OK.value()) {
            return checkResult;
        }

        // 判断该笔订单是否已经评价过，评价过了就不再继续
        Orders myOrder = (Orders) checkResult.getData();
        if (myOrder.getIsComment() == YesOrNo.YES.type) {
            return IMOOCJSONResult.errorMsg("该笔订单已经评价");
        }

        List<OrderItems> list = myCommentsService.queryPendingComment(orderId);

        return IMOOCJSONResult.ok(list);
    }

    @ApiOperation(value = "保存评论列表", notes = "保存评论列表", httpMethod = "POST")
    @PostMapping("/saveList")
    public IMOOCJSONResult saveList(
            @ApiParam(name = "orderId", value = "订单id", required = true)
            @RequestParam String orderId,
            @ApiParam(name = "userId", value = "用户id", required = true)
            @RequestParam String userId,
            @RequestBody List<OrderItemsCommentBO> commentList
    ) {
        log.info(commentList.toString());

        // 判断用户和订单是否关联
        IMOOCJSONResult checkResult = myOrdersService.checkUserOrder(userId, orderId);
        if (checkResult.getStatus() != HttpStatus.OK.value()) {
            return checkResult;
        }

        // 判断评论内容list不能为空
        if (commentList == null || commentList.isEmpty()) {
            return IMOOCJSONResult.errorMsg("评论内容不能为空！");
        }

        myCommentsService.saveComments(orderId, userId, commentList);

        return IMOOCJSONResult.ok();
    }

    @ApiOperation(value = "查询我的评价", notes = "查询我的评价", httpMethod = "POST")
    @PostMapping("/query")
    public IMOOCJSONResult query(
            @ApiParam(name = "userId", value = "用户id", required = true)
            @RequestParam String userId,
            @ApiParam(name = "page", value = "第几页", required = false)
            @RequestParam Integer page,
            @ApiParam(name = "pageSize", value = "每页显示条数", required = false)
            @RequestParam Integer pageSize
    ) {
        if(userId == null) {
            return IMOOCJSONResult.errorMsg(null);
        }

        page = page == null ? 1 : page;
        pageSize = pageSize == null ? COMMON_PAGE_SIZE : pageSize;

        //PagedGridResult result = myCommentsService.queryMyComments(userId, page, pageSize);
        // TODO 前方施工，学完Feign再来改造
        ServiceInstance instance = loadBalancerClient.choose("FOODIE-ITEM-SERVICE");
        String target = String.format("http://%s:%s/item-comments-api/myComments" +
                        "?userId=%s&page=%s&pageSize=%s",
                instance.getHost(),
                instance.getPort(),
                userId,
                page,
                pageSize);
        // 偷个懒，不判断返回status，等下个章节用Feign重写
        PagedGridResult result = restTemplate.getForObject(target, PagedGridResult.class);
        return IMOOCJSONResult.ok(result);
    }
}
