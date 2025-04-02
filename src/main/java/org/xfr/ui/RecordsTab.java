package org.xfr.ui;

import org.xfr.SFScan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RecordsTab extends JPanel implements ActionListener {
    private final SFScan sfScan;
    public JTextArea textArea;

    public RecordsTab(SFScan sfScan) {
        this.sfScan = sfScan;
        this.setLayout(new BorderLayout());
        // Build panel
        JButton updateButton = new JButton("Update");
        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        updateButton.setPreferredSize(new Dimension(100, 50));

        // Add listeners
        updateButton.addActionListener(this);

        // Add items to panel
        this.add(scrollPane, BorderLayout.CENTER);
        this.add(updateButton, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        sfScan.log.logToOutput("Button Pressed");
    }
}
