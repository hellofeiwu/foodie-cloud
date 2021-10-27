package com.imooc.item.service.impl;

import com.github.pagehelper.PageHelper;
import com.imooc.enums.CommentLevel;
import com.imooc.enums.YesOrNo;
import com.imooc.item.mapper.*;
import com.imooc.item.pojo.*;
import com.imooc.item.pojo.vo.CommentLevelCountsVO;
import com.imooc.item.pojo.vo.ItemCommentVO;
import com.imooc.item.pojo.vo.ShopcartVO;
import com.imooc.item.service.ItemService;
import com.imooc.pojo.PagedGridResult;
import com.imooc.service.BaseService;
import com.imooc.utils.DesensitizationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@RestController
public class ItemServiceImpl extends BaseService implements ItemService {
    @Autowired
    private ItemsMapper itemsMapper;

    @Autowired
    private ItemsImgMapper itemsImgMapper;

    @Autowired
    private ItemsSpecMapper itemsSpecMapper;

    @Autowired
    private ItemsParamMapper itemsParamMapper;

    @Autowired
    private ItemsCommentsMapper itemsCommentsMapper;

    @Autowired
    private ItemsMapperCustom itemsMapperCustom;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Items queryItemById(String itemId) {
        return itemsMapper.selectByPrimaryKey(itemId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<ItemsImg> queryItemImgList(String itemId) {
        Example example = new Example(ItemsImg.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("itemId", itemId);
        return itemsImgMapper.selectByExample(example);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<ItemsSpec> queryItemSpecList(String itemId) {
        Example example = new Example(ItemsSpec.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("itemId", itemId);
        return itemsSpecMapper.selectByExample(example);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public ItemsParam queryItemParam(String itemId) {
        Example example = new Example(ItemsParam.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("itemId", itemId);
        return itemsParamMapper.selectOneByExample(example);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public CommentLevelCountsVO queryItemCommentCounts(String itemId) {
        Integer goodCount = getCommentCounts(itemId, CommentLevel.GOOD.level);
        Integer normalCount = getCommentCounts(itemId, CommentLevel.NORMAL.level);
        Integer badCount = getCommentCounts(itemId, CommentLevel.BAD.level);
        Integer total = goodCount + normalCount + badCount;

        CommentLevelCountsVO commentLevelCountsVO = new CommentLevelCountsVO();

        commentLevelCountsVO.setTotalCounts(total);
        commentLevelCountsVO.setGoodCounts(goodCount);
        commentLevelCountsVO.setNormalCounts(normalCount);
        commentLevelCountsVO.setBadCounts(badCount);

        return commentLevelCountsVO;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private Integer getCommentCounts(String itemId, Integer level) {
        ItemsComments itemsComments = new ItemsComments();
        itemsComments.setItemId(itemId);
        if (level != null) {
            itemsComments.setCommentLevel(level);
        }
        return itemsCommentsMapper.selectCount(itemsComments);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public PagedGridResult queryPagedComments(
            String itemId,
            Integer level,
            Integer page,
            Integer pageSize
    ) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId", itemId);
        map.put("level", level);

        // mybatis-pagehelper
        /**
         * page: 第几页
         * pageSize: 每页显示条数
         */
        PageHelper.startPage(page, pageSize);

        List<ItemCommentVO> list = itemsMapperCustom.queryItemComments(map);

        // 信息脱敏
        for (ItemCommentVO vo: list) {
            vo.setNickname(DesensitizationUtil.commonDisplay(vo.getNickname()));
        }

        return setterPagedGrid(list,page);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<ShopcartVO> queryItemsBySpecIds(String specIds) {
        String ids[] = specIds.split(",");
        List<String> specIdsList = new ArrayList<>();
        Collections.addAll(specIdsList, ids);

        return itemsMapperCustom.queryItemsBySpecIds(specIdsList);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public ItemsSpec queryItemSpecById(String specId) {
        return itemsSpecMapper.selectByPrimaryKey(specId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public String queryItemMainImgById(String itemId) {
        ItemsImg itemsImg = new ItemsImg();
        itemsImg.setItemId(itemId);
        itemsImg.setIsMain(YesOrNo.YES.type);
        ItemsImg result = itemsImgMapper.selectOne(itemsImg);
        return result != null ? result.getUrl() : "";
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void decreaseItemSpecStock(String specId, int buyCounts) {
        // synchronized 线程锁 不推荐使用，性能低下，集群下无作用
        // 锁数据库：不推荐，导致数据库性能低下
        // 分布式锁 zookeeper redis

        // lockUtil.getLock(); -- 加锁
        // 1. 查询库存
        // int stock = 10;

        // 2. 判断库存，是否会不够
        // if (stock - buyCounts < 0) {
            // 提示用户库存不够
            // 10 - 3 -3 - 5 = -1 多用户操作就会出现库存小于0的情况
        // }
        // lockUtil.unLock(); -- 解锁

        int result = itemsMapperCustom.decreaseItemSpecStock(specId, buyCounts);
        if (result != 1) {
            throw new RuntimeException("订单创建失败，原因：库存不足！");
        }
    }
}
