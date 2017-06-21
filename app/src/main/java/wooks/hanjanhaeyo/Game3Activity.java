package wooks.hanjanhaeyo;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @breif 게임3 액티비티. 1 to 25 게임.
 */
public class Game3Activity extends AppCompatActivity
{
    /// 네트워크 세션을 통해 호출된 이벤트를 처리하는 핸들러
    private SessionEventHandler m_sessionEventHandler;
    
    /// 아래 섹션은 기존 1 to 25용 코드
    /////////////////////////////////
    Button btn[];
    int[] randomArr = new int[25];
    static int count = 1;
    TextView textView;
    Chronometer timer;
    FrameLayout frame;
    ////////////////////////////////
    
    /// 게임 정보알림 텍스트뷰
    private TextView m_tvNotice;
    /// 재시작 버튼
    private Button m_btnRetry;
    /// 대기실로 버튼
    private Button m_btnReturnToRoom;
    
    /// 플레이어 리스트
    private String[] m_nicknameList;
    /// 자신의 idx
    private int m_nMyIdx;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game3);
    
        
        m_tvNotice = (TextView)findViewById(R.id.tv_notice);
        m_btnRetry = (Button)findViewById(R.id.btn_retry);
        m_btnReturnToRoom = (Button)findViewById(R.id.btn_returnToRoom);
    
        /// 아래 섹션은 기존 1 to 25용 코드
        /////////////////////////////////
        timer = (Chronometer) findViewById(R.id.chronometer1);
        timer.setTextSize(getLcdSIzeHeight() / 32);
        timer.setHeight(getLcdSIzeHeight() / 8);
        frame = (FrameLayout) findViewById(R.id.frame);
        // LinearLayout linear = (LinearLayout)findViewById(R.id.linear);
        LinearLayout linear1 = (LinearLayout) findViewById(R.id.linear1);
        LinearLayout linear2 = (LinearLayout) findViewById(R.id.linear2);
        LinearLayout linear3 = (LinearLayout) findViewById(R.id.linear3);
        LinearLayout linear4 = (LinearLayout) findViewById(R.id.linear4);
        LinearLayout linear5 = (LinearLayout) findViewById(R.id.linear5);
    
        LinearLayout.LayoutParams parambtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        parambtn.weight = 1.0f;
    
        
        View.OnClickListener btnListener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Object tag = v.getTag();
                if (tag.equals(count + "")) {
            
                    Animation anim = AnimationUtils.loadAnimation(
                            getApplicationContext(), R.anim.scale);
                    v.startAnimation(anim);
            
                    count++;
                    if (count == 26) {
                        timer.stop();
                        count = 1;
                        
                        /// 끝까지 버튼을 눌렀다. 완주했음을 호스트에게 알려야한다.
                        Finish_1to25();
                    }
                }
            }
        };
        
        btn = new Button[25];
    
        for (int i = 0; i < 25; i++) {
            btn[i] = new Button(this);
            btn[i].setText(" ");
            btn[i].setTextSize(getLcdSIzeHeight() / 32);
            btn[i].setId(i);
            btn[i].setHeight(getLcdSIzeHeight() / 8);
            btn[i].setOnClickListener(btnListener);
            btn[i].setEnabled(false);
            if (i < 5)
                linear1.addView(btn[i], parambtn);
            else if (i < 10)
                linear2.addView(btn[i], parambtn);
            else if (i < 15)
                linear3.addView(btn[i], parambtn);
            else if (i < 20)
                linear4.addView(btn[i], parambtn);
            else if (i < 25)
                linear5.addView(btn[i], parambtn);
        }
        
        /////////////////////////////////
    
    
    
        /// 세션 이벤트 핸들러 등록
        m_sessionEventHandler = new SessionEventHandler(Looper.getMainLooper());
        if( Runtime.IsHost() ) {
            HostWorks.Register_Game3ActivityEventHandler(m_sessionEventHandler);
        }
        else {
            /// 자신이 게스트인 경우, 게임3 데이터 수신 준비완료 신호를 보낸다.
            GuestWorks.Register_Game3ActivityEventHandler(m_sessionEventHandler);
            GuestWorks.Game3_GuestReady();
        }
    }
    
    
    @Override
    public void onBackPressed()
    {
        if( Runtime.IsHost() ) {
            HostWorks.Close();
            HostWorks.Register_Game3ActivityEventHandler(null);
        }
        else {
            GuestWorks.QuitRoom();
            GuestWorks.Register_Game3ActivityEventHandler(null);
        }
        
        Intent intent = new Intent(Game3Activity.this, LobbyActivity.class);
        startActivity(intent);
        finish();
    }
    
    
    /// 아래 섹션은 기존 1 to 25용 코드
    /////////////////////////////////
    public int[] generate() {
        int[] result = new int[25];
        int count = 0;
    
        while (count != 25) {
            boolean test = true;
            int r = (int) (Math.random() * 25 + 1);
            for (int i = 0; i < result.length; i++) {
                if (result[i] == r) {
                    test = false;
                    break;
                }
            }
            if (test) {
                result[count++] = r;
            }
        }
        return result;
    }
    
    
    /**
     * @brief 자신의 화면에서 1 to 25 게임을 시작한다.
     */
    private void Start()
    {
        randomArr = generate();
    
        for (int i = 0; i < btn.length; i++) {
            btn[i].setEnabled(true);
            btn[i].setText("" + randomArr[i]);
            btn[i].setTag(btn[i].getText());
            btn[i].setAnimation(null);
        }
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
    
        Animation anim = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.translate);
        frame.setAnimation(anim);
    }
    
    public int getLcdSIzeHeight() {
        return ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getHeight();
    }
    
    public int getLcdSIzeWidth() {
        return ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getWidth();
    }
    ////////////////////////////////
    
    
    public void OnClick_Retry(View v)
    {
        HostWorks.Game3_Retry();
    }
    public void OnClick_ReturnToRoom(View v)
    {
        HostWorks.ReturnToRoom();
    }
    
    
    /**
     * @brief 1 to 25 시작하기전 카운트다운
     * @param nReadyCountdown
     */
    private void ReadyCountdown(int nReadyCountdown)
    {
        m_tvNotice.setText("준비하세요... 카운트다운 " + nReadyCountdown);
    }
    
    /**
     * @brief 1 to 25 시작
     */
    private void Start_1to25()
    {
        m_tvNotice.setText("남들보다 먼저 1 to 25를 클리어하세요!");
        Start();
    }
    
    /**
     * @brief 1 to 25를 완주했음
     */
    private void Finish_1to25()
    {
        /// 1to25 끝냈음을 호스트에 반영한다.
        if( Runtime.IsHost() ) {
            HostWorks.Game3_PlayerFinish(m_nMyIdx);
        }
        else {
            GuestWorks.Game3_PlayerFinish(m_nMyIdx);
        }
    }
    
    /**
     * @brief 남은 플레이어 수를 알림
     * @param nRemainPlayers 남은 플레이어 수
     */
    private void UpdatePlayerInput(int nRemainPlayers)
    {
        m_tvNotice.setText("아직 성공하지 못한 플레이어 수 : " + nRemainPlayers + "명");
    }
    
    /**
     * @brief 게임 결과 표시
     * @param nLoserIdx 패배자 idx
     */
    private void DisplayGameResult(int nLoserIdx)
    {
        /// 카운트다운이 켜져있으면 정지
        if( timer.isEnabled() ) {
            timer.stop();
        }
        /// 더이상 버튼을 누를 수 없도록 비활성화
        for (int i = 0; i < 25; i++) {
            btn[i].setEnabled(false);
        }
        
        
        /// 패배자를 표시
        String sLoserNickname = m_nicknameList[nLoserIdx];
        m_tvNotice.setText("패배자 : " + sLoserNickname);
    
        /// 자신이 호스트인 경우, 재시작/대기실로 버튼 표시
        if( Runtime.IsHost() ) {
            m_btnRetry.setVisibility(View.VISIBLE);
            m_btnReturnToRoom.setVisibility(View.VISIBLE);
        }
    }
    
    
    
    /**
     * @breif 게임3 액티비티 세션 이벤트 핸들러
     */
    class SessionEventHandler extends Handler
    {
        public static final int WHAT_HOST_DISCONNECTED = 0;
        public static final int WHAT_GAME_RETRY = 1;
        public static final int WHAT_RETURN_TO_ROOM = 2;
        public static final int WHAT_RECEIVED_GAME_DATA = 3;
        public static final int WHAT_1_TO_25_READY = 4;
        public static final int WHAT_1_TO_25_START = 5;
        public static final int WHAT_PLAYER_INPUT_UPDATED = 6;
        public static final int WHAT_GAME_FINISH = 7;
    
        
        
        
        public SessionEventHandler(Looper looper)
        {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what) {
            /// 호스트 연결 끊김
            case WHAT_HOST_DISCONNECTED: {
                // TODO : 서버로부터의 연결 끊김 처리
            } break;

            /// 게임 재시작
            case WHAT_GAME_RETRY: {
                if( Runtime.IsHost() ) {
                    HostWorks.Register_Game3ActivityEventHandler(null);
                }
                else {
                    GuestWorks.Register_Game3ActivityEventHandler(null);
                }
                Intent intent = new Intent(Game3Activity.this, Game3Activity.class);
                startActivity(intent);
                finish();
            } break;

            /// 대기실로 이동
            case WHAT_RETURN_TO_ROOM: {
                Intent intent = null;
                if( Runtime.IsHost() ) {
                    HostWorks.Register_Game3ActivityEventHandler(null);
                    intent = new Intent(Game3Activity.this, RoomHostActivity.class);
                }
                else {
                    GuestWorks.Register_Game3ActivityEventHandler(null);
                    intent = new Intent(Game3Activity.this, RoomGuestActivity.class);
                }
                startActivity(intent);
                finish();
            } break;
            
            /// 게임 정보를 수신함
            case WHAT_RECEIVED_GAME_DATA: {
                m_nicknameList = (String[])msg.obj;
                m_nMyIdx = msg.arg1;
            } break;
            
            /// 1 to 25 시작 전 카운트다운
            case WHAT_1_TO_25_READY: {
                int nReadyCountdown = msg.arg1; // 3.. 2.. 1..
                ReadyCountdown(nReadyCountdown);
            } break;

            /// 1 to 25 시작
            case WHAT_1_TO_25_START: {
                Start_1to25();
            } break;

            /// 플레이어의 1to25 완료 정보가 갱신되었음
            case WHAT_PLAYER_INPUT_UPDATED: {
                int nRemainPlayers = msg.arg1;
                UpdatePlayerInput(nRemainPlayers);
            } break;
            
            /// 게임이 종료되었음. 결과 발표
            case WHAT_GAME_FINISH: {
                int nLoserIdx = msg.arg1;
                DisplayGameResult(nLoserIdx);
            } break;
            
            }
        }
    }
}
