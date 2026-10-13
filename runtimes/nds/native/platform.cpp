// SPDX-License-Identifier: GPL-3.0-or-later
#include "Platform.h"
#include "state.h"
#include <android/log.h>
#include <cstdio>
#include <cstdarg>
#include <cstring>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <chrono>
#include <unistd.h>
#include <fcntl.h>
#include <dlfcn.h>
BrumaState state;
bool flushSave(){
 if(!state.dirty)return true;
 auto temp=state.savePath+".tmp";FILE*f=fopen(temp.c_str(),"wb");if(!f)return false;
 bool ok=fwrite(state.save.data(),1,state.save.size(),f)==state.save.size();
 ok=(fflush(f)==0)&&ok;ok=(fsync(fileno(f))==0)&&ok;ok=(fclose(f)==0)&&ok;
 if(ok)ok=rename(temp.c_str(),state.savePath.c_str())==0;
 if(ok)state.dirty=false;else __android_log_print(ANDROID_LOG_ERROR,"BrumaNDS","Save write failed");
 return ok;
}
namespace melonDS::Platform {
static FILE* fp(FileHandle*f){return reinterpret_cast<FILE*>(f);}
std::string GetLocalFilePath(const std::string&s){return state.directory+"/"+s;}
FileHandle* OpenFile(const std::string&p,FileMode m){
 int flags=(m&Write)?((m&Read)?O_RDWR:O_WRONLY):O_RDONLY;
 if(m&Write){if(!(m&NoCreate))flags|=O_CREAT;if(m&Append)flags|=O_APPEND;else if(!(m&Preserve))flags|=O_TRUNC;}
 int fd=open(p.c_str(),flags|O_CLOEXEC,0600);if(fd<0)return nullptr;
 FILE*f=fdopen(fd,(m&Write)?((m&Read)?"r+b":"wb"):"rb");if(!f)close(fd);return reinterpret_cast<FileHandle*>(f);
}
FileHandle* OpenLocalFile(const std::string&p,FileMode m){return OpenFile(GetLocalFilePath(p),m);}
bool FileExists(const std::string&p){return access(p.c_str(),F_OK)==0;}
bool LocalFileExists(const std::string&p){return FileExists(GetLocalFilePath(p));}
bool CloseFile(FileHandle*f){return f&&fclose(fp(f))==0;}
bool CheckFileWritable(const std::string&p){auto f=OpenFile(p,FileMode(Write|Preserve));return f&&CloseFile(f);}
bool CheckLocalFileWritable(const std::string&p){return CheckFileWritable(GetLocalFilePath(p));}
bool IsEndOfFile(FileHandle*f){return feof(fp(f));}
bool FileReadLine(char*s,int n,FileHandle*f){return fgets(s,n,fp(f));}
u64 FilePosition(FileHandle*f){return ftello(fp(f));}
bool FileSeek(FileHandle*f,s64 o,FileSeekOrigin w){return fseeko(fp(f),o,w==FileSeekOrigin::Start?SEEK_SET:w==FileSeekOrigin::Current?SEEK_CUR:SEEK_END)==0;}
void FileRewind(FileHandle*f){rewind(fp(f));}
u64 FileRead(void*d,u64 s,u64 n,FileHandle*f){return fread(d,s,n,fp(f));}
u64 FileWrite(const void*d,u64 s,u64 n,FileHandle*f){return fwrite(d,s,n,fp(f));}
bool FileFlush(FileHandle*f){return fflush(fp(f))==0;}
u64 FileLength(FileHandle*f){auto pos=ftello(fp(f));fseeko(fp(f),0,SEEK_END);auto len=ftello(fp(f));fseeko(fp(f),pos,SEEK_SET);return len<0?0:len;}
u64 FileWriteFormatted(FileHandle*f,const char*fmt,...){va_list a;va_start(a,fmt);int n=vfprintf(fp(f),fmt,a);va_end(a);return n<0?0:n;}
void Log(LogLevel l,const char*fmt,...){va_list a;va_start(a,fmt);__android_log_vprint(l==Error?ANDROID_LOG_ERROR:l==Warn?ANDROID_LOG_WARN:l==Debug?ANDROID_LOG_DEBUG:ANDROID_LOG_INFO,"BrumaNDS",fmt,a);va_end(a);}
void SignalStop(StopReason r,void*){state.stopReason=int(r);}
struct Thread{std::thread t;explicit Thread(std::function<void()>f):t(std::move(f)){} };
Thread* Thread_Create(std::function<void()>f){return new Thread(std::move(f));}
void Thread_Wait(Thread*t){if(t&&t->t.joinable())t->t.join();}
void Thread_Free(Thread*t){Thread_Wait(t);delete t;}
struct Semaphore{std::mutex m;std::condition_variable cv;int count=0;};
Semaphore* Semaphore_Create(){return new Semaphore;}
void Semaphore_Free(Semaphore*s){delete s;}
void Semaphore_Reset(Semaphore*s){std::lock_guard<std::mutex>l(s->m);s->count=0;}
void Semaphore_Wait(Semaphore*s){std::unique_lock<std::mutex>l(s->m);s->cv.wait(l,[&]{return s->count>0;});--s->count;}
bool Semaphore_TryWait(Semaphore*s,int ms){std::unique_lock<std::mutex>l(s->m);if(!s->cv.wait_for(l,std::chrono::milliseconds(ms),[&]{return s->count>0;}))return false;--s->count;return true;}
void Semaphore_Post(Semaphore*s,int n){std::lock_guard<std::mutex>l(s->m);s->count+=n;s->cv.notify_all();}
struct Mutex{std::mutex m;};
Mutex* Mutex_Create(){return new Mutex;}
void Mutex_Free(Mutex*m){delete m;}
void Mutex_Lock(Mutex*m){m->m.lock();}
void Mutex_Unlock(Mutex*m){m->m.unlock();}
bool Mutex_TryLock(Mutex*m){return m->m.try_lock();}
void Sleep(u64 us){std::this_thread::sleep_for(std::chrono::microseconds(us));}
u64 GetUSCount(){return std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now().time_since_epoch()).count();}
u64 GetMSCount(){return GetUSCount()/1000;}
void WriteNDSSave(const u8*d,u32 n,u32 offset,u32 length,void*){
 if(state.save.size()!=n||length>=n)state.save.assign(d,d+n);
 else if(n&&length){offset%=n;u32 first=std::min(length,n-offset);memcpy(state.save.data()+offset,d+offset,first);if(length>first)memcpy(state.save.data(),d,length-first);}
 state.dirty=true;
}
void WriteGBASave(const u8*,u32,u32,u32,void*){}
void WriteFirmware(const Firmware&,u32,u32,void*){}
void WriteDateTime(int,int,int,int,int,int,void*){}
void MP_Begin(void*){} void MP_End(void*){}
int MP_SendPacket(u8*,int,u64,void*){return 0;}
int MP_RecvPacket(u8*,u64*,void*){return 0;}
int MP_SendCmd(u8*,int,u64,void*){return 0;}
int MP_SendReply(u8*,int,u64,u16,void*){return 0;}
int MP_SendAck(u8*,int,u64,void*){return 0;}
int MP_RecvHostPacket(u8*,u64*,void*){return 0;}
u16 MP_RecvReplies(u8*,u64,u16,void*){return 0;}
int Net_SendPacket(u8*,int,void*){return 0;}
int Net_RecvPacket(u8*,void*){return 0;}
void Camera_Start(int,void*){} void Camera_Stop(int,void*){}
void Camera_CaptureFrame(int,u32*f,int w,int h,bool,void*){memset(f,0,w*h*4);}
void Mic_Start(void*){} void Mic_Stop(void*){}
int Mic_ReadInput(s16*d,int n,void*){memset(d,0,n*2);return n;}
AACDecoder* AAC_Init(){return nullptr;}
void AAC_DeInit(AACDecoder*){}
bool AAC_Configure(AACDecoder*,int,int){return false;}
bool AAC_DecodeFrame(AACDecoder*,const void*,int,void*,int){return false;}
bool Addon_KeyDown(KeyType,void*){return false;}
void Addon_RumbleStart(u32,void*){} void Addon_RumbleStop(void*){}
float Addon_MotionQuery(MotionQueryType,void*){return 0;}
DynamicLibrary* DynamicLibrary_Load(const char*s){return reinterpret_cast<DynamicLibrary*>(dlopen(s,RTLD_NOW|RTLD_LOCAL));}
void DynamicLibrary_Unload(DynamicLibrary*l){if(l)dlclose(l);}
void* DynamicLibrary_LoadFunction(DynamicLibrary*l,const char*s){return l?dlsym(l,s):nullptr;}
}
