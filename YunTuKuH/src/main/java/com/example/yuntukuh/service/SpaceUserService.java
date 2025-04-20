package com.example.yuntukuh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yuntukuh.model.domain.SpaceUser;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.spaceuser.SpaceUserAddRequest;
import com.example.yuntukuh.model.dto.spaceuser.SpaceUserQueryRequest;
import com.example.yuntukuh.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author dhjk1
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-04-14 15:35:49
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 创建空间成员
     * @param spaceUserAddRequest 对象
     * @return 空间id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);
    /**
     * 校验空间成员，用于更新和修改空间时进行判断
     * @param spaceUser 空间成员对象
     * @param add true 为新增时，false 为修改
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);
    /**
     *获取单条空间成员包装类
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);
    /**
     * 获取多条空间成员包装分页类
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
    /**
     把普通Java对象，转成mybatis需要的
     * 获取查询条件
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
