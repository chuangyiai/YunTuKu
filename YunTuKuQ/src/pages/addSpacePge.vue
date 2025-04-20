<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import {
  addSpaceUsingPost,
  getSpaceVoByIdUsingGet,
  listSpaceByLevelUsingPost,
  updateSpaceUsingPost,
} from '@/api/spaceController.ts'
import { useRoute } from 'vue-router'
import router from '@/router'
import {
  SPACE_LEVEL_ENUM,
  SPACE_LEVEL_OPTIONS,
  SPACE_TYPE_ENUM,
  SPACE_TYPE_MAP,
} from '@/constants/space.ts'
import { formatSize } from '@/utils'

const oldSpace = ref<API.SpaceVO>()
const formData = reactive<API.spaceAddRequest | API.SpaceUpdateRequest>({
  spaceName: '',
  spaceLevel: SPACE_LEVEL_ENUM.COMMON,
})
const loading = ref(false)

const handleSubmit = async () => {
  const spaceId = oldSpace.value?.id

  loading.value = true
  let res

  if (spaceId) {
    //更新
    res = await updateSpaceUsingPost({
      id: spaceId,

      ...formData,
    })
  } else {
    //创建
    res = await addSpaceUsingPost({
      ...formData,
      spaceType: spaceType.value,
    })
  }

  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    await router.push({
      path: `/space/${res.data.data}`,
    })
  } else message.error('操作失败' + res.data.message)
  loading.value = false
}
const route = useRoute()

// 空间类别
const spaceType = computed(() => {
  if (route.query?.type) {
    return Number(route.query.type)
  }
  return SPACE_TYPE_ENUM.PRIVATE
})

const getOldSpace = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({
      id: id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      oldSpace.value = data
      formData.spaceName = data.spaceName
      formData.spaceLevel = data.spaceLevel
    }
  }
}
const spaceLevelList = ref<API.SpaceLevel[]>([])
const fetchSpaceLevelList = async () => {
  const res = await listSpaceByLevelUsingPost({})
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error(res.data.message + '空间级别获取失败！')
  }
}

onMounted(() => {
  getOldSpace()
  fetchSpaceLevelList()
})
</script>

<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改空间' : '创建空间' }} {{ SPACE_TYPE_MAP[spaceType] }}
    </h2>
    <!--空间信息表单-->
    <!--搜索框-->
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="空间名称" name="name">
        <a-input v-model:value="formData.spaceName" placeholder="输入空间名称" allow-clear />
      </a-form-item>
      <a-form-item label="空间级别" name="spaceLevel">
        <a-select
          v-model:value="formData.spaceLevel"
          placeholder="请输入空间级别"
          :options="SPACE_LEVEL_OPTIONS"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading>创建</a-button>
      </a-form-item>
    </a-form>
    <!--空间级别介绍-->
    <a-card title="空间级别介绍">
      <a-typography-paragraph> *目前仅存在普通版，如需升级请联系xxx </a-typography-paragraph>
      <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
        {{ spaceLevel.text }}: 大小 {{ formatSize(spaceLevel.maxSize) }} , 数量
        {{ spaceLevel.maxCount }}
      </a-typography-paragraph>
    </a-card>
  </div>
</template>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
