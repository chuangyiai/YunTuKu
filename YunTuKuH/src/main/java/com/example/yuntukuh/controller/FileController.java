package com.example.yuntukuh.controller;


import com.example.yuntukuh.annotation.AuthCheck;
import com.example.yuntukuh.common.BaseResponse;
import com.example.yuntukuh.common.ResultUtils;
import com.example.yuntukuh.constant.UserConstant;
import com.example.yuntukuh.exception.BusinessException;
import com.example.yuntukuh.exception.ErrorCode;
import com.example.yuntukuh.manager.CosManager;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private CosManager cosManager;
/**
 * 测试文件上传
 *
 * @param multipartFile
 * @return
 */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }

    }
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/download")
    public void testDownload(String filePath, HttpServletResponse response) throws IOException {
        //定义输入流，方便关闭流
        COSObjectInputStream objectContent=null;
        try {
            //定义cos对象，获取文件路径
            COSObject cosObject=cosManager.getobject(filePath);
            //定义cos对象输入流获取对象内容
            objectContent = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(objectContent);
            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filePath);
            //写入响应
            response.getOutputStream().write(bytes);
            //刷新响应体
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error,filePath="+filePath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"下载失败");
        }finally {
            //关闭流
            if (objectContent!=null){
                objectContent.close();
            }
        }
    }

}
