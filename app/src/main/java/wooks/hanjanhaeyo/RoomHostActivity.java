package wooks.hanjanhaeyo;

import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @brief 호스트 룸 액티비티
 */
public class RoomHostActivity extends AppCompatActivity
{
    /// 네트워크 세션을 통해 호출된 이벤트를 처리하는 핸들러
    private SessionEventHandler m_sessionEventHandler;
    
    /// 방 정보 브로드캐스트 지속 여부
    private boolean m_bKeepBroadcasting;
    
    /// 닉네임 표시 텍스트뷰
    private TextView m_tvNickname;
    /// 플레이어 목록 표시 텍스트뷰
    private TextView m_tvPlayerList;
    /// 게임1 선택 버튼
    private Button m_btnGameSelect1;
    /// 게임2 선택 버튼
    private Button m_btnGameSelect2;
    /// 게임3 선택 버튼
    private Button m_btnGameSelect3;
    /// 게임 시작 버튼
    private Button m_btnGameStart;
    
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_host);
        
        
        
        m_tvNickname = (TextView)findViewById(R.id.tv_nickname);
        m_tvNickname.setText(Runtime.GetNickname());
        
        m_tvPlayerList = (TextView)findViewById(R.id.tv_playerList);
        
        m_btnGameSelect1 = (Button)findViewById(R.id.btn_gameSelect1);
        m_btnGameSelect2 = (Button)findViewById(R.id.btn_gameSelect2);
        m_btnGameSelect3 = (Button)findViewById(R.id.btn_gameSelect3);
        m_btnGameStart = (Button)findViewById(R.id.btn_gameStart);
    
    
    
        /// 세션 이벤트 핸들러 등록
        m_sessionEventHandler = new SessionEventHandler(Looper.getMainLooper());
        HostWorks.Register_RoomHostActivityEventHandler(m_sessionEventHandler);
        
        /// 호스트가 아직 열려있지 않다면 (처음 방을 생성한 경우)
        if( HostWorks.IsOpened() == false ) {
            /// 호스트 열기
            m_tvPlayerList.setText("접속자 : " + Runtime.GetNickname());
            HostWorks.Open();
        }
        /// 호스트가 이미 열려있다면 (이미 게임을 진행했다가 대기실로 이동한 경우)
        else {
            /// 플레이어 목록을 업데이트한다.
            HostWorks.UpdatePlayerList();
        }
        
        /// 방 정보 브로드캐스팅을 시작한다.
        Broadcast();
    }
    
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    
        HostWorks.Register_RoomHostActivityEventHandler(null);
    }
    
    @Override
    public void onBackPressed()
    {
        BroadcastStop();
        
        HostWorks.Close();
    
        Intent intent = new Intent(RoomHostActivity.this, LobbyActivity.class);
        startActivity(intent);
        finish();
    }
    
    
    /**
     * @brief 방 정보 브로드캐스트 시작
     */
    private void Broadcast()
    {
        /// 브로드캐스트 지속 여부
        m_bKeepBroadcasting = true;
        
        /// 브로드캐스트 주소 알아내기
        InetAddress broadcastAddress = null;
        try {
            /// 아래 코드를 사용하기 위해 매니페스트에 퍼미션 ACCESS_WIFI_STATE를 추가해야함
            WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            int nBroadcast = (dhcpInfo.ipAddress & dhcpInfo.netmask) | ~dhcpInfo.netmask;
            byte[] quads = new byte[4];
            for(int i = 0; i < 4; i++) {
                quads[i] = (byte) ((nBroadcast >> (i * 8)) & 0xFF);
            }
            
            /// 핫스팟인 경우 핫스팟 호스트의 ip 192.168.43.1에 대한 브로드캐스트주소 192.168.43.255 사용
            if( dhcpInfo.ipAddress == 0 )
                broadcastAddress = InetAddress.getByName("192.168.43.255");
            /// 일반 와이파이인 경우 자신의 ip 대역에 맞는 브로드캐스트 주소 사용
            else
                broadcastAddress = InetAddress.getByAddress(quads);
            
            Log.d("QQQQQ", "[추가작업#1] ip주소 : " + dhcpInfo.ipAddress);
            Log.d("QQQQQ", "[추가작업#1] netmask : " + dhcpInfo.netmask);
            Log.d("QQQQQ", "[추가작업#1] 브로드캐스트 주소 : " + broadcastAddress.toString());
        }
        catch(Exception ex) {
            Log.d("QQQQQ", "[추가작업#1] 예외 발생");
            ex.printStackTrace();
        }
    
        final InetAddress broadcastAddressFixed = broadcastAddress;
    
        /// 브로드캐스트 시작
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    /// UDP 소켓 생성
                    DatagramSocket udpSocket = new DatagramSocket();
                    /// LAN 브로드캐스트 주소를 지정
                    //InetAddress addr = InetAddress.getByName("192.168.0.255");
                    
                    /// 버퍼 생성 및 유효성 검증 패턴 4바이트 추가
                    byte[] buffer = new byte[1024];
                    buffer[0] = 0x11;
                    buffer[1] = 0x22;
                    buffer[2] = 0x33;
                    buffer[3] = 0x44;
                    
                    /// 자신의 닉네임을 추가
                    String sNickname = Runtime.GetNickname();
                    byte[] nicknameBytes = sNickname.getBytes("UTF-8");
                    for(int i=0; i < nicknameBytes.length; i++)
                        buffer[4+i] = nicknameBytes[i];
                    int nMessageLength = 4 + nicknameBytes.length;
                    
                    /// 패킷 객체 생성
                    DatagramPacket packet = new DatagramPacket(buffer, nMessageLength, broadcastAddressFixed, Constants.BROADCAST_PORT);
                
                    /// 브로드캐스트 전송을 2초 간격으로 반복한다.
                    while(m_bKeepBroadcasting) {
                        try {
                            Log.d("QQQQQ", "브로드캐스팅...");
                            udpSocket.send(packet);
                            Thread.sleep(2000);
                        }
                        catch(Exception ex) {
                            // NOP
                        }
                    }
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();
    }
    
    /**
     * @brief 브로드캐스트 중단
     */
    private void BroadcastStop()
    {
        m_bKeepBroadcasting = false;
    }
    
    
    /**
     * @brief 게임 선택
     * @param nGame 선택한 게임 번호
     */
    private void GameSelect(int nGame)
    {
        /// 화면에 선택된 게임 * 표시
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
        
        /// 플레이어들에게 게임 선택 정보를 전송한다.
        HostWorks.GameSelect(nGame);
    }
    
    public void OnClick_GameSelect1(View v)
    {
        GameSelect(1);
    }
    
    public void OnClick_GameSelect2(View v)
    {
        GameSelect(2);
    }
    
    public void OnClick_GameSelect3(View v)
    {
        GameSelect(3);
    }
    
    public void OnClick_GameStart(View v)
    {
        /// 선택된 게임 번호와 플레이어 수를 알아낸다.
        int nSelectedGame = HostWorks.GetSelectedGame();
        int nPlayerCount = HostWorks.GetPlayerCount();
        Intent intent = null;
        
        /// 최소 인원 수를 체크한다.
        switch(nSelectedGame) {
        case 0: {
            // TODO : 게임 선택 안했음 팝업
            Toast.makeText(RoomHostActivity.this, "게임이 선택되지 않음!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        case 1: {
            if( nPlayerCount < Constants.GAME1_MIN_PLAYER ) {
                Toast.makeText(RoomHostActivity.this, "게임을 하기 위한 인원이 부족합니다 (최소 2명 필요)", Toast.LENGTH_SHORT).show();
                return;
            }
            intent = new Intent(RoomHostActivity.this, Game1Activity.class);
        } break;
        
        case 2: {
            if( nPlayerCount < Constants.GAME2_MIN_PLAYER ) {
                Toast.makeText(RoomHostActivity.this, "게임을 하기 위한 인원이 부족합니다 (최소 3명 필요)", Toast.LENGTH_SHORT).show();
                return;
            }
            intent = new Intent(RoomHostActivity.this, Game2Activity.class);
        } break;
        
        case 3: {
            if( nPlayerCount < Constants.GAME3_MIN_PLAYER ) {
                Toast.makeText(RoomHostActivity.this, "게임을 하기 위한 인원이 부족합니다 (최소 2명 필요)", Toast.LENGTH_SHORT).show();
                return;
            }
            intent = new Intent(RoomHostActivity.this, Game3Activity.class);
        } break;
        
        }
        
        /// 방 정보 브로드캐스트 중단
        BroadcastStop();
        /// 게임 시작 신호 전송
        HostWorks.GameStart();
        
        /// 호스트 자신의 액티비티를 해당 게임 액티비티로 이동
        Log.d("QQQQQ", "게임 화면으로 이동 : " + nSelectedGame);
        startActivity(intent);
        finish();
    }
    
    
    
    
    /**
     * @breif 호스트 룸 액티비티 세션 이벤트 핸들러
     */
    class SessionEventHandler extends Handler
    {
        public static final int WHAT_UPDATE_PLAYER_LIST = 0;
        
        
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

            }
        }
    }
    
}
