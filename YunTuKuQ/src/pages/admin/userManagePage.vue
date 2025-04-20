<template>
  <div id="user-manage">
    <a-space>
      <!--搜索框-->
      <a-form layout="inline" :model="seachParams" @finish="doSearch">
        <a-form-item label="账号">
          <a-input v-model:value="seachParams.userAccount" placeholder="输入账号" allow-clear />
        </a-form-item>
        <a-form-item label="用户名">
          <!--allow-clear ,快速清除-->
          <a-input v-model:value="seachParams.userName" placeholder="输入用户名" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>
    </a-space>
    <div style="margin-bottom: 16px"></div>
    <a-table
      :columns="columns"
      :data-source="dateList"
      :pagination="pageination"
      @change="doDataChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="120" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY--MM--DD HH:mm:ss') }}
        </template>
        <template v-if="column.dataIndex === 'updateTime'">
          {{ dayjs(record.updateTime).format('YYYY--MM--DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button danger @click="doDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUserUsingPost, listUserVoByPageUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]
//定义数据
const dateList = ref<API.PageUserVO_>([])
const total = ref(0)
//搜素条件
const seachParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
})
const fetchData = async () => {
  const res = await listUserVoByPageUsingPost({
    ...seachParams,
  })
  if (res.data.data) {
    dateList.value = res.data.data.records ?? []
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
  const res = await deleteUserUsingPost({ id })
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
</script>
