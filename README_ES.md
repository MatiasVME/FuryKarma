# FuryKarma

[Read in English (README.md)](README.md)

**FuryKarma** es un plugin base y moderno para Minecraft Paper (compatible con 26.1.2+ y Java 25) diseñado para gestionar el karma de los jugadores. Permite a los jugadores dar valoraciones positivas o negativas a otros con una razón de registro, ver perfiles de estadísticas detalladas, consultar un top 10 de jugadores mejor valorados y personalizar todos los mensajes del juego.

El plugin funciona tanto para servidores premium (online-mode) como no-premium/cracked (offline-mode), integrándose de forma nativa con **nLogin** para garantizar que los jugadores deban estar autenticados antes de poder interactuar con el karma. Los datos se almacenan de manera segura y eficiente mediante una base de datos **SQLite** local integrada.

---

## Características

- **Votaciones de Karma**: Permite calificar a otros jugadores con `/karma <positive|negative> <player> <razon>`.
- **Información Detallada del Perfil**: Con `/karma info` se puede visualizar el karma neto, la media de valoraciones positivas y un historial de las últimas calificaciones recibidas (con tiempos relativos estilo `hace 2h`).
- **Tabla de Clasificación (Top 10)**: Muestra a los 10 mejores jugadores ordenados por su media de karma positivo, mostrando también el recuento exacto de positivos y negativos.
- **Base de Datos SQLite**: Almacenamiento integrado para un rendimiento óptimo de las consultas sin archivos YAML pesados.
- **Compatibilidad con nLogin**: Restringe de manera segura el uso de comandos a jugadores que no hayan iniciado sesión.
- **Cooldowns Configurables**: Limita el tiempo entre votaciones (por defecto 24 horas) para evitar abusos. Se puede omitir con el permiso `furykarma.bypass.cooldown`.
- **Formateo de Mensajes Avanzado**: Soporta tanto códigos tradicionales de sección (ej. `&e`) como la sintaxis moderna de Adventure MiniMessage (ej. `<green>Texto</green>`).
- **Sistema de Comandos Moderno**: Registro dinámico de comandos mediante la API Brigadier de PaperMC, con autocompletado inteligente.

---

## Requisitos

- **Servidor**: PaperMC, Purpur u otro fork compatible con versión **26.1.2** o superior.
- **Java**: Versión **25** (Eclipse Temurin recomendado).
- **Autenticación (Opcional)**: nLogin (completamente compatible).

---

## Instalación y Compilación

1. Clona o descarga el repositorio del proyecto.
2. Abre la consola en la raíz del proyecto.
3. Compila el archivo JAR utilizando el Gradle wrapper:
   ```bash
   ./gradlew build
   ```
4. El archivo JAR compilado se generará en la ruta:
   `app/build/libs/FuryKarma.jar`
5. Copia el archivo `FuryKarma.jar` dentro de la carpeta `plugins/` de tu servidor y reinícialo.

---

## Configuración (`config.yml`)

El archivo de configuración se crea automáticamente en `plugins/FuryKarma/config.yml` al iniciar el servidor por primera vez:

```yaml
# FuryKarma Configuration

# Cooldown en horas entre dando karma.
# Por defecto, un jugador puede dar karma cada 24 horas.
cooldown-hours: 24

# Configuración de todos los mensajes del juego.
# Puedes usar códigos de color clásicos de Bukkit (ej. &a, &c, &e) o tags de MiniMessage (ej. <green>, <red>, <gold>).
messages:
  prefix: "&e[&bFuryKarma&e] &r"
  no-permission: "&cYou do not have permission to execute this command."
  only-players: "&cOnly players can execute this command. Use /karma info <player> from the console."
  player-not-found: "&cPlayer &e%player% &chas never played on this server."
  cooldown: "&cYou must wait &e%time% &cbefore you can give karma again."
  cannot-self-karma: "&cYou cannot give karma to yourself!"
  karma-given-sender: "&aYou have given a &e%type% &akarma to &b%player% &afor: &7%reason%"
  karma-received-target: "&aYou have received a &e%type% &akarma from &b%player% &afor: &7%reason%"
  invalid-karma-type: "&cInvalid karma type! Use &e/karma <positive|negative> <player> <reason>&c."
  not-authenticated: "&cYou must login before using karma commands."
  config-reloaded: "&aConfiguration reloaded successfully."
  missing-arguments: "&cUsage: &e/karma <positive|negative> <player> <reason>"
  missing-reason: "&cYou must provide a reason for giving karma."
  positive-label: "&a&lPOSITIVE"
  negative-label: "&c&lNEGATIVE"

  # Formateo del comando de ayuda (help)
  help:
    header: "&e&m-------&r &b&lFuryKarma Help &e&m-------"
    give: " &e/karma <positive|negative> <player> <reason> &7- Give karma to a player"
    info-self: " &e/karma info &7- Show your karma information"
    info-other: " &e/karma info <player> &7- Show another player's karma"
    info-top: " &e/karma top &7- Show the top 10 karma leaderboard"
    info-help: " &e/karma help &7- Show this help menu"
    footer: "&e&m----------------------------"

  # Formateo del comando de información (info)
  info:
    header: "&e&m-------&r &b&lKarma Info: &e%player% &e&m-------"
    total: "&7Total Karma (Net): &6%total%"
    average: "&7Average Rating: &b%average% &8(&a+%positives% &7/ &c-%negatives%&8)"
    no-logs: "&7No karma records received yet."
    logs-header: "&eRecent received ratings:"
    log-entry: " &8- %type% &7by &b%sender% &8(&e%time% ago&8): &7%reason%"
    footer: "&e&m----------------------------"

  # Formateo del comando de ranking (top)
  top:
    header: "&e&m-------&r &b&lTop 10 Karma Leaderboard &e&m-------"
    entry: "&e%index%. &b%player% &7- Average: &b%average% &8(&6Net: %net%&8, &a+%positives%&7/&c-%negatives%&8)"
    no-data: "&7No karma data recorded yet."
    footer: "&e&m---------------------------------------"
```

---

## Comandos y Permisos

### Comandos
- `/karma <positive|negative> <player> <reason>` - Da karma positivo o negativo a otro jugador con una razón explicada. (Alias: `/furykarma ...`)
- `/karma info` - Muestra tu propio perfil de karma (karma neto, promedio y las últimas 5 calificaciones recibidas).
- `/karma info <player>` - Muestra el perfil de karma de otro jugador (karma neto, promedio y las últimas 10 calificaciones recibidas).
- `/karma top` - Muestra el top 10 de jugadores ordenados por promedio de karma.
- `/karma help` - Muestra el menú de ayuda del plugin.
- `/karma reload` - Recarga la configuración del plugin desde el disco.

### Permisos
- `furykarma.use`: Permite usar los comandos básicos de consulta (info, top, help). *Por defecto: Todos*.
- `furykarma.give`: Permite calificar a otros jugadores. *Por defecto: Todos*.
- `furykarma.bypass.cooldown`: Permite ignorar el cooldown entre votaciones. *Por defecto: OP*.
- `furykarma.admin`: Permite recargar la configuración del plugin. *Por defecto: OP*.
