package com.example.yuntukuh.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.example.yuntukuh.manager.websocket.disruptor.PictureEditEventProducer;
import com.example.yuntukuh.manager.websocket.model.PictureEditActionEnum;
import com.example.yuntukuh.manager.websocket.model.PictureEditMessageTypeEnum;
import com.example.yuntukuh.manager.websocket.model.PictureEditRequestMessage;
import com.example.yuntukuh.manager.websocket.model.PictureEditResponseMessage;
import com.example.yuntukuh.model.domain.User;
import com.example.yuntukuh.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑webSocket处理器
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;


    /**
     * 建立连接成功后，处理消息
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //保存会话到集合
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        //构造响应消息，发送加入编辑的通知
        PictureEditResponseMessage pictureEditRequestMessage = new PictureEditResponseMessage();
        pictureEditRequestMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message= String.format("用户 %s 加入编辑", user.getUserName());
        pictureEditRequestMessage.setMessage(message);
        pictureEditRequestMessage.setUser(userService.getUserVO(user));
        //广播消息,发送通知给所有用户
        broadcastToPicture(pictureId, pictureEditRequestMessage);
    }

    /**
     * 处理收到的前端消息,根据消息类型，进行不同的处理
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        //获取消息，将JSON转化为PictureEditRequestMessage
        PictureEditRequestMessage bean = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
       //从session中获取pictureId和user
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");
        //根据消息类型，进行不同的处理（生产消息到disruptor环形队列中）
        pictureEditEventProducer.publishEvent(bean, session, user,pictureId );

    }

    /**
     * 进入编辑状态
     * @param bean
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleEnterEditMessage(PictureEditRequestMessage bean, WebSocketSession session, Long pictureId, User user) throws IOException {
        //没有用户正在编辑，则设置编辑状态
        if (!pictureEditingUsers.containsKey(pictureId)){
            //设置编辑状态
            pictureEditingUsers.put(pictureId,user.getId());
            //构造响应消息，发送加入编辑的通知
            PictureEditResponseMessage pictureEditRequestMessage = new PictureEditResponseMessage();
            pictureEditRequestMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message= String.format("用户 %s 进入编辑", user.getUserName());
            pictureEditRequestMessage.setMessage(message);
            pictureEditRequestMessage.setUser(userService.getUserVO(user));
            //广播消息,发送通知给所有用户
            broadcastToPicture(pictureId, pictureEditRequestMessage);
        }
    }

    /**
     * 编辑操作状态
     * @param bean
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleEditActionMessage(PictureEditRequestMessage bean, WebSocketSession session, Long pictureId, User user) throws IOException {
        //构造响应消息，发送编辑操作的通知
        //获取正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        //获取操作
        String editAction = bean.getEditAction();
        //获取操作枚举
        PictureEditActionEnum actionEnum= PictureEditActionEnum.getEnumByValue(editAction);
        if(editAction == null){
            log.error("无效编辑动作");
            return;
        }
        //确实当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())){
            //构造响应消息，发送编辑操作的通知
            PictureEditResponseMessage pictureEditRequestMessage = new PictureEditResponseMessage();
            pictureEditRequestMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message= String.format("用户 %s 执行 %s", user.getUserName(), actionEnum.getText());
            pictureEditRequestMessage.setMessage(message);
            pictureEditRequestMessage.setEditAction(editAction);
            pictureEditRequestMessage.setUser(userService.getUserVO(user));
            //广播消息,发送通知给出自己外的用户
            broadcastToPicture(pictureId, pictureEditRequestMessage,session);
        }
    }

    /**
     * 退出编辑状态
     * @param bean
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleExitEditMessage(PictureEditRequestMessage bean, WebSocketSession session, Long pictureId, User user) throws IOException {
        //确实当前编辑者
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())){
            //移除编辑状态
            pictureEditingUsers.remove(pictureId);
            //构造响应消息，发送退出编辑的通知
            PictureEditResponseMessage pictureEditRequestMessage = new PictureEditResponseMessage();
            pictureEditRequestMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message= String.format("用户 %s 退出编辑", user.getUserName());
            pictureEditRequestMessage.setMessage(message);
            pictureEditRequestMessage.setUser(userService.getUserVO(user));
            //广播消息,发送通知给所有用户
            broadcastToPicture(pictureId, pictureEditRequestMessage);
        }
    }
    /**
     * 关闭连接
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        //移除当前用户编辑状态
        //从session中获取pictureId和user
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");
        handleExitEditMessage(null,session,pictureId,user);
        //删除会话
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (webSocketSessions!= null){
            webSocketSessions.remove(session);
            if (webSocketSessions.isEmpty()){
                pictureSessions.remove(pictureId);
            }
        }
        //通知其他用户，该用户已离开
        PictureEditResponseMessage pictureEditRequestMessage = new PictureEditResponseMessage();
        pictureEditRequestMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message= String.format("用户 %s 已离开", user.getUserName());
        pictureEditRequestMessage.setMessage(message);
        pictureEditRequestMessage.setUser(userService.getUserVO(user));
        //广播消息,发送通知给所有用户
        broadcastToPicture(pictureId, pictureEditRequestMessage);
    }

    /**
     * 广播消息给所有连接的用户,支持排除session
     * @param pictureId
     * @param pictureEditRequestMessage
     * @param excludeSession
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditRequestMessage,WebSocketSession excludeSession) throws IOException {
        // 获取当前图片的所有会话
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(webSocketSessions)){
            // 将消息转换为JSON字符串, 并创建TextMessage对象,用于发送消息
            // 解决Long类型精度丢失的问题,创建objectMapper对象
            ObjectMapper objectMapper =new ObjectMapper();
            // 配置序列化器，将Long转化为string，解决精度丢失的问题，
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            // 将pictureEditRequestMessage对象转换为JSON字符串
            String str = objectMapper.writeValueAsString(pictureEditRequestMessage);
            TextMessage tx = new TextMessage(str);
            for (WebSocketSession webSocketSession : webSocketSessions) {
                //排除掉的session不发送
                if (excludeSession != null && excludeSession.equals(webSocketSession)) {
                    continue;
                }
                if (webSocketSession.isOpen()){
                    webSocketSession.sendMessage(tx);
                }
            }
        }
    }

    /**
     * 广播消息给所有连接的用户
     * @param pictureId
     * @param pictureEditRequestMessage
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditRequestMessage) throws IOException {
        broadcastToPicture(pictureId,pictureEditRequestMessage,null);
    }
}
