import {ref, reactive, getCurrentInstance, onMounted} from "vue";
import {defineStore} from "pinia";
//export default defineStorei 必须是defineStore
export default defineStore('userStore', () => {

  const user = ref({
    username: 'admin'
  })

  return {
    user
  }
}, {
  persist: {
    storage: sessionStorage,//数据 存入  SessionStorage中
    paths: ['userStore']//把userModel属性 存入SessionStorage中
  }
})