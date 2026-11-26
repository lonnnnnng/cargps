# 车机发布签名

GPS 车机版沿用的 Release 密钥在本机生成，不存入仓库或 GitHub：

- 密钥库：`~/.config/cargps/signing/cargps-release.p12`
- 密钥库密码：`~/.config/cargps/signing/release-password`
- 本机备份：`~/.config/cargps/signing-backup/`
- 别名：`cargps-release`
- 公开证书：GitHub Release 附件 `GPS-Release-Certificate.pem`
- 证书 SHA-256：`53c39ba74f18bc02f6506154a6beec63673a16d73ebf83e9bed47704ffd9b86e`

生成 APK 前，在同一终端设置构建环境；命令只读取受限权限的口令文件，不回显密码：

```zsh
cargps_signing_dir="$HOME/.config/cargps/signing"
export ANDROID_SIGNING_STORE_FILE="$cargps_signing_dir/cargps-release.p12"
export ANDROID_SIGNING_STORE_PASSWORD="$(<"$cargps_signing_dir/release-password")"
export ANDROID_SIGNING_KEY_ALIAS="cargps-release"
export ANDROID_SIGNING_KEY_PASSWORD="$ANDROID_SIGNING_STORE_PASSWORD"
./gradlew :app:assembleRelease
unset ANDROID_SIGNING_STORE_FILE ANDROID_SIGNING_STORE_PASSWORD
unset ANDROID_SIGNING_KEY_ALIAS ANDROID_SIGNING_KEY_PASSWORD
```

发布前验证 `app/build/outputs/apk/release/app-release.apk` 的证书指纹与本文一致。密钥库与口令备份均保存在本机 `~/.config/cargps/` 下，权限为当前用户独占；绝不把 `.p12`、口令文件或其副本加入仓库或 Release。
