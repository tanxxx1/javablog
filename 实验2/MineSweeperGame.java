import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class MineSweeperGame extends JFrame {
    // 游戏难度常量 
    private static final int EASY_ROWS = 9, EASY_COLS = 9, EASY_MINES = 10;
    private static final int HARD_ROWS = 16, HARD_COLS = 16, HARD_MINES = 40;

    private int rows = EASY_ROWS;
    private int cols = EASY_COLS;
    private int totalMines = EASY_MINES;

    // 游戏运行时状态 
    private int steps = 0;
    private int timeElapsed = 0;
    private boolean isGameOver = false;
    private boolean isFirstClick = true;

    // 核心数据矩阵
    private int[][] mineMap;       // -1: 雷, 0-8: 周边雷数
    private boolean[][] revealed;  // 是否已翻开
    private boolean[][] flagged;   // 是否已插旗

    // GUI 组件 
    private JButton[][] buttons;
    private JLabel timeLabel;
    private JLabel stepLabel;
    private JComboBox<String> difficultyCombo;
    private JPanel minePanel;
    private Timer gameTimer;

    public MineSweeperGame() {
        setTitle("高级经典扫雷 - 实验2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        initTopPanel();
        resetGame();
        
        setSize(450, 500);
        setLocationRelativeTo(null);
    }

    /**
     * 初始化顶部控制面板 [cite: 15, 16, 17, 18]
     */
    private void initTopPanel() {
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        
        // 第一行：难度与存档控制 [cite: 16, 18]
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        String[] diffs = {"容易 (9x9, 10雷)", "困难 (16x16, 40雷)"};
        difficultyCombo = new JComboBox<>(diffs);
        difficultyCombo.addActionListener(e -> {
            if (difficultyCombo.getSelectedIndex() == 0) {
                rows = EASY_ROWS; cols = EASY_COLS; totalMines = EASY_MINES;
            } else {
                rows = HARD_ROWS; cols = HARD_COLS; totalMines = HARD_MINES;
            }
            resetGame();
        });

        JButton saveBtn = new JButton("保存进度");
        saveBtn.addActionListener(e -> saveGameProgress());

        JButton loadBtn = new JButton("读取进度");
        loadBtn.addActionListener(e -> loadGameProgress());

        controlRow.add(new JLabel("难度:"));
        controlRow.add(difficultyCombo);
        controlRow.add(saveBtn);
        controlRow.add(loadBtn);

        // 第二行：状态数据流（计时与计步） 
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));
        timeLabel = new JLabel("时间: 0s");
        timeLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        stepLabel = new JLabel("步数: 0");
        stepLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        statusRow.add(timeLabel);
        statusRow.add(stepLabel);

        topPanel.add(controlRow);
        topPanel.add(statusRow);
        add(topPanel, BorderLayout.NORTH);

        // 初始化计时器触发器 
        gameTimer = new Timer(1000, e -> {
            timeElapsed++;
            timeLabel.setText("时间: " + timeElapsed + "s");
        });
    }

    /**
     * 重置/刷新雷盘布局 
     */
    private void resetGame() {
        if (minePanel != null) {
            remove(minePanel);
        }

        isGameOver = false;
        isFirstClick = true;
        steps = 0;
        timeElapsed = 0;
        timeLabel.setText("时间: 0s");
        stepLabel.setText("步数: 0");
        if (gameTimer.isRunning()) gameTimer.stop();

        mineMap = new int[rows][cols];
        revealed = new boolean[rows][cols];
        flagged = new boolean[rows][cols];
        buttons = new JButton[rows][cols];

        minePanel = new JPanel(new GridLayout(rows, cols, 1, 1));
        minePanel.setBackground(Color.GRAY);

        // 动态构建雷区格子 
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                final int curR = r;
                final int curC = c;
                JButton btn = new JButton();
                btn.setFont(new Font("Arial", Font.BOLD, 14));
                btn.setMargin(new Insets(0, 0, 0, 0));
                
                // 鼠标事件监视器（处理左键和右键交互）
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (isGameOver) return;
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            handleLeftClick(curR, curC);
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            handleRightClick(curR, curC);
                        }
                    }
                });
                buttons[r][c] = btn;
                minePanel.add(btn);
            }
        }

        add(minePanel, BorderLayout.CENTER);
        validate();
        repaint();
    }

    /**
     * 生成地雷布局（首次点击不落雷，保证玩家体验）
     */
    private void generateMines(int startRow, int startCol) {
        java.util.Random rand = new java.util.Random();
        int placedMines = 0;
        while (placedMines < totalMines) {
            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);
            if (mineMap[r][c] != -1 && !(r == startRow && c == startCol)) {
                mineMap[r][c] = -1;
                placedMines++;
            }
        }

        // 计算周围格子的数字
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (mineMap[r][c] == -1) continue;
                int count = 0;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int nr = r + i, nc = c + j;
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && mineMap[nr][nc] == -1) {
                            count++;
                        }
                    }
                }
                mineMap[r][c] = count;
            }
        }
    }

    /**
     * 左键点击处理 
     */
    private void handleLeftClick(int r, int c) {
        if (revealed[r][c] || flagged[r][c]) return;

        if (isFirstClick) {
            isFirstClick = false;
            generateMines(r, c);
            gameTimer.start();
        }

        steps++;
        stepLabel.setText("步数: " + steps);

        if (mineMap[r][c] == -1) {
            // 踩雷，游戏结束
            isGameOver = true;
            gameTimer.stop();
            revealAllMines();
            JOptionPane.showMessageDialog(this, "💥 💥 您踩雷了，游戏结束！", "Game Over", JOptionPane.ERROR_MESSAGE);
        } else {
            // 递归翻开安全区
            revealCell(r, c);
            checkGameWin();
        }
    }

    /**
     * 右键插旗处理
     */
    private void handleRightClick(int r, int c) {
        if (revealed[r][c]) return;
        flagged[r][c] = !flagged[r][c];
        buttons[r][c].setText(flagged[r][c] ? "🚩" : "");
        buttons[r][c].setForeground(Color.RED);
    }

    /**
     * 级联翻开空白无雷区
     */
    private void revealCell(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols || revealed[r][c] || flagged[r][c]) return;

        revealed[r][c] = true;
        JButton btn = buttons[r][c];
        btn.setEnabled(false);
        btn.setBackground(Color.LIGHT_GRAY);

        if (mineMap[r][c] > 0) {
            btn.setText(String.valueOf(mineMap[r][c]));
            // 为不同数字上色，增强界面美观度 
            switch (mineMap[r][c]) {
                case 1: btn.setForeground(Color.BLUE); break;
                case 2: btn.setForeground(new Color(0, 128, 0)); break;
                case 3: btn.setForeground(Color.RED); break;
                default: btn.setForeground(new Color(128, 0, 128));
            }
        } else if (mineMap[r][c] == 0) {
            // 如果是0，扩散翻开
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    revealCell(r + i, c + j);
                }
            }
        }
    }

    private void revealAllMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (mineMap[r][c] == -1) {
                    buttons[r][c].setText("💣");
                    buttons[r][c].setBackground(Color.ORANGE);
                }
            }
        }
    }

    private void checkGameWin() {
        int unrevealedSafeCells = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (mineMap[r][c] != -1 && !revealed[r][c]) {
                    unrevealedSafeCells++;
                }
            }
        }
        if (unrevealedSafeCells == 0) {
            isGameOver = true;
            gameTimer.stop();
            JOptionPane.showMessageDialog(this, "🎉 恭喜你赢了！用时 " + timeElapsed + " 秒，总步数 " + steps, "Victory", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 数据快照类：用于完美实现保存/读取进度 
     */
    private static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;
        int rows, cols, totalMines, steps, timeElapsed;
        boolean isFirstClick;
        int[][] mineMap;
        boolean[][] revealed;
        boolean[][] flagged;
    }

    /**
     * 保存游戏进度至本地文件 
     */
    private void saveGameProgress() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("minesweeper.sav"))) {
            GameState state = new GameState();
            state.rows = this.rows;
            state.cols = this.cols;
            state.totalMines = this.totalMines;
            state.steps = this.steps;
            state.timeElapsed = this.timeElapsed;
            state.isFirstClick = this.isFirstClick;
            state.mineMap = this.mineMap;
            state.revealed = this.revealed;
            state.flagged = this.flagged;

            oos.writeObject(state);
            JOptionPane.showMessageDialog(this, "游戏进度已成功保存在当前目录下！", "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "存档失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 从本地文件读取并完全还原游戏进度 
     */
    private void loadGameProgress() {
        File saveFile = new File("minesweeper.sav");
        if (!saveFile.exists()) {
            JOptionPane.showMessageDialog(this, "未找到任何历史存档文件！", "读取失败", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            GameState state = (GameState) ois.readObject();
            
            this.rows = state.rows;
            this.cols = state.cols;
            this.totalMines = state.totalMines;
            this.steps = state.steps;
            this.timeElapsed = state.timeElapsed;
            this.isFirstClick = state.isFirstClick;
            this.mineMap = state.mineMap;
            this.revealed = state.revealed;
            this.flagged = state.flagged;

            // 同步调整下拉框的UI外观 [cite: 15, 16]
            if (this.rows == EASY_ROWS) difficultyCombo.setSelectedIndex(0);
            else difficultyCombo.setSelectedIndex(1);

            // 重新重构界面 
            if (minePanel != null) remove(minePanel);
            minePanel = new JPanel(new GridLayout(rows, cols, 1, 1));
            minePanel.setBackground(Color.GRAY);
            buttons = new JButton[rows][cols];

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    JButton btn = new JButton();
                    btn.setFont(new Font("Arial", Font.BOLD, 14));
                    btn.setMargin(new Insets(0, 0, 0, 0));
                    
                    // 绑定恢复后的事件监听器
                    final int curR = r; final int curC = c;
                    btn.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            if (isGameOver) return;
                            if (SwingUtilities.isLeftMouseButton(e)) handleLeftClick(curR, curC);
                            else if (SwingUtilities.isRightMouseButton(e)) handleRightClick(curR, curC);
                        }
                    });

                    // 还原格子的显示外观 
                    if (revealed[r][c]) {
                        btn.setEnabled(false);
                        btn.setBackground(Color.LIGHT_GRAY);
                        if (mineMap[r][c] > 0) {
                            btn.setText(String.valueOf(mineMap[r][c]));
                        }
                    } else if (flagged[r][c]) {
                        btn.setText("🚩");
                        btn.setForeground(Color.RED);
                    }

                    buttons[r][c] = btn;
                    minePanel.add(btn);
                }
            }

            add(minePanel, BorderLayout.CENTER);
            
            // 恢复计时器与看板数据 
            stepLabel.setText("步数: " + steps);
            timeLabel.setText("时间: " + timeElapsed + "s");
            isGameOver = false;
            
            if (gameTimer.isRunning()) gameTimer.stop();
            if (!isFirstClick) gameTimer.start();

            validate();
            repaint();
            JOptionPane.showMessageDialog(this, "历史进度已成功加载！游戏继续。", "读取成功", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "读档失败，文件可能已损坏: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MineSweeperGame().setVisible(true));
    }
}