package wooks.hanjanhaeyo;


/**
 * @brief 런타임 데이터
 */
public class Runtime
{
    /// 자신의 닉네임
    private static String g_sNickname;
    /// 자신이 호스트인지 아닌지 여부
    private static boolean g_bIsHost;
    
    
    private Runtime() {}
    
    
    /**
     * @breif 자신의 닉네임을 가져온다.
     * @return 자신의 닉네임 문자열
     */
    public static String GetNickname()
    {
        return g_sNickname;
    }
    
    /**
     * @brief 자신의 닉네임을 설정한다.
     * @param sNickname 설정한 닉네임 문자열
     */
    public static void SetNickname(String sNickname)
    {
        g_sNickname = sNickname;
    }
    
    /**
     * @brief 자신이 호스트인지 아닌지 여부를 알아낸다.
     * @return 호스트 여부
     */
    public static boolean IsHost()
    {
        return g_bIsHost;
    }
    
    /**
     * @brief 자신이 호스트인지 아닌지 여부를 설정한다.
     * @param bHost 호스트 여부
     */
    public static void SetHost(boolean bHost)
    {
        g_bIsHost = bHost;
    }
    
}
