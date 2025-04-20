import router from '@/router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'

//是否首次获取用户
let firstFetchLoginUser: boolean = true
/**
 * 全局权限校验，每次切换时加载
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  //确保页面刷新时，首次加载时，能等待后端返回用户信息后再鉴权
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  //自定义权限校验规则，管理员才能访问admin开头的页面
  if (toUrl) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('无权限')
      next('/user/login?redirectTo=  ${to.fullPath}')
      return
    }
  }
  next()
})
