package com.example.yuntukuh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.yuntukuh.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yuntukuh.model.dto.user.UserQueryRequest;
import com.example.yuntukuh.model.vo.LoginUserVO;
import com.example.yuntukuh.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author dhjk1
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-03-13 21:08:26
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     *
     * @param userPassword 明文密码
     * @return 加密密码
     */
    public String getEncryptPassword(String userPassword);
    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     *
     * @param user
     * @return 获取脱敏后的信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);
    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获得脱敏后的user对象
     * @param user
     * @return
     */
    UserVO getUserVO(User user);
    /**
     * 获得脱敏后的user对象列表
     * @param userlist
     * @return
     */
    List<UserVO> getUserVOlist(List<User> userlist);

    /**
     * 把普通Java对象，转成mybatis需要的
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);



    /**
     * 是否是管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}