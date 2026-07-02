import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class ArithmeticQuizApp extends JFrame implements ActionListener {
    // GUI 组件
    private JComboBox<String> difficultyCombo;
    private JLabel formulaLabel;
    private JTextField answerField;
    private JButton submitButton;
    private JButton nextButton;
    private JLabel resultLabel;
    private JLabel scoreLabel;

    // 题目控制变量
    private int num1, num2;
    private String operator;
    private double correctAnswer;
    private int totalQuestions = 0;
    private int correctCount = 0;
    private Random random = new Random();

    public ArithmeticQuizApp() {
        // 1. 初始化窗口设置
        setTitle("小学生算术通 - 趣味测试");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示
        setLayout(new BorderLayout(10, 10));

        // 设置全局美化 UI 风格
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. 顶部区域：难度选择与计分 (BorderLayout.NORTH)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        topPanel.setBackground(new Color(240, 248, 255)); // 浅蓝色背景

        JLabel diffLabel = new JLabel("选择难度:");
        diffLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        
        String[] difficulties = {"容易 (10以内加减)", "提高 (50以内加减乘)", "挑战 (100以内加减乘除)"};
        difficultyCombo = new JComboBox<>(difficulties);
        difficultyCombo.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        difficultyCombo.addActionListener(this); // 监听难度切换

        scoreLabel = new JLabel("得分: 0 / 0");
        scoreLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        scoreLabel.setForeground(new Color(46, 139, 87));

        topPanel.add(diffLabel);
        topPanel.add(difficultyCombo);
        topPanel.add(scoreLabel);

        // 3. 中部区域：题目显示与输入 (BorderLayout.CENTER)
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        formulaLabel = new JLabel("请选择难度开始答题", JLabel.CENTER);
        formulaLabel.setFont(new Font("Consolas", Font.BOLD, 28)); // 粗体大字号
        formulaLabel.setForeground(new Color(70, 130, 180));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        centerPanel.add(formulaLabel, gbc);

        JLabel replyInputLabel = new JLabel("你的答案:");
        replyInputLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        centerPanel.add(replyInputLabel, gbc);

        answerField = new JTextField(10);
        answerField.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 1; gbc.gridy = 1;
        centerPanel.add(answerField, gbc);

        // 4. 下部区域：操作按钮与反馈 (BorderLayout.SOUTH)
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        bottomPanel.setBackground(new Color(245, 245, 245));

        // 提示结果标签
        resultLabel = new JLabel("准备就绪，请作答！", JLabel.CENTER);
        resultLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        resultLabel.setForeground(Color.GRAY);
        bottomPanel.add(resultLabel);

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        btnPanel.setOpaque(false);
        
        submitButton = new JButton("确定提交");
        submitButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        submitButton.setBackground(new Color(100, 149, 237));
        submitButton.setForeground(Color.WHITE);
        submitButton.addActionListener(this); // 注册监视器

        nextButton = new JButton("下一题");
        nextButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        nextButton.addActionListener(this); // 注册监视器

        btnPanel.add(submitButton);
        btnPanel.add(nextButton);
        bottomPanel.add(btnPanel);

        // 5. 组装至主窗口
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 生成第一道题
        generateQuestion();
    }

    /**
     * 随机出题核心逻辑 (可支持扩展分数、小数)
     */
    private void generateQuestion() {
        int diffIndex = difficultyCombo.getSelectedIndex();
        String[] ops;
        
        // 根据自定义难度配置参数
        if (diffIndex == 0) { // 容易
            num1 = random.nextInt(10) + 1;
            num2 = random.nextInt(10) + 1;
            ops = new String[]{"+", "-"};
        } else if (diffIndex == 1) { // 提高
            num1 = random.nextInt(40) + 10;
            num2 = random.nextInt(40) + 10;
            ops = new String[]{"+", "-", "*"};
        } else { // 挑战
            num1 = random.nextInt(90) + 10;
            num2 = random.nextInt(90) + 10;
            ops = new String[]{"+", "-", "*", "/"};
        }

        operator = ops[random.nextInt(ops.length)];

        // 防止减法出现负数（更符合小学生习惯，可自行移除）
        if (operator.equals("-") && num1 < num2) {
            int temp = num1; num1 = num2; num2 = temp;
        }

        // 防止除法无法整除或除以0（此处扩展为保留2位小数或整除）
        if (operator.equals("/")) {
            while (num2 == 0) {
                num2 = random.nextInt(10) + 1;
            }
            // 为了适合小学生，让它能够整除
            num1 = num2 * (random.nextInt(9) + 1); 
        }

        // 计算正确答案
        switch (operator) {
            case "+": correctAnswer = num1 + num2; break;
            case "-": correctAnswer = num1 - num2; break;
            case "*": correctAnswer = num1 * num2; break;
            case "/": correctAnswer = (double) num1 / num2; break;
        }

        // 更新界面
        formulaLabel.setText(num1 + " " + operator + " " + num2 + " = ?");
        answerField.setText("");
        answerField.requestFocus(); // 光标自动聚焦
        submitButton.setEnabled(true);
        resultLabel.setText("请在上方输入答案后点击提交。");
        resultLabel.setForeground(Color.GRAY);
    }

    /**
     * 事件处理接口实现
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // 情况 A: 切换难度
        if (e.getSource() == difficultyCombo) {
            totalQuestions = 0;
            correctCount = 0;
            scoreLabel.setText("得分: 0 / 0");
            generateQuestion();
        } 
        // 情况 B: 提交答案
        else if (e.getSource() == submitButton) {
            String userText = answerField.getText().trim();
            if (userText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入答案后再提交！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                double userAnswer = Double.parseDouble(userText);
                totalQuestions++;
                
                // 验证正确性（考虑浮点数精度）
                if (Math.abs(userAnswer - correctAnswer) < 0.01) {
                    correctCount++;
                    resultLabel.setText(" 恭喜你，答对了！🎉");
                    resultLabel.setForeground(new Color(46, 139, 87));
                } else {
                    resultLabel.setText("❌ 答错了，正确答案是: " + (correctAnswer == (int)correctAnswer ? (int)correctAnswer : correctAnswer));
                    resultLabel.setForeground(Color.RED);
                }
                
                // 更新得分面板
                scoreLabel.setText("得分: " + correctCount + " / " + totalQuestions);
                submitButton.setEnabled(false); // 防止重复提交同一道题
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "格式不正确，请输入有效的数字！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        } 
        // 情况 C: 下一题
        else if (e.getSource() == nextButton) {
            generateQuestion();
        }
    }

    public static void main(String[] args) {
        // 在事件派发线程中启动 GUI，确保线程安全
        SwingUtilities.invokeLater(() -> {
            new ArithmeticQuizApp().setVisible(true);
        });
    }
}