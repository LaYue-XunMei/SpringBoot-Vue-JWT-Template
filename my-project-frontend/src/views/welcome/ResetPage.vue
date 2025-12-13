<script setup>

import {computed, onUnmounted, reactive, ref} from "vue";
import {EditPen, Lock, Message} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import {get,post} from "@/net/index.js";
import router from "@/router/index.js";

const active = ref(0)
const form =reactive({
  email: '',
  code: '',
  password: '',
  password_repeat: ''
})

const formRef = ref()

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
  email:[
    {required: true, message:'请输入邮箱', trigger: ['blur']},
    {type: 'email', message: '请输入合法的邮箱地址', trigger: ['blur','change']}
  ],
  code :[
    {required: true, message:'请输入获取的验证码', trigger: ['blur']}
  ],
  password:[
    {required: true, message:'请输入密码', trigger: ['blur']},
    {min:6, max:16, message: '长度在6到16个字符', trigger: ['blur','change']}
  ],
  password_repeat:[
    {validator: validatePassword, trigger: ['blur','change']}
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
  get(`api/auth/ask-code?email=${form.email}&type=reset`, () => {
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

function confirmReset(){
  formRef.value.validate((valid) => {
    if(valid){
      post('/api/auth/reset-confirm',{
        email: form.email,
        code: form.code
      },() => active.value++)
    }
  })
}

 function doReset(){
   formRef.value.validate((valid) => {
     if(valid){
       post('/api/auth/reset-password',{...form},()=>{
         ElMessage.success('密码重置成功,请重新登录')
         router.push('/')
       })
     }
   })
 }
</script>

<template>
  <div style="text-align: center">
    <div style="margin-top: 30px">
      <el-steps :active="active" finish-status="success" align-center>
        <el-step title="验证电子邮件"></el-step>
        <el-step title="重新设置密码"></el-step>
      </el-steps>
    </div>

    <div style="margin: 0 20px" v-if="active === 0">
      <div style="margin-top: 80px">
        <div style="font-size: 25px;font-weight: bold">重置密码</div>
        <div style="font-size: 14px;color: grey">请输入需要重置密码的邮箱</div>
      </div>
      <div style="margin-top: 50px" >
        <el-form :model="form" :rules="rule" ref="formRef">
          <el-form-item prop="email">
            <el-input v-model="form.email" type="email" placeholder="电子邮件地址">
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
                <el-button @click="askCode" :disabled="!isEmailValid || coldTime > 0" type="success">
                  {{ coldTime > 0 ? `请${coldTime}秒后再` : '获取验证码' }}
                </el-button>
              </el-col>
            </el-row>
          </el-form-item>
        </el-form>
      </div>

      <div style="margin-top: 80px">
        <el-button @click="confirmReset" style="width: 270px" type="warning" :plain="true">开始重置密码</el-button>
      </div>
    </div>
    <div style="margin: 0 20px" v-if="active === 1">
      <div style="margin-top: 80px">
        <div style="font-size: 25px;font-weight: bold">重置密码</div>
        <div style="font-size: 14px;color: grey">请填写需要设置的新密码</div>
      </div>
      <div style="margin-top: 50px">
        <el-form :model="form" :rules="rule" ref="formRef">
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
        </el-form>
      </div>
      <div style="margin-top: 80px">
        <el-button @click="doReset" style="width: 270px" type="danger" :plain="true">立即重置密码</el-button>
      </div>
    </div>
  </div>


</template>

<style scoped>

</style>