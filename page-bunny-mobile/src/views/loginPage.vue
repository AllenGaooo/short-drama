<template>

    <el-card class="loginPanel" style="max-width: 420px">
        <template #header>
            <img style="margin-left: 10px;" src="../assets/login.png" />
        </template>
        <el-form ref="loginFormRef" hide-required-asterisk :model="loginForm" :rules="rules" label-width="60" style="max-width: 400px">
            <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" :suffix-icon="User"/>
            </el-form-item>
            <el-form-item label="密码" prop="password">
                <el-input type="password" v-model="loginForm.password" :suffix-icon="Unlock"/>
            </el-form-item>
            <el-form-item>
                <el-button color="#626aef"  @click="onSubmit">登录</el-button>
                <el-button color="#626aef" plain @click="onReset">重置</el-button>
            </el-form-item>
        </el-form>

    </el-card>

</template>

<script setup>
import { User, Unlock } from '@element-plus/icons-vue'
import { ref,reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import userStore from '@/store/userStore.js';

//通过vue的ref属性 获得表单组件对象
const loginFormRef = ref(null)

//登录表单数据
const loginForm = ref({
    username: '',
    password: '',
})
//登录表单校验规则
const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, max: 8, message: '长度4-8字符', trigger: 'blur' },
  ],
})

//提交登录
const onSubmit = ()=>{
    //统一触发校验
    loginFormRef.value.validate((valid) => {
        if (valid) {
            //发送请求
            axios.get(`http://127.0.0.1:4523/m2/8253284-8015093-default/456108728?name=${loginForm.value.username}`)
            .then(res=>{
                //res请求和响应完整数据 res.data 响应返回的实际json数据
                //响应数据中 自定义数据  code 响应码 msg 响应信息 data 附加数据
                console.log(res.data);
                if(res.data.code==1000){
                    //弹窗
                    ElMessage.success(res.data.msg)
                    //存储用户信息在前端

                    userStore().user = res.data.data
                    //跳转页面
                    router.push('/main')
                }
                if(res.data.code==1001){ 
                    ElMessage.error(res.data.msg)
                }
            })
        }
  })
}
//清空表单
const onReset = ()=>{
    loginFormRef.value.resetFields();
}

</script>

<style scoped>
.loginPanel{
    margin: 200px auto;
    background-color: rgba(192, 192, 192, 0.541);
}

</style>