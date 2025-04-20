import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import userLoginPage from '@/pages/user/userLoginPage.vue'
import userRegisterPage from '@/pages/user/userRegisterPage.vue'
import userManagePage from '@/pages/admin/userManagePage.vue'
import PictureManagePage from '@/pages/admin/pictureManagePage.vue'
import SpaceManagePage from '@/pages/admin/spaceManagePage.vue'
import AddPicturePge from '@/pages/addPicturePge.vue'

import AddPictureBatchPge from '@/pages/addPictureBatchPge.vue'
import PictureDetailPage from '@/pages/PictureDetailPage.vue'
import MySpacePge from '@/pages/MySpacePge.vue'
import AddSpacePge from '@/pages/addSpacePge.vue'
import spaceDetailPage from '@/pages/SpaceDetailPage.vue'
import SearchPicturePage from '@/pages/SearchPicturePage.vue'
import SpaceAnalyzePage from '@/pages/SpaceAnalyzePage.vue'
import SpaceUserManagePage from '@/pages/admin/spaceUserManagePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: userLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: userRegisterPage,
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: userManagePage,
    },
    {
      path: '/admin/pictureManage',
      name: '图片管理',
      component: PictureManagePage,
    },
    {
      path: '/admin/spaceManage',
      name: '空间管理',
      component: SpaceManagePage,
    },
    {
      path: '/spaceUserManage/:id',
      name: '空间成员管理',
      component: SpaceUserManagePage,
      props: true,
    },
    {
      path: '/add_picture',
      name: '添加图片',
      component: AddPicturePge,
    },
    {
      path: '/add_space',
      name: '创建空间',
      component: AddSpacePge,
    },
    {
      path: '/add_picture/batch',
      name: '批量添加图片',
      component: AddPictureBatchPge,
    },
    {
      path: '/picture/:id',
      name: '图片详情',
      component: PictureDetailPage,
      props: true,
    },
    {
      path: '/space/:id',
      name: '空间详情',
      component: spaceDetailPage,
      props: true,
    },
    {
      path: '/space_Analyze',
      name: '空间使用分析',
      component: SpaceAnalyzePage,
    },
    {
      path: '/search_picture',
      name: '以图搜图',
      component: SearchPicturePage,
    },
    {
      path: '/my_space',
      name: '我的空间',
      component: MySpacePge,
    },
  ],
})

export default router
