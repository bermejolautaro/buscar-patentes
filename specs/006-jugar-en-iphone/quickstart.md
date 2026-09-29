# Quickstart: Jugar en el iPhone

Cómo se prueba, de punta a punta, que la app anda en el iPhone y que el Android sigue igual. Cada
bloque dice qué criterio de éxito cubre. Lo automático corre con Gradle; lo demás se camina.

## Una sola vez: preparar la PC y el iPhone

1. **iTunes de la web, no el de la Microsoft Store.** Sideloadly lo pide, y es también el que
   muestra los archivos compartidos de la app (los respaldos). Si está la versión de la Store, se
   desinstala primero.
2. **Sideloadly**, de [sideloadly.io](https://sideloadly.io).
3. **En el iPhone**: Ajustes → General → Información, y anotar la versión de iOS. Tiene que ser
   **15.5 o más**; con 16 o más, además, hay que activar el modo de desarrollador.
4. **Modo de desarrollador** (iOS 16 o más): Ajustes → Privacidad y seguridad → Modo de
   desarrollador. El iPhone se reinicia.
5. **Siempre el mismo Apple ID** en Sideloadly. Uno secundario sirve, pero una vez elegido no se
   cambia (caso borde de la spec).
6. **GitHub**: `gh auth status` tiene que decir que hay sesión iniciada en la cuenta del jugador.

## Compilar e instalar (SC-001)

```powershell
gh workflow run ios.yml
gh run watch
gh run download --name buscar-patentes-ipa --dir $env:USERPROFILE\Downloads\buscar-patentes-ios
```

En Sideloadly: iPhone enchufado, el `.ipa` y el Apple ID, y **Start**. La primera vez, en el iPhone:
Ajustes → General → VPN y gestión de dispositivos → confiar en el perfil del Apple ID.

**Esperado**: desde `gh workflow run` hasta la app abierta, **menos de 30 minutos**.

## 0. Prueba piloto (D21) — antes de portar nada

Con la app mínima de la prueba piloto instalada:

| Paso | Esperado |
|---|---|
| Abrirla | Muestra la pantalla de Compose |
| Tocar "Escribir marca" | Guarda un archivo con la hora |
| Dar ubicación "siempre" y tocar "Vigilar acá" | Registra una región de 150 m donde está parado |
| Alejarse más de 300 m, cerrar la app **sin deslizarla** y volver caminando | Llega la notificación al entrar |
| Repetir **después de reiniciar el iPhone**, sin abrir la app | Llega igual |
| Repetir después de **cerrar la app deslizándola** | Anotar si llega. No bloquea: se documenta (D11) |
| Tocar "Grabar", bloquear la pantalla y caminar 5 minutos | Al volver, la app muestra la cuenta de puntos, que tiene que ser mayor a 0 |
| Mirar la fecha de vencimiento que muestra | Unos 7 días después de la instalación |
| Reinstalar el mismo `.ipa` con Sideloadly | **La marca escrita antes sigue ahí. Si no, se para todo** |

## 1. El Android sigue igual (SC-009) — después de cada paso de D20

```powershell
./gradlew test assembleDebug
```

- Todas las pruebas pasan: las comunes en la JVM y las que quedan solo en el Android.
- El APK va al teléfono Android como siempre (a `Download/buscar-patentes.apk`, pisando el anterior)
  y se instala **encima**, sin desinstalar. Se compara el hash del APK instalado contra el compilado.
- Al abrir, están las mismas patentes, salidas y fotos que antes de actualizar.
- Después del paso del mapa compartido (D5): los quickstart de la
  [004](../004-planear-recorridos/quickstart.md) y la [005](../005-buscar-la-que-toca/quickstart.md),
  enteros, en el Android. Si alguno falla y no se arregla, se aplica la línea de corte de D5.

## 2. Anotar y encontrar en el iPhone (US1 — SC-004, SC-005)

| Paso | Esperado |
|---|---|
| Abrir, tocar el campo, tipear `318`, confirmar, con cronómetro | Guardada con ubicación, hora y precisión. **4 toques y 10 segundos o menos** |
| Lo mismo en modo avión | Se guarda, y aparece al volver a abrir |
| Apagar "ubicación exacta" en los ajustes del iPhone y anotar | Queda degradada, y la app avisa que la ubicación exacta está apagada |
| Abrir la ficha | Ni la ubicación ni la hora se pueden editar |
| Con los mismos datos en los dos teléfonos (después de la mudanza), poner el mismo número y el mismo zoom | La misma barra, la misma lista en el mismo orden y los mismos grupos |
| Entrar a cada pantalla secundaria | Todas tienen cómo volver sin gestos |

## 3. Reinstalar sin perder nada (US2 — SC-002, SC-008)

| Paso | Esperado |
|---|---|
| Anotar la cantidad de patentes, fotos y salidas, y mirar una ficha | — |
| Reinstalar **el mismo** `.ipa`, cronometrando | **Menos de 5 minutos**, y los mismos números y la misma ficha |
| Reinstalar un `.ipa` **nuevo** | Ídem |
| Ajustes | Muestra la fecha y la hora del vencimiento |
| Dejar pasar hasta que falten menos de 48 horas | La pantalla principal dice cuándo vence. La carga rápida sigue en 4 toques |
| Esperar hasta 24 horas antes | Llega la notificación, con la app cerrada |
| Dejarla vencer y reinstalar | Abre con todo |
| Ajustes → Sacar respaldo | Aparece en Archivos → En mi iPhone → Buscar patentes → respaldos, y en iTunes. Ajustes muestra la fecha del último respaldo |

## 4. Mudanza del Android al iPhone (US3 — SC-003)

Con cronómetro, desde el primer toque en el Android:

1. **Android**: Ajustes → Sacar respaldo → guardar en Descargas (o mandarlo por Drive).
2. **PC**: copiar el `.respaldo` desde el Android por cable.
3. **iTunes**: iPhone → Archivos compartidos → Buscar patentes → arrastrar el `.respaldo`.
4. **iPhone**: Ajustes → Restaurar respaldo → elegirlo.

**Esperado**:
- **Menos de 15 minutos** en total.
- Las mismas cuentas de patentes, salidas y fotos que el Android.
- Cinco registros al azar comparados campo por campo, contra la ficha del Android: ubicación, hora
  y precisión **idénticas**.
- Las fotos abren, y las salidas se dibujan igual, sin volver a pedir el ajuste a calles.

**Casos que se rechazan**, y en todos los datos del iPhone quedan como estaban:
- restaurar un `.jpg` renombrado a `.respaldo`: "no es un respaldo";
- restaurar un respaldo cortado a la mitad: "está dañado".

**Con datos en el iPhone**: aparece la comparación, y después de confirmar queda un
`…-antes-de-restaurar.respaldo` en la carpeta `respaldos`.

**Vuelta**: un respaldo del iPhone restaurado en el Android da todo idéntico.

## 5. Foto, WhatsApp y Google Maps (US4)

- Sacar una foto desde la ficha: se ve en la ficha, y sigue ahí después de reinstalar.
- Compartir: WhatsApp aparece en la hoja, el mensaje lleva la foto y el texto, y la patente pasa a
  compartida.
- "Ir" y un recorrido de tres paradas: Google Maps abre a pie con esas paradas. Sin Google Maps
  instalado, abre en Safari.

## 6. Avisos (US5 — SC-006)

Igual que en la prueba piloto, pero con una patente real del número actual sin compartir:

- con la app cerrada, llega a 150 metros o menos;
- después de reiniciar, llega igual;
- tocar el aviso abre el mapa sobre esa patente;
- pasar dos veces por la misma cuadra en una salida da un solo aviso;
- con permiso "mientras se usa", ajustes dice que los avisos no llegan con la app cerrada, y el
  botón abre los ajustes del iPhone.

## 7. Salidas (US6 — SC-007)

- Batería anotada. Empezar una salida, bloquear, **2 horas** de caminata. Terminar: el camino está
  entero, y la batería bajó **30% o menos** (SC-007, revisado).
- Empezar otra, cerrar la app deslizándola, abrirla: dice "La salida se cortó", y lo grabado está.
- Con conexión, la salida se ajusta a las calles. En el modo antigüedad, los mismos escalones que en
  el Android.

## 8. Nada del jugador en GitHub (SC-010, SC-011) — antes de cada push

```powershell
git grep -nE "58[.,]5[0-9]{2}"
git ls-files "*.png" "*.jpg" "*.respaldo" "*.db"
git log --format="%ae" | Sort-Object -Unique
```

- Ninguna coordenada de la zona. La búsqueda es por la longitud (`-58.5…`): las pruebas usan el
  Obelisco (`-58.38…`), y la latitud sola no distingue barrios.
- Ninguna imagen con patentes reales ni datos.
- Solo el email `noreply`.
- Ningún nombre de calle ni de barrio de la zona en `specs/` ni en el código. Se revisa con
  `git grep -n` sobre los nombres que el jugador conoce de su zona.

Y en GitHub, en la configuración del repositorio, **Secrets** está vacío.
