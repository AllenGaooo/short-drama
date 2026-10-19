import { ref } from 'vue'
import { defineStore } from 'pinia'

export default defineStore('userStore', () => {
  const token = ref('')
  const userId = ref('')
  const menus = ref([])

  return {
    token,
    userId,
    menus
  }
}, {
  persist: {
    storage: sessionStorage,
    paths: ['token', 'userId', 'menus']
  }
})
