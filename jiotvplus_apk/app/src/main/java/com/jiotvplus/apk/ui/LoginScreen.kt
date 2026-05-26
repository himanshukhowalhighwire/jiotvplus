package com.jiotvplus.apk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiotvplus.apk.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    var mobileNumber by mutableStateOf("")
    var otp by mutableStateOf("")
    var isOtpSent by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun sendOtp() {
        if (mobileNumber.length < 10) {
            error = "Enter valid mobile number"
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            val result = authRepository.sendOtp(mobileNumber)
            if (result.isSuccess) {
                isOtpSent = true
            } else {
                error = result.exceptionOrNull()?.message ?: "Failed to send OTP"
            }
            isLoading = false
        }
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        if (otp.length < 4) {
            error = "Enter valid OTP"
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            val result = authRepository.verifyOtp(mobileNumber, otp)
            if (result.isSuccess) {
                onSuccess()
            } else {
                error = result.exceptionOrNull()?.message ?: "Failed to verify OTP"
            }
            isLoading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("JioTV+", fontSize = 48.sp, color = Color.White, modifier = Modifier.padding(bottom = 32.dp))

        if (viewModel.error != null) {
            Text(viewModel.error!!, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
        }

        if (!viewModel.isOtpSent) {
            OutlinedTextField(
                value = viewModel.mobileNumber,
                onValueChange = { viewModel.mobileNumber = it },
                label = { Text("Mobile Number", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    focusedBorderColor = Color.Blue,
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.sendOtp() },
                enabled = !viewModel.isLoading
            ) {
                Text(if (viewModel.isLoading) "Sending..." else "Send OTP")
            }
        } else {
            OutlinedTextField(
                value = viewModel.otp,
                onValueChange = { viewModel.otp = it },
                label = { Text("Enter OTP", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    focusedBorderColor = Color.Blue,
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.verifyOtp(onLoginSuccess) },
                enabled = !viewModel.isLoading
            ) {
                Text(if (viewModel.isLoading) "Verifying..." else "Login")
            }
        }
    }
}
