# APK de pruebas Android Iperf

Aplicación Android para pruebas de red con iperf3.

## Características
- TCP Download/Upload con retransmisiones
- UDP Download/Upload con jitter y pérdida
- Ping con pérdida de paquetes
- Información WiFi (SSID, RSSI, frecuencia, estándar)
- Reporte HTML con gráficas Chart.js
- Reporte guardado en Descargas/Coopelesca/

## Requisitos
- Android 9.0+ (API 28)
- ARM64

## Compilación

### 1. Compilar iperf3 para Android ARM64
```bash
git clone https://github.com/esnet/iperf.git
cd iperf
export NDK=/opt/android-sdk/ndk-bundle
export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
export TARGET=aarch64-linux-android
export API=28
export CC=$TOOLCHAIN/bin/${TARGET}${API}-clang
export CXX=$TOOLCHAIN/bin/${TARGET}${API}-clang++
export AR=$TOOLCHAIN/bin/llvm-ar
export RANLIB=$TOOLCHAIN/bin/llvm-ranlib
export STRIP=$TOOLCHAIN/bin/llvm-strip
./configure --host=$TARGET --disable-shared --enable-static --without-openssl \
  CC=$CC CXX=$CXX AR=$AR RANLIB=$RANLIB CFLAGS="-O2 -fPIE -fPIC" LDFLAGS="-pie"
make -j$(nproc)
$STRIP src/iperf3
cp src/iperf3 app/src/main/jniLibs/arm64-v8a/libiperf3.so
```

### 2. Compilar APK
```bash
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug
```

## Versiones
- v1.0 - Primera versión
- v1.1 - Soporte Android 16 (targetSdk 36)
- v1.2 - Info WiFi en reporte + guardado en Descargas
