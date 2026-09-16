# BEAT Minecraft Plugin 実装指示書

## 0. 文書の目的

本書は、Minecraft Java Edition 26.2 上で開催予定のアスレチック大会 **BEAT** を運営するための Spigot プラグインの実装指示書である。

実装は Codex を用いて行う想定とする。

この文書では、単なる大会ルールの説明ではなく、以下を実装レベルまで定義する。

- システム全体の責務
- 競技ごとの状態遷移
- 参加者・運営・結果データの持ち方
- ランキング計算
- UI 表示
- Minecraft 内でのマップ設定
- 運営 GUI
- コマンド体系
- 設定ファイルの責務分離
- Whitelist 管理
- 結果修正
- 再試合
- 境界 tick の優先順位
- 再接続時の扱い
- デバッグ / 自動シナリオテスト
- 本番データ保護

大会ルール本文と本書に差異がある場合、**本書に記載された最新仕様を実装上の正とする**。

---

# 1. 開発前提

## 1.1 対象環境

- Minecraft Java Edition 26.2
- Spigot 26.2
- 1つの Minecraft サーバー内で大会全体を完結させる
- `online-mode=true` を必須とする
- プレイヤー本人識別は UUID を使用する
- 大会専用サーバーを想定する

起動時または大会開始前 validation で `online-mode=false` を検出した場合は ERROR とし、大会開始を拒否する。

## 1.2 外部依存

プレイヤー表示 OFF 時でも Tab ランキングを維持する必要があるため、標準 Spigot API の `hidePlayer` だけに依存しないこと。

必要に応じて ProtocolLib 等のパケット制御を使用し、

- ワールド内の他参加者の姿は非表示
- Tab には参加者を残す

を両立する。

ただし、競技ロジックそのものは外部依存へ結合させない。

## 1.3 基本思想

プラグインは「豪華な演出」よりも、

1. 競技ロジックが正確である
2. 当日の事故に強い
3. 1人でも十分デバッグできる
4. 座標設定・見た目調整をしやすい
5. 緊急時は GUI が壊れてもコマンドから操作できる

ことを優先する。

---

# 2. 設計原則

## 2.1 競技ロジックと表示を分離する

ランキング計算、タイブレーク、脱落判定、総合順位計算などの競技ロジックは、可能な限り Bukkit / Spigot API に依存しない純粋 Java クラスとして実装する。

目的:

- JUnit 等で単体テスト可能にする
- 仮想参加者を使ったシナリオテストを容易にする
- Player オブジェクトなしでも順位計算できるようにする

## 2.2 Competitor と Player を分離する

競技参加者の内部モデルは `Player` そのものにしない。

概念上、以下のような構造とする。

```text
Competitor
- uuid
- tournamentName
- onlinePlayerReference?  // オンライン時のみ
- competition states
- results
- flags
```

これにより、デバッグ用仮想参加者も同じランキングロジックに参加できるようにする。

## 2.3 ハードコードしないもの

以下は原則として設定ファイルへ分離する。

- 文言
- 色
- 太字等の装飾
- 音
- 音量
- pitch
- ActionBar 一時表示時間
- Particle
- Particle 表示時間
- アイテム Material
- アイテム名
- Lore
- 使用スロット
- 制限時間
- 脱落時刻
- TA の生存人数
- Spot ポイント
- 各種座標
- 判定範囲
- 落下 Y

一方、以下はコードに固定する。

- 順位計算方式
- タイブレーク
- 競技状態遷移
- PB 更新ロジック
- Progress 更新ロジック
- 総合順位計算
- UUID 本人識別
- 同 tick 優先順位

「何を、どこで、いつ、どう表示するか」は設定可能にするが、「競技ルールそのものを YAML で組み替える」ような過剰な汎用化はしない。

---

# 3. 推奨ディレクトリ構成

```text
plugins/BEAT/
├─ config.yml
├─ messages.yml
├─ styles.yml
├─ participants.json
├─ admins.json
│
├─ maps/
│  ├─ high-difficulty.yml
│  ├─ time-attack.yml
│  └─ endurance.yml
│
├─ data/
│  ├─ event-state.json
│  └─ results.json
│
├─ backups/
│  └─ ...
│
└─ logs/
   └─ result-edits.log
```

責務:

- `config.yml`
  - 一般設定、時間、アイテム設定、動作設定
- `messages.yml`
  - 全文言
- `styles.yml`
  - 色、音、Particle、表示時間
- `participants.json`
  - 参加者 UUID と補助 MCID
- `admins.json`
  - 運営 UUID と補助 MCID
- `maps/*.yml`
  - Minecraft 内 setup 機能で書き換えるマップ座標
- `event-state.json`
  - 大会全体状態
- `results.json`
  - 競技確定結果と総合結果
- `logs/result-edits.log`
  - 手動結果変更ログ

マップ座標ファイルは、原則手書きではなくゲーム内 setup 機能が生成・更新する。

---

# 4. 参加者・運営管理

## 4.1 participants.json

例:

```json
[
  {
    "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "mcid": "PlayerA"
  }
]
```

`mcid` は本人識別用ではない。

用途:

- Whitelist 同期補助
- 人間による確認
- 初回ログイン前の名前表示補助

本人識別は常に UUID を使用する。

## 4.2 大会時 MCID

ランキング・結果に使用する「大会時 MCID」は、参加者が大会サーバーへ初回ログインした時点の `Player#getName()` を保存し、その大会中は固定する。

participants.json の `mcid` と現在 MCID が異なっていても、UUID が一致すれば参加可能。

この場合は運営へ警告を出す。

```text
[BEAT] 登録MCIDと現在MCIDが異なります:
OldName -> NewName
```

## 4.3 admins.json

例:

```json
[
  {
    "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "mcid": "AdminName"
  }
]
```

BEAT 管理者権限は admins.json で決める。

- OP であるだけでは BEAT 管理者権限を与えない
- Console は全管理コマンドを使用可能
- admins.json の UUID は競技参加者として扱わない

## 4.4 JSON validation

reload 時に以下を検査する。

- UUID の形式
- UUID 重複
- participants / admins 両方への同一 UUID 登録
- 空の名前
- 明らかな不正 JSON

重大エラー時:

- 新データを適用しない
- 最後に正常に読み込めた状態を保持する
- 運営へ ERROR を出す

---

# 5. Whitelist

## 5.1 モード

2モードを持つ。

```text
ADMIN_ONLY
ALL
```

### ADMIN_ONLY

admins.json の UUID のみ Whitelist へ完全同期。

### ALL

admins.json + participants.json を Whitelist へ完全同期。

## 5.2 コマンド

```text
/beat whitelist admins
/beat whitelist all
/beat whitelist status
```

完全同期とは、対象 JSON に存在しない Whitelist 登録者を削除することを意味する。

ただし、同期時点ですでにオンラインの参加者は Kick しない。

Whitelist モードは保存し、再起動後も保持する。

JSON reload だけでは Whitelist を自動同期しない。

---

# 6. 大会全体状態

推奨 enum:

```text
WAITING
HIGH_PRACTICE_COUNTDOWN
HIGH_PRACTICE
HIGH_PREPARE
HIGH_RUNNING
HIGH_FINISHED
TA_READY
TA_COUNTDOWN
TA_RUNNING
TA_FINISHED
ENDURANCE_READY
ENDURANCE_COUNTDOWN
ENDURANCE_RUNNING
ENDURANCE_FINISHED
OVERALL_READY
OVERALL_CONFIRMED
```

必要に応じて内部用に再試合待機状態等を追加してよい。

通常 GUI では状態順序を飛ばせない。

例:

```text
HIGH_FINISHED
↓
[TAへ移行]
TA_READY
↓
[TA開始]
TA_COUNTDOWN
↓
TA_RUNNING
```

「次競技へ移行」は明示的操作とし、移行しただけでは自動開始しない。

---

# 7. 競技時間の扱い

## 7.1 すべてサーバー tick 基準

- 20 tick = 1秒
- TPS 低下時でも実時間補正しない
- TA 走行タイムも tick 差で計測する

TA 表示は 1/100 秒単位だが、実際の分解能は 0.05 秒。

例:

```text
247 tick = 12.35秒
```

## 7.2 境界 tick

各重要時刻は「その tick まで有効」。

例:

- 10:00 ちょうどの Course 到達 -> セーフ
- 20:00 ちょうどの Zone 到達 -> セーフ
- 20:00 ちょうどの TA Goal -> 記録反映後に脱落判定
- 30:00 ちょうどの Spot / Progress / Goal -> 有効
- 30:00 を 1 tick 超えたもの -> 無効

## 7.3 同 tick 基本順序

```text
到達・記録イベント処理
↓
順位再計算
↓
脱落判定
↓
競技終了処理
```

イベントリスナの呼び出し順に結果が依存しないよう、競技時計を基準に設計する。

---

# 8. 共通 UI 原則

役割を以下で統一する。

- Bossbar
  - 残り時間
  - 次の脱落まで
- ActionBar
  - 自分の通常情報
  - 一時的な本人向けフィードバック
- Sidebar
  - 上位ランキング、最大10人
- Tab
  - オンライン参加者の全ランキング
- Chat
  - 大会全体通知
  - 脱落カウント
  - 結果発表
- Sound
  - カウント、更新、Goal 等
- Title
  - 原則使用しない

表示色・音・文言は設定ファイル化する。

## 8.1 UI イベント優先順位

同時に複数表示要求が発生した場合:

```text
Goal / 全完走
>
Zone / Course Clear
>
PB / 記録更新
>
Split / Spot / Progress
>
通常表示
```

---

# 9. 共通プレイヤー状態

競技開始時に参加者を正規化する。

- Adventure
- Health 最大
- Food 最大
- Potion Effect 全削除
- FireTicks 解除
- Velocity = 0
- Inventory 整理

競技中:

- 空腹減少を無効化
- ダメージを無効化
- 参加者同士の collision を無効化
- 競技進行中のアイテムドロップを無効化

運営の Inventory は変更しない。

---

# 10. プレイヤー表示切替

参加者は専用アイテムで他参加者の表示 ON / OFF を切り替えられる。

ActionBar:

```text
プレイヤー表示: ON
プレイヤー表示: OFF
```

常時 ActionBar は、ラベル・値・順位・区切りを項目ごとに色分けする。
一時通知は各通知本文が持つ元の色を維持する。

要件:

- 運営は非表示対象にしない
- 新しくログインした参加者にも状態を適用
- ワールド上の姿のみ非表示
- Tab ランキングには残す
- 再接続中は大会セッション内で設定保持してよい
- サーバー再起動では ON に戻ってよい

---

# 11. 高難易度

## 11.1 構成

- 練習 10分
- 準備 1分
- 本番 30分
- Course 1〜5
- Spot +50pt
- Course Goal +100pt

次 Course への物理的アクセス制御はマップ側で行う。

プラグインは「前 Course 未クリアなら侵入禁止」といった制御を持たない。

## 11.2 判定領域

以下は「水平な長方形ブロック範囲」を踏んだときに判定する。

- Spot
- Goal

範囲指定方法は後述の setup 機能を使用する。

旧 map YAML の `entry` は廃止し、設定から削除する。

## 11.3 Spot 順序

各 Course の Spot に順序番号を持つ。

例:

```text
Course 2
Spot 1
Spot 2
Spot 3
Goal
```

Spot を踏み忘れて後ろの Spot に到達した場合、そこまでの全 Spot を取得済みとしてポイントを引き上げる。

例:

Spot1 未取得で Spot2 到達:

```text
0 -> 100pt
```

Goal 到達時は、その Course の未取得 Spot をすべて取得済み扱いにし、さらに Goal +100pt を付与する。

したがって、全 Course 完走者は全 Spot 取得済みとなる。

ポイント加算イベントを積み上げるというより、コースごとの最高進度から取得ポイントを導出する設計を推奨する。

## 11.4 タイブレーク

順位:

1. ポイントが多い
2. 同ポイントなら、そのポイントに最後に到達した大会経過 tick が早い
3. tick まで同じなら同率

0pt が複数人なら同率。

## 11.5 練習

### 開始

運営操作
↓
参加者を練習開始地点へ
↓
10秒移動固定カウント
↓
練習開始

カウント中:

- 視点移動可能
- XYZ 移動不可
- 毎秒 Chat + Sound

### 練習用飛行

Adventure のまま `allowFlight` を切り替える専用アイテムを配布。

Hotbar は個人 CP を slot 0、飛行切替を slot 1 とする。

使用:

```text
飛行: ON
飛行: OFF
```

ActionBar に短時間表示。

切替時に Sound を再生する。

練習終了時:

- 飛行強制 OFF
- allowFlight OFF
- アイテム削除

### 練習用個人 CP

専用アイテム1個で完結する。

- Q で捨てる操作 -> CP 設定
- アイテム使用 -> CP へ復帰
- 実際にはアイテムは失わない

CP 保存内容:

- X/Y/Z
- Yaw/Pitch

保存しない:

- 飛行状態
- Velocity
- その他状態

CP は1個のみ。新規設定で上書き。

空中では設定不可。

本人 Chat:

```text
CPを設定しました
```

設定成功時に Sound を再生する。CP 復帰時には再生しない。

空中時:

```text
地面にいるときのみCPを設定できます
```

CP 復帰成功時は通知不要。

復帰時:

- Velocity = 0
- 現在の飛行 ON/OFF 状態維持

Course をまたいでも CP は残す。

練習終了時に CP を削除。

練習中の Spot / Goal は公式ポイント、順位、到達 tick に影響させない。

## 11.6 練習終了 -> 準備

練習終了時:

- 練習アイテム回収
- 飛行解除
- 個人 CP 削除
- Velocity = 0
- 専用準備待機地点へ TP

準備時間 1分。

Bossbar:

```text
準備 残り時間 00:42
```

最後10秒は Chat + Sound。

本番開始時に Course 1 開始地点へ TP。

## 11.7 本番復帰アイテム

本番では「現在 Course の開始地点へ戻る」アイテムを持つ。

Course Goal を踏むと `currentCourse` を次 Course へ更新する。

更新時は本人へ `CPがNに更新されました` と表示する。Course 5 Goal で完走した場合は
復帰アイテムを削除し、以後使用できない。

復帰アイテム使用:

- Velocity = 0
- currentCourse の startLocation へ TP
- CPへ戻った際の通知は表示しない

高難易度では落下による自動復帰を行わない。

## 11.8 本番 UI

Bossbar:

```text
残り時間 18:42 ｜ 次の脱落 03:42
```

脱落予定なし:

```text
残り時間 04:32 ｜ 次の脱落 --:--
```

ActionBar:

```text
Point 0450 ｜ #07 ｜ Course 03
```

Sidebar:

```text
HIGH DIFFICULTY

#01 0950pt PlayerA
#02 0900pt PlayerB
...
```

最大10人。

Tab:

オンライン参加者全員をランキング順表示。

## 11.9 Spot / Goal 本人フィードバック

Spot 到達時、ActionBar を約3秒差し替える。

```text
Spot 2 到達 ｜ 0450pt (+50)
```

踏み忘れ補完時:

```text
Spot 2 到達 ｜ 0100pt (+100)
```

軽い Sound。

Goal:

```text
Course 3 Clear! ｜ 0700pt (+200)
```

Spot より明確な Sound。

通知中も更新間隔ごとに同じ本文を再送し、薄く消え始める前に表示を維持する。
通知期限のtickでは空白を挟まず通常 ActionBar に切り替える。

## 11.10 全 Course 完走

Course 5 Goal 時:

本人 ActionBar を約3秒:

```text
ALL CLEAR! ｜ 0950pt ｜ #01
```

強めの達成 Sound。

全体 Chat:

```text
PlayerA が全コースクリア！ 0950pt (#1)
```

順位変動の有無に関係なく必ず通知。

Goal 直後の TP / 待機場所処理はプラグインの責任外。

ただし30:00終了時には Goal 済みを含め全員回収する。

## 11.11 更新通知

ポイント更新かつ順位が変動した場合のみ全体 Chat。

```text
PlayerA が更新: 450pt (#3)
```

## 11.12 脱落

- 10:00 Course2 未到達
- 15:00 Course3 未到達
- 20:00 Course4 未到達
- 25:00 Course5 未到達
- 30:00終了

10秒前から毎秒:

```text
コース2未到達者脱落まで... 10
...
```

Sound も毎秒。脱落時刻および30:00の0秒到達時にも専用Soundを再生する。

脱落時:

```text
コース2未到達者脱落！
```

脱落者:

- Spectator
- 競技アイテム削除
- 記録固定
- 以後ポイント更新不可

本人:

```text
脱落しました。最終記録: 450pt (#14)
```

この順位は最終順位として確定する。

## 11.13 終了

30:00 tick の到達処理を先に行う。

終了後:

- 結果自動確定
- 全員指定終了地点へ TP
- Adventure
- 競技アイテム削除
- Bossbar / ActionBar / Sidebar / Tab ランキング解除

結果表示は運営操作。

```text
―― HIGH DIFFICULTY RESULT ――

#01 0950pt PlayerA
#02 0900pt PlayerB
...
```

全参加者一括表示。

---

# 12. タイムアタック

## 12.1 基本

- 本番30分
- 練習なし
- Start / Split / Goal は水平長方形ブロック範囲
- Start / Split / Goal の踏み判定は設定範囲から X/Z 各0.3 block拡張する
- Yはプレイヤーの足元直下が登録範囲のブロックYと一致したときに判定する
- タイム = Goal tick - Start tick
- 何度でも挑戦可能
- PB が速い順

## 12.2 restartLocation

Start 判定範囲とは別に restartLocation を持つ。

Goal 後とリスタートアイテム使用時はここへ TP。

restartLocation が Start 範囲内の場合は validation ERROR。

## 12.3 開始

開始操作:

1. 全参加者を restartLocation へ TP
2. Adventure
3. 状態初期化
4. リスタートアイテム配布
5. 10秒移動固定
6. Chat + Sound カウント
7. `競技開始！`
8. 移動解除

Start 判定は競技開始後のみ有効。

## 12.4 Start

「Start 領域外 -> Start 領域内」へのプレイヤー自身の進入で計測開始。

Start 領域内に立ち続けても再計測しない。

走行中に一度 Start 外へ出て、再度 Start へ入れば自動リスタート。

新 Start 時:

- 前走行破棄
- 前走行 Split 破棄
- startTick 更新
- running = true
- Start 到達 Sound

## 12.5 リスタートアイテム

使用時:

- Velocity = 0
- restartLocation へ TP
- その時点では内部走行データを破棄しない
- ActionBar 上の Time は `00.00` に戻す

次にプレイヤー自身が Start へ再進入した時点で前走行を破棄し、新走行開始。

## 12.6 Split

Split 1 / Split 2 を持つ。

Split は飛ばしてよい。

- Split1 未通過で Split2 -> Split2 を記録してよい
- Split 全未通過でも Goal は有効

同じ走行で同じ Split を複数回通過した場合は最初の1回のみ記録。

最初の Split 到達受理時に Sound を再生する。

PB 更新時は以下も保存。

- PB 時 Split1
- PB 時 Split2

未通過なら null / no record。

## 12.7 Goal

有効な走行中に Goal 到達:

- Goal タイム確定
- PB 判定
- 順位再計算
- running=false
- restartLocation へ自動 TP
- 次走は自分で Start に入る

プラグイン TP により Start 領域へ入っても計測開始しない。

## 12.8 PB タイブレーク

1. PB が速い
2. 同タイムなら、その PB を記録した大会経過 tick が早い
3. 同じ tick に同じタイムを記録した場合、Goal 記録受理順 `recordSequence` が早い方を上位

TA では完全同率を作らず、足切り時に必ず指定人数を残せるようにする。

記録なし同士のみ全員同率最下位。

## 12.9 ActionBar

通常:

```text
Time 12.35 ｜ PB 31.80 ｜ #07 ｜ Border #10 32.15
```

固定桁を意識する。

タイムは `00.00` 形式。

未記録:

```text
Time 00.00 ｜ PB --.-- ｜ #-- ｜ Border #10 --.--
```

## 12.10 Split ActionBar

Split 通過後、約3秒通常 ActionBar を差し替える。

```text
Split 1 10.25 ｜ PB 10.40 ｜ -0.15
```

PB Split が存在しない:

```text
Split 1 10.25 ｜ PB --.-- ｜ --.--
```

その後通常表示へ戻す。

## 12.11 Goal 時本人表示

PB 更新時:

```text
PB更新! 30.90 ｜ #05
```

PB 更新音。

PB を更新しなかった有効 Goal では、PB 更新音とは異なる完走 Sound を再生する。

順位変動した場合のみ全体 Chat:

```text
PlayerA が更新: 30.90 (#5)
```

PB 非更新時:

```text
Time 31.95 ｜ PB 31.80 ｜ +0.15
```

約3秒 + PB更新時とは異なる軽い Sound。

## 12.12 Bossbar

```text
残り時間 18:42 ｜ 次の脱落 08:42
```

最後の脱落後:

```text
残り時間 04:32 ｜ 次の脱落 --:--
```

制限時間終了前の最後10秒は毎秒 Chat + Sound でカウントダウンし、0秒到達時にも専用Soundを再生する。

## 12.13 Sidebar

上位最大10人。

```text
TIME ATTACK

#01 28.45 PlayerA
#02 29.10 PlayerB
...
```

## 12.14 Tab

オンライン参加者全員。

例:

```text
#01 28.45 PlayerA
#02 29.10 PlayerB
...
#-- --.-- PlayerU
[ADMIN] AdminName
```

Spigot の player list order を利用する。

仮想参加者は Tab には出さない。

## 12.15 脱落

正式な生存人数は設定値。

例:

- 20:00 -> 上位20人
- 25:00 -> 上位10人

カウント:

```text
21位以下脱落まで... 10
...
21位以下脱落！
```

毎秒 Sound。

記録なしは枠が余っても脱落。

大会失格者は元順位枠を保持するが、生存人数には数えない。

例:

#5 が失格で「有効参加者20人残す」場合、必要に応じて #21 まで生存対象にする。

脱落者:

- Spectator
- PB保持
- リスタートアイテム削除

本人:

```text
脱落しました。最終記録: 31.80 (#14)
```

## 12.16 30:00

30:00 ちょうど Goal は有効。

処理順:

1. Goal / Split 等
2. PB / 順位更新
3. 終了

終了後:

- 全員指定終了地点へ TP
- Adventure
- UI 削除
- アイテム削除
- 結果自動確定

## 12.17 結果表示

運営操作で全員一括 Chat。

```text
―― TIME ATTACK RESULT ――

#01 28.45 PlayerA
#02 29.10 PlayerB
#03 --.-- PlayerC
```

ページ送り不要。

---

# 13. 耐久

## 13.1 基本

- 30分
- Zone 1 / 2 / 3
- 多数の Progress
- Goal も最後の Progress として扱う

例:

```text
Progress 001
...
Progress 087
Progress 088 = Goal
```

## 13.2 Progress

コマンド実行時のプレイヤーの足元座標を中心点として保存する。

取得判定:

- 水平面は中心点から半径 0.25 block の円
- 垂直方向は中心点から上下 0.5 block
- 2 tick 継続、接地状態、進入方向、梯子状態などの追加条件は設けない

同じ Progress 番号には複数地点を登録できる。対称コースや分岐コースでは、
同番号のいずれか1地点へ入ればその Progress へ到達したものとする。

後ろの Progress を踏めば、途中を踏み忘れていてもその Progress 到達扱い。

保存:

- maxProgress
- maxProgressReachedTick

同 Progress の複数到達では最初の到達 tick を保持する。

プレイ中は各地点を半径 0.25 block の `DUST` 球としてプレイヤーごとに表示する。

- 取得済み: 緑
- 次に取得する Progress: 黄
- それより先: 水色

取得瞬間だけの追加Particleは表示しない。セットアップ確認表示も同じ半径のDUST球を使う。

## 13.3 Zone

Zone2 / Zone3 は「特殊属性を持つ Progress」として実装することを推奨する。

例:

```text
Progress 035 = Zone2
Progress 062 = Zone3
Progress 088 = Goal
```

Zone 到達時:

- Progress 更新
- zoneReached flag 更新
- 復帰地点切替

## 13.4 落下

競技設定の `fallY` 以下で落下判定。

落下時:

- Velocity = 0
- 最後に到達済み Zone の restartLocation へ TP

Zone1:
- startLocation

Zone2:
- zone2RestartLocation

Zone3:
- zone3RestartLocation

## 13.5 順位

1. maxProgress が大きい
2. 同 Progress なら、その Progress の初回到達 tick が早い
3. tick も同じなら同率

Goal も単なる最大 Progress として扱うため、Goal 到達者同士は Goal Progress 到達 tick で順位が決まる。

## 13.6 開始

TA と同様。

- startLocation へ TP
- Adventure
- 10秒移動固定
- Chat + Sound
- 競技開始

## 13.7 ActionBar

通常:

```text
Progress 047 ｜ #07 ｜ Zone 02
```

未記録:

```text
Progress 000 ｜ #-- ｜ Zone 01
```

## 13.8 Progress 更新

本人 ActionBar 約1秒:

```text
Progress 048 到達 ｜ #06
```

軽い Sound。

順位変動時のみ全体 Chat:

```text
PlayerA が更新: Progress 048 (#6)
```

## 13.9 Zone 到達

ActionBar:

```text
Zone 2 到達 ｜ Progress 035 ｜ #08
```

通常 Progress より分かりやすい Sound。

## 13.10 Goal

Goal = 最大 Progress。

本人 約3秒:

```text
GOAL! ｜ 18:42 ｜ #01
```

達成 Sound。

全体:

```text
PlayerA が完走！ 18:42 (#1)
```

順位変動に関係なく必ず通知。

Goal 直後の TP / GameMode 変更はプラグインでは行わない。

30:00では Goal 済みを含む全員を回収する。

## 13.11 Sidebar / Tab

全員 `Progress xxx` 表記で統一する。

完走者だけ `GOAL` 表記にはしない。

Sidebar は最大10人。

Zone 区切りで表示色を変更する。

4段階:

- Goal 到達者
- Zone3 到達者
- Zone2 到達者
- Zone2 未到達者

色判定は単なる Progress 番号ではなく、実際の Zone 到達 flag を使う。

色は styles.yml で変更可能。

## 13.12 脱落

20:00:

```text
Zone 2 未到達者脱落まで... 10
```

25:00:

```text
Zone 3 未到達者脱落まで... 10
```

毎秒 Chat + Sound。

脱落時刻の0秒到達時にも専用Soundを再生する。

脱落者:

- Spectator
- 記録固定

本人:

```text
脱落しました。最終記録: Progress 034 (#14)
```

## 13.13 終了

30:00 tick の到達処理を先に行う。

終了10秒前から毎秒 Chat + Sound でカウントし、0秒到達時にも専用Soundを再生する。

その後:

- 全員終了地点へ TP
- Adventure
- UI 削除
- 結果自動確定

結果表示:

```text
―― ENDURANCE RESULT ――

#01 Progress 088 PlayerA
#02 Progress 088 PlayerB
#03 Progress 083 PlayerC
...
```

---

# 14. 総合順位

## 14.1 対象条件

総合対象には3競技すべてへの参加が必要。

`participatedHigh`
`participatedTA`
`participatedEndurance`

のすべてが true であること。

加えて:

- `overallExcluded == false`
- `disqualified == false`

参加判定:

「その競技へ実際に参加可能な状態になった瞬間」に true。

例:

- 開始時オンライン -> true
- 遅刻したがまだ参加可能 -> true
- 最初の脱落時刻後に初ログイン -> false
- 0pt / --.-- / Progress000 でも参加した -> true
- 終始欠席 -> false

## 14.2 持ち点

```text
高難易度順位 * TA順位 * 耐久順位
```

小さいほど上位。

## 14.3 持ち点同値

各競技順位を良い順に並べる。

例:

```text
[1, 3, 4]
[2, 2, 3]
```

比較順:

1. 最も良い順位
2. 2番目
3. 3番目
4. 全部同じなら同率

## 14.4 表示

```text
―― OVERALL RESULT ――

#01 x6  PlayerA [2 * 1 * 3]
#02 x8  PlayerB [1 * 2 * 4]
#03 x12 PlayerC [2 * 2 * 3]
```

`x6` の正式名称は「持ち点」。

[] の順は:

```text
[高難易度 * TA * 耐久]
```

## 14.5 calculate / confirm

総合は自動確定しない。

```text
耐久終了
↓
異議申し立て対応
↓
必要な修正
↓
/beat overall calculate
↓
確認
↓
/beat overall confirm
```

confirm 後は通常操作では変更不可。

必要なら明示的な解除/再計算操作を別途用意する。

---

# 15. 大会失格・総合対象外

## 15.1 総合対象外

手動:

```text
/beat player overall-exclude <player>
/beat player overall-include <player>
```

## 15.2 大会失格

```text
/beat player disqualify <player>
/beat player undisqualify <player>
```

競技中に失格した場合:

- 即 Spectator
- 競技参加停止
- 総合対象外

ただし他参加者の順位を繰り上げない。

例:

```text
#01 A
#02 B
#03 C
```

B失格:

```text
#01 A
失格 B
#03 C
```

失格者の元記録と元順位枠は内部に保持する。

総合ランキングには掲載しない。

---

# 16. 結果修正

順位を直接編集しない。

運営が編集するのは元記録のみ。

その後、順位再計算する。

## 16.1 高難易度

```text
/beat result high edit <player> points <value>
/beat result high edit <player> reached-tick <value>
```

## 16.2 TA

```text
/beat result ta edit <player> time 31.80
/beat result ta edit <player> time none
/beat result ta edit <player> split 1 10.25
/beat result ta edit <player> split 2 21.40
/beat result ta edit <player> reached-tick <value>
```

0.05秒単位で表現できないタイムは拒否。

## 16.3 耐久

```text
/beat result endurance edit <player> progress 57
/beat result endurance edit <player> reached-tick <value>
```

存在しない Progress は拒否。

## 16.4 修正フロー

```text
edit
↓
結果を未確定へ
↓
recalculate
↓
show
↓
confirm
```

例:

```text
/beat result ta recalculate
/beat result ta show
/beat result ta confirm
```

## 16.5 Undo

直前の手動結果編集1件を戻せる。

```text
/beat result undo
```

失格等は専用解除コマンドを使うため Undo 対象外。

## 16.6 ログ

`logs/result-edits.log`

例:

```text
2026-xx-xx 20:41:15
Admin: AdminName
Competition: TA
Player: PlayerA
Field: time
Before: 32.15
After: 31.80
```

以下も記録:

- overall exclude/include
- disqualify/undisqualify
- result confirm
- overall confirm

---

# 17. 再試合

ルール上の再試合を正式機能として持つ。

```text
/beat competition restart high
/beat competition restart ta
/beat competition restart endurance
```

実行時は二重確認。

## 17.1 共通

最初に競技イベント受付を停止してから初期化する。

順序:

1. event acceptance stop
2. timer/task stop
3. UI stop
4. current records discard
5. elimination reset
6. spectator reset
7. participants reset
8. READY state へ戻す

自動再開はしない。

## 17.2 高難易度

- 練習中問題 -> 練習からやり直し
- 本番中問題 -> 本番のみやり直し

本番のみ再試合時は練習再実施しない。

---

# 18. 再接続・途中参加

## 18.1 初参加

競技中に初ログイン:

### 高難易度本番
- まだ脱落条件に引っかからない時刻なら Course1 から参加
- participatedHigh = true

### TA
- restartLocation
- まだ脱落済みでなければ参加

### 耐久
- startLocation
- まだ脱落済みでなければ参加

すでに最初の脱落時刻を過ぎ、条件未達が確定している場合:

- 参加不可
- Spectator
- participated は false のまま

## 18.2 高難易度練習

練習中初ログイン:

- 練習用アイテム配布
- 残り時間のみ参加

準備中:

- 準備待機地点へ TP
- 練習アイテムなし

## 18.3 再接続

一度参加した後の切断は途中参加ではなく復帰。

- 記録保持
- 生存なら復帰
- オフライン中に脱落条件成立なら Spectator
- 競技終了済みなら終了地点 + Adventure
- 必要アイテム再支給

TA の走行中切断:

- startTick 保持
- オフライン中も競技時計は進む
- 再接続後、走行状態を維持
- Goal すれば切断中時間も含む

---

# 19. サーバークラッシュ・再起動

競技途中状態の完全復元は不要。

保存するのは:

- 大会全体のどこまで進んでいたか
- 確定済み競技結果
- 重要フラグ

クラッシュ後:

### TA中
TA_READY 相当へ戻し、TA を再試合。

### 耐久中
ENDURANCE_READY 相当へ戻し、再試合。

### 高難易度練習中
練習からやり直し。

### 高難易度本番中
練習済みを保持し、本番再試合待ちへ。

確定済み過去競技結果は消さない。

---

# 20. マップセットアップ機能

## 20.1 基本思想

座標・範囲は YAML 手書きではなく Minecraft 内で設定可能にする。

WorldEdit 風の選択ツールを使用する。

## 20.2 Wand

```text
/beat setup wand
```

アイテム名例:

```text
BEAT 範囲選択ツール
```

Lore:

```text
左クリック: Pos1を設定
右クリック: Pos2を設定
選択後、/beat setup ... で登録します。
```

Material / 名前 / Lore は設定可能。

PersistentDataContainer 等で専用品として識別する。

### 操作

- ブロック左クリック -> Pos1
- ブロック右クリック -> Pos2
- 通常ブロック破壊/使用はキャンセル
- Entity 攻撃等も抑止
- 耐久値消費なし
- Q で普通に捨ててよい
- Inventory 内に既所持なら重複配布しない

### 選択状態

管理者ごとに別。

- logout で消える
- restart で消える
- reload で消える

永続化不要。

## 20.3 範囲

踏み判定は水平な長方形ブロック範囲。

保存形式例:

```yaml
world: beat
y: 64
min-x: 120
max-x: 124
min-z: -35
max-z: -31
```

Pos1 / Pos2 の順方向は問わず正規化する。

Pos1.y != Pos2.y でも選択自体は許可するが、登録時 ERROR。

別 world の Pos2 は拒否し、Pos1 を保持。

登録成功後は選択を自動クリアする。

## 20.4 単一地点

現在立っている位置・向きを登録する。

保存:

- world
- x/y/z
- yaw/pitch

---

# 21. Setup コマンド

基本:

```text
/beat setup <competition> <target> <operation> [...]
```

語彙を以下に寄せる。

```text
add
set
remove
show
info
list
validate
```

日本語 help と Tab 補完を必須とする。

## 21.1 共通

```text
/beat setup wand
/beat setup clear
/beat setup validate <competition>
/beat setup show ...
/beat setup info ...
/beat setup list ...
/beat setup help
/beat setup help high
/beat setup help ta
/beat setup help endurance
```

## 21.2 高難易度

```text
/beat setup high spot add <course>
/beat setup high spot set <course> <spot>
/beat setup high spot remove <course> <spot>

/beat setup high goal set <course>
/beat setup high goal remove <course>

/beat setup high start set <course>
/beat setup high start remove <course>

/beat setup high prepare set
/beat setup high end set
```

Spot add は次番号を自動採番。

## 21.3 TA

```text
/beat setup ta start set
/beat setup ta split add
/beat setup ta split set <number>
/beat setup ta split remove <number>
/beat setup ta goal set
/beat setup ta restart set
/beat setup ta end set
```

Split 内部構造は可変個数対応。

## 21.4 耐久

```text
/beat setup endurance progress add
/beat setup endurance progress add <number>
/beat setup endurance progress set <number>
/beat setup endurance progress remove <number>
/beat setup endurance progress remove <number> <location-index>

/beat setup endurance goal set

/beat setup endurance zone set 2 <progress>
/beat setup endurance zone set 3 <progress>

/beat setup endurance zone-restart set 2
/beat setup endurance zone-restart set 3

/beat setup endurance start set
/beat setup endurance end set

/beat setup endurance fall-y set here
/beat setup endurance fall-y set <y>
```

引数なしの Progress add と Goal set は現在最大 Progress + 1 として登録する。
`progress add <number>` は指定Progressへ現在地を追加し、`progress set <number>` は
指定Progressの全地点を現在地1件で置き換える。
地点番号は登録順の1始まりとする。

---

# 22. Setup show / info / list

## 22.1 show

例:

```text
/beat setup show high spot 3 2
/beat setup show endurance progress 47
/beat setup show high all
/beat setup show high course 3
/beat setup show endurance all
/beat setup show endurance progress all
/beat setup show endurance off
/beat setup show ta all
```

Particle はコマンド実行者本人にのみ表示。
耐久の `all` は追加地点も反映しながら継続表示し、`off` で停止する。

大量表示時:

- 外周を中心に描画
- Particle 間引き
- 1 tick あたり上限
- 表示時間上限
- 種類ごとに色/Particleを分ける
- 設定可能

## 22.2 info

```text
/beat setup info endurance progress 47
```

例:

```text
Progress 047
World: beat
X: 120 - 124
Y: 64
Z: -30 - -27
```

## 22.3 list

```text
/beat setup list endurance progress
```

例:

```text
Progress: 001-046, 048-072
Missing: 047
Goal: 073
```

高難易度 Spot も同様。

---

# 23. Setup validation

validation は単なる必須項目チェックではなく整合性検査まで行う。

結果:

```text
Validation: PASSED
```

または

```text
Validation: FAILED
Errors: 2
Warnings: 1
```

## 23.1 ERROR

ERROR が1件でもあれば競技開始拒否。

例:

- 必須地点なし
- Progress 連番抜け
- Spot 連番抜け
- Split 連番抜け
- 存在しない world
- 水平でない範囲
- restartLocation が Start 内
- Zone Progress が存在しない
- Zone2 >= Zone3
- Zone3 >= Goal
- online-mode=false

## 23.2 WARNING

開始可能だが要確認。

例:

- 判定範囲同士が不自然に重複
- 非必須設定の欠落
- 同位置に複数判定

## 23.3 連番

以下は抜け禁止。

- Course 1〜5
- 各 Course Spot
- TA Split
- Endurance Progress

例:

```text
Progress 001
Progress 002
Progress 004
```

なら:

```text
[ERROR] Progress 003 がありません
```

---

# 24. 運営 GUI

Inventory GUI を使用。

```text
/beat
/beat menu
```

内部処理は必ずコマンド/サービス層と共通化し、GUI 固有ロジックを作らない。

## 24.1 メイン

例:

```text
高難易度
TA
耐久
結果
総合
Players
Whitelist
Setup
緊急操作
```

## 24.2 開始確認

開始ボタン押下後、即開始しない。

例:

```text
TAを開始しますか？

参加者: 21
Online: 20

20:00 -> 21位以下脱落
25:00 -> 11位以下脱落

[開始する]
[戻る]
```

開始前に自動 validate。

ERROR -> 開始不可。

WARNING -> 警告表示後に続行可能。

## 24.3 緊急操作

通常画面と分離する。

- 強制終了
- 再試合
- 参加者全回収
- フェーズ中止

危険操作は二段階確認。

---

# 25. 主な運営コマンド

最終的な詳細と help 文言は実装時に整理してよいが、最低限以下の責務を持つこと。

```text
/beat
/beat menu
/beat reload

/beat players
/beat player info <player>

/beat player overall-exclude <player>
/beat player overall-include <player>
/beat player disqualify <player>
/beat player undisqualify <player>

/beat whitelist admins
/beat whitelist all
/beat whitelist status

/beat competition ...
/beat result ...
/beat overall calculate
/beat overall confirm
/beat overall result

/beat setup ...
/beat debug ...
```

すべて日本語 help を用意する。

Tab 補完は、存在する Course / Spot / Progress / player 等を可能な限り候補表示する。

---

# 26. 結果 JSON の概念 schema

厳密 schema は実装側で決めてよいが、以下の情報を欠落させないこと。

## 26.1 プレイヤー共通

```text
uuid
tournamentName
participatedHigh
participatedTa
participatedEndurance
overallExcluded
disqualified
```

## 26.2 高難易度

```text
rank
points
finalPointTick
```

## 26.3 TA

```text
rank
pbTicks | null
pbRecordedTick | null
pbRecordSequence | null
pbSplit1Ticks | null
pbSplit2Ticks | null
```

## 26.4 耐久

```text
rank
maxProgress
progressReachedTick
zone2Reached
zone3Reached
goalReached
```

## 26.5 総合

```text
rank
scoreProduct
highRank
taRank
enduranceRank
confirmed
```

---

# 27. データ保存タイミング

重要操作ごとに即保存。

- 競技終了
- 結果確定
- 結果修正
- 結果再確定
- overall exclude/include
- disqualify/undisqualify
- 総合 confirm
- 大会状態遷移
- Whitelist モード変更

サーバー停止時保存だけに依存しない。

競技中の毎 tick 状態はディスク保存不要。

---

# 28. Event reset

大会データを初期化する管理機能を持つ。

```text
/beat event reset
```

二重確認必須。

リセット対象:

- 大会進行状態
- 競技結果
- 大会時 MCID snapshot
- 総合状態
- 失格 / overall exclude 等

残す:

- マップ設定
- messages/styles/config
- participants/admins

実行前に results / event-state を backups へコピーする。

---

# 29. 同 tick 境界処理

## 29.1 共通

時間境界:

```text
記録イベント
↓
順位更新
↓
脱落
↓
終了
```

同 tick で複数到達地点に該当した場合、最も進んだ状態を採用し、通知は可能な限り1回にまとめる。

## 29.2 高難易度

- Spot + Goal 同 tick -> Goal まで取得
- Goal + 脱落時刻 -> GoalとCP更新後、脱落判定
- Course5 Goal + 30:00 -> ALL CLEAR 成立後、全員回収

## 29.3 TA

- Goal + 20/25分 -> Goal反映後、脱落判定
- Goal + 30:00 -> Goal有効後、終了
- Split + Goal 同 tick -> Split も保存
- restart item + Goal 同 tick -> Goal優先
- disconnect + Goal -> Goalイベントが先に成立していれば有効
- Start + 30:00 -> 開始可能だが同 tick 終了
- プラグイン TP で Start へ入っても計測開始しない

## 29.4 耐久

- Progress + 落下同 tick -> Progress反映後、復帰
- Zone + 落下 -> Zone/Progress更新後、新Zone復帰地点へ
- Goal + 落下 -> Goal優先。通常落下復帰しない
- Zone + 脱落時刻 -> Zone反映後判定
- Goal + 30:00 -> Goal成立後、全員回収

---

# 30. プラグイン TP と踏み判定

プラグイン自身が行う TP により、

- Start
- Spot
- Progress
- Goal
- Zone

範囲へ入っても、その TP 自体では到達判定しない。

例:

- restartLocation
- Zone復帰
- 準備TP
- 終了TP
- Course start TP

プレイヤー自身の移動により「踏んだ」時だけ判定する。

---

# 31. GameMode 異常

競技中に運営が手動で GameMode を変更した場合、毎 tick 強制復旧しない。

代わりに:

- 不正状態を検出
- 運営へ WARNING
- 必要なら復旧コマンド/GUIを使用

デバッグや緊急対応を妨げないこと。

---

# 32. デバッグモード

## 32.1 目標

**実プレイヤー1人で、20〜50人大会のランキング・脱落・総合集計・境界条件を検証できること。**

旧システムの「ランダム値投入」だけでは不十分。

以下を実装する。

1. 仮想参加者
2. 記録直接投入
3. ランダムデータ
4. 特殊ケース生成
5. 時間操作
6. イベント注入
7. シナリオテスト
8. all-run
9. JUnit 等の自動テスト

## 32.2 本番データとの分離

```text
/beat debug enable
/beat debug disable
```

Debug 有効中のデータは、

- results.json に書かない
- Whitelist に書かない
- 本大会 event-state に書かない
- participants.json を書き換えない

完全分離する。

正式結果データが存在する状態では debug enable を拒否するか、明確な安全条件を設ける。

---

# 33. 仮想参加者

例:

```text
Debug01
Debug02
...
```

コマンド例:

```text
/beat debug bots add 29
/beat debug bots clear
```

仮想参加者は:

- UUID相当の一意IDを持つ
- Competitor としてランキング参加
- 記録保持
- 脱落可能
- 総合集計可能

持たない:

- 実 Player
- Inventory
- TP
- GameMode
- Tab entry

---

# 34. デバッグ記録投入

## 34.1 高難易度

```text
/beat debug high set Debug01 850
```

必要なら tick 指定。

## 34.2 TA

```text
/beat debug ta set Debug01 28.50
/beat debug ta set Debug02 29.10 reached 12000
```

## 34.3 耐久

```text
/beat debug endurance set Debug01 78
```

---

# 35. ランダムデータ

```text
/beat debug fill high
/beat debug fill ta
/beat debug fill endurance
/beat debug fill all
```

seed 指定可能。

```text
/beat debug fill ta seed 12345
```

同 seed なら同結果。

---

# 36. デバッグ特殊ケース

例:

```text
/beat debug case ta tie
/beat debug case ta no-record
/beat debug case high tie
/beat debug case endurance tie
/beat debug case overall-tie
/beat debug case disqualified
/beat debug case missing-participation
```

目的はランダムでは出にくい境界状態を即生成すること。

---

# 37. 時間操作

```text
/beat debug time set 19:50
/beat debug time advance 10s
/beat debug time next
```

内部競技 tick 自体を操作する。

表示だけを変えてはいけない。

`time next` は次の重要イベント10秒前程度へジャンプ。

高難易度例:

```text
09:50
14:50
19:50
24:50
29:50
```

TA / 耐久:

```text
19:50
24:50
29:50
```

---

# 38. イベント注入

実際にコースを走らなくても内部処理を呼べる。

高難易度:

```text
/beat debug trigger high spot 2 3
/beat debug trigger high goal 2
```

TA:

```text
/beat debug trigger ta start
/beat debug trigger ta split 1
/beat debug trigger ta goal
```

耐久:

```text
/beat debug trigger endurance progress 47
/beat debug trigger endurance zone 2
/beat debug trigger endurance goal
```

UI・順位・通知を含む通常ロジックと同じサービス層を通す。

---

# 39. シナリオテスト

ゲーム内自動テストを実装する。

例:

```text
/beat debug scenario ta-elimination
```

内部:

1. 仮想参加者生成
2. 記録投入
3. 時間移動
4. 境界イベント
5. 脱落
6. expected と actual 比較

表示:

```text
―― DEBUG SCENARIO: TA ELIMINATION ――

✓ 20:00 Goalを先に反映
✓ 記録なしを脱落
✓ 有効参加者20人が生存
✓ 同タイムを記録順で判定
✓ 25:00 有効参加者10人が生存
✓ 脱落者PB保持

PASS 6 / 6
```

## 39.1 all-run

必須。

```text
/beat debug scenario all
```

すべてのシナリオを連続実行。

途中失敗しても残りを続行。

最後:

```text
DEBUG ALL SCENARIOS

PASS 126 / 128
FAIL 2

Failed:
- ta-elimination/same-tick-border
- overall/disqualified-seat
```

失敗時は expected / actual も確認可能にする。

---

# 40. 必須シナリオ

## 40.1 高難易度

- Spot 補完
- Goal 全 Spot 補完
- 同ポイント異 tick
- 同ポイント同 tick
- 0pt 同率
- 10:00 Course2
- 15:00 Course3
- 20:00 Course4
- 25:00 Course5
- 30:00 Goal
- Course5 all clear
- 脱落後記録固定

## 40.2 TA

- PB更新
- PB更新順位不変
- PB更新順位変動
- PB非更新
- Split飛ばし
- Start再進入
- restart item
- 同タイム異記録tick
- 同タイム同tick recordSequence
- 記録なし
- 20:00 Goal
- 25:00 Goal
- 30:00 Goal
- 脱落境界
- 失格者が生存人数を消費しない
- 切断中タイマー

## 40.3 耐久

- Progress飛ばし
- Progress同率
- Zone到達
- Zone + Progress
- Progress + fall
- Zone + fall
- Goal + fall
- 20:00 Zone2
- 25:00 Zone3
- 30:00 Goal
- Progress000

## 40.4 総合

- 通常順位積
- 持ち点同値
- 最高順位比較
- 2番目比較
- 完全同率
- 1競技不参加
- TA記録なしでも参加済み
- overallExcluded
- disqualified
- 失格者による順位非繰上げ

---

# 41. JUnit 等の外部自動テスト

ゲーム内 scenario だけに依存しない。

最低限以下は純粋 Java 単体テストを用意する。

- 高難易度ランキング
- TAランキング
- TA recordSequence
- 耐久ランキング
- 競技順位方式
- 総合持ち点
- 総合タイブレーク
- 失格席保持
- 生存人数判定
- 時間境界判定
- 参加フラグ
- Progress / Spot 補完

Codex には、テスト可能性を優先してクラス責務を分離させること。

---

# 42. Sidebar と同率

Sidebar は「10位まで」ではなく**最大10人**。

同率が大量にいても10行を超えない。

公式順位自体は同率のまま。

全体確認は Tab を使用する。

---

# 43. 結果確定と参加者一覧

各競技結果には participants.json の全員を含める。

欠席者:

- 高難易度: 0pt
- TA: `--.--`
- 耐久: Progress 000

ただし participated flag は false。

総合ランキングには条件を満たす人のみ掲載。

---

# 44. Reload

```text
/beat reload
```

読み込み対象:

- participants
- admins
- config
- messages
- styles
- maps

競技進行中は原則拒否。

```text
競技進行中のためreloadできません。
```

マップ設定更新は setup 機能から直接メモリ反映してよい。

---

# 45. Help

Help は日本語で具体例付き。

例:

```text
/beat setup help
```

```text
―― BEAT マップセットアップ ――

範囲を登録する場合:
1. /beat setup wand で選択ツールを取得
2. 左クリックでPos1
3. 右クリックでPos2
4. 登録コマンドを実行

詳細:
/beat setup help high
/beat setup help ta
/beat setup help endurance
```

コマンドを覚えていなくてもゲーム内だけで利用方法を追えること。

---

# 46. 実装優先順位

Codex に一度にすべて実装させず、以下の順を推奨する。

## Phase 1: Core

- Competitor model
- ranking logic
- tournament clock
- state machine
- persistence interfaces
- unit tests

## Phase 2: Participant/Admin

- JSON load
- UUID 管理
- Whitelist
- permissions
- tournament name snapshot

## Phase 3: Map setup

- wand
- selection
- save/load
- show/info/list
- validate

## Phase 4: High Difficulty

- practice
- flight
- personal CP
- Spot/Goal
- points
- elimination
- UI
- results

## Phase 5: TA

- Start/Split/Goal
- PB
- restart
- elimination
- UI
- Tab order
- results

## Phase 6: Endurance

- Progress
- Zone
- fall
- Goal
- elimination
- UI
- results

## Phase 7: Overall / Result editing

- overall
- confirm
- manual edit
- undo
- disqualification

## Phase 8: GUI

- main menu
- start confirmation
- transition
- emergency operations

## Phase 9: Debug

- virtual competitors
- time manipulation
- trigger
- scenarios
- all-run

## Phase 10: Packet/UI polish

- hide players while preserving Tab
- final visual tuning

---

# 47. 完了条件

実装完了とみなす最低条件:

1. 3競技を1人の実プレイヤーで開始〜終了できる
2. 仮想参加者を使って20〜30人ランキングを再現できる
3. `debug scenario all` が全件 PASS
4. JUnit 等のロジックテストが PASS
5. setup validate が全競技 PASS
6. サーバー再起動後も確定済み結果を保持
7. 再試合で競技途中データを安全に破棄可能
8. Whitelist 2モードが正常
9. 結果修正・再計算・confirm が正常
10. 総合順位が大会ルール通り
11. 失格時に順位を繰り上げない
12. 20/25/30分等の境界 tick がシナリオテストで保証されている
13. GUI が使えなくてもコマンドのみで大会運営可能
14. 本番データと Debug データが混ざらない

---

# 48. Codex への重要注意

実装時に以下を勝手に簡略化・変更しないこと。

- 時間を実時間にしない
- UUID ではなく MCID を主キーにしない
- 順位を Scoreboard 値だけで管理しない
- Bukkit Player が存在しないとランキング計算できない設計にしない
- Debug 仮想参加者を fake online Player として実装しない
- TA 同タイム同 tick の recordSequence を削除しない
- 高難易度 Goal の Spot 補完を削除しない
- 耐久 Goal を Progress と別ランキング体系にしない
- 失格者を除外して順位を詰め直さない
- 競技時間境界を Scheduler の実行順任せにしない
- プラグイン TP を踏み判定として扱わない
- GUI に競技ロジックを書かない
- Debug を正式 results.json へ保存しない
- setup 座標を Java コードへハードコードしない
- 文言・音・色を主要ロジックへ大量にハードコードしない

---

# 49. 最終方針

本プラグインでは、豪華さよりも以下を最優先する。

```text
正確な競技ロジック
+
事故に強い運営操作
+
ゲーム内で完結するマップ設定
+
1人で十分検証できるデバッグ機構
+
後から調整しやすい表示・演出設定
```

特に、**仮想参加者・時間操作・イベント注入・scenario all-run・純粋Java単体テスト**は、本番前に人手を大量に用意できない前提で重要な要件とする。
