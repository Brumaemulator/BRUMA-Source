// This might cause some weird things but it should be better than an exception.

#include "binding.h"
#include "binding-util.h"
#include "sharedstate.h"
#include "../src/config.h"

#include <ruby.h>

void checkDebug()
{
    if(shState->config().debugMode)
        rb_raise(rb_eRuntimeError, "undefined method for nil:NilClass", 0);
}

VALUE intValue (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    return rb_fix_new(0);
}

VALUE floatValue (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    return rb_float_new(0);
}

VALUE stringValue (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    return rb_str_new_cstr("");
}

VALUE falseValue (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    return Qfalse;
}

VALUE trueValue (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    return Qtrue;
}

VALUE arrValue (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    return rb_ary_new();
}

VALUE nilValue (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    return Qnil;
}

VALUE argValue (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    VALUE arg;
    rb_scan_args(argc, argv, "1", &arg);
    
    return arg;
}

VALUE minusOne (int argc, VALUE* argv, VALUE self)
{
    checkDebug();
    
    return rb_fix_new(-1);
}


void nilClassBindingInit()
{
    VALUE nilClass = rb_const_get(rb_cObject, rb_intern("NilClass"));
        
    //Numeric methods
    _rb_define_method(nilClass, "%", intValue);
    _rb_define_method(nilClass, "*", intValue);
    _rb_define_method(nilClass, "**", intValue);
    _rb_define_method(nilClass, "+", argValue);
    _rb_define_method(nilClass, "-", intValue);
    _rb_define_method(nilClass, "/", intValue);
    _rb_define_method(nilClass, "<", trueValue);
    _rb_define_method(nilClass, "<<", intValue);
    _rb_define_method(nilClass, "<=", falseValue);
    _rb_define_method(nilClass, "<=>", intValue);
    _rb_define_method(nilClass, ">", falseValue);
    _rb_define_method(nilClass, ">=", falseValue);
    _rb_define_method(nilClass, ">>", intValue);
    _rb_define_method(nilClass, "abs", intValue);
    _rb_define_method(nilClass, "abs2", intValue);
    _rb_define_method(nilClass, "angle", intValue);
    _rb_define_method(nilClass, "arg", intValue);
    _rb_define_method(nilClass, "ceil", intValue);
    _rb_define_method(nilClass, "chr", stringValue);
    _rb_define_method(nilClass, "coerce", arrValue);
    _rb_define_method(nilClass, "conj", intValue);
    _rb_define_method(nilClass, "conjugate", intValue);
    _rb_define_method(nilClass, "denominator", intValue);
    _rb_define_method(nilClass, "div", intValue);
    _rb_define_method(nilClass, "divmod", arrValue);
    _rb_define_method(nilClass, "downto", intValue);
    _rb_define_method(nilClass, "even?", trueValue);
    _rb_define_method(nilClass, "fdiv", floatValue);
    _rb_define_method(nilClass, "floor", intValue);
    _rb_define_method(nilClass, "finite?", trueValue);
    _rb_define_method(nilClass, "hash", intValue);
    _rb_define_method(nilClass, "imag", intValue);
    _rb_define_method(nilClass, "imaginary", intValue);
    _rb_define_method(nilClass, "infinite?", trueValue);
    _rb_define_method(nilClass, "integer?", falseValue);
    _rb_define_method(nilClass, "modulo", intValue);
    _rb_define_method(nilClass, "nan?", trueValue);
    _rb_define_method(nilClass, "next", intValue);
    _rb_define_method(nilClass, "nonzero?", nilValue);
    _rb_define_method(nilClass, "odd?", falseValue);
    _rb_define_method(nilClass, "ord", intValue);
    _rb_define_method(nilClass, "quo", floatValue);
    _rb_define_method(nilClass, "phase", intValue);
    _rb_define_method(nilClass, "pred", intValue);
    _rb_define_method(nilClass, "real", intValue);
    _rb_define_method(nilClass, "real?", falseValue);
    _rb_define_method(nilClass, "remainder", intValue);
    _rb_define_method(nilClass, "round", intValue);
    _rb_define_method(nilClass, "step", intValue);
    _rb_define_method(nilClass, "succ", intValue);
    _rb_define_method(nilClass, "times", intValue);
    _rb_define_method(nilClass, "to_int", intValue);
    _rb_define_method(nilClass, "truncate", intValue);
    _rb_define_method(nilClass, "to_i", intValue);
    _rb_define_method(nilClass, "upto", intValue);
    _rb_define_method(nilClass, "zero?", trueValue);
    _rb_define_method(nilClass, "|", intValue);
    
    //String methods
    _rb_define_method(nilClass, "ascii_only?", trueValue);
    _rb_define_method(nilClass, "bytes", stringValue);
    _rb_define_method(nilClass, "bytesize", intValue);
    _rb_define_method(nilClass, "capitalize", stringValue);
    _rb_define_method(nilClass, "capitalize!", nilValue);
    _rb_define_method(nilClass, "casecmp", minusOne);
    _rb_define_method(nilClass, "center", stringValue);
    _rb_define_method(nilClass, "chars", stringValue);
    _rb_define_method(nilClass, "chomp", stringValue);
    _rb_define_method(nilClass, "chomp!", stringValue);
    _rb_define_method(nilClass, "chop", stringValue);
    _rb_define_method(nilClass, "chop!", stringValue);
    _rb_define_method(nilClass, "chr", stringValue);
    _rb_define_method(nilClass, "clear", stringValue);
    _rb_define_method(nilClass, "codepoints", stringValue);
    _rb_define_method(nilClass, "concat", stringValue);
    _rb_define_method(nilClass, "count", intValue);
    _rb_define_method(nilClass, "crypt", stringValue);
    _rb_define_method(nilClass, "delete", stringValue);
    _rb_define_method(nilClass, "delete!", stringValue);
    _rb_define_method(nilClass, "downcase", stringValue);
    _rb_define_method(nilClass, "downcase!", stringValue);
    _rb_define_method(nilClass, "dump", stringValue);
    _rb_define_method(nilClass, "each", stringValue);
    _rb_define_method(nilClass, "each_byte", stringValue);
    _rb_define_method(nilClass, "each_char", stringValue);
    _rb_define_method(nilClass, "each_codepoint", stringValue);
    _rb_define_method(nilClass, "each_line", stringValue);
    _rb_define_method(nilClass, "empty?", trueValue);
    _rb_define_method(nilClass, "encode", stringValue);
    _rb_define_method(nilClass, "encode!", stringValue);
    _rb_define_method(nilClass, "end_with?", falseValue);
    _rb_define_method(nilClass, "force_encoding", stringValue);
    _rb_define_method(nilClass, "getbyte", intValue);
    _rb_define_method(nilClass, "gsub", stringValue);
    _rb_define_method(nilClass, "gsub!", stringValue);
    _rb_define_method(nilClass, "hex", intValue);
    _rb_define_method(nilClass, "include?", falseValue);
    _rb_define_method(nilClass, "index", nilValue);
    _rb_define_method(nilClass, "insert", stringValue);
    _rb_define_method(nilClass, "inspect", stringValue);
    _rb_define_method(nilClass, "length", intValue);
    _rb_define_method(nilClass, "lines", stringValue);
    _rb_define_method(nilClass, "ljust", stringValue);
    _rb_define_method(nilClass, "lstrip", stringValue);
    _rb_define_method(nilClass, "lstrip!", stringValue);
    _rb_define_method(nilClass, "match", nilValue);
    _rb_define_method(nilClass, "next", stringValue);
    _rb_define_method(nilClass, "next!", stringValue);
    _rb_define_method(nilClass, "oct", intValue);
    _rb_define_method(nilClass, "partition", arrValue);
    _rb_define_method(nilClass, "replace", stringValue);
    _rb_define_method(nilClass, "reverse", stringValue);
    _rb_define_method(nilClass, "reverse!", stringValue);
    _rb_define_method(nilClass, "rindex", nilValue);
    _rb_define_method(nilClass, "rjust", stringValue);
    _rb_define_method(nilClass, "rpartition", arrValue);
    _rb_define_method(nilClass, "rstrip", stringValue);
    _rb_define_method(nilClass, "rstrip!", stringValue);
    _rb_define_method(nilClass, "scan", arrValue);
    _rb_define_method(nilClass, "setbyte", intValue);
    _rb_define_method(nilClass, "size", intValue);
    _rb_define_method(nilClass, "slice", stringValue);
    _rb_define_method(nilClass, "slice!", stringValue);
    _rb_define_method(nilClass, "split", arrValue);
    _rb_define_method(nilClass, "squeeze", stringValue);
    _rb_define_method(nilClass, "squeeze", stringValue);
    _rb_define_method(nilClass, "start_with?", falseValue);
    _rb_define_method(nilClass, "strip", stringValue);
    _rb_define_method(nilClass, "strip!", stringValue);
    _rb_define_method(nilClass, "sub", stringValue);
    _rb_define_method(nilClass, "sub!", stringValue);
    _rb_define_method(nilClass, "succ", stringValue);
    _rb_define_method(nilClass, "succ!", stringValue);
    _rb_define_method(nilClass, "sum", intValue);
    _rb_define_method(nilClass, "swapcase", stringValue);
    _rb_define_method(nilClass, "swapcase!", stringValue);
    _rb_define_method(nilClass, "tr", stringValue);
    _rb_define_method(nilClass, "tr!", stringValue);
    _rb_define_method(nilClass, "tr_s", stringValue);
    _rb_define_method(nilClass, "tr_s!", stringValue);
    _rb_define_method(nilClass, "unpack", arrValue);
    _rb_define_method(nilClass, "upcase", stringValue);
    _rb_define_method(nilClass, "upcase!", stringValue);
    _rb_define_method(nilClass, "valid_encoding?", trueValue);
    _rb_define_method(nilClass, "to_str", stringValue);
    _rb_define_method(nilClass, "to_s", stringValue);
    
    _rb_define_method(nilClass, "to_ary", arrValue);
}

