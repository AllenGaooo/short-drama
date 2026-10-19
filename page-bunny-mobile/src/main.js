import { createApp } from 'vue'
import App from './App.vue'

//理论上 所有的引入的js都在这运行 也都在写
//1router插件
import router from './router'
//2elementPlus插件
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
//3pinia 状态管理插件 (跨组件共享数据)
//导入pinia
import { createPinia } from 'pinia'
//导入pinia持久化插件(利用webstorage)
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';

//把pinia创建出来 持久化插件配置
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

//如果是插件 需要调用use命令
//如果是依赖 可以直接使用

//1习惯去看不同技术的官网 
//2面对任何困难都不要怕 勇敢的面对他

//创建vue对象
const app = createApp(App)
//在这里写use命令
//通过use命令启用插件
app.use(router)
app.use(ElementPlus,{
  locale: zhCn,
})
//elementPlus图标 全局注册到vue的组件库
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
//创建pinia实例
app.use(pinia)

//vue挂载
app.mount('#app')
