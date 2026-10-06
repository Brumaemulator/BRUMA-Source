# Ruby 1.9.3-p551 — cierre de licencia del runtime clásico

Fecha: 2026-10-02. Alcance: la copia exacta compilada de Ruby y sus adaptaciones para el runtime clásico. No es una nueva auditoría de los demás motores.

## Fuente exacta

Distribución oficial: https://cache.ruby-lang.org/pub/ruby/1.9/ruby-1.9.3-p551.tar.xz
Referencia oficial del SHA-256: https://www.ruby-lang.org/en/news/2014/11/13/ruby-1-9-3-p551-is-released/
SHA-256: `44228297861f4dfdf23a47372a3e3c4c5116fbf5b0e10883417f2379874b55c6`.
Se conserva el archivo original. La comparación de su contenido con las fuentes usadas solo encontró sustituciones de tool/config.sub y tool/config.guess por las copias públicas fijadas de SDL. Los archivos C de Ruby no se modificaron. Los adaptadores son archivos propios separados.

## Opción elegida

COPYING concede alternativamente la licencia BSD de dos cláusulas del archivo BSDL o las condiciones propias Ruby. Elegimos expresamente **BSD-2-Clause** para las partes cubiertas por esa concesión. No se elige GPLv2 ni es necesario sostener la compatibilidad de la licencia Ruby histórica de 1.8.7.
GNU clasifica BSD de dos cláusulas y BSD de tres cláusulas sin cláusula publicitaria como compatibles con GPL; la clasificación comprende GPLv3: https://www.gnu.org/licenses/license-list.html.html#FreeBSD .
La combinación con mkxp GPL-2.0-or-later y el trabajo BRUMA GPLv3 conserva las licencias de los componentes y se distribuye como trabajo combinado GPLv3. Esto no convierte todos los archivos de Ruby en GPLv3 ni elimina sus avisos BSD.

## Alcance de la inspección

69 miembros del archivo estático; 187 archivos fuente/cabecera/entrada generada identificados mediante dependencias del compilador y Ninja; cero errores de extracción de dependencias. Inventario y hashes: evidence/ruby193-build-inputs.json. Revisión de todos los grupos listados en LEGAL: licenses/ruby193/LEGAL-CHECKLIST.json. Se inspeccionaron COPYING, BSDL, LEGAL, GPL y cabeceras de las entradas compiladas, además de avisos que no aparecen en LEGAL.

| Parte compilada | Condiciones que permanecen |
| --- | --- |
| Core Ruby, Zlib binding, codificaciones/transcodificadores no excepcionales | Opción BSD-2-Clause de COPYING/BSDL |
| Oniguruma: include/ruby/oniguruma.h, regcomp.c, regenc.[ch], regerror.c, regexec.c, regint.h, regparse.[ch], enc/*.c aplicables | Avisos BSD-2-Clause propios, conservados completos |
| parse.c y parse.h oficiales | GNU Bison 2.5: GPL-3.0-or-later con excepción Bison 2.2. La excepción permite incorporar el skeleton al intérprete. Se conservan sus textos; LEGAL contiene una descripción histórica GPLv2+, pero prevalece la cabecera exacta del archivo usado |
| random.c | BSD-3-Clause del MT19937 además de las partes Ruby |
| util.c | Permiso Lucent dtoa conservando el aviso completo; BSD-2-Clause de David Schultz; partes Ruby BSD-2-Clause |
| missing/crypt.c y missing/setproctitle.c | BSD-3-Clause; no se utiliza la variante BSD con cláusula publicitaria |
| st.c, st.h y missing/memcmp.c | Partes originales de dominio público; modificaciones Ruby con la opción BSD-2-Clause |
| rational.c | Conservar íntegra la explicación del algoritmo autorizada por Bruno Haible. No se importa un intérprete CLISP ni código bajo GPLv2-only |
| configure | Permiso ilimitado explícito de copia, distribución y modificación |
| tool/config.guess y tool/config.sub usados | GNU GPL-3.0-or-later con excepción Autoconf; procedencia y hashes vinculados a SDL en sources.lock.json |
| ruby193-init.c, ruby193-probe.c y capa de compatibilidad propia | MIT, conservando avisos; modificaciones de archivos mkxp siguen GPL-2.0-or-later y se ofrecen con el conjunto GPLv3 |

LEGAL describe también Win32, win32ole, digest/md5, digest/rmd160, digest/sha2, nkf y fallbacks socket que **no se compilan en este binario ARM64**. Sus avisos y fuentes originales permanecen en la distribución Ruby incluida; no se afirma que todo el tarball sea BSD-2-Clause. Las herramientas de host tienen sus propias licencias y no se incrustan en el runtime.

## Obligaciones concretas y fuente correspondiente

Conservar COPYING, BSDL, LEGAL, las cabeceras y NOTICE.txt, incluida la explicación de rational.c, y GPL-3.0.txt. Reproducir avisos BSD en los materiales de la distribución binaria y hacerlos accesibles desde Licencias de código abierto. Ofrecer las fuentes completas del trabajo combinado GPLv3, no solo un enlace a Ruby upstream: copia exacta de Ruby, fuentes de mkxp y todas las dependencias enlazadas, adaptaciones, parches, archivos Gradle/CMake y scripts de configuración/compilación. Mantener las licencias originales de todas las dependencias. Excluir juegos, partidas, datos privados y claves de firma. El paquete de este runtime es un componente de Corresponding Source; no sustituye al paquete de toda BRUMA.

## Resultado

**Ruby 1.9.3-p551: licencia VERIFICADA para el conjunto exacto compilado, con la opción BSD-2-Clause y las excepciones documentadas. No se identifica un archivo compilado GPLv2-only ni otro conflicto de licencia de Ruby con GPLv3.**
El bloqueo de Ruby 1.8 desaparece de una versión distribuida solamente cuando su binario es sustituido por el nuevo y el paquete de fuentes/avisos se actualiza. Conservar Ruby 1.8 exclusivamente en backups privados no lo incorpora a la nueva distribución. El funcionamiento de la copia de prueba fue aceptado por el usuario; no se realizan nuevas pruebas de juego en esta fase.
