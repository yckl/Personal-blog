import { createI18n } from 'vue-i18n'
import zhCN from '../locales/zh-CN.json'
import enUS from '../locales/en-US.json'

/**
 * Detect the user's preferred language:
 * 1. Check localStorage for a previously saved preference
 * 2. Fall back to browser language
 * 3. Default to en-US
 */
function getDefaultLocale(): string {
  const saved = localStorage.getItem('locale')
  if (saved && ['zh-CN', 'en-US'].includes(saved)) return saved

  const browserLang = navigator.language
  if (browserLang.startsWith('zh')) return 'zh-CN'
  return 'en-US'
}

const i18n = createI18n({
  legacy: false, // use Composition API mode
  locale: getDefaultLocale(),
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export default i18n
