package com.imooc.item.controller;

import com.imooc.controller.BaseController;
import com.imooc.item.pojo.Items;
import com.imooc.item.pojo.ItemsImg;
import com.imooc.item.pojo.ItemsParam;
import com.imooc.item.pojo.ItemsSpec;
import com.imooc.item.pojo.vo.CommentLevelCountsVO;
import com.imooc.item.pojo.vo.ItemInfoVO;
import com.imooc.item.pojo.vo.ShopcartVO;
import com.imooc.item.service.ItemService;
import com.imooc.pojo.IMOOCJSONResult;
import com.imooc.pojo.PagedGridResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "商品接口", tags = {"商品信息展示的相关接口"})
@RestController
@RequestMapping("items")
public class ItemsController extends BaseController {
    @Autowired
    private ItemService itemService;

    @ApiOperation(value = "查询商品详情", notes = "查询商品详情", httpMethod = "GET")
    @GetMapping("/info/{itemId}")
    public IMOOCJSONResult info(
            @ApiParam(name = "itemId", value = "商品 id", required = true)
            @PathVariable String itemId) {
        if(itemId == null) {
            return IMOOCJSONResult.errorMsg(null);
        }
        Items item = itemService.queryItemById(itemId);
        List<ItemsImg> itemsImgList = itemService.queryItemImgList(itemId);
        List<ItemsSpec> itemsSpecList = itemService.queryItemSpecList(itemId);
        ItemsParam itemsParam = itemService.queryItemParam(itemId);

        ItemInfoVO itemInfoVO = new ItemInfoVO();
        itemInfoVO.setItem(item);
        itemInfoVO.setItemsImgList(itemsImgList);
        itemInfoVO.setItemsSpecList(itemsSpecList);
        itemInfoVO.setItemsParam(itemsParam);

        return IMOOCJSONResult.ok(itemInfoVO);
    }

    @ApiOperation(value = "查询商品评价等级", notes = "查询商品评价等级", httpMethod = "GET")
    @GetMapping("/commentLevel")
    public IMOOCJSONResult commentLevel(
            @ApiParam(name = "itemId", value = "商品 id", required = true)
            @RequestParam String itemId) {
        if(itemId == null) {
            return IMOOCJSONResult.errorMsg(null);
        }
        CommentLevelCountsVO result = itemService.queryItemCommentCounts(itemId);
        return IMOOCJSONResult.ok(result);
    }

    @ApiOperation(value = "查询商品评论", notes = "查询商品评价等级", httpMethod = "GET")
    @GetMapping("/comments")
    public IMOOCJSONResult comments(
            @ApiParam(name = "itemId", value = "商品 id", required = true)
            @RequestParam String itemId,
            @ApiParam(name = "level", value = "评价等级", required = false)
            @RequestParam Integer level,
            @ApiParam(name = "page", value = "第几页", required = false)
            @RequestParam Integer page,
            @ApiParam(name = "pageSize", value = "每页显示条数", required = false)
            @RequestParam Integer pageSize
            ) {
        if(itemId == null) {
            return IMOOCJSONResult.errorMsg(null);
        }

        page = page == null ? 1 : page;
        pageSize = pageSize == null ? COMMON_PAGE_SIZE : pageSize;

        PagedGridResult result = itemService.queryPagedComments(itemId, level, page, pageSize);
        return IMOOCJSONResult.ok(result);
    }

    // 用于用户长时间未登陆网站，刷新购物车中得数据（主要是商品价格），类似京东淘宝
    @ApiOperation(value = "根据商品规格ids查找最新的商品数据", notes = "根据商品规格ids查找最新的商品数据", httpMethod = "GET")
    @GetMapping("/refresh")
    public IMOOCJSONResult refresh(
            @ApiParam(name = "itemSpecIds", value = "拼接的规格ids", required = true, example = "1001，1003，1005")
            @RequestParam String itemSpecIds
    ) {
        List<ShopcartVO> list = itemService.queryItemsBySpecIds(itemSpecIds);
        return IMOOCJSONResult.ok(list);
    }
}
