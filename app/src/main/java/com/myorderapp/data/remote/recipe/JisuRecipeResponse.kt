package com.myorderapp.data.remote.recipe

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JisuRecipeResponse(
    @Json(name = "status") val status: Int = -1,
    @Json(name = "msg") val msg: String = "",
    @Json(name = "result") val result: JisuRecipeResult? = null
) {
    val isSuccess: Boolean get() = status == 0 && result != null
    val errorMessage: String get() = when (status) {
        0 -> "成功"
        104 -> "请求超过次数限制"
        105 -> "请求过于频繁"
        106 -> "APPKEY无效"
        else -> msg.ifBlank { "未知错误($status)" }
    }
}

@JsonClass(generateAdapter = true)
data class JisuRecipeResult(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "num") val num: Int = 0,
    @Json(name = "list") val list: List<JisuRecipeItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JisuRecipeItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "classid") val classid: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "peoplenum") val peoplenum: String = "",
    @Json(name = "preparetime") val preparetime: String = "",
    @Json(name = "cookingtime") val cookingtime: String = "",
    @Json(name = "content") val content: String = "",
    @Json(name = "pic") val pic: String = "",
    @Json(name = "tag") val tag: String = "",
    @Json(name = "material") val material: List<JisuMaterial> = emptyList(),
    @Json(name = "process") val process: List<JisuProcessStep> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JisuMaterial(
    @Json(name = "mname") val mname: String = "",
    @Json(name = "type") val type: Int = 0,
    @Json(name = "amount") val amount: String = ""
)

@JsonClass(generateAdapter = true)
data class JisuProcessStep(
    @Json(name = "pcontent") val pcontent: String = "",
    @Json(name = "pic") val pic: String = ""
)
