# BEAT 本番当日 システム運用進行表・チェックリスト

> 対象：Minecraft内でBEATプラグインを操作する運営担当者  
> 用途：大会本番当日に、競技開始・結果確認・次競技への遷移・緊急対応を安全に行うための手順書

# 3. 本番 進行表

本番では**必要以上に触らない**ことを原則とする。

---

## LIVE-00｜大会開始前

### サーバー起動直後

```text
/plugins
```

- [ ] BEAT有効
- [ ] ProtocolLib有効

### 状態確認

```text
/beat competition status
/beat players
/beat setup validate all
```

- [ ] 前回大会状態なし
- [ ] 参加者登録OK
- [ ] 運営登録OK
- [ ] 全競技Validation PASSED

### 本番設定

```text
/beat settings preset production
/beat settings show
```

- [ ] 高難易度：練習10分
- [ ] 高難易度：準備1分
- [ ] 高難易度：本番30分
- [ ] TA：30分
- [ ] **TA 20分足切り人数**
- [ ] **TA 25分足切り人数**
- [ ] 耐久：30分
- [ ] 耐久20分判定
- [ ] 耐久25分判定

### Debugを確実に無効化

```text
/beat debug disable
```

- [ ] Debug無効

### Whitelist

```text
/beat whitelist all
/beat whitelist status
```

- [ ] `完全同期: YES`

---

## LIVE-01｜開始1時間前

- [ ] Whitelist ALL
- [ ] 新規参加者がログインできる
- [ ] MCID変更連絡があれば反映
- [ ] JSON更新後 `/beat reload`
- [ ] `/beat players`
- [ ] `/beat whitelist all`
- [ ] `/beat whitelist status`
- [ ] サーバーに異常なし

申込締切後：

- [ ] 最終参加人数確定
- [ ] TA足切り人数確定
- [ ] プラグイン設定へ反映
- [ ] `/beat settings show` で再確認

---

## LIVE-02｜大会開始直前

操作担当が最後に実行：

```text
/beat competition status
/beat players
/beat setup validate all
/beat settings show
/beat whitelist status
```

すべて正常なら、**主催から「高難易度開始OK」を待つ。**

---

## LIVE-03｜高難易度

GOが出たら：

```text
/beat competition high start
```

### 練習

- [ ] 全員練習へ入った
- [ ] BossBar正常
- [ ] 明らかなエラーなし

### 練習終了

- [ ] 全員Prepareへ移動

### 準備終了

- [ ] 自動で本番開始

### 本番

監視：

- [ ] BossBar
- [ ] 次の脱落
- [ ] 参加人数
- [ ] チャットの不具合報告
- [ ] サーバーTPS等

**正常なら操作しない。**

終了後：

```text
/beat competition status
```

---

## LIVE-04｜高難易度結果

GUI：

**結果 → 高難易度**

- [ ] 順位確認
- [ ] ポイント確認

問題なしなら：

- [ ] 「結果を発表」
- [ ] 「発表する」
- [ ] チャット表示確認

**ここから次の競技開始までは異議申し立て受付時間。**

主催の確認が済むまではTAを開始しない。

---

## LIVE-05｜TA

**主催からGOが出たら：**

```text
/beat competition ta start
```

監視：

- [ ] 計測が動いている
- [ ] PBが更新されている
- [ ] 順位が出ている
- [ ] 足切りボーダー正常

### 20分

- [ ] 告知済み人数まで正しく足切りされた

### 25分

- [ ] 告知済み人数まで正しく足切りされた

### 30分

- [ ] 自動終了
- [ ] Endへ移動

確認：

```text
/beat competition status
```

---

## LIVE-06｜TA結果

GUI：

**結果 → TA**

- [ ] PB確認
- [ ] 順位確認
- [ ] 記録なし確認

問題なければ：

- [ ] 結果発表
- [ ] チャット表示確認

主催のGOまで待機。

---

## LIVE-07｜耐久

```text
/beat competition endurance start
```

監視：

- [ ] Progress更新
- [ ] Zone到達
- [ ] 落下復帰
- [ ] 順位表示

### 20分

- [ ] Zone 2未到達者が脱落

### 25分

- [ ] Zone 3未到達者が脱落

### 30分

- [ ] 自動終了

確認：

```text
/beat competition status
```

---

## LIVE-08｜耐久結果

GUI：

**結果 → 耐久**

- [ ] Progress
- [ ] 完走者
- [ ] 順位

問題なければ：

- [ ] 結果発表
- [ ] チャット表示確認

---

## LIVE-09｜総合

**ここは特に急がない。**

GUI：

**総合**

まず：

```text
/beat overall calculate
/beat overall result
```

- [ ] 高難易度順位
- [ ] TA順位
- [ ] 耐久順位
- [ ] 持ち点
- [ ] 同率処理
- [ ] 総合対象外
- [ ] 失格者

**主催が結果を確認してOKを出してから：**

```text
/beat overall confirm
```

- [ ] 総合確定
- [ ] GUIから総合結果発表

---

## LIVE-10｜大会終了後

```text
/beat whitelist admins
/beat whitelist status
```

- [ ] 参加者がWhitelistから外れた
- [ ] 運営のみ

保存：

- [ ] `plugins/BEAT/data/`
- [ ] `plugins/BEAT/logs/`
- [ ] ワールド
- [ ] BEATプラグインフォルダー

---

# 4. 本番用 1枚チェックリスト

## 開始前

- [ ] `/plugins`
- [ ] BEAT OK
- [ ] ProtocolLib OK
- [ ] `/beat debug disable`
- [ ] `/beat competition status`
- [ ] `/beat players`
- [ ] `/beat setup validate all`
- [ ] `/beat settings preset production`
- [ ] `/beat settings show`
- [ ] TA足切り人数OK
- [ ] `/beat whitelist all`
- [ ] `/beat whitelist status`
- [ ] 完全同期 YES

## 高難易度

- [ ] 主催GO
- [ ] `high start`
- [ ] 練習開始
- [ ] Prepare移動
- [ ] 本番開始
- [ ] 脱落処理
- [ ] 正常終了
- [ ] 結果確認
- [ ] 結果発表
- [ ] 異議対応終了
- [ ] 主催GO

## TA

- [ ] `ta start`
- [ ] 計測正常
- [ ] 20分足切り
- [ ] 25分足切り
- [ ] 正常終了
- [ ] 結果確認
- [ ] 結果発表
- [ ] 異議対応終了
- [ ] 主催GO

## 耐久

- [ ] `endurance start`
- [ ] Progress正常
- [ ] 20分脱落
- [ ] 25分脱落
- [ ] 正常終了
- [ ] 結果確認
- [ ] 結果発表

## 総合

- [ ] `overall calculate`
- [ ] `overall result`
- [ ] 主催チェック
- [ ] `overall confirm`
- [ ] 総合発表

## 終了

- [ ] `whitelist admins`
- [ ] データ保存
- [ ] ログ保存
- [ ] ワールド保存

---

# 5. 本番中トラブル時の簡易判断表

| 状況 | システム担当が最初にすること | やらないこと |
|---|---|---|
| 何かおかしい | `/beat competition status` | `event reset` |
| 判定されない | status → setup / 状態確認 | 即再試合 |
| 参加者1人が回線落ち | 基本そのまま | 時計停止 |
| 参加者再接続 | 復帰状態確認 | 手動で記録補正 |
| 複数人に重大不具合 | 主催へ報告 → 状態確認 | 独断restart |
| サーバー全体に重大問題 | 主催判断後に再試合処理 | force-endでごまかす |
| 状況不明で参加者を安全にしたい | `emergency collect` | `event reset` |
| 現競技を再試合 | 主催判断後 `emergency restart` | 独断実行 |
| TAで重大ショートカット | 主催へ即報告 | 担当者だけで続行判断 |

## 緊急時に覚えておくコマンド

```text
/beat competition status

/beat emergency collect
/beat emergency collect confirm

/beat emergency force-end
/beat emergency force-end confirm

/beat emergency restart
/beat emergency restart confirm
```

**`force-end` と `restart` は、操作担当者が困ったから押すものではなく、主催・運営側で処理方針を決めてから使う。**

---

---

# 参照

- BEAT大会サイト  
  https://beat-explanation.pages.dev/
- BEAT プラグイン運用ガイド  
  `BEAT_plugin_operations_guide.md`
