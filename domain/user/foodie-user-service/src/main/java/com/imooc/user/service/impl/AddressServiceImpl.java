package com.imooc.user.service.impl;

import com.imooc.enums.YesOrNo;
import com.imooc.user.mapper.UserAddressMapper;
import com.imooc.user.pojo.UserAddress;
import com.imooc.user.pojo.bo.AddressBO;
import com.imooc.user.service.AddressService;
import org.n3r.idworker.Sid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private Sid sid;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<UserAddress> queryAll(String userId) {
        UserAddress ua = new UserAddress();
        ua.setUserId(userId);
        return userAddressMapper.select(ua);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void addNewUserAddress(AddressBO addressBO) {
        Integer isDefault = 0;

        // 1. 判断当前用户是否存在地址，如果没有，则新增为‘默认地址’
        List<UserAddress> addressList = this.queryAll(addressBO.getUserId());
        if (addressList == null) {
            isDefault = 1;
        }

        // 2. 保存地址到数据库
        String addressId = sid.nextShort();

        UserAddress newAddress = new UserAddress();
        BeanUtils.copyProperties(addressBO, newAddress);
        newAddress.setId(addressId);
        newAddress.setIsDefault(isDefault);
        newAddress.setCreatedTime(new Date());
        newAddress.setUpdatedTime(new Date());

        userAddressMapper.insert(newAddress);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateUserAddress(AddressBO addressBO) {

        String addressId = addressBO.getAddressId();

        UserAddress newAddress = new UserAddress();
        BeanUtils.copyProperties(addressBO, newAddress);
        newAddress.setId(addressId);
        newAddress.setUpdatedTime(new Date());

        userAddressMapper.updateByPrimaryKeySelective(newAddress);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteUserAddress(String userId, String addressId) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setId(addressId);

        userAddressMapper.delete(address);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateUserAddressToBeDefault(String userId, String addressId) {
        // 1. 查找默认地址，设置为不默认
        UserAddress queryAddress = new UserAddress();
        queryAddress.setUserId(userId);
        queryAddress.setIsDefault(YesOrNo.YES.type);
        List<UserAddress> list = userAddressMapper.select(queryAddress);
        // 这里用得到list是顺便更新一下DB里的错误数据
        for(UserAddress item : list) {
            item.setIsDefault(YesOrNo.NO.type);
            userAddressMapper.updateByPrimaryKeySelective(item);
        }

        // 2. 根据地址id修改为默认地址
        UserAddress defaultAddress = new UserAddress();
        defaultAddress.setIsDefault(YesOrNo.YES.type);
        defaultAddress.setUserId(userId);
        defaultAddress.setId(addressId);
        userAddressMapper.updateByPrimaryKeySelective(defaultAddress);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public UserAddress queryUserAddress(String userId, String addressId) {
        UserAddress queryAddress = new UserAddress();
        queryAddress.setId(addressId);
        queryAddress.setUserId(userId);

        return userAddressMapper.selectOne(queryAddress);
    }
}
