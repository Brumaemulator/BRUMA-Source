#include "binding.h"
#include "binding-util.h"

#include <ruby.h>

VALUE encodingInitialize (int argc, VALUE* argv, VALUE self)
{
    return Qnil;
}

void
encodingBindingInit()
{
#if RUBY_API_VERSION_MAJOR == 3
    VALUE encoding = rb_define_class("Encoding", rb_cObject);
    _rb_define_method(encoding, "initialize", encodingInitialize);

    rb_define_const(encoding, "UTF_8", rb_str_new_cstr("UTF-8"));
    rb_define_const(encoding, "US_ASCII", rb_str_new_cstr("US-ASCII"));
    rb_define_const(encoding, "SHIFT_JIS", rb_str_new_cstr("Shift_JIS"));

    rb_iv_set(encoding, "default_internal", rb_str_new_cstr("UTF-8"));
    rb_iv_set(encoding, "default_external", rb_str_new_cstr("UTF-8"));
#endif
}
