<template>
  <div id="searchPictureIdPage">
    <h2>以图搜图</h2>
    <h3>原图</h3>
    <!--单张卡片-->
    <a-card hoverable style="width: 240px">
      <template #cover>
        <img
          :alt="picture.name"
          :src="picture.thumbnailUrl ?? picture.url"
          style="height: 180px; object-fit: cover"
        />
      </template>
    </a-card>
    <div style="margin-bottom: 10px"></div>
    <h3>识图结果</h3>
    <a-list :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 6 }" :data-source="dataList">
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <a :href="picture.formUrl" target="_blank" />
          <!--单张卡片-->
          <a-card hoverable style="width: 240px">
            <template #cover>
              <img
                :alt="picture.name"
                :src="picture.thumbUrl"
                style="height: 180px; object-fit: cover"
              />
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, h, computed } from 'vue'
import {
  getPictureVoByIdUsingGet,
  searchPictureByPictureUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import { downloadImage } from '@/utils'

const route = useRoute()
const picture = ref<API.PictureVO>({})
const pictureId = computed(() => {
  return route.query.pictureId
})
// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: pictureId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchPictureDetail()
  fetchData()
})

const dataList = ref<API.ImageSearchResult[]>([])
// 获取搜图结果
const fetchData = async () => {
  const res = await searchPictureByPictureUsingPost({
    pictureId: pictureId.value,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

//下载图片
const doDownload = () => {
  downloadImage(picture.value.url)
}
</script>

<style scoped>
#pictureDetailPage {
  margin-bottom: 16px;
}
</style>
