package com.imooc.user.controller;

import com.imooc.controller.BaseController;
import com.imooc.pojo.IMOOCJSONResult;
import com.imooc.pojo.ShopcartBO;
import com.imooc.user.UserApplicationProperties;
import com.imooc.user.pojo.Users;

import com.imooc.user.pojo.bo.UserBO;
import com.imooc.user.pojo.vo.UsersVO;
import com.imooc.user.service.UserService;
import com.imooc.utils.CookieUtils;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.MD5Utils;
import com.imooc.utils.RedisOperator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Api(value = "注册登录", tags = {"用于注册登录的相关接口"})
@RestController
@RequestMapping("passport")
public class PassportController extends BaseController {
    @Autowired
    private RedisOperator redisOperator;

    @Autowired
    private UserService userService;

    @Autowired
    private UserApplicationProperties userApplicationProperties;

    @ApiOperation(value = "用户名是否存在", notes = "用户名是否存在", httpMethod = "GET")
    @GetMapping("/usernameIsExist")
    public IMOOCJSONResult usernameIsExist(@RequestParam String username) {
        // 1. 判断用户名不能为空
        if(StringUtils.isBlank(username)) {
            return IMOOCJSONResult.errorMsg("用户名不能为空");
        }

        // 2. 查找注册的用户名是否存在
        boolean isExist = userService.queryUsernameIsExist(username);
        if (isExist) {
            return IMOOCJSONResult.errorMsg("用户名已经存在");
        }

        // 3. 请求成功，用户名没有重复
        return IMOOCJSONResult.ok();
    }

    @ApiOperation(value = "用户注册", notes = "用户注册", httpMethod = "POST")
    @PostMapping("/regist")
    public IMOOCJSONResult regist(
            @RequestBody UserBO userBO,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (userApplicationProperties.isDisableRegistration()) {
            return IMOOCJSONResult.errorMsg("注册功能已关闭");
        }

        String username = userBO.getUsername();
        String password = userBO.getPassword();
        String confirmPwd = userBO.getConfirmPassword();

        // 0. 判断用户名和密码必须不为空
        if (StringUtils.isBlank(username) ||
            StringUtils.isBlank(password) ||
                StringUtils.isBlank(confirmPwd)
        ) {
            return IMOOCJSONResult.errorMsg("用户名或密码不能为空");
        }

        // 1. 查询用户名是否存在
        boolean isExist = userService.queryUsernameIsExist(username);
        if (isExist) {
            return IMOOCJSONResult.errorMsg("用户名已经存在");
        }

        // 2. 密码长度不能少于6位
        if (password.length() < 6) {
            return IMOOCJSONResult.errorMsg("密码长度不能少于6位");
        }

        // 3. 判断两次密码是否一致
        if (!password.equals(confirmPwd)) {
            return IMOOCJSONResult.errorMsg("两次密码输入不一致");
        }

        // 4. 实现注册
        Users user = userService.createUser(userBO);

        // 生成用户token，存入redis会话
        UsersVO usersVO = convertUsersVO(user);

        CookieUtils.setCookie(
                request,
                response,
                "user",
                JsonUtils.objectToJson(usersVO),
                true
                );

        // 同步购物车数据
        syncShopcartData(user.getId(), request, response);
        return IMOOCJSONResult.ok();
    }

    @ApiOperation(value = "用户登录", notes = "用户登录", httpMethod = "POST")
    @PostMapping("/login")
    public IMOOCJSONResult login(@RequestBody UserBO userBO,
                                 HttpServletRequest request,
                                 HttpServletResponse response
    ) throws Exception {
        String username = userBO.getUsername();
        String password = userBO.getPassword();

        // 0. 判断用户名和密码必须不为空
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return IMOOCJSONResult.errorMsg("用户名或密码不能为空");
        }

        // 1. 实现登录
        Users result = userService.queryUserForLogin(username, MD5Utils.getMD5Str(password));

        if (result == null) {
            return IMOOCJSONResult.errorMsg("用户名或密码不正确");
        }

        result.setPassword(null);
        // 生成用户token，存入redis会话
        UsersVO usersVO = convertUsersVO(result);

        CookieUtils.setCookie(
                request,
                response,
                "user",
                JsonUtils.objectToJson(usersVO),
                true
        );

        // 同步购物车数据
        syncShopcartData(result.getId(), request, response);
        return IMOOCJSONResult.ok(result);
    }

    @ApiOperation(value = "用户退出登录", notes = "用户退出登录", httpMethod = "POST")
    @PostMapping("/logout")
    public IMOOCJSONResult logout(@RequestParam String userId,
                                  HttpServletRequest request,
                                  HttpServletResponse response
                                  ) {

        // 清除带有用户信息的cookie
        CookieUtils.deleteCookie(request, response, "user");

        // 清除redis中的token
        redisOperator.del(REDIS_USER_TOKEN + ":" + userId);

        // 用户退出登录，需要清空购物车
        CookieUtils.deleteCookie(request, response, FOODIE_SHOPCART);

        return IMOOCJSONResult.ok();
    }

    @GetMapping("/setSession")
    public Object setSession(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute("userInfo", "new user");
        session.setMaxInactiveInterval(3600);
        session.getAttribute("userInfo");
        return "ok";
    }

    /**
     * 注册登录成功后，同步cookie和redis中的购物车数据
     */
    private void syncShopcartData(String userId, HttpServletRequest request, HttpServletResponse response) {
        String shopcartJsonRedis = redisOperator.get("shopcart:" + userId);
        String shopcartStrCookie = CookieUtils.getCookieValue(request, FOODIE_SHOPCART);

        if (StringUtils.isBlank(shopcartJsonRedis)) {
            if (StringUtils.isNotBlank(shopcartStrCookie)) {
                redisOperator.set("shopcart:" + userId, shopcartStrCookie);
            }
        }else {
            if (StringUtils.isNotBlank(shopcartStrCookie)) {
                List<ShopcartBO> shopcartBOListRedis = JsonUtils.jsonToList(shopcartJsonRedis, ShopcartBO.class);
                List<ShopcartBO> shopcartBOListCookie = JsonUtils.jsonToList(shopcartStrCookie, ShopcartBO.class);

                List<ShopcartBO> pendingDeleteList = new ArrayList<>();

                for (ShopcartBO redisItem : shopcartBOListRedis) {
                    for (ShopcartBO cookieItem : shopcartBOListCookie) {
                        if (cookieItem.getSpecId().equals(redisItem.getSpecId())) {
                            // Cookie覆盖redis购买数量，不累加，参考京东
                            redisItem.setBuyCounts(cookieItem.getBuyCounts());
                            pendingDeleteList.add(cookieItem);
                        }
                    }
                }

                // 从现有cookie中删除 交集中的商品
                shopcartBOListCookie.removeAll(pendingDeleteList);

                // 合并两个list
                shopcartBOListRedis.addAll(shopcartBOListCookie);

                // 更新到 redis 和 cookie
                redisOperator.set("shopcart:" + userId, JsonUtils.objectToJson(shopcartBOListRedis));
                CookieUtils.setCookie(request, response, FOODIE_SHOPCART, JsonUtils.objectToJson(shopcartBOListRedis), true);

            }else {
                CookieUtils.setCookie(request, response, FOODIE_SHOPCART, shopcartJsonRedis, true);
            }
        }

    }

    private UsersVO convertUsersVO(Users user) {
        String uniqueToken = UUID.randomUUID().toString().trim(); // 这里加trim是因为第三方工具有时候会在结尾加空格
        redisOperator.set(REDIS_USER_TOKEN + ":" + user.getId(), uniqueToken);
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(user, usersVO);
        usersVO.setUniqueToken(uniqueToken);
        return usersVO;
    }
}
