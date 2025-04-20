<template>
  <div class="space-user-analyze">
    <a-card title="空间图片用户分析">
      <v-chart :option="options" style="height: 320px; max-width: 90%" :loading="loading" />
      <template #extra
        ><a-space>
          <a-segmented v-model:value="timeDimension" :options="timeDimensionOptions" />
          <a-input-search
            aria-placeholder="请输入用户ID"
            enter-button="搜索用户"
            @search="doSearch"
          /> </a-space
      ></template>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { spaceUserAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
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
const loadData = ref<API.SpaceUserAnalyzeResponse>([])
//加载状态
const loading = ref(true)
// 获取数据
const fetchData = async () => {
  loading.value = true
  const res = await spaceUserAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
    timeDimension: timeDimension.value,
    userId: userId.value,
  })
  if (res.data.code == 0) {
    loadData.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
//用户ID
const userId = ref<string>()
//时间维度
const timeDimension = ref<string>('day')
//监听数据变化
const doSearch = (value: string) => {
  userId.value = value
}

const timeDimensionOptions = [
  {
    label: '日',
    value: 'day',
  },
  {
    label: '周',
    value: 'week',
  },
  {
    label: '月',
    value: 'month',
  },
]

const options = computed(() => {
  const periods = loadData.value.map((item) => item.period) // 时间区间
  const counts = loadData.value.map((item) => item.count) // 上传数量

  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: periods, name: '时间区间' },
    yAxis: { type: 'value', name: '上传数量' },
    series: [
      {
        name: '上传数量',
        type: 'line',
        data: counts,
        smooth: true, // 平滑折线
        emphasis: {
          focus: 'series',
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
