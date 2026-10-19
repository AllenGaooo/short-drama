<template>
     <el-table height="500" :data="tableData" style="width: 100%">
        <el-table-column prop="id" label="编号" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="age" label="年龄" />
        <el-table-column prop="gender" label="性别" >
            <template #default="scope">
                <el-tag v-if="scope.row.gender === 1" type="success">男</el-tag>
                <el-tag v-else type="warning">女</el-tag>
            </template>

        </el-table-column>
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="regTime" width="200" label="注册时间" >
             <template #default="scope">
                <el-tag><el-icon><Clock /></el-icon></el-tag>
                <el-tag> {{ scope.row.regTime }}</el-tag>
                
            </template>
        </el-table-column>

    </el-table>
    <el-pagination
      v-model:current-page="pageInfo.page"
      v-model:page-size="pageInfo.pageSize"
      :total="pageInfo.total"
      :page-sizes="[10, 20, 30]"

      layout="total, sizes, prev, pager, next, jumper"
      
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />


</template>

<script setup>
import { SwitchButton,Search } from '@element-plus/icons-vue'
import { ref,onMounted } from 'vue'
import axios from 'axios'

const pageInfo = ref({
    page:1,
    pageSize:10,
    total:22
})

const handleCurrentChange = (page) => {
   console.log(`当前第${page}页`);
      axios.get(`http://127.0.0.1:4523/m2/8253284-8015093-default/456635755?apifoxApiId=456635755&page=${page}`)  
        .then(res=>{
            //res请求和响应完整数据 res.data 响应返回的实际json数据
            console.log(res.data);
            //给table赋值
            tableData.value = res.data.data.list
            //给页码对象赋值
            pageInfo.value = res.data.data.pageInfo
        })  
}

const handleSizeChange = (pageSize)=>{
     console.log(`每页${pageSize}条`);
}


const tableData = ref([])

onMounted(()=>{
   axios.get("http://127.0.0.1:4523/m2/8253284-8015093-default/456635755?apifoxApiId=456635755&page=1")  
   .then(res=>{
       //res请求和响应完整数据 res.data 响应返回的实际json数据
       console.log(res.data);
       //给table赋值
       tableData.value = res.data.data.list
       //给页码对象赋值
       pageInfo.value = res.data.data.pageInfo
   })  
})


</script>

<style scoped>

</style>