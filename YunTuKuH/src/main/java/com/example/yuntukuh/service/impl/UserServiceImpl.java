package com.example.yuntukuh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.manager.auth.StpKit;
import com.example.yuntukuh.mapper.UserMapper;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.user.UserQueryRequest;
import com.example.yuntukuh.model.enums.UserRoleEnum;
import com.example.yuntukuh.model.vo.LoginUserVO;
import com.example.yuntukuh.model.vo.UserVO;
import com.example.yuntukuh.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.yuntukuh.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author dhjk1
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-03-13 21:08:25
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{



    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验
        if(StrUtil.hasBlank(userAccount,userPassword,checkPassword))
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        if(userAccount.length()<4)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户长度不低于4位");
        if (userPassword.length()<8||checkPassword.length()<8)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码不能低于8位");
        if (!userPassword.equals(checkPassword))
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次密码不一致");
        //2.是否重复
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);//前面查询的账号，后面输入的账号
        long count =this.baseMapper.selectCount(queryWrapper);
        if (count>0)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复");
        //3.密码加密
        String miwen=getEncryptPassword(userPassword);
        //4.插入
        User user=new User();
        user.setId(0L);
        user.setUserAccount(userAccount);
        user.setUserPassword(miwen);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveRequest=this.save(user);
        if (!saveRequest)
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"用户注册失败，数据库错误");
        return user.getId();
    }
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if(StrUtil.hasBlank(userAccount,userPassword))
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        if(userAccount.length()<4)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户错误");
        if (userPassword.length()<8)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码错误");
        //2.加密
        String miwen=getEncryptPassword(userPassword);
        //3.查询用户
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",miwen);
        User user= this.baseMapper.selectOne(queryWrapper);
        if (user==null) {
            log.info("user is null");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        //4.保存登录态
        //4.1 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        //4.2 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user==null)return null;
        LoginUserVO loginUserVo=new LoginUserVO();
        BeanUtil.copyProperties(user,loginUserVo);
        return loginUserVo;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //是否登录
        Object userobj= request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser=(User) userobj;
        if (userobj==null||currentUser.getId()==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userobj= request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userobj==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user==null)return null;
        UserVO userVO=new UserVO();
        BeanUtil.copyProperties(user,userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOlist(List<User> userlist) {
        if (CollectionUtil.isEmpty(userlist))
            return new ArrayList<>();
        /**
         * 将列表转成流，遍历每一个user，脱敏，最后在转成list
         map(user -> getUserVO(user)).
         */
        return userlist.stream().
                map(this::getUserVO).
                collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        if (user==null) return false;
        return UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
        //return user!=null&&UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }


    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "test";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

}




