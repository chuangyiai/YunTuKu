<template>
  <div class="space-rank-analyze">
    <a-card title="空间使用排行分析">
      <v-chart :option="options" style="height: 320px; max-width: 90%" :loading="loading"
    /></a-card>
  </div>
</template>
<script setup lang="ts">
import 'echarts'
import { computed, ref, watchEffect } from 'vue'

import { message } from 'ant-design-vue'
import VChart from 'vue-echarts'
import 'echarts'
import { spaceUsageRankUsingPost } from '@/api/spaceAnalyzeController.ts'

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
const loadData = ref<API.Space>([])
//加载状态
const loading = ref(true)
// 获取数据
const fetchData = async () => {
  loading.value = true
  const res = await spaceUsageRankUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
    topN: 10,
  })
  if (res.data.code == 0) {
    loadData.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
const options = computed(() => {
  const spaceNames = loadData.value.map((item) => item.spaceName)
  const usageData = loadData.value.map((item) => (item.totalSize / (1024 * 1024)).toFixed(2)) // 转为 MB

  return {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: spaceNames,
    },
    yAxis: {
      type: 'value',
      name: '空间使用量 (MB)',
    },
    series: [
      {
        name: '空间使用量 (MB)',
        type: 'bar',
        data: usageData,
        itemStyle: {
          color: '#5470C6', // 自定义柱状图颜色
        },
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
