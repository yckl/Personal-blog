import request from './request'

/**
 * Fetches site SEO/analytics config from backend and injects
 * GA4, Plausible, Search Console verification, and custom scripts.
 * Called once on app mount.
 */
export async function initExternalAnalytics() {
  try {
    const res: any = await request.get('/api/site/seo')
    const config: Record<string, string> = res.data || {}

    // Google Analytics 4
    const ga4Id = config['ga4_measurement_id']
    if (ga4Id) {
      const gtagScript = document.createElement('script')
      gtagScript.async = true
      gtagScript.src = `https://www.googletagmanager.com/gtag/js?id=${ga4Id}`
      document.head.appendChild(gtagScript)

      const inlineScript = document.createElement('script')
      inlineScript.textContent = `
        window.dataLayer = window.dataLayer || [];
        function gtag(){dataLayer.push(arguments);}
        gtag('js', new Date());
        gtag('config', '${ga4Id}');
      `
      document.head.appendChild(inlineScript)
    }

    // Google Search Console verification
    const gscVerification = config['google_search_console_verification']
    if (gscVerification) {
      const meta = document.createElement('meta')
      meta.name = 'google-site-verification'
      meta.content = gscVerification
      document.head.appendChild(meta)
    }

    // Plausible Analytics
    const plausibleDomain = config['plausible_domain']
    if (plausibleDomain) {
      const scriptUrl = config['plausible_script_url'] || 'https://plausible.io/js/script.js'
      const plausibleScript = document.createElement('script')
      plausibleScript.defer = true
      plausibleScript.dataset.domain = plausibleDomain
      plausibleScript.src = scriptUrl
      document.head.appendChild(plausibleScript)
    }

    // Custom head scripts (raw HTML injection — admin-controlled)
    const customScripts = config['custom_head_scripts']
    if (customScripts) {
      const container = document.createElement('div')
      container.innerHTML = customScripts
      Array.from(container.children).forEach(child => {
        document.head.appendChild(child.cloneNode(true))
      })
    }
  } catch (e) {
    // Silently fail — analytics should never block the app
    console.warn('[Analytics] Failed to load external analytics config:', e)
  }
}
