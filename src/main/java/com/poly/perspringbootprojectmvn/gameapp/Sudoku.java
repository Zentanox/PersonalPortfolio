package com.poly.perspringbootprojectmvn.gameapp;

import java.util.List;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Sudoku extends JFrame {
    private final CellField[][] cells = new CellField[9][9];
    private int[][] solution = new int[9][9];
    private int[][] puzzle = new int[9][9];

    public Sudoku() {
        setTitle("Sudoku");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel board = new JPanel(new GridLayout(9, 9));
        board.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                CellField f = new CellField(r, c);
                cells[r][c] = f;
                board.add(f);
            }
        }
        add(board, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        JButton newEasy = new JButton("New (Easy)");
        JButton newMed = new JButton("New (Medium)");
        JButton newHard = new JButton("New (Hard)");
        JButton solve = new JButton("Solve");
        JButton hint = new JButton("Hint");
        JButton check = new JButton("Check");
        JButton reset = new JButton("Reset");

        newEasy.addActionListener(e -> newPuzzle(36));   // remove 36 -> easier
        newMed.addActionListener(e -> newPuzzle(46));    // medium
        newHard.addActionListener(e -> newPuzzle(54));   // hard
        solve.addActionListener(e -> {
            copyToSolution();
            if (solveGrid(solution)) {
                applySolutionToUI();
                setEditable(false);
                JOptionPane.showMessageDialog(this, "Solved!");
            } else {
                JOptionPane.showMessageDialog(this, "Cannot solve this puzzle.");
            }
        });
        hint.addActionListener(e -> giveHint());
        check.addActionListener(e -> checkBoard());
        reset.addActionListener(e -> resetToPuzzle());

        controls.add(newEasy);
        controls.add(newMed);
        controls.add(newHard);
        controls.add(solve);
        controls.add(hint);
        controls.add(check);
        controls.add(reset);

        add(controls, BorderLayout.SOUTH);

        // Start with a medium puzzle
        newPuzzle(46);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // UI cell - restrict input to single digit 1-9
    private class CellField extends JTextField {
        final int r, c;
        boolean given = false;

        CellField(int r, int c) {
            super(1);
            this.r = r; this.c = c;
            setHorizontalAlignment(JTextField.CENTER);
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            setPreferredSize(new Dimension(50, 50));
            // visual border to show 3x3 boxes
            int top = (r % 3 == 0) ? 4 : 1;
            int left = (c % 3 == 0) ? 4 : 1;
            int bottom = (r == 8) ? 4 : 1;
            int right = (c == 8) ? 4 : 1;
            setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, Color.BLACK));

            ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    String newText = fb.getDocument().getText(0, fb.getDocument().getLength());
                    newText = newText.substring(0, offset) + text + newText.substring(offset + length);
                    if (text.length() == 0) { // deletion
                        super.replace(fb, offset, length, "", attrs);
                        return;
                    }
                    if (newText.length() > 1) return; // only 1 char allowed
                    if (!text.matches("[1-9]")) return;
                    super.replace(fb, offset, length, text, attrs);
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    if (given) e.consume();
                }
            });
        }

        void setValue(int v, boolean isGiven) {
            given = isGiven;
            if (v == 0) setText("");
            else setText(String.valueOf(v));
            setEditable(!isGiven);
            setForeground(isGiven ? Color.BLACK : new Color(0, 100, 200));
            setBackground(isGiven ? new Color(240, 240, 240) : Color.WHITE);
        }

        int getValue() {
            String s = getText().trim();
            if (s.isEmpty()) return 0;
            try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
        }
    }

    private void newPuzzle(int removals) {
        // generate a full valid solution
        solution = generateFullSolution();
        // copy into puzzle
        puzzle = new int[9][9];
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) puzzle[r][c] = solution[r][c];

        // remove `removals` cells randomly to create a puzzle
        List<int[]> coords = new ArrayList<>();
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) coords.add(new int[]{r,c});
        Collections.shuffle(coords, new Random());
        int removed = 0;
        for (int[] p : coords) {
            if (removed >= removals) break;
            int r = p[0], c = p[1];
            int backup = puzzle[r][c];
            puzzle[r][c] = 0;

            // Optionally ensure uniqueness: check if puzzle has exactly one solution.
            // For speed, we won't enforce uniqueness in this simple generator, but you can try to check.
            removed++;
        }

        applyPuzzleToUI();
        setEditable(true);
    }

    private void applyPuzzleToUI() {
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) {
            int v = puzzle[r][c];
            cells[r][c].setValue(v, v != 0);
        }
    }

    private void resetToPuzzle() {
        applyPuzzleToUI();
        setEditable(true);
    }

    private void setEditable(boolean editable) {
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) {
            if (!cells[r][c].given) cells[r][c].setEditable(editable);
        }
    }

    private int[][] generateFullSolution() {
        int[][] grid = new int[9][9];
        List<Integer> nums = new ArrayList<>();
        for (int i=1;i<=9;i++) nums.add(i);
        solveGridRandomized(grid, 0, 0, nums);
        return grid;
    }

    // Backtracking solver with randomized order — used to build a full solution
    private boolean solveGridRandomized(int[][] grid, int row, int col, List<Integer> baseOrder) {
        if (row == 9) return true;
        int nextR = (col == 8) ? row + 1 : row;
        int nextC = (col == 8) ? 0 : col + 1;
        if (grid[row][col] != 0) return solveGridRandomized(grid, nextR, nextC, baseOrder);
        List<Integer> order = new ArrayList<>(baseOrder);
        Collections.shuffle(order, new Random());
        for (int n : order) {
            if (isSafe(grid, row, col, n)) {
                grid[row][col] = n;
                if (solveGridRandomized(grid, nextR, nextC, baseOrder)) return true;
                grid[row][col] = 0;
            }
        }
        return false;
    }

    // Standard backtracking solver (deterministic) — used for "Solve" and optionally uniqueness checks
    private boolean solveGrid(int[][] grid) {
        int[] pos = findUnassigned(grid);
        if (pos == null) return true;
        int r = pos[0], c = pos[1];
        for (int n=1;n<=9;n++) {
            if (isSafe(grid, r, c, n)) {
                grid[r][c] = n;
                if (solveGrid(grid)) return true;
                grid[r][c] = 0;
            }
        }
        return false;
    }

    private int[] findUnassigned(int[][] grid) {
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) if (grid[r][c] == 0) return new int[]{r,c};
        return null;
    }

    private boolean isSafe(int[][] grid, int row, int col, int num) {
        for (int i=0;i<9;i++) if (grid[row][i] == num) return false;
        for (int i=0;i<9;i++) if (grid[i][col] == num) return false;
        int sr = (row/3)*3, sc = (col/3)*3;
        for (int r=sr;r<sr+3;r++) for (int c=sc;c<sc+3;c++) if (grid[r][c] == num) return false;
        return true;
    }

    private void copyToSolution() {
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) {
            int v = cells[r][c].getValue();
            solution[r][c] = v;
        }
    }

    private void applySolutionToUI() {
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) {
            cells[r][c].setValue(solution[r][c], false);
        }
    }

    private void giveHint() {
        // find the first empty cell and give the correct value according to solving the puzzle
        int[][] current = new int[9][9];
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) current[r][c] = cells[r][c].getValue();

        int[] pos = findUnassigned(current);
        if (pos == null) {
            JOptionPane.showMessageDialog(this, "No empty cells. Puzzle complete?");
            return;
        }
        int r = pos[0], c = pos[1];
        int[][] copy = new int[9][9];
        for (int i=0;i<9;i++) System.arraycopy(current[i], 0, copy[i], 0, 9);
        if (solveGrid(copy)) {
            int correct = copy[r][c];
            cells[r][c].setText(String.valueOf(correct));
            cells[r][c].setForeground(new Color(0,150,0));
            cells[r][c].setEditable(false);
            JOptionPane.showMessageDialog(this, "Hint placed at (" + (r+1) + "," + (c+1) + ")");
        } else {
            JOptionPane.showMessageDialog(this, "Cannot find solution for hint.");
        }
    }

    private void checkBoard() {
        // Validate each filled cell against Sudoku rules and report any errors or success
        int[][] current = new int[9][9];
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) current[r][c] = cells[r][c].getValue();

        List<String> errors = new ArrayList<>();
        for (int r=0;r<9;r++) {
            for (int c=0;c<9;c++) {
                int v = current[r][c];
                if (v == 0) continue;
                current[r][c] = 0; // temporarily remove to test validity
                if (!isSafe(current, r, c, v)) {
                    errors.add("Conflict at (" + (r+1) + "," + (c+1) + ") value " + v);
                }
                current[r][c] = v;
            }
        }
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this, String.join("\n", errors), "Errors found", JOptionPane.ERROR_MESSAGE);
        } else {
            // also check if solved
            boolean complete = true;
            for (int r=0;r<9;r++) for (int c=0;c<9;c++) if (current[r][c]==0) complete = false;
            if (complete) JOptionPane.showMessageDialog(this, "No conflicts found. Puzzle appears valid!");
            else JOptionPane.showMessageDialog(this, "No conflicts found so far.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Sudoku::new);
    }
}
