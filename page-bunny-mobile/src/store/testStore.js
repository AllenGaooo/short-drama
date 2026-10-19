import {ref, reactive, getCurrentInstance, onMounted} from "vue";
import {defineStore} from "pinia";
//export default defineStorei 必须是defineStore
export default defineStore('testStore', () => {

  const user = ref({
    username: 'admin',
    password: '123456'
  })

  return {
    user
  }
}, {
  persist: {
    storage: localStorage,//数据 存入  SessionStorage中
    paths: ['testStore']//把userModel属性 存入SessionStorage中
  }
})