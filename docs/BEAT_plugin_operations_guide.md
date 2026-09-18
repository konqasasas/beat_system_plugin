# BEAT プラグイン運用ガイド

この文書は、BEAT大会を準備・進行する運営担当者向けの手順書です。
開発知識がなくても、上から順番に確認すれば大会を進行できる構成にしています。

管理GUIは、ゲーム内で `/beat` または `/beat menu` を実行すると開きます。
ゲーム内の管理操作は `plugins/BEAT/admins.json` に登録された運営だけが実行できます。サーバーコンソールからはすべての管理コマンドを実行できます。

> **重要:** 大会中に不明な問題が起きた場合、すぐに大会データを初期化しないでください。まず `/beat competition status` で状態を確認し、必要なら第6章の緊急操作を使用してください。

## 1. 概要

BEATは、次の3競技と総合順位を管理します。

1. 高難易度
2. タイムアタック（TA）
3. 耐久
4. 3競技の順位から計算する総合順位

大会は原則として、高難易度、TA、耐久の順に実施します。各競技が終わるたびに結果を確認して発表し、最後に総合結果を計算・確定・発表します。

### 必要なもの

- Spigot 26.2
- Java 25
- BEATプラグイン
- ProtocolLib
- 設定済みの競技ワールド
- 必要に応じてMultiverse-CoreやWorldEdit

サーバー起動後、`/plugins` でBEATとProtocolLibが有効になっていることを確認してください。BEATは `online-mode=true` のサーバーで使用します。

### 大会の基本手順

1. 運営と参加者をJSONへ登録する。
2. 3競技のマップをセットアップする。
3. マップ検証、競技設定、Whitelistを確認する。
4. 高難易度を実施し、結果を発表する。
5. TAを実施し、結果を発表する。
6. 耐久を実施し、結果を発表する。
7. 総合順位を計算・確定し、発表する。

## 2. 準備編

### 2.1 運営と参加者の登録

運営は `plugins/BEAT/admins.json`、参加者は `plugins/BEAT/participants.json` に登録します。

```json
[
  {
    "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "mcid": "PlayerName"
  }
]
```

- 本人確認にはMCIDではなくUUIDが使用されます。
- 同じUUIDを運営と参加者の両方へ登録できません。
- OPであるだけではBEATの運営権限は付与されません。
- JSONにはコメントを記述できません。
- 複数人登録する場合は、各要素の間にカンマを入れます。

編集後、サーバーコンソールまたは既存の運営から次を実行します。

```text
beat reload
```

ゲーム内から実行する場合は `/beat reload` です。その後、次のコマンドで参加者を確認します。

```text
/beat players
/beat player info <MCID>
```

JSONに問題がある場合、新しい内容は適用されず、最後に正常に読み込めた内容が維持されます。

### 2.2 マップのセットアップ

マップ設定は原則としてYAMLを直接編集せず、ゲーム内の `/beat setup` を使用します。

範囲を登録する基本操作は次のとおりです。

1. `/beat setup wand` で選択ツールを受け取る。
2. 左クリックでPos1、右クリックでPos2を選択する。
3. 対象のsetupコマンドを実行する。
4. `/beat setup show ...` で登録位置を目視確認する。

#### 高難易度

各CourseのStart、Spot、Goalと、共通のPrepare、Endを登録します。

```text
/beat setup high start set <course>
/beat setup high spot add <course>
/beat setup high goal set <course>
/beat setup high prepare set
/beat setup high end set
```

SpotとGoalは選択した範囲、Start・Prepare・Endは実行地点が使用されます。Course 1から5まで、実際に走る順番で登録してください。

#### タイムアタック

Start、Split、Goal、Restart、Endを登録します。

```text
/beat setup ta start set
/beat setup ta split add
/beat setup ta goal set
/beat setup ta restart set
/beat setup ta end set
```

Start、Split、Goalは踏み判定の範囲です。Restartは周回開始地点、Endは競技終了後の移動先です。

#### 耐久

Progress地点、Goal Progress、Zone、復帰地点、落下Y座標などを登録します。

```text
/beat setup endurance progress add [number]
/beat setup endurance goal set
/beat setup endurance zone set <2|3> <progress>
/beat setup endurance zone-restart set <2|3>
/beat setup endurance start set
/beat setup endurance end set
/beat setup endurance fall-y set here
```

同じProgress番号に複数地点を登録できます。対称コースや複数ルートでは、同じ番号を指定して追加してください。

登録済み地点は、黄色い小型羊毛表示と白いProgress番号で確認できます。この表示はコマンド実行者にだけ見え、実ブロックではないため当たり判定はありません。

```text
/beat setup show endurance all
/beat setup show endurance progress <number>
/beat setup show endurance off
```

`all` は継続表示され、表示中に追加・変更したProgressも約1秒以内に反映されます。確認後は `off` で停止してください。

### 2.3 マップ検証

セットアップ後は必ず次を実行します。

```text
/beat setup validate all
```

- `ERROR` が1件でもある競技は開始できません。
- `WARNING` は開始を妨げませんが、本番前に内容を確認してください。
- Validationが成功しても、実際に全コースを走って判定位置と移動先を確認してください。

個別の登録内容は `/beat setup show`、`/beat setup info`、`/beat setup list` で確認できます。詳細な書式は `/beat setup help <high|ta|endurance>` を参照してください。

## 3. 本番前の確認

大会開始前に、次の順番で確認してください。

```text
/beat competition status
/beat players
/beat setup validate all
/beat settings show
/beat whitelist status
```

### 3.1 大会データの初期化

前回大会の状態や結果が残っている場合だけ実行します。

```text
/beat event reset
/beat event reset confirm
```

2回目のコマンドは、確認メッセージが表示されてから30秒以内に実行します。既存の大会状態と結果はバックアップ後に初期化されます。マップ、参加者、運営、ワールドは削除されません。

### 3.2 競技設定

管理GUIの「競技設定」または次のコマンドで確認できます。

```text
/beat settings show
```

通常の大会では本番プリセットを使用します。

```text
/beat settings preset production
```

リハーサルでは、制限時間系を半分にしたテストプリセットを使用できます。

```text
/beat settings preset test
```

GUIで変更した値は下書きです。「変更を適用」を押すまで保存されません。競技進行中は競技設定を変更できません。

### 3.3 Whitelist

準備中は運営だけ、本番前は運営と参加者を許可します。

```text
/beat whitelist admins
/beat whitelist all
/beat whitelist status
```

`status` で「完全同期: YES」と表示されることを確認してください。WhitelistはBEATの登録内容へ完全同期されるため、対象外の既存登録は削除されます。

### 本番直前チェックリスト

- [ ] BEATとProtocolLibが有効
- [ ] 参加者と運営の登録が正しい
- [ ] 3競技すべてValidation PASSED
- [ ] 本番用の競技時間になっている
- [ ] WhitelistがALLで完全同期されている
- [ ] 参加者がログインできる
- [ ] Start、Goal、Progressなどを実走確認済み
- [ ] 前回大会データを必要に応じて初期化済み

## 4. 本番編

通常は `/beat` で管理GUIを開いて操作します。以下では、同じ操作を行うコマンドも併記します。

### 4.1 高難易度

1. 管理GUIの「高難易度」を開き、Validationと参加者数を確認する。
2. 開始を確定する。コマンドの場合は `/beat competition high start`。
3. 開始カウントダウン後、練習が始まる。
4. 練習中、参加者は飛行切替と個人CPを使用できる。
5. 練習終了後、参加者はPrepare地点へ移動する。
6. 準備時間終了後、本番が自動で始まる。
7. BossBarの残り時間と「次の脱落」を監視する。
8. 制限時間または全処理終了後、参加者はEnd地点へ移動する。

高難易度を準備状態から手動で本番開始する必要がある場合も、`/beat competition high start` を使用します。

#### 高難易度の結果を発表する

**TAを始める前に、必ずここまで完了してください。**

1. 管理GUIの「結果」から「高難易度」を開く。
2. 順位とポイントを確認する。
3. 誤りを修正した場合は「順位を再計算」してから「結果を確定」を実行する。
4. 「結果を発表」を押し、確認画面でもう一度「発表する」を押す。
5. 全オンラインプレイヤーのチャットに高難易度結果が表示されたことを確認する。

コマンドで結果を確認・再確定する場合は次を使用します。全体への発表操作は管理GUIから行います。

```text
/beat result high show
/beat result high recalculate
/beat result high confirm
/beat result high
```

通常終了直後の結果は確定済みです。結果を編集した場合だけ、再計算と再確定が必要です。

### 4.2 タイムアタック

高難易度が終了し、**高難易度の結果発表まで完了していること**を確認してから開始します。

```text
/beat competition ta start
```

1. カウントダウン中は参加者の移動が固定される。
2. Startを踏むと計測が始まる。
3. SplitとGoalを順番に通過する。
4. Goal後はRestart地点へ戻り、PB更新音または通常完走音が鳴る。
5. 脱落時刻には設定された上位人数だけが残る。
6. 制限時間終了後、参加者はEnd地点へ移動する。

#### タイムアタックの結果を発表する

**耐久を始める前に、必ずここまで完了してください。**

1. 管理GUIの「結果」から「TA」を開く。
2. 順位とPBを確認する。未完走者は `--.--` と表示される。
3. 誤りを修正した場合は「順位を再計算」してから「結果を確定」を実行する。
4. 「結果を発表」を押し、確認画面でもう一度「発表する」を押す。
5. 全オンラインプレイヤーのチャットにTA結果が表示されたことを確認する。

コマンドで結果を確認・再確定する場合は次を使用します。全体への発表操作は管理GUIから行います。

```text
/beat result ta show
/beat result ta recalculate
/beat result ta confirm
/beat result ta
```

通常終了直後の結果は確定済みです。結果を編集した場合だけ、再計算と再確定が必要です。

### 4.3 耐久

TAが終了し、**TAの結果発表まで完了していること**を確認してから開始します。

```text
/beat competition endurance start
```

1. 参加者がProgress地点へ入ると記録が更新される。
2. 次に取る地点は黄色、取得済みは黄緑、将来の地点は空色の小型羊毛で表示され、上部に白いProgress番号が表示される。表示には当たり判定がない。
3. Zone 2・3到達後は、落下時の復帰地点が更新される。
4. 脱落時刻には、必要なZoneへ未到達の参加者が脱落する。
5. Goalまたは制限時間終了後、参加者はEnd地点へ移動する。

`admins.json` に登録された運営には、競技中の全Progressが空色の小型羊毛と番号で常時表示されます。
参加者の取得状況による色変化はなく、実況・監視用の固定表示です。

#### 耐久の結果を発表する

**総合順位を計算する前に、必ずここまで完了してください。**

1. 管理GUIの「結果」から「耐久」を開く。
2. 順位と最終Progressを確認する。
3. 誤りを修正した場合は「順位を再計算」してから「結果を確定」を実行する。
4. 「結果を発表」を押し、確認画面でもう一度「発表する」を押す。
5. 全オンラインプレイヤーのチャットに耐久結果が表示されたことを確認する。

コマンドで結果を確認・再確定する場合は次を使用します。全体への発表操作は管理GUIから行います。

```text
/beat result endurance show
/beat result endurance recalculate
/beat result endurance confirm
/beat result endurance
```

通常終了直後の結果は確定済みです。結果を編集した場合だけ、再計算と再確定が必要です。直前の結果編集は `/beat result undo` で1回だけ戻せます。

## 5. 総合結果と大会終了

3競技の結果を発表した後、管理GUIの「総合」を開きます。

1. 「総合を計算」を実行する。
2. 各参加者の競技順位と総合順位を確認する。
3. 必要なら総合対象外・失格の設定を確認する。
4. 「総合を確定」を実行する。
5. 「結果を発表」から総合結果を発表する。

コマンドの場合は次の順番です。

```text
/beat overall calculate
/beat overall result
/beat overall confirm
```

総合結果の発表は管理GUIから行います。総合確定後は競技結果を編集できません。

参加者を総合対象外または失格にする操作は、管理GUIの「Players」から行えます。コマンドでは次を使用します。

```text
/beat player overall-exclude <MCID>
/beat player overall-include <MCID>
/beat player disqualify <MCID>
/beat player undisqualify <MCID>
```

大会終了後は、次を実行してWhitelistを運営だけに戻します。

```text
/beat whitelist admins
```

`plugins/BEAT/data/` の大会状態・結果、`plugins/BEAT/logs/` の編集ログ、必要なワールドデータを保管してください。

## 6. トラブル・緊急操作

### 6.1 まず確認すること

```text
/beat competition status
/beat setup validate all
/beat whitelist status
```

- 判定されない場合は、登録位置を `/beat setup show ...` で表示し、同じワールドにいるか確認します。
- 設定変更後は、競技中でないことを確認して `/beat reload` を実行します。
- 再接続した参加者は、進行中の競技状態に応じて復帰または観戦状態になります。競技時計は止まりません。
- サーバー再起動後は進行中競技を完全復元せず、安全な再試合待ち状態へ戻ることがあります。確定済みの過去結果は保持されます。

### 6.2 緊急操作

緊急操作はすべて二段階確認です。最初のコマンド後、30秒以内に `confirm` を付けて再実行します。

| 操作 | 用途 |
|---|---|
| `/beat emergency cancel` | 現在のフェーズを中止して安全な状態へ戻す |
| `/beat emergency collect` | オンライン参加者を現在競技のEnd地点へ回収する |
| `/beat emergency force-end` | 現時点の記録で競技を終了・保存する |
| `/beat emergency restart` | 現在の競技を再試合準備へ戻す |

例:

```text
/beat emergency collect
/beat emergency collect confirm
```

`force-end` は高難易度・TA・耐久の本番中だけ使用できます。`restart` は進行中の現在記録を破棄する可能性があります。迷った場合は、まず `collect` で参加者を回収し、ログと大会状態を確認してください。

高難易度だけを再試合に戻す専用操作もあります。

```text
/beat competition restart high
/beat competition restart high confirm
```

## 7. 補足

### 7.1 よく使うコマンド

| 目的 | コマンド |
|---|---|
| 管理GUI | `/beat` |
| 大会状態 | `/beat competition status` |
| 参加者一覧 | `/beat players` |
| 全マップ検証 | `/beat setup validate all` |
| 競技設定確認 | `/beat settings show` |
| Whitelist確認 | `/beat whitelist status` |
| 高難易度開始 | `/beat competition high start` |
| TA開始 | `/beat competition ta start` |
| 耐久開始 | `/beat competition endurance start` |
| 大会初期化 | `/beat event reset` |
| 設定再読込 | `/beat reload` |

### 7.2 リハーサルとデバッグ

デバッグ機能は本番前のコピー環境で使用してください。

```text
/beat debug enable
/beat debug time status
/beat debug time advance 30s
/beat debug time next
/beat debug sound elimination
/beat debug high-practice skip
/beat debug disable
```

`debug time` は進行中の実競技時計を前へ進めます。時間を巻き戻すことはできません。`next` は次の脱落または終了の10秒前へ移動します。本番開始前にはDebugが無効であることを確認してください。

### 7.3 設定ファイル

通常運用で直接編集するのは `admins.json` と `participants.json` が中心です。

| ファイル | 内容 |
|---|---|
| `competition-settings.yml` | 制限時間、脱落時刻、TA生存人数 |
| `config.yml` | アイテム、判定幅などの基本設定 |
| `styles.yml` | 音、色、パーティクル、表示時間 |
| `messages.yml` | 表示メッセージ |
| `maps/*.yml` | setupで登録したマップ情報 |
| `data/event-state.json` | 現在の大会状態 |
| `data/results.json` | 競技結果と総合結果 |

YAMLやJSONを手編集した場合は、競技が進行していない状態で `/beat reload` を実行します。マップ設定は手編集せず、ゲーム内setupを使用してください。

大会データの初期化時には `plugins/BEAT/backups/` に既存データのバックアップが作成されます。それでも、大会前後にはプラグインフォルダーとワールドをサーバー外へコピーして保管することを推奨します。
