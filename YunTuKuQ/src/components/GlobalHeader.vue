<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <router-link to="/">
        <a-col flex="200px"
          ><div class="title-bar">
            <img class="logo" src="../assets/logo.svg" alt="logo" />
            <div class="title">云图库</div>
          </div></a-col
        >
      </router-link>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <a-col flex="120px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <ASpace>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </ASpace>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <router-link to="/my_space" />
                    <UserOutlined />
                    我的空间
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import {
  HomeOutlined,
  GitlabOutlined,
  LogoutOutlined,
  PictureOutlined,
  FolderOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { type MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
import router from '@/router'

//原始数组
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页 One',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    icon: () => h(GitlabOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    icon: () => h(PictureOutlined),
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    icon: () => h(FolderOutlined),
    label: '空间管理',
    title: '空间管理',
  },
  {
    key: '/add_picture',
    icon: () => h(GitlabOutlined),
    label: '创建图片',
    title: '创建图片',
  },
  {
    key: 'others',
    label: h('a', { href: 'https://www.codefather.cn', target: '_blank' }, '编程导航'),
    title: '编程导航',
  },
]
const route = useRouter()
const current = ref<string[]>([])
route.afterEach((to, from, failure) => {
  current.value = [to.path]
})
const doMenuClick = ({ key }) => {
  route.push({
    path: key,
  })
}
const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()
// 用户注销
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  console.log(res)
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
//过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    //管理员才能访问admin开头的页面
    if (menu?.key.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}
const items = computed(() => {
  return filterMenus(originItems)
})
</script>
<style scoped>
#globalHeader .title-bar {
  display: flex;
  align-items: center;
}
.title {
  color: aqua;
  font-size: 18px;
  margin-left: 10px;
}
.logo {
  height: 48px;
}
</style>
