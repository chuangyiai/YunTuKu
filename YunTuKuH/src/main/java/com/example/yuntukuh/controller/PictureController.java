package com.example.yuntukuh.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yuntukuh.annotation.AuthCheck;
import com.example.yuntukuh.api.imageSearch.ImageSearchApiFacade;
import com.example.yuntukuh.api.imageSearch.model.ImageSearchResult;
import com.example.yuntukuh.common.BaseResponse;
import com.example.yuntukuh.common.DeleteRequest;
import com.example.yuntukuh.common.ResultUtils;
import com.example.yuntukuh.constant.UserConstant;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.exception.ThrowUtils;
import com.example.yuntukuh.manager.auth.SpaceUserAuthContext;
import com.example.yuntukuh.manager.auth.SpaceUserAuthManager;
import com.example.yuntukuh.manager.auth.StpKit;
import com.example.yuntukuh.manager.auth.annotation.SaSpaceCheckPermission;
import com.example.yuntukuh.manager.auth.model.SpaceUserPermissionConstant;
import com.example.yuntukuh.model.domain.Picture;
import com.example.yuntukuh.model.domain.Space;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.picture.*;
import com.example.yuntukuh.model.enums.PictureReviewStatusEnum;
import com.example.yuntukuh.model.vo.PictureTagCategory;
import com.example.yuntukuh.model.vo.PictureVO;
import com.example.yuntukuh.service.PictureService;
import com.example.yuntukuh.service.SpaceService;
import com.example.yuntukuh.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;
    /**
     * 引入redis操作对象
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    /**
     * 本地缓存对象
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000L)
            // 缓存 5 分钟移除
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build();
    /**
 * 文件上传(可重复)
 *
 * @param multipartFile 文件
 * @return 脱敏信息
 */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> upLoadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest, HttpServletRequest request){
       //判断登录
        User loginuser= userService.getLoginUser(request);
       //调用上传方法
        PictureVO pictureVO= pictureService.upLoadPicture(multipartFile,pictureUploadRequest,loginuser);

        return ResultUtils.success(pictureVO);
    }
    /**
     * 通过URL文件上传(可重复)
     *
     * @param pictureUploadRequest 文件url
     * @return 信息
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> upLoadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request){
        //判断登录
        User loginuser= userService.getLoginUser(request);
        //调用上传方法
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO= pictureService.upLoadPicture(fileUrl,pictureUploadRequest,loginuser);

        return ResultUtils.success(pictureVO);
    }
    /**
     * 删除图片
     * @param deleteRequest id
     * @param request user
     * @return 信息
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        //if( deleteRequest==Null || deleteRequest.getId()<=0)
        ThrowUtils.throwIf(deleteRequest==null||deleteRequest.getId()<=0, ErrorCode.PARAMS_ERROR);
        User userLogin=userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        if (id==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"请求数据不存在");
        }
        pictureService.deletePicture(id,userLogin);
        return ResultUtils.success(true);
    }
    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        //spring的BeanUtil.copyProperties copy第一个参数
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        //补充审核参数
        pictureService.fillReviewParams(picture,loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR,"id不对");
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR,"图片为空");
        //空间权限验证,已改用sa-token注解鉴权
        Long spaceId = picture.getSpaceId();
        Space space=null;
        User loginUser=userService.getLoginUser(request);
        if (spaceId!=null){
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.FORBIDDEN_ERROR,"没有权限");
            space = spaceService.getById(spaceId);
//            pictureService.checkPictureAuth(loginUser,picture);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        }
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类,将对象脱敏
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //普通用户只能查审核通过的
        //pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //空间权限验证
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId==null){
            //公开图库，普通用户只能查审核通过的
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.FORBIDDEN_ERROR,"没有权限");
            //私有图库，普通用户只能查自己的,已改为使用注解鉴权
//            User loginUser = userService.getLoginUser(request);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(false,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
//            if (!space.getUserId().equals(loginUser.getId())){
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限");
//            }

        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        pictureService.editPicture(pictureEditRequest, userService.getLoginUser(request));
        return ResultUtils.success(true);
    }

    /**
     * 前端展示的默认标签
     * @return json
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片（封装类）
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                         HttpServletRequest request) {
       if (pictureReviewRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
       }
       User loginUser = userService.getLoginUser(request);
       pictureService.doPictureReview(pictureReviewRequest,loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 图片批量抓取
     * @param pictureUploadByBatchRequest 封装类
     * @param request user
     * @return 上传数量
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**使用本地缓存，redis缓存的多级缓存机制
     * 分页获取图片列表（封装类）
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //普通用户只能查审核通过的
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //查询缓存，如果有就直接返回
        String queryCondition =JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtil.md5Hex(queryCondition);
        String cachedKey = String.format("pictureDemo:listPictureVOByPage:%s", hashKey);
        //1.先用本地缓存查询
        String cachedValue = LOCAL_CACHE.getIfPresent(cachedKey);
        if (cachedValue!= null) {
            // 如果缓存中有数据，直接返回。使用json工具类将json字符串转为Page对象
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        //2.如果本地缓存没有，则查询redis分布式缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cachedKey);
        if (cachedValue!= null) {
            // 如果缓存中有数据，更新本地缓存并返回。使用json工具类将json字符串转为Page对象
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        //3.查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        //4.更新缓存
        //存入redis
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //设置过期时间,5-10分钟过期,防止缓存雪崩
        int cacheTime=300+ RandomUtil.randomInt(0,300);
        opsForValue.set(cachedKey, cacheValue,cacheTime, TimeUnit.SECONDS);
        //写入本地缓存
        LOCAL_CACHE.put(cachedKey, cacheValue);
        // 获取封装类
        return ResultUtils.success(pictureVOPage);
    }
    /**
     * 以图搜图
     * @param searchPictureByPictureRequest user
     * @return 上传数量
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
            @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null||pictureId<=0, ErrorCode.PARAMS_ERROR);
        Picture pictureServiceById = pictureService.getById(pictureId);
        ThrowUtils.throwIf(pictureServiceById == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(pictureServiceById.getUrl());
        return ResultUtils.success(imageSearchResults);
    }
    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(
    @RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }
}
