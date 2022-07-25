package cc.ecisr.vocnyan;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {
	WebView webView;
	static final String INFO_HTML_FILE_URL = "file:///android_asset/info/info.html";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);
		webView = findViewById(R.id.web_view);
		// WebSettings webSettings = webView.getSettings();
		
		webView.loadUrl(INFO_HTML_FILE_URL);
		
		
	}
}
