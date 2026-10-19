<template>
    <el-button>测试按钮</el-button>
</template>

<script setup>
import { SwitchButton,Search } from '@element-plus/icons-vue'
import { ref } from 'vue'
import testStore from '@/store/testStore.js';
import axios from 'axios'

//console.log(testStore().user);
testStore().user.username='张三'
console.log(axios);

axios.post("/xxx")

</script>

<style scoped>

</style>