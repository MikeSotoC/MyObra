# Auditoría completa del proyecto MyObra

**Fecha:** 2026-04-10  
**Auditoría realizada por:** Agente Codex

## 1) Alcance y metodología

Se realizó una auditoría estática (sin ejecución de la app) sobre:

- Configuración de build y dependencias (`build.gradle.kts`, `app/build.gradle.kts`).
- Seguridad de aplicación Android (`AndroidManifest.xml`).
- Arquitectura y persistencia (ViewModel + Room + repositorios).
- Robustez de código Kotlin (null-safety, manejo de errores, mantenibilidad).
- Riesgos operativos para CI/CD y publicación.

> Nota: no fue posible correr tareas de Gradle porque el repositorio no incluye el script `gradlew`.

## 2) Resumen ejecutivo

Estado general: **Funcional, pero con deuda técnica relevante**.

Hallazgos principales:

1. **Crítico**: Duplicación completa de bloques `android {}` y `dependencies {}` en `app/build.gradle.kts`, lo que complica mantenimiento y puede generar inconsistencias de build.
2. **Alto**: Falta de `gradlew` (wrapper), impide builds reproducibles y validación automática en entornos limpios.
3. **Medio**: Uso extendido de `!!` en fragments y flujos de UI, elevando riesgo de `NullPointerException` en tiempo de ejecución.
4. **Medio**: `allowBackup=true` habilitado en manifiesto; revisar requerimientos de privacidad y cumplimiento antes de producción.
5. **Medio**: Generación de PDF en una sola página sin paginación dinámica; posible truncamiento de contenido en presupuestos largos.

## 3) Hallazgos detallados

## 3.1 Crítico

### C1. Duplicación de configuración Gradle en módulo app
**Evidencia:** `app/build.gradle.kts` contiene dos bloques completos de `android` y dos bloques de `dependencies`.  
**Riesgo:**
- Aumento de errores por cambios parciales en un bloque y olvido del otro.
- Dependencias repetidas y posible confusión de resolución.
- Mayor complejidad para migraciones de AGP/Kotlin.

**Recomendación:**
- Consolidar en un único bloque `android {}` y un único bloque `dependencies {}`.
- Activar validación en CI (`./gradlew help`, `./gradlew lint`, `./gradlew test`).

## 3.2 Alto

### A1. Repositorio sin Gradle Wrapper (`gradlew`)
**Evidencia:** no existe archivo `./gradlew` en raíz del proyecto.  
**Riesgo:**
- No reproducibilidad de builds entre desarrolladores/CI.
- Mayor fricción de onboarding.
- Imposibilidad de ejecutar controles básicos en entornos donde Gradle no esté preinstalado.

**Recomendación:**
- Generar wrapper con versión fija (alineada con AGP 8.12.0).
- Versionar `gradlew`, `gradlew.bat` y `gradle/wrapper/*`.

## 3.3 Medio

### M1. Null-safety frágil (`!!`) en múltiples fragments
**Evidencia:** accesores como `private val binding get() = _binding!!` y usos directos de `p!!`.  
**Riesgo:**
- Crashes intermitentes por ciclo de vida (`onDestroyView`/recreación).
- Mayor costo de soporte en producción.

**Recomendación:**
- Reemplazar patrón por acceso seguro (`_binding ?: return`) en puntos de uso.
- Encapsular rendering de UI en métodos que reciban objetos no nulos.

### M2. Backup de datos habilitado por defecto
**Evidencia:** `android:allowBackup="true"` en `AndroidManifest.xml`.  
**Riesgo:**
- Exposición no deseada de información de proyectos si no está definido el alcance exacto de backup.

**Recomendación:**
- Validar política de datos; si no se requiere backup, usar `false`.
- Si se requiere, reforzar `data_extraction_rules.xml`/`backup_rules.xml` y documentarlo.

### M3. Generación PDF sin paginación
**Evidencia:** `PdfGenerator` crea una sola página y dibuja secciones/totales en flujo lineal.  
**Riesgo:**
- Pérdida visual o solapamiento de contenido cuando hay muchas partidas o acero detallado.

**Recomendación:**
- Implementar salto de página cuando `y` supere umbral de alto útil.
- Añadir pruebas instrumentadas de exportación para casos largos.

## 3.4 Bajo

### B1. Manejo silencioso de excepción JSON
**Evidencia:** en `ProyectoRepository`, parseo de JSON usa `catch (_: Exception) { emptyList() }`.  
**Riesgo:**
- Se pierden señales diagnósticas ante corrupción de datos.

**Recomendación:**
- Registrar error (Logcat/Crashlytics) con contexto mínimo.
- Considerar estrategia de migración/limpieza de datos dañados.

### B2. Cobertura de tests no evidenciada
**Evidencia:** existen dependencias de test, pero en esta auditoría no se encontraron suites ejecutables ni se pudo correr pipeline por ausencia de wrapper.  
**Riesgo:**
- Regresiones funcionales en cálculos/presupuestos.

**Recomendación:**
- Priorizar tests unitarios para fórmulas CAPECO y conversión Room ↔ dominio.

## 4) Riesgo agregado

- **Riesgo técnico actual:** Medio-Alto.
- **Riesgo operativo (entrega/CI):** Alto por falta de wrapper.
- **Riesgo de estabilidad de UI:** Medio por `!!`.
- **Riesgo de seguridad/privacidad:** Medio (depende de política de backup).

## 5) Plan de remediación propuesto (priorizado)

### Sprint 1 (rápido, 1–2 días)
1. Eliminar duplicados en `app/build.gradle.kts`.
2. Agregar Gradle Wrapper al repositorio.
3. Definir pipeline mínimo CI: `lint`, `test`, `assembleDebug`.

### Sprint 2 (2–4 días)
4. Reducir/eliminar `!!` críticos en Fragments de mayor uso.
5. Añadir logs de errores de deserialización en repositorio.
6. Revisar y formalizar política de backup.

### Sprint 3 (3–5 días)
7. Implementar paginación de PDF.
8. Agregar pruebas de regresión para cálculos y exportación.

## 6) Comandos ejecutados durante la auditoría

- `rg --files`
- `git status --short`
- `./gradlew -q tasks --all | head -n 40` (falló por ausencia de `gradlew`)
- `rg -n "TODO|FIXME|HACK|XXX|@Suppress|allowBackup|usesCleartextTraffic|exported=\"true\"|RuntimeException|!!|GlobalScope|Thread\(|synchronized" app/src/main -g '!**/build/**'`
- Inspección manual de archivos clave con `sed -n`.

## 7) Conclusión

El proyecto tiene una base funcional sólida y estructura modular razonable, pero debe atender de forma prioritaria la **higiene de build** (duplicación + wrapper) y la **robustez de UI** (`!!`) para reducir riesgos de mantenimiento y fallos en producción.
