package port;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.webkit.ValueCallback;

import com.commonwebview.webview.CommonWebView;

/**
 * webview回调辅助抽象类，可扩展
 * Created by wanglinjie.
 * create time:2018/6/20  下午4:59
 */

public abstract class WebviewCBHelper implements LongClickCallBack, ResultCallBack, UserAgentDefined, OpenFileChooser, ScanerImgCallBack {

    private CommonWebView webview;

    public WebviewCBHelper(CommonWebView webview) {
        this.webview = webview;
    }

    @Override
    public String getUserAgent() {
        return null;
    }

    @Override
    public void onLongClickCallBack(String imgUrl, boolean isScanerImg) {

    }

    @Override
    public boolean OnResultCallBack() {
        return false;
    }

    @Override
    public void NavToImageSelect(Fragment fragment, int requestCode) {

    }

    @Override
    public boolean openFileResultCallBack(int requestCode, int resultCode, Intent data, ValueCallback<Uri> mUploadMessage, ValueCallback<Uri[]> mUploadMessage21) {
        return false;
    }

    @Override
    public void OnScanerImg(String imgUrl) {

    }

    @Override
    public void OnCancelScanerThread() {

    }
}
