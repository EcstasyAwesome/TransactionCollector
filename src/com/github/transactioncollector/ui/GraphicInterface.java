package com.github.transactioncollector.ui;

import com.github.transactioncollector.SupportedTypes;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class GraphicInterface implements UserInterface {

    private JFrame jFrame = new JFrame();

    @Override
    public File[] fileChooser() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported types", SupportedTypes.getTypes());
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setDialogTitle("Choose files");
        fileChooser.setFileFilter(filter);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.showDialog(null, null);
        return fileChooser.getSelectedFiles();
    }

    @Override
    public void showBar() {
        JPanel main = new JPanel();
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        JPanel top = new JPanel();
        JLabel label = new JLabel("Processing...");
        top.add(label);
        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout());
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        bottom.add(progressBar);
        main.add(top);
        main.add(bottom);
        jFrame.setTitle("Progress");
        jFrame.getContentPane().add(main, BorderLayout.CENTER);
        jFrame.setPreferredSize(new Dimension(250, 110));
        jFrame.pack();
        jFrame.setLocationRelativeTo(null);
        jFrame.setResizable(false);
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setVisible(true);
    }

    @Override
    public void closeBar(String message) {
        if (jFrame.isShowing()) jFrame.dispose();
        JOptionPane.showMessageDialog(null, message);
    }
}
