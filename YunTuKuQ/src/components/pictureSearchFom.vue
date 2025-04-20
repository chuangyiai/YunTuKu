<template>
  <div id="picture-search-form">
    <!--搜索框-->
    <a-form layout="inline" :model="seachParams" @finish="doSearch"
      ><div style="padding-top: 10px">
        <a-form-item label="关键词" name="searchText">
          <a-input
            v-model:value="seachParams.searchText"
            placeholder="从名称和简介开始搜索"
            allow-clear
          />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="类型" name="category">
          <!--allow-clear ,快速清除-->
          <a-auto-complete
            v-model:value="seachParams.category"
            :options="categoryOptions"
            style="min-width: 180px"
            placeholder="输入图片类型"
            allow-clear
          />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="标签" name="tags">
          <!--allow-clear ,快速清除-->
          <a-select
            v-model:value="seachParams.tags"
            :options="tagOptions"
            mode="tags"
            placeholder="输入图片标签"
            style="min-width: 180px"
            allow-clear
          />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="日期" name="dateRange">
          <a-range-picker
            style="width: 400px"
            v-model:value="dateRange"
            show-time
            :placeholder="['编辑开始时间', '编辑结束时间']"
            format="YYYY/MM/DD HH:mm:ss"
            :presets="rangePresets"
            @change="onRangeChange"
        /></a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="seachParams.name" placeholder="从名称开始搜索" allow-clear />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="简介" name="introduction">
          <a-input
            v-model:value="seachParams.introduction"
            placeholder="从简介开始搜索"
            allow-clear
          />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="宽度" name="picWidth">
          <a-input-number v-model:value="seachParams.picWidth" />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="高度" name="picHeight">
          <a-input-number v-model:value="seachParams.picHeight" />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="格式" name="picFormat">
          <a-input v-model:value="seachParams.picFormat" placeholder="请输入图片格式" allow-clear />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item label="审核状态" name="reviewStatus">
          <!--allow-clear ,快速清除-->
          <a-select
            v-model:value="seachParams.reviewStatus"
            placeholder="输入图片审核状态"
            :options="PIC_REVIEW_STATUS_OPTIONS"
            style="min-width: 180px"
            allow-clear
          />
        </a-form-item>
      </div>

      <div style="padding-top: 10px">
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </div>
      <div style="padding-top: 10px">
        <a-form-item>
          <a-button type="primary" html-type="reset" @click="doClear">重置</a-button>
        </a-form-item>
      </div>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { onMounted, reactive } from 'vue'
import { PIC_REVIEW_STATUS_OPTIONS } from '@/constants/picture.ts'
import dayjs from 'dayjs'
import { ref } from 'vue'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

interface Props {
  onSearch?: (SearchParams: API.PictureQueryRequest) => void
}
const props = defineProps<Props>()
//搜素条件
const seachParams = reactive<API.PictureQueryRequest>({})

// 获取数据
const doSearch = () => {
  props.onSearch?.(seachParams)
}

const dateRange = ref<[]>([])
/**
 * 日期范围更改时触发
 * @param dates
 * @param dateStrings
 */
const onRangeChange = (dates: any[], dateStrings: string[]) => {
  if (dates.length < 2) {
    seachParams.startTime = undefined
    seachParams.endTime = undefined
  } else {
    seachParams.startTime = dates[0].toDate()
    seachParams.endTime = dates[1].toDate()
  }
}
//时间范围预设
const rangePresets = ref([
  { label: '过去 7 天', value: [dayjs().add(-7, 'd'), dayjs()] },
  { label: '过去 14 天', value: [dayjs().add(-14, 'd'), dayjs()] },
  { label: '过去 30 天', value: [dayjs().add(-30, 'd'), dayjs()] },
  { label: '过去 90 天', value: [dayjs().add(-90, 'd'), dayjs()] },
])
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
const doClear = () => {
  Object.keys(seachParams).forEach((key) => {
    seachParams[key] = undefined
  })
  //日期单独清空
  dateRange.value = []
  //重新搜索
  props.onSearch?.(seachParams)
}

onMounted(() => {
  getTagCategoryOptions()
})
</script>
<style scoped>
.picture-search-form :deep(.ant-form-item) {
  margin-top: 16px;
}
</style>
