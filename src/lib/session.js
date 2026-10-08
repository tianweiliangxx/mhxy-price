import { ref, watch } from 'vue'

const STORAGE = 'mhxy-profile'

function loadProfile() {
  try {
    return {
      nickname: '',
      boundServer: '北京1区[紫禁城]',
      boundOn: '',
      points: 0,
      characters: [],
      ...JSON.parse(localStorage.getItem(STORAGE) || '{}'),
    }
  } catch {
    return { nickname: '', boundServer: '北京1区[紫禁城]', boundOn: '', points: 0, characters: [] }
  }
}

export const serverGroup = ref('正式服')
export const serverName = ref('北京1区[紫禁城]')
export const servers = ['北京1区[紫禁城]', '夫子庙', '再续前缘', '2008', '生日快乐', '兰亭序', '龙腾', '梦回唐朝']
export const profile = ref(loadProfile())

watch(profile, (value) => {
  localStorage.setItem(STORAGE, JSON.stringify(value))
  serverName.value = value.boundServer || serverName.value
}, { deep: true })

if (profile.value.boundServer) serverName.value = profile.value.boundServer

export function todayText() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

export function canRebind() {
  return profile.value.boundOn !== todayText()
}
