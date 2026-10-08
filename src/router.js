import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import MarketView from './views/MarketView.vue'
import RegionView from './views/RegionView.vue'
import BargainView from './views/BargainView.vue'
import FindStallView from './views/FindStallView.vue'
import TrendView from './views/TrendView.vue'
import StallDataView from './views/StallDataView.vue'
import ScreenshotView from './views/ScreenshotView.vue'
import WarehouseView from './views/WarehouseView.vue'
import CaptureView from './views/CaptureView.vue'
import SessionsView from './views/SessionsView.vue'
import AlertView from './views/AlertView.vue'
import ProfileView from './views/ProfileView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/buy/capture', component: CaptureView, meta: { title: '摊位识别', side: '收购' } },
    { path: '/buy/sessions', component: SessionsView },
    { path: '/buy/today', component: MarketView, meta: { title: '今日摊位物价', side: '收购', hint: '只显示所选日期内的物品统计。本地录入只有一条价格时，最高价、均价和 7 天价相同。' } },
    { path: '/buy/region', component: RegionView, meta: { title: '全区物价', side: '收购' } },
    { path: '/buy/market', component: MarketView, meta: { title: '行情列表', side: '收购', hint: '参考行情带日期范围。人民币按顶栏金价 219 元/3000W 换算。' } },
    { path: '/buy/bargains', component: BargainView },
    { path: '/buy/locate', component: FindStallView },
    { path: '/buy/trends', component: TrendView },
    { path: '/buy/alerts', component: AlertView },
    { path: '/stall/capture', component: CaptureView, meta: { title: '摆摊识别', side: '摆摊' } },
    { path: '/stall/data', component: StallDataView },
    { path: '/stall/today', component: MarketView, meta: { title: '今天摊位物价', side: '摆摊', hint: '查看摆摊侧已经录入的出售单价。' } },
    { path: '/stall/region', component: RegionView, meta: { title: '全区物价', side: '摆摊' } },
    { path: '/screenshot', component: ScreenshotView },
    { path: '/warehouse', component: WarehouseView },
    { path: '/me', component: ProfileView },
  ],
})

export default router
