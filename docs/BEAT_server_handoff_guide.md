# BEAT サーバー引き渡しガイド

この文書は、localhostでセットアップしたBEAT大会環境を、本番サーバーの構築担当者へ引き渡すための手順書です。

Minecraftサーバーの一般的な構築方法ではなく、主催側が渡すものと、本番サーバーへ配置する手順をまとめています。

## 1. 役割分担

### 主催・マップ制作者

- localhostで競技ワールドを完成させる。
- `/beat setup` で地点・範囲を登録する。
- `/beat setup validate all` を通す。
- 参加者、運営、競技時間を設定する。
- 本番用のファイルをまとめてサーバー構築担当者へ渡す。

### サーバー構築担当者

- Java 25とSpigot 26.2を用意する。
- 必要なプラグインを配置する。
- 受け取ったワールドとBEAT設定を配置する。
- Multiverse-Coreで競技ワールドを読み込む。
- 配置後にBEATの読み込みとマップ検証を確認する。

## 2. 主催側が渡すもの

次のような構成で、一つのZIPにまとめて渡します。

```text
handoff/
├─ plugins/
│  ├─ beat-0.1.0-SNAPSHOT.jar
│  ├─ ProtocolLib-5.4.0-all.jar
│  └─ BEAT/
│     ├─ admins.json
│     ├─ participants.json
│     ├─ competition-settings.yml
│     ├─ config.yml
│     ├─ messages.yml
│     ├─ styles.yml
│     └─ maps/
│        ├─ high-difficulty.yml
│        ├─ time-attack.yml
│        └─ endurance.yml
├─ worlds/
│  ├─ ロビーワールド
│  ├─ 高難易度ワールド
│  ├─ TAワールド
│  └─ 耐久ワールド
└─ README.md
```

実際に使用していないワールドやプラグインは含める必要がありません。

### README.mdに記載する内容

- 各ワールドの用途
- ワールドの正確なフォルダー名
- 必要な外部プラグインとバージョン
- BEATのバージョンまたはJARファイル名
- 本番設定かテスト設定か
- `/beat setup validate all` の最終確認結果
- ZIPを作成した日付

BEATのマップ設定にはワールド名が保存されています。サーバー側でワールドのフォルダー名を変更しないよう、README.mdにも明記してください。

### ZIP作成前の確認

localhostで次を実行します。

```text
/beat setup validate all
/beat settings show
/beat players
```

- 3競技すべてのValidationにERRORがないこと
- `competition-settings.yml` が本番用設定になっていること
- `admins.json` と `participants.json` が本番用になっていること
- ZIP作成前にlocalhostのサーバーを停止していること

## 3. 渡さないもの

次のファイルやディレクトリは、原則として引き渡し用ZIPへ含めません。

```text
plugins/BEAT/data/
plugins/BEAT/backups/
plugins/BEAT/logs/
logs/
spigot-26.2.jar
テスト用ワールド
```

特に、次のファイルにはlocalhostで行ったテストの大会状態や結果が保存されています。

```text
plugins/BEAT/data/event-state.json
plugins/BEAT/data/results.json
```

これらを本番サーバーへ渡すと、テスト時の進行状態や結果が本番環境へ混ざる可能性があります。

そのほか、次のものも本番サーバーの実行には不要です。

- ソースコード
- Gradle開発環境
- `whitelist.json`
- `ops.json`
- localhostのサーバーログ
- BuildToolsの作業ディレクトリ

Whitelistは本番サーバー上でBEATから同期します。

## 4. サーバー構築担当者が用意するもの

- Java 25
- Spigot 26.2
- ProtocolLib
- Multiverse-Core
- 必要に応じてWorldEdit
- 本番サーバーの接続・バックアップ環境

Spigot本体は非公式サイトからダウンロードせず、公式BuildToolsで生成します。

```text
java -jar BuildTools.jar --rev 26.2
```

BEATを本番サーバーで動かすだけなら、ソースコードやGradle環境は必要ありません。

外部プラグインのJARを主催側から渡さない場合は、README.mdに記載されたバージョンをサーバー構築担当者が用意します。

## 5. サーバー側の配置手順

1. Spigotサーバーを停止する。
2. BEATとProtocolLibを `plugins/` に配置する。
3. 必要に応じてMultiverse-CoreとWorldEditを `plugins/` に配置する。
4. 受け取った `plugins/BEAT/` を本番サーバーの同じ場所へ配置する。
5. 受け取ったワールドをサーバールートへ配置する。
6. サーバーを起動する。
7. Multiverse-Coreで各競技ワールドをインポートする。
8. `/plugins` でBEATとProtocolLibが有効になっていることを確認する。
9. `/beat reload` を実行する。
10. `/beat setup validate all` が成功することを確認する。
11. `/beat settings show` で本番用の競技設定になっていることを確認する。
12. `/beat players` で参加者が正しく読み込まれていることを確認する。
13. `/beat whitelist admins` を実行し、運営だけが入れる状態にする。

Multiverse-Coreでワールドをインポートする場合の例です。

```text
/mv import <ワールド名> normal
```

ワールドの環境に応じて、`normal` の部分は適切な種類を指定してください。

ワールド名は、BEATのマップ設定に記録された名前と完全に一致させます。配置後にValidationが失敗した場合は、最初に次を確認してください。

- ワールドのフォルダー名が変更されていないか
- 対象ワールドがMultiverse-Coreへインポートされているか
- ワールドが正常にロードされているか
- `plugins/BEAT/maps/` の3ファイルが配置されているか
- BEATの起動時に設定読み込みエラーが出ていないか

配置と確認が完了したら、主催側へ次を連絡します。

- BEATと依存プラグインの読み込み結果
- `/beat setup validate all` の結果
- `/beat settings show` の結果
- 本番サーバーで読み込んだワールド名
- 主催側が接続確認できるサーバー情報

以降の大会準備と本番進行は、`BEAT_plugin_operations_guide.md` に従います。
