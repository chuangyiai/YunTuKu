package com.example.yuntukuh.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**已登录用户脱敏
 * 用户
 * @TableName user
 */

@Data
public class LoginUserVO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
    private String userPassword;
 */
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除

    private Integer isDelete;
     */
}