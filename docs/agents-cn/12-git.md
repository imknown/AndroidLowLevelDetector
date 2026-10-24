# 12-git

> **状态**: **living**. 这套规则文件的入口在根 [AGENTS.md](../../AGENTS.md), 编号和阅读时机见那份索引.

## Git

- PR 打到 `develop`, 它是默认分支. 提交信息按 Conventional Commits 走, **类型 + 作用域用小写**:  
  `fix(home): ...`.
- 每笔提交信息都以一行 trailer 收尾, 写明产出这次工作的 agent, 模型, 还有推理强度级别.  
  级别取值是 `off` / `low` / `medium` / `high` / `xhigh` / `max`, ...  
  写法形如 `Generated with ZCode (GLM-5.3, effort: xhigh)`.  
  `effort:` 就是推理强度这个旋钮本身, 不是对推理好坏的评价. 很早期的提交写的是 `reasoning:`, 那些保持原样.  
  取值绝不猜, 也不许自己写 `unknown`: 先问负责人该记什么. 他们确认这个值确实定不下来之后,  
  `unknown` 才是对的, 是商定过的写法.
- 提交信息一律英语; 语言细则见 [05-代码规则](05-代码规则.md).
- agent 的名字要拿证据来定, 别按习惯写; 把依据摆出来, 写下去之前先让用户同意.  
  不同版本级的名字是两回事. `Qoder CN` 和 `Qoder` 就是两个不同的 AI 开发环境 (ADE).  
  用户没要求, 就别自己加 `IDE` / `CLI` 这种表示宿主形态的后缀.

## 绝不提交

- 凭据, 签名材料, 本地配置 (`local.properties`, `keys/release.jks`, `google-services.json`) 绝不提交,  
  也不许用 `git add -f` 强制加进去. 根 `.gitignore` 已经把它们排除了.
- 一个改动看着非提交这类文件不可时, 先停下来问.
