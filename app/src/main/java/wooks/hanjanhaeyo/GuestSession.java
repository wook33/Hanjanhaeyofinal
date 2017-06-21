package wooks.hanjanhaeyo;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.Socket;
import java.net.SocketException;


/**
 * @brief TCP 세션을 나타내는 클래스
 */
public class GuestSession
{
    // 원격 측으로부터의 연결끊김 이벤트 핸들러
    private IRemoteClosedEventHandler m_evhRemoteClosed;
    // 메시지 수신 이벤트 핸들러
    private IMessageReceivedEventHandler m_evhMessageReceived;
    
    // 세션 id
    private int m_nId;
    // 소켓
    private Socket m_socket;
    // 해당 세션 사용자의 닉네임
    private String m_sNickname;
    // 해당 세션이 플레이어 자격이 있는지 여부
    private boolean m_bIsPlayer;
    // 송신 lock
    private Object m_csSend = new Object();
    
    // 수신 쓰레드
    private Thread m_receiveThread;
    // 수신 쓰레드의 수신 동작
    private boolean m_bKeepReceiving;
    
    
    /**
     * 게스트 측에서 사용하는 경우
     */
    public GuestSession()
    {
        m_nId = 0;
        m_socket = null;
    }
    
    /**
     * 호스트 측에서 accept 시 생성하는 경우
     * @param nId
     * @param socket
     */
    public GuestSession(int nId, Socket socket)
    {
        m_nId = nId;
        m_socket = socket;
    }
    
    /**
     * @breif 원격 측으로부터의 접속 종료 이벤트 등록
     * @param evh 이벤트 핸들러
     */
    public void Register_OnRemoteClosed(IRemoteClosedEventHandler evh)
    {
        m_evhRemoteClosed = evh;
    }
    
    /**
     * @brief 메시지 수신 이벤트 등록
     * @param evh 이벤트 핸들러
     */
    public void Register_OnMessageReceived(IMessageReceivedEventHandler evh)
    {
        m_evhMessageReceived = evh;
    }
    
    /**
     * @brief 수신 쓰레드 동작 설정
     * @param bKeepReceiving 수신상태 on/off
     */
    public void SetKeepReceiving(boolean bKeepReceiving)
    {
        m_bKeepReceiving = bKeepReceiving;
    }
    
    /**
     * @brief 세션 id 가져오기
     * @return 세션 id
     */
    public int GetId()
    {
        return m_nId;
    }
    /**
     * @brief 소켓 객체 가져오기
     * @return 소켓
     */
    public Socket GetSocket()
    {
        return m_socket;
    }
    
    /**
     * @brief 해당 세션 사용자의 닉네임 가져오기
     * @return 닉네임 문자열
     */
    public String GetNickname()
    {
        return m_sNickname;
    }
    
    /**
     * @brief 해당 세션 사용자의 닉네임 설정하기
     * @param sNickname 설정할 닉네임 문자열
     */
    public void SetNickname(String sNickname)
    {
        m_sNickname = sNickname;
    }
    
    /**
     * @brief 해당 세션이 플레이어 자격을 가지고 있는지 여부 알아내기
     * @return 세션의 플레이어 자격 여부
     */
    public boolean IsPlayer()
    {
        return m_bIsPlayer;
    }
    
    /**
     * @breif 해당 세션에게 플레이어 자격 설정하기
     * @param bIsPlayer 플레이어 자격 여부
     */
    public void SetPlayer(boolean bIsPlayer)
    {
        m_bIsPlayer = bIsPlayer;
    }
    
    
    /**
     * @brief 호스트에 접속한다
     * @param sHostname 호스트명
     * @param nPort 포트번호
     * @return 접속 성공 여부
     */
    public boolean Connect(String sHostname, int nPort)
    {
        try {
            /// 접속
            m_socket = new Socket(sHostname, nPort);
            
            /// 수신 시작
            ReceiveStart();
            
            return true;
        }
        catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * @breif 메시지 송신
     * @param sMessage 송신할 메시지
     */
    public void Send(String sMessage)
    {
        /// 송신 lock
        synchronized(m_csSend) {
            try {
                Log.d("QQQQQ", "세션 " + m_nId + " | 송신 : " + sMessage);
                
                /// 메시지 직렬화
                byte[] messageBytes = sMessage.getBytes("UTF-8");
    
                /// 스트림에 쓰기
                DataOutputStream os = new DataOutputStream(m_socket.getOutputStream());
                os.writeInt(messageBytes.length);
                os.write(messageBytes);
                os.flush();
            }
            catch(Exception ex) {
                /// 에러 발생
                ex.printStackTrace();
                Close();
                if( m_evhRemoteClosed != null ) {
                    m_evhRemoteClosed.OnRemoteClosed(GuestSession.this);
                }
            }
        }
    }
    
    /**
     * @brief 수신 시작
     */
    public void ReceiveStart()
    {
        m_bKeepReceiving = true;
        
        /// 수신 쓰레드 생성 및 시작
        m_receiveThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    /// 수신 스트림
                    DataInputStream is = new DataInputStream(m_socket.getInputStream());
                    
                    /// KeepReceiving이 설정된 동안 수신동작을 반복한다.
                    while(m_bKeepReceiving) {
                        /// 한 수신동작에서 최초로 수신한 4바이트는 메시지의 길이를 나타낸다.
                        int nLength = is.readInt();
                        
                        /// 길이만큼 버퍼를 생성한 후, 길이만큼 읽는다.
                        byte[] buffer = new byte[nLength];
                        int nReadBytes = is.read(buffer, 0, nLength);
                        if( nReadBytes != nLength ) {
                            /// TODO : 정해진 길이만큼 읽지 못한 경우의 처리를 추가한다 (그러나, 인터넷을 경유하지 않으므로 이 문제가 발생할 가능성이 미미할 것으로 보이므로 보류한다)
                            Log.d("QQQQQ", "체크포인트1");
                        }
                        
                        /// 읽은 바이트열을 문자열로 역직렬화
                        String sMessage = new String(buffer, "UTF-8");
                        Log.d("QQQQQ", "세션 " + m_nId +" | 읽은 메시지 : " + sMessage);
                        
                        /// 메시지 수신 이벤트 호출
                        if( m_evhMessageReceived != null ) {
                            m_evhMessageReceived.OnMessageReceived(GuestSession.this, sMessage);
                        }
                    }
                }
                /// 원격 측으로부터 스트림이 닫힌 경우
                catch(EOFException eofe) {
                    Close();
                    if( m_evhRemoteClosed != null ) {
                        m_evhRemoteClosed.OnRemoteClosed(GuestSession.this);
                    }
                }
                /// 호스트 측에서 연결 끊는 경우, 이미 Close가 동작되므로
                catch(NullPointerException npe) {
                    // NOP;
                }
                catch(SocketException se) {
                    Close();
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                    Close();
                    if( m_evhRemoteClosed != null ) {
                        m_evhRemoteClosed.OnRemoteClosed(GuestSession.this);
                    }
                }
            }
        });
        m_receiveThread.start();
    }
    
    /**
     * @brief 세션 종료
     */
    public void Close()
    {
        if( m_socket != null ) {
            /// 수신 중단
            if( m_bKeepReceiving )
                m_bKeepReceiving = false;
            /// 접속 종료
            try {
                m_socket.close();
            }
            catch(Exception ex) {
                // NOP
            }
            m_socket = null;
        }
    }
}
