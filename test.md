```

啊明白，你是 
"photon:5.0" 这个 Docker image，不是装 VM 的那个 Photon OS ——前面我按"虚拟机 OS"路子答偏了，切回来。

photon:5.0 作为 Docker image 是个啥

"vmware/photon"（Docker Hub 官方库）的定位就是极简 Linux base image，给 Dockerfile 的 
"FROM" 用的，类比 
"alpine" / 
"distroless" / 
"ubi"，但它是 RPM 系（tdnf）。5.0 这个 tag 背后：

- 基础是 Photon OS 5.0（内核 6.1 系，但容器里看不到宿主机内核，image 里只带 userspace）
- 包管理 
"tdnf"，兼容 yum 语法
- 体积 ~10M+（Docker Hub 显示 10M+ 那档）
- 支持 
"amd64" + 
"arm64v8"

"support 版本"在 Docker image 语境下要看三层

容器版的 Photon 跟 VM 版不太一样——它没有"装好就不动"的 ISO，而是滚动维护的 tag，所以"support"得拆开看：

① 大版本 5.0 的 lifecycle（跟前面答的 VM 版一致）

5.0 目前 EOL 日期未公布，还在活跃维护期。Docker Hub 上 3.0 的 tag 还挂着但 OS 本身已 EOL（2025-10-02），4.0 支持到 2027-10-11，5.0 的 EOL 估计会跟着下一个大版本发布才定。

② image 本身的维护 tag（重点）

Docker Hub 
"photon" 页的 supported tags 是这样的结构 ：

5.0 , 5.0-20260116 , latest
4.0 , 4.0-20260201
3.0 , 3.0-20250316

"5.0" 是个浮动 tag，背后实际指向一个带日期的快照（比如 
"5.0-20260116"），VMware 每次修 CVE / 升组件就 re-push 一个新日期子 tag，
"5.0" 跟着漂。
"5.0-20250920" 这种是当时某次 CVE roll 的快照 。

👉 所以"我在哪看 support"对你这个 image 来说，实操是：

- Docker Hub photon 页 → 看 supported tags 列表，确认 
"5.0" 当前漂到哪个日期快照：https://hub.docker.com/_/photon
- GitHub vmware/photon-docker-image → 看 issues + 每次更新的 Dockerfile 变更：https://github.com/vmware/photon-docker-image
- VMware/Broadcom Photon OS Release Notes → 搜 "Photon OS 5.0 Update XXX"，会列这次滚了哪些 CVE、Docker 版本从多少升到多少（比如之前 5.0 里 Docker 24.0.9 有 CVE-2024-41110，后面提交里升掉了 ）

③ 商业 support 的坑

Docker Hub 官方页自己写了一句 ：

Photon OS is released as open source software and comes with no commercial support.
意思是：你拉 
"photon:5.0" 这 image，VMware/Broadcom 默认不给你 commercial support。要商业支持得走签合同的路子——比如你是 Tanzu / vSphere 客户，Photon 作为底层组件在那些产品里是受支持的；但单独拿 
"photon:5.0" 当 base image 跑自己的 app，那就是社区 best-effort，出问题 GitHub 提 issue。

自查你手上这个 image 的版本

docker run --rm photon:5.0 cat /etc/os-release
# 看 VERSION_ID=5.0 和 BUILD_ID（那个日期就是对应 5.0-YYYYMMDD 的快照）

docker run --rm photon:5.0 tdnf info docker
# 看当前 image 里 Docker 组件版本，对照 CVE 情况

"BUILD_ID" 那个日期跟 Docker Hub 上 
"5.0-YYYYMMDD" 能对上，就能反查这次滚修了啥。

所以总结你这场景："去哪看 support"→ Docker Hub photon 页看 tag 漂移 + GitHub photon-docker-image 看变更 + Broadcom Photon OS release notes 看 CVE 清单；"5.0 还在不在 support"→ 大版本还在活跃，但 image 是滚动的，得看你 
"docker pull" 抓的是哪个日期快照。

你是拿 
"photon:5.0" 当自己 app 的 base image，还是某个 VMware 产品（Tanzu 之类）里打包好的 layer？前者要留意"无商业 support"那条，后者不用管，随产品走。