import { createApp } from 'vue'
import App from './App.vue'
import router from "@/router/index.js";
import axios from "axios";
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

axios.defaults.baseURL = 'http://localhost:8080'

const app = createApp(App)

app.use(router)
app.use(ElementPlus)

app.mount('#app')
