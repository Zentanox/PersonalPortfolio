package com.poly.perspringbootprojectmvn.gameapp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.Stack;

public class BombSweeper extends JFrame {
    private final int rows;
    private final int cols;
    private final int totalMines;
    private final CellButton[][] buttons;
    private boolean[][] mines;
    private boolean firstClick = true;
    private final JLabel minesLabel;
    private int flagsPlaced = 0;
    private boolean gameOver = false;

    public BombSweeper(int rows, int cols, int mines) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = mines;
        this.mines = new boolean[rows][cols];
        this.buttons = new CellButton[rows][cols];

        setTitle("Bomb Sweeper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        minesLabel = new JLabel("Mines: " + totalMines);
        minesLabel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> resetGame());
        top.add(minesLabel, BorderLayout.WEST);
        top.add(reset, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(rows, cols));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                CellButton btn = new CellButton(r, c);
                btn.setPreferredSize(new Dimension(32, 32));
                btn.setMargin(new Insets(0,0,0,0));
                btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (gameOver) return;
                        if (SwingUtilities.isRightMouseButton(e)) {
                            toggleFlag(btn);
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            openCell(btn);
                        }
                    }
                });
                buttons[r][c] = btn;
                board.add(btn);
            }
        }
        add(board, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void resetGame() {
        firstClick = true;
        gameOver = false;
        flagsPlaced = 0;
        mines = new boolean[rows][cols];
        minesLabel.setText("Mines: " + totalMines);
        for (int r=0; r<rows; r++) {
            for (int c=0; c<cols; c++) {
                buttons[r][c].reset();
            }
        }
    }

    private void placeMines(int avoidR, int avoidC) {
        Random rnd = new Random();
        int placed = 0;
        while (placed < totalMines) {
            int r = rnd.nextInt(rows);
            int c = rnd.nextInt(cols);
            if (mines[r][c]) continue;
            // avoid placing mine at the first clicked cell and its neighbors
            if (Math.abs(r - avoidR) <= 1 && Math.abs(c - avoidC) <= 1) continue;
            mines[r][c] = true;
            placed++;
        }
    }

    private int adjacentMines(int r, int c) {
        int count = 0;
        for (int dr=-1; dr<=1; dr++) {
            for (int dc=-1; dc<=1; dc++) {
                if (dr==0 && dc==0) continue;
                int nr = r+dr, nc = c+dc;
                if (nr>=0 && nr<rows && nc>=0 && nc<cols && mines[nr][nc]) count++;
            }
        }
        return count;
    }

    private void toggleFlag(CellButton btn) {
        if (btn.isRevealed()) return;
        if (!btn.isFlagged()) {
            if (flagsPlaced >= totalMines) {
                // optional: disallow more flags than mines
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            btn.setFlagged(true);
            flagsPlaced++;
        } else {
            btn.setFlagged(false);
            flagsPlaced--;
        }
        minesLabel.setText("Mines: " + (totalMines - flagsPlaced));
        checkWin();
    }

    private void openCell(CellButton btn) {
        if (btn.isFlagged() || btn.isRevealed()) return;

        if (firstClick) {
            placeMines(btn.row, btn.col);
            firstClick = false;
        }

        int r = btn.row, c = btn.col;
        if (mines[r][c]) {
            // Reveal all mines and end game
            btn.revealMine();
            revealAllMines();
            gameOver = true;
            JOptionPane.showMessageDialog(this, "BOOM! You hit a mine. Game over.", "Game Over", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // BFS/DFS reveal
        floodReveal(r, c);
        checkWin();
    }

    private void floodReveal(int startR, int startC) {
        Stack<Point> stack = new Stack<>();
        stack.push(new Point(startR, startC));
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            int r = p.x, c = p.y;
            CellButton b = buttons[r][c];
            if (b.isRevealed() || b.isFlagged()) continue;
            int adj = adjacentMines(r, c);
            b.reveal(adj);
            if (adj == 0) {
                for (int dr=-1; dr<=1; dr++) {
                    for (int dc=-1; dc<=1; dc++) {
                        int nr = r+dr, nc = c+dc;
                        if (nr>=0 && nr<rows && nc>=0 && nc<cols) {
                            if (!buttons[nr][nc].isRevealed() && !buttons[nr][nc].isFlagged()) {
                                stack.push(new Point(nr, nc));
                            }
                        }
                    }
                }
            }
        }
    }

    private void revealAllMines() {
        for (int r=0; r<rows; r++) {
            for (int c=0; c<cols; c++) {
                if (mines[r][c]) {
                    if (!buttons[r][c].isRevealed()) buttons[r][c].revealMine();
                }
            }
        }
    }

    private void checkWin() {
        if (gameOver) return;
        int revealed = 0;
        for (int r=0; r<rows; r++)
            for (int c=0; c<cols; c++)
                if (buttons[r][c].isRevealed()) revealed++;

        int totalCells = rows * cols;
        if (revealed == totalCells - totalMines) {
            gameOver = true;
            // flag remaining mines automatically
            for (int r=0; r<rows; r++) for (int c=0; c<cols; c++)
                if (mines[r][c]) buttons[r][c].setFlagged(true);
            JOptionPane.showMessageDialog(this, "Congratulations — you cleared the field!", "You Win", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // CellButton inner class
    private static class CellButton extends JButton {
        final int row, col;
        private boolean revealed = false;
        private boolean flagged = false;

        CellButton(int r, int c) {
            this.row = r;
            this.col = c;
            reset();
        }

        void reset() {
            revealed = false;
            flagged = false;
            setEnabled(true);
            setText("");
            setBackground(null);
            setForeground(Color.BLACK);
        }

        boolean isRevealed() { return revealed; }
        boolean isFlagged() { return flagged; }

        void setFlagged(boolean f) {
            flagged = f;
            if (f) {
                setText("⚑");
                setForeground(Color.RED.darker());
            } else {
                setText("");
            }
        }

        void reveal(int adjacentMines) {
            if (revealed) return;
            revealed = true;
            setEnabled(false);
            if (adjacentMines > 0) {
                setText(String.valueOf(adjacentMines));
                setForeground(colorForNumber(adjacentMines));
            } else {
                setText("");
            }
            setBackground(new Color(220,220,220));
        }

        void revealMine() {
            revealed = true;
            setEnabled(false);
            setText("✹");
            setForeground(Color.BLACK);
            setBackground(new Color(255, 100, 100));
        }

        private Color colorForNumber(int n) {
            switch (n) {
                case 1: return new Color(0,0,180);
                case 2: return new Color(0,150,0);
                case 3: return new Color(180,0,0);
                case 4: return new Color(120,0,120);
                case 5: return new Color(120,70,0);
                case 6: return new Color(0,120,120);
                default: return Color.BLACK;
            }
        }
    }

    // Main to start the game with a default grid; you can change difficulty here.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Beginner: 9x9 with 10 mines
            // Intermediate: 16x16 with 40 mines
            // Expert: 16x30 with 99 mines (but layout in this frame uses rows,cols)
            String[] options = {"Beginner (9x9, 10)", "Intermediate (16x16, 40)", "Custom"};
            int choice = JOptionPane.showOptionDialog(null, "Choose difficulty", "Bomb Sweeper",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice == 0) new BombSweeper(9, 9, 10);
            else if (choice == 1) new BombSweeper(16, 16, 40);
            else {
                try {
                    String r = JOptionPane.showInputDialog("Rows (e.g. 9):");
                    String c = JOptionPane.showInputDialog("Cols (e.g. 9):");
                    String m = JOptionPane.showInputDialog("Mines (e.g. 10):");
                    int rows = Integer.parseInt(r.trim());
                    int cols = Integer.parseInt(c.trim());
                    int mines = Integer.parseInt(m.trim());
                    if (mines >= rows*cols) throw new NumberFormatException();
                    new BombSweeper(rows, cols, mines);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Invalid input. Starting Beginner.", "Error", JOptionPane.ERROR_MESSAGE);
                    new BombSweeper(9,9,10);
                }
            }
        });
    }
}

