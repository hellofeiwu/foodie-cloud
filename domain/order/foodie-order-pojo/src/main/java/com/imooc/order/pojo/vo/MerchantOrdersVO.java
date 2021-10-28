package com.imooc.order.pojo.vo;



import com.imooc.pojo.ShopcartBO;

import java.util.List;

public class MerchantOrdersVO {
    private String merchantOrderId;
    private String merchantUserId;
    private Integer amount;
    private Integer payMethod;
    private String returnUrl;
    private List<ShopcartBO> toBeRemovedShopcartList;

    public String getMerchantOrderId() {
        return merchantOrderId;
    }

    public void setMerchantOrderId(String merchantOrderId) {
        this.merchantOrderId = merchantOrderId;
    }

    public String getMerchantUserId() {
        return merchantUserId;
    }

    public void setMerchantUserId(String merchantUserId) {
        this.merchantUserId = merchantUserId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(Integer payMethod) {
        this.payMethod = payMethod;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public List<ShopcartBO> getToBeRemovedShopcartList() {
        return toBeRemovedShopcartList;
    }

    public void setToBeRemovedShopcartList(List<ShopcartBO> toBeRemovedShopcartList) {
        this.toBeRemovedShopcartList = toBeRemovedShopcartList;
    }
}
