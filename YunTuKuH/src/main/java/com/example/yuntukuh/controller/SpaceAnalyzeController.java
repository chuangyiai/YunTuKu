package com.example.yuntukuh.controller;


import com.example.yuntukuh.annotation.AuthCheck;
import com.example.yuntukuh.common.BaseResponse;
import com.example.yuntukuh.common.ResultUtils;
import com.example.yuntukuh.constant.UserConstant;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.exception.ThrowUtils;
import com.example.yuntukuh.model.domain.Space;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.space.analyze.*;
import com.example.yuntukuh.model.vo.space.analyze.*;
import com.example.yuntukuh.service.PictureService;
import com.example.yuntukuh.service.SpaceAnalyzeService;
import com.example.yuntukuh.service.SpaceService;
import com.example.yuntukuh.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间分析接口
 */
@Slf4j
@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    /**
     * 获取空间使用状态
     */
    @PostMapping("/Usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> spaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null||request==null, ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse result = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 获取空间分类分析
     */
    @PostMapping("/Category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> spaceCategoryAnalyze(
            @RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null||request==null, ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> result = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 获取空间标签分析
     */
    @PostMapping("/Tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> spaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null||request==null, ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> result = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 获取空间图片大小分析
     */
    @PostMapping("/Size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> spaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null || request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> result = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 用户上传行为分析
     */
    @PostMapping("/User")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> spaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
                                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null || request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> result = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 空间使用排行
     */
    @PostMapping("/Rank")
    public BaseResponse<List<Space>> spaceUsageRank(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null || request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        User loginUser = userService.getLoginUser(request);
        List<Space> result = spaceAnalyzeService.getSpaceUsageRank(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }
}
