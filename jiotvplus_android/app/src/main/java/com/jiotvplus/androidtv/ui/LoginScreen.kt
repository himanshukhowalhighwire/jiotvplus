package com.jiotvplus.androidtv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.jiotvplus.androidtv.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    var mobileNumber by mutableStateOf("")
    var otp by mutableStateOf("")
    var identifier by mutableStateOf<String?>(null)
    var isOtpSent by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var loginSuccess by mutableStateOf(false)

    fun sendOtp() {
        viewModelScope.launch {
            val id = authRepository.sendOtpAndGetId(mobileNumber)
            if (id != null) {
                identifier = id
                isOtpSent = true
                error = null
            } else {
                error = "Failed to send OTP"
            }
        }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            if (identifier != null) {
                val success = authRepository.verifyOtp(mobileNumber, otp, identifier!!)
                if (success) {
                    loginSuccess = true
                    error = null
                } else {
                    error = "Invalid OTP"
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("JioTV+ Login", fontSize = 32.sp, color = Color.White, modifier = Modifier.padding(bottom = 32.dp))

        if (!viewModel.isOtpSent) {
            BasicTextField(
                value = viewModel.mobileNumber,
                onValueChange = { viewModel.mobileNumber = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = TextStyle(color = Color.Black, fontSize = 24.sp),
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            )
            Button(
                onClick = { viewModel.sendOtp() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Send OTP")
            }
        } else {
            BasicTextField(
                value = viewModel.otp,
                onValueChange = { viewModel.otp = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = Color.Black, fontSize = 24.sp),
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            )
            Button(
                onClick = { viewModel.verifyOtp() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Verify OTP")
            }
        }

        if (viewModel.error != null) {
            Text(viewModel.error!!, color = Color.Red, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
