/*
** graphics-binding.cpp
**
** This file is part of mkxp.
**
** Copyright (C) 2013 Jonas Kulla <Nyocurio@gmail.com>
**
** mkxp is free software: you can redistribute it and/or modify
** it under the terms of the GNU General Public License as published by
** the Free Software Foundation, either version 2 of the License, or
** (at your option) any later version.
**
** mkxp is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
** GNU General Public License for more details.
**
** You should have received a copy of the GNU General Public License
** along with mkxp.  If not, see <http://www.gnu.org/licenses/>.
*/

#include "graphics.h"
#include "sharedstate.h"
#include "filesystem.h"
#include "binding-util.h"
#include "binding-types.h"
#include "exception.h"

RB_METHOD(graphicsDelta) {
    RB_UNUSED_PARAM;
    
    return ULL2NUM(shState->graphics().getDelta());
}

RB_METHOD(graphicsUpdate)
{
	RB_UNUSED_PARAM;

	shState->graphics().update();

	return Qnil;
}

RB_METHOD(graphicsAverageFrameRate)
{
    RB_UNUSED_PARAM;
    
    return rb_float_new(shState->graphics().averageFrameRate());
}

RB_METHOD(graphicsFreeze)
{
	RB_UNUSED_PARAM;

	shState->graphics().freeze();

	return Qnil;
}

RB_METHOD(graphicsTransition)
{
	RB_UNUSED_PARAM;

	int duration = 8;
	const char *filename = "";
	int vague = 40;

	rb_get_args(argc, argv, "|izi", &duration, &filename, &vague RB_ARG_END);

	GUARD_EXC( shState->graphics().transition(duration, filename, vague); )

	return Qnil;
}

RB_METHOD(graphicsFrameReset)
{
	RB_UNUSED_PARAM;

	shState->graphics().frameReset();

	return Qnil;
}

#define DEF_GRA_PROP_I(PropName) \
	RB_METHOD(graphics##Get##PropName) \
	{ \
		RB_UNUSED_PARAM; \
		return rb_fix_new(shState->graphics().get##PropName()); \
	} \
	RB_METHOD(graphics##Set##PropName) \
	{ \
		RB_UNUSED_PARAM; \
		int value; \
		rb_get_args(argc, argv, "i", &value RB_ARG_END); \
		shState->graphics().set##PropName(value); \
		return rb_fix_new(value); \
	}
    
    
#define DEF_GRA_PROP_F(PropName) \
	RB_METHOD(graphics##Get##PropName) \
	{ \
		RB_UNUSED_PARAM; \
		return rb_float_new(shState->graphics().get##PropName()); \
	} \
	RB_METHOD(graphics##Set##PropName) \
	{ \
		RB_UNUSED_PARAM; \
		double value; \
		rb_get_args(argc, argv, "f", &value RB_ARG_END); \
		shState->graphics().set##PropName(value); \
		return rb_float_new(value); \
	}

#define DEF_GRA_PROP_B(PropName) \
	RB_METHOD(graphics##Get##PropName) \
	{ \
		RB_UNUSED_PARAM; \
		return rb_bool_new(shState->graphics().get##PropName()); \
	} \
	RB_METHOD(graphics##Set##PropName) \
	{ \
		RB_UNUSED_PARAM; \
		bool value; \
		rb_get_args(argc, argv, "b", &value RB_ARG_END); \
		shState->graphics().set##PropName(value); \
		return rb_bool_new(value); \
	}

RB_METHOD(graphicsWidth)
{
	RB_UNUSED_PARAM;

	return rb_fix_new(shState->graphics().width());
}

RB_METHOD(graphicsHeight)
{
	RB_UNUSED_PARAM;

	return rb_fix_new(shState->graphics().height());
}

RB_METHOD(graphicsSize)
{
	RB_UNUSED_PARAM;
    
    VALUE arr = rb_ary_new();
    rb_ary_push(arr, rb_fix_new(shState->graphics().width()));
    rb_ary_push(arr, rb_fix_new(shState->graphics().height()));
	return arr;
}

RB_METHOD(graphicsWait)
{
	RB_UNUSED_PARAM;

	int duration;
	rb_get_args(argc, argv, "i", &duration RB_ARG_END);

	shState->graphics().wait(duration);

	return Qnil;
}

RB_METHOD(graphicsFadeout)
{
	RB_UNUSED_PARAM;

	int duration;
	rb_get_args(argc, argv, "i", &duration RB_ARG_END);

	shState->graphics().fadeout(duration);

	return Qnil;
}

RB_METHOD(graphicsFadein)
{
	RB_UNUSED_PARAM;

	int duration;
	rb_get_args(argc, argv, "i", &duration RB_ARG_END);

	shState->graphics().fadein(duration);

	return Qnil;
}

void bitmapInitProps(Bitmap *b, VALUE self);

RB_METHOD(graphicsSnapToBitmap)
{
	RB_UNUSED_PARAM;

	Bitmap *result = 0;
	GUARD_EXC( result = shState->graphics().snapToBitmap(); );

	VALUE obj = wrapObject(result, BitmapType);
	bitmapInitProps(result, obj);

	return obj;
}

RB_METHOD(graphicsResizeScreen)
{
	RB_UNUSED_PARAM;

	int width, height;
	rb_get_args(argc, argv, "ii", &width, &height RB_ARG_END);

	shState->graphics().resizeScreen(width, height);

	return Qnil;
}

RB_METHOD(graphicsReset)
{
	RB_UNUSED_PARAM;

	shState->graphics().reset();

	return Qnil;
}

RB_METHOD(graphicsCenter)
{
    RB_UNUSED_PARAM;
    
    return Qnil;
}

typedef struct {
    const char *filename;
    int volume;
    bool skippable;
} PlayMovieArgs;

void *playMovieInternal(void *args) {
    PlayMovieArgs *a = (PlayMovieArgs*)args;
    GUARD_EXC(
                  shState->graphics().playMovie(a->filename, a->volume, a->skippable);

                  // Signals for shutdown or reset only make playMovie quit early,
                  // so check again
                  shState->graphics().update();
                  );
    return 0;
}

RB_METHOD(graphicsPlayMovie)
{
	RB_UNUSED_PARAM;

    VALUE filename, volumeArg, skippable;
    rb_scan_args(argc, argv, "12", &filename, &volumeArg, &skippable);
    SafeStringValue(filename);


    bool skip;
    rb_bool_arg(skippable, &skip);
    PlayMovieArgs args{};
    args.filename = RSTRING_PTR(filename);
    args.volume = (volumeArg == Qnil) ? 100 : NUM2INT(volumeArg);;
    args.skippable = skip;
#if RAPI_MAJOR >= 2
    rb_thread_call_without_gvl(playMovieInternal, &args, 0, 0);
#else
    playMovieInternal(&args);
#endif
	return Qnil;
}

DEF_GRA_PROP_I(FrameRate)
DEF_GRA_PROP_I(FrameCount)
DEF_GRA_PROP_I(Brightness)

DEF_GRA_PROP_B(Fullscreen)
DEF_GRA_PROP_B(ShowCursor)
DEF_GRA_PROP_F(Scale)

#define INIT_GRA_PROP_BIND(PropName, prop_name_s) \
{ \
	_rb_define_module_function(module, prop_name_s, graphics##Get##PropName); \
	_rb_define_module_function(module, prop_name_s "=", graphics##Set##PropName); \
}

RB_METHOD(graphicsScreenshot)
{
    RB_UNUSED_PARAM;

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
    return Qnil;
}

/* Dummy method for resize_window */
RB_METHOD(graphicsResizeWindow)
{
	RB_UNUSED_PARAM;

	return Qnil;
}

RB_METHOD(graphicsDisableFastForward)
{
	RB_UNUSED_PARAM;

	shState->graphics().disableFastForward();

	return Qnil;
}

void graphicsBindingInit()
{
	VALUE module = rb_define_module("Graphics");

	_rb_define_module_function(module, "delta", graphicsDelta);
	_rb_define_module_function(module, "update", graphicsUpdate);
	_rb_define_module_function(module, "freeze", graphicsFreeze);
	_rb_define_module_function(module, "transition", graphicsTransition);
	_rb_define_module_function(module, "frame_reset", graphicsFrameReset);
    _rb_define_module_function(module, "screenshot", graphicsScreenshot);
	_rb_define_module_function(module, "__reset__", graphicsReset);
    _rb_define_module_function(module, "size", graphicsSize);
	_rb_define_module_function(module, "resize_window", graphicsResizeWindow);

	INIT_GRA_PROP_BIND( FrameRate,  "frame_rate"  );
	INIT_GRA_PROP_BIND( FrameCount, "frame_count" );
	_rb_define_module_function(module, "average_frame_rate", graphicsAverageFrameRate);

	//Needed for Pokemon Essential v18
	_rb_define_module_function(module, "resize_screen", graphicsResizeScreen);
	_rb_define_module_function(module, "center", graphicsCenter);
	_rb_define_module_function(module, "snap_to_bitmap", graphicsSnapToBitmap);

#if RUBY_API_VERSION_MAJOR != 3
	if (rgssVer >= 2)
	{
#endif
	_rb_define_module_function(module, "width", graphicsWidth);
	_rb_define_module_function(module, "height", graphicsHeight);
	_rb_define_module_function(module, "wait", graphicsWait);
	_rb_define_module_function(module, "fadeout", graphicsFadeout);
	_rb_define_module_function(module, "fadein", graphicsFadein);
    _rb_define_module_function(module, "disable_fast_forward", graphicsDisableFastForward);
    
    INIT_GRA_PROP_BIND( Brightness, "brightness" );
#if RUBY_API_VERSION_MAJOR != 3
	}

	if (rgssVer >= 3)
	{
#endif
	_rb_define_module_function(module, "play_movie", graphicsPlayMovie);
    _rb_define_module_function(module, "zeus_play_movie", graphicsPlayMovie);
#if RUBY_API_VERSION_MAJOR != 3
	}
#endif

	//Bindings for Pokemon Essential
    _rb_define_module_function(module, "poke_wait", graphicsWait);
	_rb_define_module_function(module, "poke_fadeout", graphicsFadeout);
	_rb_define_module_function(module, "poke_fadein", graphicsFadein);
    _rb_define_module_function(module, "poke_width", graphicsWidth);
	_rb_define_module_function(module, "poke_height", graphicsHeight);
    _rb_define_module_function(module, "poke_snap_to_bitmap", graphicsSnapToBitmap);
	_rb_define_module_function(module, "mkxp_snap_to_bitmap", graphicsSnapToBitmap);
    _rb_define_module_function(module, "poke_resize_screen", graphicsResizeScreen);
    _rb_define_module_function(module, "poke_center", graphicsCenter);
	
    INIT_GRA_PROP_BIND( Brightness, "poke_brightness" );
	
	INIT_GRA_PROP_BIND( Fullscreen, "fullscreen"  );
	INIT_GRA_PROP_BIND( ShowCursor, "show_cursor" );
    INIT_GRA_PROP_BIND( Scale,      "scale"       );
}
