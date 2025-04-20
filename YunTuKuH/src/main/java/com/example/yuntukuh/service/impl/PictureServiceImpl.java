package com.example.yuntukuh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yuntukuh.constant.UserConstant;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.exception.ThrowUtils;
import com.example.yuntukuh.manager.CosManager;
import com.example.yuntukuh.manager.upload.FilePictureUpload;
import com.example.yuntukuh.manager.upload.PictureUploadTemplate;
import com.example.yuntukuh.manager.upload.UrlPictureUpload;
import com.example.yuntukuh.mapper.PictureMapper;
import com.example.yuntukuh.model.domain.Picture;
import com.example.yuntukuh.model.domain.Space;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.file.UploadPictureResult;
import com.example.yuntukuh.model.dto.picture.*;
import com.example.yuntukuh.model.enums.PictureReviewStatusEnum;
import com.example.yuntukuh.model.vo.PictureVO;
import com.example.yuntukuh.model.vo.UserVO;
import com.example.yuntukuh.service.PictureService;
import com.example.yuntukuh.service.SpaceService;
import com.example.yuntukuh.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.replaceAll;

/**
* @author dhjk1
* &#064;description  针对表【picture(图片)】的数据库操作Service实现
* &#064;createDate  2025-03-15 19:31:11
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

//    @Resource
//    private FileManager fileManager;
    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate  transactionTemplate;

    @Override
    public PictureVO upLoadPicture(Object inputSource,PictureUploadRequest pictureUploadRequest, User loginUser) {
        //校验参数，判断更新还是什么
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NO_AUTH_ERROR,"登陆后可上传");

        //校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId!=null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            //判断是否有空间权限,仅空间管理员可以上传
//            if(!loginUser.getId().equals(space.getUserId())){
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"仅空间管理员可以上传");
//            }
            //校验空间额度
            if (space.getTotalSize()>=space.getMaxSize()){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间已满");
            }
            if (space.getTotalCount()>=space.getMaxCount()){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间图片已满");
            }
        }
        //新增还是修改
        Long pictureid=null;
        if (pictureUploadRequest!=null){
            pictureid=pictureUploadRequest.getId();
        }
        if (pictureid!=null){
            Picture oldPicture = this.getById(pictureid);
            ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
            //仅本人或管理员可修改
            if (!Objects.equals(oldPicture.getUserId(), loginUser.getId()) && !Objects.equals(loginUser.getUserRole(), UserConstant.ADMIN_ROLE)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //校验空间是否一致，没传spaceId，则复用原有空间id（兼容公共图库）
            if (spaceId==null){
                if (oldPicture.getSpaceId()!=null){
                    spaceId=oldPicture.getSpaceId();
                }
            }else {
                //传了id，必须和原空间的一致
                if (ObjUtil.notEqual(spaceId,oldPicture.getSpaceId())){
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"图片空间不一致");
                }
            }
        }

        //上传图片，返回信息
        //上传路径前缀,按照用户id划分目录
        String upLoadPathPrefix;
        if (spaceId==null){
            //公共图库
            upLoadPathPrefix= String.format("public/%s",loginUser.getId());
        }else {
            //个人图库
            upLoadPathPrefix= String.format("space/%s",spaceId);
        }
        PictureUploadTemplate filePictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            filePictureUploadTemplate= urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = filePictureUploadTemplate.uploadPicture(inputSource, upLoadPathPrefix);
        //构造入库的图片信息(bean util)
        Picture picture=new Picture();
        picture.setSpaceId(spaceId);//指定空间id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        //补充图片名称
        String picName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank( pictureUploadRequest.getPictureName())){
            picName = pictureUploadRequest.getPictureName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        //补充审核参数
        this.fillReviewParams(picture,loginUser);
        //操作数据库
        //如果图片id不为空，新增。否则修改
        if(pictureid!=null){
            //更新。补充id和编辑时间
            picture.setId(pictureid);
            picture.setEditTime(new Date());
        }
        //开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            //插入数据库
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"图片上传失败，数据库操作失败");
            if(finalSpaceId!=null){
            //更新空间使用额度
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, finalSpaceId)
                    .setSql("totalSize=totalSize+" + picture.getPicSize())
                    .setSql("totalCount=totalCount+1")
                    .update();
            ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"空间额度更新失败");
            }
            return picture;
        });
        //如果是更新可以清理图片资源
//        if (result&&pictureid!=null){
//            Picture oldPicture = this.getById(pictureid);
//            this.cleanPictureFile(oldPicture);
//        }
        return PictureVO.objToVo(picture);
    }
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限，已改为使用注解鉴权
        //checkPictureAuth(loginUser, oldPicture);
        //开启事务
        Long finalSpaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            if(finalSpaceId!=null){
            //更新空间使用额度,释放额度
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, finalSpaceId)
                    .setSql("totalSize=totalSize- " + oldPicture.getPicSize())
                    .setSql("totalCount=totalCount-1")
                    .update();
            ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"空间额度更新失败");
           }
           return true;
        });
        // 异步清理文件
        this.cleanPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限，已改为使用注解鉴权
        //checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }




    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        //设置用户列表映射，1=》use1
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        //有就判断，没就算了
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startTime = pictureQueryRequest.getStartTime();
        Date endTime = pictureQueryRequest.getEndTime();


        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        //>=starttime
        queryWrapper.ge(ObjUtil.isNotEmpty(startTime), "editTime", startTime);
        //<=endtime
        queryWrapper.lt(ObjUtil.isNotEmpty(endTime), "editTime", endTime);


        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(pictureReviewRequest==null,ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatuseEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id==null || reviewStatuseEnum==null|| PictureReviewStatusEnum.REVIEWING.equals(reviewStatuseEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        //校验审核状态是否同步
        if (oldPicture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请勿重复审核");
        }
        Picture updataPicture=new Picture();
        BeanUtil.copyProperties(pictureReviewRequest,updataPicture);
        updataPicture.setReviewerId(loginUser.getId());
        updataPicture.setReviewTime(new Date());
        boolean result = this.updateById(updataPicture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
    }

    public void fillReviewParams(Picture picture,User loginUser){
        String userRole = loginUser.getUserRole();
        if (ObjUtil.equals(userRole, "admin")){
            //管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        }else {
            //非管理员无论是编辑还是更新都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }

    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //校验参数
        String searchText= pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        //获取前缀
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)){
            namePrefix=searchText;
        }
        ThrowUtils.throwIf(count>30,ErrorCode.PARAMS_ERROR,"最多30条");
        //抓取内容
        String fetchUrl= String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document  = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("抓取失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"抓取失败");
        }
        //解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"抓取失败");
        }
        Elements imgElementsList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementsList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)){
                log.info("图片地址为空,已跳过：{}",fileUrl);
                continue;}
            //处理图片地址，防止转义问题，只保留？号之前的
            int questcount= fileUrl.indexOf("?");
            if (questcount>-1){
                fileUrl=fileUrl.substring(0,questcount);
            }
            //上传图片
            PictureUploadRequest pictureUploadRequest=new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            if (StrUtil.isNotBlank(namePrefix)){
                pictureUploadRequest.setPictureName(namePrefix+(uploadCount+1));
            }

            try {
                PictureVO pictureVO = this.upLoadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功：id={}",pictureVO.getId());
                uploadCount++;
            }catch (Exception e){
                log.error("图片上传失败：fileUrl={}",fileUrl,e);
                continue;
            }
            if (uploadCount>=count){
                break;
            }
        }
        return uploadCount;
        }

    @Override
    @Async
    public void cleanPictureFile(Picture oldPicture) {
        //判断该图片是否被多条记录使用，如果没有使用则删除文件.
        String oldUrl=oldPicture.getUrl();
        Long count = this.lambdaQuery().eq(Picture::getUrl, oldUrl).count();
        if (count>1){
           return;
        }
        cosManager.deleteObject(oldUrl);
        //删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)){
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long userId = loginUser.getId();
        if (spaceId==null){
            //公共图片，仅管理员和本人可操作
            if (!ObjUtil.equals(loginUser.getUserRole(),"admin") && !ObjUtil.equals(userId,picture.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限");
            }
        }else {
            //私有图片，仅本人可操作
            if (!ObjUtil.equals(userId,picture.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限");
            }
        }
    }

    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        //获取校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList),ErrorCode.PARAMS_ERROR,"图片列表不能为空");
        ThrowUtils.throwIf(spaceId==null&&StrUtil.isBlank(category)&&CollUtil.isEmpty(tags),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NO_AUTH_ERROR,"用户不能为空");
        //校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        if (!ObjUtil.equals(space.getUserId(),loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限");
        }
        //查询指定图片
        List<Picture> pictureList = this.lambdaQuery()
        .select(Picture::getId, Picture::getSpaceId)
        .eq(Picture::getUserId, loginUser.getId())
        .in(Picture::getId, pictureIdList)
        .list();
        if (CollUtil.isEmpty(pictureList)){
            return ;
        }
        //更新图片信息
        pictureList.forEach(picture->{
            if (StrUtil.isNotBlank(category)){
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)){
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        //批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList,nameRule);
        //操作数据库
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"批量更新失败");
    }

    /**
     * 根据命名规则填充图片名称 格式：图片{序号}
     * @param pictureList 图片列表
     * @param nameRule 命名规则
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule)||CollUtil.isEmpty(pictureList)){
            return;
        }
        try {
            int count=1;
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}",String.valueOf(count++));
                picture.setName(pictureName);

            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"名称解析错误");
        }
    }

}




