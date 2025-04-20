package com.example.yuntukuh.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yuntukuh.model.domain.Picture;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.picture.*;
import com.example.yuntukuh.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author dhjk1
* &#064;description  针对表【picture(图片)】的数据库操作Service
* &#064;createDate  2025-03-15 19:31:11
 */
public interface PictureService extends IService<Picture> {
    /**
     * 图片上传
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO upLoadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 图片删除
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 图片编辑
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);


    /**
     *获取单条图片包装类
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取多条图片包装分页类
     * @param picturePage
     * @param request
     * @return
     */
     Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);
    /**
     * 校验图片，用于更新和修改图片时进行判断
     * @param picture
     */
    void validPicture(Picture picture);
     /**
     把普通Java对象，转成mybatis需要的
     * 获取查询条件
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片审核
     * @param pictureReviewRequest
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest,User loginUser);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 清理图片文件
     * @param oldPicture
     */
    void cleanPictureFile(Picture oldPicture);

    /**
     * 校验空间图片权限
     * @param loginUser 登录用户
     * @param picture  图片
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
}
