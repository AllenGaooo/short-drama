<template>
    <el-row justify="end">
        <el-col :span="9" style="font-size: 50px;color: gray;">XXX管理系统</el-col>
        <el-col :span="5">
              <el-button-group direction="horizontal">
                <el-button type="primary" color="#626aef" plain :icon="User" >{{ username }}</el-button>
                <el-button type="primary" color="#626aef" plain :icon="Histogram" />
                <el-button type="primary" color="#626aef" plain :icon="SwitchButton" @click="logout" />
            </el-button-group>


        </el-col>
    </el-row>
</template>

<script setup>
import { User,Histogram,SwitchButton } from '@element-plus/icons-vue'
import { ref,onMounted } from 'vue'
import userStore from '@/store/userStore.js';
import router from '@/router'
const username = ref('')

const logout = ()=>{
    router.push("/login")
}


onMounted(()=>{
    //console.log(userStore().user)
    username.value = userStore().user.username

})

</script>

<style scoped>

</style>