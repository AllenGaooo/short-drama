<template>
    <el-container>
      <el-header>
        <Top></Top>
      </el-header>
      <el-container>
        <el-aside width="200px">
            <Left></Left>
        </el-aside>
        <el-main>
            <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
</template>

<script setup>
import { SwitchButton,Search } from '@element-plus/icons-vue'
import { ref } from 'vue'
import Top from '@/components/Top.vue'
import Left from '@/components/Left.vue'

</script>

<style scoped>
    .el-container {
        background-color: rgb(238, 238, 238);
        width: 100vw;
        height: 100vh;
    }
    .el-header{
        background-image: url(../assets/logo2.png);
        height: 96px;
        line-height: 96px;
    }
    
</style>