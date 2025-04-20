<template>
  <div id="picture-manage">
    <a-flex justify="space-between">
      <h2>图片管理</h2>
      <a-space>
        <a-button type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
        <a-button type="primary" href="/add_picture/batch" target="_blank" ghost
          >+ 批量创建图片</a-button
        >
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px"></div>
    <!--搜索框-->
    <a-form layout="inline" :model="seachParams" @finish="doSearch">
      <a-form-item label="关键词" name="seachText">
        <a-input
          v-model:value="seachParams.searchText"
          placeholder="从名称和简介开始搜索"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="类型" name="category">
        <!--allow-clear ,快速清除-->
        <a-input v-model:value="seachParams.category" placeholder="输入图片类型" allow-clear />
      </a-form-item>
      <a-form-item label="标签" name="tags">
        <!--allow-clear ,快速清除-->
        <a-select
          v-model:value="seachParams.tags"
          mode="tags"
          placeholder="输入图片标签"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
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
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px"></div>
    <a-table
      :columns="columns"
      :data-source="datelist"
      :pagination="pageination"
      @change="doDataChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'url'">
          <a-image :src="record.url" :width="120" />
        </template>
        <template v-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <a-tag v-for="tag in JSON.parse(record.tags || '[]')" :key="tag">
              {{ tag }}
            </a-tag>
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'picInfo'">
          <div>格式：{{ record.picFormat }}</div>
          <div>宽度：{{ record.picWidth }}</div>
          <div>高度：{{ record.picHeight }}</div>
          <div>大小：{{ (record.picSize / 1024).toFixed(2) }}KB</div>
        </template>
        <template v-if="column.dataIndex === 'UserId'">
          {{ record.userId }}
        </template>
        <template v-if="column.dataIndex === 'reviewMessage'">
          <div>
            审核状态：<a-tag>{{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }}</a-tag>
          </div>
          <div>
            审核信息：<a-tag>{{ record.reviewMessage }}</a-tag>
          </div>
          <div>
            审核人：<a-tag>{{ record.reviewerId }}</a-tag>
          </div>
          <div v-if="record.reviewTime">
            审核时间：{{ dayjs(record.reviewTime).format('YYYY--MM--DD HH:mm:ss') }}
          </div>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY--MM--DD HH:mm:ss') }}
        </template>
        <template v-if="column.dataIndex === 'editTime'">
          {{ dayjs(record.editTime).format('YYYY--MM--DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
              type="link"
              @click="handReview(record, PIC_REVIEW_STATUS_ENUM.PASS)"
              >通过</a-button
            >
            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
              type="link"
              danger
              @click="handReview(record, PIC_REVIEW_STATUS_ENUM.REJECT)"
              >拒绝</a-button
            >
            <a-button type="link" :href="`/add_picture?id=${record.id}`" target="_blank"
              >编辑</a-button
            >
            <a-button danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deletePictureUsingPost,
  doPictureReviewUsingPost,
  listPictureByPageUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PIC_REVIEW_STATUS_ENUM,
  PIC_REVIEW_STATUS_MAP,
  PIC_REVIEW_STATUS_OPTIONS,
} from '@/constants/picture.ts'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '图片',
    dataIndex: 'url',
  },
  {
    title: '名称',
    dataIndex: 'name',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
  },
  {
    title: '标签',
    dataIndex: 'tags',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
  },
  {
    title: '用户id',
    dataIndex: 'UserId',
    width: 80,
  },
  {
    title: '空间id',
    dataIndex: 'spaceId',
    width: 80,
  },
  {
    title: '审核信息',
    dataIndex: 'reviewMessage',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

//定义数据
const datelist = ref<API.Picture[]>([])
const total = ref(0)
//搜素条件
const seachParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'CreateTime',
  sortOrder: 'descend',
})
const fetchData = async () => {
  const res = await listPictureByPageUsingPost({
    ...seachParams,
    //nullSpaceId: true,
  })
  if (res.data.code === 0 && res.data.data) {
    datelist.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else message.error(res.data.message)
}
//页面加载时请求一次
onMounted(() => {
  fetchData()
})
const pageination = computed(() => {
  return {
    current: seachParams.current,
    pageSize: seachParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: any) => `共${total}条`,
  }
})
const doDataChange = (page: any) => {
  seachParams.current = page.current
  seachParams.pageSize = page.pageSize
  fetchData()
}
// 删除数据
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    await fetchData()
  } else {
    message.error('删除失败')
  }
}
// 获取数据
const doSearch = () => {
  // 重置页码
  seachParams.current = 1
  fetchData()
}
const handReview = async (record: API.Picture, reviewStatus: number) => {
  const reviewMessage =
    reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '管理操作通过' : '管理员操作拒绝'

  const res = await doPictureReviewUsingPost({
    id: record.id,
    reviewStatus,
    reviewMessage,
  })
  if (res.data.code === 0) {
    message.success('审核操作成功')
    //获取列表数据
    await fetchData()
  } else {
    message.error('审核操作失败，' + res.data.message)
  }
}
</script>
