package wooks.hanjanhaeyo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * @brief 로그인 액티비티 - 닉네임을 입력하고, 로비 액티비티로 이동한다.
 */
public class LoginActivity extends AppCompatActivity
{
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        
    }
    
    
    /**
     * @brief 로그인 버튼 클릭 콜백
     */
    public void OnClick_Login(View v)
    {
        /// 입력된 닉네임 가져오기
        EditText etNickname = (EditText)findViewById(R.id.et_nickname);
        String sNickname = etNickname.getText().toString();
        
        /// 금지문자(@, #, %)가 포함되었다면 오류 메시지를 표시한다.
        if( sNickname.contains("@")  ||  sNickname.contains("#")  ||  sNickname.contains("%") ) {
            Toast.makeText(this, "잘못된 닉네임!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        /// 런타임 정보 클래스에 자신의 닉네임을 저장한다.
        Runtime.SetNickname(sNickname);
    
        /// 로비 액티비티로 이동한다.
        Intent intent = new Intent(LoginActivity.this, LobbyActivity.class);
        startActivity(intent);
        finish();
    }
}
