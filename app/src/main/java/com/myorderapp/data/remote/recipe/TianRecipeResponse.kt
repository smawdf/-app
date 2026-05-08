package com.myorderapp.data.remote.recipe

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TianRecipeResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "msg") val msg: String = "",
    @Json(name = "result") val result: TianRecipeResult? = null
) {
    val isSuccess: Boolean get() = code == 200 && result != null
    val errorMessage: String get() = when (code) {
        200 -> "成功"
        150 -> "API可用次数不足"
        230 -> "API密钥无效"
        else -> msg.ifBlank { "未知错误($code)" }
    }
}

@JsonClass(generateAdapter = true)
data class TianRecipeResult(
    @Json(name = "curpage") val curpage: Int = 1,
    @Json(name = "allnum") val allnum: Int = 0,
    @Json(name = "list") val list: List<TianRecipeItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TianRecipeItem(
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
