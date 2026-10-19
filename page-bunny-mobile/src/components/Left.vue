<template>
  <el-menu
        active-text-color="#626aef"
        background-color="#E4E7ED"
        text-color="#909399"
        router
      >
        <el-sub-menu  v-for="menu in usermenu" :index="menu.mid+''" >
          <template #title>
            <el-icon><component :is="menu.icon"></component></el-icon>
            <span>{{menu.name}}</span>
          </template>
          <!-- @click="toPage(subm.path)" -->
           <el-menu-item v-for="subm in menu.subMenu" :index="subm.path" >
                <el-icon><component :is="subm.icon"></component></el-icon>
                <span>{{subm.name}}</span>
            </el-menu-item>
           
        </el-sub-menu>
</el-menu>


      sjakhdlkaskldj

</template>

<script setup>
import { SwitchButton,Search } from '@element-plus/icons-vue'
import { ref,onMounted } from 'vue'
import userStore from '@/store/userStore.js';
import router from '@/router'

//1.菜单需要展示动态菜单
//不用的用户登录上来 看到的菜单不同

//2.菜单数据通过后端查询获取 登录时放入缓存 需要读取时直接从缓存读取

//3.配置子路由,只切换主菜单页面的部分页面

//4.菜单组件自带路由模式 开启后自动配置跳转功能
//菜单上配置router  在子菜单路径直接写入菜单的index属性

const usermenu = ref([])

// const toPage = (path) =>{
//    router.push(path)
// }
onMounted(()=>{
    //'' null undefinded false
    //只要有值 就会判断为ture
    if(userStore().user.usermenu){
        usermenu.value = userStore().user.usermenu
    }

})


</script>

<style scoped>

</style>