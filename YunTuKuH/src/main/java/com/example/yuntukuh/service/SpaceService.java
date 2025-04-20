package com.example.yuntukuh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yuntukuh.model.domain.Space;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.space.SpaceQueryRequest;
import com.example.yuntukuh.model.dto.space.spaceAddRequest;
import com.example.yuntukuh.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author dhjk1
* &#064;description  针对表【space(空间)】的数据库操作Service
* &#064;createDate  2025-03-31 17:24:13
 */
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     * @param spaceAddRequest 对象
     * @param loginUser 请求
     * @return 空间id
     */
    Long addSpace(spaceAddRequest spaceAddRequest, User loginUser);
    /**
     * 校验空间，用于更新和修改空间时进行判断
     * @param space 空间对象
     * @param add true 为新增时，false 为修改
     */
    void validSpace(Space space,boolean add);
    /**
     *获取单条空间包装类
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);
    /**
     * 获取多条空间包装分页类
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);
    /**
     把普通Java对象，转成mybatis需要的
     * 获取查询条件
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根基空间等级填充空间信息
     * @param space 对象
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 空间权限校验
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(Space space, User loginUser);
}
