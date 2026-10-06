// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original BRUMA implementation of the existing runtime contract.
package com.hatkid.mkxpz.gamepad;
import android.view.KeyEvent;
/** Existing mappings, percentages and four-way touch default. */
public final class GamepadConfig {
    public Integer opacity=30,scale=100;
    public Boolean diagonalMovement=false;
    public final Integer keycodeA=KeyEvent.KEYCODE_Z,keycodeB=KeyEvent.KEYCODE_X,keycodeC=KeyEvent.KEYCODE_C;
    public final Integer keycodeX=KeyEvent.KEYCODE_A,keycodeY=KeyEvent.KEYCODE_S,keycodeZ=KeyEvent.KEYCODE_D;
    public final Integer keycodeL=KeyEvent.KEYCODE_Q,keycodeR=KeyEvent.KEYCODE_W;
    public final Integer keycodeCTRL=KeyEvent.KEYCODE_CTRL_LEFT,keycodeALT=KeyEvent.KEYCODE_ALT_LEFT,keycodeSHIFT=KeyEvent.KEYCODE_SHIFT_LEFT;
}
