// BRUMA translation pilot. Own JNI glue; xdelta3 remains Apache-2.0.
#include <jni.h>
#include <cstdint>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
extern "C" {
#include "xdelta3.h"
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_linkcore_emulator_translations_XdeltaNative_decode(JNIEnv* e,jobject,jstring from,jbyteArray patch,jstring to,jlong capacity) {
 if(capacity<1 || capacity>512LL*1024*1024)return -1;
 const char* a=e->GetStringUTFChars(from,nullptr);const char* b=e->GetStringUTFChars(to,nullptr);
 int in=open(a,O_RDONLY|O_CLOEXEC),out=open(b,O_RDWR|O_CREAT|O_EXCL|O_CLOEXEC,0600);
 e->ReleaseStringUTFChars(from,a);e->ReleaseStringUTFChars(to,b);
 struct stat st{};void* src=MAP_FAILED;void* dst=MAP_FAILED;jbyte* bytes=nullptr;jlong result=-1;
 if(in>=0 && out>=0 && !fstat(in,&st) && st.st_size>0 && st.st_size<=512LL*1024*1024 && !ftruncate(out,capacity)) {
  src=mmap(nullptr,st.st_size,PROT_READ,MAP_PRIVATE,in,0);dst=mmap(nullptr,capacity,PROT_READ|PROT_WRITE,MAP_SHARED,out,0);
  if(src!=MAP_FAILED && dst!=MAP_FAILED) {
   bytes=e->GetByteArrayElements(patch,nullptr);
   if(bytes){usize_t size=0;int rc=xd3_decode_memory(reinterpret_cast<uint8_t*>(bytes),e->GetArrayLength(patch),reinterpret_cast<uint8_t*>(src),st.st_size,reinterpret_cast<uint8_t*>(dst),&size,capacity,0);
    if(!rc && size==static_cast<usize_t>(capacity) && !msync(dst,capacity,MS_SYNC))result=size;
   }
  }
 }
 if(bytes)e->ReleaseByteArrayElements(patch,bytes,JNI_ABORT);
 if(src!=MAP_FAILED)munmap(src,st.st_size);if(dst!=MAP_FAILED)munmap(dst,capacity);
 if(in>=0)close(in);if(out>=0)close(out);return result;
}
