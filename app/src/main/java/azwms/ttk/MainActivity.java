package azwms.ttk;


import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private AssetWebServer server;
    private static int PORT = 8025;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //解决全屏时手机屏幕摄像头部位的白边问题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        // 初始化本地服务器
        server = new AssetWebServer(PORT, getAssets());

        try {
            server.start();
//            String serverUrl = "http://localhost:" + PORT + "/"; // 本地访问地址
//            Toast.makeText(this, "服务器已启动：" + serverUrl, Toast.LENGTH_LONG).show();
//            Log.d("AssetWebServer", "服务器启动，端口：" + PORT);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "服务器启动失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        setupWebView(); // 配置 WebView 参数
        loadTargetWebsite(); // 加载目标网站
        hideSystemUI(); // 隐藏系统栏
    }

    /**
     * 解析用户代理字符串，提取内核版本
     */
    private String parseUserAgent(String userAgent) {
        // 检查是否为Chromium内核（含Chrome标识）
        if (userAgent.contains("Chrome/")) {
            int chromeStart = userAgent.indexOf("Chrome/") + 7; // "Chrome/"长度为7
            int chromeEnd = userAgent.indexOf(" ", chromeStart);
            if (chromeEnd == -1) chromeEnd = userAgent.length();
            return "Chromium " + userAgent.substring(chromeStart, chromeEnd);
        }
        // 否则为WebKit内核（含AppleWebKit标识）
        else if (userAgent.contains("AppleWebKit/")) {
            int webkitStart = userAgent.indexOf("AppleWebKit/") + 12; // "AppleWebKit/"长度为12
            int webkitEnd = userAgent.indexOf(" ", webkitStart);
            if (webkitEnd == -1) webkitEnd = userAgent.length();
            return "WebKit " + userAgent.substring(webkitStart, webkitEnd);
        }
        // 未知内核
        return "未知内核";
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // 获取用户代理字符串（包含内核信息）
        String userAgent = webSettings.getUserAgentString();
        // 解析内核版本
        String kernelVersion = parseUserAgent(userAgent);
        // 显示Toast
        Toast.makeText(this, "WebView内核版本：" + kernelVersion, Toast.LENGTH_LONG).show();

        // 启用远程调试，chrome访问chrome://inspect
//        WebView.setWebContentsDebuggingEnabled(true);

        // 基础设置
        webSettings.setJavaScriptEnabled(true); // 启用 JavaScript
        webSettings.setDomStorageEnabled(true); // 启用 DOM 存储（HTML5 特性）
        webSettings.setAllowFileAccess(true); // 允许访问本地文件（缓存需要）
        webSettings.setAllowContentAccess(true); // 允许访问内容提供器
        // 允许媒体播放不依赖用户手势（部分 Android 版本生效）;此设置在 Android 10（API 29）及以上版本中可能不完全生效，具体取决于 Chromium 内核版本。
        webSettings.setMediaPlaybackRequiresUserGesture(false);//解决录像无声问题

        //优化设置
        webView.setLayerType(View.LAYER_TYPE_HARDWARE,null);//硬件层视图缓存策略，利用 GPU 内存缓存渲染结果以减少重复计算
//        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);//已弃用，现代WebView（基于Chromium）的渲染线程调度已高度优化

        //缓存策略
//        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT); // 优先使用缓存（根据 HTTP 缓存头自动判断）
        // 优先使用本地缓存资源，仅在缓存不存在时才尝试网络请求
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        //下面两个方法已不存在
//        webSettings.setAppCacheEnabled(true); // 启用应用缓存（兼容旧版，新版推荐 HTTP 缓存）
//        webSettings.setAppCachePath(getCacheDir().getPath()); // 缓存存储路径（应用私有目录）

        // 其他优化
        webSettings.setSupportZoom(false); // 禁止缩放
        webSettings.setBuiltInZoomControls(false); // 隐藏缩放控件
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕宽度
        webSettings.setUseWideViewPort(true); // 使用视口元标签
        webSettings.setDatabaseEnabled(true);//为 WebView 启用 Web SQL Database 支持
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);//关闭滚动回弹效果

        // 设置 WebViewClient 处理页面跳转和缓存
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 所有链接在当前 WebView 内加载（避免跳转到系统浏览器）
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });

        // 设置 WebChromeClient 处理媒体播放（如 MP3）
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadTargetWebsite() {
        webView.loadUrl("http://localhost:" + PORT+"/index.html");
    }

    // 隐藏系统状态栏和导航栏（全屏）
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY; // 沉浸式模式（滑动短暂显示系统栏后自动隐藏）
        decorView.setSystemUiVisibility(uiOptions);
    }

    // 重写返回键
    @Override
    public void onBackPressed() {
        // 弹出退出确认对话框
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("提示") // 对话框标题
                .setMessage("是否退出应用？") // 对话框提示信息
                .setPositiveButton("确认", (dialog, which) -> {
                    finish();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    // 取消操作：关闭对话框
                    dialog.dismiss();
                });
        // 显示对话框（必须调用 show()）
        dialogBuilder.show();
    }

    // 生命周期管理（避免内存泄漏）
    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause(); // 暂停 WebView 所有动作（如视频播放）
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume(); // 恢复 WebView 动作
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            //销毁时关闭硬件图层
            webView.setLayerType(View.LAYER_TYPE_NONE,null);
            webView.destroy(); // 销毁 WebView
            webView = null;
        }
        // 确保服务器关闭
        if (server != null && server.isAlive()) {
            server.stop();
        }
        super.onDestroy();
    }
}
