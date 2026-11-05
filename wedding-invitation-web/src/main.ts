import { createApp } from 'vue'
import App from './App.vue'
import './styles.css'
import { initializeWechatShare } from './services/wechatShare'

createApp(App).mount('#app')
void initializeWechatShare()
