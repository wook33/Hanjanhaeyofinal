package wooks.hanjanhaeyo;

import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


/**
 * @breif 호스트 작업
 */
public class HostWorks
{
    /// 호스트 룸 액티비티 핸들러
    private static RoomHostActivity.SessionEventHandler g_evhRoomHostActivity;
    /// 게임1 액티비티 핸들러
    private static Game1Activity.SessionEventHandler g_evhGame1Activity;
    /// 게임2 액티비티 핸들러
    private static Game2Activity.SessionEventHandler g_evhGame2Activity;
    /// 게임3 액티비티 핸들러
    private static Game3Activity.SessionEventHandler g_evhGame3Activity;
    
    /// 게스트의 접속 종료 시 호출되는 이벤트 콜백
    private static IRemoteClosedEventHandler g_evhRemoteClosedCallback = new IRemoteClosedEventHandler()
    {
        @Override
        public void OnRemoteClosed(GuestSession session)
        {
            /// 게스트 측의 접속 종료 처리
            if( g_sessionList != null  &&  g_sessionList.contains(session) ) {
                g_sessionList.remove(session);
                session.SetKeepReceiving(false);
                session.SetPlayer(false);
            }
            UpdatePlayerList();
        }
    };
    /// 게스트로부터의 메시지 수신 이벤트 콜백
    private static IMessageReceivedEventHandler g_evhMessageReceivedCallback = new IMessageReceivedEventHandler()
    {
        @Override
        public void OnMessageReceived(GuestSession session, String sMessage)
        {
            String[] sp = sMessage.split("@");
            if( sp.length >= 1 ) {
                switch(sp[0].toUpperCase()) {
                /// 게스트의 방 입장 요청
                case Constants.PROTOCOL_ROOM_JOIN_REQUEST: {
                    /// 현재 방의 상태가 입장가능한 상태인지 여부에 따라 [ROOM_JOIN_RESPONSE]@[OK/NO] 메시지
                    if( m_state == EHostState.ROOM_WAITING ) {
                        /// 현재 방의 인원 수 제한에 걸리지 않은 경우
                        if( GetPlayerCount() < Constants.ROOM_MAX_PLAYER ) {
                            session.SetPlayer(true);
                            session.SetNickname(sp[1]);
                            session.Send(Constants.PROTOCOL_ROOM_JOIN_RESPONSE + "@OK");
                            UpdatePlayerList();
                        }
                        /// 인원 수 제한에 걸린 경우
                        else {
                            session.SetPlayer(false);
                            session.Send(Constants.PROTOCOL_ROOM_JOIN_RESPONSE + "@NO");
                        }
                    }
                    /// 현재 방이 입장 불가능한 경우
                    else {
                        session.SetPlayer(false);
                        session.Send(Constants.PROTOCOL_ROOM_JOIN_RESPONSE + "@NO");
                    }
                } break;

                /// 게스트의 플레이어 리스트 요청
                case Constants.PROTOCOL_ROOM_PLAYERLIST_REQUEST: {
                    UpdatePlayerList(session);
                } break;
                
                /// 현재 선택된 게임 요청
                case Constants.PROTOCOL_ROOM_SELECTED_GAME_REQUEST: {
                    UpdateSelectedGame(session);
                } break;
                
                /// 플레이어가 게임1 데이터를 수신할 준비가 되었음
                case Constants.PROTOCOL_GAME1_GUEST_READY: {
                    Game1_GuestReady();
                } break;
                
                /// 플레이어의 버튼 입력 혹은 시간초과
                case Constants.PROTOCOL_GAME1_PLAYER_INPUT_REQUEST: {
                    int nTurn = Integer.parseInt(sp[1]);
                    int nTooth = Integer.parseInt(sp[2]);
                    Game1_PlayerInput(nTurn, nTooth);
                } break;


                /// 플레이어가 게임2 데이터를 수신할 준비가 되었음
                case Constants.PROTOCOL_GAME2_GUEST_READY: {
                    Game2_GuestReady();
                } break;
                
                /// 플레이어가 버튼 입력 혹은 시간초과
                case Constants.PROTOCOL_GAME2_PLAYER_INPUT: {
                    int nPlayerIdx = Integer.parseInt(sp[1]);
                    int nSelection = Integer.parseInt(sp[2]);
                    Game2_PlayerInput(nPlayerIdx, nSelection);
                } break;


                /// 플레이어가 게임3 데이터를 수신할 준비가 되었음
                case Constants.PROTOCOL_GAME3_GUEST_READY: {
                    Game3_GuestReady();
                } break;

                /// 플레이어의 1 to 25 완주
                case Constants.PROTOCOL_GAME3_PLAYER_INPUT_REQUEST: {
                    int nPlayerIdx = Integer.parseInt(sp[1]);
                    Game3_PlayerFinish(nPlayerIdx);
                } break;
                
                }
            }
        }
    };
    
    /// 세션 목록에 접근할 때의 lock 객체
    private static Object g_csSessionList = new Object();
    /// 연결된 세션 목록
    private static ArrayList<GuestSession> g_sessionList;
    /// 다음에 할당될 세션 id
    private static int g_nNextId;
    
    /// 호스트 상태
    private static EHostState m_state;
    /// 리스닝 쓰레드 동작 여부
    private static boolean g_bKeepListening;
    /// 선택된 게임
    private static int g_nSelectedGame;
    
    /// 게임1 - 데이터 수신 준비된 플레이어 수
    private static int g_nGame1GuestReadyCount;
    /// 게임1 - 플레이어의 턴 순서 리스트
    private static ArrayList<Integer> g_game1PlayerTurnList;
    /// 게임1 - 현재 턴 번호
    private static int g_nGame1CurrentTurn;
    /// 게임1 - 폭탄 이빨 번호
    private static int g_nGame1Bomb;
    
    /// 게임2 - 데이터 수신 준비된 플레이어 수
    private static int g_nGame2GuestReadyCount;
    /// 게임2 - 플레이어의 입력 횟수
    private static int g_nGame2InputCount;
    /// 게임2 - 1번을 선택한 플레이어 리스트
    private static ArrayList<Integer> g_game2InputSelect1List;
    /// 게임2 - 2번을 선택한 플레이어 리스트
    private static ArrayList<Integer> g_game2InputSelect2List;
    /// 게임2 - 제한시간을 초과한 플레이어 리스트
    private static ArrayList<Integer> g_game2InputTimeoutList;
    
    /// 게임3 - 데이터 수신 준비된 플레이어 수
    private static int g_nGame3GuestReadyCount;
    /// 게임3 - 1 to 25를 끝낸 플레이어 리스트
    private static ArrayList<Integer> g_game3PlayerInputList;
    
    
    
    private HostWorks() {}
    
    
    public static void Register_RoomHostActivityEventHandler(RoomHostActivity.SessionEventHandler evh)
    {
        g_evhRoomHostActivity = evh;
    }
    public static void Register_Game1ActivityEventHandler(Game1Activity.SessionEventHandler evh)
    {
        g_evhGame1Activity = evh;
    }
    public static void Register_Game2ActivityEventHandler(Game2Activity.SessionEventHandler evh)
    {
        g_evhGame2Activity = evh;
    }
    public static void Register_Game3ActivityEventHandler(Game3Activity.SessionEventHandler evh)
    {
        g_evhGame3Activity = evh;
    }
    
    
    /**
     * @brief 호스트 소켓의 열림 상태 가져오기
     * @return 호스트 소켓이 열려있으면 true, 아니면 false
     */
    public static boolean IsOpened()
    {
        return m_state == EHostState.ROOM_WAITING  ||  m_state == EHostState.GAME_PLAYING;
    }
    
    /**
     * @brief 현재 선택된 게임 번호 가져오기
     * @return 선택된 게임 번호
     */
    public static int GetSelectedGame()
    {
        return g_nSelectedGame;
    }
    
    /**
     * @brief 현재 플레이어 수 가져오기 (게스트 수 + 호스트 자신1)
     * @return 현재 플레이어 수
     */
    public static int GetPlayerCount()
    {
        int nPlayerCount = 0;
        synchronized(g_csSessionList) {
            if(g_sessionList != null) {
                for(GuestSession s : g_sessionList) {
                    if(s.IsPlayer()) {
                        nPlayerCount++;
                    }
                }
            }
        }
        return nPlayerCount + 1;
    }
    
    
    /**
     * @brief 호스트 열기
     */
    public static void Open()
    {
        new Thread(new Runnable(){
            @Override
            public void run()
            {
                try {
                    /// 초기화
                    g_nNextId = 0;
                    g_sessionList = new ArrayList<>();
                    
                    /// 지정된 리스닝 포트에 대한 리스너 생성
                    ServerSocket listener = new ServerSocket(Constants.ROOM_HOST_PORT);
                    listener.setSoTimeout(1000);
                    m_state = EHostState.ROOM_WAITING;
                    
                    /// 반복하여 리스닝
                    g_bKeepListening = true;
                    while(g_bKeepListening) {
                        try {
                            /// 게스트가 접속하면 accept 처리 후 GuestSession 객체 생성
                            Socket socket = listener.accept();
                            GuestSession session = new GuestSession(g_nNextId++, socket);
                            session.SetNickname("undefined");
                            
                            /// 해당 세션에 대한 이벤트 핸들러 등록
                            session.Register_OnRemoteClosed(g_evhRemoteClosedCallback);
                            session.Register_OnMessageReceived(g_evhMessageReceivedCallback);
                            
                            /// 수신 쓰레드 시작
                            session.ReceiveStart();
                            
                            synchronized(g_csSessionList) {
                                g_sessionList.add(session);
                            }
                        }
                        catch(SocketTimeoutException ste) {
                            // NOP
                        }
                    }
                    /// 리스닝의 반복이 종료된 경우 리스너 닫기
                    listener.close();
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }
    
    /**
     * @brief 호스트 닫기
     */
    public static void Close()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                /// 상태 변경
                m_state = EHostState.CLOSED;
                g_bKeepListening = false;
                
                /// 모든 세션 닫기
                synchronized(g_csSessionList) {
                    if(g_sessionList != null) {
                        for(GuestSession s : g_sessionList) {
                            s.Close();
                        }
                        g_sessionList.clear();
                        g_sessionList = null;
                    }
                }
            }
        }).start();
    }
    
    /**
     * @brief 모든 플레이어에게 메시지를 전송한다
     * @param sMessage 전송할 메시지
     */
    public static void SendAllPlayer(final String sMessage)
    {
        /// 현재 쓰레드가 UI쓰레드인 경우 새로운 쓰레드를 생성하여 전송한다.
        if( Looper.myLooper() == Looper.getMainLooper() ) {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    SendAllPlayer(sMessage);
                }
            }).start();
            return;
        }
        
        /// 세션 리스트 중 플레이어인 세션에 대하여 메시지를 전송한다.
        synchronized(g_csSessionList) {
            for(GuestSession s : g_sessionList) {
                if(s.IsPlayer()) {
                    s.Send(sMessage);
                }
            }
        }
    }
    
    
    /**
     * @brief 룸 내 모든 플레이어들에게 플레이어 목록 정보를 전송한다.
     */
    public static void UpdatePlayerList()
    {
        /// 플레이어 목록 정보 생성
        String sParam = BuildMessageParam_PlayerList();
        String sMessage = Constants.PROTOCOL_ROOM_PLAYERLIST_UPDATE + "@" + sParam;
        
        /// 일괄 전송
        SendAllPlayer(sMessage);
        
        /// 호스트 자신의 액티비티에도 갱신
        if( g_evhRoomHostActivity != null ) {
            String[] sPlayerList = sParam.split("#");
    
            Message msg = g_evhRoomHostActivity.obtainMessage();
            msg.what = RoomHostActivity.SessionEventHandler.WHAT_UPDATE_PLAYER_LIST;
            msg.obj = sPlayerList;
            g_evhRoomHostActivity.sendMessage(msg);
        }
    }
    /**
     * @brief 특정 플레이어에게 플레이어 목록 정보를 전송한다.
     * @param session 전송할 플레이어
     */
    public static void UpdatePlayerList(GuestSession session)
    {
        String sParam = BuildMessageParam_PlayerList();
        String sMessage = Constants.PROTOCOL_ROOM_PLAYERLIST_UPDATE + "@" + sParam;
    
        if( session.IsPlayer() ) {
            session.Send(sMessage);
        }
    }
    
    /**
     * @brief 플레이어 목록 정보를 나타내는 메시지 파라미터 생성
     * @return 생성된 파라미터
     */
    private static String BuildMessageParam_PlayerList()
    {
        String sParam = "";
        sParam += Runtime.GetNickname() + "#";
        for(GuestSession s : g_sessionList) {
            if( s.IsPlayer() ) {
                sParam += s.GetNickname() + "#";
            }
        }
        if( sParam.endsWith("#") ) {
            sParam = sParam.substring(0, sParam.length()-1);
        }
    
        return sParam;
    }
    
    
    /**
     * @brief 호스트가 현재 선택된 게임 정보를 모든 플레이어에게 전송한다.
     * @param nGame 선택된 게임
     */
    public static void GameSelect(int nGame)
    {
        g_nSelectedGame = nGame;
    
        SendAllPlayer(Constants.PROTOCOL_ROOM_SELECTED_GAME_UPDATE+"@"+nGame);
    }
    
    /**
     * @breif 특정 플레이어에게 현재 선택된 게임 정보를 전송한다.
     * @param session 전송할 플레이어
     */
    public static void UpdateSelectedGame(GuestSession session)
    {
        if( session.IsPlayer() ) {
            session.Send(Constants.PROTOCOL_ROOM_SELECTED_GAME_UPDATE+"@"+g_nSelectedGame);
        }
    }
    
    /**
     * @brief 게임 시작
     */
    public static void GameStart()
    {
        /// 게임이 선택되지 않은 경우 동작하지 않음
        if( g_nSelectedGame <= 0 )
            return;
        
        /// 선택된 게임에 대한 초기화 작업
        switch(g_nSelectedGame) {
        case 1:
            Game1_Init();
            break;
        case 2:
            Game2_Init();
            break;
        case 3:
            Game3_Init();
            break;
        }
        
        /// 호스트 상태 변경
        m_state = EHostState.GAME_PLAYING;
    
        /// 모든 플레이어에게 게임 시작 신호를 전송한다.
        SendAllPlayer(Constants.PROTOCOL_ROOM_GAME_START +"@"+g_nSelectedGame);
    }
    
    /**
     * @brief 게임 종료 후 대기실로 돌아간다.
     */
    public static void ReturnToRoom()
    {
        /// 호스트 상태 변경
        m_state = EHostState.ROOM_WAITING;
        
        /// 모든 플레이어에게 대기실로 돌아간다는 신호를 전송한다.
        SendAllPlayer(Constants.PROTOCOL_GAME_RETURN_TO_ROOM);
        
        /// 호스트 자신의 액티비티에 반영한다.
        if( g_evhGame1Activity != null ) {
            g_evhGame1Activity.sendEmptyMessage(Game1Activity.SessionEventHandler.WHAT_RETURN_TO_ROOM);
        }
        if( g_evhGame2Activity != null ) {
            g_evhGame2Activity.sendEmptyMessage(Game2Activity.SessionEventHandler.WHAT_RETURN_TO_ROOM);
        }
        if( g_evhGame3Activity != null ) {
            g_evhGame3Activity.sendEmptyMessage(Game3Activity.SessionEventHandler.WHAT_RETURN_TO_ROOM);
        }
    }
    
    /**
     * @breif 게임1 - 초기화 작업
     */
    public static void Game1_Init()
    {
        g_nGame1GuestReadyCount = 0;
        g_game1PlayerTurnList = null;
        g_nGame1CurrentTurn = 0;
        g_nGame1Bomb = 0;
    }
    
    /**
     * @breif 게임1 - 플레이어의 데이터 수신 준비완료 신호 처리
     */
    public static void Game1_GuestReady()
    {
        /// 데이터 수신 준비가 완료된 플레이어 수 카운트
        g_nGame1GuestReadyCount++;
        int nGuestPlayerCount = GetPlayerCount() - 1;
        
        /// 모든 플레이어가 데이터를 수신할 준비가 되면 데이터 전송
        if( g_nGame1GuestReadyCount >= nGuestPlayerCount ) {
            Game1_InitGameData();
        }
    }
    
    /**
     * @brief 게임1 - 게임 데이터 생성 및 전송
     */
    public static void Game1_InitGameData()
    {
        /// 모든 플레이어의 턴 순서를 무작위로 결정하고, 턴 순서 정보와 폭탄 이빨의 정보를 전송한다.
        synchronized(g_csSessionList) {
            if(g_sessionList != null) {
                ArrayList<Integer> playerTurnList = new ArrayList<>();
    
                for(GuestSession s : g_sessionList) {
                    if(s.IsPlayer()) {
                        playerTurnList.add(s.GetId());
                    }
                }
                playerTurnList.add(-1); // 호스트 자신
        
                Collections.shuffle(playerTurnList);
                g_game1PlayerTurnList = playerTurnList;
        
                ArrayList<String> playerTurnNicknameList = new ArrayList<>();
                for(int i = 0; i < playerTurnList.size(); i++) {
                    int nPlayerId = playerTurnList.get(i);
                    if(nPlayerId == -1) {
                        playerTurnNicknameList.add(Runtime.GetNickname());
                        continue;
                    }
                    for(GuestSession s : g_sessionList) {
                        if(s.GetId() == nPlayerId) {
                            playerTurnNicknameList.add(s.GetNickname());
                            break;
                        }
                    }
                }
        
                String sTurnInfo = "";
                for(int i = 0; i < playerTurnNicknameList.size(); i++) {
                    sTurnInfo += playerTurnNicknameList.get(i) + "#";
                }
                if(sTurnInfo.endsWith("#")) {
                    sTurnInfo = sTurnInfo.substring(0, sTurnInfo.length() - 1);
                }
    
                g_nGame1Bomb = new Random().nextInt(10);
        
                for(GuestSession s : g_sessionList) {
                    if(s.IsPlayer()) {
                        int nTurn = playerTurnList.indexOf(s.GetId());
                        String sMessage = Constants.PROTOCOL_GAME1_INIT_GAME_DATA + "@" + sTurnInfo + "@" + nTurn + "@" + g_nGame1Bomb;
                        s.Send(sMessage);
                    }
                }
    
                /// 호스트 자신의 액티비티에도 반영한다.
                if(g_evhGame1Activity != null) {
                    Message msg = g_evhGame1Activity.obtainMessage();
                    msg.what = Game1Activity.SessionEventHandler.WHAT_RECEIVED_GAME_DATA;
                    msg.obj = playerTurnNicknameList.toArray(new String[playerTurnNicknameList.size()]);
                    msg.arg1 = playerTurnList.indexOf(-1);
                    msg.arg2 = g_nGame1Bomb;
                    g_evhGame1Activity.sendMessage(msg);
                }
                
                /// 데이터 전송이 끝날 때, 첫 턴을 시작한다.
                g_nGame1CurrentTurn = -1;
                Game1_NextTurn();
            }
        }
    }
    
    /**
     * @breif 게임1 - 턴 흐름 처리
     */
    public static void Game1_NextTurn()
    {
        /// 턴 카운트 증가
        g_nGame1CurrentTurn++;
    
    
        SendAllPlayer(Constants.PROTOCOL_GAME1_TURN_CHANGED + "@" + g_nGame1CurrentTurn);
        /// 호스트 자신의 게임1 액티비티에 메시지 전달
        if( g_evhGame1Activity != null ) {
            Message msg = g_evhGame1Activity.obtainMessage();
            msg.what = Game1Activity.SessionEventHandler.WHAT_TURN_CHANGED;
            msg.arg1 = g_nGame1CurrentTurn;
            g_evhGame1Activity.sendMessage(msg);
        }
    }
    
    /**
     * @brief 게임1 - 플레이어의 버튼 입력 처리
     * @param nTurn 턴 정보
     * @param nTooth 버튼 정보
     */
    public static void Game1_PlayerInput(final int nTurn, final int nTooth)
    {
        /// 플레이어가 누른 이빨이 무엇인지 모든 플레이어에게 알린다.
        SendAllPlayer(Constants.PROTOCOL_GAME1_PLAYER_INPUT_UPDATE + "@" + nTurn + "@" + nTooth);
        /// 호스트 자신의 게임1 액티비티에도 알린다.
        if( g_evhGame1Activity != null ) {
            Message msg = g_evhGame1Activity.obtainMessage();
            msg.what = Game1Activity.SessionEventHandler.WHAT_PLAYER_INPUT_UPDATED;
            msg.arg1 = nTurn;
            msg.arg2 = nTooth;
            g_evhGame1Activity.sendMessage(msg);
        }
        
        /// 플레이어가 폭탄을 누르거나, 타임아웃되어 게임이 끝난 경우
        if( nTooth == g_nGame1Bomb  ||  nTooth == -1 ) {
            new Thread(new Runnable(){
                @Override
                public void run()
                {
                    try {
                        Thread.sleep(300);
                    }
                    catch(Exception ex) {
                        // NOP
                    }
                    SendAllPlayer(Constants.PROTOCOL_GAME1_GAME_FINISH + "@" + (nTurn%g_game1PlayerTurnList.size()));
                    /// 호스트 자신의 게임1 액티비티에도 알린다.
                    if( g_evhGame1Activity != null ) {
                        Message msg = g_evhGame1Activity.obtainMessage();
                        msg.what = Game1Activity.SessionEventHandler.WHAT_GAME_FINISH;
                        msg.arg1 = nTurn%g_game1PlayerTurnList.size();
                        msg.arg2 = nTooth;
                        g_evhGame1Activity.sendMessage(msg);
                    }
                }
            }).start();
        }
        /// 플레이어가 정상이빨을 누른 경우
        else {
            new Thread(new Runnable(){
                @Override
                public void run()
                {
                    try {
                        Thread.sleep(1000);
                    }
                    catch(Exception ex) {
                        // NOP
                    }
                    Game1_NextTurn();
                }
            }).start();
        }
    }
    
    /**
     * @brief 게임1 - 재시작
     */
    public static void Game1_Retry()
    {
        /// 모든 플레이어에게 재시작 신호 보냄
        SendAllPlayer(Constants.PROTOCOL_GAME1_RETRY);
        
        /// 호스트 자신의 액티비티에 반영
        if( g_evhGame1Activity != null ) {
            g_evhGame1Activity.sendEmptyMessage(Game1Activity.SessionEventHandler.WHAT_GAME_RETRY);
        }
    }
    
    
    /**
     * @brief 게임2 - 초기화 작업
     */
    public static void Game2_Init()
    {
        g_nGame2GuestReadyCount = 0;
        g_nGame2InputCount = 0;
        g_game2InputSelect1List = new ArrayList<>();
        g_game2InputSelect2List = new ArrayList<>();
        g_game2InputTimeoutList = new ArrayList<>();
    }
    
    /**
     * @brief 게임2 - 플레이어의 데이터 수신 준비완료 신호 처리
     */
    public static void Game2_GuestReady()
    {
        g_nGame2GuestReadyCount++;
        int nGuestPlayerCount = GetPlayerCount() - 1;
        
        /// 모든 플레이어가 데이터를 수신할 준비가 되면
        if( g_nGame2GuestReadyCount >= nGuestPlayerCount ) {
            Game2_InitGameData();
        }
    }
    
    /**
     * @brief 게임2 - 게임 데이터 생성 및 전송
     */
    public static void Game2_InitGameData()
    {
        /// 플레이어 정보와 버튼 모양 정보를 생성 및 전송
        synchronized(g_csSessionList) {
            ArrayList<Integer> playerList = new ArrayList<>();
            playerList.add(-1); // 호스트 자신
            for(GuestSession s : g_sessionList) {
                if(s.IsPlayer()) {
                    playerList.add(s.GetId());
                }
            }
    
            ArrayList<String> playerNicknameList = new ArrayList<>();
            for(int i = 0; i < playerList.size(); i++) {
                int nPlayerId = playerList.get(i);
                if(nPlayerId == -1) {
                    playerNicknameList.add(Runtime.GetNickname());
                    continue;
                }
                for(GuestSession s : g_sessionList) {
                    if(s.GetId() == nPlayerId) {
                        playerNicknameList.add(s.GetNickname());
                        break;
                    }
                }
            }
    
            String sNicknameInfo = "";
            for(int i = 0; i < playerNicknameList.size(); i++) {
                sNicknameInfo += playerNicknameList.get(i) + ((i < (playerNicknameList.size() - 1) ? "#" : ""));
            }
    
    
            int nShape = new Random().nextInt(10);
    
            /// 게스트에게 게임 정보를 전송
            for(GuestSession s : g_sessionList) {
                if(s.IsPlayer()) {
                    int nIdx = playerList.indexOf(s.GetId());
                    String sMessage = Constants.PROTOCOL_GAME2_INIT_GAME_DATA + "@" + sNicknameInfo + "@" + nIdx + "@" + nShape;
                    s.Send(sMessage);
                }
            }
            /// 호스트 자신의 액티비티에도 반영
            if(g_evhGame2Activity != null) {
                Message msg = g_evhGame2Activity.obtainMessage();
                msg.what = Game2Activity.SessionEventHandler.WHAT_RECEIVED_GAME_DATA;
                msg.obj = playerNicknameList.toArray(new String[playerNicknameList.size()]);
                msg.arg1 = 0;
                msg.arg2 = nShape;
                g_evhGame2Activity.sendMessage(msg);
            }
    
    
            /// 게스트에게 선택 시작하라고 알림
            SendAllPlayer(Constants.PROTOCOL_GAME2_ON_SELECTION_TIME);
            /// 호스트 자신의 액티비티에도 반영
            if(g_evhGame2Activity != null) {
                g_evhGame2Activity.sendEmptyMessage(Game2Activity.SessionEventHandler.WHAT_ON_SELECTION_TIME);
            }
        }
    }
    
    /**
     * @breif 게임2 - 플레이어의 선택 정보를 처리
     * @param nPlayerIdx 플레이어 정보
     * @param nSelection 선택 정보
     */
    public static void Game2_PlayerInput(int nPlayerIdx, int nSelection)
    {
        /// 입력한 플레이어의 전체 수와 각 입력별 인원수 카운트
        g_nGame2InputCount++;
        switch(nSelection) {
        case 0:
            g_game2InputSelect1List.add(nPlayerIdx);
            break;
        case 1:
            g_game2InputSelect2List.add(nPlayerIdx);
            break;
        case -1:
            g_game2InputTimeoutList.add(nPlayerIdx);
            break;
        }
        
        /// 모든 사용자가 입력하거나 시간초과를 한 경우
        if( g_nGame2InputCount >= GetPlayerCount() ) {
            
            /// 모든 사용자의 입력 정보를 전송하기 위해 메시지 파라미터를 생성한다.
            String sParamSelect1 = "";
            for(int i=0; i < g_game2InputSelect1List.size(); i++) {
                sParamSelect1 += g_game2InputSelect1List.get(i) + ((i<(g_game2InputSelect1List.size()-1)?"#":""));
            }
            String sParamSelect2 = "";
            for(int i=0; i < g_game2InputSelect2List.size(); i++) {
                sParamSelect2 += g_game2InputSelect2List.get(i) + ((i<(g_game2InputSelect2List.size()-1)?"#":""));
            }
            String sParamTimeout = "";
            for(int i=0; i < g_game2InputTimeoutList.size(); i++) {
                sParamTimeout += g_game2InputTimeoutList.get(i) + ((i<(g_game2InputTimeoutList.size()-1)?"#":""));
            }
    
    
            String sParamLoser = "";
            
            /// 시간초과한 사람이 패배
            if( g_game2InputTimeoutList.size() > 0 ) {
                sParamLoser = sParamTimeout;
            }
            /// 양쪽의 수가 같으면 모두 패배
            else if( g_game2InputSelect1List.size() == g_game2InputSelect2List.size() ) {
                sParamLoser = sParamSelect1 + "#" + sParamSelect2;
            }
            /// 양쪽의 수가 다르면 적은 쪽이 패배
            else if( g_game2InputSelect1List.size() < g_game2InputSelect2List.size() ) {
                sParamLoser = sParamSelect1;
            }
            /// 양쪽의 수가 다르면 적은 쪽이 패배
            else {
                sParamLoser = sParamSelect2;
            }
    
            /// 게임 결과 전송
            String sGameResultInfo = sParamSelect1+"%"+sParamSelect2+"%"+sParamTimeout+"%"+sParamLoser;
            SendAllPlayer(Constants.PROTOCOL_GAME2_GAME_FINISH+"@"+sGameResultInfo);
            /// 호스트 자신에게도 반영
            if( g_evhGame2Activity != null ) {
                Message msg = g_evhGame2Activity.obtainMessage();
                msg.what = Game2Activity.SessionEventHandler.WHAT_GAME_FINISH;
                msg.obj = sGameResultInfo;
                g_evhGame2Activity.sendMessage(msg);
            }
            
        }
        
    }
    
    /**
     * @brief 게임2 - 재시작
     */
    public static void Game2_Retry()
    {
        SendAllPlayer(Constants.PROTOCOL_GAME2_RETRY);
        
        if( g_evhGame2Activity != null ) {
            g_evhGame2Activity.sendEmptyMessage(Game2Activity.SessionEventHandler.WHAT_GAME_RETRY);
        }
    }
    
    
    /**
     * @brief 게임3 - 초기화 작업
     */
    public static void Game3_Init()
    {
        g_nGame3GuestReadyCount = 0;
        g_game3PlayerInputList = new ArrayList<>();
    }
    
    /**
     * @brief 게임3 - 게임 데이터 수신준비 완료 신호 처리
     */
    public static void Game3_GuestReady()
    {
        g_nGame3GuestReadyCount++;
        int nGuestPlayerCount = GetPlayerCount() - 1;
        
        /// 모든 플레이어가 데이터를 수신할 준비가 되면
        if( g_nGame3GuestReadyCount >= nGuestPlayerCount ) {
            Game3_InitGameData();
        }
    }
    
    /**
     * @brief 게임3 - 게임 정보 생성 및 전송
     */
    public static void Game3_InitGameData()
    {
        /// 플레이어 정보 생성 및 전송
        synchronized(g_csSessionList) {
            ArrayList<Integer> playerList = new ArrayList<>();
            playerList.add(-1); // 호스트 자신
            for(GuestSession s : g_sessionList) {
                if(s.IsPlayer()) {
                    playerList.add(s.GetId());
                }
            }
    
            ArrayList<String> playerNicknameList = new ArrayList<>();
            for(int i = 0; i < playerList.size(); i++) {
                int nPlayerId = playerList.get(i);
                if(nPlayerId == -1) {
                    playerNicknameList.add(Runtime.GetNickname());
                    continue;
                }
                for(GuestSession s : g_sessionList) {
                    if(s.GetId() == nPlayerId) {
                        playerNicknameList.add(s.GetNickname());
                        break;
                    }
                }
            }
    
            String sNicknameInfo = "";
            for(int i = 0; i < playerNicknameList.size(); i++) {
                sNicknameInfo += playerNicknameList.get(i) + ((i < (playerNicknameList.size() - 1) ? "#" : ""));
            }
    
    
            /// 게스트에게 게임 정보를 전송
            for(GuestSession s : g_sessionList) {
                if(s.IsPlayer()) {
                    int nIdx = playerList.indexOf(s.GetId());
                    String sMessage = Constants.PROTOCOL_GAME3_INIT_GAME_DATA + "@" + sNicknameInfo + "@" + nIdx;
                    s.Send(sMessage);
                }
            }
            /// 호스트 자신의 액티비티에도 반영
            if(g_evhGame3Activity != null) {
                Message msg = g_evhGame3Activity.obtainMessage();
                msg.what = Game3Activity.SessionEventHandler.WHAT_RECEIVED_GAME_DATA;
                msg.obj = playerNicknameList.toArray(new String[playerNicknameList.size()]);
                msg.arg1 = 0;
                g_evhGame3Activity.sendMessage(msg);
            }
    
    
            /// 3-2-1 카운트다운 후 1 to 25 시작
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try {
                        /// 카운트다운 3 전송
                        if(m_state == EHostState.GAME_PLAYING) {
                            SendAllPlayer(Constants.PROTOCOL_GAME3_1_TO_25_READY + "@3");
                            if(g_evhGame3Activity != null) {
                                Message msg = g_evhGame3Activity.obtainMessage();
                                msg.what = Game3Activity.SessionEventHandler.WHAT_1_TO_25_READY;
                                msg.arg1 = 3;
                                g_evhGame3Activity.sendMessage(msg);
                            }
                        }
                        
                        Thread.sleep(1000);
    
                        /// 카운트다운 2 전송
                        if(m_state == EHostState.GAME_PLAYING) {
                            SendAllPlayer(Constants.PROTOCOL_GAME3_1_TO_25_READY + "@2");
                            if(g_evhGame3Activity != null) {
                                Message msg = g_evhGame3Activity.obtainMessage();
                                msg.what = Game3Activity.SessionEventHandler.WHAT_1_TO_25_READY;
                                msg.arg1 = 2;
                                g_evhGame3Activity.sendMessage(msg);
                            }
                        }
                
                        Thread.sleep(1000);
    
                        /// 카운트다운 1 전송
                        if(m_state == EHostState.GAME_PLAYING) {
                            SendAllPlayer(Constants.PROTOCOL_GAME3_1_TO_25_READY + "@1");
                            if(g_evhGame3Activity != null) {
                                Message msg = g_evhGame3Activity.obtainMessage();
                                msg.what = Game3Activity.SessionEventHandler.WHAT_1_TO_25_READY;
                                msg.arg1 = 1;
                                g_evhGame3Activity.sendMessage(msg);
                            }
                        }
                
                        Thread.sleep(1000);
                
                        /// 1 to 25 시작 신호 전송
                        if(m_state == EHostState.GAME_PLAYING) {
                            SendAllPlayer(Constants.PROTOCOL_GAME3_1_TO_25_START);
                            if(g_evhGame3Activity != null) {
                                g_evhGame3Activity.sendEmptyMessage(Game3Activity.SessionEventHandler.WHAT_1_TO_25_START);
                            }
                        }
                    } catch(Exception ex) {
                        // NOP
                    }
                }
            }).start();
        }
            
    }
    
    /**
     * @brief 게임3 - 플레이어의 1 to 25 완료 처리
     * @param nPlayerIdx 플레이어 정보
     */
    public static void Game3_PlayerFinish(int nPlayerIdx)
    {
        /// 1 to 25를 완료한 플레이어 목록에 추가
        g_game3PlayerInputList.add(nPlayerIdx);
        
        /// 1명 빼고 다 완료한 경우
        if( g_game3PlayerInputList.size() >= GetPlayerCount()-1 ) {
            
            ArrayList<Integer> tmpList = new ArrayList<>();
            tmpList.add(-1); // 호스트 자신
            for(GuestSession s : g_sessionList) {
                if(s.IsPlayer()) {
                    tmpList.add(s.GetId());
                }
            }
            
            /// 꼴찌가 누구냐 알아낸다.
            int nLoserIdx = -1;
            for(int i = 0; i < tmpList.size(); i++) {
                boolean bFound = false;
                for(int j=0; j < g_game3PlayerInputList.size(); j++) {
                    if( i == g_game3PlayerInputList.get(j) ) {
                        bFound = true;
                    }
                }
                if( bFound == false ) {
                    nLoserIdx = i;
                }
            }
            
            /// 꼴찌 정보를 전송하며 게임 종료를 알린다.
            SendAllPlayer(Constants.PROTOCOL_GAME3_GAME_FINISH+"@"+nLoserIdx);
            /// 호스트 자신의 액티비티에 반영한다.
            if( g_evhGame3Activity != null ) {
                Message msg = g_evhGame3Activity.obtainMessage();
                msg.what = Game3Activity.SessionEventHandler.WHAT_GAME_FINISH;
                msg.arg1 = nLoserIdx;
                g_evhGame3Activity.sendMessage(msg);
            }
        }
        /// 아직 2명 이상의 플레이어가 1 to 25를 진행하고 있는 경우
        else {
            /// 남은 플레이어가 몇명이냐 알아낸다.
            int nRemainPlayers = GetPlayerCount() - g_game3PlayerInputList.size();
            
            /// 남은 플레이어의 인원과 함께 다른 플레이어가 1 to 25를 완료했음을 알린다.
            SendAllPlayer(Constants.PROTOCOL_GAME3_PLAYER_INPUT_UPDATE+"@"+nRemainPlayers);
            /// 호스트 자신의 액티비티에도 반영한다.
            if( g_evhGame3Activity != null ) {
                Message msg = g_evhGame3Activity.obtainMessage();
                msg.what = Game3Activity.SessionEventHandler.WHAT_PLAYER_INPUT_UPDATED;
                msg.arg1 = nRemainPlayers;
                g_evhGame3Activity.sendMessage(msg);
            }
        }
    }
    
    /**
     * @brief 게임3 - 재시작
     */
    public static void Game3_Retry()
    {
        SendAllPlayer(Constants.PROTOCOL_GAME3_RETRY);
        
        if( g_evhGame3Activity != null ) {
            g_evhGame3Activity.sendEmptyMessage(Game3Activity.SessionEventHandler.WHAT_GAME_RETRY);
        }
    }
        
        
        
        
    
    
    
    
}
