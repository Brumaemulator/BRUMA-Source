package com.linkcore.emulator.translations

/** Separate family: never treat an RPG folder as a ROM or execute downloaded scripts. */
data class RpgTranslationManifest(val engine:String,val gameManifestSha256:String,val files:List<RpgTranslationFile>)
data class RpgTranslationFile(val relativePath:String,val originalSha256:String,val translatedSha256:String,val patchType:String)
interface RpgTranslationAdapter {
    /** Future opt-in implementation must validate canonical paths, all file hashes and saves exclusions. */
    fun validate(manifest:RpgTranslationManifest):Boolean
}
