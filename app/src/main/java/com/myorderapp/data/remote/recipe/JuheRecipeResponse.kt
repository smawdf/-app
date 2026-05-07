package com.myorderapp.data.remote.recipe

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


data class JuheRecipeResponse(
    @Json(name = "error_code") val errorCode: Int = 0,
    @Json(name = "reason") val reason: String = "",
    @Json(name = "resultcode") val resultCode: String? = null,
    @Json(name = "result") val result: JuheRecipeResult? = null
) {
    val isSuccess: Boolean get() = errorCode == 0 && result != null
    val errorMessage: String get() = when (errorCode) {
        0 -> "成功"
        273300 -> "网络超时，请稍后再试"
        273301 -> "请输入菜名或食材"
        273302 -> "服务异常，请稍后重试"
        273303 -> "网络错误，请检查网络"
        10001 -> "API密钥配置错误"
        10002 -> "API密钥无权限"
        10003 -> "API密钥已过期"
        10011 -> "请求过于频繁，请稍后再试"
        10012 -> "今日请求次数已用完"
        else -> reason.ifBlank { "未知错误($errorCode)" }
    }
}

data class JuheRecipeResult(
    @Json(name = "curpage") val curpage: Int = 1,
    @Json(name = "allnum") val allnum: Int = 0,
    @Json(name = "list") val list: List<JuheRecipeItem> = emptyList()
)


data class JuheRecipeItem(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "type_id") val typeId: Int = 0,
    @Json(name = "type_name") val typeName: String = "",
    @Json(name = "cp_name") val cpName: String = "",
    @Json(name = "zuofa") val zuofa: String = "",
    @Json(name = "texing") val texing: String = "",
    @Json(name = "tishi") val tishi: String = "",
    @Json(name = "tiaoliao") val tiaoliao: String = "",
    @Json(name = "yuanliao") val yuanliao: String = ""
)
