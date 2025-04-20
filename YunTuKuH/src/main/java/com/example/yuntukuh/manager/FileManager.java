package com.example.yuntukuh.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.example.yuntukuh.config.CosClientConfig;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.exception.ThrowUtils;
import com.example.yuntukuh.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
/**
 * 文件服务
 * @deprecated 已废弃，改为使用 upload 包的模板方法优化
 */
@Deprecated
public class FileManager {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     */

    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix){
        //校验图片
        valiPicture(multipartFile);
        //图片上传地址
        String uuid= RandomUtil.randomString(6);
        String Originalfilename = multipartFile.getOriginalFilename();
        //获取时间戳DateUtil.formatDate(new Date())，获取随机数,获取文件名FileUtil.getSuffix(Originalfilename)，
        String uploadName=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,
                FileUtil.getSuffix(Originalfilename));
        String upLoadPath=String.format("/%s/%s",uploadPathPrefix,uploadName);
        //解析结果并返回，新建一个文件对象
        File file = null;
        try {
            // 上传文件，创建临时空间
            file = File.createTempFile(upLoadPath, null);
            multipartFile.transferTo(file);
            //获取上传返回信息进行解析
            PutObjectResult putObjectResult = cosManager.putPictureObject(upLoadPath, file);
            //获取图片信息对象
            //CIUploadResult 类用于返回图片处理结果信息originalInfo和processResults,（originalInfo）获取原始信息包括imageInfo原图图片信息，
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            UploadPictureResult uploadPictureResult=new UploadPictureResult();
            int Pwidth = imageInfo.getWidth();
            int Pheight = imageInfo.getHeight();
            //计算宽高比，使用工具进行四舍五入，最后取出double值
            double scale= NumberUtil.round(Pwidth * 1.0 / Pheight ,2).doubleValue();
            //设置前缀+相对路经得到绝对路径
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+upLoadPath);
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
            //返回给调用方
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //清理临时文件
            this.deleteTempFile(file);
        }
    }



    /**
     * 校验文件（图片）
     * @param multipartFile
     */
    public void valiPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile==null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        //校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M=1024*1024;//1024k*1024b
        ThrowUtils.throwIf(fileSize>2*ONE_M,ErrorCode.PARAMS_ERROR,"图片过大，图片大小不超过2M");
        //校验文件格式（后缀）,getSuffix获取后缀，getOriginalFilename获取源文件名
        String Filesuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //允许上传的文件后缀列表（集合）
        final List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpg","png","webp","jpeg");
        //contains判断指定内容中是否包含括号中的内容
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(Filesuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

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
    /**
     * 通过url上传图片
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix){
        //校验图片
        valiPicture(fileUrl);
        //图片上传地址
        String uuid= RandomUtil.randomString(6);
        //hutu工具类自动去除后缀
        String Originalfilename=FileUtil.mainName(fileUrl);
        //获取时间戳DateUtil.formatDate(new Date())，获取随机数,获取文件名FileUtil.getSuffix(Originalfilename)，
        String uploadName=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,
                FileUtil.getSuffix(Originalfilename));
        String upLoadPath=String.format("/%s/%s",uploadPathPrefix,uploadName);
        //解析结果并返回，新建一个文件对象
        File file = null;
        try {
            // 上传文件，创建临时空间
            file = File.createTempFile(upLoadPath, null);
            HttpUtil.downloadFile(fileUrl,file);
            //获取上传返回信息进行解析
            PutObjectResult putObjectResult = cosManager.putPictureObject(upLoadPath, file);
            //获取图片信息对象
            //CIUploadResult 类用于返回图片处理结果信息originalInfo和processResults,（originalInfo）获取原始信息包括imageInfo原图图片信息，
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            UploadPictureResult uploadPictureResult=new UploadPictureResult();
            int Pwidth = imageInfo.getWidth();
            int Pheight = imageInfo.getHeight();
            //计算宽高比，使用工具进行四舍五入，最后取出double值
            double scale= NumberUtil.round(Pwidth * 1.0 / Pheight ,2).doubleValue();
            //设置前缀+相对路经得到绝对路径
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+upLoadPath);
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
            //返回给调用方
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //清理临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 根据url校验文件
     * @param fileUrl
     */
    private void valiPicture(String fileUrl) {
        //校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl),ErrorCode.PARAMS_ERROR,"文件地址为空");
        //校验格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
        }
        //校验协议
        HttpResponse httpResponse=null;
        ThrowUtils.throwIf(!fileUrl.startsWith("http://")&& !fileUrl.startsWith("https://"),ErrorCode.PARAMS_ERROR,"仅支持http和https的文件");
        try {
            //发送head请求，判断文件是否存在
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            //未正常返回，无需执行其他判断
            if (httpResponse.getStatus()!= HttpStatus.HTTP_OK){
                return;
            }
            //若存在校验文件类型
            String contentType = httpResponse.header("Content-Type");
            //不为空才校验合法，规则相对宽松
            if (StrUtil.isNotBlank(contentType)){
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //校验文件大小
            String contentLenghtStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLenghtStr)){
                try {
                    long contentLength = Long.parseLong(contentLenghtStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        }finally {
            //释放资源
            if (httpResponse!=null){
                httpResponse.close();
            }
        }

    }
}
