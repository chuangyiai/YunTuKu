<script setup lang="ts">
import PicutreUpLoad from '@/components/PicutreUpLoad.vue'
import { computed, h, onMounted, reactive, ref, watchEffect } from 'vue'
import { EditOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { useRoute } from 'vue-router'
import UrlPicutreUpLoad from '@/components/UrlPicutreUpLoad.vue'
import router from '@/router'
import ImageCropper from '@/components/imageCropper.vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'

const picture = ref<API.PictureVO>()

const route = useRoute()
const onSuccess = (newpicture: API.PictureVO) => {
  picture.value = newpicture
  pictureForm.name = newpicture.name
}
const SpId = computed(() => {
  return route.query?.spaceId
})

const pictureForm = reactive<API.Picture>({})
const handleSubmit = async (values: any) => {
  const pictureid = picture.value.id
  if (!pictureid) {
    return
  }
  const res = await editPictureUsingPost({
    id: pictureid,
    spaceId: SpId.value,
    ...values,
  })

  if (res.data.code === 0 && res.data.data) {
    message.success('创建成功')
    await router.push({
      path: `/picture/${pictureid}`,
    })
  } else message.error('创建失败' + res.data.message)
}

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

// 获取标签和分类选项
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    // 转换成下拉选项组件接受的格式
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('加载选项失败，' + res.data.message)
  }
}

const getOldPicture = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id: id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      //pictureForm.id = data.id
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}

// 图片编辑弹窗引用
const imageCropperRef = ref()

// 编辑图片
const doEditPicture = () => {
  if (imageCropperRef.value) {
    imageCropperRef.value.openModal()
  }
}

// 编辑成功事件
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

const uploadType = ref<'file' | 'url'>('file')

onMounted(() => {
  getOldPicture()
  getTagCategoryOptions()
})
const space = ref<API.SpaceVO>()

// 获取空间信息
const fetchSpace = async () => {
  // 获取数据
  if (SpId.value) {
    const res = await getSpaceVoByIdUsingGet({
      id: SpId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    }
  }
}

watchEffect(() => {
  fetchSpace()
})
</script>

<template>
  <div id="addpicturepage">
    <h2 style="margin-bottom: 16px">{{ route.query?.id ? '修改图片' : '创建图片' }}</h2>
    <a-typography-paragraph v-if="SpId" type="secondary">
      保存至空间：<a :href="`/space/${SpId}`" target="_blank">{{ SpId }}</a>
    </a-typography-paragraph>
    <a-tabs v-model:activeKey="uploadType">
      <a-tab-pane key="file" tab="文件上传">
        <PicutreUpLoad :picture="picture" :space-id="SpId" :on-success="onSuccess" />
      </a-tab-pane>
      <a-tab-pane key="url" tab="url上传">
        <!--URL上传组件-->
        <UrlPicutreUpLoad :picture="picture" :spaceId="SpId" :on-success="onSuccess" />
      </a-tab-pane>
    </a-tabs>
    <div v-if="picture" class="edit-bar">
      <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
      <ImageCropper
        ref="imageCropperRef"
        :imageUrl="picture?.url"
        :picture="picture"
        :spaceId="SpId"
        :space="space"
        :onSuccess="onCropSuccess"
      />
    </div>
    <!--图片信息表单-->
    <a-form v-if="picture" layout="vertical" :model="pictureForm" @finish="handleSubmit">
      <a-form-item label="图片名称" name="name">
        <a-input v-model:value="pictureForm.name" placeholder="输入图片名称" allow-clear />
      </a-form-item>
      <a-form-item label="简介" name="introduction">
        <!--allow-clear ,快速清除--><!--a-textarea ,多行文本框-->
        <a-textarea
          v-model:value="pictureForm.introduction"
          placeholder="输入图片简介"
          :autosize="{ minRows: 2, maxRows: 5 }"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="分类" name="category">
        <a-auto-complete
          v-model:value="pictureForm.category"
          :options="categoryOptions"
          placeholder="请输入分类"
          allowClear
        />
      </a-form-item>
      <a-form-item label="标签" name="tags">
        <a-select
          v-model:value="pictureForm.tags"
          :options="tagOptions"
          mode="tags"
          placeholder="请输入标签"
          allowClear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">创建</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
#addpicturepage {
  max-width: 720px;
  margin: 0 auto;
}
#addpicturepage .edit-bar {
  text-align: center;
  margin: 16px 0;
}
</style>
