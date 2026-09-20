package org.example;
import org.example.Ui;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Spec item 11: the server's Start / Stop screen. Shows whether the server is
 * running and a live log of connections and requests.
 */
public class ServerGUI extends JFrame {

    private final Server server = new Server();

    private final JLabel statusLabel = Ui.label("Server is stopped", Font.BOLD, 16, Ui.DANGER);
    private final Ui.RoundedButton startBtn = new Ui.RoundedButton("Start Server", Ui.SUCCESS);
    private final Ui.RoundedButton stopBtn  = new Ui.RoundedButton("Stop Server", Ui.DANGER);
    private final JTextArea log = new JTextArea();

    public ServerGUI() {
        super("i-Wish Server");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(640, 460);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Ui.BG);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(Ui.label("i-Wish Server", Font.BOLD, 22, Ui.TEXT));
        titles.add(statusLabel);
        top.add(titles, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(startBtn);
        buttons.add(stopBtn);
        top.add(buttons, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        root.add(scroll, BorderLayout.CENTER);

        setContentPane(root);
        redirectConsoleToLog();
        refreshButtons();

        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                server.stop();
                dispose();
                System.exit(0);
            }
        });
    }

    private void startServer() {
        try {
            server.start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Could not start", JOptionPane.ERROR_MESSAGE);
        }
        refreshButtons();
    }

    private void stopServer() {
        server.stop();
        refreshButtons();
    }

    private void refreshButtons() {
        boolean running = server.isRunning();
        startBtn.setEnabled(!running);
        stopBtn.setEnabled(running);
        statusLabel.setText(running ? "Server is running on port " + Server.PORT : "Server is stopped");
        statusLabel.setForeground(running ? Ui.OK_TEXT : Ui.DANGER);
    }

    /** Everything the server prints (System.out / System.err) also appears in the window. */
    private void redirectConsoleToLog() {
        System.setOut(new PrintStream(new LogStream(System.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new LogStream(System.err), true, StandardCharsets.UTF_8));
    }

    private class LogStream extends OutputStream {
        private final PrintStream original;
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();

        LogStream(PrintStream original) { this.original = original; }

        @Override public synchronized void write(int b) {
            original.write(b);
            if (b == '\n') {
                String text = line.toString(StandardCharsets.UTF_8).trim();
                line.reset();
                if (!text.isEmpty()) SwingUtilities.invokeLater(() -> {
                    log.append(text + "\n");
                    log.setCaretPosition(log.getDocument().getLength());
                });
            } else if (b != '\r') {
                line.write(b);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerGUI().setVisible(true));
    }
}

