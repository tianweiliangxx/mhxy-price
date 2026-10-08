import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import MarketView from './views/MarketView.vue'
import RegionView from './views/RegionView.vue'
import BargainView from './views/BargainView.vue'
import TrendView from './views/TrendView.vue'
import StallDataView from './views/StallDataView.vue'
import ScreenshotView from './views/ScreenshotView.vue'
import WarehouseView from './views/WarehouseView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/buy/today', component: MarketView, meta: { title: '今日摊位物价', side: '收购', hint: '按当前录入的摊位单价查价，看现在卖什么、卖多少。' } },
    { path: '/buy/region', component: RegionView, meta: { title: '全区物价', side: '收购' } },
    { path: '/buy/market', component: MarketView, meta: { title: '行情列表', side: '收购', hint: '把已录入的价格整理成参考行情，可按名称、分类和更新记录查看。' } },
    { path: '/buy/bargains', component: BargainView },
    { path: '/buy/trends', component: TrendView },
    { path: '/stall/data', component: StallDataView },
    { path: '/stall/today', component: MarketView, meta: { title: '今天摊位物价', side: '摆摊', hint: '查看摆摊侧已经录入的出售单价。' } },
    { path: '/stall/region', component: RegionView, meta: { title: '全区物价', side: '摆摊' } },
    { path: '/screenshot', component: ScreenshotView },
    { path: '/warehouse', component: WarehouseView },
  ],
})

export default router
