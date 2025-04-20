<script setup lang="ts">
import { reactive } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})
const route = useRouter()
const loginUserStore = useLoginUserStore()
const handleSubmit = async (values: any) => {
  const res = await userLoginUsingPost(values)
  //登录成功,code为业务返回值
  if (res.data.code === 0 && res.data.data) {
    //await userLoginUsingPost(res.data.data)
    await loginUserStore.fetchLoginUser()
    message.success('登陆成功')
    await route.push({
      path: '/',
      replace: true,
    })
  } else message.error('登陆失败' + res.data.message)
}
</script>

<template>
  <div id="userLoginPage">
    <h2 class="title">云图库系统 - 用户登录</h2>
    <div class="desc">智能云图库</div>
    <a-form
      :model="formState"
      name="basic"
      :label-col="{ span: 8 }"
      :wrapper-col="{ span: 16 }"
      autocomplete="off"
      @finish="handleSubmit"
    >
      <a-form-item
        label="用户账号"
        name="userAccount"
        :rules="[{ required: true, message: '请输入账号!' }]"
      >
        <a-input v-model:value="formState.userAccount" />
      </a-form-item>

      <a-form-item
        label="用户密码"
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码!' },
          { min: 8, message: '密码长度不小于8' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" />
      </a-form-item>
      <div class="tips">
        没有账号？立即
        <RouterLink to="/user/register">注册</RouterLink>
      </div>
      <a-form-item :wrapper-col="{ offset: 8, span: 16 }">
        <a-button type="primary" html-type="submit" style="width: 100%">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
#userLoginPage {
  max-width: 360px;
  margin: 0 auto;
}
#userLoginPage .tips {
  text-align: right;
  color: #bbb;
  font-size: 13px;
  margin-bottom: 16px;
}
#userLoginPage .desc {
  color: #bbb;
  margin-bottom: 16px;
}
#userLoginPage .title {
  text-align: center;
  font-size: 16px;
}
</style>
