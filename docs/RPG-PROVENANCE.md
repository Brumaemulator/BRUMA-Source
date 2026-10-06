# BRUMA — procedencia y sustitución de componentes, 2026-10-01

## Resultado y alcance

Se sustituyó la base Java/recursos de controles cuya autorización no se podía acreditar
por implementaciones originales de BRUMA. Se sustituyeron los tres archivos MiniFFI del
binding moderno por implementación original MIT compilada desde fuente. No se separaron
apps, módulos, UID ni procesos; la arquitectura anterior permanece intacta. Hay respaldo
previo en backup-before-phase1/. No se han relicenciado fuentes ajenas mediante etiquetas.

No equivale a certificar que el APK actual pueda distribuirse con frontend cerrado.
Sigue habiendo enlace JNI, clases y estado compartido de integración GPL dentro del APK.

## Wrapper original: origen identificado, permiso insuficiente

Origen: https://github.com/thehatkid/mkxp-z-android
Autor de los commits iniciales: Sasha Ermohin (cuenta thehatkid).
Introducción de Java: 55702d1b9688daeeeb7a1c04e7e84f0062241ddf.
Introducción de controles: 880a3ae53c01472ce9bcb83c390b238fdca89c77.
Historial posterior de MainActivity: b20539a3273b5b93c60e2e975b43bb9476323571,
e55b68bbab7466a4abe9da4b1e226ad50ecb0e43 y demás commits recogidos por archivo en
wrapper-provenance-before.json. El HEAD consultado fue 468fe8128a40cc7d2cba4ac7fbe21a82787de255.

BRUMA recibió la base desde https://github.com/BookerRues9/mkxp-z-android-reworked
en 0828373e0ad65402eced54e6b45645fceacf6bea. El fork importó el proyecto en
791ed9c76ae96b53a41954220189d7284fd66d63, autor Git Danit. Esto acredita trazabilidad
de snapshots, no que todos los autores concedan una misma licencia al wrapper.

No se encontró concesión explícita aplicable al Java/UI en raíz, cabeceras ni historial
de archivos legales consultado. COPYING en app/jni/mkxp-z acredita el motor; no basta
para inferir una licencia para archivos fuera de él. GitHub distingue acceso público de
permiso de redistribución: [documentación oficial](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository).
Conclusión: permiso original PENDIENTE DE VERIFICAR; se deja de utilizar esa base en
el APK actual, en lugar de atribuirle GPL/MIT sin autorización.

Antes de sustituir:
- Base upstream con modificaciones BRUMA: MainActivity.java; gamepad/Gamepad.java,
  GamepadButton.java, GamepadConfig.java; utils/ViewUtils.java, DirectionUtils.java;
  layout/gamepad_layout.xml, values/styleable.xml y drawables gamepad_*.xml.
- La cruceta GamepadDPad.java ya estaba reimplementada por BRUMA; RpgInput.java es
  código propio de fuentes de entrada. Se conserva el comportamiento de ambas.
- Nuevos respecto del upstream: ClassicActivity.java, ConfigJson.java,
  RpgImporter.java, RpgLibraryActivity.java. No se presentan como archivos ajenos.
- org/libsdl/app/*.java procede de SDL 2.30.4 (zlib), con adaptaciones BRUMA;
  no forma parte de la base Java sin licencia. Debe publicarse su fuente modificada
  al incluirlo en el runtime GPL, conservando los avisos SDL.

El JSON incluye hashes previos y diffs por archivo, incluidos recursos. Las coincidencias
de líneas son evidencia de comparación, no un porcentaje jurídico de autoría.

## Qué se sustituyó

MainActivity.java conserva el contrato JNI GAME_PATH/getSystemLanguage/vibración,
las bibliotecas seleccionadas, gestión SDL y mejoras propias de BRUMA de sesión,
teclado/Enter, opciones, pausa, pantalla completa y escalado. Se reconstruyó su base
desde el contrato funcional público y se conservaron las extensiones propias.
Gamepad.java/Config/Button usan composición programática y dibujo propios, manteniendo
cruceta/multitouch, mapeos, editor y preferencias. Los recursos geométricos se regeneraron;
no se garantiza equivalencia píxel a píxel. Esto no rediseña la pantalla principal.
Se eliminaron utilidades upstream ya sin consumidores; gamepad_layout queda como
recurso vacío de compatibilidad y los IDs se declaran en rpg_control_ids.xml.
Fuente nueva licenciada MIT en bruma-rpg-adapter-MIT.txt. La obra combinada con motor
sigue sometida a GPL: MIT individual no convierte el runtime en propietario.

Se leyó el upstream para la auditoría: no se afirma que hubiera un proceso formal de
clean room con equipos aislados. La evidencia técnica de sustitución no es una resolución
judicial sobre derivación; la validación jurídica final debe considerar los diffs concretos.

## MiniFFI

Las tres fuentes antiguas estaban fijadas por el commit padre y hashes, pero el comentario
de miniffi-binding.cpp reconoce adaptación de Win32API.c de Ruby 1.8. No se identificó
el patchlevel exacto. La alternativa BSD-2-Clause de Ruby 3.1 NO demuestra la licencia
de ese código Ruby 1.8; se corrigió la entrada histórica que hacía esa inferencia.
Ruby 1.8 tiene Ruby License/GPL-2.0-only. No se certifica aquí que elegir su licencia
personalizada resuelva automáticamente todas las contribuciones posteriores.

Solución aplicada: overrides/mkxp-z/binding/miniffi.h, miniffi.cpp,
miniffi-binding.cpp, originales MIT. stage-native.py los copia después de preparar
el árbol fijo; sources.lock y patches/manifest.json fijan hashes. La implementación
ARM64 conserva MiniFFI/Win32API, new/call/Call, parámetros N/I/L/P/B, salida V,
String/Array/nil, punteros a String mutables, hasta diez argumentos y ejecución
sin GVL. No añade ABI de coma flotante ni nuevas plataformas.
Las fuentes históricas quedan marcadas excluded/not packaged: no son la implementación
compilada, aunque el archivo upstream histórico siga conservado en privado.

## Pruebas y límites

Compilación debug, AndroidTest, release APK y AAB: correcta (final-build.log).
Instalación debug y APK de tests en el móvil: correcta.
Prueba aislada en ambos procesos reales: RGSS1 120 frames a 40 FPS, ~2,98 s,
Enter táctil, vuelta a biblioteca/pausa y reanudación con el mismo token: PASS.
Pruebas de cruceta/multitouch y fuentes de mando: PASS; son eventos automatizados,
no una validación física Xbox. MiniFFI: alias, cadena, array, puntero mutable,
llamada sin parámetros, booleano y aridad incorrecta: PASS en libmkxp-z.so nueva.
Se ejecutó Audio.bgm_play con WAV de prueba; no se afirma haber escuchado físicamente
el altavoz ni haber repetido una batería de juegos reales/guardados de usuario.
Los dos juegos de fixture se eliminan al terminar; no se modifican partidas del usuario.
La configuración global clásica se respalda y restaura durante la prueba.
device-test-final.log conserva el resultado; nada de esta prueba se empaqueta en release.

Los hashes de los otros inputs nativos no cambiaron (native-input-comparison.json).
MainActivity.kt principal, clasificador RPG, biblioteca, NdsActivity y NdsNative
son idénticos al snapshot privado previo (frontend-and-other-motors-comparison.json).
Solo libmkxp-z.so nativa cambia. No se presenta este rebuild del binding como una
nueva reconstrucción limpia de todas las dependencias desde cero.

## Fuentes y publicación

Phase-1-Sources.zip contiene la enmienda: adapter actual, overrides MIT, lock,
recetas y manifiesto de parches; NO es Corresponding Source completo. Hashes en
phase1-artifact-hashes.json. Los ZIPs antiguos de Corresponding Source corresponden
a binarios anteriores y contienen material histórico: no publicarlos como las fuentes
de esta build ni publicar backup-before-phase1/.

Las apps de pruebas antiguas BrumaModernRuntime/app y BrumaRpgRuntime/app aún contienen
su harness histórico de Java/recursos. No son las fuentes Java compiladas en BRUMA actual.
Antes de distribuir esas apps o reconstruirlas como proyectos públicos hay que sustituir
su harness por el adapter acreditado o excluirlas del paquete nativo. Esto queda pendiente
de empaquetado público y no se oculta como procedencia resuelta del harness antiguo.

Preparar la fuente completa exacta y revisar el alcance GPL sigue siendo requisito
de publicación; ninguna sustitución de este informe autoriza cerrar el APK combinado actual.

Comprobación adicional: los 22 IDs de recursos de controles conservan su valor numérico
respecto del APK anterior (control-resource-ids.json). En el APK release final todas las
librerías nativas excepto libmkxp-z.so son idénticas al candidato previo, incluido
liblinkcore/mGBA (packaged-native-comparison.json). Artefactos release siguen sin firma
de publicación; no confundir compilación correcta con autorización para distribuir.
