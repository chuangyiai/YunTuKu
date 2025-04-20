<template>
  <div class="space-size-analyze">
    <a-card title="空间图片大小分析">
      <v-chart :option="options" style="height: 320px; max-width: 90%" :loading="loading" />
    </a-card>
  </div>
</template>
<script setup lang="ts">
import 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { spaceSizeAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import VChart from 'vue-echarts'
import 'echarts'

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
const loadData = ref<API.SpaceSizeAnalyzeResponse>([])
//加载状态
const loading = ref(true)
// 获取数据
const fetchData = async () => {
  loading.value = true
  const res = await spaceSizeAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.code == 0) {
    loadData.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
const options = computed(() => {
  const pieData = loadData.value.map((item) => ({
    name: item.sizeRange,
    value: item.count,
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
    },
    legend: {
      top: 'bottom',
    },
    series: [
      {
        name: '图片大小',
        type: 'pie',
        radius: '50%',
        data: pieData,
      },
    ],
  }
})

/**
 * 监听 queryAll queryPublic spaceId 变化时重新获取数据
 */
watchEffect(() => {
  fetchData()
})
</script>
<style scoped></style>
