# Termux Hunter

Interfaz Android con botones y formularios para las herramientas de [Termux](https://github.com/termux/termux-app), inspirada en las pantallas de herramientas de Kali NetHunter. Funciona sin root y aprovecha el root si el teléfono lo tiene.

> Úsala solo en redes y equipos para los que tengas autorización.

## Funciones

- **Detección automática**: lista los ejecutables de `$PREFIX/bin` de Termux. Las herramientas conocidas (nmap, whois, dig, curl, sqlmap, hydra, tcpdump, etc.) tienen su propio formulario; el resto de binarios usa un formulario genérico de argumentos.
- **Root opcional**: detecta `su` (Magisk/KernelSU/SuperSU) y permite ejecutar cualquier herramienta o comando como root. Las herramientas que lo necesitan se marcan como "requiere root".
- **Comandos personalizados**: botones que tú creas, editas y borras, con modo terminal o salida dentro de la app.
- **Dos modos de ejecución**: abrir una sesión visible de Termux, o ejecutar en segundo plano y ver stdout/stderr/código de salida en la app.
- **Instalación**: botón para instalar con `pkg` las herramientas que falten (incluye `root-repo` cuando hace falta).

## Cómo funciona

Igual que NetHunter construye un comando y lo envía a su terminal, Termux Hunter usa el Intent oficial [`com.termux.RUN_COMMAND`](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent) para ejecutar `bash -c "<comando>"` dentro de Termux. Los comandos root se envuelven en `su -c`, así que el gestor de root se lo pide a Termux.

## Configuración

1. Instala Termux desde [F-Droid](https://f-droid.org/packages/com.termux/) o [GitHub](https://github.com/termux/termux-app/releases) (no la versión de Play Store).
2. En Termux ejecuta:
   ```sh
   mkdir -p ~/.termux && echo 'allow-external-apps=true' >> ~/.termux/termux.properties && termux-reload-settings
   ```
3. Abre Termux Hunter y concede el permiso "Ejecutar comandos en Termux" (pestaña Ajustes).

## Compilar

Requiere JDK 17 y Android SDK 35.

```sh
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
