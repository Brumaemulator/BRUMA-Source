#include "binding.h"
#include "binding-util.h"
#include "http.h"

#include <ruby.h>
#include <string>
#include <map>

VALUE httpGet (int argc, VALUE* argv, VALUE self)
{
	VALUE hostValue, queryValue;
    rb_scan_args(argc, argv, "2", &hostValue, &queryValue);

    std::string host = StringValueCStr(hostValue);
    std::string query = StringValueCStr(queryValue);


    std::string responseBody = HTTP::get(host, query);

    return rb_str_new_cstr(responseBody.c_str());
}

VALUE httpPost (int argc, VALUE* argv, VALUE self)
{
	VALUE hostValue, queryValue, requestValue;
    rb_scan_args(argc, argv, "3", &hostValue, &queryValue, &requestValue);

    std::string host = StringValueCStr(hostValue);
    std::string query = StringValueCStr(queryValue);

    std::map<std::string, std::string> requestBody;

    VALUE keys = rb_funcall(requestValue, rb_intern("keys"), 0);
    VALUE keyCount = rb_funcall(keys, rb_intern("size"), 0);

    for(long i = 0; i < NUM2LONG(keyCount); i++)
    {
    	VALUE key = rb_ary_entry(keys, i);
    	VALUE value = rb_hash_aref(requestValue, key);

    	requestBody[StringValueCStr(key)] = StringValueCStr(value);
    }

    std::string responseBody = HTTP::post(host, query, requestBody);

    return (rb_str_new_cstr(responseBody.c_str()));
}

VALUE httpDownload (int argc, VALUE* argv, VALUE self)
{
	VALUE hostValue, queryValue, pathValue, onProgress;
    rb_scan_args(argc, argv, "31", &hostValue, &queryValue, &pathValue, &onProgress);

    std::string host = StringValueCStr(hostValue);
    std::string query = StringValueCStr(queryValue);
    std::string path = StringValueCStr(pathValue);
    std::string methodName = StringValueCStr(onProgress);

    int status = HTTP::download(
        host,
        query,
        path,
        [&self, &methodName](long current, long total){
            rb_funcall(self, rb_intern(methodName.c_str()), 2, LONG2NUM(current), LONG2NUM(total));
        }
    );

    return INT2NUM(status);
}

void
httpBindingInit()
{
    VALUE http = rb_define_module("HTTP");
    _rb_define_module_function(http, "get", httpGet);
    _rb_define_module_function(http, "post", httpPost);
    _rb_define_module_function(http, "download", httpDownload);
}