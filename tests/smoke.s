// Original LinkCore test ROM. No game or BIOS assets. CC0-1.0.
.syntax unified
.arm
.section .text
.global _start
_start:
    b boot
    .space 156
    .ascii "LINKCORETEST"
    .ascii "TEST"
    .ascii "00"
    .byte 0x96, 0, 0
    .space 7
    .byte 0, 0
    .space 2
boot:
    mov r0, #0x04000000
    ldr r1, =0x0403
    strh r1, [r0]
    mov r1, #0x80
    strh r1, [r0, #0x84]
    ldr r1, =0x2277
    strh r1, [r0, #0x80]
    mov r1, #2
    strh r1, [r0, #0x82]
    ldr r1, =0xf080
    strh r1, [r0, #0x68]
    ldr r1, =0x86d6
    strh r1, [r0, #0x6c]
    mov r7, #0x0e000000
    ldrb r1, [r7]
    mov r8, #31
    cmp r1, #0x42
    ldreq r8, =0x03ff
    mov r1, #0x42
    strb r1, [r7]
loop:
    mov r2, r8
    ldr r1, =0x04000130
    ldrh r1, [r1]
    tst r1, #1
    ldreq r2, =0x03e0
    tst r1, #2
    ldreq r2, =0x7c00
    tst r1, #16
    ldreq r2, =0x7fff
    mov r3, #0x06000000
    ldr r4, =38400
fill:
    strh r2, [r3], #2
    subs r4, r4, #1
    bne fill
    b loop
    .ltorg
    .asciz "SRAM_V113"
