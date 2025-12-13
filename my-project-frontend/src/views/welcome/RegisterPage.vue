<script setup>

import {computed, onUnmounted, reactive, ref} from "vue";
import {User, Lock, Message, EditPen} from "@element-plus/icons-vue";
import router from "@/router/index.js";
import {ElMessage} from "element-plus";
import {get,post} from "@/net/index.js";

const formRef = ref()

const form = reactive({
  username : '',
  password : '',
  password_repeat : '',
  email: '',
  code: ''
})

const validateUsername = (rule,value,callback) =>{
  if(value === ''){
    callback(new Error('请输入用户名'))
  }else if(!/^[a-zA-Z0-9\u4e00-\u9fa5]+$/.test(value)){
    callback(new Error('用户名包含特殊字符，只能是中/英文'))
  }else{
    callback()
  }
}

const validatePassword = (rule,value,callback) =>{
  if(value === ''){
    callback(new Error('请再次输入密码'))
  }else if(value !== form.password){
    callback(new Error('两次输入的密码不一致'))
  }else {
    callback()
  }
}

const rule = {
  username : [
    {validator: validateUsername, trigger: ['blur','change']},
    {min:2,max:10, message: '长度在2到10个字符', trigger: ['blur','change']}
  ],
  password:[
    {required: true, message:'请输入密码', trigger: ['blur']},
    {min:6, max:16, message: '长度在6到16个字符', trigger: ['blur','change']}
  ],
  password_repeat:[
    {validator: validatePassword, trigger: ['blur','change']}
  ],
  email:[
    {required: true, message:'请输入邮箱', trigger: ['blur']},
    {type: 'email', message: '请输入合法的邮箱地址', trigger: ['blur','change']}
  ],
  code :[
    {required: true, message:'请输入获取的验证码', trigger: ['blur']}
  ]
}

const coldTime =ref(0)
let timer = null
function askCode(){
  if (!isEmailValid.value) {
    ElMessage.warning("请输入正确的邮箱地址")
    return
  }
  // 如果正在冷却中
  if (coldTime.value > 0) {
    ElMessage.warning(`请等待 ${coldTime.value} 秒后再获取验证码`)
    return
  }
  // 清除之前的定时器
  clearTimer()
  // 发送请求
  get(`api/auth/ask-code?email=${form.email}&type=register`, () => {
        ElMessage.success("验证码已发送到您的邮箱")
        startCountdown()
      }, (message) => {
        ElMessage.warning(message)
        clearTimer()
      }
  )
}

function startCountdown() {
  coldTime.value = 60
  timer = setInterval(() => {
    coldTime.value--
    if (coldTime.value <= 0) {
      clearTimer()
    }
  }, 1000)
}

function clearTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

// 组件卸载时清理定时器（重要！）
onUnmounted(() => {
  clearTimer()
})

const isEmailValid = computed(() => /^[\w.-]+@[\w.-]+\.\w+$/.test(form.email))

function register(){
    formRef.value.validate((valid)=>{
      if(valid){
        post(`/api/auth/register`,{...form},()=>{
          ElMessage.success("注册成功")
          router.push('/')
        })
      }else{
        ElMessage.warning("请完整填写注册表单内容")
      }
  })
}

</script>

<template>
  <div style="text-align: center;margin: 0 20px">
    <div style="margin-top: 100px">
      <div style="font-size: 25px;font-weight: bold">注册新用户</div>
      <div style="font-size: 14px;color: grey">欢迎新用户注册，请在下方填写相关信息</div>
    </div>
    <div style="margin-top: 50px">
      <el-form :model="form" :rules="rule" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" maxlength="10" type="text" placeholder="用户名">
            <template #prefix>
              <el-icon><User/></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" maxlength="20" type="password" placeholder="密码">
            <template #prefix>
              <el-icon><Lock/></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item pro="password_repeat">
          <el-input v-model="form.password_repeat" maxlength="20" type="password" placeholder="重复密码">
            <template #prefix>
              <el-icon><Lock/></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email"  type="email" placeholder="电子邮件地址">
            <template #prefix>
              <el-icon><Message/></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="code">
          <el-row :gutter="10" style="width: 100%">
            <el-col :span="17">
              <el-input v-model="form.code" maxlength="6" type="text" placeholder="请输入验证码">
                <template #prefix>
                  <el-icon><EditPen/></el-icon>
                </template>
              </el-input>
            </el-col>
            <el-col :span="5">
              <el-button @click="askCode" :disabled="!isEmailValid || coldTime" type="success">
                {{ coldTime > 0 ? `请${coldTime}秒后再` : '获取验证码' }}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>

      <div style="margin-top: 80px">
        <el-button @click="register" style="width: 270px" type="warning" :plain="true">立即注册</el-button>
      </div>
      <div style="margin-top: 20px">
        <span style="font-size: 14px;line-height: 15px;color: grey">已有账号？</span>
        <el-link style="translate: 0 -1px" @click="router.push('/')">立即登录</el-link>
      </div>


    </div>


  </div>

</template>

<style scoped>

</style>