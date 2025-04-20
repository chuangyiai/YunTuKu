<template>
  <div class="image-cropper">
    <a-modal
      class="image-cropper"
      v-model:visible="visible"
      title="编辑图片"
      :footer="false"
      @cancel="closeModal"
    >
      <vue-cropper
        ref="cropperRef"
        :img="imageUrl"
        :autoCrop="true"
        :fixedBox="false"
        :centerBox="true"
        :canMoveBox="true"
        :info="true"
        outputType="png"
      />
      <div style="margin-bottom: 16px" />
      <!-- 协同编辑操作 -->
      <div class="image-edit-actions" v-if="isTeamSpace">
        <a-space>
          <a-button v-if="editingUser" disabled>{{ editingUser.userName }}正在编辑</a-button>
          <a-button v-if="cnaEnterEdit" type="primary" ghost @click="EnterEdit">进入编辑</a-button>
          <a-button v-if="canExitEdit" danger @click="ExitEdit">退出编辑</a-button>
        </a-space>
      </div>
      <div style="margin-bottom: 16px" />
      <!-- 图片操作 -->
      <div class="image-cropper-actions">
        <a-space>
          <a-button @click="rotateLeft" :disabled="!canEdit">向左旋转</a-button>
          <a-button @click="rotateRight" :disabled="!canEdit">向右旋转</a-button>
          <a-button @click="changeScale(1)" :disabled="!canEdit">放大</a-button>
          <a-button @click="changeScale(-1)" :disabled="!canEdit">缩小</a-button>
          <a-button type="primary" :loading="loading" @click="handleConfirm" :disabled="!canEdit"
            >确认</a-button
          >
        </a-space>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watchEffect } from 'vue'
import { upLoadPictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import PictureEditWebSocket from '@/utils/PictureEditWebSocket.ts'
import { PICTURE_EDIT_ACTION_ENUM, PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constants/picture.ts'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'

interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
}
const props = defineProps<Props>()
//是否为团队空间
const isTeamSpace = computed(() => {
  return props.space?.spaceType === SPACE_TYPE_ENUM.TEAM
})
// 编辑器组件的引用
const cropperRef = ref()

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
  editPicture(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
  editPicture(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}

// 缩放
const changeScale = (num: number) => {
  cropperRef.value.changeScale(num)
  if (num > 0) {
    editPicture(PICTURE_EDIT_ACTION_ENUM.ZOOM_IN)
  } else {
    editPicture(PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
  }
}
const loading = ref<boolean>(false)

// 确认裁剪
const handleConfirm = () => {
  cropperRef.value.getCropBlob((blob: Blob) => {
    const fileName = (props.picture?.name || 'image') + '.png'
    const file = new File([blob], fileName, { type: blob.type })
    // 上传图片
    handleUpload({ file })
  })
}

/**
 * 上传
 * @param file
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    const res = await upLoadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    message.error('图片上传失败')
  } finally {
    loading.value = false
  }
}

// 是否可见
const visible = ref(false)

// 打开弹窗
const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
  if (websocket) {
    websocket.disconnect()
  }
  editingUser.value = undefined
}

// 暴露函数给父组件
defineExpose({
  openModal,
})
//实时编辑
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser
//正在编辑的用户
const editingUser = ref<API.UserVO>()
// 判断当前用户是否可进入编辑
const cnaEnterEdit = computed(() => {
  return !editingUser.value
})
//是否可退出编辑
const canExitEdit = computed(() => {
  return editingUser.value?.id === loginUser.id
})
//可以点击编辑图片的操作按钮
const canEdit = computed(() => {
  //不是团队空间默认可编辑
  if (!isTeamSpace.value) {
    return true
  }
  //是团队空间，只有编辑者可编辑
  return editingUser.value?.id === loginUser.id
})
//编写websocket逻辑
let websocket: PictureEditWebSocket | null
//初始化websocket
const initWebSocket = () => {
  const pictureId = props.picture?.id
  if (!pictureId || !visible.value) {
    return
  }
  //防止在之前的连接未释放
  if (websocket) {
    websocket.disconnect()
  }
  //创建websocket实例
  websocket = new PictureEditWebSocket(pictureId)
  //建立连接
  websocket.connect()

  //监听事件
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => {
    console.log('收到通知', msg)
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => {
    console.log('收到错误', msg)
    message.info(msg.message)
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => {
    console.log('收到进入编辑的通知', msg)
    message.info(msg.message)
    editingUser.value = msg.user
  })

  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg) => {
    console.log('收到编辑操作的通知', msg)
    message.info(msg.message)
    //根据编辑操作执行相应的操作
    switch (msg.editAction) {
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT:
        rotateLeft()
        break
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT:
        rotateRight()
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN:
        changeScale(1)
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT:
        changeScale(-1)
        break
    }
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg) => {
    console.log('收到退出编辑的通知', msg)
    message.info(msg.message)
    editingUser.value = undefined
  })
}
//
watchEffect(() => {
  // 只有在团队空间下才初始化websocket
  if (isTeamSpace.value) {
    initWebSocket()
  }
})
onUnmounted(() => {
  //关闭websocket连接
  if (websocket) {
    websocket.disconnect()
  }
  editingUser.value = undefined
})
const EnterEdit = () => {
  if (websocket) {
    //发送进入编辑的消息
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT,
    })
  }
}
//退出编辑
const ExitEdit = () => {
  if (websocket) {
    //发送退出编辑的消息
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT,
    })
  }
}
//编辑图片操作
const editPicture = (Action: string) => {
  if (websocket) {
    //发送编辑操作的消息
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION,
      editAction: Action,
    })
  }
}
</script>

<style scoped>
.image-cropper-actions {
  text-align: center;
}
.image-edit-actions {
  text-align: center;
}
.image-cropper .vue-cropper {
  height: 400px;
}
</style>
