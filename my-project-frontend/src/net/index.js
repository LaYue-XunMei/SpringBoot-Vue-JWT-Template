import axios from 'axios'
import {ElMessage} from "element-plus";

const authItemName = "access_token"

const defaultFailure = (message,code,url)=>{
    console.warn(`请求地址:${url} 状态码:${code} 错误信息:${message}`)
    ElMessage.warning(message)
}
const defaultError = (err)=>{
    console.error(err)
    ElMessage.warning("发生了一些错误")
}

function storeAccessToken(token, remember, expire){
    const authObj = {
        token:token,
        expire:expire
    }//先封装一下
    const str = JSON.stringify(authObj)//只能存储字符串，先字符串化
    if(remember)
        localStorage.setItem(authItemName, str)
    else
        sessionStorage.setItem(authItemName,str)
}

function takeAccessToken(){
    const str = localStorage.getItem(authItemName) || sessionStorage.getItem(authItemName)
    if(!str) return null
    const authObj = JSON.parse(str)//重新封装成object
    if(authObj.expire<=new Date()){
        deleteAccessToken()
        ElMessage.warning("登录状态已过期，请重新登录")
        return null
    }
    return authObj.token //返回对象的token字段
}

function deleteAccessToken(){
    localStorage.removeItem(authItemName)
    sessionStorage.removeItem(authItemName)
}

function accessHeader(){//获取请求头
    const token = takeAccessToken();
    return token ? {'Authorization': `Bearer ${takeAccessToken()}`} : {}
}


/**
 * 内部post请求
 * @param {string} url - 请求的url
 * @param {object} data - 请求的数据
 * @param {object} header - 请求的头
 * @param {function} success - 成功回调函数，参数为请求返回的数据
 * @param {function} failure - 失败回调函数，参数分别为错误信息、状态码、url
 * @param {function} error - 错误回调函数，参数为错误对象
 */
function internalPost(url, data, header, success, failure, error = defaultError){
    axios.post(url, data, {headers: header}).then(({data})=>{//then是请求得到data数据后的处理
        if(data.code === 200){
            success(data.data)
        }else{
            failure(data.message,data.code,url)
        }
    }).catch(err=>error(err))
}

function internalGet(url, header, success, failure, error = defaultError){
    axios.get(url, {headers: header}).then(({data})=>{
        if(data.code === 200){
            success()
        }else {
            failure()
        }
    }).catch(err=>error(err))
}

function get(url, success, failure = defaultFailure){
    internalGet(url, accessHeader(), success, failure)
}

function post(url, data, success, failure = defaultFailure){
    internalPost(url, data, accessHeader(), success, failure)
}

function login(username, password, remember, success, failure =defaultFailure){
    internalPost("/api/auth/login", {
        username:username,
        password:password
    },{//axios默认以JSON格式传递数据，而SpringSecurity只支持表单登录,只能以表单形式提交
        "Content-Type": "application/x-www-form-urlencoded"
    },(data)=>{//成功回调
        storeAccessToken(data.token, remember, data.expire)
        ElMessage.success(`登录成功，欢迎${data.username}来到我们系统。`)
        success(data)
    }, failure)
}

function logout(success, failure=defaultFailure){//500报错，记得开启redis服务
    get("/api/auth/logout",()=>{
        deleteAccessToken()
        ElMessage.success("退出登录成功！")
        success()
    },failure)
}

function unAuthorized(){
    return !takeAccessToken()//没有token说明没有登录
}

export { login, logout, get, post, unAuthorized}