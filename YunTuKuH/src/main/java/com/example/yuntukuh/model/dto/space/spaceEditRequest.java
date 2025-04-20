package com.example.yuntukuh.model.dto.space;

import lombok.Data;
import lombok.Getter;

import java.io.Serializable;

/**
 * 编辑空间请求体
 */
@Data
public class spaceEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}

