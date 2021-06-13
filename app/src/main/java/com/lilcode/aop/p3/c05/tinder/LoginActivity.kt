package com.lilcode.aop.p3.c05.tinder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth // 코틀린 스러운 가져오기;

        callbackManager = CallbackManager.Factory.create() // 콜백 매니저 초기화;

        initLoginButton()

        initSignupButton()

        initEmailAndPasswordEditText()

        initFacebookLoginButton()

    }

    private fun initFacebookLoginButton() {


        val facebookLoginButton = findViewById<LoginButton>(R.id.facebookLoginButton)

        // 가져올 정보;
        facebookLoginButton.setPermissions("email","public_profile")
        facebookLoginButton.registerCallback(callbackManager, object: FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult) {
                // 로그인 성공;

                // 로그인 결과에서 엑세스 토큰을 가져옴
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)

                // 페이스북 로그인 엑세스 토큰을 넘겨주어 로그
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this@LoginActivity) { task ->
                        if(task.isSuccessful){
                            finish()
                        }else{
                            Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패했습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }

            override fun onCancel() {
               // 로그인 취소;
            }

            override fun onError(error: FacebookException?) {
               Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패했습니다.", Toast.LENGTH_SHORT)
                   .show()
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun initEmailAndPasswordEditText() {
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signupButton)

        // 비어있는 경우 처리;
        emailEditText.addTextChangedListener {
            // 입력 될 때마다 체크
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }

        passwordEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }
    }

    private fun initLoginButton() {
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener {

            val email = getInputEmail()
            val password = getInputPassword()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful){ // 로그인 성공;

                        finish() // 엑티비티 종료;
                    } else{
                        Toast.makeText(this, "로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT)
                            .show()

                    }
                }

        }
    }

    private fun initSignupButton() {
        val signUpButton = findViewById<Button>(R.id.signupButton)
        signUpButton.setOnClickListener {
            val email = getInputEmail()
            val password = getInputPassword()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if(task.isSuccessful){
                      Toast.makeText(this, "회원가입에 성공했습니다. 로그인 버튼을 눌러 로그인 해주세요.", Toast.LENGTH_SHORT)
                          .show()
                    } else{
                        // 2021-06-12 18:26:30.011 19535-19535/com.lilcode.aop.p3.c05.tinder D/debug: com.google.firebase.auth.FirebaseAuthWeakPasswordException: The given password is invalid. [ Password should be at least 6 characters ]
                        Log.d("debug",task.exception.toString())
                        Toast.makeText(this, "이미 가입된 이메일 이거나 회원가입에 실패했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

        }
    }

    private fun getInputEmail(): String {
        return findViewById<EditText>(R.id.emailEditText).text.toString()
    }

    private fun getInputPassword(): String {
        return findViewById<EditText>(R.id.passwordEditText).text.toString()
    }

}