<template>
  <div class="space-usage-analyze">
    <a-flex gap="middle">
      <a-card title="空间资源使用分析" style="width: 50%">
        <div style="height: 320px; text-align: center">
          <h3>
            {{ formatSize(loadData.usedSize) }} /
            {{ loadData.maxSize ? formatSize(loadData.maxSize) : '无限制' }}
          </h3>
          <a-progress type="dashboard" :percent="loadData.sizeUsageRatio ?? 0" />
        </div>
      </a-card>
      <a-card title="图片数量" style="width: 50%">
        <div style="height: 320px; text-align: center">
          <h3>{{ loadData.usedCount }} / {{ loadData.maxCount ?? '无限制' }}</h3>
          <a-progress type="dashboard" :percent="loadData.countUsageRatio ?? 0" />
        </div>
      </a-card>
    </a-flex>
  </div>
</template>
<script setup lang="ts">
import 'echarts'
import { ref, watchEffect } from 'vue'
import { spaceUsageAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { formatSize } from '@/utils'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: number
}
const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

//加载数据
const loadData = ref<API.SpaceUsageAnalyzeResponse>({})
//加载状态
const loading = ref(true)
// 获取数据
const fetchData = async () => {
  loading.value = true
  const res = await spaceUsageAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.data && res.data.code === 0) {
    loadData.value = res.data.data
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
/**
 * 监听 queryAll queryPublic spaceId 变化时重新获取数据
 */
watchEffect(() => {
  fetchData()
})
</script>
<style scoped></style>
