package azwms.ttk;

import android.content.res.AssetManager;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class AssetWebServer extends NanoHTTPD {

    private final AssetManager assetManager;
    private static final String ASSETS_BASE_PATH = "www"; // assets根目录下的子目录（www）

    private final Map<String, String> customMimeTypes;

    /**
     * 构造函数
     * @param port 服务器端口
     * @param assetManager Android的AssetManager
     */
    public AssetWebServer(int port, AssetManager assetManager) {
        super(port);
        this.assetManager = assetManager;
        this.customMimeTypes = new HashMap<>();
        initMimeTypes();
    }

    private void initMimeTypes() {
        customMimeTypes.put("webp", "image/webp");
        customMimeTypes.put("json", "application/json");
        customMimeTypes.put("js", "application/javascript");
        customMimeTypes.put("mjs", "application/javascript");
        customMimeTypes.put("jsx", "application/javascript");
        customMimeTypes.put("ts", "application/javascript");
        customMimeTypes.put("tsx", "application/javascript");
        customMimeTypes.put("wasm", "application/wasm");
        customMimeTypes.put("woff", "font/woff");
        customMimeTypes.put("woff2", "font/woff2");
        customMimeTypes.put("ttf", "font/ttf");
        customMimeTypes.put("otf", "font/otf");
        customMimeTypes.put("svg", "image/svg+xml");
        customMimeTypes.put("mp4", "video/mp4");
        customMimeTypes.put("mp3", "audio/mpeg");
        customMimeTypes.put("webm", "video/webm");
        customMimeTypes.put("map", "application/json");
        customMimeTypes.put("ico", "image/x-icon");
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();
        if (uri == null || uri.isEmpty()) {
            uri = "/"; // 默认访问根路径
        }
        try {
            // 拼接assets中的文件路径（例如：www/index.html）
            String assetPath = ASSETS_BASE_PATH + uri;
            // 从 assets 打开输入流
            InputStream inputStream = assetManager.open(assetPath);

            // 根据文件扩展名获取MIME类型
            String mimeType = getMimeType(uri);
            NanoHTTPD.Response response = newChunkedResponse(NanoHTTPD.Response.Status.OK, mimeType, inputStream);
            response.addHeader("Cache-Control", "public, max-age=31536000");
            return response;
        } catch (IOException e) {
            // 文件不存在或读取失败时返回404
            return newFixedLengthResponse(
                NanoHTTPD.Response.Status.NOT_FOUND, 
                NanoHTTPD.MIME_PLAINTEXT, 
                "File not found: " + uri
            );
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return null;// 无扩展名或点在末尾（如"file."）
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private String getMimeType(String filename) {
        String extension = getExtension(filename);
        if (extension == null) return "text/html";

        // 1. 检查自定义映射
        if (customMimeTypes.containsKey(extension)) {
            return customMimeTypes.get(extension);
        }

        // 2. 使用系统MIME映射
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType != null) {
            return mimeType;
        }

        // 3. 特殊处理文本类型
        if (isTextExtension(extension)) {
            return "text/plain";
        }

        return "application/octet-stream";
    }

    private boolean isTextExtension(String extension) {
        return extension.matches("txt|md|log|ini|conf|cpp|c|h|hpp|java|kt|py|rb|go|rs|swift|sh|bat|pl|php");
    }


}