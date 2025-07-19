import {boot} from 'quasar/wrappers'
import {createI18n} from 'vue-i18n'
import messages from 'src/i18n'

let lang = localStorage.getItem("curLang");
if (!lang) {
    lang = "ru";
    localStorage.setItem("curLang", lang);
}

export default boot(({ app }) => {
  const i18n = createI18n({
    locale: lang,
    globalInjection: true,
    messages
  })

  // Set i18n instance on app
  app.use(i18n)
})
