import {createRouter, createWebHistory} from "vue-router";
import {unAuthorized} from "@/net/index.js";


const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes:[
        {
            path:"/",
            name:"welcome",
            component:()=>import("@/views/WelcomeView.vue"),
            children:[
                {
                    path:"",
                    name:"welcome-longin",
                    component:()=>import("@/views/welcome/LoginPage.vue")
                }
            ]
        },
        {
            path:"/index",
            name:"index",
            component:()=>import("@/views/IndexView.vue")
        }
    ]

})
//路由守卫
router.beforeEach((to,from,next)=>{
    const isUnauthorized = unAuthorized()
    if(to.name.startsWith('welcome-') && !isUnauthorized){//如果用户已经登录，不能再访问登录页面
        next('/index')//重定向到index主页
    }else if(to.fullPath.startsWith('/index') && isUnauthorized){
        next("/")//r如果没有登录就访问index主页，则重定向到登录页面
    }else{
        next()
    }
})


export default router