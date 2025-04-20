package com.example.yuntukuh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yuntukuh.model.domain.Picture;
import com.example.yuntukuh.model.domain.Space;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.space.analyze.*;
import com.example.yuntukuh.model.vo.space.analyze.*;

import java.util.List;

/**
 * 空间分析实现类
 */
public interface SpaceAnalyzeService extends IService<Space> {
    /**
     * 获取空间使用情况分析
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间分类分析
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间标签分析
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片大小分析
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 用户上传行为分析
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间使用排行（仅管理）
     */
    List<Space> getSpaceUsageRank(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}

