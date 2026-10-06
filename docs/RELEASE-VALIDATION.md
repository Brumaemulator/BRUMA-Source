# Release validation and pre-existing Lint baseline

No feature/interface changes. One critical compatibility flag was added to the application manifest: `android:enableOnBackInvokedCallback="false"`. Android 16/API 36 enables predictive back by default and no longer dispatches onBackPressed/KEYCODE_BACK without migration or opt-out. This preserves the existing BRUMA pause/library navigation. Primary documentation: https://developer.android.com/about/versions/16/behavior-changes-16 .

The initial release bundle/APK were generated successfully; the additional full Lint task rejected 110 existing diagnostics. The three GestureBackNavigation diagnostics were addressed by that compatibility flag. A baseline records the other existing diagnostics; it does not globally disable Lint or new diagnostics:

- MissingPermission (47): SDL's optional direct Steam/BLE HID path. HIDDeviceManager.initializeBluetooth explicitly returns if BLUETOOTH_CONNECT/BLUETOOTH permission is absent. These permissions are not requested in BRUMA's manifest; ordinary controllers use Android InputDevice events. No new permission/Steam support is introduced.
- UnspecifiedRegisterReceiverFlag (2): USB code already uses RECEIVER_NOT_EXPORTED on API >=33 and the legacy overload only below API33. The Bluetooth filter contains platform protected ACL broadcasts and the whole direct BLE initialization is permission-gated off in this app.
- WrongConstant (2): the document-picker result flags are explicitly masked to READ/WRITE URI grant bits before passing them to takePersistableUriPermission. This is a dataflow limitation of the check.
- MissingLeanbackLauncher (1): BRUMA targets phone/tablet; optional leanback declaration is not an Android TV launcher implementation. No TV feature is introduced.
- MissingTranslation (55): existing strings fall back to the default language. Translation completeness is a known non-blocking limitation; no claim is made that every string is localized. No UI wording is changed for this release.

Warnings remain visible. The baseline should be reduced in later development. It is not proof of all-device runtime correctness, nor a substitute for Play pre-launch results.
