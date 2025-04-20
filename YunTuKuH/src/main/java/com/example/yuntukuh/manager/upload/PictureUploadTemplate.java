package com.example.yuntukuh.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.yuntukuh.config.CosClientConfig;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.manager.CosManager;
import com.example.yuntukuh.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 文件上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     */

    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix){
        //1.校验图片
        valiPicture(inputSource);
        //2.获取图片上传地址
        String uuid= RandomUtil.randomString(6);
        String Originalfilename = getOriginalFilename(inputSource);
        //获取时间戳DateUtil.formatDate(new Date())，获取随机数,获取文件名FileUtil.getSuffix(Originalfilename)，
        String uploadName=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,
                FileUtil.getSuffix(Originalfilename));
        String upLoadPath=String.format("/%s/%s",uploadPathPrefix,uploadName);
        //解析结果并返回，新建一个文件对象
        File file = null;
        try {
            // 3.创建临时文件，上传文件，创建临时空间
            file = File.createTempFile(upLoadPath, null);
            //处理对象来源
            processFile(inputSource,file);
            //4.上传对象，获取上传返回信息进行解析
            PutObjectResult putObjectResult = cosManager.putPictureObject(upLoadPath, file);
            //5.获取图片信息对象，封装返回结果
            //CIUploadResult 类用于返回图片处理结果信息originalInfo和processResults,（originalInfo）获取原始信息包括imageInfo原图图片信息，

            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //获取webp格式图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                //获取压缩后的结果
                CIObject ciObject = objectList.get(0);
                //获取压缩后的结果
                CIObject thumbnailObject =ciObject;
                if (objectList.size()>1){
                    thumbnailObject= objectList.get(1);//判断是否需要生成缩略图
                }
                //封装压缩图的返回结果
                return buildResult(Originalfilename,ciObject,thumbnailObject);
            }
            //返回给调用方
            return buildResult(imageInfo, upLoadPath, Originalfilename, file);
        } catch (Exception e) {
            log.error("图片上传对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //清理临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     *封装返回结果
     * @param originalfilename 原始文件名
     * @param ciObject  压缩后的对象
     * @param thumbnailObject 缩略图对象
     * @return
     */
    private UploadPictureResult buildResult(String originalfilename, CIObject ciObject , CIObject thumbnailObject) {

        int Pwidth = ciObject.getWidth();
        int Pheight = ciObject.getHeight();
        //计算宽高比，使用工具进行四舍五入，最后取出double值
        double scale= NumberUtil.round(Pwidth * 1.0 / Pheight ,2).doubleValue();
        //设置前缀+相对路经得到绝对路径，设置压缩后的地址
        UploadPictureResult uploadPictureResult=new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+ ciObject.getKey());
        //获取图片名
        uploadPictureResult.setPicName(FileUtil.mainName(originalfilename));
        //获取图片大小
        uploadPictureResult.setPicSize(ciObject.getSize().longValue());
        //获取图片宽度
        uploadPictureResult.setPicWidth(Pwidth);
        //获取图片高度
        uploadPictureResult.setPicHeight(Pheight);
        //获取图片比例
        uploadPictureResult.setPicScale(scale);
        //获取图片格式，直接从图片对象中得到
        uploadPictureResult.setPicFormat(ciObject.getFormat());
        //设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost()+"/"+ thumbnailObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     * @param imageInfo
     * @param upLoadPath
     * @param Originalfilename
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String upLoadPath, String Originalfilename, File file) {

        UploadPictureResult uploadPictureResult=new UploadPictureResult();
        int Pwidth = imageInfo.getWidth();
        int Pheight = imageInfo.getHeight();
        //计算宽高比，使用工具进行四舍五入，最后取出double值
        double scale= NumberUtil.round(Pwidth * 1.0 / Pheight ,2).doubleValue();
        //设置前缀+相对路经得到绝对路径
        uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+ upLoadPath);
        //获取图片名
        uploadPictureResult.setPicName(FileUtil.mainName(Originalfilename));
        //获取图片大小
        uploadPictureResult.setPicSize(FileUtil.size(file));
        //获取图片宽度
        uploadPictureResult.setPicWidth(Pwidth);
        //获取图片高度
        uploadPictureResult.setPicHeight(Pheight);
        //获取图片比例
        uploadPictureResult.setPicScale(scale);
        //获取图片格式，直接从图片对象中得到
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }

    /**
     * 校验输入源（本地或url）
     * @param inputSource
     */
    protected abstract void valiPicture(Object inputSource);

    /**
     * 获取输入源原始文件名称
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源生成临时文件
     * @param inputSource
     */
    protected abstract void processFile (Object inputSource,File file) throws Exception;



    /**
     * 清理临时文件
     * @param file
     */
    public  void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) log.error("file delete error, filepath = {}", file.getAbsolutePath());//获取文件绝对路径
    }

}
