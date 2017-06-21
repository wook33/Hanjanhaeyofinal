package wooks.hanjanhaeyo;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

/**
 * @breif 게스트 룸 액티비티. 일반 플레이어로 대기실에 들어가있는 상태.
 */
public class RoomGuestActivity extends AppCompatActivity
{
    /// 네트워크 세션을 통해 호출된 이벤트를 처리하는 핸들러
    private SessionEventHandler m_sessionEventHandler;
    
    /// 닉네임 표시 텍스트뷰
    private TextView m_tvNickname;
    /// 접속한 플레이어 목록
    private TextView m_tvPlayerList;
    /// 선택된 게임을 나타내는 버튼1
    private Button m_btnGameSelect1;
    /// 선택된 게임을 나타내는 버튼2
    private Button m_btnGameSelect2;
    /// 선택된 게임을 나타내는 버튼3
    private Button m_btnGameSelect3;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_guest);
    
    
        m_tvNickname = (TextView)findViewById(R.id.tv_nickname);
        m_tvNickname.setText(Runtime.GetNickname());
    
        m_tvPlayerList = (TextView)findViewById(R.id.tv_playerList);
        m_tvPlayerList.setText("접속자 : -");
    
        m_btnGameSelect1 = (Button)findViewById(R.id.btn_gameSelect1);
        m_btnGameSelect2 = (Button)findViewById(R.id.btn_gameSelect2);
        m_btnGameSelect3 = (Button)findViewById(R.id.btn_gameSelect3);
    
    
        /// 세션 이벤트 핸들러 등록
        m_sessionEventHandler = new SessionEventHandler(Looper.getMainLooper());
        GuestWorks.Register_RoomGuestActivityEventHandler(m_sessionEventHandler);
        
        
        /// 현재 방의 플레이어 정보와 선택된 게임 정보를 요청한다.
        GuestWorks.RequestPlayerList();
        GuestWorks.RequestSelectedGame();
    }
    
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    
        GuestWorks.Register_RoomGuestActivityEventHandler(null);
    }
    
    @Override
    public void onBackPressed()
    {
        GuestWorks.QuitRoom();
        
        Intent intent = new Intent(RoomGuestActivity.this, LobbyActivity.class);
        startActivity(intent);
        finish();
    }
    
    
    /**
     * @brief 선택된 게임을 표시
     * @param nGame 선택된 게임 번호
     */
    private void GameSelected(int nGame)
    {
        /// 게임이 변경될 때, 선택된 게임의 버튼에는 * 표시를 한다
        m_btnGameSelect1.setText("악어룰렛");
        m_btnGameSelect2.setText("텔레파시");
        m_btnGameSelect3.setText("1 to 25");
        
        switch(nGame) {
        case 1:
            m_btnGameSelect1.setText("악어룰렛 *");
            break;
        case 2:
            m_btnGameSelect2.setText("텔레파시 *");
            break;
        case 3:
            m_btnGameSelect3.setText("1 to 25 *");
            break;
        }
    }
    
    /**
     * @breif 게임 시작
     * @param nGame 시작할 게임 번호
     */
    private void GameStarted(int nGame)
    {
        /// 해당 게임 액티비티로 이동
        Intent intent = null;
        switch(nGame) {
        case 1 :
            intent = new Intent(RoomGuestActivity.this, Game1Activity.class);
            break;
        case 2 :
            intent = new Intent(RoomGuestActivity.this, Game2Activity.class);
            break;
        case 3 :
            intent = new Intent(RoomGuestActivity.this, Game3Activity.class);
            break;
        }
        Log.d("QQQQQ", "게임 시작 : " + nGame);
        startActivity(intent);
        finish();
    }
    
    
    /**
     * @breif 게스트 룸 액티비티 세션 이벤트 핸들러
     */
    class SessionEventHandler extends Handler
    {
        public static final int WHAT_UPDATE_PLAYER_LIST = 0;
        public static final int WHAT_UPDATE_SELECTED_GAME = 1;
        public static final int WHAT_GAMESTART = 2;
        public static final int WHAT_HOST_DISCONNECTED = 3;
        
        
        public SessionEventHandler(Looper looper)
        {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what) {

            /// 플레이어 목록 갱신
            case WHAT_UPDATE_PLAYER_LIST: {
                String[] playerList = (String[])msg.obj;
                String tmp = "접속자 : ";
                for(int i=0; i < playerList.length; i++)
                    tmp += playerList[i] + ", ";
                if( tmp.endsWith(", ") )
                    tmp = tmp.substring(0, tmp.length()-2);
                m_tvPlayerList.setText(tmp);
                
                Log.d("QQQQQ", "플레이어 리스트 갱신");
            } break;

            /// 선택된 게임 갱신
            case WHAT_UPDATE_SELECTED_GAME: {
                int nGame = GuestWorks.GetSelectedGame();
                GameSelected(nGame);
                // TODO : 선택된 게임 표시부 갱신
                Log.d("QQQQQ", "선택된 게임 갱신 : " + nGame);
            } break;

            /// 게임 시작
            case WHAT_GAMESTART: {
                int nGame = GuestWorks.GetSelectedGame();
                GameStarted(nGame);
            } break;
            
            /// 호스트 연결 끊김
            case WHAT_HOST_DISCONNECTED: {
                Intent intent = new Intent(RoomGuestActivity.this, LobbyActivity.class);
                startActivity(intent);
                finish();
            } break;
                
            }
        }
    }
    
}
