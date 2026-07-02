---
title: "lab4：基于 Java Swing 的图形用户界面与事件处理实践"
date: 2026-07-02
tags: ["Java", "Swing", "实验", "GUI", "事件处理", "序列化"]
description: "使用 Java Swing 构建两个综合项目——小学生算术测试软件与经典扫雷游戏，涵盖事件驱动编程、GridBagLayout 布局、递归级联算法与对象序列化本地存档。"
readingTime: 12
---

## 概述

本次实验通过两个完整的 Java Swing 项目，系统性实践了 Java 图形用户界面编程的核心概念：事件处理模型、自定义布局管理、组件交互，以及 Java 对象序列化技术。所有程序均可在浏览器中**免安装、直接运行**。

---

## 实验 1：小学生算术测试软件

> 源码文件：`ArithmeticQuizApp.java` · 主类：`ArithmeticQuizApp`

### 1.1 核心思想

应用通过 `Random` 随机生成整数操作数，结合 JComboBox 提供的三档难度配置，动态调整运算范围与运算符类型。程序基于 `ActionListener` 事件监听接口实现"选择难度 → 出题 → 提交验证 → 下一题"的完整交互闭环，涵盖事件源、监视器绑定与事件处理方法三层模型。

**难度参数表：**

| 难度 | 数值范围 | 运算符 |
|------|----------|--------|
| 容易 | 1 ~ 10 | `+` `-` |
| 提高 | 10 ~ 50 | `+` `-` `*` |
| 挑战 | 10 ~ 100 | `+` `-` `*` `/` |

特殊处理：减法保证结果非负（小学生友好）；除法保证整除，避免小数混淆。

### 1.2 关键方法

#### `generateQuestion()` — 核心出题算法

```java
private void generateQuestion() {
    int diffIndex = difficultyCombo.getSelectedIndex();
    String[] ops;

    if (diffIndex == 0) {          // 容易
        num1 = random.nextInt(10) + 1;
        num2 = random.nextInt(10) + 1;
        ops = new String[]{"+", "-"};
    } else if (diffIndex == 1) {   // 提高
        num1 = random.nextInt(40) + 10;
        num2 = random.nextInt(40) + 10;
        ops = new String[]{"+", "-", "*"};
    } else {                        // 挑战
        num1 = random.nextInt(90) + 10;
        num2 = random.nextInt(90) + 10;
        ops = new String[]{"+", "-", "*", "/"};
    }

    operator = ops[random.nextInt(ops.length)];

    // 减法防负数
    if (operator.equals("-") && num1 < num2) {
        int temp = num1; num1 = num2; num2 = temp;
    }
    // 除法保证整除
    if (operator.equals("/")) {
        while (num2 == 0) num2 = random.nextInt(10) + 1;
        num1 = num2 * (random.nextInt(9) + 1);
    }

    // 计算答案并刷新界面
    switch (operator) {
        case "+": correctAnswer = num1 + num2; break;
        case "-": correctAnswer = num1 - num2; break;
        case "*": correctAnswer = num1 * num2; break;
        case "/": correctAnswer = (double) num1 / num2; break;
    }

    formulaLabel.setText(num1 + " " + operator + " " + num2 + " = ?");
    answerField.setText("");
    answerField.requestFocus();
}
```

#### `actionPerformed(ActionEvent e)` — 事件处理中心

此方法实现了 `ActionListener` 接口，统一处理三类事件源——难度下拉框、提交按钮、下一题按钮——的分发逻辑：

```java
@Override
public void actionPerformed(ActionEvent e) {
    if (e.getSource() == difficultyCombo) {
        // 难度切换：清零计分、重新出题
        totalQuestions = 0; correctCount = 0;
        scoreLabel.setText("得分: 0 / 0");
        generateQuestion();
    } else if (e.getSource() == submitButton) {
        // 提交答案：浮点精度容差比较、统计正确率
        double userAnswer = Double.parseDouble(answerField.getText().trim());
        totalQuestions++;
        if (Math.abs(userAnswer - correctAnswer) < 0.01) {
            correctCount++;
            resultLabel.setText("✓ 恭喜你，答对了！🎉");
        } else {
            resultLabel.setText("✗ 答错了，正确答案是: " + correctAnswer);
        }
        scoreLabel.setText("得分: " + correctCount + " / " + totalQuestions);
        submitButton.setEnabled(false); // 禁止重复提交
    } else if (e.getSource() == nextButton) {
        generateQuestion(); // 刷新题目
    }
}
```

**设计要点：** 使用 `e.getSource()` 精确区分事件源，避免在单个方法中逻辑混乱；`submitButton.setEnabled(false)` 防止用户对同一道题反复提交刷分。

---

## 实验 2：经典扫雷游戏

> 源码文件：`MineSweeperGame.java` · 主类：`MineSweeperGame`

### 2.1 核心思想

游戏利用二维 `JButton[][]` 矩阵构建雷盘，以 `MouseAdapter` 监听从左键（翻开）和右键（插旗/取消）两种交互。核心算法包括：

- **安全首击**：首次点击时动态布雷，确保该格及其 3×3 邻域无雷。
- **递归级联翻开**：若翻开格为数字 0，自动向八个方向递归展开，直到遇到数字边界。
- **二进制序列化存档**：通过 `Serializable` 接口将全部游戏状态（雷图、翻开状态、计时、步数）写入 `ObjectOutputStream`，实现一键存档 / 读档。

**游戏参数：**

| 难度 | 网格 | 雷数 |
|------|------|------|
| 容易 | 9 × 9 | 10 |
| 困难 | 16 × 16 | 40 |

### 2.2 关键方法

#### `revealCell(int r, int c)` — 递归级联翻开算法

```java
private void revealCell(int r, int c) {
    // 越界 / 已翻开 / 已插旗 → 立即返回（递归终止条件）
    if (r < 0 || r >= rows || c < 0 || c >= cols
        || revealed[r][c] || flagged[r][c]) return;

    revealed[r][c] = true;
    JButton btn = buttons[r][c];
    btn.setEnabled(false);
    btn.setBackground(Color.LIGHT_GRAY);

    if (mineMap[r][c] > 0) {
        btn.setText(String.valueOf(mineMap[r][c]));
        // 数字着色：1 蓝、2 绿、3 红、4+ 紫
        switch (mineMap[r][c]) {
            case 1: btn.setForeground(Color.BLUE); break;
            case 2: btn.setForeground(new Color(0, 128, 0)); break;
            case 3: btn.setForeground(Color.RED); break;
            default: btn.setForeground(new Color(128, 0, 128));
        }
    } else if (mineMap[r][c] == 0) {
        // 空单元格 → 洪水填充展开
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++)
                revealCell(r + i, c + j);
    }
}
```

**算法分析：** 此递归采用 DFS 深度优先策略，类比于图像处理中的"洪水填充（Flood Fill）"。每一次 `revealCell(0)` 的调用会向八个方向试探，遇到数字格（1-8）时仅显示数字而不继续展开，形成自然的"边界"效果。

#### `saveGameProgress()` / `loadGameProgress()` — 序列化存档

```java
// 数据快照类：实现 Serializable 接口
private static class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    int rows, cols, totalMines, steps, timeElapsed;
    boolean isFirstClick;
    int[][] mineMap;
    boolean[][] revealed;
    boolean[][] flagged;
}

private void saveGameProgress() {
    try (ObjectOutputStream oos = new ObjectOutputStream(
            new FileOutputStream("minesweeper.sav"))) {
        GameState state = new GameState();
        // 将全部运行时状态复制到快照对象
        state.mineMap = this.mineMap;
        state.revealed = this.revealed;
        state.flagged = this.flagged;
        // ...
        oos.writeObject(state);
    } catch (IOException e) { /* 错误处理 */ }
}

private void loadGameProgress() {
    try (ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream("minesweeper.sav"))) {
        GameState state = (GameState) ois.readObject();
        // 从快照对象恢复全部状态
        this.mineMap = state.mineMap;
        this.revealed = state.revealed;
        this.flagged = state.flagged;
        // … 重建 JButton 矩阵并刷新 UI
    } catch (Exception e) { /* 错误处理 */ }
}
```

**技术亮点：** `GameState` 作为内部静态类实现了 `Serializable`，充当"数据快照"。存档时将所有运行时状态打包写入二进制文件；读档时反序列化后遍历重建 GUI 矩阵，完整恢复游戏进度——包括计时器、步数和已翻开/已插旗的格子状态。

---

## 技术总结

| 技术点 | 实验 1（算术测试） | 实验 2（扫雷游戏） |
|--------|-------------------|-------------------|
| 布局管理 | `BorderLayout` + `GridBagLayout` | `BorderLayout` + `GridLayout` |
| 事件模型 | `ActionListener` 接口 | `MouseAdapter` 继承 |
| 核心算法 | 随机出题 + 难度参数化 | 递归洪水填充 + DFS 级联翻开 |
| 数据持久化 | 无（纯运行时） | Java 原生 `Serializable` 二进制序列化 |
| 浏览器运行 | CheerpJ 3.0 WebAssembly | CheerpJ 3.0 WebAssembly |

---

## 在线体验

点击下方按钮，在浏览器中直接运行 Java Swing 程序（无需安装 JDK 或 JRE）：

<div style="display:flex; gap:16px; flex-wrap:wrap; margin:24px 0;">

<a href="quiz-demo.html" style="display:inline-block; padding:14px 28px; background:linear-gradient(135deg, #38bdf8, #0ea5e9); color:#fff; text-decoration:none; border-radius:10px; font-weight:700; font-size:1.05rem; box-shadow:0 4px 16px rgba(56,189,248,0.3);">
  🧮 点击在线体验：实验1 — 算术测试
</a>

<a href="minesweeper-demo.html" style="display:inline-block; padding:14px 28px; background:linear-gradient(135deg, #a78bfa, #7c3aed); color:#fff; text-decoration:none; border-radius:10px; font-weight:700; font-size:1.05rem; box-shadow:0 4px 16px rgba(167,139,250,0.3);">
  💣 点击在线体验：实验2 — 经典扫雷
</a>

</div>

> **运行说明：** 页面加载时会通过 CheerpJ CDN 下载约 10 MB 的 Java WebAssembly 运行时，首次等待约 10–30 秒后即可交互。建议使用 Chrome / Edge 等现代浏览器以获得最佳体验。

---

*本文由 CheerpJ 3.0 驱动，源代码及构建脚本见 [GitHub 仓库](https://github.com/tanxxx1/javablog)。*
