package wooks.hanjanhaeyo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

/**
 * @brief 인트로 액티비티 - 처음 몇초간 표시된 후 로그인 액티비티로 이동한다
 */
public class IntroActivity extends AppCompatActivity
{
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
    
        /// 1초 후 로그인 액티비티로 이동
        final Handler h = new Handler() {
            @Override
            public void handleMessage(Message msg)
            {
                Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        };
        h.sendEmptyMessageDelayed(0, 1000);
    }
}
