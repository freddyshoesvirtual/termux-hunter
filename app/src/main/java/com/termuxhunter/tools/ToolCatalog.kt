package com.termuxhunter.tools

import com.termuxhunter.tools.ToolField.Choice
import com.termuxhunter.tools.ToolField.Flag
import com.termuxhunter.tools.ToolField.Text

private fun pkg(name: String) = "pkg install -y $name"
private fun rootPkg(name: String) = "pkg install -y root-repo && pkg install -y $name"

object ToolCatalog {
    val tools: List<Tool> = listOf(
        Tool(
            id = "nmap", name = "Nmap", binary = "nmap", category = Category.RECON,
            description = "Escáner de puertos y servicios. Los escaneos SYN/UDP y la detección de SO necesitan root.",
            installCommand = pkg("nmap"),
            fields = listOf(
                Text("target", "Objetivo", hint = "192.168.1.0/24 o ejemplo.com", required = true, raw = true),
                Choice(
                    "type", "Tipo de escaneo",
                    listOf(
                        "Por defecto" to "",
                        "TCP connect (-sT)" to "-sT",
                        "SYN sigiloso (-sS, root)" to "-sS",
                        "UDP (-sU, root)" to "-sU",
                        "Descubrir hosts (-sn)" to "-sn",
                    ),
                ),
                Choice(
                    "timing", "Velocidad",
                    listOf("Normal" to "", "Rápido (-T4)" to "-T4", "Agresivo (-T5)" to "-T5", "Sigiloso (-T1)" to "-T1"),
                ),
                Text("ports", "Puertos", arg = "-p", hint = "22,80,443 o 1-1000"),
                Flag("fast", "Solo 100 puertos comunes (-F)", "-F"),
                Flag("version", "Detectar versiones (-sV)", "-sV"),
                Flag("os", "Detectar sistema operativo (-O, root)", "-O"),
                Flag("aggressive", "Agresivo: SO, versiones, scripts (-A)", "-A"),
                Flag("noping", "No hacer ping (-Pn)", "-Pn"),
                Flag("verbose", "Detallado (-v)", "-v"),
                Text("extra", "Argumentos extra", hint = "--script vuln", raw = true),
            ),
        ),
        Tool(
            id = "whois", name = "Whois", binary = "whois", category = Category.RECON,
            description = "Consulta el registro de un dominio o una IP.",
            installCommand = pkg("whois"),
            fields = listOf(Text("target", "Dominio o IP", hint = "ejemplo.com", required = true)),
        ),
        Tool(
            id = "dig", name = "Dig (DNS)", binary = "dig", category = Category.RECON,
            description = "Consulta registros DNS de un dominio.",
            installCommand = pkg("dnsutils"),
            fields = listOf(
                Text("domain", "Dominio", hint = "ejemplo.com", required = true),
                Choice(
                    "type", "Tipo de registro",
                    listOf("A" to "A", "AAAA" to "AAAA", "MX" to "MX", "NS" to "NS", "TXT" to "TXT", "CNAME" to "CNAME", "ANY" to "ANY"),
                ),
                Text("server", "Servidor DNS", hint = "@8.8.8.8", raw = true),
                Flag("short", "Respuesta corta (+short)", "+short"),
            ),
        ),
        Tool(
            id = "traceroute", name = "Traceroute", binary = "traceroute", category = Category.RECON,
            description = "Muestra la ruta de red hasta un host.",
            installCommand = pkg("traceroute"),
            fields = listOf(
                Text("host", "Host", hint = "ejemplo.com", required = true),
                Text("hops", "Saltos máximos", arg = "-m", hint = "30"),
                Flag("numeric", "Sin resolver nombres (-n)", "-n"),
                Flag("icmp", "Usar ICMP (-I, root)", "-I"),
            ),
        ),
        Tool(
            id = "sslscan", name = "SSLScan", binary = "sslscan", category = Category.WEB,
            description = "Analiza los cifrados y certificados TLS de un servidor.",
            installCommand = pkg("sslscan"),
            fields = listOf(
                Text("target", "Host[:puerto]", hint = "ejemplo.com:443", required = true),
                Flag("cert", "Mostrar certificado", "--show-certificate"),
                Flag("nocolor", "Sin colores", "--no-colour", default = true),
            ),
        ),
        Tool(
            id = "dirb", name = "Dirb", binary = "dirb", category = Category.WEB,
            description = "Busca directorios y archivos ocultos en un sitio web.",
            installCommand = pkg("dirb"),
            fields = listOf(
                Text("url", "URL", hint = "http://ejemplo.com/", required = true),
                Text("wordlist", "Diccionario (ruta)", hint = "\$PREFIX/share/dirb/wordlists/common.txt", raw = true),
                Text("ext", "Extensiones", arg = "-X", hint = ".php,.html"),
                Flag("silent", "No mostrar cada prueba (-S)", "-S"),
            ),
        ),
        Tool(
            id = "curl", name = "cURL", binary = "curl", category = Category.WEB,
            description = "Hace peticiones HTTP y muestra la respuesta.",
            installCommand = pkg("curl"),
            fields = listOf(
                Text("url", "URL", hint = "https://ejemplo.com", required = true),
                Choice("method", "Método", listOf("GET" to "", "POST" to "-X POST", "PUT" to "-X PUT", "DELETE" to "-X DELETE", "HEAD" to "-I")),
                Text("header", "Cabecera", arg = "-H", hint = "Authorization: Bearer ..."),
                Text("data", "Datos (body)", arg = "-d", hint = "a=1&b=2"),
                Text("agent", "User-Agent", arg = "-A", hint = "Mozilla/5.0"),
                Flag("headers", "Incluir cabeceras de respuesta (-i)", "-i"),
                Flag("follow", "Seguir redirecciones (-L)", "-L"),
                Flag("insecure", "Ignorar certificado (-k)", "-k"),
                Flag("silent", "Silencioso (-s)", "-s", default = true),
            ),
        ),
        Tool(
            id = "sqlmap", name = "SQLMap", binary = "sqlmap", category = Category.WEB,
            description = "Detecta y explota inyecciones SQL. Se instala con pip.",
            installCommand = "pkg install -y python && pip install sqlmap",
            fields = listOf(
                Text("url", "URL", arg = "-u", hint = "http://ejemplo.com/item.php?id=1", required = true),
                Text("data", "Datos POST", arg = "--data", hint = "user=a&pass=b"),
                Choice("level", "Nivel", listOf("1" to "", "2" to "--level=2", "3" to "--level=3", "4" to "--level=4", "5" to "--level=5")),
                Choice("risk", "Riesgo", listOf("1" to "", "2" to "--risk=2", "3" to "--risk=3")),
                Flag("batch", "Respuestas automáticas (--batch)", "--batch", default = true),
                Flag("dbs", "Listar bases de datos (--dbs)", "--dbs"),
                Text("db", "Base de datos", arg = "-D"),
                Flag("tables", "Listar tablas (--tables)", "--tables"),
                Flag("agent", "User-Agent aleatorio", "--random-agent"),
            ),
        ),
        Tool(
            id = "wget", name = "Wget", binary = "wget", category = Category.UTILS,
            description = "Descarga archivos o sitios completos.",
            installCommand = pkg("wget"),
            fields = listOf(
                Text("url", "URL", hint = "https://ejemplo.com/archivo", required = true),
                Text("output", "Guardar como", arg = "-O"),
                Flag("recursive", "Recursivo (-r)", "-r"),
                Flag("noparent", "No subir de directorio (-np)", "-np"),
                Flag("continue", "Continuar descarga (-c)", "-c"),
            ),
        ),
        Tool(
            id = "nc", name = "Netcat", binary = "nc", category = Category.NETWORK,
            description = "Conecta, escucha o escanea puertos TCP/UDP.",
            installCommand = pkg("netcat-openbsd"),
            interactive = true,
            fields = listOf(
                Choice("mode", "Modo", listOf("Conectar" to "", "Escuchar (-l)" to "-l", "Escanear puertos (-z)" to "-z")),
                Flag("verbose", "Detallado (-v)", "-v", default = true),
                Flag("udp", "UDP (-u)", "-u"),
                Flag("numeric", "Sin DNS (-n)", "-n"),
                Text("host", "Host", hint = "192.168.1.10 (vacío al escuchar)"),
                Text("port", "Puerto(s)", hint = "4444 o 20-100", required = true, raw = true),
            ),
        ),
        Tool(
            id = "ssh", name = "SSH", binary = "ssh", category = Category.NETWORK,
            description = "Conexión remota segura y túneles.",
            installCommand = pkg("openssh"),
            interactive = true,
            fields = listOf(
                Text("target", "usuario@host", hint = "root@192.168.1.10", required = true),
                Text("port", "Puerto", arg = "-p", hint = "22"),
                Text("key", "Llave privada", arg = "-i", hint = "~/.ssh/id_ed25519", raw = true),
                Text("socks", "Proxy SOCKS local (-D)", arg = "-D", hint = "1080"),
                Text("forward", "Reenvío local (-L)", arg = "-L", hint = "8080:localhost:80"),
            ),
        ),
        Tool(
            id = "socat", name = "Socat", binary = "socat", category = Category.NETWORK,
            description = "Relés y túneles entre sockets, archivos y procesos.",
            installCommand = pkg("socat"),
            interactive = true,
            fields = listOf(
                Text("a", "Dirección 1", hint = "TCP-LISTEN:8080,fork", required = true),
                Text("b", "Dirección 2", hint = "TCP:192.168.1.10:80", required = true),
                Flag("verbose", "Detallado (-v)", "-v"),
            ),
        ),
        Tool(
            id = "crunch", name = "Crunch", binary = "crunch", category = Category.PASSWORDS,
            description = "Genera diccionarios de contraseñas.",
            installCommand = pkg("crunch"),
            fields = listOf(
                Text("min", "Longitud mínima", hint = "4", required = true, raw = true),
                Text("max", "Longitud máxima", hint = "6", required = true, raw = true),
                Text("charset", "Caracteres", hint = "abc123"),
                Text("output", "Guardar en", arg = "-o", hint = "lista.txt"),
            ),
        ),
        Tool(
            id = "hydra", name = "Hydra", binary = "hydra", category = Category.PASSWORDS,
            description = "Fuerza bruta de logins en red. No está en los repos de Termux: instálala manualmente.",
            installCommand = null,
            fields = listOf(
                Text("user", "Usuario", arg = "-l"),
                Text("userlist", "Lista de usuarios", arg = "-L", raw = true),
                Text("pass", "Contraseña", arg = "-p"),
                Text("passlist", "Lista de contraseñas", arg = "-P", raw = true),
                Text("threads", "Hilos", arg = "-t", hint = "4"),
                Flag("verbose", "Mostrar intentos (-V)", "-V"),
                Text("target", "Objetivo", hint = "ssh://192.168.1.10", required = true, raw = true),
            ),
        ),
        Tool(
            id = "john", name = "John the Ripper", binary = "john", category = Category.PASSWORDS,
            description = "Crackeo de hashes. No está en los repos de Termux: instálala manualmente.",
            installCommand = null,
            fields = listOf(
                Text("wordlist", "Diccionario", arg = "--wordlist", raw = true),
                Text("format", "Formato", arg = "--format", hint = "raw-md5"),
                Text("hashes", "Archivo de hashes", required = true, raw = true),
            ),
        ),
        Tool(
            id = "hashcat", name = "Hashcat", binary = "hashcat", category = Category.PASSWORDS,
            description = "Crackeo de hashes por GPU/CPU. No está en los repos de Termux: instálala manualmente.",
            installCommand = null,
            fields = listOf(
                Text("mode", "Tipo de hash (-m)", arg = "-m", hint = "0 = MD5"),
                Choice("attack", "Ataque", listOf("Diccionario (-a 0)" to "-a 0", "Fuerza bruta (-a 3)" to "-a 3")),
                Text("hashes", "Archivo de hashes", required = true, raw = true),
                Text("wordlist", "Diccionario o máscara", raw = true),
            ),
        ),
        Tool(
            id = "msfconsole", name = "Metasploit", binary = "msfconsole", category = Category.EXPLOIT,
            description = "Framework de explotación. Instálalo manualmente (no está en los repos de Termux).",
            installCommand = null,
            interactive = true,
            fields = listOf(
                Flag("quiet", "Sin banner (-q)", "-q", default = true),
                Text("cmds", "Comandos al iniciar (-x)", arg = "-x", hint = "use auxiliary/scanner/portscan/tcp"),
            ),
        ),
        Tool(
            id = "tcpdump", name = "Tcpdump", binary = "tcpdump", category = Category.NETWORK,
            description = "Captura tráfico de red.", requiresRoot = true,
            installCommand = rootPkg("tcpdump"),
            fields = listOf(
                Text("iface", "Interfaz", arg = "-i", hint = "wlan0"),
                Text("count", "Paquetes", arg = "-c", hint = "100"),
                Text("write", "Guardar .pcap", arg = "-w", hint = "captura.pcap"),
                Flag("numeric", "Sin resolver nombres (-n)", "-n", default = true),
                Text("filter", "Filtro", hint = "port 80", raw = true),
            ),
        ),
        Tool(
            id = "tshark", name = "TShark", binary = "tshark", category = Category.NETWORK,
            description = "Wireshark en terminal.", requiresRoot = true,
            installCommand = rootPkg("tshark"),
            fields = listOf(
                Text("iface", "Interfaz", arg = "-i", hint = "wlan0"),
                Text("count", "Paquetes", arg = "-c", hint = "100"),
                Text("capture", "Filtro de captura", arg = "-f", hint = "tcp port 443"),
                Text("display", "Filtro de visualización", arg = "-Y", hint = "http.request"),
            ),
        ),
        Tool(
            id = "arp-scan", name = "ARP-Scan", binary = "arp-scan", category = Category.RECON,
            description = "Descubre dispositivos en tu red local.", requiresRoot = true,
            installCommand = rootPkg("arp-scan"),
            fields = listOf(
                Text("iface", "Interfaz", arg = "-I", hint = "wlan0"),
                Flag("local", "Escanear red local (-l)", "--localnet", default = true),
                Text("target", "Rango (si no es la red local)", hint = "192.168.1.0/24", raw = true),
            ),
        ),
        Tool(
            id = "hping3", name = "Hping3", binary = "hping3", category = Category.NETWORK,
            description = "Generador de paquetes TCP/UDP/ICMP.", requiresRoot = true,
            installCommand = rootPkg("hping3"),
            fields = listOf(
                Text("host", "Host", required = true),
                Choice("mode", "Modo", listOf("TCP SYN (-S)" to "-S", "ICMP (-1)" to "-1", "UDP (-2)" to "-2")),
                Text("port", "Puerto", arg = "-p", hint = "80"),
                Text("count", "Paquetes", arg = "-c", hint = "5"),
            ),
        ),
        Tool(
            id = "macchanger", name = "MAC Changer", binary = "macchanger", category = Category.WIRELESS,
            description = "Muestra o cambia la dirección MAC de una interfaz.", requiresRoot = true,
            installCommand = rootPkg("macchanger"),
            fields = listOf(
                Choice(
                    "mode", "Acción",
                    listOf("Mostrar (-s)" to "-s", "Aleatoria (-r)" to "-r", "Restaurar original (-p)" to "-p"),
                ),
                Text("mac", "MAC personalizada (-m)", arg = "-m", hint = "00:11:22:33:44:55"),
                Text("iface", "Interfaz", hint = "wlan0", required = true),
            ),
        ),
        Tool(
            id = "aircrack-ng", name = "Aircrack-ng", binary = "aircrack-ng", category = Category.WIRELESS,
            description = "Crackeo de claves WiFi a partir de capturas. Necesita root e instalación manual.",
            requiresRoot = true, installCommand = null,
            fields = listOf(
                Text("wordlist", "Diccionario", arg = "-w", raw = true),
                Text("bssid", "BSSID", arg = "-b"),
                Text("capture", "Captura .cap", required = true, raw = true),
            ),
        ),
    )

    private val byBinary = tools.associateBy { it.binary }

    fun forBinary(binary: String): Tool? = byBinary[binary]
}
