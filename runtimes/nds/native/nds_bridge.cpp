// SPDX-License-Identifier: GPL-3.0-or-later
#include <jni.h>
#include <android/log.h>
#include "NDS.h"
#include "Args.h"
#include "state.h"
#include <fstream>
#include <cstring>
#include <ctime>
#include <stdexcept>
static std::unique_ptr<melonDS::NDS> console;
static std::string str(JNIEnv*e,jstring s){const char*p=e->GetStringUTFChars(s,nullptr);std::string r(p);e->ReleaseStringUTFChars(s,p);return r;}
static void fail(JNIEnv*e,const char*s){e->ThrowNew(e->FindClass("java/io/IOException"),s);}
extern "C" JNIEXPORT void JNICALL Java_com_linkcore_emulator_NdsNative_open(JNIEnv*e,jobject,jstring rom,jstring save,jstring dir,jint language){
 try{
 if(console)throw std::runtime_error("NDS session already open");
 state=BrumaState{};state.directory=str(e,dir);state.savePath=str(e,save);
 auto path=str(e,rom);std::ifstream f(path,std::ios::binary|std::ios::ate);
 if(!f)throw std::runtime_error("Cannot read NDS ROM");auto size=f.tellg();
 if(size<512||size>512LL*1024*1024)throw std::runtime_error("Invalid NDS ROM size");
 auto data=std::make_unique<melonDS::u8[]>(size);f.seekg(0);f.read((char*)data.get(),size);if(!f)throw std::runtime_error("Incomplete NDS ROM");
 auto cart=melonDS::NDSCart::ParseROM(std::move(data),(melonDS::u32)size,&state);if(!cart)throw std::runtime_error("Unsupported NDS ROM");
 melonDS::NDSArgs args;
 if(language<0||language>6)throw std::runtime_error("Invalid NDS firmware language");
 // Configure both firmware user-data copies before direct boot; preserve other settings.
 for(auto& user:args.Firmware.GetUserData()){
  user.Settings=(user.Settings & ~melonDS::Firmware::Language::Reserved)|language;
  user.UpdateChecksum();
 }
 args.Firmware.UpdateChecksums();
 auto renderer=static_cast<melonDS::SoftRenderer*>(args.Renderer3D.get());
 console=std::make_unique<melonDS::NDS>(std::move(args),&state);renderer->SetThreaded(true,console->GPU);
 console->SetNDSCart(std::move(cart));console->Reset();
 std::ifstream sf(state.savePath,std::ios::binary|std::ios::ate);
 if(sf){auto n=sf.tellg();if(n>0&&n<=16*1024*1024){state.save.resize(n);sf.seekg(0);sf.read((char*)state.save.data(),n);if(!sf)throw std::runtime_error("Cannot read NDS save");console->SetNDSSave(state.save.data(),state.save.size());}}
 console->SetupDirectBoot(path.substr(path.find_last_of('/')+1));
 const int bootLanguage=console->ARM9Read16(0x027FFCE4)&7;
 if(bootLanguage!=language)throw std::runtime_error("NDS boot language mismatch");
 __android_log_print(ANDROID_LOG_INFO,"BrumaNDS","Firmware language=%d, game boot language=%d",language,bootLanguage);
 auto now=std::time(nullptr);tm t{};localtime_r(&now,&t);console->RTC.SetDateTime(t.tm_year+1900,t.tm_mon+1,t.tm_mday,t.tm_hour,t.tm_min,t.tm_sec);console->Start();
 }catch(const std::exception&x){console.reset();fail(e,x.what());}
}
extern "C" JNIEXPORT jint JNICALL Java_com_linkcore_emulator_NdsNative_frame(JNIEnv*e,jobject,jint keys,jint x,jint y,jobject video,jshortArray audio){
 if(!console)return -1;
 console->SetKeyMask((~keys)&0xFFF);if(x>=0)console->TouchScreen(x,y);else console->ReleaseScreen();console->RunFrame();
 if(!console->IsRunning()){fail(e,"NDS core stopped");return -1;}
 auto pixels=(uint8_t*)e->GetDirectBufferAddress(video);if(!pixels||e->GetDirectBufferCapacity(video)<256*384*4){fail(e,"Invalid video buffer");return -1;}
 // melonDS outputs BGRA words; Android Bitmap buffers use RGBA bytes.
 int front=console->GPU.FrontBuffer;auto out=reinterpret_cast<uint32_t*>(pixels);
 for(int s=0;s<2;s++){auto src=console->GPU.Framebuffer[front][s].get();for(int i=0;i<256*192;i++){auto c=src[i];out[s*256*192+i]=(c&0xFF00FF00)|((c&255)<<16)|((c>>16)&255);}}
 short samples[4096];int count=console->SPU.ReadOutput(samples,std::min(2048,e->GetArrayLength(audio)/2));e->SetShortArrayRegion(audio,0,count*2,samples);return count*2;
}
extern "C" JNIEXPORT void JNICALL Java_com_linkcore_emulator_NdsNative_flush(JNIEnv*e,jobject){if(!flushSave())fail(e,"Cannot write NDS save");}
extern "C" JNIEXPORT void JNICALL Java_com_linkcore_emulator_NdsNative_close(JNIEnv*e,jobject){bool ok=flushSave();console.reset();if(!ok)fail(e,"Cannot write NDS save");}
