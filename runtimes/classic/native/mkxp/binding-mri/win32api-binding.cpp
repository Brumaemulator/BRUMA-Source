#include "binding.h"
#include "binding-util.h"
#include "eventthread.h"
#include "sdl-util.h"
#include "sharedstate.h"
#include "graphics.h"
#include "input.h"
#include "../src/config.h"
#include "iniconfig.h"

#include <cstdlib>
#include <string>
#include <vector>
#include <map>
#include <iostream>
#include <fstream>
#include <random>
#include <ruby.h>
#include <SDL_filesystem.h>


typedef struct
{
    int one;
    int two;
    int three;
    int four;
} WRect;

typedef struct
{
    int x;
    int y;
} WPos;

typedef struct {
  long x;
  long y;
} POINT;

typedef struct
{
    void *hwnd;
    unsigned int message;
    unsigned int wParam;
    long lParam;
    unsigned long time;
    POINT pt;
    unsigned long lPrivate;
} MSG;

//Implement methods from different dlls. Most methods are incomplete and implemented only to prevents exceptions.

//Dummy Implementation
VALUE DummyInit(int argc, VALUE *argv)
{
    return rb_fix_new(0);
}

//User32

VALUE User32Getasynckeystate(int argc, VALUE *argv)
{
    VALUE num;
    rb_scan_args(argc, argv, "1", &num);
    
	int state = shState->input().asyncKeyState(NUM2INT(num));
    
    if (state == 0x8000)
    {
        return rb_fix_new(1<<15);
    }
    else 
    {
        return rb_fix_new(0);
    }
}

VALUE User32Getkeystate(int argc, VALUE *argv)
{
    return User32Getasynckeystate(argc, argv);
}

VALUE User32Getkeyboardstate(int argc, VALUE *argv)
{
    VALUE state;
    rb_scan_args(argc, argv, "1", &state);
    
    char *stPtr;

    switch (TYPE(state))
    {
        case T_FIXNUM:
        case T_BIGNUM:
            stPtr = (char*) NUM2PTR(state);
            break;
        case T_STRING:
            stPtr = RSTRING_PTR(state);
            break;
        default:
            Debug()<<"User32Getkeyboardstate: Unvalid value.";
            break;
    }
    
    if(stPtr){
        for(int i = 255; i>0; i--)
        {
            stPtr[i] = shState->input().asyncKeyState(i) == 0x8000 ? 128 : 0;
        }
        return Qtrue;
    } else {
        return Qfalse;
    }
}

VALUE User32Getkeyboardlayout(int argc, VALUE *argv)
{
    VALUE id;
    rb_scan_args(argc, argv, "1", &id);
    
    return rb_fix_new(0);
}

VALUE User32Mapvirtualkeyexa(int argc, VALUE *argv)
{
    VALUE uCode, uMapType, dwhkl;
    rb_scan_args(argc, argv, "3", &uCode,&uMapType,&dwhkl);
    
    return uCode;
}

VALUE User32Mapvirtualkeyex(int argc, VALUE *argv)
{    
    return User32Mapvirtualkeyexa(argc, argv);
}

VALUE User32Tounicodeex (int argc, VALUE *argv)
{
    VALUE wVirtKey, wScanCode, lpKeyState, pwszBuff, cchBuff, wFlags, dwhkl;
    rb_scan_args(argc, argv, "7", &wVirtKey, &wScanCode, &lpKeyState, &pwszBuff, &cchBuff, &wFlags, &dwhkl);
    
	//Fix later to write chars to bufffer and return real value
    return rb_fix_new(0);
}

VALUE User32Getcursorpos(int argc, VALUE *argv)
{
    VALUE pos;
    
    rb_scan_args(argc, argv, "1", &pos);
    
    WPos* wPos = reinterpret_cast<WPos*>(RSTRING_PTR(pos));
    
    wPos->x = shState->input().mouseX();
    wPos->y = shState->input().mouseY();
    
    return Qtrue;
}

VALUE User32Getclientrect(int argc, VALUE *argv)
{
    VALUE rect;
    VALUE num;
    
    rb_scan_args(argc, argv, "2", &num, &rect);
        
    WRect* wRect = reinterpret_cast<WRect*>(RSTRING_PTR(rect));
    wRect->one = 0;
    wRect->two = 0;
    wRect->three = shState->graphics().width();
    wRect->four = shState->graphics().height();

    return Qtrue;
    
}

VALUE User32Getwindowrect(int argc, VALUE *argv)
{
    return User32Getclientrect(argc, argv);
}

VALUE User32Screentoclient(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE User32Findwindowa(int argc, VALUE *argv)
{
    return rb_fix_new(42);
}

VALUE User32Findwindow(int argc, VALUE *argv)
{
    return rb_fix_new(42);
}

VALUE User32Findwindowex(int argc, VALUE *argv)
{
    return rb_fix_new(42);
}

VALUE User32Getforegroundwindow(int argc, VALUE *argv)
{
    return rb_fix_new(42);
}

VALUE User32Getactivewindow(int argc, VALUE *argv)
{
    return rb_fix_new(42);
}

VALUE User32Createwindowex(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE User32Getsystemmetrics(int argc, VALUE *argv)
{
    VALUE num;
    rb_scan_args(argc, argv, "1", &num);
    
    switch(NUM2INT(num))
    {
        case 0:
            return rb_fix_new(shState->graphics().width());
            break;
        case 1:
            return rb_fix_new(shState->graphics().height());
            break;
        case 4:
            return rb_fix_new(shState->graphics().height());
            break;
        case 7:
            return rb_fix_new(0);
            break;
        case 8:
            return rb_fix_new(0);
            break;
        default:
            return rb_fix_new(shState->graphics().height());
    }
}

VALUE User32Getwindowthreadprocessid(int argc, VALUE *argv)
{
    return rb_fix_new(4263);
}

VALUE User32Systemparametersinfoa(int argc, VALUE *argv)
{
    VALUE action;
    VALUE uiParam;
    VALUE pvParam;
    VALUE fWinIni;
    
    rb_scan_args(argc, argv, "13", &action, &uiParam, &pvParam, &fWinIni);
    
    switch(NUM2INT(action))
    {
        case 0x30: 
        {
            WRect* wRect = reinterpret_cast<WRect*>(RSTRING_PTR(pvParam));
            wRect->three = shState->graphics().width();
            wRect->four = shState->graphics().height();
            return Qtrue;
            break;
        }
            
        default:
            return Qtrue;
    }
}

VALUE User32Systemparametersinfo(int argc, VALUE *argv)
{
    return User32Systemparametersinfoa(argc,argv);
}

VALUE User32Showwindow(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE User32Setwindowlong(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE User32Setwindowpos(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE User32Messagebox(int argc, VALUE *argv)
{
    VALUE message;
    message = argv[1];
    shState->eThread().showMessageBox((const char*)StringValueCStr(message));
    
    return Qnil;
}

VALUE User32Messageboxa(int argc, VALUE *argv)
{
    VALUE message;
    message = argv[1];
    shState->eThread().showMessageBox((const char*)StringValueCStr(message));
    
    return Qnil;
}

VALUE User32Getmessage(int argc, VALUE *argv)
{
    VALUE lpMsg;
    VALUE hWnd;
    VALUE wMsgFilterMin;
    VALUE wMsgFilterMax;
    
    rb_scan_args(argc, argv, "13", &lpMsg, &hWnd, &wMsgFilterMin, &wMsgFilterMax);

    MSG* msg = reinterpret_cast<MSG*>(RSTRING_PTR(lpMsg));
    msg->hwnd = new int(42);
    
    return rb_fix_new(1);
}

VALUE User32Translatemessage(int argc, VALUE *argv)
{
    VALUE lpMsg;
    
    rb_scan_args(argc, argv, "1", &lpMsg);
    
    return rb_fix_new(1);
}

VALUE User32Dispatchmessage(int argc, VALUE *argv)
{
    VALUE lpMsg;
    
    rb_scan_args(argc, argv, "1", &lpMsg);
    
    return rb_fix_new(1);
}

//Kernel32

VALUE Kernel32Getprivateprofilestring(int argc, VALUE *argv)
{
    VALUE lpAppName, lpKeyName, lpDefault, lpReturnedString, nSize, lpFileName;
    rb_scan_args(argc, argv, "15", &lpAppName, &lpKeyName, &lpDefault, &lpReturnedString, &nSize, &lpFileName);
    
    std::string lpdefault;
    if(RB_TYPE_P(lpDefault, T_STRING))
    {
        lpdefault = StringValueCStr(lpDefault);
    }
    else
    {
        VALUE strValue = rb_funcall2(lpDefault, rb_intern("to_s"), 0, 0);
        lpdefault = StringValueCStr(strValue);
    }
    
#ifdef INI_ENCODING
    std::string filename = StringValueCStr(lpFileName);
    std::string appname = StringValueCStr(lpAppName);
    std::string keyname = StringValueCStr(lpKeyName);
    
    if(!convertIfNotValidUTF8("", filename) &&
    !convertIfNotValidUTF8("", appname) &&
    !convertIfNotValidUTF8("", keyname) &&
    !convertIfNotValidUTF8("", lpdefault)) {
        memcpy(RSTRING_PTR(lpReturnedString), lpdefault.c_str(), lpdefault.size());
        return rb_fix_new(0);
    }
    
    
    SDLRWStream iniFile(filename.c_str(), "r");
    if (iniFile)
	{
        INIConfiguration ic;
        if(ic.load(iniFile.stream()))
        {
            std::string value = ic.getStringProperty(appname.c_str(), keyname.c_str(), lpdefault.c_str());
            memcpy(RSTRING_PTR(lpReturnedString), value.c_str(), value.size());
            return rb_fix_new(value.size());
       }
    }
#endif

    memcpy(RSTRING_PTR(lpReturnedString), lpdefault.c_str(), lpdefault.size());
    return rb_fix_new(0);
}

VALUE Kernel32Getprivateprofileint(int argc, VALUE *argv)
{
    VALUE lpAppName, lpKeyName, lpDefault, lpFileName;
    rb_scan_args(argc, argv, "4", &lpAppName, &lpKeyName, &lpDefault, &lpFileName);
    
    int lpdefault;
    if(RB_TYPE_P(lpDefault, T_FIXNUM))
    {
        lpdefault = FIX2INT(lpDefault);
    }
    else
    {
        VALUE intValue = rb_funcall2(lpDefault, rb_intern("to_i"), 0, 0);
        lpdefault = FIX2INT(intValue);
    }
    
#ifdef INI_ENCODING
    std::string filename = StringValueCStr(lpFileName);
    std::string appname = StringValueCStr(lpAppName);
    std::string keyname = StringValueCStr(lpKeyName);
    
    if(!convertIfNotValidUTF8("", filename) &&
    !convertIfNotValidUTF8("", appname) &&
    !convertIfNotValidUTF8("", keyname)) {
        return rb_fix_new(lpdefault);
    }
    
    
    SDLRWStream iniFile(filename.c_str(), "r");
    if (iniFile)
	{
        INIConfiguration ic;
        if(ic.load(iniFile.stream()))
        {
            int value = ic.getIntProperty(appname.c_str(), keyname.c_str(), lpdefault);
            return rb_fix_new(value);
       }
    }
#endif

    return rb_fix_new(lpdefault);
}

VALUE Kernel32Writeprivateprofilestring(int argc, VALUE *argv)
{
    VALUE lpAppName, lpKeyName, lpString, lpFileName;
    rb_scan_args(argc, argv, "13", &lpAppName, &lpKeyName, &lpString, &lpFileName);
    
#ifdef INI_ENCODING
    std::string filename = StringValueCStr(lpFileName);
    std::string appname = StringValueCStr(lpAppName);
    std::string keyname = StringValueCStr(lpKeyName);
    std::string str = StringValueCStr(lpString);
    
    if(!convertIfNotValidUTF8("", filename) &&
    !convertIfNotValidUTF8("", appname) &&
    !convertIfNotValidUTF8("", keyname) &&
    !convertIfNotValidUTF8("", str)) {
        return Qfalse;
    }
    
    SDLRWStream iniFile(filename.c_str(), "r+");
    if (iniFile)
	{
        INIConfiguration ic;
        if(ic.load(iniFile.stream()))
        {
            std::ofstream file(filename.c_str());
            ic.addProperty(appname, keyname, str);
            ic.save(file);
            return Qtrue;
       }
    }
#endif
    return Qfalse;
}

VALUE Kernel32Getcurrentthreadid(int argc, VALUE *argv)
{
    return rb_fix_new(4263);
}

VALUE Kernel32Getsystempowerstatus(int argc, VALUE *argv)
{
    return Qfalse;
}

VALUE Kernel32Getuserdefaultlangid(int argc, VALUE *argv)
{
    return rb_fix_new(0x09);
}

VALUE Kernel32Rtlzeromemory(int argc, VALUE *argv)
{
    VALUE destination, length;
    rb_scan_args(argc, argv, "2", &destination, &length);
    
    std::memset(RSTRING_PTR(destination), 0, NUM2INT(length));
    
    return Qnil;
}

VALUE Kernel32Rtlmovememory(int argc, VALUE *argv)
{
    VALUE destination, source, length;
    rb_scan_args(argc, argv, "3", &destination, &source, &length);

    char* destinationPtr;
    if(RB_TYPE_P(destination, T_STRING))
    {
        destinationPtr = RSTRING_PTR(destination);
    }
    else if(RB_TYPE_P(destination, T_FIXNUM) || RB_TYPE_P(source, T_BIGNUM))
    {
        destinationPtr = reinterpret_cast<char*>(NUM2PTR(destination));
    }

    char* sourcePtr;
    if(RB_TYPE_P(source, T_STRING))
    {
        sourcePtr = RSTRING_PTR(source);
    }
    else if(RB_TYPE_P(source, T_FIXNUM) || RB_TYPE_P(source, T_BIGNUM))
    {
        sourcePtr = reinterpret_cast<char*>(NUM2PTR(source));
    }

#if INTPTR_MAX == INT32_MAX
    //memmove(destinationPtr, sourcePtr, NUM2ULONG(length));
    //return PTR2NUM(destinationPtr);
    return Qnil;
#else
    return Qnil;
#endif
}

VALUE Kernel32Multibytetowidechar(int argc, VALUE *argv)
{
	//Don't have a proper working solution so just copy original string
    VALUE CodePage, dwFlags, lpMultiByteStr, cchMultiByte, lpWideCharStr, cchWideChar;
    
    rb_scan_args(argc, argv, "15", &CodePage, &dwFlags, &lpMultiByteStr, &cchMultiByte, &lpWideCharStr, &cchWideChar);
    
    char* multibyte = RSTRING_PTR(lpMultiByteStr);
    
    if(NUM2INT(cchWideChar) == 0)
    {
        return rb_fix_new(sizeof(multibyte));
    }
    
    memcpy(RSTRING_PTR(lpWideCharStr), multibyte, sizeof(multibyte));
    
    return rb_fix_new(sizeof(multibyte));
}

VALUE Kernel32Widechartomultibyte(int argc, VALUE *argv)
{
	//Don't have a proper working solution so just copy original string
    VALUE CodePage, dwFlags, lpWideCharStr, cchWideChar, lpMultiByteStr, cchMultiByte, lpDefaultChar, lpUsedDefaultChar;
    
    rb_scan_args(argc, argv, "17", &CodePage, &dwFlags, &lpWideCharStr, &cchWideChar, &lpMultiByteStr, &cchMultiByte, &lpDefaultChar, &lpUsedDefaultChar);
    
    char* widechar = RSTRING_PTR(lpWideCharStr);
    
    if(NUM2INT(cchMultiByte) == 0)
    {
        return rb_fix_new(sizeof(widechar));
    }
        
    memcpy(RSTRING_PTR(lpMultiByteStr), widechar, sizeof(widechar));
    
    return rb_fix_new(sizeof(widechar));
}

VALUE Kernel32Gettemppatha(int argc, VALUE *argv)
{
    VALUE nBufferLength, lpBuffer;
    
    rb_scan_args(argc, argv, "2", &nBufferLength, &lpBuffer);
    
    char* buffer = RSTRING_PTR(lpBuffer);
    char* path = "UserData/Temp/";
    
    memcpy(buffer, path, strlen(path)+1);
    
    return rb_fix_new(strlen(path)+1);
}

VALUE Kernel32Gettempfilenamea(int argc, VALUE *argv)
{
    VALUE lpPathName, lpPrefixString, uUnique, lpTempFileName;
    
    rb_scan_args(argc, argv, "4", &lpPathName, &lpPrefixString, &uUnique, &lpTempFileName);

    std::string chars("0123456789abcdef");
    std::random_device randev;
    std::mt19937 generator(randev());
    std::shuffle(chars.begin(), chars.end(), generator);
    std::string randString = chars.substr(0, 4);
    
    char* path = RSTRING_PTR(lpPathName);
    char* pre = RSTRING_PTR(lpPrefixString);
    int unique = NUM2INT(uUnique);
    char* tempFileName = RSTRING_PTR(lpTempFileName);

    sprintf(tempFileName, "%s/%s%s.TMP", path, pre, randString.c_str());
    
    if (unique == 0)
    {
        char* buff = new char[256];
        sprintf(buff, "%s/%s", shState->config().gameFolder.c_str(), tempFileName);
        std::ofstream of(buff);
    }
    
    
    return rb_fix_new(unique);
}
//RGSS Linker

VALUE RgsslinkerRgsslinkerinitialize(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

//RubyScreen

VALUE RubyscreenTakescreenshot(int argc, VALUE *argv)
{
    VALUE filename;
    rb_scan_args(argc, argv, "1", &filename);
    SafeStringValue(filename);
    try
    {
        shState->graphics().screenshot(RSTRING_PTR(filename));
    }
    catch(const Exception &e)
    {
        raiseRbExc(e);
    }
    
    return Qtrue;
}

//Wininet
VALUE WininetInternetopena(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE WininetInternetconnecta(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE WininetInternetopenurl(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE WininetInternetreadfile(int argc, VALUE *argv)
{
    return Qfalse;
}

VALUE WininetInternetclosehandle(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE WininetHttpqueryinfo(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

//Onlinefuncs

VALUE OnlinefuncsCreatesession(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

//Systemkgl2klib

VALUE Systemkgl2Kglversion(int argc, VALUE *argv)
{
    return rb_fix_new(300);
}

//Libbackgroundrunning

VALUE LibbackgroundrunningOnfocus(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

//steam_api.dll

VALUE SteamapiSteamapiinit(int argc, VALUE *argv)
{
    return rb_fix_new(1);
}

VALUE SteamapiSteamapishutdown(int argc, VALUE *argv)
{
    return Qnil;
}

//Bindings

typedef VALUE (*FunPtr)(int, VALUE *);
std::map<std::string, FunPtr> ImpMap = 
{
    {"DummyInit", DummyInit},
    {"User32Getasynckeystate", User32Getasynckeystate},
    {"User32Getkeystate", User32Getkeystate},
    {"User32Getkeyboardstate", User32Getkeyboardstate},
	{"User32Getkeyboardlayout", User32Getkeyboardlayout},
	{"User32Mapvirtualkeyexa", User32Mapvirtualkeyexa},
	{"User32Mapvirtualkeyex", User32Mapvirtualkeyex},
	{"User32Tounicodeex", User32Tounicodeex},
    {"User32Getcursorpos", User32Getcursorpos},
    {"User32Getclientrect", User32Getclientrect},
    {"User32Getwindowrect", User32Getwindowrect},
    {"User32Screentoclient", User32Screentoclient},
    {"User32Findwindowa", User32Findwindowa},
    {"User32Findwindow", User32Findwindow},
    {"User32Findwindowex", User32Findwindowex},
    {"User32Getforegroundwindow", User32Getforegroundwindow},
    {"User32Getactivewindow", User32Getactivewindow},
    {"User32Createwindowex", User32Createwindowex},
    {"User32Getsystemmetrics", User32Getsystemmetrics},
    {"User32Getwindowthreadprocessid", User32Getwindowthreadprocessid},
    {"User32Systemparametersinfoa", User32Systemparametersinfoa},
    {"User32Systemparametersinfo", User32Systemparametersinfo},
    {"User32Showwindow", User32Showwindow},
    {"User32Setwindowlong", User32Setwindowlong},
    {"User32Setwindowpos", User32Setwindowpos},
    {"User32Messagebox", User32Messagebox},
    {"User32Messageboxa", User32Messageboxa},
    {"User32Getmessage", User32Getmessage},
    {"User32Translatemessage", User32Translatemessage},
    {"User32Dispatchmessage", User32Dispatchmessage},
    {"Kernel32Getprivateprofilestring", Kernel32Getprivateprofilestring},
    {"Kernel32Getprivateprofileint", Kernel32Getprivateprofileint},
    {"Kernel32Writeprivateprofilestring", Kernel32Writeprivateprofilestring},
    {"Kernel32Getcurrentthreadid", Kernel32Getcurrentthreadid},
    {"Kernel32Getsystempowerstatus", Kernel32Getsystempowerstatus},
    {"Kernel32Getuserdefaultlangid", Kernel32Getuserdefaultlangid},
    {"Kernel32Rtlzeromemory", Kernel32Rtlzeromemory},
    {"Kernel32Rtlmovememory", Kernel32Rtlmovememory},
    {"Kernel32Multibytetowidechar", Kernel32Multibytetowidechar},
    {"Kernel32Widechartomultibyte", Kernel32Widechartomultibyte},
    {"Kernel32Gettemppatha", Kernel32Gettemppatha},
    {"Kernel32Gettempfilenamea", Kernel32Gettempfilenamea},
    {"RgsslinkerRgsslinkerinitialize", RgsslinkerRgsslinkerinitialize},
    {"RubyscreenTakescreenshot", RubyscreenTakescreenshot},
    {"WininetInternetopena", WininetInternetopena},
    {"WininetInternetconnecta", WininetInternetconnecta},
    {"WininetInternetopenurl", WininetInternetopenurl},
    {"WininetInternetreadfile", WininetInternetreadfile},
    {"WininetInternetclosehandle", WininetInternetclosehandle},
    {"WininetHttpqueryinfo", WininetHttpqueryinfo},
    {"OnlinefuncsCreatesession", OnlinefuncsCreatesession},
    {"Systemkgl2Kglversion", Systemkgl2Kglversion},
    {"LibbackgroundrunningOnfocus", LibbackgroundrunningOnfocus},
    {"SteamapiSteamapiinit",SteamapiSteamapiinit},
    {"SteamapiSteamapishutdown",SteamapiSteamapishutdown}
};

std::string capitalize(std::string str)
{
    if (str.length()>0)
    {
        str[0] = std::toupper(str[0]);
        for (size_t i = 1; i < str.length(); i++)
            str[i] = std::tolower(str[i]);
    }
    
    return str;
}

std::string removeNonAlphaNumeric(std::string str)
{
    if (str.length()>0)
    {
        str.erase( std::remove_if( str.begin(), str.end(), []( char c ) { return !std::isalnum(c) ; } ), str.end() ) ;
    }
    return str;
}

std::string removeExt(std::string str)
{
    return str.substr(0, str.find_last_of(".")); 
}

bool checkImp(std::string &imp)
{
    return ImpMap.find(imp) != ImpMap.end();
}

VALUE win32apiInitialize (int argc, VALUE* argv, VALUE self)
{
    VALUE dll;
    VALUE function;
    VALUE opts;
    rb_scan_args(argc, argv, "2*", &dll, &function, &opts);
    
    std::string strDll = StringValueCStr(dll);
    std::string strFunction = StringValueCStr(function);
    
    strDll = removeExt(strDll);
    strDll = removeNonAlphaNumeric(strDll);
    strFunction = removeNonAlphaNumeric(strFunction);
    strDll = capitalize(strDll);
    strFunction = capitalize(strFunction);
    
    std::string imp = strDll + strFunction;
        
    VALUE callImp;
    if(checkImp(imp))
    {
       callImp = rb_str_new_cstr(imp.c_str()); 
    }
    else
    {
       callImp = rb_str_new_cstr("DummyInit"); 
    }
    
    rb_iv_set(self, "imp", callImp);
    return Qnil;
}

VALUE win32apiCall (int argc, VALUE* argv, VALUE self){
    VALUE imp = rb_iv_get(self, "imp");
    std::string strImp = StringValueCStr(imp);
    return ImpMap[strImp](argc,argv);
}


void
win32apiBindingInit()
{
    VALUE win32api = rb_define_class("Win32API", rb_cObject);
    _rb_define_method(win32api, "initialize", win32apiInitialize);
    _rb_define_method(win32api, "call", win32apiCall);
    rb_define_alias(win32api,  "Call", "call");
}