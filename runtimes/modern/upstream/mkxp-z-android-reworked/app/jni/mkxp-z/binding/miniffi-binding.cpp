// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original implementation from the documented BRUMA MiniFFI Ruby API contract.
// No Ruby 1.8 Win32API.c implementation is included in this file.
#include <ruby.h>
#include <ruby/thread.h>
#include <SDL.h>
#include "miniffi.h"

namespace {
enum Kind { Nothing=0, Word=1, Pointer=2, Integer32=3, Boolean=4 };
int kind(char code) {
    switch(code){
        case 'N':case 'n':case 'L':case 'l':return Word;
        case 'P':case 'p':return Pointer;
        case 'I':case 'i':return Integer32;
        case 'B':case 'b':return Boolean;
        default:return Nothing;
    }
}
void unload(void* handle){if(handle)SDL_UnloadObject(handle);}
VALUE allocate(VALUE klass){return Data_Wrap_Struct(klass,nullptr,unload,nullptr);}
void addType(VALUE list,VALUE text){
    StringValue(text);
    int selected=RSTRING_LEN(text)>0?kind(RSTRING_PTR(text)[0]):Nothing;
    if(selected!=Nothing)rb_ary_push(list,INT2FIX(selected));
}
VALUE initialize(int argc,VALUE* argv,VALUE self){
    VALUE library,name,inputs,output;
    rb_scan_args(argc,argv,"22",&library,&name,&inputs,&output);
    StringValue(library);StringValue(name);
    void* handle=SDL_LoadObject(StringValueCStr(library));
    if(!handle)rb_raise(rb_eRuntimeError,"%s",SDL_GetError());
    if(DATA_PTR(self))unload(DATA_PTR(self));DATA_PTR(self)=handle;
    void* address=SDL_LoadFunction(handle,StringValueCStr(name));
    if(!address)rb_raise(rb_eRuntimeError,"%s",SDL_GetError());
    VALUE types=rb_ary_new();
    if(RB_TYPE_P(inputs,T_ARRAY)){
        for(long i=0;i<RARRAY_LEN(inputs);++i)addType(types,rb_ary_entry(inputs,i));
    }else if(!NIL_P(inputs)){
        StringValue(inputs);
        for(long i=0;i<RSTRING_LEN(inputs);++i){int type=kind(RSTRING_PTR(inputs)[i]);if(type)rb_ary_push(types,INT2FIX(type));}
    }
    if(RARRAY_LEN(types)>MINIFFI_MAX_ARGS)rb_raise(rb_eRuntimeError,"too many parameters: %ld/%ld\n",RARRAY_LEN(types),MINIFFI_MAX_ARGS);
    int resultType=Nothing;
    if(!NIL_P(output)){StringValue(output);if(RSTRING_LEN(output)>0)resultType=kind(RSTRING_PTR(output)[0]);}
    rb_iv_set(self,"_func",ULONG2NUM(reinterpret_cast<mffi_value>(address)));
    rb_iv_set(self,"_funcname",name);rb_iv_set(self,"_libname",library);
    rb_iv_set(self,"_imports",types);rb_iv_set(self,"_exports",INT2FIX(resultType));
    if(rb_block_given_p())rb_yield(self);
    return Qnil;
}
struct Invocation {MINIFFI_FUNC function;MiniFFIFuncArgs values;int count;};
void* outsideRuby(void* data){auto* call=static_cast<Invocation*>(data);return reinterpret_cast<void*>(miniffi_call_intern(call->function,&call->values,call->count));}
VALUE invoke(int argc,VALUE* argv,VALUE self){
    VALUE imports=rb_iv_get(self,"_imports");
    int required=static_cast<int>(RARRAY_LEN(imports));
    if(argc!=required)rb_raise(rb_eRuntimeError,"wrong number of parameters: expected %d, got %d",required,argc);
    Invocation request{};
    request.function=reinterpret_cast<MINIFFI_FUNC>(NUM2ULONG(rb_iv_get(self,"_func")));request.count=required;
    for(int i=0;i<required;++i){
        VALUE value=argv[i];int type=FIX2INT(rb_ary_entry(imports,i));mffi_value converted=0;
        if(type==Pointer){
            if(!NIL_P(value)){
                if(FIXNUM_P(value))converted=NUM2ULONG(value);
                else{StringValue(value);rb_str_modify(value);argv[i]=value;converted=reinterpret_cast<mffi_value>(RSTRING_PTR(value));}
            }
        }else if(type==Boolean){
            if(value!=Qtrue&&value!=Qfalse&&value!=Qnil)rb_raise(rb_eTypeError,"Argument 0: Expected bool");
            converted=value==Qtrue?1:0;
        }else{
            converted=NUM2ULONG(value);if(type==Integer32)converted&=UINT32_MAX;
        }
        request.values.params[i]=converted;
    }
    mffi_value result=reinterpret_cast<mffi_value>(rb_thread_call_without_gvl(outsideRuby,&request,nullptr,nullptr));
    switch(FIX2INT(rb_iv_get(self,"_exports"))){
        case Word:case Integer32:return ULONG2NUM(result);
        case Pointer:return rb_utf8_str_new_cstr(reinterpret_cast<const char*>(result));
        case Boolean:return result?Qtrue:Qfalse;
        default:return ULONG2NUM(0);
    }
}
}
void MiniFFIBindingInit(){
    VALUE klass=rb_define_class("MiniFFI",rb_cObject);
    rb_define_alloc_func(klass,allocate);
    rb_define_method(klass,"initialize",RUBY_METHOD_FUNC(initialize),-1);
    rb_define_method(klass,"call",RUBY_METHOD_FUNC(invoke),-1);
    rb_define_alias(klass,"Call","call");
    rb_define_const(rb_cObject,"Win32API",klass);
}
