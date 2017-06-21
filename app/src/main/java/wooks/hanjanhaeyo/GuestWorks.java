package wooks.hanjanhaeyo;

import android.os.Message;
import android.support.annotation.IntegerRes;
import android.util.Log;

/**
 * @breif 게스트 작업
 */
public class GuestWorks
{
    /// 로비 액티비티 핸들러
    private static LobbyActivity.SessionEventHandler g_evhLobbyActivity;
    /// 게스트 룸 액티비티 핸들러
    private static RoomGuestActivity.SessionEventHandler g_evhRoomGuestActivity;
    /// 게임1 액티비티 핸들러
    private static Game1Activity.SessionEventHandler g_evhGame1Activity;
    /// 게임2 액티비티 핸들러
    private static Game2Activity.SessionEventHandler g_evhGame2Activity;
    /// 게임3 액티비티 핸들러
    private static Game3Activity.SessionEventHandler g_evhGame3Activity;
    
    /// 호스트 측의 접속 종료 시 호출되는 이벤트 콜백
    private static IRemoteClosedEventHandler g_evhRemoteClosedCallback = new IRemoteClosedEventHandler()
    {
        @Override
        public void OnRemoteClosed(GuestSession session)
        {
            //// 각 액티비티 핸들러에 접속 종료 이벤트를 포워딩한다.
            if( g_evhLobbyActivity != null ) {
                g_evhLobbyActivity.sendEmptyMessage(LobbyActivity.SessionEventHandler.WHAT_HOST_DISCONNECTED);
            }
            if( g_evhRoomGuestActivity != null ) {
                g_evhRoomGuestActivity.sendEmptyMessage(RoomGuestActivity.SessionEventHandler.WHAT_HOST_DISCONNECTED);
            }
            if( g_evhGame1Activity != null ) {
                g_evhGame1Activity.sendEmptyMessage(Game1Activity.SessionEventHandler.WHAT_HOST_DISCONNECTED);
            }
            if( g_evhGame2Activity != null ) {
                g_evhGame2Activity.sendEmptyMessage(Game2Activity.SessionEventHandler.WHAT_HOST_DISCONNECTED);
            }
            if( g_evhGame3Activity != null ) {
                g_evhGame3Activity.sendEmptyMessage(Game3Activity.SessionEventHandler.WHAT_HOST_DISCONNECTED);
            }
        }
    };
    /// 호스트로부터의 메시지 수신 이벤트 콜백
    private static IMessageReceivedEventHandler g_evhMessageReceivedCallback = new IMessageReceivedEventHandler()
    {
        @Override
        public void OnMessageReceived(GuestSession session, String sMessage)
        {
            /// 데이터 구분자 @
            String[] sp = sMessage.split("@");
            if( sp.length >= 1 ) {
                switch(sp[0].toUpperCase()) {
                
                /// 방 입장 요청에 대해 응답받음
                case Constants.PROTOCOL_ROOM_JOIN_RESPONSE: {
                    if( sp[1].equals("OK") ) {
                        /// 입장 허가받음 (룸 화면 이동)
                        if( g_evhLobbyActivity != null )
                            g_evhLobbyActivity.sendEmptyMessage(LobbyActivity.SessionEventHandler.WHAT_JOIN_ROOM_OK);
                    }
                    else {
                        /// 입장 금지됨
                        if( g_evhLobbyActivity != null )
                            g_evhLobbyActivity.sendEmptyMessage(LobbyActivity.SessionEventHandler.WHAT_JOIN_ROOM_NO);
                    }
                } break;
                
                /// 방 내 플레이어 목록 갱신
                case Constants.PROTOCOL_ROOM_PLAYERLIST_UPDATE: {
                    if( g_evhRoomGuestActivity != null ) {
                        String[] sPlayerList = sp[1].split("#");
                        
                        Message msg = g_evhRoomGuestActivity.obtainMessage();
                        msg.what = RoomGuestActivity.SessionEventHandler.WHAT_UPDATE_PLAYER_LIST;
                        msg.obj = sPlayerList;
                        g_evhRoomGuestActivity.sendMessage(msg);
                    }
                } break;
                
                /// 게임 선택/변경 됨
                case Constants.PROTOCOL_ROOM_SELECTED_GAME_UPDATE: {
                    g_nSelectedGame = Integer.parseInt(sp[1]);
                    if( g_evhRoomGuestActivity != null )
                        g_evhRoomGuestActivity.sendEmptyMessage(RoomGuestActivity.SessionEventHandler.WHAT_UPDATE_SELECTED_GAME);
                } break;

                /// 게임 시작함
                case Constants.PROTOCOL_ROOM_GAME_START: {
                    g_nSelectedGame = Integer.parseInt(sp[1]);
                    if( g_evhRoomGuestActivity != null )
                        g_evhRoomGuestActivity.sendEmptyMessage(RoomGuestActivity.SessionEventHandler.WHAT_GAMESTART);
                } break;
                
                
                /// 대기실로 돌아감
                case Constants.PROTOCOL_GAME_RETURN_TO_ROOM: {
                    if( g_evhGame1Activity != null ) {
                        g_evhGame1Activity.sendEmptyMessage(Game1Activity.SessionEventHandler.WHAT_RETURN_TO_ROOM);
                    }
                    else if( g_evhGame2Activity != null ) {
                        g_evhGame2Activity.sendEmptyMessage(Game2Activity.SessionEventHandler.WHAT_RETURN_TO_ROOM);
                    }
                    else if( g_evhGame3Activity != null ) {
                        g_evhGame3Activity.sendEmptyMessage(Game3Activity.SessionEventHandler.WHAT_RETURN_TO_ROOM);
                    }
                } break;
                
                
                
                /// 게임1 - 게임 데이터 받아오기
                case Constants.PROTOCOL_GAME1_INIT_GAME_DATA: {
                    if( g_evhGame1Activity != null ) {
                        String[] sPlayerTurnList = sp[1].split("#");
                        int nMyTurn = Integer.parseInt(sp[2]);
                        int nBomb = Integer.parseInt(sp[3]);
                        Message msg = g_evhGame1Activity.obtainMessage();
                        msg.what = Game1Activity.SessionEventHandler.WHAT_RECEIVED_GAME_DATA;
                        msg.obj = sPlayerTurnList;
                        msg.arg1 = nMyTurn;
                        msg.arg2 = nBomb;
                        g_evhGame1Activity.sendMessage(msg);
                    }
                } break;
                
                /// 게임1 - 턴 흐름
                case Constants.PROTOCOL_GAME1_TURN_CHANGED: {
                    if( g_evhGame1Activity != null ) {
                        int nTurn = Integer.parseInt(sp[1]);
                        Message msg = g_evhGame1Activity.obtainMessage();
                        msg.what = Game1Activity.SessionEventHandler.WHAT_TURN_CHANGED;
                        msg.arg1 = nTurn;
                        g_evhGame1Activity.sendMessage(msg);
                    }
                } break;
                
                /// 게임1 - 플레이어의 입력을 반영
                case Constants.PROTOCOL_GAME1_PLAYER_INPUT_UPDATE: {
                    if( g_evhGame1Activity != null ) {
                        int nTurn = Integer.parseInt(sp[1]);
                        int nTooth = Integer.parseInt(sp[2]);
                        Message msg = g_evhGame1Activity.obtainMessage();
                        msg.what = Game1Activity.SessionEventHandler.WHAT_PLAYER_INPUT_UPDATED;
                        msg.arg1 = nTurn;
                        msg.arg2 = nTooth;
                        g_evhGame1Activity.sendMessage(msg);
                    }
                } break;
                
                /// 게임1 - 게임 종료
                case Constants.PROTOCOL_GAME1_GAME_FINISH: {
                    if( g_evhGame1Activity != null ) {
                        int nLosePlayerTurn = Integer.parseInt(sp[1]);
                        Message msg = g_evhGame1Activity.obtainMessage();
                        msg.what = Game1Activity.SessionEventHandler.WHAT_GAME_FINISH;
                        msg.arg1 = nLosePlayerTurn;
                        g_evhGame1Activity.sendMessage(msg);
                    }
                } break;
                
                /// 게임1 - 재시작
                case Constants.PROTOCOL_GAME1_RETRY: {
                    if( g_evhGame1Activity != null ) {
                        g_evhGame1Activity.sendEmptyMessage(Game1Activity.SessionEventHandler.WHAT_GAME_RETRY);
                    }
                } break;


                /// 게임2 - 게임 데이터 받아오기
                case Constants.PROTOCOL_GAME2_INIT_GAME_DATA: {
                    if( g_evhGame2Activity != null ) {
                        String[] nicknameList = sp[1].split("#");
                        int nMyIdx = Integer.parseInt(sp[2]);
                        int nShape = Integer.parseInt(sp[3]);
                        Message msg = g_evhGame2Activity.obtainMessage();
                        msg.what = Game2Activity.SessionEventHandler.WHAT_RECEIVED_GAME_DATA;
                        msg.obj = nicknameList;
                        msg.arg1 = nMyIdx;
                        msg.arg2 = nShape;
                        g_evhGame2Activity.sendMessage(msg);
                    }
                } break;
                
                /// 게임2 - 입력 시작
                case Constants.PROTOCOL_GAME2_ON_SELECTION_TIME: {
                    if( g_evhGame2Activity != null ) {
                        g_evhGame2Activity.sendEmptyMessage(Game2Activity.SessionEventHandler.WHAT_ON_SELECTION_TIME);
                    }
                } break;
                
                /// 게임2 - 게임 종료 알림
                case Constants.PROTOCOL_GAME2_GAME_FINISH: {
                    if( g_evhGame2Activity != null ) {
                        Message msg = g_evhGame2Activity.obtainMessage();
                        msg.what = Game2Activity.SessionEventHandler.WHAT_GAME_FINISH;
                        msg.obj = sp[1];
                        g_evhGame2Activity.sendMessage(msg);
                    }
                } break;

                /// 게임2 - 재시작
                case Constants.PROTOCOL_GAME2_RETRY: {
                    if( g_evhGame2Activity != null ) {
                        g_evhGame2Activity.sendEmptyMessage(Game2Activity.SessionEventHandler.WHAT_GAME_RETRY);
                    }
                } break;
                
                
                /// 게임3 - 게임 데이터 받아오기
                case Constants.PROTOCOL_GAME3_INIT_GAME_DATA: {
                    if( g_evhGame3Activity != null ) {
                        String[] nicknameList = sp[1].split("#");
                        int nMyIdx = Integer.parseInt(sp[2]);
                        Message msg = g_evhGame3Activity.obtainMessage();
                        msg.what = Game3Activity.SessionEventHandler.WHAT_RECEIVED_GAME_DATA;
                        msg.obj = nicknameList;
                        msg.arg1 = nMyIdx;
                        g_evhGame3Activity.sendMessage(msg);
                    }
                } break;
                
                /// 게임3 - 1 to 25 시작 카운트다운
                case Constants.PROTOCOL_GAME3_1_TO_25_READY: {
                    if( g_evhGame3Activity != null ) {
                        int nReadyCountdown = Integer.parseInt(sp[1]);
                        Message msg = g_evhGame3Activity.obtainMessage();
                        msg.what = Game3Activity.SessionEventHandler.WHAT_1_TO_25_READY;
                        msg.arg1 = nReadyCountdown;
                        g_evhGame3Activity.sendMessage(msg);
                    }
                } break;

                /// 게임3 - 1 to 25 시작
                case Constants.PROTOCOL_GAME3_1_TO_25_START: {
                    if( g_evhGame3Activity != null ) {
                        g_evhGame3Activity.sendEmptyMessage(Game3Activity.SessionEventHandler.WHAT_1_TO_25_START);
                    }
                } break;
                
                /// 게임3 - 플레이어의 입력을 반영
                case Constants.PROTOCOL_GAME3_PLAYER_INPUT_UPDATE: {
                    if( g_evhGame3Activity != null ) {
                        int nRemainPlayers = Integer.parseInt(sp[1]);
                        Message msg = g_evhGame3Activity.obtainMessage();
                        msg.what = Game3Activity.SessionEventHandler.WHAT_PLAYER_INPUT_UPDATED;
                        msg.arg1 = nRemainPlayers;
                        g_evhGame3Activity.sendMessage(msg);
                    }
                } break;
                
                /// 게임3 - 게임 종료 알림
                case Constants.PROTOCOL_GAME3_GAME_FINISH: {
                    if( g_evhGame3Activity != null ) {
                        int nLoserIdx = Integer.parseInt(sp[1]);
                        Message msg = g_evhGame3Activity.obtainMessage();
                        msg.what = Game3Activity.SessionEventHandler.WHAT_GAME_FINISH;
                        msg.arg1 = nLoserIdx;
                        g_evhGame3Activity.sendMessage(msg);
                    }
                } break;
                
                /// 게임3 - 재시작
                case Constants.PROTOCOL_GAME3_RETRY: {
                    if( g_evhGame3Activity != null ) {
                        g_evhGame3Activity.sendEmptyMessage(Game3Activity.SessionEventHandler.WHAT_GAME_RETRY);
                    }
                } break;
                
                
                }
            }
        }
    };
    
    
    /// 호스트와 통신하기 위한 세션 객체
    private static GuestSession g_session;
    /// 선택된 게임 번호 (0: 선택되지않음, 1:게임1, 2:게임2, 3:게임3)
    private static int g_nSelectedGame;
    
    
    private GuestWorks() {}
    
    
    public static void Register_LobbyActivityEventHandler(LobbyActivity.SessionEventHandler evh)
    {
        g_evhLobbyActivity = evh;
    }
    public static void Register_RoomGuestActivityEventHandler(RoomGuestActivity.SessionEventHandler evh)
    {
        g_evhRoomGuestActivity = evh;
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
     * @brief 선택된 게임 번호 가져오기
     * @return 선택된 게임 번호
     */
    public static int GetSelectedGame()
    {
        return g_nSelectedGame;
    }
    
    
    /**
     * @brief 방 입장 시도 (호스트에 접속하여 플레이어 자격 얻기)
     * @param sHostname 호스트명
     */
    public static void JoinRoom(final String sHostname)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                /// 접속 시도
                g_session = new GuestSession();
                boolean bConnected = g_session.Connect(sHostname, Constants.ROOM_HOST_PORT);
    
                Log.d("QQQQQ", "호스트와의 연결 성공여부 : " + bConnected);
                /// 성공적으로 세션 연결 시, 플레이어 자격 요청
                if( bConnected ) {
                    g_session.Register_OnRemoteClosed(g_evhRemoteClosedCallback);
                    g_session.Register_OnMessageReceived(g_evhMessageReceivedCallback);
                    g_session.Send(Constants.PROTOCOL_ROOM_JOIN_REQUEST+"@"+Runtime.GetNickname());
                }
                /// 접속 실패 시, 실패 상황을 사용자에게 표시
                else {
                    if( g_evhLobbyActivity != null ) {
                        g_evhLobbyActivity.sendEmptyMessage(LobbyActivity.SessionEventHandler.WHAT_HOST_CONNECT_FAIL);
                    }
                }
            }
        }).start();
    }
    
    /**
     * @brief 방 나가기 (호스트와의 접속을 종료)
     */
    public static void QuitRoom()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Close();
                    g_session = null;
                }
            }
        }).start();
    }
    
    
    /**
     * @brief 플레이어 목록 요청
     */
    public static void RequestPlayerList()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Send(Constants.PROTOCOL_ROOM_PLAYERLIST_REQUEST);
                }
            }
        }).start();
    }
    
    /**
     * @brief 선택된 게임 정보 요청
     */
    public static void RequestSelectedGame()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Send(Constants.PROTOCOL_ROOM_SELECTED_GAME_REQUEST);
                }
            }
        }).start();
    }
    
    
    /**
     * @brief 게임1 - 플레이어가 데이터를 수신할 준비가 되었음을 알림
     */
    public static void Game1_GuestReady()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Send(Constants.PROTOCOL_GAME1_GUEST_READY);
                }
            }
        }).start();
    }
    
    /**
     * @brief 게임1 - 플레이어의 이빨 버튼 입력 정보를 호스트에게 송신
     * @param nMyTurn 턴 정보
     * @param nTooth 이빨 버튼 정보
     */
    public static void Game1_InputToothButton(final int nMyTurn, final int nTooth)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Send(Constants.PROTOCOL_GAME1_PLAYER_INPUT_REQUEST + "@" + nMyTurn +"@" + nTooth);
                }
            }
        }).start();
    }
    
    
    /**
     * @brief 게임2 - 플레이어가 데이터를 수신할 준비가 되었음을 알림
     */
    public static void Game2_GuestReady()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Send(Constants.PROTOCOL_GAME2_GUEST_READY);
                }
            }
        }).start();
    }
    
    /**
     * @brief 게임2 - 플레이어의 버튼 선택 정보를 호스트에게 송신
     * @param nPlayerIdx 플레이어 idx
     * @param nSelection 선택 정보
     */
    public static void Game2_PlayerSelection(final int nPlayerIdx, final int nSelection)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Send(Constants.PROTOCOL_GAME2_PLAYER_INPUT + "@" + nPlayerIdx + "@" + nSelection);
                }
            }
        }).start();
    }
    
    /**
     * @brief 게임3 - 플레이어가 데이터를 수신할 준비가 되었음을 알림
     */
    public static void Game3_GuestReady()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Send(Constants.PROTOCOL_GAME3_GUEST_READY);
                }
            }
        }).start();
    }
    
    /**
     * @brief 게임3 - 플레이어가 1 to 25를 끝냈음
     * @param nPlayerIdx 플레이어 idx
     */
    public static void Game3_PlayerFinish(final int nPlayerIdx)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if( g_session != null ) {
                    g_session.Send(Constants.PROTOCOL_GAME3_PLAYER_INPUT_REQUEST + "@" + nPlayerIdx);
                }
            }
        }).start();
    }
    
}
