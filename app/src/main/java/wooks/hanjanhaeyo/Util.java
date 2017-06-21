package wooks.hanjanhaeyo;

import java.util.ArrayList;


/**
 * @breif 유틸리티 클래스
 */
public class Util
{
    
    
    
    private Util() {}
    
    
    /**
     * @brief String.split() 시 빈 문자열이 포함되는 경우를 제외시키고 생성하는 메서드
     * @param sOriginal 대상 문자열
     * @param sRegex split하기 위한 delimiter
     * @return 생성된 문자열 배열
     */
    public static String[] SplitWithoutEmptyStrings(String sOriginal, String sRegex)
    {
        String[] sp = sOriginal.split(sRegex);
        ArrayList<String> list = new ArrayList<>();
        for(int i=0; i < sp.length; i++) {
            if( sp[i].equals("") == false )
                list.add(sp[i]);
        }
        return list.toArray(new String[list.size()]);
    }
    
}
