package com.hatkid.mkxpz;
/** The classic runtime is loaded alone, in its own Android process. */
public class ClassicActivity extends MainActivity {
 @Override protected String[] getLibraries(){return new String[]{"bruma_classic_sdl2","bruma_classic_openal","bruma_classic_fluidlite","bruma_classic"};}
 @Override protected String[] getArguments(){return new String[]{};}
}
