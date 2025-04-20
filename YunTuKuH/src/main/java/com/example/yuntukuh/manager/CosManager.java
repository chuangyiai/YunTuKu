package com.example.yuntukuh.manager;


import cn.hutool.core.io.FileUtil;
import com.example.yuntukuh.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;

import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private COSClient cosClient;
  
    // ... 一些操作 COS 的方法

// 将本地文件上传到 COS,String bucketName,
    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     *
     * @param key 输入所用的桶，和路径
     * @return COSObject对象，从中拿到文件的流
     */
    public COSObject getobject(String key){
        GetObjectRequest getObjectRequest=new GetObjectRequest(cosClientConfig.getBucket(),key);
        return cosClient.getObject(getObjectRequest);
    }
    /**
     * 上传解析对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        //缩略图规则(仅对大于20k的生成)
        if (file.length() > 20 * 1024) {
            //拼接缩略图路径
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail.webp";
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setFileId(thumbnailKey);
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

}
