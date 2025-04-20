package com.example.yuntukuh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.exception.ThrowUtils;
import com.example.yuntukuh.manager.sharding.DynamicShardingManager;
import com.example.yuntukuh.mapper.SpaceMapper;
import com.example.yuntukuh.model.domain.Space;
import com.example.yuntukuh.model.domain.SpaceUser;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.space.SpaceQueryRequest;
import com.example.yuntukuh.model.dto.space.spaceAddRequest;
import com.example.yuntukuh.model.enums.SpaceLevelEnum;
import com.example.yuntukuh.model.enums.SpaceRoleEnum;
import com.example.yuntukuh.model.enums.SpaceTypeEnum;
import com.example.yuntukuh.model.vo.SpaceVO;
import com.example.yuntukuh.model.vo.UserVO;
import com.example.yuntukuh.service.SpaceService;
import com.example.yuntukuh.service.SpaceUserService;
import com.example.yuntukuh.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author dhjk1
* &#064;description  针对表【space(空间)】的数据库操作Service实现
* &#064;createDate  2025-03-31 17:24:13
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    /**spring提供
     * 编程式事务管理器
     */
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

//    @Resource
//    @Lazy
//    //private DynamicShardingManager dynamicShardingManager;

    @Override
    public Long addSpace(spaceAddRequest spaceAddRequest, User loginUser) {
        //填充默认值
        Space space = new Space();
        //转换实体类和DTO
        BeanUtil.copyProperties(spaceAddRequest, space);
        if (space.getSpaceName()==null){
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel()==null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType()==null){
            space.setSpaceLevel(SpaceTypeEnum.PRIVATE.getValue());
        }
        //填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 校验
        this.validSpace(space,true);
        //校验权限
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue()!=space.getSpaceLevel()&&!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"仅管理员可创建高级空间");
        }
        //一个账户一个私有空间，以及一个团队空间
        //根据用户id生成锁,intern() 方法用于在运行时将字符串添加到内部的字符串池中，并返回字符串池中的引用
        //取到不同string对象的同一值
        String lock = String.valueOf(userId).intern();
        //使用本地锁，加锁
        synchronized (lock){
            return transactionTemplate.execute(status -> {
                //判断是否已经创建过空间
                //exists判断有没有符合条件的记录，one返回结果集的第一个元素
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType,space.getSpaceType())
                        .exists();
                //如果有就不能再创建
                ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "每个用户每类空间只能创建一个");
                //保存
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间创建失败");
                //成功，返回id
                //如果是团队空间关联新增成员记录（将创建人加入设置为admin）
                if (SpaceTypeEnum.TEAM.getValue()==space.getSpaceType()){
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());

                    result = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"空间成员添加失败");
                }
                //创建分表
                //dynamicShardingManager.createSpacePictureTable(space);
                //返回id
                return space.getId();
            });
        }
    }

    @Override
    public void validSpace(Space space,boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间名不能为空");
        //从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        //如果是新增，判断是否为空
        if (add) {
            if (StrUtil.isBlank(spaceName)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名不能为空");
            }
            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间等级不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间类型不能为空");
            }
        }
        //修改数据时，空间名不能为空，有参数则校验,有就判断，没就算了
        if (StrUtil.isNotBlank(spaceName)&&spaceName.length()>30){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名不能超过30个字符");
        }
        //修改时，空间级别判断
        if (spaceLevel != null && spaceLevelEnum==null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间等级不存在");
        }
        if (spaceType != null && spaceTypeEnum==null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间类别不存在");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        //设置用户列表映射，1=》use1
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();



        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            //如果管理员指定，按管理员来
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize()==null){
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount()==null){
                space.setMaxCount(maxCount);
            }
        }

    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        // 判断仅本人或管理员可编辑
        if (!space.getUserId().equals(loginUser.getId())&& !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限");
        }
    }
}




