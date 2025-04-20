package com.example.yuntukuh.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.exception.ThrowUtils;
import com.example.yuntukuh.mapper.SpaceMapper;
import com.example.yuntukuh.model.domain.Picture;
import com.example.yuntukuh.model.domain.Space;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.dto.space.analyze.*;
import com.example.yuntukuh.model.vo.space.analyze.*;
import com.example.yuntukuh.service.PictureService;
import com.example.yuntukuh.service.SpaceAnalyzeService;
import com.example.yuntukuh.service.SpaceService;
import com.example.yuntukuh.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author dhjk1
* &#064;description  针对表【space(空间)】的数据库操作Service实现
* &#064;createDate  2025-03-31 17:24:13
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceAnalyzeService{

    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //校验空间
        //公共图库或全空间从picture表查询
        if (spaceUsageAnalyzeRequest.isQueryAll()||spaceUsageAnalyzeRequest.isQueryPublic()){
            //校验权限
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR,"没有权限");
            //统计空间使用情况
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            //补充查询范围
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest,queryWrapper);
            List<Object> objects = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = objects.stream().mapToLong(obj -> (Long) obj).sum();
            long usedCount = objects.size();
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            //公共空间没有使用限制
            spaceUsageAnalyzeResponse.setMaxSize(null);
            //也没有比例
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        }else {
            //私有空间从space表查询
            ThrowUtils.throwIf(spaceUsageAnalyzeRequest.getSpaceId()==null||spaceUsageAnalyzeRequest.getSpaceId()<=0,ErrorCode.PARAMS_ERROR,"参数错误");
            Space space = spaceService.getById(spaceUsageAnalyzeRequest.getSpaceId());
            ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            //校验权限
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //统计空间使用情况
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            //计算比例
            double sizeUsageRatio = NumberUtil.round( space.getTotalSize() * 100.0 / space.getMaxSize(),2).doubleValue();
            double countUsageRatio = NumberUtil.round( space.getTotalCount() * 100.0 / space.getMaxCount(),2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 根据分析范围补充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 使用 MyBatis-Plus 分组查询
        queryWrapper.select("category AS category",
                        "COUNT(*) AS count",
                        "SUM(picSize) AS totalSize")
                .groupBy("category");
        // 查询并转换结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest==null,ErrorCode.PARAMS_ERROR,"参数错误");
        //鉴权
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //填充查询条件
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest,queryWrapper);
        //查询所以符合的标签
        queryWrapper.select("tags");
        List<String> tageJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        //转换为Map，key为标签，value为使用次数
        Map<String, Long> tageJsonMap = tageJsonList.stream()
                //扁平化，将【"tag1","tag2"]转换为tag1,tag2
                .flatMap(json -> JSONUtil.toList(json, String.class).stream())
                //根据tag分组，并统计每个tag的使用次数
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        //转换为SpaceTagAnalyzeResponse,并按使用次数排序
        //entrySet取到一个key-value对的集合，然后使用stream流进行排序
        return tageJsonMap.entrySet().stream()
                //Long.compare：java自带的排序，排序规则是按照value进行排序，降序排序
                .sorted((e1,e2)->Long.compare(e2.getValue(),e1.getValue()))
                //将entrySet转换为SpaceTagAnalyzeResponse，并返回
                .map(e->new SpaceTagAnalyzeResponse(e.getKey(),e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest==null||loginUser==null,ErrorCode.PARAMS_ERROR,"参数错误");
        //鉴权
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest,queryWrapper);
        //查询所以符合的图片大小
        queryWrapper.select("picSize");
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                //.filter(ObjUtil::isNotNull)
                .map(size-> (Long)size)
                .collect(Collectors.toList());
        //转换为Map，key为图片大小范围，value为使用次数.使用有序map（treemap，LinkedHashMap），保证顺序
        Map<String, Long> sizeRangeMap = new LinkedHashMap<>();
        sizeRangeMap.put("<100KB", picSizeList.stream().filter(size->size<100*1024).count());
        sizeRangeMap.put("100KB-500KB", picSizeList.stream().filter(size->size>=100*1024&&size<=500*1024).count());
        sizeRangeMap.put("500KB-1MB", picSizeList.stream().filter(size->size>500*1024&&size<=1024*1024).count());
        sizeRangeMap.put(">1MB", picSizeList.stream().filter(size->size>1024*1024).count());
        //转换为SpaceSizeAnalyzeResponse,并按使用次数排序
        return sizeRangeMap.entrySet().stream()
                .map(e->new SpaceSizeAnalyzeResponse(e.getKey(),e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest==null||loginUser==null,ErrorCode.PARAMS_ERROR,"参数错误");
        //鉴权
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest,queryWrapper);
        //补充用户ID查询
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId),"userId",userId);
        //补充分析维度。（每周，每日，每月）
        String analyzeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        ThrowUtils.throwIf(ObjUtil.isNull(analyzeDimension),ErrorCode.PARAMS_ERROR,"参数错误");
        switch (analyzeDimension){
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period","count(*) as count");
                break;
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') as period","count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') as period","count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数错误");
        }
        //分组排序
        queryWrapper.groupBy("period").orderByAsc("period");
        //查询
        List<Map<String, Object>> queryMaps = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryMaps
                .stream()
                .map(result->{
                    String period =  result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period,count);
                }).collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceUsageRank(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest==null||loginUser==null,ErrorCode.PARAMS_ERROR,"参数错误");
        //ThrowUtils.throwIf(!userService.isAdmin(loginUser),ErrorCode.NO_AUTH_ERROR,"没有权限");
        //构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","spaceName","userId","totalSize")
                //倒序查询
                .orderByDesc("totalSize")
                //只取前n个
                .last("limit "+spaceRankAnalyzeRequest.getTopN());
        //查询
        return spaceService.list(queryWrapper);
    }

    /**
     *校验空间分析权限
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
     Long spaceId = spaceAnalyzeRequest.getSpaceId();
     boolean isQueryPublic = spaceAnalyzeRequest.isQueryPublic();
     boolean isQueryAll = spaceAnalyzeRequest.isQueryAll();
    if (isQueryPublic || isQueryAll){
        //全空间分析或公共图库分析，校验仅管理员
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR,"没有权限");
    }else {
        //私有空间分析，校验本人
        ThrowUtils.throwIf(spaceId==null||spaceId<=0,ErrorCode.PARAMS_ERROR,"参数错误");
        Space spaceServiceById = spaceService.getById(spaceId);
        ThrowUtils.throwIf(spaceServiceById==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        spaceService.checkSpaceAuth(spaceServiceById,loginUser);
    }
    }

    /**
     * 根据请求对象,填充空间分析查询条件
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        //全空间分析
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        //公共图库分析
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        //私有空间分析
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }


}




