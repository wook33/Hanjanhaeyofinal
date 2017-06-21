package wooks.hanjanhaeyo;


/**
 * @brief 방 정보를 나타내는 클래스
 */
public class HostInfo
{
    private String m_sHostname;
    private String m_sNickname;
    
    
    public HostInfo(String sHostname, String sNickname)
    {
        m_sHostname = sHostname;
        m_sNickname = sNickname;
    }
    
    /**
     * @breif 호스트명을 가져온다.
     * @return 호스트명
     */
    public String GetHostname()
    {
        return m_sHostname;
    }
    
    /**
     * @breif 해당 호스트의 닉네임을 가져온다.
     * @return 호스트의 닉네임 문자열
     */
    public String GetNickname()
    {
        return m_sNickname;
    }
    
    
    /**
     * @brief listView에는 닉네임만을 표시하기 위해서 toString()을 오버라이드한다.
     * @return 호스트의 닉네임 문자열
     */
    @Override
    public String toString()
    {
        return m_sNickname;
    }
}
