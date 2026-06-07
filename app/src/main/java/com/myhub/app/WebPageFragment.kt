package com.myhub.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * 외부 URL 하나를 보여주는 네이티브 WebView 페이지.
 * iframe 으로는 X-Frame-Options 때문에 대부분의 실사이트가 안 뜨므로
 * 페이지마다 독립된 WebView 를 쓴다. 쿠키 영속 + 로그인 팝업 + 파일 업로드 처리 포함.
 */
class WebPageFragment : Fragment() {

    private var webView: WebView? = null
    private var popupDialog: Dialog? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val entryId: String get() = arguments?.getString(ARG_ID) ?: ""
    private val entryUrl: String get() = arguments?.getString(ARG_URL) ?: "about:blank"

    private val fileChooser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val cb = fileCallback
            fileCallback = null
            if (cb == null) return@registerForActivityResult
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            cb.onReceiveValue(uris)
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_web, container, false) as SwipeRefreshLayout
        val webContainer = root.findViewById<FrameLayout>(R.id.webContainer)

        root.setColorSchemeColors(Color.parseColor("#2DD4BF"))
        root.setProgressBackgroundColorSchemeColor(Color.parseColor("#11151C"))

        val wv = WebView(requireContext())
        webView = wv
        webContainer.addView(
            wv,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        configure(wv)
        attachClients(wv, root)

        // 위로 스크롤되어 있을 때만 당겨서 새로고침 동작
        wv.setOnScrollChangeListener { _, _, _, _, _ ->
            root.isEnabled = wv.scrollY == 0
        }
        root.setOnRefreshListener { wv.reload() }

        wv.loadUrl(entryUrl)
        return root
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // 기본(Chrome) User-Agent 를 유지해 로그인/렌더링 호환성을 최대화한다.
        }
        // 로그인 세션 유지를 위한 쿠키 영속 설정
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }
    }

    private fun attachClients(wv: WebView, refresh: SwipeRefreshLayout) {
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                // 외부 스킴(전화/메일/스토어 등)은 시스템에 위임, 나머지는 WebView 안에서 처리
                if (url.startsWith("http://") || url.startsWith("https://")) return false
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                } catch (e: Exception) {
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                refresh.isRefreshing = false
                CookieManager.getInstance().flush()
                host()?.onPageTitle(entryId, view.title ?: "")
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                host()?.onPageProgress(entryId, newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                host()?.onPageTitle(entryId, title ?: "")
            }

            // 로그인 팝업(window.open / target=_blank) → 다이얼로그 WebView 로 처리
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val ctx = context ?: return false
                val popup = WebView(ctx)
                configure(popup)
                popup.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        v: WebView, req: WebResourceRequest
                    ): Boolean = false
                }
                popup.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView) {
                        dismissPopup()
                    }
                }

                val dialog = Dialog(ctx, android.R.style.Theme_Black_NoTitleBar)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(popup)
                dialog.setOnDismissListener { popupDialog = null }
                dialog.show()
                popupDialog = dialog

                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = popup
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView) {
                dismissPopup()
            }

            // 로그인 폼 안의 파일 첨부(예: 인증서/이미지 업로드)
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback
                return try {
                    fileChooser.launch(fileChooserParams.createIntent())
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }
        }
    }

    private fun dismissPopup() {
        popupDialog?.let { if (it.isShowing) it.dismiss() }
        popupDialog = null
    }

    private fun host(): PageHost? = activity as? PageHost

    /** 메인 액티비티에서 호출 */
    fun reload() = webView?.reload()
    fun canGoBack(): Boolean = webView?.canGoBack() == true
    fun goBack() {
        webView?.goBack()
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onDestroyView() {
        dismissPopup()
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.stopLoading()
            it.destroy()
        }
        webView = null
        super.onDestroyView()
    }

    /** 메인 액티비티가 진행률·제목을 받기 위한 콜백 */
    interface PageHost {
        fun onPageProgress(id: String, progress: Int)
        fun onPageTitle(id: String, title: String)
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_URL = "url"
        fun create(id: String, url: String): WebPageFragment {
            val f = WebPageFragment()
            f.arguments = Bundle().apply {
                putString(ARG_ID, id)
                putString(ARG_URL, url)
            }
            return f
        }
    }
}
