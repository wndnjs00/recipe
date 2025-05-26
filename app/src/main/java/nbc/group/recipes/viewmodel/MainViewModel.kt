package nbc.group.recipes.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nbc.group.recipes.data.model.dto.Recipe
import nbc.group.recipes.data.model.entity.RecipeEntity
import nbc.group.recipes.data.model.firebase.UserMetaData
import nbc.group.recipes.data.network.NetworkResult
import nbc.group.recipes.data.repository.AuthRepository
import nbc.group.recipes.data.repository.FirebaseRepository
import nbc.group.recipes.data.repository.SpecialtyRepository
import nbc.group.recipes.data.utils.getUserProfileStoragePath
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SpecialtyRepository,
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository
): ViewModel() {

    /**
     * <Sample code>
     * private val _recipes = MutableStateFlow(listOf<Recipe>())
     * val recipes = _recipes.asStateFlow()
     *
     * */

    private val _splashFlow = MutableStateFlow(true)
    val splashFlow = _splashFlow.asStateFlow()

    private val _signInFlow = MutableStateFlow<NetworkResult<FirebaseUser>?>(null)
    val signInFlow = _signInFlow.asStateFlow()

    private val _signUpFlow = MutableStateFlow<NetworkResult<FirebaseUser>?>(null)
    val signUpFlow = _signUpFlow.asStateFlow()

    private val _makeRecipeFlow = MutableStateFlow<NetworkResult<Boolean>?>(null)
    val makeRecipeFlow = _makeRecipeFlow.asStateFlow()

    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user = _user.asStateFlow()
    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    private val _recipes = MutableStateFlow<NetworkResult<List<Recipe>>?>(null)
    val recipes = _recipes.asStateFlow()

    private val _putRecipeFlow = MutableStateFlow<NetworkResult<Boolean>?>(null)
    val putRecipeFlow = _putRecipeFlow.asStateFlow()

    private val _userMetaData = MutableStateFlow<NetworkResult<UserMetaData>?>(null)
    val userMetaData = _userMetaData.asStateFlow()

    init {
        if(authRepository.currentUser != null) {
            viewModelScope.launch {
                _signInFlow.emit(NetworkResult.Success(authRepository.currentUser!!))
            }
        }
    }

    fun homeFragmentStatusChange() {
        viewModelScope.launch {
            _splashFlow.emit(false)
        }
    }

    fun signIn(id: String, pw: String) = viewModelScope.launch {
        val result = authRepository.signIn(id, pw)
        _signInFlow.emit(result)
        _user.emit(authRepository.currentUser)
    }

    fun signUp(name: String, id: String, pw: String) = viewModelScope.launch {
        val result = authRepository.signUp(name, id, pw)
        _signUpFlow.emit(result)
        _user.emit(authRepository.currentUser)
    }

    fun resign() = viewModelScope.launch {
        authRepository.resign()
    }

    fun logout() = viewModelScope.launch {
        authRepository.logout()
        _signInFlow.emit(null)
        _signUpFlow.emit(null)
        _user.emit(null)
    }

    fun putRecipe(recipe: Recipe) = viewModelScope.launch {
        val result = firebaseRepository.putRecipe(recipe)
        _putRecipeFlow.emit(result)
    }

    fun getRecipe() = viewModelScope.launch {
        val result = firebaseRepository.getRecipes()
        _recipes.emit(result)
    }

    fun putRecipeTransaction(
        recipe: Recipe,
        imageStreamList: List<InputStream>
    ) = viewModelScope.launch {
        currentUser?.let {
            _makeRecipeFlow
                .emit(firebaseRepository.putRecipeTransaction(it.uid, recipe, imageStreamList))
        }
    }

    fun resetMakeRecipeFlow() {
        _makeRecipeFlow.value = null
    }

    fun putUserMeta(
        userMetaData: UserMetaData
    ) = viewModelScope.launch {
        currentUser?.let {
            Log.e("TAG", "putUserMeta: uid: ${it.uid}")
            firebaseRepository.putUserMeta(it.uid, userMetaData)
        }
    }

    fun getUserMeta(uid: String) = viewModelScope.launch {
        val result = firebaseRepository.getUserMeta(uid)
        _userMetaData.emit(result)
    }

    suspend fun getRecipeFromFirebaseById(id: String) =
        firebaseRepository.getRecipeByDocumentId(id)


    fun putImage(
        inputStream: InputStream
    ) = viewModelScope.launch {
        currentUser?.let {
            firebaseRepository.putImage(
                getUserProfileStoragePath(it.uid),
                inputStream
            )
        }
    }
}
