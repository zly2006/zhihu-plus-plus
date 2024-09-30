package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
                    binding.web.loadUrl("https://www.zhihu.com/question/586608436/answer/3122557790")
                    return true
                }
                return false
            }

            var loadedJS = false

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                if (url == "https://static.zhihu.com/zse-ck/v3.js") {
                    loadedJS = true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == "https://www.zhihu.com/question/586608436/answer/3122557790") {
                    val cookies = CookieManager.getInstance().getCookie("https://www.zhihu.com/").split(";").associate {
                        it.substringBefore("=").trim() to it.substringAfter("=")
                    }
                    runBlocking {
                        if (loadedJS && AccountData.verifyLogin(this@LoginActivity, cookies)) {
                            val data = AccountData.getData(this@LoginActivity)

                            AlertDialog.Builder(this@LoginActivity).apply {
                                setTitle("登录成功")
                                setMessage("欢迎回来，${data.username}")
                                setPositiveButton("OK") { _, _ ->
                                }
                            }.create().show()
                            // back to the main activity
                            GlobalScope.launch {
                                delay(5000)
                                runOnUiThread {
                                    binding.web.evaluateJavascript("document.cookie") {
                                        data.cookies.putAll(it.removeSurrounding("\"")
                                            .removeSurrounding("\'")
                                            .split(";").associate {
                                            it.substringBefore("=").trim() to it.substringAfter("=")
                                        })
                                        if ("__zse_ck" !in data.cookies) {
                                            AccountData.saveData(this@LoginActivity, AccountData.Data())
                                            AlertDialog.Builder(this@LoginActivity).apply {
                                                setTitle("登录失败")
                                                setMessage("模拟正常登录环境失败，请检查网络")
                                                setPositiveButton("OK") { _, _ ->
                                                }
                                            }.create().show()
                                        }
                                        else {
                                            AccountData.saveData(this@LoginActivity, data)
                                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                        }
                                    }
                                }
                            }
                            return@runBlocking true
                        } else {
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
            }
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.web, true)
        binding.web.loadUrl("https://www.zhihu.com/signin")
    }

}
