package com.example.yuntukuh.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void valiPicture(Object inputSource) {
        //转换格式
        String fileUrl = (String) inputSource;
        //校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR,"文件地址不能为空");
        //校验格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
        }
        //校验协议
        HttpResponse httpResponse=null;
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

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        //下载文件
        HttpUtil.downloadFile(fileUrl, file);
    }
}
