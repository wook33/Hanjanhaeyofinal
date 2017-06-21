package wooks.hanjanhaeyo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

/**
 * @brief 로비 액티비티. 호스트가 되어 방을 생성하거나(호스트 룸 액티비티), 다른 호스트가 생성한 방을 검색하여 입장한다(게스트 룸 액티비티).
 */
public class LobbyActivity extends AppCompatActivity
{
    /// 네트워크 세션을 통해 호출된 이벤트를 처리하는 핸들러
    private SessionEventHandler m_sessionEventHandler;
    
    /// 방 목록을 표시할 리스트 뷰
    private ListView m_lvHostList;
    /// 방 목록 리스트에 대한 리스트 어댑터
    private ArrayAdapter<HostInfo> m_lvAdapter;
    
    /// 방 검색의 지속 여부
    private boolean m_bKeepSearching;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        
        /// 방 목록 리스트뷰 세팅
        m_lvHostList = (ListView)findViewById(R.id.lv_hostList);
        m_lvHostList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                /// 방을 클릭한 경우 접속을 시도한다.
                HostInfo item = (HostInfo)m_lvHostList.getItemAtPosition(position);
                Log.d("QQQQQ", "선택한 호스트 : " + item.GetHostname() + " / " + item.GetNickname());
                JoinRoom(item.GetHostname());
            }
        });
        
        /// 방 검색을 시작한다.
        SearchHost();
    }
    
    /**
     * @breif 방 검색 시작
     */
    private void SearchHost()
    {
        /// 검색 지속 설정
        m_bKeepSearching = true;
        
        /// 리스트 뷰 어댑터 세팅
        m_lvAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        m_lvHostList.setAdapter(m_lvAdapter);
    
        /// 호스트의 브로드캐스트 감지
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    /// 지정된 브로드캐스트 포트에 대한 UDP 소켓 생성
                    DatagramSocket udpSocket = new DatagramSocket(Constants.BROADCAST_PORT);
                    /// 수신 타임아웃을 2초로 설정
                    udpSocket.setSoTimeout(2000);
                    
                    /// 버퍼 및 패킷 객체 생성
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    /// 검색을 반복한다
                    while(m_bKeepSearching) {
                        try {
                            /// 패킷 수신 대기
                            Log.d("QQQQQ", "검색중...");
                            udpSocket.receive(packet);
                            
                            /// 수신한 데이터
                            byte[] message = packet.getData();
                            
                            /// 수신한 패킷의 유효성 검증(0x11,0x22,0x33,0x44 패턴인지 아닌지)
                            if(packet.getLength() >= 4 && message[0] == 0x11 && message[1] == 0x22 && message[2] == 0x33 && message[3] == 0x44) {
                                InetSocketAddress hostAddress = (InetSocketAddress) packet.getSocketAddress();
                                /// 유효성 검사용 4바이트 이후 바이트는 호스트의 닉네임 정보이다.
                                byte[] nicknameBytes = new byte[packet.getLength() - 4];
                                for(int i=0; i < packet.getLength()-4; i++)
                                    nicknameBytes[i] = message[4+i];
                                
                                final String sHostname = hostAddress.getHostName();
                                final String sNickname = new String(nicknameBytes, "UTF-8");
                            
                                Log.d("QQQQQ", "검색한 호스트 : " + sHostname + " / " + sNickname);
                                
                            
                                /// 리스트 내에 해당 호스트명이 존재하지 않으면 추가
                                boolean bAlreadyContained = false;
                                for(int i=0; i < m_lvAdapter.getCount(); i++) {
                                    HostInfo t = m_lvAdapter.getItem(i);
                                    if( t.GetHostname().equals(sHostname) ) {
                                        bAlreadyContained = true;
                                        break;
                                    }
                                }
                                if( bAlreadyContained == false  &&  m_bKeepSearching ) {
                                    /// 리스트에 추가하는 작업은 UI쓰레드에서 수행한다.
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            HostInfo hi = new HostInfo(sHostname, sNickname);
                                            m_lvAdapter.add(hi);
                                        }
                                    });
                                }
                                //
                            }
                        }
                        /// 타임아웃 시 아무 동작도 하지 않는다
                        catch(SocketTimeoutException ste) {
                            // NOP
                        }
                        catch(Exception ex) {
                            // NOP
                            ex.printStackTrace();
                            Log.d("QQQQQ", "방 검색 오류");
                        }
                    }
                    /// 검색 플래그가 off된 경우 소켓을 닫는다.
                    udpSocket.close();
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();
    }
    
    /**
     * @brief 방 생성 버튼 클릭 이벤트
     */
    public void OnClick_CreateRoom(View v)
    {
        /// 방 검색 off
        m_bKeepSearching = false;
        
        /// 런타임 데이터에 자신을 호스트로 지정
        Runtime.SetHost(true);
    
        /// 현재 액티비티에 등록된 세션 이벤트 핸들러를 해제한다.
        m_sessionEventHandler = null;
        GuestWorks.Register_LobbyActivityEventHandler(null);
    
        /// 호스트 룸 액티비티로 이동한다.
        Intent intent = new Intent(LobbyActivity.this, RoomHostActivity.class);
        startActivity(intent);
        finish();
    }
    
    
    /**
     * @breif 해당 호스트의 방에 입장을 시도한다.
     * @param sHostname 호스트명
     */
    public void JoinRoom(String sHostname)
    {
        /// 방 검색 off
        m_bKeepSearching = false;
        
        /// 런타임 데이터에 자신을 게스트로 지정한다.
        Runtime.SetHost(false);
    
        /// 게스트로서의 세션 이벤트를 수신하기 위해 핸들러를 등록한다.
        m_sessionEventHandler = new SessionEventHandler(Looper.getMainLooper());
        GuestWorks.Register_LobbyActivityEventHandler(m_sessionEventHandler);
        
        /// 입장 시도
        GuestWorks.JoinRoom(sHostname);
    }
    
    
    /**
     * @breif 로비 액티비티 세션 이벤트 핸들러
     */
    class SessionEventHandler extends Handler
    {
        public static final int WHAT_HOST_CONNECT_FAIL = 0;
        public static final int WHAT_JOIN_ROOM_OK = 1;
        public static final int WHAT_JOIN_ROOM_NO = 2;
        public static final int WHAT_HOST_DISCONNECTED = 3;
        
        
        
        public SessionEventHandler(Looper looper)
        {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what) {
            
            /// 호스트에 접속하지 못한 경우 (방이 사라지는 등의 네트워크적 문제)
            case WHAT_HOST_CONNECT_FAIL: {
                Log.d("QQQQQ", "룸 입장 실패");
                
                // TODO : 실패 팝업
                Toast.makeText(LobbyActivity.this, "입장 실패!", Toast.LENGTH_SHORT).show();
            } break;
            
            /// 방 입장 허가를 받은 경우
            case WHAT_JOIN_ROOM_OK: {
                // TODO : 룸 화면 이동
                Log.d("QQQQQ", "룸 입장 허가");
                
                /// 게스트 룸 액티비티로 이동
                Intent intent = new Intent(LobbyActivity.this, RoomGuestActivity.class);
                startActivity(intent);
                finish();
            } break;
            
            /// 방 입장이 금지된 경우 (이미 게임이 시작되는 등의 정책적 문제)
            case WHAT_JOIN_ROOM_NO: {
                // TODO : 룸 입장 금지
                Log.d("QQQQQ", "룸 입장 금지");
    
                Toast.makeText(LobbyActivity.this, "실패!!!!", Toast.LENGTH_SHORT).show();
            } break;
            
            /// 연결된 호스트의 접속이 끊어진 경우
            case WHAT_HOST_DISCONNECTED: {
                Log.d("QQQQQ", "호스트 연결 끊김");
            } break;
            }
        }
    }
}
