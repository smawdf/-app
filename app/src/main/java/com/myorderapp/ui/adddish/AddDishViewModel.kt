package com.myorderapp.ui.adddish

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.remote.supabase.SupabaseStorageUploader
import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AddDishUiState(
    val editDishId: String? = null,
    val name: String = "",
    val category: String = "中餐",
    val difficulty: Int = 1,
    val cookTimeMin: String = "",
    val imageUrl: String = "",
    val ingredients: List<String> = emptyList(),
    val ingredientInput: String = "",
    val cookSteps: List<CookStep> = emptyList(),
    val notes: String = "",
    val whoLikesYou: Boolean = false,
    val whoLikesPartner: Boolean = false,
    val myName: String = "我",
    val partnerName: String = "她",
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val uploadMessage: String? = null
)

class AddDishViewModel(
    private val dishRepository: DishRepository,
    private val profileRepository: ProfileRepository,
    private val storageUploader: SupabaseStorageUploader,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDishUiState())
    val uiState: StateFlow<AddDishUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profileRepository.getProfile().first()
            val myNick = profile?.nickname?.ifBlank { null } ?: "我"
            val pairInfo = profileRepository.getPairInfo()
            val partnerNick = pairInfo.partnerName.ifBlank { "她" }
            _uiState.value = _uiState.value.copy(myName = myNick, partnerName = partnerNick)
        }
    }

    fun onNameChanged(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun onCategoryChanged(category: String) { _uiState.value = _uiState.value.copy(category = category) }
    fun onDifficultyChanged(difficulty: Int) { _uiState.value = _uiState.value.copy(difficulty = difficulty) }
    fun onCookTimeChanged(time: String) { _uiState.value = _uiState.value.copy(cookTimeMin = time) }

    fun onIngredientInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(ingredientInput = input)
    }

    fun addIngredient() {
        val input = _uiState.value.ingredientInput.trim()
        if (input.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                ingredients = _uiState.value.ingredients + input,
                ingredientInput = ""
            )
        }
    }

    fun removeIngredient(index: Int) {
        _uiState.value = _uiState.value.copy(
            ingredients = _uiState.value.ingredients.toMutableList().also { it.removeAt(index) }
        )
    }

    fun addStep() {
        _uiState.value = _uiState.value.copy(
            cookSteps = _uiState.value.cookSteps + CookStep(
                step = _uiState.value.cookSteps.size + 1,
                description = ""
            )
        )
    }

    fun updateStep(index: Int, description: String) {
        val steps = _uiState.value.cookSteps.toMutableList()
        if (index < steps.size) {
            steps[index] = steps[index].copy(description = description)
            _uiState.value = _uiState.value.copy(cookSteps = steps)
        }
    }

    fun removeStep(index: Int) {
        val steps = _uiState.value.cookSteps.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(cookSteps = steps.mapIndexed { i, s -> s.copy(step = i + 1) })
    }

    fun onImageUrlChanged(url: String) { _uiState.value = _uiState.value.copy(imageUrl = url) }
    fun onNotesChanged(notes: String) { _uiState.value = _uiState.value.copy(notes = notes) }
    fun toggleWhoLikesYou() { _uiState.value = _uiState.value.copy(whoLikesYou = !_uiState.value.whoLikesYou) }
    fun toggleWhoLikesPartner() { _uiState.value = _uiState.value.copy(whoLikesPartner = !_uiState.value.whoLikesPartner) }

    fun loadDishForEdit(dishId: String) {
        viewModelScope.launch {
            val dish = dishRepository.getDishById(dishId) ?: return@launch
            _uiState.value = _uiState.value.copy(
                editDishId = dishId,
                name = dish.name,
                category = dish.category,
                difficulty = dish.difficulty,
                cookTimeMin = dish.cookTimeMin.toString(),
                imageUrl = dish.imageUrl ?: "",
                ingredients = dish.ingredients,
                cookSteps = dish.cookSteps,
                notes = dish.notes,
                whoLikesYou = dish.whoLikes.contains(_uiState.value.myName),
                whoLikesPartner = dish.whoLikes.contains(_uiState.value.partnerName) ||
                    dish.whoLikes.any { it != _uiState.value.myName && it != "我" && it != "你" }
            )
        }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        _uiState.value = current.copy(isSaving = true, uploadMessage = null)
        viewModelScope.launch {
            val state = _uiState.value
            val whoLikes = buildList {
                if (state.whoLikesYou) add(state.myName)
                if (state.whoLikesPartner) add(state.partnerName)
            }

            // 先保存菜品（获取 dishId）
            val dishId = if (state.editDishId != null) {
                dishRepository.updateDish(Dish(
                    id = state.editDishId,
                    name = state.name, category = state.category,
                    difficulty = state.difficulty,
                    cookTimeMin = state.cookTimeMin.toIntOrNull() ?: 0,
                    imageUrl = state.imageUrl,
                    ingredients = state.ingredients, cookSteps = state.cookSteps,
                    notes = state.notes, whoLikes = whoLikes,
                    source = "custom", createdBy = "${state.myName}创建"
                ))
                state.editDishId
            } else {
                dishRepository.addDish(Dish(
                    name = state.name, category = state.category,
                    difficulty = state.difficulty,
                    cookTimeMin = state.cookTimeMin.toIntOrNull() ?: 0,
                    imageUrl = state.imageUrl,
                    ingredients = state.ingredients, cookSteps = state.cookSteps,
                    notes = state.notes, whoLikes = whoLikes,
                    source = "custom", createdBy = "${state.myName}创建"
                ))
            }

            // 如果图片是本地 URI（拍照/相册），压缩并上传到云端
            val imgUrl = state.imageUrl
            if (imgUrl.isNotBlank() && (imgUrl.startsWith("content://") || imgUrl.startsWith("file://"))) {
                val result = storageUploader.compressAndUpload(
                    appContext, Uri.parse(imgUrl), dishId
                )
                if (result.publicUrl != null) {
                    dishRepository.updateDish(Dish(
                        id = dishId, name = state.name, category = state.category,
                        difficulty = state.difficulty,
                        cookTimeMin = state.cookTimeMin.toIntOrNull() ?: 0,
                        imageUrl = result.publicUrl,
                        ingredients = state.ingredients, cookSteps = state.cookSteps,
                        notes = state.notes, whoLikes = whoLikes,
                        source = "custom", createdBy = "${state.myName}创建"
                    ))
                    _uiState.value = _uiState.value.copy(imageUrl = result.publicUrl, savedSuccess = true, isSaving = false, uploadMessage = "图片上传成功")
                } else {
                    _uiState.value = _uiState.value.copy(savedSuccess = true, isSaving = false, uploadMessage = result.error ?: "图片未上传到云端")
                }
            } else {
                _uiState.value = _uiState.value.copy(savedSuccess = true, isSaving = false)
            }
        }
    }
}
