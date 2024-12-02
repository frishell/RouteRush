package com.example.routerush.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routerush.data.UserRepository
import com.example.routerush.data.datastore.UserModel
import com.example.routerush.data.datastore.UserPreference
import com.example.routerush.data.response.LoginAndRegisterResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException


class LoginViewModel(private val repository: UserRepository,
                     private val userPreference: UserPreference
) : ViewModel() {


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isError = MutableLiveData<String?>()
    val isError: MutableLiveData<String?> get() = _isError

    private val _loginResponse = MutableLiveData<LoginAndRegisterResponse>()
    val loginResponse: LiveData<LoginAndRegisterResponse> = _loginResponse

    private suspend fun fetchUserName(): String {
        return userPreference.getSession()
            .first()
            .name
    }
    fun login(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.login(email, password)
                val loginResult = response.user



                val userId = loginResult?.uid ?: ""
                fetchUserName()
                val name = fetchUserName()

                val user = UserModel(
                    userId = userId,
                    name = name,
                    email = email,
                    isLogin = true
                )
                saveAuth(user)
                _loginResponse.postValue(response)
            } catch (e: HttpException) {
                httpException(e)
            } catch (e: Exception) {
                generalException(e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    private fun saveAuth(userModel: UserModel) {
        viewModelScope.launch {
            repository.saveAuth(userModel)
        }
    }
    private fun httpException(e: HttpException) {
        val jsonInString = e.response()?.errorBody()?.string()
        val errorBody = Gson().fromJson(jsonInString, LoginAndRegisterResponse::class.java)
        _isError.postValue(errorBody.message)
    }
    private fun generalException(e: Exception) {
        _isError.postValue(e.message ?: "An unexpected error occurred")
    }
}