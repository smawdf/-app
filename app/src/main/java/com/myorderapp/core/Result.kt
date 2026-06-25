package com.myorderapp.core

/**
 * 统一结果封装，替代直接 try-catch
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String = exception.message ?: "未知错误") : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun exceptionOrNull(): Exception? = (this as? Error)?.exception

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Exception, message: String = exception.message ?: "未知错误"): Result<Nothing> = Error(exception, message)
        fun error(message: String): Result<Nothing> = Error(Exception(message), message)
    }
}

/**
 * 将 suspend 函数包装为 Result
 */
suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }
}
