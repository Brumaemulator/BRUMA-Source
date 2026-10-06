package com.hatkid.mkxpz;
import org.json.*;
final class ConfigJson {
    // Keep JSON ASCII on disk: the native encoding detector can pass an invalid
    // iconv descriptor for UTF-8 configurations. JSON escapes preserve Unicode.
    static String nativeText(JSONObject config) {
        String source=config.toString();StringBuilder encoded=new StringBuilder(source.length());
        final char[] hex="0123456789abcdef".toCharArray();
        for(int i=0;i<source.length();i++) {
            char c=source.charAt(i);
            if(c<128)encoded.append(c);
            else {encoded.append((char)92).append('u');encoded.append(hex[(c>>>12)&15]);encoded.append(hex[(c>>>8)&15]);encoded.append(hex[(c>>>4)&15]);encoded.append(hex[c&15]);}
        }
        return encoded.toString();
    }
    static JSONObject parse(String source)throws JSONException {
        StringBuilder clean=new StringBuilder();boolean quote=false,escape=false;
        for(int i=0;i<source.length();i++) {
            char c=source.charAt(i);
            if(quote){clean.append(c);if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='"')quote=false;continue;}
            if(c=='"'){quote=true;clean.append(c);continue;}
            if(c=='/'&&i+1<source.length()&&source.charAt(i+1)=='/'){while(i+1<source.length()&&source.charAt(i+1)!='\n')i++;continue;}
            if(c=='/'&&i+1<source.length()&&source.charAt(i+1)=='*'){i+=2;while(i+1<source.length()&&!(source.charAt(i)=='*'&&source.charAt(i+1)=='/'))i++;i++;clean.append(' ');continue;}
            if(c!='\ufeff')clean.append(c);
        }
        source=clean.toString();clean.setLength(0);quote=false;escape=false;
        for(int i=0;i<source.length();i++) {
            char c=source.charAt(i);
            if(quote){clean.append(c);if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='"')quote=false;continue;}
            if(c=='"')quote=true;
            if(c==','){int next=i+1;while(next<source.length()&&Character.isWhitespace(source.charAt(next)))next++;if(next<source.length()&&(source.charAt(next)=='}'||source.charAt(next)==']'))continue;}
            clean.append(c);
        }
        return new JSONObject(clean.toString());
    }
}


