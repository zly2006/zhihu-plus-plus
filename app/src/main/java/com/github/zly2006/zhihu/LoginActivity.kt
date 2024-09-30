package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.databinding.ActivityLoginBinding
import com.github.zly2006.zhihu.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking
import java.net.URLDecoder

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        @SuppressLint("SetJavaScriptEnabled")
        binding.web.settings.javaScriptEnabled = true
        CookieManager.getInstance().removeAllCookies { }
        binding.web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request?.url.toString() == "https://www.zhihu.com/") {
                    val cookies = CookieManager.getInstance().getCookie("https://www.zhihu.com/").split(";").associate {
                        it.substringBefore("=") to it.substringAfter("=")
                    }
                    return runBlocking {
                        if (AccountData.verifyLogin(this@LoginActivity, cookies)) {
                            val data = AccountData.getData(this@LoginActivity)

                            AlertDialog.Builder(this@LoginActivity).apply {
                                setTitle("登录成功")
                                setMessage("欢迎回来，${data.username}")
                                setPositiveButton("OK") { _, _ ->
                                }
                            }.create().show()
                            return@runBlocking true
                        }
                        else {
                            AlertDialog.Builder(this@LoginActivity).apply {
                                setTitle("登录失败")
                                setMessage("请检查用户名和密码")
                                setPositiveButton("OK") { _, _ ->
                                }
                            }.create().show()
                            return@runBlocking false
                        }
                    }
                }
                return false
            }

        }
        binding.web.loadUrl("https://www.zhihu.com/signin")
    }

}