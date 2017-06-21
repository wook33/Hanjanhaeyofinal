package wooks.hanjanhaeyo;


/**
 * @brief 호스트 상태를 나타내는 열거형
 */
public enum EHostState
{
    // 서버가 닫혀있음.
    CLOSED,
    // 서버가 열려있고, 게스트의 접속을 기다리는 상태.
    ROOM_WAITING,
    // 서버가 열려있고, 게임이 진행 중인 상태. 새로운 게스트가 접속할 수 없다.
    GAME_PLAYING
}
