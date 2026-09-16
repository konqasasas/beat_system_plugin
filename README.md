# Beat

Minecraft Java Edition 26.2 / Spigot 26.2 向けの Java プラグイン開発プロジェクトです。Java 25 と Gradle Wrapper を使用します。

## 開発コマンド

macOS / Linux:

```bash
./gradlew test
./gradlew build
./gradlew deployPlugin
./gradlew runServer
```

Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat deployPlugin
.\gradlew.bat runServer
```

`deployPlugin` はメインのプラグイン JAR と、姿を隠してもTabを維持するために必要なProtocolLibを `run/plugins/` に配置します。`runServer` は先に `deployPlugin` を実行してから、Java 25 でローカルサーバーを起動します。

ProtocolLib 5.4.0はMaven Centralから取得するサーバー側依存です。BEAT本体には同梱せず、`plugin.yml` の必須依存として読み込まれます。

## Spigot 26.2 サーバーの準備

Spigot 本体は自動ダウンロードしません。公式サイトから最新の `BuildTools.jar` を入手し、Java 25 を使える作業用ディレクトリで次を実行してください。

```bash
java -jar BuildTools.jar --rev 26.2
```

生成された `spigot-26.2.jar` をこのプロジェクトの `run/spigot-26.2.jar` にコピーします。その後、初回起動時は `run/eula.txt` の内容を確認して Mojang EULA に同意し、もう一度 `runServer` を実行してください。

JAR 名を変える場合は、`run/` からの相対パスを指定できます。

```bash
./gradlew runServer -PspigotJar=custom-spigot.jar
```

サーバー本体、EULA、ログ、設定、ワールド、および配置済みプラグイン JAR は Git 管理対象外です。

## 参加者・運営の登録

初回起動後、`run/plugins/Beat/participants.json` と `run/plugins/Beat/admins.json` を編集します。

```json
[
  {
    "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "mcid": "PlayerName"
  }
]
```

本人識別と管理権限にはUUIDを使用します。OPであるだけではBEAT管理者になりません。管理者は`admins.json`へ登録してください。編集後はConsoleまたは登録済み管理者から次を実行します。

```text
/beat reload
/beat players
/beat player info <player>
/beat whitelist admins
/beat whitelist all
/beat whitelist status
```

`reload`だけではWhitelistを同期しません。`whitelist admins`または`whitelist all`を明示的に実行してください。

## 分離デバッグ環境

Debugデータはメモリ内だけに保持され、`results.json`、`event-state.json`、参加者、Whitelistには保存されません。正式結果が既に存在する場合は、安全のため有効化を拒否します。

```text
/beat debug enable
/beat debug bots add 29
/beat debug fill all seed 12345
/beat debug time set 19:50
/beat debug trigger ta start
/beat debug time advance 30s
/beat debug trigger ta goal
/beat debug high-practice skip
/beat debug scenario all
/beat debug disable
```

`debug disable`またはサーバー再起動でDebugデータは破棄されます。利用できる全サブコマンドは `/beat debug` で確認できます。

## 大会データのリセット

大会終了後などに大会固有データを初状態へ戻す場合は、二段階確認でリセットします。

```text
/beat event reset
/beat event reset confirm
```

確認コマンドには有効期限があります。実行前の `data/event-state.json` と
`data/results.json` は `run/plugins/Beat/backups/` へコピーされます。
大会状態、競技結果、大会時MCID、総合状態、失格・総合除外状態が初期化されますが、
マップ設定、各YAML設定、`participants.json`、`admins.json`、Whitelistモードは維持されます。
