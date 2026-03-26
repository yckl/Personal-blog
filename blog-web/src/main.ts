import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import i18n from './i18n'
import { createHead } from '@vueuse/head'
import App from './App.vue'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(createHead())
app.use(router)
app.use(i18n)
app.mount('#app')
