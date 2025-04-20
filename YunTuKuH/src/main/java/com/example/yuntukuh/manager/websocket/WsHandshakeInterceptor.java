package com.example.yuntukuh.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.example.yuntukuh.manager.auth.SpaceUserAuthManager;
import com.example.yuntukuh.manager.auth.model.SpaceUserPermissionConstant;
import com.example.yuntukuh.model.domain.Picture;
import com.example.yuntukuh.model.domain.Space;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.model.enums.SpaceTypeEnum;
import com.example.yuntukuh.service.PictureService;
import com.example.yuntukuh.service.SpaceService;
import com.example.yuntukuh.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket握手拦截器,连接前先校验
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 在握手之前执行该方法，如果返回false，则断开连接
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 获取用户信息
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 获取图片信息，放入到websocket的属性中
            String pictureId = httpServletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片参数，拒绝链接");
                return false;
            }
            // 获取当前用户信息，放入到websocket的属性中
            User loginUser = userService.getLoginUser(httpServletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝链接");
                return false;
            }
            // 校验用户是否具有编辑权限
            Picture picture = pictureService.getById(pictureId);
            if (picture==null) {
                log.error("图片不存在，拒绝链接");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space =null;
            if (spaceId!=null) {
                space = spaceService.getById(picture.getSpaceId());
                if (space==null){
                    log.error("图片所属空间不存在，拒绝链接");
                    return false;
                }
                if (space.getSpaceType()!= SpaceTypeEnum.TEAM.getValue()){
                    log.error("图片所属空间不是团队空间，拒绝链接");
                    return false;
                }
            }
            // 校验用户信息，是否具有编辑权限，sa-token
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            // 如果是团队空间的成员，且具有编辑者权限，则允许连接
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)){
                log.error("用户没有编辑权限，拒绝链接");
                return false;
            }
            //设置用户登录信息等属性到websocket 连接中
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.valueOf(pictureId));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}